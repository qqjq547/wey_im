package framework.telegram.app.keepalive.service

import android.content.Intent
import framework.telegram.app.keepalive.manager.ForegroundManager
import framework.telegram.app.keepalive.manager.ScreenManager
import framework.telegram.app.keepalive.utils.AbstractServiceConnection
import framework.telegram.app.keepalive.utils.bindServiceSafely
import framework.telegram.app.keepalive.utils.startServiceSafely
import framework.telegram.app.keepalive.utils.unbindServiceSafely

class KeepLiveService : LoggerService() {

    private val mForegroundManager by lazy {
        ForegroundManager(this)
    }

    private val mScreenManager by lazy {
        ScreenManager(this)
    }

    private val mService by lazy {
        Intent(this, RemoteService::class.java)
    }

    private val mConn by lazy {
        AbstractServiceConnection()
    }

    override fun onCreate() {
        super.onCreate()
        mForegroundManager.setServiceForeground()
        mScreenManager.registerScreenReceiver()
        startServiceSafely(mService)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        bindServiceSafely(mService, mConn)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindServiceSafely(mConn)
        mScreenManager.unregisterScreenReceiver()
        mForegroundManager.cancelServiceForeground()
    }

}
