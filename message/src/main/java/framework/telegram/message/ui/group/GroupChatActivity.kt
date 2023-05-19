package framework.telegram.message.ui.group

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
import android.net.Uri
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
import android.view.animation.AnimationUtils
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
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.group.GroupMemberModel
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.ideas.common.model.im.MessageModel.MESSAGE_TYPE_VIDEO
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_CONTACT_DETAIL
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_ADD_FRIEND_FROM
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_ADD_TOKEN
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_GID
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_UID
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.BuildConfig
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_FORWARD_CHATS
import framework.telegram.message.bridge.event.*
import framework.telegram.message.controller.DownloadAttachmentController
import framework.telegram.message.controller.MessageController
import framework.telegram.message.controller.UploadAttachmentController
import framework.telegram.message.event.DynamicFaceUpdateEvent
import framework.telegram.message.http.HttpManager
import framework.telegram.message.http.creator.UserHttpReqCreator
import framework.telegram.message.http.getResultWithCache
import framework.telegram.message.http.protocol.UserHttpProtocol
import framework.telegram.message.manager.*
import framework.telegram.message.sp.CommonPref
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
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.*
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.file.DirManager
import framework.telegram.support.tools.framework.telegram.support.UriUtils
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
import io.realm.RealmResults
import kotlinx.android.synthetic.main.msg_activity_group_chat.*
import kotlinx.android.synthetic.main.msg_activity_group_chat.custom_toolbar
import kotlinx.android.synthetic.main.msg_activity_group_chat.emoji_text_view_reply_content
import kotlinx.android.synthetic.main.msg_activity_group_chat.emoji_text_view_reply_user_nickname
import kotlinx.android.synthetic.main.msg_activity_group_chat.image_view_delete_check_msg
import kotlinx.android.synthetic.main.msg_activity_group_chat.image_view_fire_bg
import kotlinx.android.synthetic.main.msg_activity_group_chat.image_view_new_msg
import kotlinx.android.synthetic.main.msg_activity_group_chat.image_view_reply_close
import kotlinx.android.synthetic.main.msg_activity_group_chat.image_view_send_check_msg
import kotlinx.android.synthetic.main.msg_activity_group_chat.layout_check_msg
import kotlinx.android.synthetic.main.msg_activity_group_chat.layout_new_msg
import kotlinx.android.synthetic.main.msg_activity_group_chat.layout_reply_msg
import kotlinx.android.synthetic.main.msg_activity_group_chat.message_input_view
import kotlinx.android.synthetic.main.msg_activity_group_chat.recycler_view_messages
import kotlinx.android.synthetic.main.msg_activity_group_chat.text_view_cancel_check_msg
import kotlinx.android.synthetic.main.msg_activity_group_chat.text_view_check_msg_title
import kotlinx.android.synthetic.main.msg_activity_group_chat.text_view_clear_msg
import kotlinx.android.synthetic.main.msg_activity_group_chat.text_view_new_msg
import kotlinx.android.synthetic.main.msg_activity_pvt_chat.*
import kotlinx.android.synthetic.main.msg_recording_layout.*
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

@Route(path = Constant.ARouter.ROUNTE_MSG_GROUP_CHAT_ACTIVITY)
class GroupChatActivity : BaseActivity(), GroupChatContract.View, SensorEventListener {

    companion object {
        internal const val TAG = "GroupChatActivity"
        internal const val GET_PERMISSIONS_REQUEST_CODE = 100
        internal const val TOOL_IMAGEPICKER_REQUESTCODE = 0x1000
        internal const val TOOL_CAMERAVIEW_REQUESTCODE = 0x2000
        internal const val AT_USERS_REQUESTCODE = 0x3000
        internal const val TOOL_NAMECARD_REQUESTCODE = 0x4000

        private val lastUpdateGroupInfoTimes by lazy { HashMap<Long, Long>() }

        private val sendForbiddenMaxTimeCache by lazy { HashMap<String, Long>() }
        private val sendForbiddenMsgTimeCache by lazy { HashMap<String, String>() }
    }

    private var mPresenter: GroupChatPresenterImpl? = null
    private var mEmojiPopup: IconFacePopup? = null
    private var mToolsPopup: ToolsPopup? = null

    private var mGroupIconView: AppImageView? = null
    private var mGroupNameView: TextView? = null
    private var mTitleView: TextView? = null
    private var mUnreadMessageCountView: TextView? = null

    private val mLinearLayoutManager by lazy { LinearLayoutManager(this@GroupChatActivity) }

    private val mMessageAdapter by lazy {
        MessageAdapter(
            mLinearLayoutManager,
            WeakReference(this@GroupChatActivity),
            message_input_view,
            ChatModel.CHAT_TYPE_GROUP,
            mTargetGid
        )
    }

