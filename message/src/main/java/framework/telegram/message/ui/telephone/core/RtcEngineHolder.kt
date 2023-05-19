package framework.telegram.message.ui.telephone.core

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.text.TextUtils
import android.util.Log
import android.view.ViewGroup
import com.alibaba.android.arouter.launcher.ARouter
import com.im.pb.IMPB
import com.yhao.floatwindow.FloatWindow
import com.yhao.floatwindow.PermissionListener
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.manager.ReceiveMessageManager
import framework.telegram.message.manager.SendMessageManager
import framework.telegram.message.manager.SoundPoolManager
import framework.telegram.message.controller.StreamCallController
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.ui.telephone.TelephoneActivity
import framework.telegram.message.ui.telephone.view.TelephoneMiniView
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.AESHelper
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.HexString
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.ui.utils.ScreenUtils
import io.agora.rtc.Constants
import io.agora.rtc.Constants.*
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

/**
 *
 *
 *
 */
@SuppressLint("InvalidWakeLockTag")
object RtcEngineHolder : SensorEventListener {

    private const val CALLED_TIME_OUT = 10 * 1000//超时时间

    private const val CALLING_TIME_OUT = 60 * 1000//超时时间

    var pendingCall: PendingCall? = null

    //当前的频道号
    var currentChannelName: String = ""

    //当前用户id
    var mineUid = 0L

    //当前的通话目标用户id
    var targetUid = 0L

    //通话方向(0是请求，1是接收)
    var openType = 0

    //通话类型(0是语音，1是视频)
    var streamType = 0

    //是否已加入语音频道
    var isJoinAudio = false

    //是否已加入视频频道
    var isJoinVideo = false

    //是否静音
    var isMute = false

    //是否开启扬声器
    var isSpeaker = true

    // 是否需要检测FloatWindow视图是否存在（不存在自动创建）
    var checkFloatWindow = false

    // 发起请求的时间(己方发起请求)
    var reqTime = 0L

    // 加入频道的时间(己方进入频道)
    var joinChannelTime = 0L

    // 对方用户是否加入频道
    var hasUserJoined = false

    // 通话开始的时间(双方都进入频道)
    private var callStartTime = 0L

    // 对方用户基本信息（FloatWindow需要使用）
    private var targetUserInfo: ContactDataModel? = null

    // 最后一次找到Handler的时间，用于配合checkFloatWindow检测视图状态
    private var findNoHandlerTime = 0L

    // 通话引擎实例
    private var mRtcEngine: RtcEngine? = null

    // 通话视图实例
    private var mRtcEngineHandler: RtcEngineHandler? = null

    // 定时器
    private var mDisposable: Disposable? = null

    private val mSensorManager by lazy { BaseApp.app.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    private val mPowerManager by lazy { BaseApp.app.getSystemService(Context.POWER_SERVICE) as PowerManager }

    private val mWakeLock by lazy { mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "wakeLock") }

    private var mHasFloatPremission: Boolean? = null

