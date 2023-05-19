package framework.telegram.app.keepalive.service

import android.content.Intent
import framework.telegram.app.keepalive.manager.AudioManager
import framework.telegram.app.keepalive.manager.ForegroundManager
import framework.telegram.app.keepalive.manager.TimingManager
import framework.telegram.app.keepalive.manager.WakeUpBroadcastManager
import framework.telegram.app.keepalive.utils.AbstractServiceConnection
import framework.telegram.app.keepalive.utils.bindServiceSafely
import framework.telegram.app.keepalive.utils.startServiceSafely
import framework.telegram.app.keepalive.utils.unbindServiceSafely

/**
 * @author hyf
 * @date 2019/3/7
 */
class RemoteService : LoggerService() {

    private val mForegroundManager by lazy {
        ForegroundManager(this)
    }

    private val mAudioManager by lazy {
        AudioManager(this)
    }

    private val mTimingManager by lazy {
        TimingManager(this)
    }

    private val mWakeUpBroadcastManager by lazy {
        WakeUpBroadcastManager(this)
    }

    private val mConn by lazy {
        AbstractServiceConnection()
    }

    override fun onCreate() {
        super.onCreate()
        mForegroundManager.setServiceForeground()
        mAudioManager.startMusic()
        mTimingManager.startJob()
        mWakeUpBroadcastManager.registerWakeUpReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        bindServiceSafely(Intent(this, KeepLiveService::class.java), mConn)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindServiceSafely(mConn)
        mAudioManager.stopMusic()
        mForegroundManager.cancelServiceForeground()
        mTimingManager.stopJob()
        mWakeUpBroadcastManager.unregisterWakeUpReceiver()
        // 调用stopKeepLiveService方法会杀不死服务，但是如果不关心服务的关闭，可取消注释。
//        restart()
    }

    private fun restart() {
        startServiceSafely(Intent(this, RemoteService::class.java))
    }
}