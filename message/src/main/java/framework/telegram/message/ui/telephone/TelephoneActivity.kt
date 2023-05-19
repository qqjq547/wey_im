package framework.telegram.message.ui.telephone

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.alibaba.android.arouter.facade.annotation.Route
import com.im.pb.IMPB
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.controller.StreamCallController
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.manager.SoundPoolManager
import framework.telegram.message.ui.group.GroupChatActivity
import framework.telegram.message.ui.telephone.core.RtcEngineHolder
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.AESHelper
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.TimeUtils
import framework.telegram.ui.cameraview.activity.CameraActivity
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView
import framework.telegram.ui.tools.FrescoUtils
import framework.telegram.ui.utils.ScreenUtils.dp2px
import framework.telegram.ui.videoplayer.utils.NetworkUtils
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

@Route(path = Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO)
class TelephoneActivity : BaseActivity(), View.OnClickListener, RtcEngineHolder.RtcEngineHandler {
    private val mAccountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    private val mMineUid by lazy { mAccountInfo.getUserId() }

    private val isShowLastCall by lazy { intent.getBooleanExtra("showLastCall", false) }

    private var mTargetUserContactInfo: ContactDataModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RtcEngineHolder.pendingCall = null

        val streamType: Int
        val openType: Int
        val targetUid: Long
        if (isShowLastCall) {
            if (RtcEngineHolder.isActive() && RtcEngineHolder.mineUid == mMineUid) {
                targetUid = RtcEngineHolder.targetUid
                streamType = RtcEngineHolder.streamType
                openType = RtcEngineHolder.openType
            } else {
                RtcEngineHolder.unAttachRtcEngineHandler()
                RtcEngineHolder.endCall()
                finish()
                return
            }
        } else if (RtcEngineHolder.isActive()) {
            toast(getString(R.string.be_on_the_phone))
            val rtcEngineHandler = RtcEngineHolder.getAttachedRtcEngineHandler()
            finish()//finish会清除掉之前的handler
            rtcEngineHandler?.let {
                RtcEngineHolder.attachRtcEngineHandler(rtcEngineHandler)
            }
            return
        } else {
            if (!NetworkUtils.isAvailable(BaseApp.app)) {
                toast(getString(R.string.please_check_your_network_settings))
                RtcEngineHolder.endCall()
                finish()
                return
            }

            targetUid = intent.getLongExtra("targetUid", 0)
            streamType = intent.getIntExtra("streamType", 1)
            openType = intent.getIntExtra("openType", 0)
        }

        if (mMineUid <= 0 || targetUid <= 0) {
            Toast.makeText(applicationContext, getString(R.string.uid_error_two), Toast.LENGTH_SHORT).show()
            RtcEngineHolder.unAttachRtcEngineHandler()
            RtcEngineHolder.endCall()
            finish()
            return
        }

        setContentView(R.layout.msg_activity_telephone)
        overridePendingTransition(R.anim.anim_down_in, 0)

        findViewById<ImageView>(R.id.image_view_float_view).visibility = View.INVISIBLE
        findViewById<View>(R.id.layout_operate).visibility = View.INVISIBLE
        findViewById<View>(R.id.layout_switch_to_audio).visibility = View.INVISIBLE
        findViewById<View>(R.id.text_view_time).visibility = View.INVISIBLE
        findViewById<View>(R.id.app_image_view_icon_calling).visibility = View.GONE
        findViewById<View>(R.id.app_text_view_nickname_calling).visibility = View.GONE

        if (RtcEngineHolder.initEngine(this@TelephoneActivity, openType, streamType, mMineUid, targetUid)) {
            RtcEngineHolder.attachRtcEngineHandler(this@TelephoneActivity)
        } else {
            finish()
            return
        }

        getTargetUserInfo(targetUid)