    private val mMineUid by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
    }

    private var mLocalMsgUid = 0L

    var mTargetGid = 0L

    private val mSensorManager by lazy { BaseApp.app.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    private val mSensor by lazy { mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) }

    private val mDefaultVolumeControlStream by lazy { volumeControlStream }

    private var mGroupInfoModel: GroupInfoModel? = null

    private var mRefMessage: MessageModel? = null

    private var mUseSpeaker = false

    private var isFirstLoadMessages = true

    private var mLoadAllMessageBeforeFirstMsgLocalId = 0L

    private var mMemberCount = 0

    private var isActivityPause = false

    private var floatMenu: FloatMenu? = null

    private var mContent: Editable? = null

    private var mPv: OptionsPickerView<String>? = null

    private val mAccountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mTargetGid = intent.getLongExtra("targetGid", 0L)
        if (mMineUid <= 0 || mTargetGid <= 0) {
            Toast.makeText(applicationContext, getString(R.string.gid_error), Toast.LENGTH_SHORT)
                .show()
            finish()
            return
        }

        setContentView(R.layout.msg_activity_group_chat)
        AndroidBug5497Workaround.assistActivity(this@GroupChatActivity)

        // 获取扬声器设置
        mUseSpeaker = ArouterServiceManager.settingService.getDefaultUseSpeaker()

        initView()
        initData()

        DownloadAttachmentController.attachDownloadListener(mMessageAdapter)
        UploadAttachmentController.attachUploadListener(mMessageAdapter)
        mMessageAdapter.audioPlayController.attachListener()

        registerEventBus()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        mTargetGid = intent?.getLongExtra("targetGid", 0L) ?: 0L

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

        GroupChatPresenterImpl(
            this@GroupChatActivity,
            this@GroupChatActivity,
            lifecycle(),
            mTargetGid
        ).start(mLocalMsgUid > 0)

        refreshGroupInfo(syncGroupInfo = true, syncMembers = true)
    }

    private fun locationMsg(intent: Intent?) {
        mLocalMsgUid = intent?.getLongExtra("localMsgId", 0L) ?: 0L
    }

    @SuppressLint("CheckResult")
    private fun registerEventBus() {

        EventBus.getFlowable(DynamicFaceUpdateEvent::class.java)
            .bindToLifecycle(this@GroupChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                updateDynamicFace()
            }

        EventBus.getFlowable(RecallMessageEvent::class.java)
            .bindToLifecycle(this@GroupChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.chatType == ChatModel.CHAT_TYPE_GROUP && it.targetId == mTargetGid) {
                    floatMenu?.dismiss()
                }
            }

        EventBus.getFlowable(AtMeMessageEvent::class.java)
            .bindToLifecycle(this@GroupChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.groupId == mTargetGid) {
                    // 查询@me的所有消息完成后的新来@me消息，都必须置isAtMe为0
                    MessagesManager.updateMessagesAtMeStatus(mMineUid, mTargetGid, {
                        ChatsHistoryManager.clearChatAtMeCount(
                            mMineUid,
                            ChatModel.CHAT_TYPE_GROUP,
                            mTargetGid,
                            {
                                EventBus.publishEvent(ChatHistoryChangeEvent())
                            })
                    })
                }
            }

        EventBus.getFlowable(ReciveMessageEvent::class.java)
            .bindToLifecycle(this@GroupChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.chaterType == ChatModel.CHAT_TYPE_GROUP && it.chaterId == mTargetGid) {
                    if (ActivitiesHelper.getInstance()
                            .toForeground() && mGroupInfoModel?.bfDisturb != true
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

        EventBus.getFlowable(GroupMemberChangeEvent::class.java)
            .bindToLifecycle(this@GroupChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.groupId == mTargetGid) {
                    refreshGroupMemberList()
                }
            }

        EventBus.getFlowable(GroupInfoChangeEvent::class.java)
            .bindToLifecycle(this@GroupChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.groupId == mTargetGid) {
                    refreshGroupInfo(syncGroupInfo = true, syncMembers = false)
                }
            }

        EventBus.getFlowable(SearchChatEvent::class.java)
            .bindToLifecycle(this@GroupChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.chatType == ChatModel.CHAT_TYPE_GROUP && it.targetId == mTargetGid) {
                    jumpToTargetMessageByMsgLocalId(it.messageLocalId)
                }
            }

        EventBus.getFlowable(SocketStatusChangeEvent::class.java)
            .bindToLifecycle(this@GroupChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                showSocketContactStatus()
            }

        EventBus.getFlowable(UnreadMessageEvent::class.java)
            .bindToLifecycle(this@GroupChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.targetType != ChatModel.CHAT_TYPE_GROUP || it.targetId != mTargetGid) {
                    showUnreadMessageCount()
                }
            }

        EventBus.getFlowable(GroupShutupEvent::class.java)
            .bindToLifecycle(this@GroupChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.groupId == mTargetGid) {
                    ArouterServiceManager.groupService.updateGroupInfoByCache(
                        lifecycle(),
                        mTargetGid,
                        { groupInfoModel ->
                            mGroupInfoModel = groupInfoModel
                            refreshShutupStatus()
                        })
                }
            }

        EventBus.getFlowable(BanGroupMessageEvent::class.java)
            .bindToLifecycle(this@GroupChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.groupId == mTargetGid) {
                    finish()
                }
            }

        EventBus.getFlowable(DisableGroupMessageEvent::class.java)
            .bindToLifecycle(this@GroupChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.groupId == mTargetGid) {
                    if (ActivitiesHelper.getInstance().topActivity == this@GroupChatActivity) {
                        AppDialog.show(this@GroupChatActivity, this@GroupChatActivity) {
                            positiveButton(text = getString(R.string.confirm), click = {
                                //清空聊天记录
                                finish()
                            })
                            cancelOnTouchOutside(false)
                            message(text = getString(R.string.string_group_dismiss_title))
                        }
                    } else {
                        finish()
                    }
                }
            }
    }

    override fun getAdapterItemCount(): Int {
        return mMessageAdapter.itemCount
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as GroupChatPresenterImpl
    }

    override fun getVisibleItemCount(): Int {
        val layoutManager =
            recycler_view_messages.recyclerViewController().layoutManager as LinearLayoutManager
        return layoutManager.findLastVisibleItemPosition() - layoutManager.findFirstVisibleItemPosition()
    }

    override fun getAddToken(userId: Long, addToken: String) {
        ARouter.getInstance().build(ROUNTE_BUS_CONTACT_DETAIL)
            .withLong(KEY_TARGET_UID, userId)
            .withString(KEY_ADD_TOKEN, addToken)
            .withSerializable(KEY_ADD_FRIEND_FROM, ContactsProto.ContactsAddType.REQ_MSG)
            .navigation()
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

    private fun showGroupNoice() {
        mGroupInfoModel?.noticeId?.let { noticeId ->
            val storageName = "group_notice_${mMineUid}_$mTargetGid"
            val commonPref =
                SharePreferencesStorage.createStorageInstance(CommonPref::class.java, storageName)
            val lastShowGroupNoticeId = commonPref.getLastShowGroupNotice()
            if (lastShowGroupNoticeId != noticeId && noticeId > 0) {
                emoji_text_view_notice_content.text = mGroupInfoModel?.notice
                layout_group_notice.visibility = View.VISIBLE

                val anim =
                    AnimationUtils.loadAnimation(this@GroupChatActivity, R.anim.group_notice_anim)
                anim.fillAfter = true
                layout_group_notice.startAnimation(anim)

                layout_group_notice.setOnClickListener {
                    commonPref.putLastShowGroupNotice(noticeId)
                    ARouter.getInstance()
                        .build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_GROUP_NOTICE)
                        .withLong("groupId", mGroupInfoModel?.groupId!!)
                        .withLong("noticeId", mGroupInfoModel?.noticeId!!)
                        .withBoolean("bfPushNotice", false).navigation()
                }
                text_view_group_notice_close.setOnClickListener {
                    commonPref.putLastShowGroupNotice(noticeId)
                    layout_group_notice.visibility = View.GONE
                    layout_group_notice.clearAnimation()
                }
            }
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

    private fun showSocketContactStatus() {
        if (ReceiveMessageManager.socketIsLogin) {
            mGroupNameView?.visibility = View.VISIBLE
            mGroupIconView?.visibility = View.VISIBLE
            mTitleView?.visibility = View.GONE
        } else {
            mGroupNameView?.visibility = View.GONE
            mGroupIconView?.visibility = View.GONE
            mTitleView?.visibility = View.VISIBLE

            if (NetworkUtils.isAvailable(BaseApp.app)) {
                //连接中...
                showCenterTitle(getString(R.string.extern_nsstring_const))
            } else {
                //未连接
                showCenterTitle(getString(R.string.ununited))
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

    private fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back, {
            val size = ScreenUtils.dp2px(this@GroupChatActivity, 2f)
            it.setPadding(0, 0, size, 0)
        }) {
            finish()
        }

        custom_toolbar.showLeftTextView("", {
            gotoGroupSetting()
        }) {
            mGroupNameView = it
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            it.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        }

        custom_toolbar.showLeftImageView(
            uri = Uri.EMPTY,
            height = 28f,
            width = 28f,
            onClickCallback = {
                gotoGroupSetting()
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
                            this@GroupChatActivity,
                            R.color.edeff2
                        )
                    )
                )
                it.hierarchy.fadeDuration = 300
                (it.layoutParams as LinearLayout.LayoutParams).setMargins(
                    ScreenUtils.dp2px(this@GroupChatActivity, 16f), 0,
                    ScreenUtils.dp2px(this@GroupChatActivity, 8f), 0
                )
                mGroupIconView = it
            })

        custom_toolbar.showLeftTextView("", {
            finish()
        }, listen = {
            it.layoutParams.height = ScreenUtils.dp2px(this@GroupChatActivity, 16f)
            (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 10, 0, 10)
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            it.setTextColor(ContextCompat.getColor(this@GroupChatActivity, R.color.white))
            it.setBackgroundResource(R.drawable.common_corners_trans_f50d2e_16_0)
            it.minWidth = ScreenUtils.dp2px(this@GroupChatActivity, 16f)
            it.setPadding(5, 0, 5, 0)

            mUnreadMessageCountView = it
            mUnreadMessageCountView?.visibility = View.GONE
        })

        layout_at_me.setOnClickListener {
            mPresenter?.clickAtMeButton()
        }

        // set buttons
        message_input_view.inputView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                mEmojiPopup?.dismiss()
                mToolsPopup?.dismiss()
            }
            false
        }
        message_input_view.inputView.setOnMentionInputListener {
            //跳转到@人的页面
            ARouter.getInstance()
                .build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_OPERATE)
                .withInt("operateType", 2)
                .withInt("groupPermission", mGroupInfoModel?.memberRole ?: 2)
                .withLong("groupId", mTargetGid)
                .navigation(this, AT_USERS_REQUESTCODE)
        }

        message_input_view.buttonsListener = object : MessageInputView.ButtonsListener {
            override fun onClickFire() {
                if (message_input_view.isRecording) {
                    return
                }
                if ((mGroupInfoModel?.memberRole ?: 2) >= 2) {
                    toast(getString(R.string.only_the_administrator_can_change_the_burn_time))
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

            override fun onClickSend(uids: List<Long>, msg: String?) {
                if (!TextUtils.isEmpty(msg)) {
                    if (checkSendFrequency()) {
                        mPresenter?.sendTextMessage(
                            msg
                                ?: "", uids, mRefMessage, mMineUid, mTargetGid, mGroupInfoModel
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
                        mTargetGid,
                        mGroupInfoModel
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
                return "group_draft_${mMineUid}_$mTargetGid"
            }

            override fun onInputHeightChange(height: Int) {
                var layoutParams = fl_message.layoutParams as RelativeLayout.LayoutParams
                layoutParams.bottomMargin = height
                fl_message.layoutParams = layoutParams

                layoutParams = layout_at_me.layoutParams as RelativeLayout.LayoutParams
                layoutParams.bottomMargin =
                    height + ScreenUtils.dp2px(this@GroupChatActivity, 32.0f)
                layout_at_me.layoutParams = layoutParams

                layoutParams = layout_reply_msg.layoutParams as RelativeLayout.LayoutParams
                layoutParams.bottomMargin = height
                layout_reply_msg.layoutParams = layoutParams
            }

            override fun onInputing() {

            }
        }

        // set recyclerview
        mMessageAdapter.chaterId = mTargetGid
        mMessageAdapter.isSpeakerphoneOn = mUseSpeaker
        mMessageAdapter.setOnItemChildClickListener { _, view, position ->
            val data = mMessageAdapter.getItem(position)
            data?.let {
                if (view.id == R.id.app_image_view_icon || view.id == R.id.text_view_nickname) {
                    ArouterServiceManager.groupService.getGroupMemberRole(
                        lifecycle(),
                        mTargetGid,
                        it.ownerUid,
                        { role ->
                            val isForbidJoinFriend =
                                if (mGroupInfoModel?.forbidJoinFriend == true) {
                                    mGroupInfoModel?.memberRole == 2 && role == 2
                                } else false
                            var displayName = it.ownerName
                            val groupMemberInfo = mMessageAdapter.mMessageOwnerList[it.ownerUid]
                            if (groupMemberInfo != null && groupMemberInfo is GroupMemberModel) {
                                displayName = groupMemberInfo.groupNickName
                            }
                            ARouter.getInstance().build(ROUNTE_BUS_CONTACT_DETAIL)
                                .withLong(KEY_TARGET_GID, mTargetGid)
                                .withBoolean("isForbidJoinFriend", isForbidJoinFriend)
                                .withSerializable(
                                    KEY_ADD_FRIEND_FROM,
                                    ContactsProto.ContactsAddType.CROWD
                                )
                                .withLong(KEY_TARGET_UID, data.ownerUid)
                                .withString(
                                    framework.telegram.business.bridge.Constant.ARouter_Key.KEY_GROUP_NICKNAME,
                                    displayName
                                )
                                .navigation()
                        })
                } else if (view.id == R.id.image_view_pause) {
                    if (data.type == MessageModel.MESSAGE_TYPE_FILE
                        || data.type == MessageModel.MESSAGE_TYPE_VIDEO
                        || data.type == MessageModel.MESSAGE_TYPE_IMAGE
                        || data.type == MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE
                    ) {
                        if (data.isSend == 0) {
                            // 接收方一定是下载
                            DownloadAttachmentController.cancelDownload(
                                data.fileMessageContentBean.fileUri,
                                ChatModel.CHAT_TYPE_GROUP,
                                data
                            )
                        } else {
                            // 发送方可能是下载也可能是上传
                            DownloadAttachmentController.cancelDownload(
                                data.fileMessageContentBean.fileUri,
                                ChatModel.CHAT_TYPE_GROUP,
                                data
                            )
                            UploadAttachmentController.cancelUpload(
                                ChatModel.CHAT_TYPE_GROUP,
                                mMineUid,
                                mTargetGid,
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
                                ChatModel.CHAT_TYPE_GROUP,
                                data.copyMessage()
                            )
                        } else {

                        }
                    } else {

                    }
                } else if (view.id == R.id.emoji_text_view) {
                    val lastTime = view.getTag(R.id.doubleCheckId)
                    val nowTime = System.currentTimeMillis()
                    if (it.type != MessageModel.MESSAGE_TYPE_NOTICE) {
                        lastTime?.let {
                            val checkTime = nowTime - (lastTime) as Long
                            if (checkTime in 1..199) {
                                KeyboardktUtils.hideKeyboard(root_view_group_chat)
                                ARouter.getInstance()
                                    .build(Constant.ARouter.ROUNTE_MSG_PREVIEW_TEXT)
                                    .withString("text", data.textMessageContent)
                                    .withLong("msgId", data.msgId)
                                    .withLong("targetUid", mTargetGid)
                                    .withBoolean("copyable", data.snapchatTime == 0)
                                    .navigation()
                            }
                        }

                    } else {
                        lastTime?.let {
                            val checkTime = nowTime - (lastTime) as Long
                            if (checkTime > 200) {
                                ARouter.getInstance()
                                    .build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_GROUP_NOTICE)
                                    .withLong("groupId", mGroupInfoModel?.groupId!!)
                                    .withLong("noticeId", data.noticeMessageBean.noticeId)
                                    .withBoolean("bfPushNotice", false).navigation()
                            }
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
                                    .withInt("chatType", ChatModel.CHAT_TYPE_GROUP)
                                    .withLong("targetId", targetId)
                                    .withBoolean("group", true)
                                    .navigation()
                            } else {
                                ARouter.getInstance()
                                    .build(Constant.ARouter.ROUNTE_MSG_PREVIEW_BRIDGE_ACTIVITY)
                                    .withLong("messageLocalId", it.id)
                                    .withInt("chatType", ChatModel.CHAT_TYPE_GROUP)
                                    .withLong("targetId", targetId)
                                    .navigation()
                            }
                        }
                        MessageModel.MESSAGE_TYPE_NAMECARD -> {
                            mPresenter?.getAddToken(
                                data.nameCardMessageContent.identify,
                                data.nameCardMessageContent.uid,
                                MD5.md5("${data.nameCardMessageContent.uid}|${data.nameCardMessageContent.icon}|${data.nameCardMessageContent.nickName}")
                            )
                        }
                        MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE -> {
                            ARouter.getInstance()
                                .build(Constant.ARouter.ROUNTE_MSG_PREVIEW_GIF_ACTIVITY)
                                .withLong("messageLocalId", it.id)
                                .withInt("chatType", ChatModel.CHAT_TYPE_GROUP)
                                .withLong("targetId", mTargetGid)
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
                        MessageModel.MESSAGE_TYPE_FILE -> {
                            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_PREVIEW_FILE)
                                .withLong("messageLocalId", data.id)
                                .withInt("chatType", ChatModel.CHAT_TYPE_GROUP)
                                .withLong("targetId", mTargetGid)
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
                        MessageModel.MESSAGE_TYPE_NOTICE -> {
                            ARouter.getInstance()
                                .build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_GROUP_NOTICE)
                                .withLong("groupId", mGroupInfoModel?.groupId!!)
                                .withLong("noticeId", it.noticeMessageBean.noticeId)
                                .withBoolean("bfPushNotice", false).navigation()
                        }
                        MessageModel.MESSAGE_TYPE_UNDECRYPT -> {
                            AppDialog.show(this@GroupChatActivity, this@GroupChatActivity) {
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
            view.setTag(R.id.long_click, true)
            val data = mMessageAdapter.getItem(position)
            data?.let {
                if (view.id == R.id.app_image_view_icon) {
                    val ownerUid = data.ownerUid
                    ArouterServiceManager.groupService.getGroupMemberInfo(
                        lifecycle(),
                        mTargetGid,
                        ownerUid,
                        { groupMemberInfo, _ ->
                            message_input_view.inputView.mentionUserWithOutAt(
                                groupMemberInfo.uid,
                                "@" + groupMemberInfo.displayAtMeName
                            )
                            KeyboardktUtils.showKeyboard(message_input_view.inputView)
                        })
                    true
                } else {
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
                                        forwardMsg(it.id, mTargetGid)
                                    else if (view.selectedText.isNotEmpty())
                                        forwardMsg(view.selectedText)
                                    dismissSelectableText(view)
                                }
                                OperationItem.ACTION_CANCEL -> dismissSelectableText(
                                    view
                                )
                                OperationItem.ACTION_DELETE -> {
                                    deleteMsg(listOf(it))
                                    dismissSelectableText(view)
                                }
                                OperationItem.ACTION_MULTIPLE -> setCheckableMessage(
                                    it
                                )
                                OperationItem.ACTION_REPLY -> {
                                    replyMsg(it)
                                    dismissSelectableText(view)
                                }
                                OperationItem.ACTION_DETAIL -> {
                                    ARouter.getInstance()
                                        .build(Constant.ARouter.ROUNTE_MSG_DETAIL_ACTIVITY)
                                        .withInt("chatType", ChatModel.CHAT_TYPE_GROUP)
                                        .withLong("messageLocalId", it.id)
                                        .withLong("targetId", mTargetGid)
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
                        floatMenu = FloatMenu(this@GroupChatActivity)
                        val floatMenuItems = initFloatMenuItemList(it)
                        if (floatMenuItems.isNotEmpty()) {
                            floatMenu?.items(*floatMenuItems.toTypedArray())
                            floatMenu?.setOnItemClickListener { _, text ->
                                when (text) {
                                    getString(R.string.copy) -> {
                                        Helper.setPrimaryClip(BaseApp.app, it.textMessageContent)
                                    }
                                    getString(R.string.forward) -> {
                                        forwardMsg(it.id, mTargetGid)
                                    }
                                    getString(R.string.reply) -> {
                                        replyMsg(it)
                                    }
                                    getString(R.string.delete) -> {
                                        deleteMsg(listOf(it))
                                    }
                                    getString(R.string.details) -> {
                                        ARouter.getInstance()
                                            .build(Constant.ARouter.ROUNTE_MSG_DETAIL_ACTIVITY)
                                            .withInt("chatType", ChatModel.CHAT_TYPE_GROUP)
                                            .withLong("messageLocalId", it.id)
                                            .withLong("targetId", mTargetGid)
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
                                                .withInt("chatType", ChatModel.CHAT_TYPE_GROUP)
                                                .withLong("targetId", targetId)
                                                .withBoolean("group", true)
                                                .withBoolean("isSilentPlay", true)
                                                .navigation()
                                        } else {
                                            ARouter.getInstance()
                                                .build(Constant.ARouter.ROUNTE_MSG_PREVIEW_BRIDGE_ACTIVITY)
                                                .withLong("messageLocalId", it.id)
                                                .withInt("chatType", ChatModel.CHAT_TYPE_GROUP)
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
                        true
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


        val headerView = LayoutInflater.from(this@GroupChatActivity)
            .inflate(R.layout.msg_chat_messages_head_tip, null)
        headerView.setOnClickListener {
            AppDialog.show(this@GroupChatActivity, this@GroupChatActivity) {
                message(text = getString(R.string.encryption_hint_two))
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

    override fun onBackPressed() {
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


    private fun showSelectableText(view: View) {
        if (view is SelectableTextView) {
            view.isShowOperateView(true)
            view.showAndSelectAll()
        }
    }

    private fun showSelectableTextOperView(view: View) {
        if (view is SelectableTextView) {
            view.isShowOperateView(true)
        }
    }

    private fun dismissSelectableText(view: View) {
        if (view is SelectableTextView) {
            view.isShowOperateView(false)
            view.hideAndUnSelectAll()
        }
    }

    private fun dimissSelectableTextOperView(view: View) {
        if (view is SelectableTextView) {
            view.isShowOperateView(false)
        }
    }

    private fun dismissCheckMessages() {
        mMessageAdapter.setUnCheckable()
        layout_check_msg.visibility = View.GONE

        message_input_view.inputView.text = mContent
        mContent = null
    }

    private fun lightCheckMsgView() {
        image_view_delete_check_msg.isEnabled = true
        image_view_delete_check_msg.setImageResource(R.drawable.msg_icon_delete)
        image_view_send_check_msg.isEnabled = true
        image_view_send_check_msg.setImageResource(R.drawable.msg_chat_ic_send)
    }

    private fun grayCheckMsgView() {
        image_view_delete_check_msg.isEnabled = false
        image_view_delete_check_msg.setImageResource(R.drawable.delete_gray)
        image_view_send_check_msg.isEnabled = false
        image_view_send_check_msg.setImageResource(R.drawable.send_gray)
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

            forwardMsgs(ids, mTargetGid)

            dismissCheckMessages()
        }

        layout_check_msg.visibility = View.VISIBLE
        mMessageAdapter.setCheckable(msg) { msgCount ->
            text_view_check_msg_title.text = "$msgCount"

            if (msgCount == 0) {
                grayCheckMsgView()
            } else {
                lightCheckMsgView()
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
            .withInt("chatType", ChatModel.CHAT_TYPE_GROUP)
            .withLong("targetId", targetId)
            .navigation()
    }

    private fun forwardMsg(id: Long, targetId: Long) {
        ARouter.getInstance().build(ROUNTE_MSG_FORWARD_CHATS)
            .withLong("messageLocalId", id)
            .withInt("chatType", ChatModel.CHAT_TYPE_GROUP)
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
            val name =
                if (TextUtils.isEmpty(mGroupInfoModel?.groupNickName)) AccountManager.getLoginAccount(
                    AccountInfo::class.java
                ).getNickName() else mGroupInfoModel?.groupNickName
                    ?: ""
            setRefMessage(getString(R.string.you), name, messageModel)
        } else {
            // 回复他人
            ArouterServiceManager.contactService.getContactInfo(
                lifecycle(),
                messageModel.ownerUid,
                { contactModel, _ ->
                    val name =
                        if (TextUtils.isEmpty(mGroupInfoModel?.groupNickName)) contactModel.nickName else mGroupInfoModel?.groupNickName
                            ?: ""
                    setRefMessage(contactModel.displayName, name, messageModel)
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
        if (mGroupInfoModel?.memberRole ?: 2 > 1) {
            // 成员
            deleteAllLocalImpl()
        } else {
            // 群主或管理员
            deleteAllImpl()
        }
    }

    private fun deleteMsg(datas: List<MessageModel>) {
        if (mGroupInfoModel?.memberRole ?: 2 > 1) {
            // 群成员
            if (datas.size == 1 && datas[0].isSend == 1) {
                // 单条，且是自己发的
                deleteDatasImpl(datas)
            } else {
                // 多条
                deleteDatasLocalImpl(datas)
            }
        } else {
            // 群主或群管理
            deleteDatasImpl(datas)
        }
    }

    private fun deleteAllLocalImpl() {
        AppDialog.showBottomListView(
            this,
            this,
            listOf(
                getString(R.string.clear_and_delete_msgs_local),
                getString(R.string.cancel)
            )
        ) { dialog, index, _ ->
            when (index) {
                0 -> {
                    // 删除
                    mMessageAdapter.audioPlayController.stopPlayVoice()
                    MessageController.clearMessageHistory(
                        ChatModel.CHAT_TYPE_GROUP,
                        mTargetGid
                    )
                }
                else -> {
                    dialog.dismiss()
                }
            }
        }
    }

    private fun deleteAllImpl() {
        AppDialog.showBottomListView(
            this,
            this,
            listOf(
                getString(R.string.clear_and_delete_msgs_local),
                getString(R.string.clear_and_delete_group_msgs_local),
                getString(R.string.cancel)
            )
        ) { dialog, index, _ ->
            when (index) {
                0 -> {
                    // 删除
                    mMessageAdapter.audioPlayController.stopPlayVoice()
                    MessageController.clearMessageHistory(
                        ChatModel.CHAT_TYPE_GROUP,
                        mTargetGid
                    )
                }
                1 -> {
                    // 撤回
                    mMessageAdapter.audioPlayController.stopPlayVoice()
                    MessageController.recallMessages(
                        ChatModel.CHAT_TYPE_GROUP,
                        mMineUid,
                        mTargetGid,
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

    private fun deleteDatasLocalImpl(datas: List<MessageModel>) {
        AppDialog.showBottomListView(
            this,
            this,
            listOf(
                getString(R.string.clear_and_delete_msgs),
                getString(R.string.cancel)
            )
        ) { dialog, index, _ ->
            when (index) {
                0 -> {
                    // 删除
                    datas.forEach { data ->
                        val id = data.id
                        val isVoice = data.type == MessageModel.MESSAGE_TYPE_VOICE
                        if (isVoice) {
                            mMessageAdapter.audioPlayController.stopPlayVoice(data)
                        }

                        UploadAttachmentController.cancelUpload(
                            ChatModel.CHAT_TYPE_GROUP,
                            mMineUid,
                            mTargetGid,
                            id
                        )
                        MessageController.deleteMessage(
                            ChatModel.CHAT_TYPE_GROUP,
                            mMineUid,
                            mTargetGid,
                            id
                        )
                    }
                }
                else -> {
                    dialog.dismiss()
                }
            }
        }
    }

    private fun deleteDatasImpl(datas: List<MessageModel>) {
        AppDialog.showBottomListView(
            this,
            this,
            listOf(
                getString(R.string.clear_and_delete_msgs),
                getString(R.string.clear_and_delete_group_msgs),
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
                        ChatModel.CHAT_TYPE_GROUP,
                        mMineUid,
                        mTargetGid,
                        id
                    )
                }
            }

            when (index) {
                0 -> {
                    // 删除
                    datas.forEach { data ->
                        MessageController.deleteMessage(
                            ChatModel.CHAT_TYPE_GROUP,
                            mMineUid,
                            mTargetGid,
                            data.id
                        )
                    }
                }
                1 -> {
                    // 撤回
                    datas.forEach { data ->
                        val id = data.id
                        val msgId = data.msgId
                        if (msgId > 0) {
                            MessageController.recallMessage(
                                ChatModel.CHAT_TYPE_GROUP,
                                mMineUid,
                                mTargetGid,
                                msgId
                            )
                        } else {
                            MessageController.deleteMessage(
                                ChatModel.CHAT_TYPE_GROUP,
                                mMineUid,
                                mTargetGid,
                                id
                            )
                        }
                    }
                }
                2 -> {
                    dialog.dismiss()
                }
            }
        }
    }

    private val SMOOTHSCROLLSTEP = 10

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

    private fun scoldPosition(adapterPosition: Int) {
        if (adapterPosition > -1) {
            val recyclerView = recycler_view_messages.recyclerViewController().recyclerView
            val linearManager = recyclerView.layoutManager as LinearLayoutManager
            val lastIndex = linearManager.findLastVisibleItemPosition()//下边界
            val firstIndex = linearManager.findFirstVisibleItemPosition()//上边界
            when {
                adapterPosition < firstIndex - SMOOTHSCROLLSTEP -> {//要向上滚,上区间 + 想上10个单位
                    recyclerView.smoothScrollToPosition(firstIndex - SMOOTHSCROLLSTEP)
                    flashScrollToPosition(recyclerView) {
                        recycler_view_messages.recyclerViewController()
                            .scrollToPosition(adapterPosition)
                        Handler().postDelayed({
                            flashView(adapterPosition)
                        }, 500)
                    }

                }
                adapterPosition > lastIndex + SMOOTHSCROLLSTEP -> {//要向下滚,下区间 + 想下10个单位
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
                    recyclerView.smoothScrollToPosition(adapterPosition)
                    Handler().postDelayed({
                        flashView(adapterPosition)
                    }, 500)
                }
            }
        }
    }

    private fun flashView(adapterPosition: Int) {
        val holder =
            recycler_view_messages.recyclerView.findViewHolderForAdapterPosition(adapterPosition)
        if (holder is BaseViewHolder) {
            val warpLayout = holder.getView<RelativeLayout>(R.id.warp_layout)
            warpLayout?.let { warpView ->
                val view = holder.getView<FlashRelativeLayout>(R.id.flash_layout)
                view.layoutParams.width = warpView.width
                view.layoutParams.height = warpView.height
                view?.let {
                    it.flash(warpView.width, warpView.height)
                }
            }
        }
    }

    private fun flashScrollToPosition(
        recyclerView: RecyclerView,
        idleCall: (() -> Unit)? = null
    ) {
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

    @SuppressLint("CheckResult")
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
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun refreshGroupInfo(syncGroupInfo: Boolean, syncMembers: Boolean) {
        ArouterServiceManager.groupService.updateGroupInfoByCache(
            lifecycle(),
            mTargetGid,
            { groupInfoModel ->
                mGroupInfoModel = groupInfoModel
                mMessageAdapter.showReceiptStatus =
                    mGroupInfoModel?.memberCount ?: 0 <= Constant.Common.SHOW_RECEIPT_MAX_GROUP_MESSAGE_COUNT
                refreshShutupStatus()
                refreshGroupPic()
                refreshGroupName()
                showSocketContactStatus()
                showUnreadMessageCount()
                showGroupNoice()
                showFireStatus()

                if (syncGroupInfo) {
                    syncGroupInfo()
                } else {
                    val lastUpdateGroupInfoTime = lastUpdateGroupInfoTimes[mTargetGid]
                    if (groupInfoModel.memberCount >= 50) {
                        if (lastUpdateGroupInfoTime == null || (System.currentTimeMillis() - lastUpdateGroupInfoTime > 300000)) {
                            lastUpdateGroupInfoTimes[mTargetGid] = System.currentTimeMillis()
                            syncGroupInfo()
                        }
                    } else {
                        if (lastUpdateGroupInfoTime == null || (System.currentTimeMillis() - lastUpdateGroupInfoTime > 60000)) {
                            lastUpdateGroupInfoTimes[mTargetGid] = System.currentTimeMillis()
                            syncGroupInfo()
                        }
                    }
                }
            }) {
            syncGroupInfo()
        }

        if (syncMembers) {
            syncGroupAllMemberInfo()
        }
    }

    private fun syncGroupInfo() {
        ArouterServiceManager.groupService.updateGroupInfoByNet(
            lifecycle(),
            mTargetGid,
            { groupInfoModel ->
                mGroupInfoModel = groupInfoModel
                mMessageAdapter.showReceiptStatus =
                    mGroupInfoModel?.memberCount ?: 0 <= Constant.Common.SHOW_RECEIPT_MAX_GROUP_MESSAGE_COUNT
                refreshShutupStatus()
                refreshGroupPic()
                refreshGroupName()
                showSocketContactStatus()
                showUnreadMessageCount()
                showGroupNoice()
                showFireStatus()
            })
    }

    private fun refreshGroupMemberList() {//这里是为了更新 mGroupInfo ,有些数据本地更新了，比如用户角色
        ArouterServiceManager.groupService.updateGroupInfoByCache(
            lifecycle(),
            mTargetGid,
            { groupInfoModel ->
                mGroupInfoModel = groupInfoModel
                refreshMemberInfo(mGroupInfoModel?.memberCount ?: 0)
            })
    }

    private fun refreshShutupStatus() {
        if (mGroupInfoModel?.forShutupGroup == true && (mGroupInfoModel?.memberRole ?: 2) > 1) {
            layout_shutup_mask.visibility = View.VISIBLE
        } else {
            layout_shutup_mask.visibility = View.GONE
        }
    }

    private fun refreshGroupPic() {
        mGroupInfoModel?.let { groupInfoModel ->
            if (mGroupIconView != null) {
                mGroupIconView?.setImageURI(UriUtils.parseUri(groupInfoModel.pic))
            }
        }
    }

    private fun syncGroupAllMemberInfo() {
        ArouterServiceManager.groupService.syncGroupAllMemberInfoNew(
            lifecycle(),
            1,
            100,
            mTargetGid,
            1,
            { _, _, _ ->
                refreshMemberInfo(mGroupInfoModel?.memberCount ?: 0)
            }) {
            refreshMemberInfo(mGroupInfoModel?.memberCount ?: 0)
        }
    }

    private fun refreshMemberInfo(count: Int) {
        ArouterServiceManager.groupService.getAllGroupMembersInfoByCache(
            mTargetGid,
            Long.MAX_VALUE,
            { groupInfoModels, _ ->
                val map = HashMap<Long, Any>()
                groupInfoModels.forEach {
                    map[it.uid] = it
                }
                mMemberCount = count
                mMessageAdapter.setMessageOwnerList(map)
                refreshGroupName()
            })
    }

    @SuppressLint("SetTextI18n")
    private fun refreshGroupName() {
        val name = mGroupInfoModel?.name ?: ""
        var countText = if (mMemberCount == 0) name else "$name($mMemberCount)"
        if (BuildConfig.DEBUG) countText += " (ID:${mGroupInfoModel?.groupId ?: 0L})"
        if (mGroupNameView != null) {
            mGroupNameView?.text = countText
        }
    }

    private fun gotoGroupSetting() {
        ArouterServiceManager.groupService.isGotoGroupSettingPermission(mTargetGid, mMineUid, {
            if (it) {
                ARouter.getInstance()
                    .build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_GROUP_SETTING)
                    .withLong("groupId", mTargetGid).navigation()
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
                                mTargetGid,
                                mGroupInfoModel
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
                                    mTargetGid,
                                    mGroupInfoModel
                                )
                            }
                        }
                        else -> {
                            // 以静态图片传输
                            mPresenter?.sendImageMessage(
                                mediaInfo.path,
                                mRefMessage,
                                mMineUid,
                                mTargetGid,
                                mGroupInfoModel
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
                    CameraActivity.JPEG -> {
                        // 以图片传输
                        mPresenter?.sendImageMessage(
                            filePath,
                            mRefMessage,
                            mMineUid,
                            mTargetGid,
                            mGroupInfoModel
                        )
                        clearRefMessage()
                    }
                    CameraActivity.MP4 -> {
                        // 以视频传输
                        mPresenter?.sendVideoMessage(
                            filePath,
                            mRefMessage,
                            mMineUid,
                            mTargetGid,
                            mGroupInfoModel
                        )
                        clearRefMessage()
                    }
                    else -> {
                        // 报错
                    }
                }
            }
        }

        if (requestCode == AT_USERS_REQUESTCODE && resultCode == Activity.RESULT_OK) {
            val atUids = data?.getStringExtra("atUids")?.split(",") ?: ArrayList()
            val atName = data?.getStringExtra("atName")?.split(",") ?: ArrayList()
            val atUid = atUids.getOrNull(0)?.toLong() ?: 0L
            if (atUid > 0) {
                message_input_view.mentionUser(atUids[0].toLong(), atName[0])
            } else {
                message_input_view.mentionUser(-1, getString(R.string.whole_members))
            }
        }

        if (requestCode == TOOL_NAMECARD_REQUESTCODE && resultCode == Activity.RESULT_OK) {
            val uid = data?.getLongExtra("uid", 0L) ?: 0L
            if (uid > 0) {
                mPresenter?.sendNameCardMessage(
                    uid,
                    mRefMessage,
                    mMineUid,
                    mTargetGid,
                    mGroupInfoModel
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
                        mTargetGid,
                        mGroupInfoModel
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
                    mTargetGid,
                    mGroupInfoModel
                )
                clearRefMessage()
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val audioManager = BaseApp.app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (!audioManager.isWiredHeadsetOn) {
            val range = event?.values?.get(0) ?: 0f
            if (range >= mSensor.maximumRange) {
                mMessageAdapter.audioPlayController.setSpeakerphoneOn(true)
            } else {
                mMessageAdapter.audioPlayController.setSpeakerphoneOn(false)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onDestroy() {
        super.onDestroy()

        mEmojiPopup?.dismiss()
        mToolsPopup?.dismiss()

        DownloadAttachmentController.detachDownloadListener(mMessageAdapter)
        UploadAttachmentController.detachUploadListener(mMessageAdapter)

        recycler_view_messages?.destory()

        message_input_view.recordingListener = null

        mPresenter?.destory()

        mMessageAdapter.audioPlayController.detachListener()

        (BaseApp.app.getSystemService(Context.AUDIO_SERVICE) as AudioManager).isSpeakerphoneOn =
            true
        (BaseApp.app.getSystemService(Context.AUDIO_SERVICE) as AudioManager).mode =
            AudioManager.MODE_NORMAL
        volumeControlStream = mDefaultVolumeControlStream

        MessagesManager.deleteToGroupFireMessage(mMineUid, mTargetGid, {
            if (it != null) {
                ChatsHistoryManager.updateChatLastMsg(
                    mMineUid,
                    ChatModel.CHAT_TYPE_GROUP,
                    mTargetGid,
                    it,
                    changeTime = false
                )
            }
        })
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
                recycler_view_messages.recyclerViewController()?.layoutManager as LinearLayoutManager
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


    override fun refreshAtMeButton(count: Int) {
        if (count > 0) {
            layout_at_me.visibility = View.VISIBLE
        } else {
            layout_at_me.visibility = View.GONE
        }
    }

    override fun scrollToPosition(position: Int) {
        recycler_view_messages.recyclerViewController().scrollToPosition(position)
    }

    override fun isActive(): Boolean {
        return !ActivitiesHelper.isDestroyedActivity(this@GroupChatActivity)
    }

    override fun isShutup(): Boolean {
        return mGroupInfoModel?.forShutupGroup == true && (mGroupInfoModel?.memberRole ?: 2) > 1
    }

    override fun showError(msg: String) {
        toast(msg)
    }

    override fun onPause() {
        super.onPause()

        isActivityPause = true

        mMessageAdapter.audioPlayController.stopPlayVoice()

        message_input_view.saveToDraft()

        if (!mUseSpeaker) {
            mSensorManager.unregisterListener(this)
        }
    }

    private fun setUpEmojiPopup() {
        mEmojiPopup = IconFacePopup.Builder.fromRootView(root_view_group_chat)
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
                        if (this@GroupChatActivity.getString(R.string.append) == iconFaceItem.name) {
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
                                    mTargetGid,
                                    mGroupInfoModel
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
        val forbiddenMaxTime = sendForbiddenMaxTimeCache["${mMineUid}_${mTargetGid}"] ?: 0L
        return if (System.currentTimeMillis() < forbiddenMaxTime) {
            // 当前时间小于解禁的时间
            ArouterServiceManager.messageService.insertSystemTipMsg(
                ChatModel.CHAT_TYPE_GROUP,
                mTargetGid,
                0L,
                getString(R.string.send_messge_so_fast_tip)
            )
            false
        } else {
            val forbiddenCountCache = sendForbiddenMsgTimeCache["${mMineUid}_${mTargetGid}"]
            if (TextUtils.isEmpty(forbiddenCountCache)) {
                sendForbiddenMsgTimeCache["${mMineUid}_${mTargetGid}"] =
                    "${System.currentTimeMillis() + 5000}_${1}"
                AppLogcat.logger.d("demo", "计数器被初始化，5秒内发送的消息条数计数器置为初始值1")
            } else {
                val data = forbiddenCountCache!!.split("_")
                val time = data[0].toLong()
                val count = data[1].toInt()
                if (System.currentTimeMillis() < time) {
                    val newCount = count + 1
                    sendForbiddenMsgTimeCache["${mMineUid}_${mTargetGid}"] =
                        "${time}_${newCount}"
                    AppLogcat.logger.d("demo", "5秒内发送的消息条数计数器+1 --->${newCount}")

                    if (newCount >= 20) {
                        sendForbiddenMaxTimeCache["${mMineUid}_${mTargetGid}"] =
                            System.currentTimeMillis() + 120000
                        AppLogcat.logger.d("demo", "5秒内发送条数大于20条，禁言两分钟")
                    }
                } else {
                    sendForbiddenMsgTimeCache["${mMineUid}_${mTargetGid}"] =
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
        mToolsPopup = ToolsPopup.Builder.fromRootView(root_view_group_chat)
            .setOnToolClickListener { index ->
                KeyboardktUtils.hideKeyboard(root_view_group_chat)
                when (index) {
                    0 -> ImagePicker.from(this@GroupChatActivity)
                        .choose(
                            EnumSet.of(MimeType.JPEG, MimeType.PNG, MimeType.GIF, MimeType.MP4),
                            false
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
                                CameraActivity.getLaunchIntent(this@GroupChatActivity, saveDir)
                            startActivityForResult(intent, TOOL_CAMERAVIEW_REQUESTCODE)
                        }
                    }
                    2 -> {
                        mGroupInfoModel.let {
                            val name =
                                if (TextUtils.isEmpty(it?.groupNickName)) it?.name else it?.groupNickName
                            ARouter.getInstance()
                                .build(Constant.ARouter.ROUNTE_MSG_SELECT_CONTACT_CARD)
                                .withString("targetPic", it?.pic)
                                .withString("targetName", name)
                                .navigation(this@GroupChatActivity, TOOL_NAMECARD_REQUESTCODE)
                        }
                    }
                    //位置
                    3 -> {
                        ARouter.getInstance()
                            .build(Constant.ARouter.ROUNTE_LOCATION_CHOICE_ACTIVITY)
                            .navigation(
                                this@GroupChatActivity,
                                ChoiceLocationActivity.REQUEST_CODE_SEND_LOCATION
                            )
                    }
                    //文件
                    4 -> {
                        FilePickerManager.from(this@GroupChatActivity)
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
            msgModel.senderId == mTargetGid -> {
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

    private fun isShowMsgCopy(msgType: Int): Boolean {
        return msgType == MessageModel.LOCAL_TYPE_MYSELF_TEXT || msgType == MessageModel.LOCAL_TYPE_OTHER_TEXT
    }

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

    private fun isShowMsgSpeaker(msgType: Int): Boolean {
        return msgType == MessageModel.LOCAL_TYPE_MYSELF_VOICE || msgType == MessageModel.LOCAL_TYPE_OTHER_VOICE
    }

    private fun isShowMsgRepty(msgType: Int): Boolean {
        return when (msgType) {
            MessageModel.LOCAL_TYPE_MYSELF_STREAM_MEDIA,
            MessageModel.LOCAL_TYPE_OTHER_STREAM_MEDIA,
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

    private fun isShowMsgDelete(msgType: Int): Boolean {
        return msgType != MessageModel.LOCAL_TYPE_TIP
    }

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

    private fun isShowMsgDetail(msgType: Int, status: Int): Boolean {
        return if (mGroupInfoModel?.memberCount ?: 0 <= Constant.Common.SHOW_RECEIPT_MAX_GROUP_MEMBER_COUNT) {
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
        } else {
            false
        }
    }

    private fun showPicker() {
        mPv = MsgFireTimePickerUtil.showSelectTimePickerChat(
            this, mGroupInfoModel?.groupMsgCancelTime
                ?: 0
        ) { _, timeValue ->
            if (timeValue == -1) {
                //关闭
                ArouterServiceManager.groupService.setBurnAfterRead(
                    lifecycle(),
                    mTargetGid,
                    false,
                    {
                        mGroupInfoModel?.let {
                            it.bfGroupReadCancel = false
                            showFireStatus()
                        }
                    },
                    { t ->
                        toast(String.format(getString(R.string.setup_failed), t.message))
                    })
            } else {
                ArouterServiceManager.groupService.setBurnAfterReadTime(
                    lifecycle(),
                    mTargetGid,
                    timeValue,
                    {
                        mGroupInfoModel?.groupMsgCancelTime = timeValue
                        showFireStatus()
                    },
                    { t ->
                        toast(String.format(getString(R.string.setup_failed), t.message))
                    })
            }
        }
    }

    private fun showFireStatus() {
        mGroupInfoModel?.let { it ->
            if (it.bfGroupReadCancel) {
                // 阅后即焚模式
                message_input_view.setFireText(TimeUtils.timeFormatForDeadline(it.groupMsgCancelTime))
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

//        MessagesManager.checkToMessageFireStatus(ChatModel.CHAT_TYPE_PVT, mMineUid, mTargetGid, {
//            if (it || mContactInfoModel?.isBfReadCancel == true) {
//                timer?.cancel()
//                timer = Timer()
//                timer?.schedule(object : TimerTask() {
//                    override fun run() {
//                        try {
//                            ThreadUtils.runOnUIThread {
//                                mMessageAdapter.notifyItemExpireStatus()
//                            }
//                        } catch (e: java.lang.Exception) {
//                        }
//                    }
//                }, 0, 1000)
//            } else {
//                timer?.cancel()
//            }
//        })

    }
}
