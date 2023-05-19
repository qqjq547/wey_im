package framework.telegram.message.manager

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Vibrator
import framework.telegram.message.R
import framework.telegram.support.BaseApp

object SoundPoolManager {

    private val VIBRATE_DURATION = 200L

    private val mSoundPool by lazy {
        SoundPool(10, AudioManager.STREAM_RING, 8)
    }

    private val mSoundIds by lazy { ArrayList<Int>() }

    private val mSoundStreamMaps by lazy { HashMap<Int, Int?>() }

    fun init() {
        mSoundIds.add(mSoundPool.load(BaseApp.app, R.raw.msg_send, 1))
        mSoundIds.add(mSoundPool.load(BaseApp.app, R.raw.msg_recv, 1))
        mSoundIds.add(mSoundPool.load(BaseApp.app, R.raw.msg_stream_call_calling, 1))
        mSoundIds.add(mSoundPool.load(BaseApp.app, R.raw.msg_stream_call_waiting, 1))
        mSoundIds.add(mSoundPool.load(BaseApp.app, R.raw.msg_stream_call_end, 1))
    }

    fun playMsgRecvForeground() {
        mSoundPool.play(mSoundIds[0], 1.0f, 1.0f, 1, 0, 1.0f)
    }

    fun playMsgRecv() {
        mSoundPool.play(mSoundIds[1], 1.0f, 1.0f, 1, 0, 1.0f)
    }

    fun vibrator() {
        val vibrator = BaseApp.app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VIBRATE_DURATION)
    }

    fun vibratorRepeat() {
        val vibrator = BaseApp.app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(listOf<Long>(1000, 1000, 1000).toLongArray(), 1)
    }

    fun stopVibrator() {
        val vibrator = BaseApp.app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.cancel()
    }

    fun playStreamCalling() {
        stopPlayStreamCalling()
        mSoundStreamMaps[mSoundIds[2]] = mSoundPool.play(mSoundIds[2], 1.0f, 1.0f, 1, -1, 1.0f)
    }

    fun playStreamCallWaiting() {
        stopPlayStreamCallWaiting()
        mSoundStreamMaps[mSoundIds[3]] = mSoundPool.play(mSoundIds[3], 1.0f, 1.0f, 1, -1, 1.0f)
    }

    fun stopPlayStreamCalling() {
        mSoundStreamMaps[mSoundIds[2]]?.let {
            mSoundPool.stop(it)
            mSoundStreamMaps[mSoundIds[2]] = null
        }
    }

    fun stopPlayStreamCallWaiting() {
        mSoundStreamMaps[mSoundIds[3]]?.let {
            mSoundPool.stop(it)
            mSoundStreamMaps[mSoundIds[3]] = null
        }
    }

    fun playStreamCallEnd() {
        mSoundPool.play(mSoundIds[4], 1.0f, 1.0f, 1, 0, 1.0f)
    }
}