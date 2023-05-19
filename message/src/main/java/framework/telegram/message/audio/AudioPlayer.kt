package framework.telegram.message.audio

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

import java.io.File
import java.lang.ref.WeakReference

import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.utils.FileUtils
import tv.danmaku.ijk.media.player.IjkMediaPlayer

class AudioPlayer(var onAudioPlayerListener: OnAudioPlayerListener? = null) {

    var mediaPlayer: IjkMediaPlayer? = null
        private set

    var playingUri: String? = null
        private set

    var playingTag: Any? = null
        private set

    private var mSeekTo: Int = 0

    val isPlaying: Boolean
        get() = if (mediaPlayer != null) {
            mediaPlayer?.isPlaying ?: false
        } else false

    fun seekTo(time: Int) {
        if (isPlaying)
            mediaPlayer?.seekTo(time.toLong())
    }

    @JvmOverloads
    fun startPlaying(activity: WeakReference<Activity>, playFileUri: Uri, tag: String, isSpeakerphoneOn: Boolean, seekTo: Int, isLooping: Boolean = false) {
        try {
            activity.get()?.let { it ->
                mSeekTo = seekTo

                playingUri = playFileUri.toString()
                playingTag = tag

                onAudioPlayerListener?.onPrepare()
                mediaPlayer = IjkMediaPlayer()
                mediaPlayer?.setDataSource(it.applicationContext, playFileUri)
                val audioManager = activity.get()?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                if (isSpeakerphoneOn) {
                    Log.e("isSpeakerphoneOn","isSpeakerphoneOn==true")
                    mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    audioManager.isSpeakerphoneOn = true
                    audioManager.mode = AudioManager.MODE_NORMAL
                    it.volumeControlStream = AudioManager.STREAM_MUSIC
                } else {
                    Log.e("isSpeakerphoneOn","isSpeakerphoneOn==false")
                    it.volumeControlStream = AudioManager.STREAM_VOICE_CALL
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    audioManager.isSpeakerphoneOn = false
                    mediaPlayer?.setAudioStreamType(AudioManager.STREAM_VOICE_CALL)
                }
                mediaPlayer?.setOnCompletionListener { mp ->
                    playingUri = null
                    playingTag = null
                    stopPlaying(activity, false)
                    activity.get()?.let { AudioUtil.abandonAudioFocus(it) }
                    onAudioPlayerListener?.onPlayComplete()
                }
                mediaPlayer?.setOnErrorListener { _, what, ex ->
                    playingUri = null
                    playingTag = null
                    stopPlaying(activity, false)
                    activity.get()?.let { AudioUtil.abandonAudioFocus(it) }
                    onAudioPlayerListener?.onStopPlay()
                    false
                }
                mediaPlayer?.setOnPreparedListener { mp ->
                    if (seekTo > 0) {
                        mp.start()
                        mp.pause()
                        mp.seekTo(seekTo.toLong())
                        mp.start()
                    } else {
                        mp.start()
                    }

                    activity.get()?.let { AudioUtil.requestFocus(it) }

                    onAudioPlayerListener?.onStartPlay()

                    callTimeChange(0)
                }

                if (isLooping) {
                    mediaPlayer?.isLooping = true
                }

                mediaPlayer?.prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()

            stopAndRelease(activity, true)

            if (playingUri != null) {
                val uri = UriUtils.parseUri(playingUri)
                if (uri != null) {
                    FileUtils.deleteQuietly(File(uri.path))
                }
            }
        }
    }

    private fun stopAndRelease(activity: WeakReference<Activity>, callListener: Boolean) {
        playingUri = null
        playingTag = null

        try {
            mediaPlayer?.setOnPreparedListener(null)
            mediaPlayer?.setOnErrorListener(null)
            mediaPlayer?.setOnCompletionListener(null)
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        activity.get()?.let { AudioUtil.abandonAudioFocus(it) }

        if (callListener) {
            onAudioPlayerListener?.onStopPlay()
        }
    }

    fun stopPlaying(activity: WeakReference<Activity>): Int {
        return stopPlaying(activity, true)
    }

    fun stopPlaying(activity: WeakReference<Activity>, callListener: Boolean): Int {
        val currentPosition = if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.currentPosition ?: 0
        } else {
            0
        }

        stopAndRelease(activity, callListener)

        return currentPosition.toInt()
    }

    private fun callTimeChange(nextTime: Long) {
        ThreadUtils.runOnUIThread(nextTime) {
            if (onAudioPlayerListener != null && mediaPlayer != null) {
                onAudioPlayerListener?.onTimerChange(mediaPlayer?.currentPosition?.toLong()
                        ?: 0L)
            }

            if (mediaPlayer != null) {
                callTimeChange(90)
            }
        }
    }

    interface OnAudioPlayerListener {

        fun onDownloadStart()

        fun onDownloadProgress(totalBytes: Long, downloadedBytes: Long, progress: Int)

        fun onDownloadComplete()

        fun onPrepare()

        fun onStartPlay()

        /**
         * 录音时长改变时回调（1000ms回调一次）
         *
         * @param time
         */
        fun onTimerChange(time: Long)

        fun onStopPlay()

        fun onPlayComplete()
    }
}
