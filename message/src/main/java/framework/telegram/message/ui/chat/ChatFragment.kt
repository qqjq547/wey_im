package framework.telegram.message.ui.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.qiniu.android.netdiag.Ping
import com.trello.rxlifecycle3.RxLifecycle
import com.trello.rxlifecycle3.android.FragmentEvent
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.ideas.common.webview.WebUrlConfig
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_SEARCH_MULT_CHAT_EXPAND
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.event.JumpTpUnreadChatEvent
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.*
import framework.telegram.message.controller.MessageController
import framework.telegram.message.db.RealmCreator
import framework.telegram.message.manager.*
import framework.telegram.message.ui.IMultiCheckChatCallback
import framework.telegram.message.ui.IMultiCheckable
import framework.telegram.message.ui.chat.adapter.ChatsAdapter
import framework.telegram.message.ui.chat.adapter.ChatsDiffCallback
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.BaseFragment
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.BitUtils
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.Helper
import framework.telegram.support.tools.StringUtils
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.menu.FloatMenu
import framework.telegram.ui.menu.MenuItem
import framework.telegram.ui.qr.decoding.Intents
import framework.telegram.ui.videoplayer.utils.NetworkUtils
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.msg_fragment_chats.*
import kotlinx.android.synthetic.main.msg_fragment_chats.custom_toolbar
import kotlinx.android.synthetic.main.msg_search.*

