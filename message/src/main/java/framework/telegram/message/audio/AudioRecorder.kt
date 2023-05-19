package framework.telegram.message.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import framework.telegram.support.BaseApp

import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.tools.IoUtils
import framework.telegram.support.tools.ThreadUtils
import fftlib.FftFactory
import framework.telegram.message.R


class AudioRecorder {

    private val mAudioRecordPackage by lazy { createAudioRecord() }

    private var mAudioRecordThread: AudioRecordThread? = null

    private var mOnAudioRecorderListener: OnAudioRecorderListener? = null

    fun isRecording(): Boolean {
        return mAudioRecordThread?.isRecording() ?: false
    }

    fun startRecording(cacheDir: File, recordMinTime: Int, recordMaxTime: Int) {
        if (mAudioRecordThread == null) {
            mAudioRecordThread = AudioRecordThread(mAudioRecordPackage, cacheDir, recordMinTime, recordMaxTime, mOnAudioRecorderListener)
        }

        mAudioRecordThread?.start()
    }

    fun stopRecording() {
        mAudioRecordThread?.stopRecord()
        mAudioRecordThread = null
    }

    fun cancelRecording() {
        mAudioRecordThread?.cancelRecord()
        mAudioRecordThread = null
    }

    fun isLessThanOneSecond(): Boolean {
        return mAudioRecordThread?.isLessThanOneSecond() ?: false
    }

    fun setOnAudioRecorderListener(listener: OnAudioRecorderListener) {
        mOnAudioRecorderListener = listener
    }

