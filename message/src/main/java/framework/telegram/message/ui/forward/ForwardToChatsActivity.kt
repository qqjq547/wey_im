package framework.telegram.message.ui.forward

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.common.TitleModel.TITLE_SELECT_FRIEND
import framework.ideas.common.model.common.TitleModel.TITLE_SELECT_GROUP
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT
import framework.telegram.business.bridge.Constant.Search.FORWARD_IDS
import framework.telegram.business.bridge.Constant.Search.SEARCH_FORWARD_CONTACTS_GROUP
import framework.telegram.business.bridge.Constant.Search.SEARCH_TYPE
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.event.ForwardFinishEvent
import framework.telegram.business.bridge.event.ForwardMessageEvent
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.controller.MessageController
import framework.telegram.message.db.RealmCreator
import framework.telegram.message.manager.*
import framework.telegram.message.ui.forward.adapter.ForwardChatAdapter
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.wordfilter.WordFilter
import framework.telegram.ui.utils.ScreenUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Sort
import kotlinx.android.synthetic.main.msg_activity_forward_chats.*
import kotlinx.android.synthetic.main.msg_search.*

/**
 * 这里有3中情况的
 * 1.传 messageLocalId 的，只要转发一条数据
 * 2.传 messageLocalIds 的，转发多条数据
 * 3.传 msgTextContent 的，这是转发部分文字的，不是一个model
 * 4.只传 targetId 的，就是分享用户卡片的
 * 5.只传 还有就是从外部分享进来的
 */
@Route(path = Constant.ARouter.ROUNTE_MSG_FORWARD_CHATS)
class ForwardToChatsActivity : BaseActivity() {

