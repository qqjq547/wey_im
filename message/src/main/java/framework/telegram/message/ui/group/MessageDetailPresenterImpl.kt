package framework.telegram.message.ui.group

import android.content.Context
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.im.domain.pb.CommonProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.common.TitleModel.TITLE_HEAD
import framework.ideas.common.model.group.GroupChatDetailModel
import framework.ideas.common.model.group.PvtChatDetailModel
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.ideas.common.model.im.MessageReceiptModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.controller.MessageController
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.manager.MessagesManager
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.BitUtils
import framework.telegram.support.tools.Helper
import io.reactivex.Observable
import io.realm.Sort

class MessageDetailPresenterImpl(view: MessageDetailContract.View, val context: Context, val observable: Observable<ActivityEvent>, val chatType: Int, val targetId: Long) : MessageDetailContract.Presenter {

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private var mMessageModel: MessageModel? = null

    private val mDataList by lazy { mutableListOf<MultiItemEntity>() }

    private val mView: MessageDetailContract.View = view

    var mCurStatue = ""

    init {
        view.setPresenter(this)
    }

    override fun start() {

    }

    override fun loadModelData(msgLocalId: Long) {
        MessageController.executeChatTransactionAsyncWithResult(chatType, mMineUid, targetId, {
            it.where(MessageModel::class.java)?.equalTo("id", msgLocalId)?.findFirst()?.copyMessage()
        }, { model ->
            if (model != null) {
                mMessageModel = model

                if (chatType == ChatModel.CHAT_TYPE_GROUP) {
                    mDataList.clear()
                    mDataList.add(TitleModel("", TITLE_HEAD, 0))
                    mDataList.add(model)

                    loadPageData(model.msgId)
                } else {
                    val tempList = mutableListOf<MultiItemEntity>()
                    if (model.type == MessageModel.MESSAGE_TYPE_VOICE) {
                        tempList.add(PvtChatDetailModel(CommonProto.MsgReceiptStatusBase
                                .newBuilder()
                                .setStatus(CommonProto.MsgReceiptStatus.PLAYED)
                                .setTime(mMessageModel?.readedAttachmentTime ?: 0L).build()))
                    }


                    ArouterServiceManager.contactService.getContactInfo(observable, targetId, { contactInfoModel, _ ->
                        val privacy = AccountManager.getLoginAccount(AccountInfo::class.java).getPrivacy()
                        val isClose = BitUtils.checkBitValue(Helper.int2Bytes(privacy)[2], 7)

                        val viewedItem = PvtChatDetailModel(CommonProto.MsgReceiptStatusBase
                                .newBuilder()
                                .setStatus(CommonProto.MsgReceiptStatus.VIEWED)
                                .setTime(mMessageModel?.readTime ?: 0).build())
                        viewedItem.isShowAlreadyRead = !isClose && contactInfoModel.isReadReceipt
                        tempList.add(viewedItem)

                        tempList.add(PvtChatDetailModel(CommonProto.MsgReceiptStatusBase.newBuilder()
                                .setStatus(CommonProto.MsgReceiptStatus.DELIVERED)
                                .setTime(mMessageModel?.deliverTime ?: 0).build()))
                        mDataList.clear()
                        mDataList.add(TitleModel("", TITLE_HEAD, 0))
                        mDataList.add(model)
                        mDataList.addAll(tempList)

                        mView.loadPageSuccess(mDataList)
                    })
                }
            } else {
                mView.loadPageFail(context.getString(R.string.common_fail), false)
            }
        }) {
            mView.loadPageFail(context.getString(R.string.common_fail), false)
        }
    }

    private fun loadPageData(msgId: Long) {
        val messageReceiptModels = mutableListOf<MessageReceiptModel>()
        MessagesManager.executeChatTransactionAsync(ChatModel.CHAT_TYPE_GROUP, mMineUid, targetId, {
            val models = it.where(MessageReceiptModel::class.java)?.equalTo("msgId", msgId)?.and()?.notEqualTo("senderUid", mMineUid)
                    ?.sort(mutableListOf("readedAttachmentTime", "readTime", "deliverTime").toTypedArray(), mutableListOf(Sort.DESCENDING, Sort.DESCENDING, Sort.DESCENDING).toTypedArray())
                    ?.findAll()
            models?.forEach { model ->
                messageReceiptModels.add(model.copyMessage())
            }
        }, {
            mCurStatue = ""
            mDataList.addAll(getSortListData(messageReceiptModels))
            mView.loadPageSuccess(mDataList)
        }) {
            mView.loadPageFail(context.getString(R.string.common_fail), false)
        }
    }

    private fun getSortListData(result: List<MessageReceiptModel>): MutableList<MultiItemEntity> {
        val tempList = mutableListOf<MultiItemEntity>()
        result.forEach {
            val status = getTag(it)
            if (mCurStatue != status) {
                mCurStatue = status
                tempList.add(TitleModel(mCurStatue, TITLE_HEAD, 0))
            }
            tempList.add(GroupChatDetailModel(it, status != context.getString(R.string.delivery)))
        }
        return tempList
    }

    private fun getTag(model: MessageReceiptModel): String {
        var status1 = false
        var status2 = false
        var status3 = false
        when {
            model.readedAttachmentTime > 0 -> status1 = true
            model.readTime > 0 -> status2 = true
            model.deliverTime > 0 -> status3 = true
        }

        return when {
            status1 -> context.getString(R.string.play)
            status2 -> context.getString(R.string.read)
            status3 -> context.getString(R.string.delivery)
            else -> ""
        }
    }

    override fun destory() {

    }
}