    private class AudioRecordThread(private val audioRecordPackage: AudioRecordPackage, private val cacheDir: File,
                                    private val recordMinTime: Int, private val recordMaxTime: Int,
                                    private val onAudioRecorderListener: OnAudioRecorderListener?) : Thread() {
        private val isRecording by lazy { AtomicBoolean(true) }
        private val isOverRecordMaxTime by lazy { AtomicBoolean(false) }
        private val isCancel by lazy { AtomicBoolean(false) }
        private var startRecordTime = 0L
        private var endRecordTime = 0L
        private var volumeValues = mutableListOf<ByteArray>()
        private val fftFactory = FftFactory()

        override fun run() {
            var audioRecord: AudioRecord? = null
            var output: DataOutputStream? = null
            var buffer: ShortArray? = null
            var fileName: String? = null
            var tmpFile: File? = null
            endRecordTime = 0L
            startRecordTime = 0L

            try {
                audioRecord = audioRecordPackage.audioRecord
                buffer = ShortArray(audioRecordPackage.bufferSize)

                fileName = System.currentTimeMillis().toString()

                tmpFile = File(cacheDir, "$fileName.tmp")
                output = DataOutputStream(BufferedOutputStream(
                        FileOutputStream(tmpFile)))

                AppLogcat.logger.d(TAG, "开始录制》》》》》")
                audioRecord.startRecording()
                startRecordTime = System.currentTimeMillis()
                callRecordStart()
                while (isRecording.get() && !isCancel.get()) {
                    AppLogcat.logger.d(TAG, "录制中》》》》》")
                    val readSize = audioRecord.read(buffer, 0, buffer.size)
                    if (readSize >= AudioRecord.SUCCESS) {
                        for (i in 0 until readSize) {
                            output.writeShort(buffer[i].toInt())
                        }

                        // 计算音量变化
                        val volumn = calculateVolume(buffer).toInt()
                        val fftData = fftFactory.makeFftData(buffer)
                        volumeValues.add(fftData)

                        // 音量回调
                        callVolumnChange(volumn.toDouble())

                        val time = System.currentTimeMillis() - startRecordTime
                        if (time >= recordMaxTime) {
                            isOverRecordMaxTime.set(true)
                            stopRecord()
                        } else {
                            // 计算时间变化
                            callTimeChange(time)
                        }
                    } else {
                        //录音可能被禁用了，做出适当的提示
                        throw UnsupportedOperationException("录音数据为空")
                    }
                }
                endRecordTime = System.currentTimeMillis()
                output.flush()
                AppLogcat.logger.d(TAG, "录制结束》》》》》")

                //取fft数据
                val maxReadSamples = 200
                val adjustedSamples = IntArray(maxReadSamples)
                var numSamples = 0
                volumeValues.forEach {
                    numSamples += it.size
                }
                if (numSamples >= 200) {
                    // 先计算出需要的索引，暂存
                    for (i in 0 until maxReadSamples) {
                        adjustedSamples[i] = i * numSamples / maxReadSamples
                    }

                    var index = 0//当前索引
                    var adjustedSamplesIndex = 0
                    var findNextIndex = adjustedSamples[adjustedSamplesIndex]
                    var preMaxValue = 0
                    var totalMaxValue = 0
                    run outside@{
                        volumeValues.forEach {
                            for (position in it.indices) {
                                //找区间最大值
                                if (it[position] > preMaxValue) {
                                    preMaxValue = it[position].toInt()
                                }

                                if (index == findNextIndex) {
                                    //赋值
                                    adjustedSamples[adjustedSamplesIndex] = preMaxValue

                                    //找总最大值
                                    if (preMaxValue > totalMaxValue) {
                                        totalMaxValue = preMaxValue
                                    }

                                    //下一个需要计算的点
                                    val next = ++adjustedSamplesIndex
                                    if (next < maxReadSamples) {
                                        findNextIndex = adjustedSamples[next]
                                        preMaxValue = 0
                                    } else {
                                        return@outside
                                    }
                                }

                                index++
                            }
                        }
                    }

                    //将值计算到0-100的区间
                    for (i in 0 until maxReadSamples) {
                        adjustedSamples[i] = 100 * adjustedSamples[i] / totalMaxValue
                    }
                }

                if (!isCancel.get()) {
                    if (endRecordTime - startRecordTime > recordMinTime) {
                        if (isOverRecordMaxTime.get()) {
                            callRecordSoLong(tmpFile, endRecordTime - startRecordTime, adjustedSamples.toTypedArray())
                        } else {
                            callRecordComplete(tmpFile, endRecordTime - startRecordTime, adjustedSamples.toTypedArray())
                        }
                        AppLogcat.logger.d(TAG, "录制完成》》》》》")
                    } else {
                        callRecordSoShort()
                        AppLogcat.logger.d(TAG, "录制时间太短》》》》》")
                        tmpFile.delete()
                    }
                } else {
                    AppLogcat.logger.d(TAG, "取消录制》》》》》")
                    callRecordCancel()
                    tmpFile.delete()
                }
            } catch (e: UnsupportedOperationException) {
                AppLogcat.logger.d(TAG, "录制失败》》》》》")
                callRecordFail(e, BaseApp.app.getString(R.string.check_the_recording_permissions))
                tmpFile?.delete()
            } catch (t: Throwable) {
                t.printStackTrace()
                AppLogcat.logger.d(TAG, "录制失败》》》》》")
                callRecordFail(t, t.message)
                tmpFile?.delete()
            } finally {
                if (audioRecord != null) {
                    try {
                        audioRecord.stop()
                        audioRecord.release()
                        audioRecord = null
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                IoUtils.safeClose(output)
                isRecording.set(false)
                isCancel.set(false)
            }
        }

        fun isLessThanOneSecond(): Boolean {
            return System.currentTimeMillis() - startRecordTime < recordMinTime
        }

        fun stopRecord() {
            isRecording.set(false)
        }

        fun isRecording(): Boolean {
            return isRecording.get()
        }

        fun cancelRecord() {
            isCancel.set(true)
        }

        private fun callRecordStart() {
            ThreadUtils.runOnUIThread {
                onAudioRecorderListener?.onStartRecord()
            }
        }

        private fun callVolumnChange(volumnLevel: Double) {
            ThreadUtils.runOnUIThread {
                onAudioRecorderListener?.onVolumnChange(volumnLevel)
            }
        }

        private fun callTimeChange(time: Long) {
            ThreadUtils.runOnUIThread {
                onAudioRecorderListener?.onTimerChange(time)
            }
        }

        private fun callRecordComplete(saveFile: File, time: Long, highDArr: Array<Int>) {
            ThreadUtils.runOnUIThread {
                onAudioRecorderListener?.onRecordComplete(saveFile, time, highDArr)
            }
        }

        private fun callRecordFail(e: Throwable, info: String?) {
            ThreadUtils.runOnUIThread {
                onAudioRecorderListener?.onRecordFail(e, info)
            }
        }

        private fun callRecordCancel() {
            ThreadUtils.runOnUIThread {
                onAudioRecorderListener?.onRecordCancel()
            }
        }

        private fun callRecordSoShort() {
            ThreadUtils.runOnUIThread {
                onAudioRecorderListener?.onRecordSoShort()
            }
        }

        private fun callRecordSoLong(saveFile: File, time: Long, highDArr: Array<Int>) {
            ThreadUtils.runOnUIThread {
                onAudioRecorderListener?.onRecordSoLong(saveFile, time, highDArr)
            }
        }
    }

    private class AudioRecordPackage(var audioRecord: AudioRecord, var bufferSize: Int, var sampleRateInHz: Int, var channelConfig: Int, var audioFormat: Int)

    interface OnAudioRecorderListener {
        /**
         * 开始
         */
        fun onStartRecord()

        /**
         * 完成
         *
         * @param file 录音文件保存的File
         */
        fun onRecordComplete(file: File, recordTime: Long, highDArr: Array<Int>)

        /**
         * 失败
         *
         * @param e
         * @param info
         */
        fun onRecordFail(e: Throwable, info: String?)

        /**
         * 取消中
         */
        fun onRecordCancel()

        /**
         * 录音时间太长
         */
        fun onRecordSoLong(file: File, recordTime: Long, highDArr: Array<Int>)

        /**
         * 录音时间太短
         */
        fun onRecordSoShort()

        /**
         * 音量等级改变时回调（1000ms回调一次）
         *
         * @param volumnLevel
         */
        fun onVolumnChange(volumnLevel: Double)

        /**
         * 录音时长改变时回调（1000ms回调一次）
         *
         * @param time
         */
        fun onTimerChange(time: Long)
    }

    companion object {

        private const val TAG = "AudioRecorder"

        private fun createAudioRecord(): AudioRecordPackage {
            for (sampleRate in intArrayOf(8000, 11025, 16000, 22050, 32000, 44100, 47250, 48000)) {
                for (audioFormat in shortArrayOf(AudioFormat.ENCODING_PCM_16BIT.toShort(), AudioFormat.ENCODING_PCM_8BIT.toShort())) {
                    for (channelConfig in shortArrayOf(AudioFormat.CHANNEL_IN_MONO.toShort(), AudioFormat.CHANNEL_IN_STEREO.toShort(), AudioFormat.CHANNEL_CONFIGURATION_MONO.toShort(), AudioFormat.CHANNEL_CONFIGURATION_STEREO.toShort())) {
                        // Try to initialize
                        try {
                            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig.toInt(), audioFormat.toInt())
                            if (bufferSize < 0) {
                                continue
                            }

                            var audioRecord: AudioRecord? = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig.toInt(), audioFormat.toInt(), bufferSize)
                            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                                return AudioRecordPackage(audioRecord, bufferSize, sampleRate, channelConfig.toInt(), audioFormat.toInt())
                            }

                            audioRecord?.release()
                            audioRecord = null
                        } catch (e: Exception) {
                            // Do nothing
                        }
                    }
                }
            }

            val bufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
            return AudioRecordPackage(audioRecord, bufferSize, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        }

        private fun calculateVolume(buffer: ShortArray): Double {
            var sumVolume = 0.0
            var avgVolume = 0.0
            var volume = 0.0
            for (b in buffer) {
                sumVolume += Math.abs(b.toInt()).toDouble()
            }
            avgVolume = sumVolume / buffer.size
            volume = Math.log10(1 + avgVolume) * 10
            return volume
        }
    }
}