    private val mMineUid by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
    }

    private val mMessageLocalIds by lazy { ArrayList<Long>() }

    private val mChatType by lazy { intent.getIntExtra("chatType", -1) }

    private val mTargetId by lazy { intent.getLongExtra("targetId", -1) }

    private val mTextContent by lazy { intent.getStringExtra("msgTextContent") ?: "" }

    private val mChatModelsList by lazy { ArrayList<MultiItemEntity>() }

    private val mMessageModels: ArrayList<MessageModel> = ArrayList<MessageModel>()

    private val mIsFromShare by lazy { intent.getBooleanExtra("isFromShare", false) }

    private val mShareFileList by lazy { intent.getSerializableExtra("share_list") as List<HashMap<String, String>>? }

    private val mAdapter by lazy {
        ForwardChatAdapter { chatModel ->
            forwardMessageToChat(chatModel.chaterType, chatModel.chaterId)
        }
    }

    private var mSureTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val messageLocalIds = intent.getStringArrayListExtra("messageLocalIds")
        messageLocalIds?.forEach {
            mMessageLocalIds.add(it.toLong())
        }

        //  如果为1则为转发部分文字，否则为转发其他消息
        val messageLocalId = intent.getLongExtra("messageLocalId", -1)
        if (messageLocalId > 0L) {
            mMessageLocalIds.add(messageLocalId)
        }

        setContentView(R.layout.msg_activity_forward_chats)

        initTitleBar()
        initData()
        initListen()
    }

    @SuppressLint("CheckResult")
    private fun initListen() {
        EventBus.getFlowable(ForwardMessageEvent::class.java)
            .bindToLifecycle(this@ForwardToChatsActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                forwardMessageToChat(event.chaterType, event.chaterId)
            }

        EventBus.getFlowable(ForwardFinishEvent::class.java)
            .bindToLifecycle(this@ForwardToChatsActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                finish()
            }
    }

    private fun initTitleBar() {
        custom_toolbar.showCenterTitle(getString(R.string.send_to))
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        text_view_search_icon.setOnClickListener {
            ARouter.getInstance().build(ROUNTE_BUS_SEARCH_CONTACT)
                .withStringArrayList(FORWARD_IDS, getSendedId())
                .withInt(SEARCH_TYPE, SEARCH_FORWARD_CONTACTS_GROUP).navigation()
        }

        custom_toolbar.showRightTextView("", {
            finish()
        }) {
            val size = ScreenUtils.dp2px(this, 10f)
            mSureTextView = it
            val lp = mSureTextView?.layoutParams as LinearLayout.LayoutParams
            lp.rightMargin = ScreenUtils.dp2px(this, 10f)
            mSureTextView?.setPadding(size, 0, size, 0)
            setSureButton()
        }
    }

    private fun initData() {
        if (mTextContent != null && mTextContent.isNotEmpty()) {//截取消息的转发
            forward_text_view.text = mTextContent

        } else if (mMessageLocalIds.size > 0) {//以消息为单位的转发
            val msgModels = ArrayList<MessageModel>()
            MessagesManager.executeChatTransactionAsync(mChatType, mMineUid, mTargetId, { realm ->
                mMessageLocalIds.forEach {
                    val msgModel =
                        realm.where(MessageModel::class.java).equalTo("id", it).findFirst()
                            ?.copyMessage()
                    if (msgModel != null) {
                        msgModels.add(msgModel)
                    }
                }
            }, {
                if (msgModels.isNullOrEmpty()) {
                    finish()
                } else {
                    mMessageModels.addAll(msgModels)

                    if (mMessageModels.size == 1) {
                        val model = mMessageModels[0]
                        forward_text_view.text = ForwardMessageManager.getForwardMessageTitle(model)
                    } else {
                        forward_text_view.text =
                            String.format(getString(R.string.string_merge_msg), mMessageModels.size)
                    }
                }
            }, {
                finish()
            })
        } else if (mIsFromShare && mShareFileList != null && mShareFileList!!.isNotEmpty()) {
            if (mShareFileList!!.size > 1) {
                forward_text_view.text =
                    String.format(getString(R.string.string_merge_msg), mMessageModels.size)
            } else {
                val file = mShareFileList!![0]
                forward_text_view.text =
                    ForwardMessageManager.getForwardFileTitle(file["mimetype"]!!, file["path"]!!)
            }
        } else {//分享名片
            forward_text_view.text = BaseApp.app.getString(R.string.business_card_sign)
        }
        initListData()
    }

    private fun setSureButton() {
        mSureTextView?.isEnabled = true
        mSureTextView?.text = getString(R.string.accomplish)
    }

    @SuppressLint("CheckResult")
    private fun initListData() {

        mAdapter.setNewData(mChatModelsList)

        mAdapter.setOnItemClickListener { _, view, position ->
            when (mAdapter.getItemViewType(position)) {
                TITLE_SELECT_FRIEND -> {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_FORWARD_SELECT_CONTACTS)
                        .withStringArrayList(FORWARD_IDS, getSendedId())
                        .navigation()
                }
                TITLE_SELECT_GROUP -> {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_FORWARD_SELECT_GROUP)
                        .withStringArrayList(FORWARD_IDS, getSendedId())
                        .navigation()
                }
            }
        }
        recycler_view_history.initMultiTypeRecycleView(
            LinearLayoutManager(this@ForwardToChatsActivity),
            mAdapter,
            false
        )
        recycler_view_history.refreshController().setEnablePullToRefresh(false)

        loadChatHistory()
    }

    @SuppressLint("CheckResult")
    private fun loadChatHistory() {
        val chatModelsList = ArrayList<ChatModel>()
        RealmCreator.executeChatsHistoryTransactionAsync(mMineUid, { realm ->
            val chatModel =
                realm.where(ChatModel::class.java)?.equalTo("chaterType", ChatModel.CHAT_TYPE_PVT)
                    ?.or()?.equalTo("chaterType", ChatModel.CHAT_TYPE_GROUP)
                    ?.sort("isTop", Sort.DESCENDING, "lastMsgTime", Sort.DESCENDING)?.findAll()
            chatModel?.forEach {
                chatModelsList.add(it.copyChat())
            }
        }, {
            mChatModelsList.add(
                TitleModel(
                    getString(R.string.choosing_friends),
                    TITLE_SELECT_FRIEND,
                    R.drawable.common_contacts_icon_friend
                )
            )
            mChatModelsList.add(
                TitleModel(
                    getString(R.string.select_group_chat),
                    TITLE_SELECT_GROUP,
                    R.drawable.common_contacts_icon_group
                )
            )
            if (chatModelsList.size > 0) {
                mChatModelsList.add(
                    TitleModel(
                        getString(R.string.recently),
                        TitleModel.TITLE_HEAD,
                        0
                    )
                )
            }
            mChatModelsList.addAll(chatModelsList)
            recycler_view_history.recyclerViewController()?.notifyDataSetChanged()
        }) {
            finish()
        }
    }

    private fun forwardMessageToChat(chaterType: Int, chaterId: Long) {
        if (!TextUtils.isEmpty(mTextContent)) {
            //转发部分文字
            sendTextMsg(mTextContent, chaterType, chaterId)
        } else if (mMessageModels.size > 0) {
            //转发多条内容
            ThreadUtils.runOnIOThread {
                // 在异步线程执行，每隔500毫秒发送一次
                mMessageModels.forEach {
                    ForwardMessageManager.forwardMessage(it, chaterType, chaterId)
                    ThreadUtils.sleep(500)
                }
            }
        } else if (mIsFromShare && mShareFileList != null && mShareFileList!!.isNotEmpty()) {
            mShareFileList?.let {
                for (hashMap in it) {
                    val filePath = hashMap["path"]!!
                    val mimeType = hashMap["mimetype"]!!
                    ShareMessageManager.shareMessage(
                        filePath,
                        mimeType,
                        mMineUid,
                        chaterId,
                        chaterType
                    )
                }
            }
        } else {
            //分享名片
            if (chaterType == ChatModel.CHAT_TYPE_PVT) {
                SendMessageManager.sendNameCardMessageToUser(mTargetId, null, mMineUid, chaterId)
            } else {
                SendMessageManager.sendNameCardMessageToGroup(mTargetId, null, mMineUid, chaterId)
            }
        }
        val editText = edit_text_input.text.toString().trim()
        if (!TextUtils.isEmpty(editText)) {
            sendTextMsg(editText, chaterType, chaterId)
        }

        mAdapter.addSendedId(chaterId)
        mAdapter.notifyDataSetChanged()
        setSureButton()
    }

    private fun sendTextMsg(content: String, chaterType: Int, chaterId: Long) {
        if (chaterType == ChatModel.CHAT_TYPE_PVT) {
            ArouterServiceManager.contactService.getContactInfo(
                lifecycle(),
                chaterId,
                { contactInfoModel, _ ->
                    val atUidsValue = ArrayList<Long>().toLongArray()
                    val msgModel = MessageModel.createTextMessage(
                        WordFilter.doFilter(content),
                        ArouterServiceManager.messageService.getCurrentTime(),
                        atUidsValue,
                        null,
                        if (contactInfoModel.isBfReadCancel) contactInfoModel.msgCancelTime else 0,
                        mMineUid,
                        chaterId, chaterType
                    )
                    MessageController.saveMessage(mMineUid, chaterId, chaterType, msgModel) {
                        SendMessageManager.sendMessagePackage(
                            chaterType,
                            mMineUid,
                            it,
                            null,
                            contactInfoModel
                        )
                    }
                })
        } else {
            ArouterServiceManager.groupService.getGroupInfo(lifecycle(), chaterId, { groupinfo, _ ->
                val atUidsValue = ArrayList<Long>().toLongArray()
                val msgModel = MessageModel.createTextMessage(
                    WordFilter.doFilter(content),
                    ArouterServiceManager.messageService.getCurrentTime(),
                    atUidsValue,
                    null,
                    if (groupinfo.bfGroupReadCancel) groupinfo.groupMsgCancelTime else 0,
                    mMineUid,
                    chaterId, chaterType
                )
                MessageController.saveMessage(mMineUid, chaterId, chaterType, msgModel) {
                    SendMessageManager.sendMessagePackage(
                        chaterType,
                        mMineUid,
                        it,
                        groupInfo = groupinfo
                    )
                }
            })
        }
    }

    private fun getSendedId(): ArrayList<String> {
        val list = ArrayList<String>()
        mAdapter.getSendedModleId().forEach {
            list.add(it.toString())
        }
        return list
    }


}
