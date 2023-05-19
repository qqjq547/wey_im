package framework.telegram.app.keepalive.manager

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import framework.telegram.app.keepalive.KeepLive.logger
import framework.telegram.app.keepalive.activity.OnePixelActivity
import framework.telegram.app.keepalive.utils.ACTION_FINISH_ONE_PIXEL_ACTIVITY
import framework.telegram.app.keepalive.utils.AbstractReceiver
import framework.telegram.app.keepalive.utils.registerReceiverSafely
import framework.telegram.app.keepalive.utils.unregisterReceiverSafely

/**
 * @author hyf
 * @date 2019/3/7
 */
internal class ScreenManager(private val mContext: Context) {

    private val mScreenReceiver by lazy {
        AbstractReceiver().onReceive { _, intent ->
            intent?.action?.let {
                when (it) {
                    Intent.ACTION_SCREEN_ON -> finishOnePixelActivity()
                    Intent.ACTION_SCREEN_OFF -> startOnePixelActivity()
                    Intent.ACTION_USER_PRESENT -> {
                        // 解锁，暂时不用，保留
                    }
                }
                Unit
            }
        }
    }

    private fun startOnePixelActivity() {
        logger("开启一像素")
        val intent = Intent(mContext, OnePixelActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        mContext.startActivity(intent)
    }

    private fun finishOnePixelActivity() {
        logger("关闭一像素")
        mContext.sendBroadcast(Intent(ACTION_FINISH_ONE_PIXEL_ACTIVITY))
    }

    fun registerScreenReceiver() {
        mContext.registerReceiverSafely(mScreenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        })
    }

    fun unregisterScreenReceiver() {
        mContext.unregisterReceiverSafely(mScreenReceiver)
    }

}