@Route(path = Constant.ARouter.ROUNTE_MSG_CHATS_FRAGMENT)
class ChatFragment : BaseFragment(), RealmChangeListener<RealmResults<ChatModel>>,
    IMultiCheckChatCallback {

    override val fragmentName: String
        get() = "ChatFragment"

    companion object {
        const val QRCODE_REQUEST_CODE = 0x1999
        const val GET_PERMISSIONS_REQUEST_CODE = 124
    }

    private val mRealm by lazy { RealmCreator.getChatsHistoryRealm(mMineUid) }

    private val mMineUid by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
    }

    private var mChatModelResults: RealmResults<ChatModel>? = null

    private var mTitleBarRightView: View? = null

    private var mTitleBarRightUnLockView: View? = null

    private var mFloatMenu: FloatMenu? = null

    private var mTitleTextView: TextView? = null

    private var mTvWebMuteStatus: TextView? = null
    private var mWebMuteStatusLayout: LinearLayout? = null
    private var mNetWorkStatusLayout: LinearLayout? = null
    private var mHeadView: View? = null

    private var mTempExtraMap = mutableMapOf<Long, ChatExtraData>()

    private val mAdapter by lazy {
        ChatsAdapter((activity as BaseActivity?)?.lifecycle())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.msg_fragment_chats, container, false)
    }

    private fun initTitleBar() {
        custom_toolbar.showLeftTextView(getString(R.string.message), {
            val lastTime = mTitleTextView?.getTag(R.id.doubleCheckId)
            val nowTime = System.currentTimeMillis()
            lastTime?.let {
                val checkTime = nowTime - (lastTime) as Long
                if (checkTime in 1..299) {
                    jumpToUnReadChat(true)
                }
            }
            mTitleTextView?.setTag(R.id.doubleCheckId, nowTime)
        }) {
            mTitleTextView = it
            it.id = R.id.doubleClickMessageId
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            it.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        }
        custom_toolbar.setToolbarSize(64f)

        custom_toolbar.showRightImageView(R.drawable.common_icon_add, {}) {
            mTitleBarRightView = it
        }

        custom_toolbar.showRightImageView(R.drawable.common_icon_fast_lock, {
            activity?.let {
                ArouterServiceManager.systemService.checkAppUnlockActivity(it)
            }
        }) {
            val param = it.layoutParams as LinearLayout.LayoutParams
            param.rightMargin = 30
            it.layoutParams = param
            mTitleBarRightUnLockView = it
        }
    }

    private fun initFloatView() {
        if (mFloatMenu != null) {
            return
        }

        mFloatMenu = FloatMenu(activity, 1)//这里要按这样写，因为FloatMenu实例化的时候需要点击的view
        mFloatMenu?.setMenuDrawable(
            R.drawable.common_half_corners_trans_020419_10_0,
            R.drawable.pop_selector_black_item,
            R.color.c8da7d3,
            R.color.c40446a
        )
        mTitleBarRightView?.setOnClickListener {
            val anim = RotateAnimation(
                0f,
                135f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            anim.fillAfter = true
            anim.duration = 300
            mTitleBarRightView?.startAnimation(anim)

            mFloatMenu?.showDropDown(it, it.width - 5, 0)
        }

        mFloatMenu?.items(
            mutableListOf(
                MenuItem(
                    getString(R.string.common_contacts_make_friend),
                    R.drawable.common_icon_pop_add
                ),
                MenuItem(
                    getString(R.string.common_contacts_create_ground_chat),
                    R.drawable.common_icon_pop_create_group
                ),
                MenuItem(getString(R.string.common_contacts_qr_code), R.drawable.common_icon_pop_qr)
            )
        )

        mFloatMenu?.setOnItemClickListener { _, text ->
            when (text) {
                getString(R.string.common_contacts_make_friend) -> {
                    ARouter.getInstance()
                        .build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_CONTACTS_ADD_FRIEND)
                        .navigation()
                }
                getString(R.string.common_contacts_create_ground_chat) -> {
                    ARouter.getInstance()
                        .build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE)
                        .withInt("operate", 1).navigation()
                }
                getString(R.string.common_contacts_qr_code) -> {
                    if (checkPermission()) {
                        ARouter.getInstance()
                            .build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_QR_SCAN)
                            .withString(Intents.Scan.SCAN_FORMATS, "QR_CODE")
                            .navigation(activity, QRCODE_REQUEST_CODE)
                        // 到MainActivity中解析返回的数据
                    }
                }
            }
        }
        mFloatMenu?.setOnDismissListener {
            val anim = RotateAnimation(
                135f,
                0f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            anim.fillAfter = true
            anim.duration = 300
            mTitleBarRightView?.startAnimation(anim)
        }
    }

    @SuppressLint("CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTitleBar()
        initFloatView()
        initRecyclerView()
        initHeadView()

        text_view_search_icon.setOnClickListener {
            ARouter.getInstance().build(ROUNTE_BUS_SEARCH_MULT_CHAT_EXPAND).navigation()
        }

        loadChatHistory()

        EventBus.getFlowable(ChatHistoryChangeEvent::class.java)
            .bindToLifecycle(this@ChatFragment)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                //todo 这里有待优化
                if (activity != null) {
                    recycler_view_history.recyclerViewController().adapter.notifyDataSetChanged()
                }
            }

        EventBus.getFlowable(SocketStatusChangeEvent::class.java)
            .bindToLifecycle(this@ChatFragment)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (activity != null) {
                    refreshSocketStatus()
                }
            }

        EventBus.getFlowable(OnlineStatusChangeEvent::class.java)//在线状态更改
            .bindToLifecycle(this@ChatFragment)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (activity != null) {
                    refreshRecyclerData(it.uid) { oldModel ->
                        mTempExtraMap[it.uid] =
                            ChatExtraData(it.isStatu, oldModel.msgStatus, oldModel.isFireStatus)
                        ChatCacheModel.createCacheChat(
                            it.isStatu,
                            oldModel.msgStatus,
                            oldModel.isFireStatus,
                            oldModel.chatModel
                        )
                    }
                }
            }

        EventBus.getFlowable(FireStatusChangeEvent::class.java)//阅后即焚状态更改
            .bindToLifecycle(this@ChatFragment)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (activity != null) {
                    refreshRecyclerFireStuts(it.fireList)
                }
            }

        EventBus.getFlowable(MessageStateChangeEvent::class.java)//回执状态更改/发送状态更改
            .bindToLifecycle(this@ChatFragment)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (activity != null) {
                    refreshRecyclerSendStatus(it.msgModels)
                }
            }

        EventBus.getFlowable(MessageSendStatusEvent::class.java)//从adapter发送过来的更改请求
            .bindToLifecycle(this@ChatFragment)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (activity != null) {
                    refreshRecyclerData(it.msgModel.targetId) { oldModel ->
                        val msgStatus =
                            if (oldModel.chatModel.chaterType == ChatModel.CHAT_TYPE_PVT) {
                                val privacy =
                                    AccountManager.getLoginAccount(AccountInfo::class.java)
                                        .getPrivacy()
                                val isClose =
                                    BitUtils.checkBitValue(Helper.int2Bytes(privacy)[2], 7)
                                getChatLastMsgStatus(isClose, it.msgModel)
                            } else {
                                -1001
                            }
                        mTempExtraMap[it.msgModel.targetId] =
                            ChatExtraData(oldModel.isOnlineStatus, msgStatus, oldModel.isFireStatus)
                        ChatCacheModel.createCacheChat(
                            oldModel.isOnlineStatus,
                            msgStatus,
                            oldModel.isFireStatus,
                            oldModel.chatModel
                        )
                    }
                }
            }

        EventBus.getFlowable(JumpTpUnreadChatEvent::class.java)
            .bindToLifecycle(this@ChatFragment)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (activity != null) {
                    jumpToUnReadChat(false)
                }
            }

        EventBus.getFlowable(WebOnlineStatusChangeEvent::class.java)
            .bindToLifecycle(this@ChatFragment)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (activity != null) {
                    refreshWebClientSocketStatus()
                    updateWebMuteStatus()
                }
            }

        EventBus.getFlowable(BanGroupMessageEvent::class.java)
            .bindToLifecycle(this@ChatFragment)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (activity != null) {
                    Handler().postDelayed({
                        ArouterServiceManager.messageService.deleteChat(
                            ChatModel.CHAT_TYPE_GROUP,
                            it.groupId
                        )
                    }, 400)
                }
            }

        EventBus.getFlowable(DisableGroupMessageEvent::class.java)
            .bindToLifecycle(this@ChatFragment)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (activity != null) {
                    Handler().postDelayed({
                        ArouterServiceManager.messageService.deleteChat(
                            ChatModel.CHAT_TYPE_GROUP,
                            it.groupId
                        )
                    }, 400)
                }
            }

        refreshSocketStatus()
        refreshWebClientSocketStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshSocketStatus()
        refreshWebClientSocketStatus()
        updateWebMuteStatus()
        updateAppLockButton()
    }

    private var mLastJumpUnReadChatIndex: Int = -1

    private fun jumpToUnReadChat(isTop: Boolean) {
        if (isTop) {
            moveToPosition(mAdapter.headerLayoutCount)
        } else {
            val datas = mAdapter.data
            var firstUnReadChatIndex = -1
            var adapterPosition = -1
            run outside@{
                datas.forEachIndexed { index, messageModel ->
                    if (messageModel != null) {
                        if (messageModel.chatModel.unReadCount > 0) {
                            // 找到最上面的未读消息
                            if (firstUnReadChatIndex < 0) {
                                firstUnReadChatIndex = index
                            }

                            // 找到下面的未读消息
                            if (index > mLastJumpUnReadChatIndex) {
                                mLastJumpUnReadChatIndex = index
                                adapterPosition = index + mAdapter.headerLayoutCount
                                return@outside
                            }
                        }
                    }
                }
            }

            if (adapterPosition < 0) {
                // 没找到下面的未读消息
                mLastJumpUnReadChatIndex = -1

                if (firstUnReadChatIndex >= 0) {
                    // 使用最上面的未读消息
                    adapterPosition = firstUnReadChatIndex + mAdapter.headerLayoutCount
                }
            }

            if (adapterPosition > -1) {
                moveToPosition(adapterPosition)
            } else {
                moveToPosition(mAdapter.headerLayoutCount)
            }
        }
    }

    private fun moveToPosition(position: Int) {
        if (position != -1) {
            recycler_view_history.recyclerViewController().scrollToPosition(position)
            val layoutManager =
                recycler_view_history.recyclerView.layoutManager as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(position, 0)
        }
    }

    private fun refreshRecyclerData(uid: Long, callBack: ((ChatCacheModel) -> ChatCacheModel)) {
        val tmpList = mutableListOf<ChatCacheModel>()
        mAdapter.data.forEach { oldModel ->
            if (oldModel != null) {
                if (uid == oldModel.chatModel.chaterId) {
                    tmpList.add(callBack.invoke(oldModel))
                } else {
                    tmpList.add(oldModel)
                }
            }
        }
        recycler_view_history.diffController().compareToAndSet(tmpList, ChatsDiffCallback(tmpList))
    }

    private fun refreshRecyclerFireStuts(fireList: List<FireStatus>) {
        val tmpList = mutableListOf<ChatCacheModel>()
        mAdapter.data.forEach { chatModel ->
            var matchData = false
            run outside@{
                fireList.forEach { fire ->
                    if (fire.uid == chatModel.chatModel.chaterId) {
                        matchData = true
                        mTempExtraMap[fire.uid] = ChatExtraData(
                            chatModel.isOnlineStatus,
                            chatModel.msgStatus,
                            fire.isStatu
                        )
                        tmpList.add(
                            ChatCacheModel.createCacheChat(
                                chatModel.isOnlineStatus,
                                chatModel.msgStatus,
                                fire.isStatu,
                                chatModel.chatModel
                            )
                        )
                        return@outside
                    }
                }
            }
            if (!matchData) {
                tmpList.add(chatModel)
            }
        }
        recycler_view_history.diffController().compareToAndSet(tmpList, ChatsDiffCallback(tmpList))
    }

    private fun refreshRecyclerSendStatus(msgs: List<MessageModel>) {
        val tmpList = mutableListOf<ChatCacheModel>()
        val privacy = AccountManager.getLoginAccount(AccountInfo::class.java).getPrivacy()
        val isClose = BitUtils.checkBitValue(Helper.int2Bytes(privacy)[2], 7)
        mAdapter.data.forEach { chatModel ->
            refreshRecyclerSendStatusImpl(isClose, tmpList, chatModel, msgs)
        }
        recycler_view_history.diffController().compareToAndSet(tmpList, ChatsDiffCallback(tmpList))
    }

    private fun refreshRecyclerSendStatusImpl(
        isClose: Boolean,
        tmpList: MutableList<ChatCacheModel>,
        chatModel: ChatCacheModel,
        msgs: List<MessageModel>
    ) {
        var matchData = false
        run outside@{
            msgs.forEach { msg ->
                if (msg.targetId == chatModel.chatModel.chaterId || msg.senderId == chatModel.chatModel.chaterId) {
                    matchData = true
                    val status = if (msg.chatType == ChatModel.CHAT_TYPE_PVT) {
                        if (chatModel.chatModel.lastMsgLocalId == msg.id) {
                            getChatLastMsgStatus(isClose, msg)
                        } else {
                            -1001
                        }
                    } else {
                        -1001
                    }
                    mTempExtraMap[chatModel.chatModel.chaterId] =
                        ChatExtraData(chatModel.isOnlineStatus, status, chatModel.isFireStatus)
                    tmpList.add(
                        ChatCacheModel.createCacheChat(
                            chatModel.isOnlineStatus,
                            status,
                            chatModel.isFireStatus,
                            chatModel.chatModel
                        )
                    )
                    return@outside
                }
            }
        }
        if (!matchData) {
            tmpList.add(chatModel)
        }
    }

    private fun getChatLastMsgStatus(isClose: Boolean, msg: MessageModel?): Int {
        var msgStatus = -1000
        if (msg?.isSend == 1) {
            // 显示发送状态
            when {
                msg.type == MessageModel.MESSAGE_TYPE_STREAM -> return -1001 //如果是视频流，不设置
                msg.status == -1 -> msgStatus = R.drawable.msg_icon_send_fail
                msg.status == 2 -> when {
                    (msg.isReadedAttachment == 1 && (msg.type == MessageModel.MESSAGE_TYPE_VIDEO || msg.type == MessageModel.MESSAGE_TYPE_VOICE)) -> {
                        msgStatus = if (!isClose && msg.isShowAlreadyRead) {
                            R.drawable.msg_icon_readed
                        } else {
                            R.drawable.msg_icon_deliver
                        }
                    }
                    msg.isRead == 1 -> {
                        msgStatus = if (!isClose && msg.isShowAlreadyRead) {
                            R.drawable.msg_icon_readed
                        } else {
                            R.drawable.msg_icon_deliver
                        }
                    }
                    msg.isDeliver == 1 -> msgStatus = R.drawable.msg_icon_deliver
                    else -> msgStatus = R.drawable.msg_icon_sended
                }
                msg.status == 1 -> msgStatus = R.drawable.msg_icon_sended
                else -> msgStatus = R.drawable.msg_icon_sending
            }
        } else {
            return -1001
        }
        return msgStatus
    }

    private fun refreshSocketStatus() {
        if (ReceiveMessageManager.socketIsLogin) {
            mTitleTextView?.text = getString(R.string.message)
            refreshNetworkBottom(false)
        } else {
            if (NetworkUtils.isAvailable(BaseApp.app)) {
                mTitleTextView?.text = getString(R.string.connecting_sign)
            } else {
                mTitleTextView?.text = getString(R.string.ununited)
            }

            Ping.start("www.baidu.com", 1, {}, {
                ThreadUtils.runOnUIThread {
                    refreshNetworkBottom(TextUtils.isEmpty(it.ip))
                }
            })
        }
    }

    private fun refreshWebClientSocketStatus() {
        if (AccountManager.getLoginAccount(AccountInfo::class.java).getWebOnline() == 1) {
            refreshWebBottom(true)
        } else {
            refreshWebBottom(false)
        }
    }

    private fun initRecyclerView() {
        mAdapter.setHeaderAndEmpty(true)
        mAdapter.setOnItemChildClickListener { _, _, position ->
            if (!mAdapter.isMultiCheckMode) {
                val data = mAdapter.getItem(position)
                data?.let {
                    when (it.chatModel.chaterType) {
                        ChatModel.CHAT_TYPE_PVT -> {
                            ARouter.getInstance()
                                .build(Constant.ARouter.ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY)
                                .withLong("targetUid", data.chatModel.chaterId).navigation()
                        }
                        ChatModel.CHAT_TYPE_GROUP -> {
                            ARouter.getInstance()
                                .build(Constant.ARouter.ROUNTE_MSG_GROUP_CHAT_ACTIVITY)
                                .withLong("targetGid", data.chatModel.chaterId).navigation()
                        }
                        ChatModel.CHAT_TYPE_GROUP_NOTIFY -> {
                            ARouter.getInstance()
                                .build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_GROUP_JOIN_REQ_LIST)
                                .navigation()
                        }
                        else -> {
                        }
                    }
                }
            }
        }
        mAdapter.setOnItemChildLongClickListener { _, _, position ->
            if (!mAdapter.isMultiCheckMode) {
                val data = mAdapter.getItem(position)
                data?.let {
                    val chaterType = it.chatModel.chaterType
                    val chaterId = it.chatModel.chaterId

                    val floatMenu = FloatMenu(this.activity)
                    val items = mutableListOf<String>()
                    if (it.chatModel.isTop == 0) {
                        items.add(getString(R.string.top_chat))
                    } else {
                        items.add(getString(R.string.unstick))
                    }

                    if (it.chatModel.chaterType != ChatModel.CHAT_TYPE_GROUP_NOTIFY) {
                        if (it.chatModel.bfDisturb == 1) {
                            items.add(getString(R.string.set_do_disturb))
                        } else {
                            items.add(getString(R.string.set_do_not_disturb))
                        }
                    }

                    if (it.chatModel.unReadCount == 0) {
                        items.add(getString(R.string.set_all_unreaded))
                    } else {
                        items.add(getString(R.string.set_all_readed))
                    }

                    items.add(getString(R.string.delete))
                    items.add(getString(R.string.multi_checkable))

                    floatMenu.items(*items.toTypedArray())
                    floatMenu.show(recycler_view_history.popPoint)
                    floatMenu.setOnItemClickListener { _, text ->
                        when (text) {
                            getString(R.string.top_chat), getString(R.string.unstick) -> {
                                ArouterServiceManager.messageService.setChatTopStatus(
                                    chaterType,
                                    chaterId,
                                    data.chatModel.isTop == 0
                                )
                            }
                            getString(R.string.set_all_unreaded) -> {
                                ArouterServiceManager.messageService.setChatIsUnreaded(
                                    chaterType,
                                    chaterId,
                                    1,
                                    {
                                        EventBus.publishEvent(UnreadMessageEvent())//修改红点数字
                                    })
                            }
                            getString(R.string.set_all_readed) -> {
                                //所有本地未阅读消息改成已阅，并发送回执
                                when (chaterType) {
                                    ChatModel.CHAT_TYPE_GROUP -> {
                                        MessagesManager.setAllGroupMessageReaded(
                                            mMineUid,
                                            chaterId,
                                            { c, id ->
                                                //会话改成已阅
                                                ChatsHistoryManager.setChatMessagesAllReaded(
                                                    mMineUid,
                                                    ChatModel.CHAT_TYPE_GROUP,
                                                    chaterId,
                                                    {
                                                        EventBus.publishEvent(UnreadMessageEvent())//修改红点数字
                                                    })
                                            })
                                    }
                                    ChatModel.CHAT_TYPE_PVT -> {
                                        MessagesManager.setAllUserMessageReaded(
                                            mMineUid,
                                            chaterId,
                                            { c, id ->
                                                //会话改成已阅
                                                ChatsHistoryManager.setChatMessagesAllReaded(
                                                    mMineUid,
                                                    ChatModel.CHAT_TYPE_PVT,
                                                    chaterId,
                                                    {
                                                        EventBus.publishEvent(UnreadMessageEvent())//修改红点数字
                                                    })
                                            })
                                    }
                                    ChatModel.CHAT_TYPE_GROUP_NOTIFY -> {
                                        //会话改成已阅
                                        ArouterServiceManager.messageService.setChatIsReaded(
                                            ChatModel.CHAT_TYPE_GROUP_NOTIFY,
                                            0,
                                            {
                                                EventBus.publishEvent(UnreadMessageEvent())//修改红点数字
                                            })
                                    }
                                }
                            }
                            getString(R.string.delete) -> {
                                clickDeleteImpl(it.chatModel)
                            }
                            getString(R.string.multi_checkable) -> {
                                setCheckableMessage(it.chatModel)
                            }
                            getString(R.string.set_do_not_disturb) -> {
                                if (it.chatModel.chaterType == ChatModel.CHAT_TYPE_PVT) {
                                    ArouterServiceManager.contactService.setContactMessageQuiet(
                                        null,
                                        it.chatModel.chaterId,
                                        true,
                                        {
                                            ArouterServiceManager.messageService.setChatIsDisturb(
                                                ChatModel.CHAT_TYPE_PVT,
                                                it.chatModel.chaterId,
                                                true,
                                                {
                                                    EventBus.publishEvent(UnreadMessageEvent())
                                                })
                                        })
                                } else if (it.chatModel.chaterType == ChatModel.CHAT_TYPE_GROUP) {
                                    ArouterServiceManager.groupService.setGroupMessageQuiet(
                                        null,
                                        it.chatModel.chaterId,
                                        true,
                                        {
                                            ArouterServiceManager.messageService.setChatIsDisturb(
                                                ChatModel.CHAT_TYPE_GROUP,
                                                it.chatModel.chaterId,
                                                true,
                                                {
                                                    EventBus.publishEvent(UnreadMessageEvent())
                                                })
                                        })
                                }
                            }
                            getString(R.string.set_do_disturb) -> {
                                if (it.chatModel.chaterType == ChatModel.CHAT_TYPE_PVT) {
                                    ArouterServiceManager.contactService.setContactMessageQuiet(
                                        null,
                                        it.chatModel.chaterId,
                                        false,
                                        {
                                            ArouterServiceManager.messageService.setChatIsDisturb(
                                                ChatModel.CHAT_TYPE_PVT,
                                                it.chatModel.chaterId,
                                                false,
                                                {
                                                    EventBus.publishEvent(UnreadMessageEvent())
                                                })
                                        })
                                } else if (it.chatModel.chaterType == ChatModel.CHAT_TYPE_GROUP) {
                                    ArouterServiceManager.groupService.setGroupMessageQuiet(
                                        null,
                                        it.chatModel.chaterId,
                                        false,
                                        {
                                            ArouterServiceManager.messageService.setChatIsDisturb(
                                                ChatModel.CHAT_TYPE_GROUP,
                                                it.chatModel.chaterId,
                                                false,
                                                {
                                                    EventBus.publishEvent(UnreadMessageEvent())
                                                })
                                        })
                                }
                            }
                        }
                    }
                }
            }

            true
        }
        recycler_view_history.initMultiTypeRecycleView(
            LinearLayoutManager(activity),
            mAdapter,
            false
        )
        recycler_view_history.refreshController().setEnablePullToRefresh(false)
        recycler_view_history.emptyController()
            .setEmpty(getString(R.string.no_recent_conversation), R.drawable.common_icon_empty_chat)
    }

    private fun clickDeleteImpl(chatModel: ChatModel) {
        when (chatModel.chaterType) {
            ChatModel.CHAT_TYPE_PVT -> {
                if (chatModel.chaterId == Constant.Common.FILE_TRANSFER_UID) {
                    deleteFileTransferChat(chatModel)
                } else {
                    deletePvtChat(chatModel)
                }
            }
            ChatModel.CHAT_TYPE_GROUP -> {
                ArouterServiceManager.groupService.updateGroupInfoByCache(
                    (this@ChatFragment.context as BaseActivity).lifecycle(),
                    chatModel.chaterId, {
                        if (it.memberRole > 1) {
                            deleteGroupChatLocal(chatModel)
                        } else {
                            deleteGroupChat(chatModel)
                        }
                    }, {
                        deleteGroupChatLocal(chatModel)
                    })
            }
            else -> {
                activity?.let {
                    AppDialog.show(it, this@ChatFragment) {
                        positiveButton(text = getString(R.string.confirm), click = {
                            deleteChatImpl(chatModel.chaterType, chatModel.chaterId)
                        })
                        negativeButton(text = getString(R.string.cancel))
                        title(text = getString(R.string.clear_and_delete))
                        message(text = getString(R.string.also_clear_chat_records))
                    }
                }
            }
        }
    }

    private fun deletePvtChat(chatModel: ChatModel) {
        AppDialog.showBottomListView(
            this@ChatFragment.context as AppCompatActivity,
            this@ChatFragment,
            arrayListOf(
                getString(R.string.clear_and_delete_chat),
                String.format(getString(R.string.clear_and_delete_pvt_chat), StringUtils.replaceSymbol(chatModel.chaterName)),
                getString(R.string.cancel)
            )
        ) { dialog, index, _ ->
            when (index) {
                0 -> {
                    deleteChatImpl(
                        chatModel.chaterType,
                        chatModel.chaterId
                    )
                }
                1 -> {
                    MessageController.recallMessages(
                        ChatModel.CHAT_TYPE_PVT,
                        mMineUid,
                        chatModel.chaterId,
                        ArouterServiceManager.messageService.getCurrentTime(),
                        deleteChat = true
                    )
                }
                else -> {
                    dialog.dismiss()
                }
            }
        }
    }

    private fun deleteFileTransferChat(chatModel: ChatModel) {
        AppDialog.showBottomListView(
            this@ChatFragment.context as AppCompatActivity,
            this@ChatFragment,
            arrayListOf(
                getString(R.string.clear_and_delete_chat),
                getString(R.string.clear_and_delete_file_tran_chat),
                getString(R.string.cancel)
            )
        ) { dialog, index, _ ->
            when (index) {
                0 -> {
                    deleteChatImpl(
                        chatModel.chaterType,
                        chatModel.chaterId
                    )
                }
                1 -> {
                    MessageController.recallMessages(
                        ChatModel.CHAT_TYPE_PVT,
                        mMineUid,
                        chatModel.chaterId,
                        ArouterServiceManager.messageService.getCurrentTime(),
                        deleteChat = true
                    )
                }
                else -> {
                    dialog.dismiss()
                }
            }
        }
    }

    private fun deleteGroupChatLocal(chatModel: ChatModel) {
        AppDialog.showBottomListView(
            this@ChatFragment.context as AppCompatActivity,
            this@ChatFragment,
            arrayListOf(
                getString(R.string.clear_and_delete_chat),
                getString(R.string.cancel)
            )
        ) { dialog, index, _ ->
            when (index) {
                0 -> {
                    deleteChatImpl(
                        chatModel.chaterType,
                        chatModel.chaterId
                    )
                }
                else -> {
                    dialog.dismiss()
                }
            }
        }
    }

    private fun deleteGroupChat(chatModel: ChatModel) {
        AppDialog.showBottomListView(
            this@ChatFragment.context as AppCompatActivity,
            this@ChatFragment,
            arrayListOf(
                getString(R.string.clear_and_delete_chat),
                getString(R.string.clear_and_delete_group_chat),
                getString(R.string.cancel)
            )
        ) { dialog, index, _ ->
            when (index) {
                0 -> {
                    deleteChatImpl(
                        chatModel.chaterType,
                        chatModel.chaterId
                    )
                }
                1 -> {
                    MessageController.recallMessages(
                        ChatModel.CHAT_TYPE_GROUP,
                        mMineUid,
                        chatModel.chaterId,
                        ArouterServiceManager.messageService.getCurrentTime(),
                        deleteChat = true
                    )
                }
                else -> {
                    dialog.dismiss()
                }
            }
        }
    }

    private fun deleteChatImpl(chaterType: Int, chaterId: Long) {
        //删除会话并清空聊天记录
        if (chaterType == ChatModel.CHAT_TYPE_GROUP_NOTIFY) {
            ArouterServiceManager.groupService.clearGroupReq(null, {
                ArouterServiceManager.messageService.deleteChat(
                    ChatModel.CHAT_TYPE_GROUP_NOTIFY,
                    0
                )//群通知id=0
            }, {
                toast(it.message.toString())
            })
        } else {
            ArouterServiceManager.messageService.deleteChat(chaterType, chaterId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.removeAllChangeListeners()
        mRealm.close()

        recycler_view_history?.destory()
    }

    @SuppressLint("CheckResult")
    private fun loadChatHistory() {
        Flowable.just<Realm>(mRealm)
            .compose(RxLifecycle.bindUntilEvent(lifecycle(), FragmentEvent.DESTROY))
            .subscribeOn(AndroidSchedulers.mainThread())
            .map {
                it.where(ChatModel::class.java)
                    ?.sort("isTop", Sort.DESCENDING, "lastMsgTime", Sort.DESCENDING)
                    ?.findAllAsync()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                // 初次则不进行数据集对比
                mChatModelResults = it
                mChatModelResults?.addChangeListener(this@ChatFragment)
            }
    }

    override fun onChange(t: RealmResults<ChatModel>) {
        if (!t.isValid || recycler_view_history == null)
            return

        if (activity == null || context == null) {
            return
        }

        //  当如果查看的item为第一位时，则需要在刷新item后回滚到第一位
        var shouldScrollerToFirst = false
        val layoutManager =
            recycler_view_history.recyclerViewController().layoutManager as LinearLayoutManager
        if (layoutManager.findFirstVisibleItemPosition() == 0)
            shouldScrollerToFirst = true
        val tmpList = arrayListOf<ChatCacheModel>()
        t.forEach { chat ->
            val chatExtraData = mTempExtraMap[chat.chaterId]
            if (chatExtraData != null) {
                tmpList.add(
                    ChatCacheModel.createCacheChat(
                        chatExtraData.isOnlineStatus,
                        chatExtraData.msgStatus,
                        chatExtraData.isFire,
                        chat.copyChat()
                    )
                )
            } else {
                tmpList.add(ChatCacheModel.createCacheChat(null, -1000, null, chat.copyChat()))
            }
        }
        recycler_view_history.diffController().compareToAndSet(tmpList, ChatsDiffCallback(tmpList))

        if (shouldScrollerToFirst)
            recycler_view_history.recyclerViewController().scrollToPosition(0)

        if (mAdapter.isMultiCheckMode) {
            if (mAdapter.data.size != mAdapter.getCheckableMessages().size) {
                (activity as IMultiCheckable).setAllChecked(false)
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.context?.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.CAMERA),
                    GET_PERMISSIONS_REQUEST_CODE
                )
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            GET_PERMISSIONS_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (this.context?.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        toast(getString(R.string.no_permissions_were_obtained))
                    } else {
                        ARouter.getInstance()
                            .build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_QR_SCAN)
                            .withString(Intents.Scan.SCAN_FORMATS, "QR_CODE")
                            .navigation(activity, QRCODE_REQUEST_CODE)
                        // 到MainActivity中解析返回的数据
                    }
                }
            }
        }
    }

    private fun initHeadView() {
        recycler_view_history.headerController().removeAllHeaderView()
        val view = LayoutInflater.from(activity)
            .inflate(R.layout.msg_chat_fragment_head_item, recycler_view_history, false)
        mTvWebMuteStatus = view.findViewById<TextView>(R.id.tv_status)

        mWebMuteStatusLayout = view.findViewById<LinearLayout>(R.id.layout_web)
        mNetWorkStatusLayout = view.findViewById<LinearLayout>(R.id.layout_network)
        mHeadView = view.findViewById<View>(R.id.view_line)

        mWebMuteStatusLayout?.setOnClickListener {
            ARouter.getInstance().build("/bus/login/web_already_login")
                .withTransition(R.anim.anim_up_in, 0).navigation(this@ChatFragment.context)
        }

        mNetWorkStatusLayout?.setOnClickListener {
            val i = Intent(Intent.ACTION_VIEW)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            i.data = Uri.parse(WebUrlConfig.networkErrorUrl)
            startActivity(i)
        }
        recycler_view_history.headerController().addHeader(view)
    }


    private fun updateWebMuteStatus() {
        mTvWebMuteStatus?.let {
            val privacy = AccountManager.getLoginAccount(AccountInfo::class.java).getPrivacy()
            val isOpen = !BitUtils.checkBitValue(Helper.int2Bytes(privacy)[1], 0)
            if (isOpen) {
                it.text = getString(R.string.mobile_notification_is_off)
            } else {
                it.text = getString(R.string.mobile_notification_is_on)
            }
        }
    }

    private fun updateAppLockButton() {
        if (ArouterServiceManager.systemService.isAppLockOn()) {
            mTitleBarRightUnLockView?.visibility = View.VISIBLE
        } else {
            mTitleBarRightUnLockView?.visibility = View.GONE
        }
    }

    private fun refreshWebBottom(visible: Boolean) {
        mWebMuteStatusLayout?.visibility = if (visible) View.VISIBLE else View.GONE
        refreshLineView()
    }

    private fun refreshLineView() {
        if (mNetWorkStatusLayout?.visibility == View.GONE
            && mWebMuteStatusLayout?.visibility == View.GONE
        ) {
            mHeadView?.visibility = View.GONE
        } else {
            mHeadView?.visibility = View.VISIBLE
        }
    }

    private fun refreshNetworkBottom(visible: Boolean) {
        mNetWorkStatusLayout?.visibility = if (visible) View.VISIBLE else View.GONE
        refreshLineView()
    }

    private fun setCheckableMessage(msg: ChatModel) {
        if (activity is IMultiCheckable) {
            mAdapter.setCheckable(msg) { msgCount ->
                (activity as IMultiCheckable).setCheckableMessage(
                    true,
                    getString(R.string.sign_call),
                    msgCount
                )

                if (mAdapter.data.size != mAdapter.getCheckableMessages().size) {
                    (activity as IMultiCheckable).setAllChecked(false)
                }
            }
        }
    }

    override fun hasUnread(): Boolean {
        if (activity == null) {
            return false
        }
        return mAdapter.hasUnread()
    }

    override fun showCheckMessages() {
        text_view_search_icon?.let {
            it.isEnabled = false
            it.isClickable = false
        }
    }

    override fun clickAllChecked(isChecked: Boolean): Int {
        return mAdapter.setAllChecked(isChecked)
    }

    override fun clickBatchDelete() {
        val msgModels = mAdapter.getCheckableMessages()

        AppDialog.show(activity!!, this@ChatFragment) {
            positiveButton(text = getString(R.string.confirm), click = {
                // 删除
                msgModels.forEach {
                    deleteChatImpl(it.chaterType, it.chaterId)
                }
                (activity as IMultiCheckable).dismissCheckMessages()
            })
            negativeButton(text = getString(R.string.cancel))
            title(text = getString(R.string.clear_and_delete))
            message(text = String.format(getString(R.string.multi_delete_chat_tip), msgModels.size))
        }
    }

    override fun clickBatchSetReaded() {
        val msgModels = mAdapter.getCheckableMessages()

        // 设置已读
        msgModels.forEach {
            //所有本地未阅读消息改成已阅，并发送回执
            when (it.chaterType) {
                ChatModel.CHAT_TYPE_GROUP -> {
                    MessagesManager.setAllGroupMessageReaded(mMineUid, it.chaterId, { c, id ->
                        //会话改成已阅
                        ChatsHistoryManager.setChatMessagesAllReaded(
                            mMineUid,
                            ChatModel.CHAT_TYPE_GROUP,
                            it.chaterId,
                            {
                                EventBus.publishEvent(UnreadMessageEvent())//修改红点数字
                            })
                    })
                }
                ChatModel.CHAT_TYPE_PVT -> {
                    MessagesManager.setAllUserMessageReaded(mMineUid, it.chaterId, { c, id ->
                        //会话改成已阅
                        ChatsHistoryManager.setChatMessagesAllReaded(
                            mMineUid,
                            ChatModel.CHAT_TYPE_PVT,
                            it.chaterId,
                            {
                                EventBus.publishEvent(UnreadMessageEvent())//修改红点数字
                            })
                    })
                }
                ChatModel.CHAT_TYPE_GROUP_NOTIFY -> {
                    //会话改成已阅
                    ArouterServiceManager.messageService.setChatIsReaded(
                        ChatModel.CHAT_TYPE_GROUP_NOTIFY,
                        0,
                        {
                            EventBus.publishEvent(UnreadMessageEvent())//修改红点数字
                        })
                }
            }
        }

        (activity as IMultiCheckable).dismissCheckMessages()
    }

    override fun dismissCheckMessages() {
        ThreadUtils.runOnUIThread {
            text_view_search_icon.isEnabled = true
            text_view_search_icon.isClickable = true
            mAdapter.setUnCheckable()
        }
    }
}