        if (ActivitiesHelper.getInstance().hasActivity(CameraActivity::class.java)) {
            ActivitiesHelper.getInstance().closeTarget(CameraActivity::class.java)
        }
    }

    private fun getTargetUserInfo(targetUid: Long) {
        ArouterServiceManager.contactService.getContactInfo(lifecycle(), targetUid, { contactInfo, _ ->
            mTargetUserContactInfo = contactInfo
            RtcEngineHolder.attachTargetUserInfo(contactInfo)
            showUI()
        }, {
            showUI()
        })
    }

    private fun showUI() {
        when {
            isShowLastCall && RtcEngineHolder.isJoinAudio -> {
                //正在语音
                showUI(true)
                setOperateUI()
                joinAudio()
            }
            isShowLastCall && RtcEngineHolder.isJoinVideo -> {
                //正在视频
                showUI(true)
                setOperateUI()
                joinVideo()
            }
            else -> {
                //新通话
                showUI(false)
            }
        }
    }

    private fun setOperateUI() {
        setAudioMuteUI(findViewById(R.id.image_view_switch_mute), !RtcEngineHolder.isMute)
        setSpeakerphoneUI(findViewById(R.id.image_view_switch_speaker), !RtcEngineHolder.isSpeaker)
    }

    private fun showUI(resetUI: Boolean) {
        if (!RtcEngineHolder.hasUserJoined) {
            findViewById<TextView>(R.id.text_view_time).text = getString(R.string.in_connection)
        } else {
            findViewById<TextView>(R.id.text_view_time).text = String.format(getString(R.string.second_mat), ((System.currentTimeMillis() - RtcEngineHolder.joinChannelTime) / 1000).toString())
            findViewById<View>(R.id.text_view_time).visibility = View.INVISIBLE
        }

        if (RtcEngineHolder.openType == 0) {
            if (RtcEngineHolder.streamType == 0) {
                showRequestAudioCallUI()
                if (!resetUI) {
                    requestAudioCall()
                    SoundPoolManager.playStreamCallWaiting()
                } else {
                    findViewById<ImageView>(R.id.image_view_float_view).setImageResource(R.drawable.msg_audio_float_view)
                    findViewById<ImageView>(R.id.image_view_float_view).setOnClickListener(this@TelephoneActivity)
                    findViewById<ImageView>(R.id.image_view_float_view).visibility = View.VISIBLE
                }
            } else {
                showRequestVideoCallUI()
                if (!resetUI) {
                    requestVideoCall()
                    SoundPoolManager.playStreamCallWaiting()
                }
            }
        } else {
            if (RtcEngineHolder.streamType == 0) {
                showRespAudioCallUI()
                if (!resetUI) {
                    playSoundAndVibrator(mAccountInfo.getPrivacy())
                }
            } else {
                showRespVideoCallUI()
                if (!resetUI) {
                    playSoundAndVibrator(mAccountInfo.getPrivacy())
                }
            }
        }
    }

    private fun showRequestAudioCallUI() {
        LayoutInflater.from(this@TelephoneActivity).inflate(R.layout.msg_layout_request_audio_call, findViewById(R.id.layout_request_resp))
        findViewById<AppImageView>(R.id.app_image_view_icon).setImageURI(mTargetUserContactInfo?.icon)
        FrescoUtils.showUrlBlur(findViewById<AppImageView>(R.id.app_image_view_icon_2), mTargetUserContactInfo?.icon)
        FrescoUtils.showUrlBlur(findViewById<AppImageView>(R.id.app_image_view_icon_3), mTargetUserContactInfo?.icon)
        findViewById<AppTextView>(R.id.app_text_view_nickname).text = mTargetUserContactInfo?.displayName

        findViewById<View>(R.id.audio_icon_bg).visibility = View.GONE
        findViewById<ImageView>(R.id.image_view_enc_cancel_call).setOnClickListener {
            //取消通话请求
            cancelStreamCallReq(IMPB.StreamType.streamAudio)
            SoundPoolManager.stopPlayStreamCalling()
            SoundPoolManager.stopPlayStreamCallWaiting()
            SoundPoolManager.stopVibrator()
            RtcEngineHolder.endCall()
        }
    }

    private fun showRespAudioCallUI() {
        LayoutInflater.from(this@TelephoneActivity).inflate(R.layout.msg_layout_resp_audio_call, findViewById(R.id.layout_request_resp))
        findViewById<AppImageView>(R.id.app_image_view_icon).setImageURI(mTargetUserContactInfo?.icon)
        FrescoUtils.showUrlBlur(findViewById<AppImageView>(R.id.app_image_view_icon_2), mTargetUserContactInfo?.icon)
        FrescoUtils.showUrlBlur(findViewById<AppImageView>(R.id.app_image_view_icon_3), mTargetUserContactInfo?.icon)
        findViewById<AppTextView>(R.id.app_text_view_nickname).text = mTargetUserContactInfo?.displayName

        findViewById<ImageView>(R.id.image_view_float_view).setImageResource(R.drawable.msg_audio_float_view)
        findViewById<ImageView>(R.id.image_view_float_view).setOnClickListener(this@TelephoneActivity)
        findViewById<ImageView>(R.id.image_view_float_view).visibility = View.VISIBLE

        findViewById<View>(R.id.audio_icon_bg).visibility = View.GONE
        findViewById<ImageView>(R.id.image_view_enc_refuse_call).setOnClickListener {
            //拒绝通话请求
            refuseStreamCallReq(IMPB.StreamType.streamAudio)
            SoundPoolManager.stopPlayStreamCalling()
            SoundPoolManager.stopPlayStreamCallWaiting()
            RtcEngineHolder.endCall()
        }
        findViewById<ImageView>(R.id.image_view_enc_agree_call).setOnClickListener {
            //同意通话请求
            agreeStreamCallReq(IMPB.StreamType.streamAudio)
            SoundPoolManager.stopPlayStreamCalling()
            SoundPoolManager.stopPlayStreamCallWaiting()
            SoundPoolManager.stopVibrator()
        }
    }

    private fun showRequestVideoCallUI() {
        LayoutInflater.from(this@TelephoneActivity).inflate(R.layout.msg_layout_request_video_call, findViewById(R.id.layout_request_resp))
        findViewById<AppImageView>(R.id.app_image_view_icon).setImageURI(mTargetUserContactInfo?.icon)
        findViewById<AppTextView>(R.id.app_text_view_nickname).text = mTargetUserContactInfo?.displayName

        findViewById<View>(R.id.audio_icon_bg).visibility = View.GONE
        findViewById<ImageView>(R.id.image_view_enc_cancel_call).setOnClickListener {
            //取消通话请求
            SoundPoolManager.stopPlayStreamCalling()
            SoundPoolManager.stopPlayStreamCallWaiting()
            SoundPoolManager.stopVibrator()
            cancelStreamCallReq(IMPB.StreamType.streamVideo)
            RtcEngineHolder.endCall()
        }

        // 打开本地预览
        val localContainer = findViewById<FrameLayout>(R.id.local_video_view_container)
        localContainer.removeAllViews()
        RtcEngineHolder.setupLocalVideo(localContainer, RtcEngineHolder.mineUid.toInt())
    }

    private fun showRespVideoCallUI() {
        LayoutInflater.from(this@TelephoneActivity).inflate(R.layout.msg_layout_resp_video_call, findViewById(R.id.layout_request_resp))
        findViewById<AppImageView>(R.id.app_image_view_icon).setImageURI(mTargetUserContactInfo?.icon)
        findViewById<AppTextView>(R.id.app_text_view_nickname).text = mTargetUserContactInfo?.displayName

        findViewById<ImageView>(R.id.image_view_float_view).setImageResource(R.drawable.msg_audio_float_view)
        findViewById<ImageView>(R.id.image_view_float_view).setOnClickListener(this@TelephoneActivity)
        findViewById<ImageView>(R.id.image_view_float_view).visibility = View.VISIBLE

        findViewById<View>(R.id.audio_icon_bg).visibility = View.GONE
        findViewById<ImageView>(R.id.image_view_enc_refuse_call).setOnClickListener {
            //拒绝通话请求
            SoundPoolManager.stopPlayStreamCalling()
            SoundPoolManager.stopPlayStreamCallWaiting()
            refuseStreamCallReq(IMPB.StreamType.streamVideo)
            RtcEngineHolder.endCall()
        }
        findViewById<ImageView>(R.id.image_view_enc_agree_call).setOnClickListener {
            //同意通话请求
            SoundPoolManager.stopPlayStreamCalling()
            SoundPoolManager.stopPlayStreamCallWaiting()
            agreeStreamCallReq(IMPB.StreamType.streamVideo)
        }
        findViewById<ImageView>(R.id.image_view_audio_agree).setOnClickListener {
            //语音接听
            SoundPoolManager.stopPlayStreamCalling()
            SoundPoolManager.stopPlayStreamCallWaiting()
            SoundPoolManager.stopVibrator()
            agreeStreamCallReq(IMPB.StreamType.streamAudio)
        }

        // 打开本地预览
        val localContainer = findViewById<FrameLayout>(R.id.local_video_view_container)
        localContainer.removeAllViews()
        RtcEngineHolder.setupLocalVideo(localContainer, RtcEngineHolder.mineUid.toInt())
    }

    /**
     * 取消通话请求
     */
    private fun cancelStreamCallReq(streamType: IMPB.StreamType) {
        //取消通话请求
        StreamCallController.cancelStreamCallReq(streamType)
        finish()
    }

    /**
     * 拒绝通话请求
     */
    private fun refuseStreamCallReq(streamType: IMPB.StreamType) {
        StreamCallController.refuseStreamCallReq(streamType)
        finish()
    }

    /**
     * 同意通话请求
     */
    private fun agreeStreamCallReq(streamType: IMPB.StreamType) {
        StreamCallController.agreeStreamCallReq(streamType)
    }

    /**
     * 发起语音通话请求
     */
    @SuppressLint("CheckResult")
    private fun requestAudioCall() {
        //开始发送socket
        StreamCallController.requestAudioCall(AESHelper.generatePassword(16))
    }

    /**
     * 发起视频通话请求
     */
    @SuppressLint("CheckResult")
    private fun requestVideoCall() {
        //开始发送socket
        StreamCallController.requestVideoCall(AESHelper.generatePassword(16))
    }

    private fun onLocalAudioMuteClicked(view: View) {
        val iv = view as ImageView
        setAudioMuteUI(view, iv.isSelected)
        RtcEngineHolder.muteLocalAudioStream(iv.isSelected)
    }

    private fun onSwitchSpeakerphoneClicked(view: View) {
        val iv = view as ImageView
        setSpeakerphoneUI(view, iv.isSelected)
        RtcEngineHolder.setEnableSpeakerphone(view.isSelected())
    }

    private fun setAudioMuteUI(view: View, enable: Boolean) {
        val iv = view as ImageView
        if (enable) {
            iv.isSelected = false
            iv.setImageResource(R.drawable.msg_icon_tele_mute_off)
        } else {
            iv.isSelected = true
            iv.setImageResource(R.drawable.msg_icon_tele_mute_on)
        }
    }

    private fun setSpeakerphoneUI(view: View, enable: Boolean) {
        val iv = view as ImageView
        if (enable) {
            iv.isSelected = false
            iv.setImageResource(R.drawable.msg_icon_tele_speaker_off)
        } else {
            iv.isSelected = true
            iv.setImageResource(R.drawable.msg_icon_tele_speaker_on)
        }
    }

    private fun playSoundAndVibrator(privacy: Int) {
        if (ArouterServiceManager.settingService.getVoiceStatus(privacy, true)) // 声音
            SoundPoolManager.playStreamCalling()
        if (ArouterServiceManager.settingService.getVibrationStatus(privacy, true)) // 震动
            SoundPoolManager.vibratorRepeat()
    }

    private fun onSwitchCameraClicked(view: View) {
        RtcEngineHolder.switchCamera()
    }

    private fun onSwitchToAudioClicked(view: View) {
        RtcEngineHolder.switchToAudio()
    }

    @SuppressLint("CheckResult")
    private fun onFloatClicked(v: View) {
        // 停止本地预览
        RtcEngineHolder.stopLocalPreview()
        // 关闭页面
        finish()

        Observable.timer(200, TimeUnit.MILLISECONDS).subscribe {
            RtcEngineHolder.checkFloatWindow = true
        }
    }

    private fun onEncCallClicked(view: View) {
        if (RtcEngineHolder.hasUserJoined) {
            RtcEngineHolder.endCall()
        } else {
            RtcEngineHolder.cancelCall()
        }
    }

    override fun onClick(v: View?) {
        when {
            v?.id == R.id.image_view_switch_mute -> onLocalAudioMuteClicked(v)
            v?.id == R.id.image_view_switch_speaker -> onSwitchSpeakerphoneClicked(v)
            v?.id == R.id.image_view_enc_call -> onEncCallClicked(v)
            v?.id == R.id.image_view_switch_to_audio -> onSwitchToAudioClicked(v)
            v?.id == R.id.image_view_switch_camera -> onSwitchCameraClicked(v)
            v?.id == R.id.image_view_float_view -> onFloatClicked(v)
        }
    }

    override fun onBackPressed() {

    }

    override fun switchToAudio() {
        findViewById<ImageView>(R.id.image_view_float_view).setImageResource(R.drawable.msg_audio_float_view)
        findViewById<ImageView>(R.id.image_view_float_view).setOnClickListener(this@TelephoneActivity)
        findViewById<ImageView>(R.id.image_view_float_view).visibility = View.VISIBLE

        findViewById<AppImageView>(R.id.app_image_view_icon_calling).setImageURI(mTargetUserContactInfo?.icon)
        findViewById<AppTextView>(R.id.app_text_view_nickname_calling).text = mTargetUserContactInfo?.displayName
        findViewById<View>(R.id.app_image_view_icon_calling).visibility = View.VISIBLE
        findViewById<View>(R.id.app_text_view_nickname_calling).visibility = View.VISIBLE

        findViewById<ViewGroup>(R.id.remote_video_view_container).removeAllViews()
        findViewById<ViewGroup>(R.id.local_video_view_container).removeAllViews()

        findViewById<View>(R.id.layout_switch_speaker).visibility = View.VISIBLE
        findViewById<View>(R.id.layout_switch_camera).visibility = View.GONE
        findViewById<View>(R.id.layout_switch_to_audio).visibility = View.GONE
        findViewById<View>(R.id.audio_icon_bg).visibility = View.VISIBLE

        setSpeakerphoneUI(findViewById<ImageView>(R.id.image_view_switch_speaker), true)
        RtcEngineHolder.setEnableSpeakerphone(false)
    }

    override fun joinAudio() {
        findViewById<AppImageView>(R.id.app_image_view_icon_calling).setImageURI(mTargetUserContactInfo?.icon)
        findViewById<AppTextView>(R.id.app_text_view_nickname_calling).text = mTargetUserContactInfo?.displayName
        findViewById<View>(R.id.app_image_view_icon_calling).visibility = View.VISIBLE
        findViewById<View>(R.id.app_text_view_nickname_calling).visibility = View.VISIBLE

        findViewById<ViewGroup>(R.id.remote_video_view_container).removeAllViews()
        findViewById<ViewGroup>(R.id.local_video_view_container).removeAllViews()

        findViewById<ViewGroup>(R.id.layout_request_resp).removeAllViews()

        findViewById<View>(R.id.layout_operate).visibility = View.VISIBLE
        findViewById<View>(R.id.layout_switch_to_audio).visibility = View.VISIBLE
        findViewById<View>(R.id.text_view_time).visibility = View.VISIBLE

        findViewById<View>(R.id.image_view_switch_mute).setOnClickListener(this@TelephoneActivity)
        findViewById<View>(R.id.image_view_switch_speaker).setOnClickListener(this@TelephoneActivity)
        findViewById<View>(R.id.image_view_enc_call).setOnClickListener(this@TelephoneActivity)
        findViewById<View>(R.id.image_view_switch_camera).setOnClickListener(this@TelephoneActivity)
        findViewById<View>(R.id.image_view_switch_to_audio).setOnClickListener(this@TelephoneActivity)

        findViewById<View>(R.id.layout_switch_speaker).visibility = View.VISIBLE
        findViewById<View>(R.id.layout_switch_camera).visibility = View.GONE
        findViewById<View>(R.id.layout_switch_to_audio).visibility = View.GONE
        findViewById<View>(R.id.audio_icon_bg).visibility = View.VISIBLE
    }

    override fun joinVideo() {
        findViewById<View>(R.id.app_image_view_icon_calling).visibility = View.GONE
        findViewById<View>(R.id.app_text_view_nickname_calling).visibility = View.GONE

        findViewById<ViewGroup>(R.id.layout_request_resp).removeAllViews()

        findViewById<View>(R.id.layout_operate).visibility = View.VISIBLE
        findViewById<View>(R.id.layout_switch_to_audio).visibility = View.VISIBLE
        findViewById<View>(R.id.text_view_time).visibility = View.VISIBLE

        findViewById<View>(R.id.image_view_switch_mute).setOnClickListener(this@TelephoneActivity)
        findViewById<View>(R.id.image_view_switch_speaker).setOnClickListener(this@TelephoneActivity)
        findViewById<View>(R.id.image_view_enc_call).setOnClickListener(this@TelephoneActivity)
        findViewById<View>(R.id.image_view_switch_camera).setOnClickListener(this@TelephoneActivity)
        findViewById<View>(R.id.image_view_switch_to_audio).setOnClickListener(this@TelephoneActivity)

        findViewById<View>(R.id.layout_switch_speaker).visibility = View.GONE
        findViewById<View>(R.id.layout_switch_camera).visibility = View.VISIBLE
        findViewById<View>(R.id.layout_switch_to_audio).visibility = View.VISIBLE
        findViewById<View>(R.id.audio_icon_bg).visibility = View.GONE

        val localContainer = findViewById<FrameLayout>(R.id.local_video_view_container)
        val layoutParams = localContainer.layoutParams as RelativeLayout.LayoutParams
        layoutParams.width = dp2px(this@TelephoneActivity, 120f)
        layoutParams.height = dp2px(this@TelephoneActivity, 160f)
        layoutParams.leftMargin = dp2px(this@TelephoneActivity, 12f)
        layoutParams.topMargin = dp2px(this@TelephoneActivity, 36f)
        localContainer.layoutParams = layoutParams

        // 打开远程预览
        val container = findViewById<FrameLayout>(R.id.remote_video_view_container)
        container.removeAllViews()
        RtcEngineHolder.setupRemoteVideo(container, RtcEngineHolder.targetUid.toInt())
    }

    override fun waitOtherJoin() {
        findViewById<TextView>(R.id.text_view_time).text = getString(R.string.in_connection_sign)
    }

    override fun otherJoined() {
        if (RtcEngineHolder.streamType == 0) {
            findViewById<ImageView>(R.id.image_view_float_view).setImageResource(R.drawable.msg_audio_float_view)
            findViewById<ImageView>(R.id.image_view_float_view).setOnClickListener(this@TelephoneActivity)
            findViewById<ImageView>(R.id.image_view_float_view).visibility = View.VISIBLE
        } else {
            findViewById<ImageView>(R.id.image_view_float_view).setImageResource(R.drawable.msg_audio_float_view)
            findViewById<ImageView>(R.id.image_view_float_view).setOnClickListener(this@TelephoneActivity)
            findViewById<ImageView>(R.id.image_view_float_view).visibility = View.VISIBLE
        }
    }

    override fun tick(time: Long) {
        findViewById<TextView>(R.id.text_view_time).text = "${TimeUtils.timeFormatToMediaDuration(System.currentTimeMillis() - RtcEngineHolder.joinChannelTime)}"
    }

    override fun networkStatusChange(isMe: Boolean, isBad: Boolean) {
        findViewById<TextView>(R.id.text_view_network_status).visibility = if (isBad) View.VISIBLE else View.GONE
        if (isMe) {
            findViewById<TextView>(R.id.text_view_network_status).text = getString(R.string.calling_network_status_low_my_network_is_bad)
        } else {
            findViewById<TextView>(R.id.text_view_network_status).text = getString(R.string.calling_network_status_low_target_network_is_bad)
        }
    }

    override fun endCall() {
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.anim_up_out)
        RtcEngineHolder.unAttachRtcEngineHandler()
    }

    override fun onResume() {
        super.onResume()

        // get permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE), GroupChatActivity.GET_PERMISSIONS_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requestCode == GroupChatActivity.GET_PERMISSIONS_REQUEST_CODE) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, getString(R.string.no_permissions_were_obtained), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun isPortraitScreen(): Boolean = false //这里不设置竖屏，因为windowIsTranslucent 的问题，这里其实还是竖屏
}
