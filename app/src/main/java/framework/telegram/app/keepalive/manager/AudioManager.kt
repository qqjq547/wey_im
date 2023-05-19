package framework.telegram.app.keepalive.manager

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import framework.telegram.app.R
import framework.telegram.app.keepalive.KeepLive.logger
import framework.telegram.app.keepalive.utils.getCurrentProcessName
import kotlin.concurrent.thread

/**
 * @author hyf
 * @date 2019/3/7
 */

internal class AudioManager(private val context: Context) {

    private val mHandler by lazy {
        Handler()
    }

    private val mMediaPlayer by lazy {
        MediaPlayer.create(context, R.raw.silent)
    }

    init {
        mMediaPlayer.isLooping = true
    }

    fun startMusic() {
        thread {
            mMediaPlayer.start()
        }
        startTask()
    }

    fun stopMusic() {
        mMediaPlayer.stop()
        stopTask()
    }

    private fun startTask() {
        mHandler.postDelayed(object : Runnable {
            override fun run() {
                logger("音频是否在播放：${mMediaPlayer.isPlaying}")
                logger("processName: ${getCurrentProcessName(context)}")
                mHandler.postDelayed(this, 30 * 1000)
            }

        }, 30 * 1000)
    }

    private fun stopTask() {
        mHandler.removeCallbacksAndMessages(null)
    }
}