    fun initEngine(baseContext: Context, openType: Int, streamType: Int, myUid: Long, targetUid: Long): Boolean {
        if (mRtcEngine != null) {
            // 已创建实例
            mHasFloatPremission = null
            return true
        } else {
            try {
                mineUid = myUid
                RtcEngineHolder.targetUid = targetUid
                RtcEngineHolder.openType = openType
                RtcEngineHolder.streamType = streamType
                isJoinAudio = false
                isJoinVideo = false
                isMute = false
                isSpeaker = false

                reqTime = System.currentTimeMillis()
                joinChannelTime = 0L
                callStartTime = 0L

                findNoHandlerTime = 0L

                checkFloatWindow = false
                targetUserInfo = null
                hasUserJoined = false

                val context = baseContext.applicationContext
                val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
                val agoraAppId = if (TextUtils.isEmpty(accountInfo.getAgoraAppId())) "59fd3240e2b141eeb45f5944925eebe8" else accountInfo.getAgoraAppId()
                mRtcEngine = RtcEngine.create(context, agoraAppId, object : IRtcEngineEventHandler() {

                    override fun onWarning(warn: Int) {
                        super.onWarning(warn)
                    }

                    override fun onError(err: Int) {
                        super.onError(err)
                        if (isActive() && err != 120 && err != 110) {//120: 解密失败 ，110：token 失效 ； 不关闭聊天，重试连接
                            endCall()
                        }
                    }

                    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                        super.onJoinChannelSuccess(channel, uid, elapsed)
                        ThreadUtils.runOnUIThread {
                            if (RtcEngineHolder.streamType == 0) {
                                RtcEngineHolder.isJoinAudio = true
                                RtcEngineHolder.mRtcEngineHandler?.joinAudio()
                            } else {
                                RtcEngineHolder.isJoinVideo = true
                                RtcEngineHolder.mRtcEngineHandler?.joinVideo()
                            }

                            RtcEngineHolder.joinChannelTime = System.currentTimeMillis()
                            RtcEngineHolder.mRtcEngineHandler?.waitOtherJoin()
                        }
                    }

                    override fun onUserJoined(uid: Int, elapsed: Int) {
                        super.onUserJoined(uid, elapsed)
                        ThreadUtils.runOnUIThread {
                            RtcEngineHolder.hasUserJoined = true
                            resetCallStartTime(RtcEngineHolder.mineUid, RtcEngineHolder.targetUid, RtcEngineHolder.currentChannelName, RtcEngineHolder.streamType)
                            mRtcEngineHandler?.otherJoined()
                        }
                    }

                    override fun onUserOffline(uid: Int, reason: Int) {
                        super.onUserOffline(uid, reason)
                        if (isActive()) {
                            endCall()
                        }
                    }

                    override fun onTokenPrivilegeWillExpire(token: String?) {
                        super.onTokenPrivilegeWillExpire(token)
                        SendMessageManager.sendRefreshTokenPackage(RtcEngineHolder.currentChannelName)
                    }

                    override fun onRequestToken() {
                        super.onRequestToken()
                        SendMessageManager.sendRefreshTokenPackage(RtcEngineHolder.currentChannelName)
                    }

                    override fun onUserEnableVideo(uid: Int, enabled: Boolean) {
                        super.onUserEnableVideo(uid, enabled)
                        if (!enabled) {
                            switchToAudio()
                        }
                    }

                    private var myBadQualityCount = 0
                    private var targetBadQualityCount = 0

                    override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
                        super.onNetworkQuality(uid, txQuality, rxQuality)

                        Log.d("demo", "myUid=${myUid} uid=${uid} txQuality=${txQuality} rxQuality=${rxQuality}")

                        val tx = checkQuality(txQuality)
                        val rx = checkQuality(rxQuality)
                        if (tx < 0 || rx < 0) {
                            // 质量差了
                            if (uid == 0) {
                                myBadQualityCount++
                                Log.d("demo", "myBadQualityCount=${myBadQualityCount}")
                            } else {
                                targetBadQualityCount++
                                Log.d("demo", "targetBadQualityCount=${targetBadQualityCount}")
                            }

                            ThreadUtils.runOnUIThread {
                                when {
                                    myBadQualityCount >= 3 -> {
                                        Log.d("demo", "myBadQualityCount>3 -->${myBadQualityCount}")
                                        mRtcEngineHandler?.networkStatusChange(true, myBadQualityCount >= 3)
                                    }
                                    targetBadQualityCount >= 3 -> {
                                        Log.d("demo", "targetBadQualityCount>3 -->${targetBadQualityCount}")
                                        mRtcEngineHandler?.networkStatusChange(false, targetBadQualityCount >= 3)
                                    }
                                }
                            }
                        } else if (tx > 0 || rx > 0) {
                            // 质量好了
                            if (uid == 0) {
                                myBadQualityCount = 0
                                Log.d("demo", "myBadQualityCount=${myBadQualityCount}")
                            } else {
                                targetBadQualityCount = 0
                                Log.d("demo", "targetBadQualityCount=${targetBadQualityCount}")
                            }

                            ThreadUtils.runOnUIThread {
                                mRtcEngineHandler?.networkStatusChange(isMe = false, isBad = false)
                            }
                        } else {
                            // 探测中
                        }
                    }

                    private fun checkQuality(quality: Int): Int {
                        return if (quality == QUALITY_BAD || quality == QUALITY_VBAD || quality == QUALITY_DOWN) {
                            -1
                        } else if (quality == QUALITY_EXCELLENT || quality == QUALITY_GOOD || quality == QUALITY_POOR) {
                            1
                        } else {
                            0
                        }
                    }
                })

                // 高音质
                mRtcEngine?.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO, Constants.AUDIO_SCENARIO_SHOWROOM)

