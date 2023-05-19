package framework.telegram.message.ui.pvt

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.text.Editable
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bigkoo.pickerview.view.OptionsPickerView
import com.chad.library.adapter.base.BaseViewHolder
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.RoundingParams
import com.im.domain.pb.ContactsProto
import com.im.domain.pb.UserProto
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.ideas.common.model.im.MessageModel.MESSAGE_TYPE_VIDEO
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_CONTACTS_VERIFY_INFO_EDIT
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_CONTACT_DETAIL
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_ADD_FRIEND_FROM
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_ADD_TOKEN
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_UID
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_FORWARD_CHATS
import framework.telegram.message.bridge.event.*
import framework.telegram.message.controller.DownloadAttachmentController
import framework.telegram.message.controller.MessageController
import framework.telegram.message.controller.UploadAttachmentController
import framework.telegram.message.event.DynamicFaceUpdateEvent
import framework.telegram.message.event.InputtingStatusEvent
import framework.telegram.message.event.ScreenShotDetectionEvent
import framework.telegram.message.event.ScreenShotStateEvent
import framework.telegram.message.http.HttpManager
import framework.telegram.message.http.creator.UserHttpReqCreator
import framework.telegram.message.http.getResultWithCache
import framework.telegram.message.http.protocol.UserHttpProtocol
import framework.telegram.message.manager.*
import framework.telegram.message.ui.AndroidBug5497Workaround
import framework.telegram.message.ui.adapter.MessageAdapter
import framework.telegram.message.ui.location.ChoiceLocationActivity
import framework.telegram.message.ui.location.bean.LocationBean
import framework.telegram.message.ui.location.bean.POIBean
import framework.telegram.message.ui.telephone.core.RtcEngineHolder
import framework.telegram.message.ui.widget.MessageInputView
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.cache.kotlin.applyCache
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.*
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.file.DirManager
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.support.tools.screenshot.ScreenShotsUtils
import framework.telegram.ui.cameraview.activity.CameraActivity
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.emoji.IconFacePopup
import framework.telegram.ui.emoji.ToolsPopup
import framework.telegram.ui.face.IconFaceItem
import framework.telegram.ui.face.IconFacePageView
import framework.telegram.ui.face.dynamic.DynamicFaceBean
import framework.telegram.ui.filepicker.config.FilePickerManager
import framework.telegram.ui.flashview.FlashRelativeLayout
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.imagepicker.ImagePicker
import framework.telegram.ui.imagepicker.MimeType
import framework.telegram.ui.imagepicker.engine.impl.GlideEngine
import framework.telegram.ui.imagepicker.filter.Mp4SizeFilter
import framework.telegram.ui.menu.FloatMenu
import framework.telegram.ui.recyclerview.RecyclerViewController
import framework.telegram.ui.selectText.OperationItem
import framework.telegram.ui.selectText.SelectableTextView
import framework.telegram.ui.utils.KeyboardktUtils
import framework.telegram.ui.utils.ScreenUtils
import framework.telegram.ui.videoplayer.utils.NetworkUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.realm.RealmResults
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.*
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.custom_toolbar
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.emoji_text_view_reply_content
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.emoji_text_view_reply_user_nickname
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.image_view_delete_check_msg
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.image_view_fire_bg
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.image_view_new_msg
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.image_view_reply_close
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.image_view_send_check_msg
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.layout_check_msg
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.layout_new_msg
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.layout_reply_msg
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.message_input_view
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.recycler_view_messages
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.text_view_cancel_check_msg
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.text_view_check_msg_title
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.text_view_new_msg
import kotlinx.android.synthetic.main.msg_recording_layout.*
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Route(path = Constant.ARouter.ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY)
class PrivateChatActivity : BaseActivity(), PrivateChatContract.View, SensorEventListener {

    companion object {
        internal const val TAG = "PrivateChatActivity"
        internal const val GET_PERMISSIONS_REQUEST_CODE = 0x100
        internal const val TOOL_IMAGEPICKER_REQUESTCODE = 0x1000
        internal const val TOOL_CAMERAVIEW_REQUESTCODE = 0x2000
        internal const val TOOL_NAMECARD_REQUESTCODE = 0x3000

        private val lastUpdateUserKeyTimes by lazy { java.util.HashMap<Long, Long>() }
        private val lastUpdateUserInfoTimes by lazy { java.util.HashMap<Long, Long>() }

        private val sendForbiddenMaxTimeCache by lazy { HashMap<String, Long>() }
        private val sendForbiddenMsgTimeCache by lazy { HashMap<String, String>() }
    }

    private var mPresenter: PrivateChatPresenterImpl? = null
    private var mEmojiPopup: IconFacePopup? = null
    private var mToolsPopup: ToolsPopup? = null

    private var mContactIconView: AppImageView? = null
    private var mContactNameView: TextView? = null
    private var mCustomTitleView: View? = null
    private var mTitleView: TextView? = null
    private var mContactOnlineStatusView: TextView? = null
    private var mUnreadMessageCountView: TextView? = null

    private val mAccountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    private val mLinearLayoutManager by lazy { LinearLayoutManager(this@PrivateChatActivity) }

    private val mMessageAdapter by lazy {
        MessageAdapter(
            mLinearLayoutManager,
            WeakReference(this@PrivateChatActivity),
            message_input_view,
            ChatModel.CHAT_TYPE_PVT,
            mTargetUid
        )
    }

