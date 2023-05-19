package framework.telegram.app.keepalive

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import framework.telegram.app.keepalive.service.KeepLiveService
import framework.telegram.app.keepalive.service.RemoteService
import framework.telegram.app.keepalive.service.TimingService
import framework.telegram.app.keepalive.utils.*

/**
 * @author hyf
 * @date 2019/3/7
 */
@SuppressLint("StaticFieldLeak")
object KeepLive {

    private var mContext: Context? = null

    /**
     * 主进程Application的super.onCreate()之后立即调用
     */
    fun init(context: Context): Boolean {
        mContext = context.applicationContext
        return isMainProcess(context)
    }

    /**
     * 开启进程保护
     * 在主进程中调用
     */
    fun startKeepLiveService() {
        val context = mContext.assertNull()
        if (isMainProcess(context)) {
            val intent = Intent(context, KeepLiveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundServiceSafely(intent)
            } else {
                context.startServiceSafely(intent)
            }
        }
    }

    /**
     * 停止进程保护
     * 在主进程中调用
     */
    fun stopKeepLiveService() {
        val context = mContext.assertNull()
        if (isMainProcess(context)) {
            context.apply {
                stopServiceSafely(Intent(this, TimingService::class.java))
                stopServiceSafely(Intent(this, RemoteService::class.java))
                stopServiceSafely(Intent(this, KeepLiveService::class.java))
            }
        }
    }

    internal fun logger(message: String) {
        val context = mContext.assertNull()
        if (isDebug(context)) {
            Log.d("KeepLive", message)
        }
    }

    private fun Context?.assertNull(): Context {
        return this ?: throw IllegalStateException("please init first")
    }
}