                // 检测当前设备是否支持自动曝光并设置
                val shouldSetExposure = mRtcEngine?.isCameraExposurePositionSupported ?: false
                if (shouldSetExposure) {
                    // 假设在屏幕(50，100)的位置曝光
                    val positionX = 50.0f
                    val positionY = 100.0f
                    mRtcEngine?.setCameraExposurePosition(positionX, positionY)
                }

                // 检测当前设备是否支持人脸自动对焦并设置
                val shouldSetFaceMode = mRtcEngine?.isCameraAutoFocusFaceModeSupported ?: false
                mRtcEngine?.setCameraAutoFocusFaceModeEnabled(shouldSetFaceMode)

                // 检测当前设备是否支持手动对焦并设置
                val shouldManualFocus = mRtcEngine?.isCameraFocusSupported ?: false
                if (shouldManualFocus) {
                    // 假设在屏幕(50，100)的位置对焦
                    val positionX = 50.0f
                    val positionY = 100.0f
                    mRtcEngine?.setCameraFocusPositionInPreview(positionX, positionY)
                }

                val orientationMode = VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
                val dimensions = VideoEncoderConfiguration.VideoDimensions(720, 960)
                val frameRate = VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15
                val bitrate = VideoEncoderConfiguration.STANDARD_BITRATE
                val videoEncoderConfiguration = VideoEncoderConfiguration(dimensions, frameRate, bitrate, orientationMode)

                mRtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
                mRtcEngine?.setVideoEncoderConfiguration(videoEncoderConfiguration)

                mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL)

                restartTimer()
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                release()
                reset()
                return false
            }
        }
    }

    /**
     * 定时检查通话状态
     */
    private fun restartTimer() {
        cancelTimer()
        Observable.interval(500, 500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Long> {
                    override fun onSubscribe(disposable: Disposable) {
                        mDisposable = disposable
                    }

                    @SuppressLint("CheckResult")
                    override fun onNext(number: Long) {
                        if (isActive()) {
                            if (isJoinAudio || isJoinVideo) {
                                // 建立连接中/正常通话中...

                                val calledTime = System.currentTimeMillis() - joinChannelTime
                                if (!hasUserJoined) {
                                    // 对方用户没有加入,建立连接中...
                                    if (calledTime > CALLED_TIME_OUT) {
                                        // 超时，状态改为已取消
                                        StreamCallController.updateCallStreamStatus(mineUid, targetUid, currentChannelName, 3)

                                        //没有用户加入，超时
                                        endCall()

                                        ThreadUtils.runOnUIThread {
                                            BaseApp.app.toast(BaseApp.app.getString(R.string.did_not_answer))
                                        }
                                    } else {
                                        // 未超时
                                    }
                                } else {
                                    // 对方用户已加入,正常通话中...
                                    // 更新通话记录的时间
                                    StreamCallController.updateCallEndTime(mineUid, targetUid, currentChannelName, false, ArouterServiceManager.messageService.getCurrentTime())

                                    if (mRtcEngineHandler != null) {
                                        // 有视图
                                        findNoHandlerTime = 0L
                                        mRtcEngineHandler?.tick(calledTime)
                                    } else {
                                        // 无视图
                                        checkHandlerWindow()
                                    }
                                }
                            } else {
                                // 呼叫中...
                                // 检测超时
                                val requestedTime = System.currentTimeMillis() - reqTime
                                if (requestedTime > CALLING_TIME_OUT) {
                                    // 用户没有响应，超时

                                    if (openType == 0) {
                                        //主叫者需发送取消请求（被叫在消息中插入一条未接听的记录）
//                                        SendMessageManager.sendCancelStreamRequestPackage(currentChannelName, openType, mineUid, targetUid, if (streamType == 0) IMPB.StreamType.streamAudio else IMPB.StreamType.streamVideo)
                                    }

                                    ThreadUtils.runOnUIThread {
                                        BaseApp.app.toast(BaseApp.app.getString(R.string.no_answer))
                                    }

                                    // 关闭通话
                                    endCall()
                                } else {
                                    // 有视图
                                    if (mRtcEngineHandler != null) {
                                        findNoHandlerTime = 0L
                                    } else {
                                        // 无视图
                                        checkHandlerWindow()
                                    }
                                }
                            }
                        } else {
                            cancelTimer()
                        }
                    }

                    override fun onError(e: Throwable) {
                    }

                    override fun onComplete() {
                    }
                })
    }

    private fun checkHandlerWindow() {
        val tag = "stream_call_float_view"
        if (FloatWindow.get(tag) == null && !ActivitiesHelper.getInstance().hasActivity(TelephoneActivity::class.java)) {
            when {
                findNoHandlerTime == 0L -> {
                    //记录第一次发现没有任何视图的时间
                    findNoHandlerTime = System.currentTimeMillis()
                    checkFloatWindow(tag)
                }
                findNoHandlerTime > 0L && System.currentTimeMillis() - findNoHandlerTime > 5000 -> {
                    //超过5秒没有任何视图显示则结束通话
                    endCall()
                }
                else -> {
                    checkFloatWindow(tag)
                }
            }
        }
    }

    private fun checkFloatWindow(tag: String) {
        if (mHasFloatPremission == false) {
            showActivityWindow()

            ThreadUtils.runOnUIThread {
                BaseApp.app.toast(BaseApp.app.getString(R.string.no_nover_window_permissions))
            }
        } else if (checkFloatWindow) {
            //弹出FloatWindow
            val width = ScreenUtils.dp2px(BaseApp.app, if (streamType == 1 && hasUserJoined) 120.0f else 90.0f)
            val height = ScreenUtils.dp2px(BaseApp.app, if (streamType == 1 && hasUserJoined) 160.0f else 160.0f)
            val telephoneMiniView = TelephoneMiniView(BaseApp.app)
            telephoneMiniView.setFloatWindowKey(tag)
            telephoneMiniView.setTargetUserInfo(targetUserInfo)
            FloatWindow.with(BaseApp.app).setTag(tag)
                    .setView(telephoneMiniView)
                    .setWidth(width)
                    .setHeight(height)
                    .setDesktopShow(true)
                    .setFilter(true, BaseActivity::class.java)
                    .setX(0)
                    .setY(0)
                    .setPermissionListener(object : PermissionListener {
                        override fun onSuccess() {
                            mHasFloatPremission = true
                        }

                        override fun onFail() {
                            mHasFloatPremission = false
                            FloatWindow.destroy(tag)
                        }
                    })
                    .build()

            ThreadUtils.runOnUIThread(300) {
                FloatWindow.get(tag)?.show()
            }
        }
    }

    private fun showActivityWindow() {
        ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO)
                .withBoolean("showLastCall", true)
                .navigation()
    }

    private fun cancelTimer() {
        if (mDisposable?.isDisposed == false) {
            mDisposable?.dispose()
            mDisposable = null
        }
    }

    /**
     * RTCEngine是否活跃中
     */
    fun isActive(): Boolean {
        return mRtcEngine != null
    }

    /**
     * 刷新token
     */
    fun renewToken(token: String) {
        mRtcEngine?.renewToken(token)
    }

    /**
     * 加入语音流
     */
    fun joinAudioStream(token: String, channleName: String, secretKey: String) {
        SoundPoolManager.stopPlayStreamCalling()
        SoundPoolManager.stopPlayStreamCallWaiting()
        SoundPoolManager.stopVibrator()

        streamType = 0

        mRtcEngine?.stopPreview()
        mRtcEngine?.disableVideo()
        mRtcEngine?.enableAudio()

        if (TextUtils.isEmpty(secretKey)) {
            mRtcEngine?.joinChannel(token, channleName, "Extra Optional Data", mineUid.toInt())
        } else {
            ArouterServiceManager.systemService.getUserSecretKey(targetUid, appVer = 0, complete = { sk, _, _, _ ->
                val key = AESHelper.decrypt(HexString.hexToBuffer(secretKey), sk)
                mRtcEngine?.setEncryptionSecret(key)
                mRtcEngine?.setEncryptionMode("aes-128-xts")
                mRtcEngine?.joinChannel(token, channleName, "Extra Optional Data", mineUid.toInt())
            }, error = {
                endCall()
            })
        }
    }

    /**
     * 加入语音视频流
     */
    fun joinVideoStream(token: String, channleName: String, secretKey: String) {
        SoundPoolManager.stopPlayStreamCalling()
        SoundPoolManager.stopPlayStreamCallWaiting()
        SoundPoolManager.stopVibrator()

        streamType = 1

        mRtcEngine?.enableVideo()
        mRtcEngine?.enableAudio()

        if (TextUtils.isEmpty(secretKey)) {
            mRtcEngine?.joinChannel(token, channleName, "Extra Optional Data", mineUid.toInt())
        } else {
            ArouterServiceManager.systemService.getUserSecretKey(targetUid, appVer = 0, complete = { sk, _, _, _ ->
                val key = AESHelper.decrypt(HexString.hexToBuffer(secretKey), sk)
                mRtcEngine?.setEncryptionSecret(key)
                mRtcEngine?.setEncryptionMode("aes-128-xts")
                mRtcEngine?.joinChannel(token, channleName, "Extra Optional Data", mineUid.toInt())
            }, error = {
                endCall()
            })
        }
    }

    /**
     * 配置安装本地视频预览
     */
    fun setupLocalVideo(surfaceViewContainer: ViewGroup, uid: Int) {
        val surfaceView = RtcEngine.CreateRendererView(surfaceViewContainer.context)
        surfaceView.setZOrderMediaOverlay(true)
        surfaceViewContainer.addView(surfaceView)
        mRtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
        mRtcEngine?.enableVideo()
        mRtcEngine?.enableLocalVideo(true)
        mRtcEngine?.startPreview()
    }

    /**
     * 配置安装远程视频预览
     */
    fun setupRemoteVideo(surfaceViewContainer: ViewGroup, uid: Int) {
        val surfaceView = RtcEngine.CreateRendererView(surfaceViewContainer.context)
        surfaceViewContainer.addView(surfaceView)
        mRtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
        mRtcEngine?.enableVideo()
    }

    /**
     * 停止本地视频预览
     */
    fun stopLocalPreview() {
        mRtcEngine?.stopPreview()
    }

    /**
     * 本地音频是否静音（对方是否能听见自己的声音）
     */
    fun muteLocalAudioStream(muted: Boolean) {
        mRtcEngine?.muteLocalAudioStream(muted)
        isMute = muted
    }

    /**
     * 是否开启扬声器
     */
    fun setEnableSpeakerphone(enabled: Boolean) {
        mRtcEngine?.setEnableSpeakerphone(enabled)
        isSpeaker = enabled
    }

    /**
     * 切换摄像头
     */
    fun switchCamera() {
        mRtcEngine?.switchCamera()
    }

    /**
     * 切换至语音
     */
    fun switchToAudio() {
        ThreadUtils.runOnUIThread {
            streamType = 0
            isJoinAudio = true
            isJoinVideo = false
            mRtcEngine?.stopPreview()
            mRtcEngine?.disableVideo()
            mRtcEngineHandler?.switchToAudio()
        }
    }

    fun endCall() {
        Log.d("demo", "endCall--->")
        StreamCallController.overStreamCallReq(openType)
        overCall()
    }

    fun cancelCall() {
        Log.d("demo", "cancelCall--->")
        StreamCallController.cancelStreamCallReq(if (streamType == 0) IMPB.StreamType.streamAudio else IMPB.StreamType.streamVideo)
        overCall()
    }

    /**
     * 结束本次通话
     */
    private fun overCall() {
        if (release()) {
            Log.d("demo", "playStreamCallEnd--->")
            SoundPoolManager.playStreamCallEnd()
        }

        ThreadUtils.runOnUIThread {
            mRtcEngineHandler?.endCall()
        }

        SoundPoolManager.stopPlayStreamCalling()
        SoundPoolManager.stopPlayStreamCallWaiting()
        SoundPoolManager.stopVibrator()

        if ((isJoinAudio || isJoinVideo) && hasUserJoined) {
            // 更新流媒体通话记录的状态
            StreamCallController.updateCallEndTime(mineUid, targetUid, currentChannelName, true, ArouterServiceManager.messageService.getCurrentTime())
        }

        // 重置参数
        reset()
    }

    /**
     * 重置本次通话开始时间和状态
     */
    private fun resetCallStartTime(myUid: Long, targetUid: Long, channelName: String, streamType: Int) {
        callStartTime = System.currentTimeMillis()
        StreamCallController.updateCallStartTime(myUid, targetUid, channelName, streamType, callStartTime + ReceiveMessageManager.serverDifferenceTime)
    }

    /**
     * 释放资源
     */
    private fun release(): Boolean {
        val hasRtcInstance = mRtcEngine != null

        val rtcEngine = mRtcEngine
        mRtcEngine = null

        ThreadUtils.runOnIOThread {
            rtcEngine?.leaveChannel()
            RtcEngine.destroy()
        }

        cancelTimer()

        mSensorManager.unregisterListener(this)

        return hasRtcInstance
    }

    /**
     * 重置本地参数
     */
    private fun reset() {
        mineUid = 0
        targetUid = 0
        streamType = 0
        openType = 0
        isJoinAudio = false
        isJoinVideo = false
        isMute = false
        isSpeaker = false
        findNoHandlerTime = 0L
        reqTime = 0L
        joinChannelTime = 0L
        callStartTime = 0L
        checkFloatWindow = false
        targetUserInfo = null
        hasUserJoined = false
    }

    fun attachTargetUserInfo(contactModel: ContactDataModel) {
        targetUserInfo = contactModel
    }

    fun unAttachRtcEngineHandler() {
        mRtcEngineHandler = null
    }

    fun attachRtcEngineHandler(handler: RtcEngineHandler) {
        mRtcEngineHandler = handler
    }

    fun getAttachedRtcEngineHandler(): RtcEngineHandler? {
        return mRtcEngineHandler
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val range = event?.values?.get(0) ?: 0f
        if (range >= event?.sensor?.maximumRange ?: 0f) {
            if (mWakeLock.isHeld) {
                mWakeLock.release()
            }
        } else {
            if (!mWakeLock.isHeld) {
                mWakeLock.acquire()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    class PendingCall(val targetUid: Long, val streamType: Int, val openType: Int, val time: Long)

    interface RtcEngineHandler {
        fun switchToAudio()

        fun joinAudio()

        fun joinVideo()

        fun waitOtherJoin()

        fun otherJoined()

        fun tick(time: Long)

        fun networkStatusChange(isMe: Boolean, isBad: Boolean)

        fun endCall()
    }
}