    private val mMineUid by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
    }

    val mTargetUid by lazy { intent.getLongExtra("targetUid", 0L) }

    private var mLocalMsgUid = 0L

    private val mSensorManager by lazy { BaseApp.app.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    private val mDefaultVolumeControlStream by lazy { volumeControlStream }

    private var mContactInfoModel: ContactDataModel? = null

    private var mRefMessage: MessageModel? = null

    //是否使用扬声器
    private var mUseSpeaker = false

    private var isFirstLoadMessages = true

    private var mLoadAllMessageBeforeFirstMsgLocalId = 0L

    private var isActivityPause = false

    private var mDeleteMe = false

    var floatMenu: FloatMenu? = null

    private var timer: Timer? = null

    private var mPv: OptionsPickerView<String>? = null

    private var mLastSendInputingTime = System.currentTimeMillis()

    private var mLastDisposable: Disposable? = null

    private var mContent: Editable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (mMineUid <= 0 || mTargetUid <= 0) {
            Toast.makeText(applicationContext, getString(R.string.uid_error), Toast.LENGTH_SHORT)
                .show()
            finish()
            return
        }

        setContentView(R.layout.msg_activity_pvt_chat)
        AndroidBug5497Workaround.assistActivity(this@PrivateChatActivity)
        mUseSpeaker = ArouterServiceManager.settingService.getDefaultUseSpeaker()

        initView()
        initData()

        DownloadAttachmentController.attachDownloadListener(mMessageAdapter)
        UploadAttachmentController.attachUploadListener(mMessageAdapter)
        mMessageAdapter.audioPlayController.attachListener()

        registerEventBus()

        initListen()
    }

    fun getTargetUid(): Long {
        return mTargetUid
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        locationMsg(intent)

        if (mPresenter?.isLoadAll() == false) {
            mPresenter?.loadAllMessageHistory()
        } else {
            if (mLocalMsgUid != 0L) {
                ThreadUtils.runOnUIThread(200) {
                    jumpToTargetMessageByMsgLocalId(mLocalMsgUid)
                    mLocalMsgUid = 0L
                }
            }
        }
    }

    private fun initData() {
        locationMsg(intent)

        PrivateChatPresenterImpl(
            this@PrivateChatActivity,
            this@PrivateChatActivity,
            lifecycle(),
            mTargetUid
        ).start(mLocalMsgUid > 0)

        //从数据库获取 当前聊天对象是不是把我删除好友了,默认不是（false）
        checkFriendship()

        //更新用户秘钥
        val lastUpdateUserKeyTime = lastUpdateUserKeyTimes[mTargetUid]
        if (lastUpdateUserKeyTime == null || (System.currentTimeMillis() - lastUpdateUserKeyTime > 60000)) {
            lastUpdateUserKeyTimes[mTargetUid] = System.currentTimeMillis()
            ArouterServiceManager.systemService.updateUserPublicKey(
                mTargetUid,
                0,
                0,
                { _, _, _, _ ->
                    AppLogcat.logger.d(TAG, "用户公钥更新成功--->")
                },
                null
            )
        }
    }

    private fun locationMsg(intent: Intent?) {
        mLocalMsgUid = intent?.getLongExtra("localMsgId", 0L) ?: 0L
    }

    @SuppressLint("CheckResult")
    private fun registerEventBus() {
        EventBus.getFlowable(DynamicFaceUpdateEvent::class.java)
            .bindToLifecycle(this@PrivateChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                updateDynamicFace()
            }

        EventBus.getFlowable(OnlineStatusChangeEvent::class.java)
            .bindToLifecycle(this@PrivateChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.uid == mTargetUid) {
                    initContactInfo(true)
                }
            }

        EventBus.getFlowable(RecallMessageEvent::class.java)
            .bindToLifecycle(this@PrivateChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.chatType == ChatModel.CHAT_TYPE_PVT && it.targetId == mTargetUid) {
                    //TODO notifyDataChange()
                    floatMenu?.dismiss()
                }
            }

        EventBus.getFlowable(ReciveMessageEvent::class.java)
            .bindToLifecycle(this@PrivateChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.chaterType == ChatModel.CHAT_TYPE_PVT && it.chaterId == mTargetUid) {
                    if (ActivitiesHelper.getInstance()
                            .toForeground() && mContactInfoModel?.isBfDisturb != true
                    ) {
                        val privacy =
                            AccountManager.getLoginAccount(AccountInfo::class.java).getPrivacy()
                        if (!BitUtils.checkBitValue(Helper.int2Bytes(privacy)[3], 0)) {
                            if (ArouterServiceManager.settingService.getVibrationStatus(
                                    privacy,
                                    false
                                )
                            ) {//震动
                                SoundPoolManager.vibrator()
                            }
                            if (ArouterServiceManager.settingService.getVoiceStatus(
                                    privacy,
                                    false
                                )
                            ) {//声音
                                SoundPoolManager.playMsgRecvForeground()
                            }
                        }
                    }
                }
            }

        EventBus.getFlowable(SearchChatEvent::class.java)
            .bindToLifecycle(this@PrivateChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.chatType == ChatModel.CHAT_TYPE_PVT && it.targetId == mTargetUid) {
                    jumpToTargetMessageByMsgLocalId(it.messageLocalId)
                }
            }

        EventBus.getFlowable(SocketStatusChangeEvent::class.java)
            .bindToLifecycle(this@PrivateChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                showContactSocketStatus()
            }

        EventBus.getFlowable(UnreadMessageEvent::class.java)
            .bindToLifecycle(this@PrivateChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.targetType != ChatModel.CHAT_TYPE_PVT || it.targetId != mTargetUid) {
                    showUnreadMessageCount()
                }
            }

        EventBus.getFlowable(FriendRelationChangeEvent::class.java)
            .bindToLifecycle(this@PrivateChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.userId == mTargetUid) {
                    if (it.deleteMe != mDeleteMe) {
                        mDeleteMe = it.deleteMe
                        initAddFriendView()
                    }
                }
            }

        EventBus.getFlowable(FriendInfoChangeEvent::class.java)
            .bindToLifecycle(this@PrivateChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.user.uid == mTargetUid) {
                    mContactInfoModel = it.user
                    showFireStatus()
                }
            }

        EventBus.getFlowable(ScreenShotDetectionEvent::class.java)
            .bindToLifecycle(this@PrivateChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                // 优先级： 已被截图提示框 >  已被删除好友框
                if (it.userId == mMineUid && it.targetId == mTargetUid) {
                    //我自己截了图
                    linear_layout_screenshots_warning.visibility = View.VISIBLE
                    text_view_screenshots_warning.text = getString(R.string.you_took_a_screenshot)
                } else if (it.userId == mTargetUid && it.targetId == mMineUid) {
                    //被对方截了图
                    linear_layout_screenshots_warning.visibility = View.VISIBLE
                    text_view_screenshots_warning.text = String.format(
                        getString(R.string.some_one_took_a_screenshot),
                        mContactInfoModel?.displayName
                    )
                }
            }

        EventBus.getFlowable(InputtingStatusEvent::class.java)
            .bindToLifecycle(this@PrivateChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.targetId == mTargetUid) {
                    showContactInputStatus(true)
                }
            }

        EventBus.getFlowable(ScreenShotStateEvent::class.java)
            .bindToLifecycle(this@PrivateChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (mTargetUid == it.targetId) {
                    if (it.open) {
                        ScreenShotsUtils.startScreenShotsListen(this@PrivateChatActivity) {
                            ArouterServiceManager.messageService.sendScreenShotsPackage(
                                mMineUid,
                                mTargetUid
                            )
                        }
                    } else {
                        ScreenShotsUtils.stopScreenShotsListen(this@PrivateChatActivity)
                    }
                }
            }
    }

    override fun getAdapterItemCount(): Int {
        return mMessageAdapter.itemCount
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as PrivateChatPresenterImpl
    }

    private fun initListen() {
        text_view_add_friend.setOnClickListener {
            mContactInfoModel?.let {
                if (!TextUtils.isEmpty(it.identify)) {
                    mPresenter?.addFriend(it.identify)
                }
            }
        }

        image_view_close_warning.setOnClickListener {
            linear_layout_screenshots_warning.visibility = View.GONE
        }
    }

    private fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back, { imageView ->
            val size = ScreenUtils.dp2px(this@PrivateChatActivity, 2f)
            imageView.setPadding(0, 0, size, 0)
        }) {
            finish()
        }

        if (mTargetUid > Constant.Common.SYSTEM_USER_MAX_UID) {
            custom_toolbar.showRightImageView(R.drawable.msg_icon_chat_stream_audio, {
                callStream(0)
            })

            custom_toolbar.showRightImageView(R.drawable.msg_icon_chat_stream_video, {
                callStream(1)
            }) {
                val size = ScreenUtils.dp2px(it.context, 20f)
                val param = it.layoutParams as LinearLayout.LayoutParams
                param.rightMargin = size
                it.layoutParams = param
            }
        }

        // set buttons
        message_input_view.inputView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                mEmojiPopup?.dismiss()
                mToolsPopup?.dismiss()
            }
            false
        }

        message_input_view.buttonsListener = object : MessageInputView.ButtonsListener {

            override fun onClickSend(uids: List<Long>, msg: String?) {
                if (!TextUtils.isEmpty(msg)) {
                    if (checkSendFrequency()) {
                        mPresenter?.sendTextMessage(
                            msg
                                ?: "", mRefMessage, mMineUid, mTargetUid, mContactInfoModel
                        )
                        clearRefMessage()
                    }
                }
            }

            override fun onClickFace() {
                mEmojiPopup?.toggle()
                message_input_view.postDelayed({
                    mToolsPopup?.dismiss()
                }, 200)
            }

            override fun onClickTools() {
                mToolsPopup?.toggle()
                message_input_view.postDelayed({
                    mEmojiPopup?.dismiss()
                }, 200)
            }

            override fun onClickFire() {
                if (message_input_view.isRecording) {
                    return
                }

                mToolsPopup?.let {
                    if (it.isShowing) {
                        it.dismiss()
                    }
                }
                KeyboardktUtils.hideKeyboard(recycler_view_messages)
                showPicker()
            }
        }

        message_input_view.recordingListener = object : MessageInputView.RecordingListener {

            override fun checkRecordable(): Boolean {
                return if (RtcEngineHolder.isActive()) {
                    if (RtcEngineHolder.streamType == 0) {
                        toast(getString(R.string.calling_audio_busy_tip))
                    } else {
                        toast(getString(R.string.calling_video_busy_tip))
                    }
                    false
                } else {
                    true
                }
            }

            override fun onRecordingStarted() {
                mMessageAdapter.audioPlayController.stopPlayVoice()
            }

            override fun onRecordingLocked() {

            }

            override fun onRecordingCompleted(
                recordTime: Long,
                recordFilePath: String?,
                highDArr: Array<out Int>
            ) {
                if (!TextUtils.isEmpty(recordFilePath)) {
                    mPresenter?.sendVoiceMessage(
                        (recordTime / 1000L).toInt(),
                        recordFilePath!!,
                        highDArr.toIntArray().toTypedArray(),
                        mRefMessage,
                        mMineUid,
                        mTargetUid,
                        mContactInfoModel
                    )
                    clearRefMessage()
                }
            }

            override fun onRecordingCanceled() {

            }

            override fun onRecordSoShort() {

            }

            override fun onPlayAudioDraft() {
                mMessageAdapter.audioPlayController.stopPlayVoice()
            }

            override fun getDraftStorageName(): String {
                return "pvt_draft_${mMineUid}_$mTargetUid"
            }

            override fun onInputHeightChange(height: Int) {
                val layoutParams = recycler_view_messages.layoutParams as FrameLayout.LayoutParams
                layoutParams.bottomMargin = height
                recycler_view_messages.layoutParams = layoutParams

                val layoutParams1 = layout_reply_msg.layoutParams as RelativeLayout.LayoutParams
                layoutParams1.bottomMargin = height
                layout_reply_msg.layoutParams = layoutParams1
            }

            override fun onInputing() {
                if (mLastSendInputingTime > 0 && mLastSendInputingTime > System.currentTimeMillis() - 1000) {
                    //1s内发过一次输入状态则忽略
                    return
                }

                val privacy = mAccountInfo.getPrivacy()
                if (!BitUtils.checkBitValue(Helper.int2Bytes(privacy)[2], 6)) {
                    MessagesManager.findPvtLastBizMessage(mMineUid, mTargetUid) { msg ->
                        msg?.let {
                            val currentTime = ArouterServiceManager.messageService.getCurrentTime()
                            if ((currentTime - msg.time) < 30 * 1000 || (currentTime - mLastSendInputingTime) < 30 * 1000) {
                                SendMessageManager.sendInputtingStatusPackage(mTargetUid)
                                mLastSendInputingTime = System.currentTimeMillis()
                            }
                        }
                    }
                }
            }
        }

        // set recyclerview
        mMessageAdapter.chaterId = mTargetUid
        mMessageAdapter.isSpeakerphoneOn = mUseSpeaker
        mMessageAdapter.setOnItemChildClickListener { _, view, position ->
            val data = mMessageAdapter.getItem(position)
            data?.let {
                if (view.id == R.id.image_view_pause) {
                    if (data.type == MessageModel.MESSAGE_TYPE_FILE
                        || data.type == MessageModel.MESSAGE_TYPE_VIDEO
                        || data.type == MessageModel.MESSAGE_TYPE_IMAGE
                        || data.type == MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE
                    ) {
                        if (data.isSend == 0) {
                            // 接收方一定是下载
                            DownloadAttachmentController.cancelDownload(
                                data.fileMessageContentBean.fileUri,
                                ChatModel.CHAT_TYPE_PVT,
                                data
                            )
                        } else {
                            // 发送方可能是下载也可能是上传
                            DownloadAttachmentController.cancelDownload(
                                data.fileMessageContentBean.fileUri,
                                ChatModel.CHAT_TYPE_PVT,
                                data
                            )
                            UploadAttachmentController.cancelUpload(
                                ChatModel.CHAT_TYPE_PVT,
                                mMineUid,
                                mTargetUid,
                                data.id
                            )
                        }
                    } else {
                    }
                } else if (view.id == R.id.image_view_gif) {
                    if (data.type == MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE) {
                        if (data.itemType == MessageModel.LOCAL_TYPE_OTHER_DYNAMIC_IMAGE ||
                            data.itemType == MessageModel.LOCAL_TYPE_MYSELF_DYNAMIC_IMAGE
                        ) {
                            DownloadAttachmentController.downloadAttachment(
                                ChatModel.CHAT_TYPE_PVT,
                                data.copyMessage()
                            )
                        } else {
                        }
                    } else {
                    }
                } else if (view.id == R.id.emoji_text_view) {
                    val lastTime = view.getTag(R.id.doubleCheckId)
                    val nowTime = System.currentTimeMillis()
                    lastTime?.let {
                        val checkTime = nowTime - (lastTime) as Long
                        if (checkTime in 1..199) {
                            KeyboardktUtils.hideKeyboard(root_view_private_chat)
                            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_PREVIEW_TEXT)
                                .withString("text", data.textMessageContent)
                                .withLong("msgId", data.msgId)
                                .withLong("targetUid", mTargetUid)
                                .withBoolean("copyable", data.snapchatTime == 0)
                                .navigation()
                        }
                    }
                    view.setTag(R.id.doubleCheckId, System.currentTimeMillis())

                } else if (view.id == R.id.layout_ref_msg) {
                    data.refMessageBean?.let {
                        jumpToTargetMessageByMsgId(data.refMessageBean.msgId)
                    }
                } else {
                    when (it.type) {
                        MessageModel.MESSAGE_TYPE_IMAGE, MessageModel.MESSAGE_TYPE_VIDEO -> {
                            val targetId = if (data.isSend == 0) data.senderId else data.targetId
                            if (it.snapchatTime > 0) {
                                ARouter.getInstance()
                                    .build(Constant.ARouter.ROUNTE_MSG_PREVIEW_PRIVATE_BRIDGE_ACTIVITY)
                                    .withLong("messageLocalId", it.id)
                                    .withInt("chatType", ChatModel.CHAT_TYPE_PVT)
                                    .withLong("targetId", targetId)
                                    .navigation()
                            } else {
                                ARouter.getInstance()
                                    .build(Constant.ARouter.ROUNTE_MSG_PREVIEW_BRIDGE_ACTIVITY)
                                    .withLong("messageLocalId", it.id)
                                    .withInt("chatType", ChatModel.CHAT_TYPE_PVT)
                                    .withLong("targetId", targetId)
                                    .navigation()
                            }
                        }
                        MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE -> {
                            val targetId = if (data.isSend == 0) data.senderId else data.targetId

                            ARouter.getInstance()
                                .build(Constant.ARouter.ROUNTE_MSG_PREVIEW_GIF_ACTIVITY)
                                .withLong("messageLocalId", it.id)
                                .withInt("chatType", ChatModel.CHAT_TYPE_PVT)
                                .withLong("targetId", targetId)
                                .withLong("msgId", it.msgId)
                                .withString("imageFileUri", it.dynamicImageMessageBean.imageFileUri)
                                .withString(
                                    "imageFileBackupUri",
                                    it.dynamicImageMessageBean.imageFileBackupUri
                                )
                                .withString("attachmentKey", it.attachmentKey)
                                .withBoolean("private", it.snapchatTime > 0)
                                .navigation()
                        }
                        MessageModel.MESSAGE_TYPE_NAMECARD -> {
                            mPresenter?.getAddToken(
                                data.nameCardMessageContent.identify,
                                data.nameCardMessageContent.uid,
                                MD5.md5("${data.nameCardMessageContent.uid}|${data.nameCardMessageContent.icon}|${data.nameCardMessageContent.nickName}")
                            )
                        }
                        MessageModel.MESSAGE_TYPE_STREAM -> {
                            val streamMessage = data.streamMessageContent
                            streamMessage?.let {
                                callStream(streamMessage.streamType)
                            }
                        }
                        MessageModel.MESSAGE_TYPE_FILE -> {
                            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_PREVIEW_FILE)
                                .withLong("messageLocalId", data.id)
                                .withInt("chatType", ChatModel.CHAT_TYPE_PVT)
                                .withLong("targetId", mTargetUid)
                                .withString("mimetype", data.fileMessageContentBean.mimeType)
                                .withString("downloadPath", data.fileMessageContentBean.fileUri)
                                .withString("fileName", data.fileMessageContentBean.name)
                                .withBoolean("private", it.snapchatTime > 0)
                                .withLong("fileSize", data.fileMessageContentBean.size).navigation()
                        }
                        MessageModel.MESSAGE_TYPE_LOCATION -> {
                            val location = data.locationMessageContentBean
                            val locationBean = LocationBean()
                            locationBean.lat = (location.lat / 1000000.0f).toDouble()
                            locationBean.lng = (location.lng / 1000000.0f).toDouble()
                            locationBean.address = location.address
                            ARouter.getInstance()
                                .build(Constant.ARouter.ROUNTE_LOCATION_SHOW_ACTIVITY)
                                .withSerializable("location", locationBean).navigation()
                        }
                        MessageModel.MESSAGE_TYPE_UNDECRYPT -> {
                            AppDialog.show(this@PrivateChatActivity, this@PrivateChatActivity) {
                                positiveButton(text = getString(R.string.confirm))
                                title(text = getString(R.string.hint))
                                message(text = getString(R.string.string_undecryte_tip))
                            }
                        }
                        else -> {
                        }
                    }
                }
            }
        }

        mMessageAdapter.setOnItemChildLongClickListener { _, view, position ->
            val data = mMessageAdapter.getItem(position)
            view?.setTag(R.id.long_click, true)
            data?.let {
                // 文本消息
                if (view is SelectableTextView) {
                    view.operationItemList = initOperItemList(it, false)
                    if (it.snapchatTime > 0) {
                        showSelectableText(view)
                    } else {
                        showSelectableText(view)
                    }
                    view.setOperationClickListener { item ->
                        when (item.action) {
                            OperationItem.ACTION_FORWARD -> {
                                if (view.selectedText == it.textMessageContent)
                                    forwardMsg(it.id, mTargetUid)
                                else if (view.selectedText.isNotEmpty())
                                    forwardMsg(view.selectedText)
                                dismissSelectableText(view)
                            }
                            OperationItem.ACTION_CANCEL -> dismissSelectableText(view)
                            OperationItem.ACTION_DELETE -> {
                                deleteMsg(
                                    listOf(it)
                                )
                                dismissSelectableText(view)
                            }
                            OperationItem.ACTION_MULTIPLE -> setCheckableMessage(it)
                            OperationItem.ACTION_REPLY -> {
                                replyMsg(it)
                                dismissSelectableText(view)
                            }
                            OperationItem.ACTION_DETAIL -> {
                                ARouter.getInstance()
                                    .build(Constant.ARouter.ROUNTE_MSG_DETAIL_ACTIVITY)
                                    .withInt("chatType", ChatModel.CHAT_TYPE_PVT)
                                    .withLong("messageLocalId", it.id)
                                    .withLong("targetId", mTargetUid)
                                    .navigation()
                                dismissSelectableText(view)
                            }
                        }
                    }
                    view.setOnCursorDragListener {
                        view.operationItemList = initOperItemList(it, true)
                        showSelectableTextOperView(view)
                    }

                }
                // 非文本消息
                else {
                    floatMenu?.dismiss()
                    floatMenu = FloatMenu(this@PrivateChatActivity)
                    val floatMenuItems = initFloatMenuItemList(it)
                    if (floatMenuItems.isNotEmpty()) {
                        floatMenu?.items(*floatMenuItems.toTypedArray())
                        floatMenu?.setOnItemClickListener { _, text ->
                            when (text) {
                                getString(R.string.copy) -> {
                                    Helper.setPrimaryClip(BaseApp.app, it.textMessageContent)
                                }
                                getString(R.string.forward) -> {
                                    forwardMsg(it.id, mTargetUid)
                                }
                                getString(R.string.reply) -> {
                                    replyMsg(it)
                                }
                                getString(R.string.delete) -> {
                                    deleteMsg(
                                        listOf(it)
                                    )
                                }
                                getString(R.string.details) -> {
                                    ARouter.getInstance()
                                        .build(Constant.ARouter.ROUNTE_MSG_DETAIL_ACTIVITY)
                                        .withInt("chatType", ChatModel.CHAT_TYPE_PVT)
                                        .withLong("messageLocalId", it.id)
                                        .withLong("targetId", mTargetUid)
                                        .navigation()
                                }
                                getString(R.string.multiple_choice) -> {
                                    setCheckableMessage(it)
                                }
                                getString(R.string.the_receiver_play),
                                getString(R.string.loudspeaker_playback) -> {
                                    playVoiceAndChangeSetting(it)
                                }
                                getString(R.string.string_silent_play) -> {
                                    val targetId =
                                        if (data.isSend == 0) data.senderId else data.targetId
                                    if (it.snapchatTime > 0) {
                                        ARouter.getInstance()
                                            .build(Constant.ARouter.ROUNTE_MSG_PREVIEW_PRIVATE_BRIDGE_ACTIVITY)
                                            .withLong("messageLocalId", it.id)
                                            .withInt("chatType", ChatModel.CHAT_TYPE_PVT)
                                            .withLong("targetId", targetId)
                                            .withBoolean("isSilentPlay", true)
                                            .navigation()
                                    } else {
                                        ARouter.getInstance()
                                            .build(Constant.ARouter.ROUNTE_MSG_PREVIEW_BRIDGE_ACTIVITY)
                                            .withLong("messageLocalId", it.id)
                                            .withInt("chatType", ChatModel.CHAT_TYPE_PVT)
                                            .withLong("targetId", targetId)
                                            .withBoolean("isSilentPlay", true)
                                            .navigation()
                                    }
                                }
                            }
                        }
                        if ((floatMenu?.listSize ?: 0) > 0) {
                            floatMenu?.tag = it.msgId
                            floatMenu?.show(recycler_view_messages.popPoint)
                        }
                    }
                }
            }
            true
        }

        recycler_view_messages.initMultiTypeRecycleView(
            mLinearLayoutManager,
            mMessageAdapter,
            false
        )
        recycler_view_messages.refreshController().setEnablePullToRefresh(false)
        recycler_view_messages.recyclerViewController().removeItemAnimator()

        val headerView = LayoutInflater.from(this@PrivateChatActivity)
            .inflate(R.layout.msg_chat_messages_head_tip, null)
        headerView.setOnClickListener {
            AppDialog.show(this@PrivateChatActivity, this@PrivateChatActivity) {
                message(text = getString(R.string.encryption_hint2))
                positiveButton(text = getString(R.string.find))
            }
        }
        recycler_view_messages.headerController().addHeader(headerView)
        recycler_view_messages.recyclerViewController().addOnVerticalScrollListener(object :
            RecyclerViewController.OnVerticalScrollListener() {

            override fun onScrolledUp() {
                super.onScrolledUp()
                KeyboardktUtils.hideKeyboard(recycler_view_messages)
            }
        })
        recycler_view_messages.recyclerViewController()
            .addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    if (layout_new_msg.visibility == View.VISIBLE && text_view_new_msg.tag != null) {
                        val firstMsgLocalId = text_view_new_msg.tag as Long
                        val first = mLinearLayoutManager.findFirstVisibleItemPosition()
                        val last = mLinearLayoutManager.findLastVisibleItemPosition()
                        for (adapterPosition in last downTo first) {
                            val dataPosition = adapterPosition - mMessageAdapter.headerLayoutCount
                            if (dataPosition >= 0) {
                                val data = mMessageAdapter.data[dataPosition]
                                if (data.id == firstMsgLocalId) {
                                    text_view_new_msg.tag = null
                                    layout_new_msg.tag = 0
                                    layout_new_msg.visibility = View.GONE
                                }
                            }
                        }
                    }

                    if (!isFirstLoadMessages && !recyclerView.canScrollVertically(-1)) {
                        if (mPresenter?.isLoadAll() == false) {
                            // 滑动到顶部
                            if (mMessageAdapter.data.count() == Constant.Common.FIRST_LOAD_MESSAGE_HISTORY_COUNT.toInt()) {
                                mLoadAllMessageBeforeFirstMsgLocalId = mMessageAdapter.data[0].id
                                mPresenter?.loadAllMessageHistory()
                            }
                        }
                    }
                }
            })

        // setup emoji and tools
        recycler_view_messages.post {
            setUpToolsPopup()
            setUpEmojiPopup()
        }

        message_input_view.setKeyboardSend(
            !BitUtils.checkBitValue(
                Helper.int2Bytes(mAccountInfo.getPrivacy())[1],
                3
            )
        )
    }

    private fun showPicker() {
        mPv = MsgFireTimePickerUtil.showSelectTimePickerChat(
            this, mContactInfoModel?.msgCancelTime
                ?: 0
        ) { _, timeValue ->
            if (timeValue == -1) {
                //关闭
                ArouterServiceManager.contactService.setBurnAfterRead(
                    lifecycle(),
                    mTargetUid,
                    false,
                    {
                        mContactInfoModel?.let {
                            it.isBfReadCancel = false
                            showFireStatus()
                        }
                    },
                    {
                        toast(String.format(getString(R.string.setup_failed), it))
                    })
            } else {
                ArouterServiceManager.contactService.setBurnAfterReadTime(
                    lifecycle(),
                    mTargetUid,
                    timeValue,
                    {
                        mContactInfoModel?.msgCancelTime = timeValue
                        showFireStatus()
                    },
                    {
                        toast(String.format(getString(R.string.setup_failed), it))
                    })
            }
        }
    }

    /**
     * 初始化截图监听 ScreenShotsUtils.startScreenShotsListen( this@PrivateChatActivity，callback)
     *
     * 停止监听  ScreenShotsUtils.stopScreenShotsListen(this@PrivateChatActivity)
     */
    private fun initScreenShotsUtils() {
        ArouterServiceManager.contactService.getContactInfo(
            lifecycle(),
            mTargetUid,
            { contactInfoModel, _ ->
                if (contactInfoModel.isBfScreenshot) {
                    ScreenShotsUtils.startScreenShotsListen(this@PrivateChatActivity) {
                        ArouterServiceManager.messageService.sendScreenShotsPackage(
                            mMineUid,
                            mTargetUid
                        )
                    }
                }
            })
    }

    override fun onBackPressed() {
        if (mPv?.isShowing == true) {
            mPv?.dismiss()
            return
        }

        if (layout_check_msg.visibility == View.VISIBLE) {
            dismissCheckMessages()
            return
        }

        if (layout_reply_msg.visibility == View.VISIBLE) {
            clearRefMessage()
            return
        }

        super.onBackPressed()
    }

    override fun resetInputingStatus() {
        showContactInputStatus(false)
    }

    override fun showNewMsgTip(unreadCount: Int, firstMsgLocalId: Long) {
        val unreadCountTmp: Int =
            if (layout_new_msg.visibility == View.VISIBLE && layout_new_msg.tag != null) {
                (layout_new_msg.tag as Int) + unreadCount
            } else {
                unreadCount
            }

        if (unreadCountTmp > 0) {
            val first = mLinearLayoutManager.findFirstVisibleItemPosition()
            val last = mLinearLayoutManager.findLastVisibleItemPosition()
            if (last == mMessageAdapter.itemCount - 1) {
                // 已滑动到最下方
                if (unreadCountTmp > (last - first + 1)) {
                    val layoutParams = layout_new_msg.layoutParams as RelativeLayout.LayoutParams
                    layoutParams.topMargin = ScreenUtils.dp2px(BaseApp.app, 128.0f)
                    layout_new_msg.layoutParams = layoutParams

                    text_view_new_msg.tag = firstMsgLocalId
                    layout_new_msg.tag = unreadCountTmp
                    layout_new_msg.visibility = View.VISIBLE

                    text_view_new_msg.text =
                        String.format(BaseApp.app.getString(R.string.has_new_msg), unreadCountTmp)
                    image_view_new_msg.setImageResource(R.drawable.msg_icon_new_msg_point_up)
                    layout_new_msg.setOnClickListener {
                        text_view_new_msg.tag = null
                        layout_new_msg.tag = 0
                        layout_new_msg.visibility = View.GONE

                        jumpToTargetMessageByMsgLocalId(firstMsgLocalId)
                    }
                } else {
                    text_view_new_msg.tag = null
                    layout_new_msg.tag = 0
                    layout_new_msg.visibility = View.GONE
                }
            } else {
                // 正在查看历史消息
                val layoutParams = layout_new_msg.layoutParams as RelativeLayout.LayoutParams
                layoutParams.topMargin =
                    ScreenUtils.getScreenHeight(BaseApp.app) - ScreenUtils.dp2px(
                        BaseApp.app,
                        128.0f
                    )
                layout_new_msg.layoutParams = layoutParams

                text_view_new_msg.tag = firstMsgLocalId
                layout_new_msg.tag = unreadCountTmp
                layout_new_msg.visibility = View.VISIBLE

                text_view_new_msg.text =
                    String.format(BaseApp.app.getString(R.string.has_new_msg), unreadCountTmp)
                image_view_new_msg.setImageResource(R.drawable.msg_icon_new_msg_point_down)
                layout_new_msg.setOnClickListener {
                    text_view_new_msg.tag = null
                    layout_new_msg.tag = 0
                    layout_new_msg.visibility = View.GONE

                    jumpToTargetMessageByMsgLocalId(firstMsgLocalId)
                }
            }
        }
    }

    private fun dismissCheckMessages() {
        mMessageAdapter.setUnCheckable()
        layout_check_msg.visibility = View.GONE

        message_input_view.inputView.text = mContent
        mContent = null
    }

    private fun setCheckableMessage(msg: MessageModel) {
        mContent = message_input_view.inputView.text
        message_input_view.inputView.text = null

        KeyboardktUtils.hideKeyboard(message_input_view.inputView)

        text_view_cancel_check_msg.setOnClickListener {
            dismissCheckMessages()
        }

        text_view_clear_msg.setOnClickListener {
            // 删除
            deleteAllMsgs()
            dismissCheckMessages()
        }

        image_view_delete_check_msg.setOnClickListener {
            val msgModels = mMessageAdapter.getCheckableMessages()
            // 删除
            deleteMsg(msgModels)
            dismissCheckMessages()
        }

        image_view_send_check_msg.setOnClickListener {
            // 发送
            val ids = ArrayList<String>()
            mMessageAdapter.getCheckableMessages().forEach {
                if (it.type != MessageModel.MESSAGE_TYPE_UNDECRYPT && it.type != MessageModel.MESSAGE_TYPE_UNKNOW) {
                    ids.add(it.id.toString())
                }
            }

            forwardMsgs(ids, mTargetUid)

            dismissCheckMessages()
        }

        layout_check_msg.visibility = View.VISIBLE
        mMessageAdapter.setCheckable(msg) { msgCount ->
            text_view_check_msg_title.text = "$msgCount"

            if (msgCount == 0) {
                image_view_send_check_msg.isEnabled = false
                image_view_send_check_msg.isClickable = false
                image_view_delete_check_msg.isEnabled = false
                image_view_delete_check_msg.isClickable = false
                image_view_delete_check_msg.setImageResource(R.drawable.msg_icon_delete_disable)
                image_view_send_check_msg.setImageResource(R.drawable.msg_chat_ic_send_disable)
            } else {
                image_view_send_check_msg.isEnabled = true
                image_view_send_check_msg.isClickable = true
                image_view_delete_check_msg.isEnabled = true
                image_view_delete_check_msg.isClickable = true
                image_view_delete_check_msg.setImageResource(R.drawable.msg_icon_delete)
                image_view_send_check_msg.setImageResource(R.drawable.msg_chat_ic_send)
            }
        }
    }

    private fun playVoiceAndChangeSetting(data: MessageModel) {
        message_input_view.saveToDraft()
        //需求是每次长按语音消息，会弹出与设置相反的播放模式，如果用户选择了这个播放模式，则保存设置
        if (mUseSpeaker) {
            //听筒播放
            ArouterServiceManager.settingService.setDefaultUseSpeaker(false)
            mMessageAdapter.audioPlayController.playVoice(data, false)
        } else {
            //扬声器播放
            ArouterServiceManager.settingService.setDefaultUseSpeaker(true)
            mMessageAdapter.audioPlayController.playVoice(data, true)
        }
        mUseSpeaker = ArouterServiceManager.settingService.getDefaultUseSpeaker()
    }

    private fun forwardMsgs(ids: ArrayList<String>, targetId: Long) {
        ARouter.getInstance().build(ROUNTE_MSG_FORWARD_CHATS)
            .withStringArrayList("messageLocalIds", ids)
            .withInt("chatType", ChatModel.CHAT_TYPE_PVT)
            .withLong("targetId", targetId)
            .navigation()
    }

    private fun forwardMsg(id: Long, targetId: Long) {
        ARouter.getInstance().build(ROUNTE_MSG_FORWARD_CHATS)
            .withLong("messageLocalId", id)
            .withInt("chatType", ChatModel.CHAT_TYPE_PVT)
            .withLong("targetId", targetId)
            .navigation()
    }

    private fun forwardMsg(msg: String) {
        ARouter.getInstance().build(ROUNTE_MSG_FORWARD_CHATS)
            .withLong("messageLocalId", 1)
            .withInt("chatType", ChatModel.CHAT_TYPE_PVT)
            .withLong("targetId", 1)
            .withString("msgTextContent", msg)
            .navigation()
    }

    private fun replyMsg(messageModel: MessageModel) {
        if (messageModel.isSend == 1 && messageModel.status != MessageModel.STATUS_SENDED_HAS_RESP) {
            return
        }

        if (messageModel.ownerUid == mMineUid) {
            // 回复自己
            setRefMessage(
                getString(R.string.you),
                AccountManager.getLoginAccount(AccountInfo::class.java).getNickName(),
                messageModel
            )
        } else {
            // 回复他人
            ArouterServiceManager.contactService.getContactInfo(
                lifecycle(),
                messageModel.ownerUid,
                { contactModel, _ ->
                    setRefMessage(contactModel.displayName, contactModel.nickName, messageModel)
                })
        }
    }

    private fun setRefMessage(
        displayName: String,
        messageOwnerName: String,
        messageModel: MessageModel
    ) {
        val refMessage = messageModel.copyMessage()

        // set ownerName
        refMessage.ownerName = messageOwnerName

        // set RefMessage
        mRefMessage = refMessage

        // display
        editTextMessage.requestFocus()
        layout_reply_msg.visibility = View.VISIBLE
        image_view_reply_close.setOnClickListener {
            clearRefMessage()
        }

        emoji_text_view_reply_user_nickname.text = "$displayName"
        if (refMessage.type == MessageModel.MESSAGE_TYPE_FILE) {
            emoji_text_view_reply_content.text =
                String.format(getString(R.string.file_sign_mat), refMessage.refContent)
        } else {
            emoji_text_view_reply_content.text = refMessage.refContent
        }
    }

    private fun clearRefMessage() {
        mRefMessage = null
        layout_reply_msg.visibility = View.GONE
    }

    private fun deleteAllMsgs() {
        AppDialog.showBottomListView(
            this,
            this,
            listOf(
                getString(R.string.clear_and_delete_msgs_local),
                String.format(
                    getString(R.string.clear_and_delete_pvt_msgs_local),
                    mContactInfoModel?.displayName
                ),
                getString(R.string.cancel)
            )
        ) { dialog, index, _ ->
            if (index <= 1) {
                mMessageAdapter.audioPlayController.stopPlayVoice()
            }
            when (index) {
                0 -> {
                    // 删除本地
                    MessageController.clearMessageHistory(
                        ChatModel.CHAT_TYPE_PVT,
                        mTargetUid
                    )
                }
                1 -> {
                    // 删除本地和远程
                    MessageController.recallMessages(
                        ChatModel.CHAT_TYPE_PVT,
                        mMineUid,
                        mTargetUid,
                        ArouterServiceManager.messageService.getCurrentTime(),
                        deleteChat = false
                    )
                }
                else -> {
                    dialog.dismiss()
                }
            }
        }
    }

    private fun deleteMsg(datas: List<MessageModel>) {
        AppDialog.showBottomListView(
            this,
            this,
            listOf(
                getString(R.string.clear_and_delete_msgs),
                String.format(
                    getString(R.string.clear_and_delete_pvt_msgs),
                    mContactInfoModel?.displayName
                ),
                getString(R.string.cancel)
            )
        ) { dialog, index, _ ->
            if (index <= 1) {
                datas.forEach { data ->
                    val id = data.id
                    val isVoice = data.type == MessageModel.MESSAGE_TYPE_VOICE
                    if (isVoice) {
                        mMessageAdapter.audioPlayController.stopPlayVoice(data)
                    }
                    UploadAttachmentController.cancelUpload(
                        ChatModel.CHAT_TYPE_PVT,
                        mMineUid,
                        mTargetUid,
                        id
                    )
                }
            }
            when (index) {
                0 -> {
                    // 删除本地
                    datas.forEach { data ->
                        MessageController.deleteMessage(
                            ChatModel.CHAT_TYPE_PVT,
                            mMineUid,
                            mTargetUid,
                            data.id
                        )
                    }
                }
                1 -> {
                    // 删除本地和远程
                    datas.forEach { data ->
                        if (data.type == MessageModel.MESSAGE_TYPE_STREAM) {
                            MessageController.recallMessage(
                                ChatModel.CHAT_TYPE_PVT,
                                mMineUid,
                                mTargetUid,
                                0,
                                data.flag
                            )
                        } else {
                            if (data.msgId > 0) {
                                MessageController.recallMessage(
                                    ChatModel.CHAT_TYPE_PVT,
                                    mMineUid,
                                    mTargetUid,
                                    data.msgId
                                )
                            } else {
                                MessageController.deleteMessage(
                                    ChatModel.CHAT_TYPE_PVT,
                                    mMineUid,
                                    mTargetUid,
                                    data.id
                                )
                            }
                        }
                    }
                }
                else -> {
                    dialog.dismiss()
                }
            }
        }
    }

    private fun jumpToTargetMessageByMsgLocalId(msgLocalId: Long) {
        val datas = mMessageAdapter.data
        var adapterPosition = -1
        val count = datas.count() - 1
        run outside@{
            for (i in count downTo 0) {
                if (msgLocalId == datas[i].id) {
                    adapterPosition = i + mMessageAdapter.headerLayoutCount
                    return@outside
                }
            }
        }

        scoldPosition(adapterPosition)
    }

    private fun jumpToTargetMessageByMsgId(msgId: Long) {
        val datas = mMessageAdapter.data
        var adapterPosition = -1
        val count = datas.count() - 1
        run outside@{
            for (i in count downTo 0) {
                if (msgId == datas[i].msgId) {
                    adapterPosition = i + mMessageAdapter.headerLayoutCount
                    return@outside
                }
            }
        }
        scoldPosition(adapterPosition)
    }

    private val SMOOTHSCROLLSTEP = 10

    private fun scoldPosition(adapterPosition: Int) {
        if (adapterPosition > -1) {
            val recyclerView = recycler_view_messages.recyclerViewController().recyclerView
            val linearManager = recyclerView.layoutManager as LinearLayoutManager
            val lastIndex = linearManager.findLastVisibleItemPosition()//下边界
            val firstIndex = linearManager.findFirstVisibleItemPosition()//上边界
            Log.i("lzh", "下边界 $lastIndex  上边界 $firstIndex  目标index $adapterPosition")

            when {
                adapterPosition < firstIndex - SMOOTHSCROLLSTEP -> {//要向上滚,上区间 + 想上10个单位
                    recyclerView.smoothScrollToPosition(firstIndex - SMOOTHSCROLLSTEP)
                    Log.i("lzh", "1  ${firstIndex - SMOOTHSCROLLSTEP}")
                    flashScrollToPosition(recyclerView) {
                        recycler_view_messages.recyclerViewController()
                            .scrollToPosition(adapterPosition)
                        Handler().postDelayed({
                            flashView(adapterPosition)
                        }, 500)
                    }
                }
                adapterPosition > lastIndex + SMOOTHSCROLLSTEP -> {//要向下滚,下区间 + 想下10个单位
                    Log.i("lzh", "2  ${lastIndex + SMOOTHSCROLLSTEP}")
                    recyclerView.smoothScrollToPosition(lastIndex + SMOOTHSCROLLSTEP)
                    flashScrollToPosition(recyclerView) {
                        recycler_view_messages.recyclerViewController()
                            .scrollToPosition(adapterPosition)
                        Handler().postDelayed({
                            flashView(adapterPosition)
                        }, 500)
                    }
                }
                else -> {
                    Log.i("lzh", "3  ")//可视范围
                    recyclerView.smoothScrollToPosition(adapterPosition)
                    Handler().postDelayed({
                        flashView(adapterPosition)
                    }, 500)
                }
            }
        }
    }

    private fun flashScrollToPosition(recyclerView: RecyclerView, idleCall: (() -> Unit)? = null) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                Log.i("lzh", "onScrollStateChanged  $newState")
                if (newState == SCROLL_STATE_IDLE) {
                    recyclerView.removeOnScrollListener(this)
                    Log.i("lzh", "idleCall")
                    idleCall?.invoke()
                }
            }
        })
    }

    private fun flashView(adapterPosition: Int) {
        val holder =
            recycler_view_messages.recyclerView.findViewHolderForAdapterPosition(adapterPosition)
        if (holder is BaseViewHolder) {
            Log.i("lzh", "flashView 1")
            val warpLayout = holder.getView<RelativeLayout>(R.id.warp_layout)
            warpLayout?.let { warpView ->
                val view = holder.getView<FlashRelativeLayout>(R.id.flash_layout)
                view.layoutParams.width = warpView.width
                view.layoutParams.height = warpView.height
                view?.let {
                    it.flash(warpView.width, warpView.height)
                }
            }
        } else {
            Log.i("lzh", "hold null")
        }
    }

    private fun initContactInfo(froce: Boolean) {
        // 获取个人信息缓存
        val lastUpdateUserInfoTime = lastUpdateUserInfoTimes[mTargetUid]
        if (froce || lastUpdateUserInfoTime == null || (System.currentTimeMillis() - lastUpdateUserInfoTime > 60000)) {
            lastUpdateUserInfoTimes[mTargetUid] = System.currentTimeMillis()

            ArouterServiceManager.contactService.getContactInfo(
                lifecycle(),
                mTargetUid,
                { contactInfoModel, _ ->
                    if (contactInfoModel.isBfCancel) {
                        ArouterServiceManager.messageService.clearMessageHistory(
                            ChatModel.CHAT_TYPE_PVT,
                            contactInfoModel.uid
                        )
                    }

                    val privacy =
                        AccountManager.getLoginAccount(AccountInfo::class.java).getPrivacy()
                    val isClose = BitUtils.checkBitValue(Helper.int2Bytes(privacy)[2], 7)
                    mMessageAdapter.showReceiptStatus = !isClose && contactInfoModel.isReadReceipt

                    initContactInfoView(contactInfoModel)
                })
        } else {
            ArouterServiceManager.contactService.getContactInfoCache(
                lifecycle(),
                mTargetUid,
                { contactInfoModel ->
                    if (contactInfoModel.isBfCancel) {
                        ArouterServiceManager.messageService.clearMessageHistory(
                            ChatModel.CHAT_TYPE_PVT,
                            contactInfoModel.uid
                        )
                    }

                    val privacy =
                        AccountManager.getLoginAccount(AccountInfo::class.java).getPrivacy()
                    val isClose = BitUtils.checkBitValue(Helper.int2Bytes(privacy)[2], 7)
                    mMessageAdapter.showReceiptStatus = !isClose && contactInfoModel.isReadReceipt

                    initContactInfoView(contactInfoModel)
                })
        }
    }

    private fun callStream(streamType: Int) {
        if (NetworkUtils.isAvailable(BaseApp.app) && ReceiveMessageManager.socketIsLogin) {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO)
                .withLong("targetUid", mTargetUid).withInt("streamType", streamType).navigation()
        } else {
            toast(getString(R.string.socket_is_error))
        }
    }

    private fun showUnreadMessageCount() {
        ThreadUtils.runOnIOThread {
            val count = ArouterServiceManager.messageService.getAllUnreadMessageCount()
            ThreadUtils.runOnUIThread {
                if (count > 0) {
                    val countText = when {
                        count >= 1000 -> "..."
                        count >= 100 -> "99+"
                        else -> "$count"
                    }
                    mUnreadMessageCountView?.text = countText
                    mUnreadMessageCountView?.visibility = View.VISIBLE
                } else {
                    mUnreadMessageCountView?.visibility = View.GONE
                }
            }
        }
    }

    private fun showFireStatus() {
        mContactInfoModel?.let { it ->
            if (it.isBfReadCancel) {
                // 阅后即焚模式
                message_input_view.setFireText(TimeUtils.timeFormatForDeadline(it.msgCancelTime))
                image_view_fire_bg.visibility = View.VISIBLE
            } else {
                // 普通模式
                message_input_view.setFireText(null)
                image_view_fire_bg.visibility = View.GONE

                mPv?.let { dialog ->
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                }
            }
        }

        MessagesManager.checkToMessageFireStatus(ChatModel.CHAT_TYPE_PVT, mMineUid, mTargetUid, {
            if (it || mContactInfoModel?.isBfReadCancel == true) {
                timer?.cancel()
                timer = Timer()
                timer?.schedule(object : TimerTask() {
                    override fun run() {
                        try {
                            ThreadUtils.runOnUIThread {
                                mMessageAdapter.notifyItemExpireStatus()
                            }
                        } catch (e: java.lang.Exception) {
                        }
                    }
                }, 0, 1000)
            } else {
                timer?.cancel()
            }
        })

    }

    private fun showContactSocketStatus() {
        if (ReceiveMessageManager.socketIsLogin) {
            mCustomTitleView?.visibility = View.VISIBLE
            mContactNameView?.visibility = View.VISIBLE
            mContactIconView?.visibility = View.VISIBLE
            mTitleView?.visibility = View.GONE

            if (mTargetUid > Constant.Common.SYSTEM_USER_MAX_UID) {
                mContactOnlineStatusView?.visibility = View.VISIBLE
            } else {
                mContactOnlineStatusView?.visibility = View.GONE
            }

        } else {
            mCustomTitleView?.visibility = View.GONE
            mContactNameView?.visibility = View.GONE
            mContactOnlineStatusView?.visibility = View.GONE
            mContactIconView?.visibility = View.GONE
            mTitleView?.visibility = View.VISIBLE

            if (NetworkUtils.isAvailable(BaseApp.app)) {
                //连接中...
                showCenterTitle(getString(R.string.connecting_sign))
            } else {
                //未连接
                showCenterTitle(getString(R.string.ununited))
            }
        }
    }

    private fun showContactInputStatus(inputting: Boolean) {
        if (inputting) {
            mContactOnlineStatusView?.visibility = View.VISIBLE
            mContactOnlineStatusView?.text = getString(R.string.in_the_input)

            // 延迟5s取消输入状态
            mLastDisposable?.dispose()
            mLastDisposable = io.reactivex.Observable.just("")
                .delay(5, TimeUnit.SECONDS)
                .bindToLifecycle(this@PrivateChatActivity)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    showContactInputStatus(false)
                }
        } else {
            if (mContactOnlineStatusView != null && mContactInfoModel != null && mTargetUid > Constant.Common.SYSTEM_USER_MAX_UID) {
                mContactOnlineStatusView?.let { contactOnlineStatusView ->
                    mContactOnlineStatusView?.visibility = View.VISIBLE
                    mContactInfoModel?.let { contactInfoModel ->
                        ArouterServiceManager.contactService.showOnlineStatus(
                            contactInfoModel.uid,
                            contactInfoModel.isShowLastOnlineTime,
                            contactInfoModel.isOnlineStatus,
                            contactInfoModel.lastOnlineTime,
                            contactOnlineStatusView
                        )
                    }
                }
            } else {
                mContactOnlineStatusView?.visibility = View.GONE
            }
        }
    }

    private fun showCenterTitle(title: String) {
        if (mTitleView == null) {
            custom_toolbar.showCenterTitle(title) {
                it.textSize = 16.0f
                mTitleView = it
            }
        } else {
            mTitleView?.text = title
        }
    }

    private fun showSelectableText(view: SelectableTextView) {
        view.isShowOperateView(true)
        view.showAndSelectAll()
    }

    private fun showSelectableTextOperView(view: SelectableTextView) {
        view.isShowOperateView(true)
    }

    private fun dismissSelectableText(view: SelectableTextView) {
        view.isShowOperateView(false)
        view.hideAndUnSelectAll()
    }

    private fun dimissSelectableTextOperView(view: SelectableTextView) {
        view.isShowOperateView(false)
    }

    private fun initOnlineStatusView(
        uid: Long,
        isShowLastOnlineTime: Boolean,
        isOnlineStatus: Boolean,
        lastOnlineTime: Long
    ) {
        if (mTargetUid > Constant.Common.SYSTEM_USER_MAX_UID) {
            mContactOnlineStatusView?.visibility = View.VISIBLE
            ArouterServiceManager.contactService.showOnlineStatus(
                uid,
                isShowLastOnlineTime,
                isOnlineStatus,
                lastOnlineTime,
                mContactOnlineStatusView!!
            )
        } else {
            mContactOnlineStatusView?.visibility = View.GONE
        }
    }

    private fun initContactInfoView(contactInfoModel: ContactDataModel) {
        mContactInfoModel = contactInfoModel

        val map = HashMap<Long, Any>()
        map[contactInfoModel.uid] = contactInfoModel
        mMessageAdapter.setMessageOwnerList(map)

        val name = contactInfoModel.displayName
        if (mContactNameView == null) {
            val view = LayoutInflater.from(this@PrivateChatActivity)
                .inflate(R.layout.msg_chat_title_name_and_status_view, null)
            mCustomTitleView = view
            mContactNameView = view.findViewById(R.id.text_view_username)
            mContactNameView?.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            mContactNameView?.text = name

            mContactOnlineStatusView = view.findViewById(R.id.text_view_online_status)
            initOnlineStatusView(
                contactInfoModel.uid,
                contactInfoModel.isShowLastOnlineTime,
                contactInfoModel.isOnlineStatus,
                contactInfoModel.lastOnlineTime
            )

            custom_toolbar.showCustomLeftView(view) {
                if (mTargetUid == Constant.Common.FILE_TRANSFER_UID) {
                    ARouter.getInstance()
                        .build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_TRANSMISSION_ASSISTANT_SETTING)
                        .navigation()
                } else {
                    ARouter.getInstance()
                        .build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_CONTACT_SETTING)
                        .withLong("userId", mTargetUid).navigation()
                }
            }
        } else {
            mContactNameView?.text = name
            initOnlineStatusView(
                contactInfoModel.uid,
                contactInfoModel.isShowLastOnlineTime,
                contactInfoModel.isOnlineStatus,
                contactInfoModel.lastOnlineTime
            )
        }

        if (mContactIconView == null) {
            custom_toolbar.showLeftImageView(
                uri = UriUtils.parseUri(contactInfoModel.icon),
                height = 28f,
                width = 28f,
                onClickCallback = {
                    if (mTargetUid == Constant.Common.FILE_TRANSFER_UID) {
                        ARouter.getInstance()
                            .build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_TRANSMISSION_ASSISTANT_SETTING)
                            .navigation()
                    } else {
                        ARouter.getInstance()
                            .build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_CONTACT_SETTING)
                            .withLong("userId", mTargetUid).navigation()
                    }
                },
                listen = {
                    val roundingParams = RoundingParams.fromCornersRadius(5f)
                    roundingParams.roundAsCircle = true
                    it.hierarchy.roundingParams = roundingParams
                    it.hierarchy.actualImageScaleType = ScalingUtils.ScaleType.FOCUS_CROP
                    it.hierarchy.setPlaceholderImage(R.drawable.common_holder_one_user)
                    it.hierarchy.setBackgroundImage(
                        ColorDrawable(
                            ContextCompat.getColor(
                                this@PrivateChatActivity,
                                R.color.edeff2
                            )
                        )
                    )
                    it.hierarchy.fadeDuration = 300
                    (it.layoutParams as LinearLayout.LayoutParams).setMargins(
                        ScreenUtils.dp2px(this@PrivateChatActivity, 16f),
                        0,
                        ScreenUtils.dp2px(this@PrivateChatActivity, 8f),
                        0
                    )
                    mContactIconView = it
                })
        } else {
            mContactIconView?.setImageURI(UriUtils.parseUri(contactInfoModel.icon))
        }

        if (mUnreadMessageCountView == null) {
            custom_toolbar.showLeftTextView("", {
                finish()
            }, listen = {
                it.layoutParams.height = ScreenUtils.dp2px(this@PrivateChatActivity, 16f)
                (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 10, 0, 10)
                it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                it.setTextColor(ContextCompat.getColor(this@PrivateChatActivity, R.color.white))
                it.setBackgroundResource(R.drawable.common_corners_trans_f50d2e_16_0)
                it.minWidth = ScreenUtils.dp2px(this@PrivateChatActivity, 16f)
                it.setPadding(5, 0, 5, 0)

                mUnreadMessageCountView = it
                mUnreadMessageCountView?.visibility = View.GONE
            })
        }

        showFireStatus()
        showContactSocketStatus()
        showUnreadMessageCount()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requestCode == GET_PERMISSIONS_REQUEST_CODE) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.no_permissions_were_obtained),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }

        if (requestCode == TOOL_IMAGEPICKER_REQUESTCODE && resultCode == Activity.RESULT_OK) {
            val paths = ImagePicker.obtainInfoResult(data)
            if (paths != null && paths.isNotEmpty()) {
                paths.forEach { mediaInfo ->
                    when {
                        3 == mediaInfo.type -> {
                            // 以视频传输
                            mPresenter?.sendVideoMessage(
                                mediaInfo.path,
                                mRefMessage,
                                mMineUid,
                                mTargetUid,
                                mContactInfoModel
                            )
                        }
                        2 == mediaInfo.type -> {
                            // 以动图传输
                            if (checkSendFrequency()) {
                                mPresenter?.sendDynamicImageMessage(
                                    0,
                                    mediaInfo.path,
                                    mRefMessage,
                                    mMineUid,
                                    mTargetUid,
                                    mContactInfoModel
                                )
                            }
                        }
                        else -> {
                            // 以静态图片传输
                            mPresenter?.sendImageMessage(
                                mediaInfo.path,
                                mRefMessage,
                                mMineUid,
                                mTargetUid,
                                mContactInfoModel
                            )
                        }
                    }
                }

                clearRefMessage()
            }
        }

        if (requestCode == TOOL_CAMERAVIEW_REQUESTCODE && resultCode == Activity.RESULT_OK) {
            val mimeType = data?.getStringExtra(CameraActivity.RESULT_FLAG_MIME_TYPE)
            val filePath = data?.getStringExtra(CameraActivity.RESULT_FLAG_PATH)

            filePath?.let {
                when (mimeType) {
                    CameraActivity.JPEG -> {// 以图片传输
                        mPresenter?.sendImageMessage(
                            filePath,
                            mRefMessage,
                            mMineUid,
                            mTargetUid,
                            mContactInfoModel
                        )
                        clearRefMessage()
                    }
                    CameraActivity.MP4 -> {// 以视频传输
                        mPresenter?.sendVideoMessage(
                            filePath,
                            mRefMessage,
                            mMineUid,
                            mTargetUid,
                            mContactInfoModel
                        )
                        clearRefMessage()
                    }
                    else -> {
                        // 报错
                    }
                }
            }
        }

        if (requestCode == TOOL_NAMECARD_REQUESTCODE && resultCode == Activity.RESULT_OK) {
            val uid = data?.getLongExtra("uid", 0L) ?: 0L
            if (uid > 0) {
                mPresenter?.sendNameCardMessage(
                    uid,
                    mRefMessage,
                    mMineUid,
                    mTargetUid,
                    mContactInfoModel
                )
                clearRefMessage()
            }
        }

        //文件
        if (requestCode == FilePickerManager.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            //返回的是文件和文件类型
            val arrayList = FilePickerManager.obtainData()
            for (hashMap in arrayList) {
                val mimeType = hashMap["mimetype"] as String
                val path = hashMap["path"] as String
                val file = File(path)
                if (file.exists() && file.isFile && file.length() > 0) {
                    mPresenter?.sendFileMessage(
                        file.absolutePath,
                        mimeType,
                        mRefMessage,
                        mMineUid,
                        mTargetUid,
                        mContactInfoModel
                    )
                }
            }
            clearRefMessage()
        }

        //位置
        if (requestCode == ChoiceLocationActivity.REQUEST_CODE_SEND_LOCATION && resultCode == Activity.RESULT_OK && data != null) {
            val poiBean = data.getSerializableExtra("data") as POIBean?
            poiBean?.let {
                mPresenter?.sendLocationMessage(
                    poiBean.lat,
                    poiBean.lng,
                    poiBean.name,
                    mRefMessage,
                    mMineUid,
                    mTargetUid,
                    mContactInfoModel
                )
                clearRefMessage()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (isActivityPause) {
            isActivityPause = false
            mPresenter?.setAllMessageReaded()
        } else {
            isActivityPause = false
        }

        // get permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ), GET_PERMISSIONS_REQUEST_CODE
                )
            }
        }

        if (!mUseSpeaker) {
            mSensorManager.registerListener(
                this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        initContactInfo(false)

        mMessageAdapter.notifyDataSetChanged()

        initScreenShotsUtils()
    }

    override fun onPause() {
        super.onPause()

        isActivityPause = true

        mMessageAdapter.audioPlayController.stopPlayVoice()

        message_input_view.saveToDraft()

        if (!mUseSpeaker) {
            mSensorManager.unregisterListener(this)
        }

        ScreenShotsUtils.stopScreenShotsListen(this@PrivateChatActivity)
    }

    override fun onDestroy() {
        super.onDestroy()

        mEmojiPopup?.dismiss()
        mToolsPopup?.dismiss()

        DownloadAttachmentController.detachDownloadListener(mMessageAdapter)
        UploadAttachmentController.detachUploadListener(mMessageAdapter)

        recycler_view_messages?.destory()

        timer?.cancel()

        message_input_view.recordingListener = null

        mPresenter?.destory()

        mMessageAdapter.audioPlayController.detachListener()

        (BaseApp.app.getSystemService(Context.AUDIO_SERVICE) as AudioManager).isSpeakerphoneOn =
            true
        (BaseApp.app.getSystemService(Context.AUDIO_SERVICE) as AudioManager).mode =
            AudioManager.MODE_NORMAL
        volumeControlStream = mDefaultVolumeControlStream

        MessagesManager.deleteToUserFireMessage(mMineUid, mTargetUid, {
            if (it != null) {
                ChatsHistoryManager.updateChatLastMsg(
                    mMineUid,
                    ChatModel.CHAT_TYPE_PVT,
                    mTargetUid,
                    it,
                    changeTime = false
                )
            }
        })
    }

    override fun isActivityPause(): Boolean {
        val pm = BaseApp.app.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            isActivityPause || !pm.isInteractive
        } else {
            isActivityPause || !pm.isScreenOn
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val audioManager = BaseApp.app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (!audioManager.isWiredHeadsetOn) {
            val range = event?.values?.get(0) ?: 0f
            if (range >= event?.sensor?.maximumRange ?: 0f) {
                mMessageAdapter.audioPlayController.setSpeakerphoneOn(true)
            } else {
                mMessageAdapter.audioPlayController.setSpeakerphoneOn(false)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onMessagesLoadComplete(messageModels: RealmResults<MessageModel>?) {

    }

    override fun onMessagesChanged(result: RealmResults<MessageModel>) {
        if (!result.isValid)
            return

        if (mPresenter?.isLoadAll() == true) {
            recycler_view_messages.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (mLoadAllMessageBeforeFirstMsgLocalId > 0) {
                        jumpToTargetMessage(mLoadAllMessageBeforeFirstMsgLocalId)
                        mLoadAllMessageBeforeFirstMsgLocalId = 0L
                    }

                    if (mLocalMsgUid != 0L) {
                        ThreadUtils.runOnUIThread(200) {
                            jumpToTargetMessageByMsgLocalId(mLocalMsgUid)
                            mLocalMsgUid = 0L
                        }
                    }

                    recycler_view_messages.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }

        mMessageAdapter.setNewData(result)
        val resultCount = result.size
        recycler_view_messages.post {
            val layoutManager =
                recycler_view_messages.recyclerViewController().layoutManager as LinearLayoutManager
            if (isFirstLoadMessages || layoutManager.findLastVisibleItemPosition() > resultCount - 3) {
                recycler_view_messages.recyclerViewController()?.scrollToEnd()
                recycler_view_messages.post {
                    if (recycler_view_messages.visibility != View.VISIBLE) {
                        recycler_view_messages.visibility = View.VISIBLE
                    }
                }
            }
            isFirstLoadMessages = false
        }
    }

    private fun jumpToTargetMessage(msgLocalId: Long) {
        val datas = mMessageAdapter.data
        var adapterPosition = -1
        val count = datas.count() - 1
        run outside@{
            for (i in count downTo 0) {
                if (msgLocalId == datas[i].id) {
                    adapterPosition = i + mMessageAdapter.headerLayoutCount
                    return@outside
                }
            }
        }

        val first = mLinearLayoutManager.findFirstVisibleItemPosition()
        val last = mLinearLayoutManager.findLastVisibleItemPosition()
        recycler_view_messages.recyclerViewController()
            .scrollToPosition(adapterPosition + (last - first + 1))
    }

    override fun isActive(): Boolean {
        return !ActivitiesHelper.isDestroyedActivity(this@PrivateChatActivity)
    }

    private fun setUpEmojiPopup() {
        mEmojiPopup = IconFacePopup.Builder.fromRootView(root_view_private_chat)
            .setOnPopupShownListener { message_input_view.faceView.setImageResource(R.drawable.msg_chat_ic_keyboard) }
            .setOnSoftKeyboardOpenListener {
                recycler_view_messages.post {
                    recycler_view_messages.recyclerViewController().scrollToEnd()
                }
            }
            .setOnPopupDismissListener { message_input_view.faceView.setImageResource(R.drawable.msg_chat_ic_emoji) }
            .setOnIconFaceListener(object : IconFacePageView.OnIconFaceListener {
                override fun onIconFaceBackspaceClicked() {

                }

                override fun onIconFaceClicked(iconFaceItem: IconFaceItem?, type: Int) {
                    if (type == 3 && iconFaceItem != null) {
                        if (this@PrivateChatActivity.getString(R.string.append) == iconFaceItem.name) {
                            // 管理表情
                            ARouter.getInstance()
                                .build(Constant.ARouter.ROUNTE_MSG_DYNAMIC_FACE_MANAGER)
                                .navigation()
                        } else {
                            // 发送表情
                            if (checkSendFrequency()) {
                                mPresenter?.sendDynamicImageUrlMessage(
                                    iconFaceItem.id,
                                    iconFaceItem.path,
                                    iconFaceItem.width,
                                    iconFaceItem.height,
                                    null,
                                    mMineUid,
                                    mTargetUid,
                                    mContactInfoModel
                                )
                            }
                        }
                    }
                }
            })
            .setOnSoftKeyboardCloseListener { }
            .build(message_input_view.inputView)

        EventBus.publishEvent(DynamicFaceUpdateEvent())
    }

    private fun checkSendFrequency(): Boolean {
        val forbiddenMaxTime = sendForbiddenMaxTimeCache["${mMineUid}_${mTargetUid}"] ?: 0L
        return if (System.currentTimeMillis() < forbiddenMaxTime) {
            // 当前时间小于解禁的时间
            ArouterServiceManager.messageService.insertSystemTipMsg(
                ChatModel.CHAT_TYPE_PVT,
                mTargetUid,
                0L,
                getString(R.string.send_messge_so_fast_tip)
            )
            false
        } else {
            val forbiddenCountCache = sendForbiddenMsgTimeCache["${mMineUid}_${mTargetUid}"]
            if (TextUtils.isEmpty(forbiddenCountCache)) {
                sendForbiddenMsgTimeCache["${mMineUid}_${mTargetUid}"] =
                    "${System.currentTimeMillis() + 5000}_${1}"
                AppLogcat.logger.d("demo", "计数器被初始化，5秒内发送的消息条数计数器置为初始值1")
            } else {
                val data = forbiddenCountCache!!.split("_")
                val time = data[0].toLong()
                val count = data[1].toInt()
                if (System.currentTimeMillis() < time) {
                    val newCount = count + 1
                    sendForbiddenMsgTimeCache["${mMineUid}_${mTargetUid}"] = "${time}_${newCount}"
                    AppLogcat.logger.d("demo", "5秒内发送的消息条数计数器+1 --->${newCount}")

                    if (newCount >= 20) {
                        sendForbiddenMaxTimeCache["${mMineUid}_${mTargetUid}"] =
                            System.currentTimeMillis() + 120000
                        AppLogcat.logger.d("demo", "5秒内发送条数大于20条，禁言两分钟")
                    }
                } else {
                    sendForbiddenMsgTimeCache["${mMineUid}_${mTargetUid}"] =
                        "${System.currentTimeMillis() + 5000}_${1}"
                    AppLogcat.logger.d("demo", "计数器被重置，5秒内发送的消息条数计数器置为初始值1")
                }
            }
            true
        }
    }

    private fun updateDynamicFace() {
        HttpManager.getStore(UserHttpProtocol::class.java)
            .getEmoticon(object : HttpReq<UserProto.GetEmoticonReq>() {
                override fun getData(): UserProto.GetEmoticonReq {
                    return UserHttpReqCreator.getEmoticon()
                }
            })
            .applyCache(
                "${mMineUid}_dynamic_face_cache",
                framework.telegram.support.system.cache.stategy.CacheStrategy.firstCache()
            )
            .getResultWithCache(lifecycle(), {
                val faces = mutableListOf<DynamicFaceBean>()
                it.data.emoticonsList.forEach { emoticon ->
                    faces.add(
                        DynamicFaceBean(
                            emoticon.emoticonId,
                            emoticon.emoticonUrl,
                            emoticon.width,
                            emoticon.height
                        )
                    )
                }

                mEmojiPopup?.addDynamicFaceIcons(faces)
            }, {
            })
    }

    private fun setUpToolsPopup() {
        mToolsPopup = ToolsPopup.Builder.fromRootView(root_view_private_chat)
            .setOnToolClickListener { index ->
                KeyboardktUtils.hideKeyboard(root_view_private_chat)
                when (index) {
                    0 -> {
                        // 选择图片
                        ImagePicker.from(this@PrivateChatActivity)
                            .choose(
                                EnumSet.of(
                                    MimeType.JPEG,
                                    MimeType.PNG,
                                    MimeType.GIF,
                                    MimeType.MP4
                                ), false
                            )
                            .addFilter(
                                Mp4SizeFilter(
                                    150 * 1024 * 1024,
                                    getString(R.string.hint),
                                    getString(R.string.maximum_file_limit_50m)
                                )
                            )
                            .countable(true)
                            .maxSelectablePerMediaType(9, 1)
                            .thumbnailScale(0.85f)
                            .originalEnable(false)
                            .showSingleMediaType(false)
                            .imageEngine(GlideEngine())
                            .forResult(TOOL_IMAGEPICKER_REQUESTCODE)
                    }
                    1 -> {
                        // 拍照
                        if (RtcEngineHolder.isActive()) {
                            if (RtcEngineHolder.streamType == 0) {
                                toast(getString(R.string.calling_audio_busy_tip))
                            } else {
                                toast(getString(R.string.calling_video_busy_tip))
                            }
                        } else {
                            val saveDir = DirManager.getCameraFileDir(
                                BaseApp.app,
                                AccountManager.getLoginAccountUUid()
                            )
                            val intent =
                                CameraActivity.getLaunchIntent(this@PrivateChatActivity, saveDir)
                            startActivityForResult(intent, TOOL_CAMERAVIEW_REQUESTCODE)
                        }
                    }
                    2 -> {
                        // 选择联系人
                        mContactInfoModel.let {
                            ARouter.getInstance()
                                .build(Constant.ARouter.ROUNTE_MSG_SELECT_CONTACT_CARD)
                                .withString("targetPic", it?.icon)
                                .withString("targetName", it?.displayName)
                                .navigation(this@PrivateChatActivity, TOOL_NAMECARD_REQUESTCODE)
                        }
                    }
                    3 -> {
                        //位置
                        ARouter.getInstance()
                            .build(Constant.ARouter.ROUNTE_LOCATION_CHOICE_ACTIVITY)
                            .navigation(
                                this@PrivateChatActivity,
                                ChoiceLocationActivity.REQUEST_CODE_SEND_LOCATION
                            )
                    }
                    4 -> {
                        //文件
                        FilePickerManager.from(this@PrivateChatActivity)
                            .forResult(FilePickerManager.REQUEST_CODE)
                    }
                }
            }
            .setOnPopupShownListener { message_input_view.faceView.setImageResource(R.drawable.msg_chat_ic_emoji) }
            .setOnSoftKeyboardOpenListener {
                recycler_view_messages.post {
                    recycler_view_messages.recyclerViewController().scrollToEnd()
                }
            }
            .setOnPopupDismissListener { message_input_view.faceView.setImageResource(R.drawable.msg_chat_ic_emoji) }
            .setOnSoftKeyboardCloseListener { }
            .build(message_input_view.inputView)
    }

    /**
     * 是否可以显示拷贝按钮
     */
    private fun isShowMsgCopy(msgType: Int): Boolean {
        return msgType == MessageModel.LOCAL_TYPE_MYSELF_TEXT || msgType == MessageModel.LOCAL_TYPE_OTHER_TEXT
    }

    /**
     * 是否可以显示转发按钮
     */
    private fun isShowMsgForward(msgType: Int): Boolean {
        return when (msgType) {
            MessageModel.LOCAL_TYPE_MYSELF_VOICE,
            MessageModel.LOCAL_TYPE_OTHER_VOICE,
            MessageModel.LOCAL_TYPE_MYSELF_NAMECARD,
            MessageModel.LOCAL_TYPE_OTHER_NAMECARD,
            MessageModel.LOCAL_TYPE_MYSELF_STREAM_MEDIA,
            MessageModel.LOCAL_TYPE_OTHER_STREAM_MEDIA,
            MessageModel.LOCAL_TYPE_MYSELF_UNKNOW,
            MessageModel.LOCAL_TYPE_OTHER_UNKNOW,
            MessageModel.LOCAL_TYPE_MYSELF_NOTICE,
            MessageModel.LOCAL_TYPE_OTHER_NOTICE,
            MessageModel.LOCAL_TYPE_MYSELF_UNDECRYPT,
            MessageModel.LOCAL_TYPE_OTHER_UNDECRYPT,
            MessageModel.LOCAL_TYPE_TIP -> {
                false
            }
            else -> {
                true
            }
        }
    }

    /**
     * 是否可以显示扬声器切换按钮
     */
    private fun isShowMsgSpeaker(msgType: Int): Boolean {
        return msgType == MessageModel.LOCAL_TYPE_MYSELF_VOICE || msgType == MessageModel.LOCAL_TYPE_OTHER_VOICE
    }

    /**
     * 是否可以显示回复按钮
     */
    private fun isShowMsgRepty(msgType: Int): Boolean {
        return if (mTargetUid == Constant.Common.FILE_TRANSFER_UID) {
            false
        } else {
            when (msgType) {
                MessageModel.LOCAL_TYPE_MYSELF_STREAM_MEDIA,
                MessageModel.LOCAL_TYPE_OTHER_STREAM_MEDIA,
                MessageModel.LOCAL_TYPE_MYSELF_UNKNOW,
                MessageModel.LOCAL_TYPE_OTHER_UNKNOW,
                MessageModel.LOCAL_TYPE_MYSELF_NOTICE,
                MessageModel.LOCAL_TYPE_OTHER_NOTICE,
                MessageModel.LOCAL_TYPE_MYSELF_UNDECRYPT,
                MessageModel.LOCAL_TYPE_OTHER_UNDECRYPT,
                MessageModel.LOCAL_TYPE_TIP -> {
                    false
                }
                else -> {
                    true
                }
            }
        }
    }

    /**
     * 是否可以显示删除按钮
     */
    private fun isShowMsgDelete(msgType: Int): Boolean {
        return msgType != MessageModel.LOCAL_TYPE_TIP
    }

    /**
     * 是否可以显示多选按钮
     */
    private fun isShowMsgSelect(msgType: Int): Boolean {
        return when (msgType) {
            MessageModel.LOCAL_TYPE_MYSELF_UNKNOW,
            MessageModel.LOCAL_TYPE_OTHER_UNKNOW,
            MessageModel.LOCAL_TYPE_MYSELF_UNDECRYPT,
            MessageModel.LOCAL_TYPE_OTHER_UNDECRYPT,
            MessageModel.LOCAL_TYPE_TIP -> {
                false
            }
            else -> {
                true
            }
        }
    }

    /**
     * 是否可以显示详情按钮
     */
    private fun isShowMsgDetail(msgType: Int, status: Int): Boolean {
        return if (mTargetUid < Constant.Common.SYSTEM_USER_MAX_UID) {
            false
        } else {
            when (msgType) {
                MessageModel.LOCAL_TYPE_MYSELF_TEXT,
                MessageModel.LOCAL_TYPE_MYSELF_VOICE,
                MessageModel.LOCAL_TYPE_MYSELF_IMAGE,
                MessageModel.LOCAL_TYPE_MYSELF_VIDEO,
                MessageModel.LOCAL_TYPE_MYSELF_LOCATION,
                MessageModel.LOCAL_TYPE_MYSELF_FILE,
                MessageModel.LOCAL_TYPE_MYSELF_NOTICE,
                MessageModel.LOCAL_TYPE_MYSELF_DYNAMIC_IMAGE,
                MessageModel.LOCAL_TYPE_MYSELF_NAMECARD -> {
                    status == MessageModel.STATUS_SENDED_HAS_RESP
                }
                else -> {
                    false
                }
            }
        }
    }

    private fun initAddFriendView() {
        linear_layout_add_friend.visibility = if (mDeleteMe) View.VISIBLE else View.GONE
    }

    private fun initFloatMenuItemList(data: MessageModel): MutableList<String> {
        val floatMenuItems = mutableListOf<String>()
        if (data.snapchatTime > 0) {
            // 未过期
            if (isShowMsgCopy(data.itemType)) {
                floatMenuItems.add(getString(R.string.copy))
            }

            if (data.type == MessageModel.MESSAGE_TYPE_VOICE) {
                if (mUseSpeaker) {
                    floatMenuItems.add(getString(R.string.the_receiver_play))
                } else {
                    floatMenuItems.add(getString(R.string.loudspeaker_playback))
                }
            }
            if (data.type == MESSAGE_TYPE_VIDEO) {
                floatMenuItems.add(getString(R.string.string_silent_play))
            }
            floatMenuItems.add(getString(R.string.delete))

            floatMenuItems.add(getString(R.string.multiple_choice))
        } else {
            val items = mutableListOf<String>()
            if (isShowMsgCopy(data.itemType)) {
                items.add(getString(R.string.copy))
            }
            if (data.type == MESSAGE_TYPE_VIDEO) {
                items.add(getString(R.string.string_silent_play))
            }

            if (isShowMsgForward(data.itemType)) {
                items.add(getString(R.string.forward))
            }

            if (isShowMsgSpeaker(data.itemType)) {
                if (mUseSpeaker) {
                    items.add(getString(R.string.the_receiver_play))
                } else {
                    items.add(getString(R.string.loudspeaker_playback))
                }
            }

            if (isShowMsgRepty(data.itemType)) {
                items.add(getString(R.string.reply))
            }

            if (isShowMsgDelete(data.itemType)) {
                items.add(getString(R.string.delete))
            }

            if (isShowMsgDetail(data.itemType, data.status)) {
                items.add(getString(R.string.details))
            }

            if (isShowMsgSelect(data.itemType)) {
                items.add(getString(R.string.multiple_choice))
            }

            floatMenuItems.addAll(items)
        }
        return floatMenuItems
    }

    private fun initOperItemList(
        msgModel: MessageModel,
        isDragging: Boolean
    ): ArrayList<OperationItem> {
        val list = arrayListOf<OperationItem>()
        val copyItem = OperationItem()
        copyItem.action = OperationItem.ACTION_COPY
        copyItem.name = resources.getString(framework.telegram.ui.R.string.copy)
        val forwardItem = OperationItem()
        forwardItem.action = OperationItem.ACTION_FORWARD
        forwardItem.name = resources.getString(framework.telegram.ui.R.string.forward)
        val replyItem = OperationItem()
        replyItem.action = OperationItem.ACTION_REPLY
        replyItem.name = resources.getString(R.string.reply)
        val deleteItem = OperationItem()
        deleteItem.action = OperationItem.ACTION_DELETE
        deleteItem.name = resources.getString(R.string.delete)
        val detailItem = OperationItem()
        detailItem.action = OperationItem.ACTION_DETAIL
        detailItem.name = resources.getString(R.string.details)
        val multiItem = OperationItem()
        multiItem.action = OperationItem.ACTION_MULTIPLE
        multiItem.name = resources.getString(R.string.multiple_choice)
        val allItem = OperationItem()
        allItem.action = OperationItem.ACTION_SELECT_ALL
        allItem.name = resources.getString(framework.telegram.ui.R.string.all)
        when {
            isDragging -> {
                if (isShowMsgCopy(msgModel.itemType)) {
                    list.add(copyItem)
                    list.add(allItem)
                }
                if (isShowMsgForward(msgModel.itemType)) {
                    list.add(forwardItem)
                }
            }
            msgModel.snapchatTime > 0 -> {
                if (isShowMsgDelete(msgModel.itemType)) {
                    list.add(deleteItem)
                }
                if (isShowMsgSelect(msgModel.itemType)) {
                    list.add(multiItem)
                }
            }
            msgModel.senderId == mTargetUid -> {
                if (isShowMsgCopy(msgModel.itemType)) {
                    list.add(copyItem)
                }
                if (isShowMsgForward(msgModel.itemType)) {
                    list.add(forwardItem)
                }
                if (isShowMsgRepty(msgModel.itemType)) {
                    list.add(replyItem)
                }
                if (isShowMsgDelete(msgModel.itemType)) {
                    list.add(deleteItem)
                }
                if (isShowMsgSelect(msgModel.itemType)) {
                    list.add(multiItem)
                }
            }
            else -> {
                if (isShowMsgCopy(msgModel.itemType)) {
                    list.add(copyItem)
                }
                if (isShowMsgForward(msgModel.itemType)) {
                    list.add(forwardItem)
                }
                if (isShowMsgRepty(msgModel.itemType)) {
                    list.add(replyItem)
                }
                if (isShowMsgDelete(msgModel.itemType)) {
                    list.add(deleteItem)
                }
                if (isShowMsgDetail(msgModel.itemType, msgModel.status)) {
                    list.add(detailItem)
                }
                if (isShowMsgSelect(msgModel.itemType)) {
                    list.add(multiItem)
                }
            }
        }
        return list
    }

    override fun showAddFriendMsg() {
        toast(getString(R.string.add_ok))
        mDeleteMe = false
        initAddFriendView()
        mPresenter?.updataFriendShip(mMineUid, mTargetUid, false, {}) {}
    }

    override fun getAddToken(userId: Long, addToken: String) {
        ARouter.getInstance().build(ROUNTE_BUS_CONTACT_DETAIL)
            .withLong(KEY_TARGET_UID, userId)
            .withString(KEY_ADD_TOKEN, addToken)
            .withSerializable(KEY_ADD_FRIEND_FROM, ContactsProto.ContactsAddType.REQ_MSG)
            .navigation()
    }

    override fun showErrorMsg(code: Int, errStr: String?) {
        if (code == 5105) {
            mContactInfoModel?.let {
                ARouter.getInstance()
                    .build(ROUNTE_BUS_CONTACTS_VERIFY_INFO_EDIT)
                    .withSerializable(
                        KEY_ADD_FRIEND_FROM, ContactsProto.ContactsAddType.REQ_MSG
                    )
                    .withString(
                        framework.telegram.business.bridge.Constant.ARouter_Key.KEY_ADD_TOKEN,
                        errStr
                    )
                    .withLong(
                        framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_UID,
                        mTargetUid
                    ).navigation()
            }
        } else {
            toast(errStr.toString())
        }
    }

    private fun checkFriendship() {
        mPresenter?.checkFriendShip(mMineUid, mTargetUid, {
            mDeleteMe = it
            initAddFriendView()
        }) {}
    }
}