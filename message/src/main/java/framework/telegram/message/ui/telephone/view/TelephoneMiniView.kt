package framework.telegram.message.ui.telephone.view

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.alibaba.android.arouter.launcher.ARouter
import com.yhao.floatwindow.FloatWindow
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.ui.telephone.core.RtcEngineHolder
import framework.telegram.message.ui.telephone.TelephoneActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.TimeUtils
import framework.telegram.ui.image.AppImageView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class TelephoneMiniView : RelativeLayout, RtcEngineHolder.RtcEngineHandler {

    private var mFloatWindowKey = ""

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private var mTargetUserInfo: ContactDataModel? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        if (RtcEngineHolder.streamType == 1) {
            LayoutInflater.from(context).inflate(R.layout.msg_view_telephone_mini_video, this, true)
        } else {
            LayoutInflater.from(context).inflate(R.layout.msg_view_telephone_mini_audio, this, true)
        }

        setOnClickListener {
            // 关闭FloatWindow
            RtcEngineHolder.checkFloatWindow = false
            FloatWindow.get(mFloatWindowKey)?.hide()
            FloatWindow.destroy(mFloatWindowKey)

            // 打开TelephoneActivity
            Observable.timer(500, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO)
                        .withBoolean("showLastCall", true)
                        .navigation()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (RtcEngineHolder.isActive() && RtcEngineHolder.mineUid == mMineUid) {
            RtcEngineHolder.attachRtcEngineHandler(this@TelephoneMiniView)
            showUI()
        } else {
            // 关闭FloatWindow
            FloatWindow.get(mFloatWindowKey)?.hide()
            FloatWindow.destroy(mFloatWindowKey)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        RtcEngineHolder.unAttachRtcEngineHandler()
    }

    private fun showUI() {
        if (RtcEngineHolder.hasUserJoined) {
            //通话中
            findViewById<View>(R.id.waiting_layout).visibility = View.GONE

            if (RtcEngineHolder.streamType == 0) {
                findViewById<AppImageView>(R.id.app_image_view_icon).setImageURI(mTargetUserInfo?.icon)
                findViewById<TextView>(R.id.text_view_time).text = TimeUtils.timeFormatToMediaDuration(System.currentTimeMillis() - RtcEngineHolder.joinChannelTime)
            } else {
                val container = findViewById<FrameLayout>(R.id.remote_video_view_container)
                container.removeAllViews()
                RtcEngineHolder.setupRemoteVideo(container, RtcEngineHolder.targetUid.toInt())
            }
        } else {
            //等待接通
            findViewById<View>(R.id.waiting_layout).visibility = View.VISIBLE
            findViewById<AppImageView>(R.id.waiting_app_image_view_icon).setImageURI(mTargetUserInfo?.icon)
        }
    }

    fun setFloatWindowKey(key: String) {
        mFloatWindowKey = key
    }

    fun setTargetUserInfo(targetUserInfo: ContactDataModel?) {
        mTargetUserInfo = targetUserInfo
    }

    override fun switchToAudio() {

    }

    override fun joinAudio() {

    }

    override fun joinVideo() {

    }

    override fun waitOtherJoin() {
        findViewById<TextView>(R.id.text_view_time).text = context.getString(R.string.in_connection)
    }

    override fun otherJoined() {

    }

    override fun tick(time: Long) {
        if (RtcEngineHolder.streamType == 0) {
            findViewById<TextView>(R.id.text_view_time).text = "${TimeUtils.timeFormatToMediaDuration(time)}"
        }
    }

    override fun networkStatusChange(isMe: Boolean, isBad: Boolean) {

    }

    override fun endCall() {
        // 对方关闭了会话
        FloatWindow.get(mFloatWindowKey)?.hide()
        FloatWindow.destroy(mFloatWindowKey)
    }
}