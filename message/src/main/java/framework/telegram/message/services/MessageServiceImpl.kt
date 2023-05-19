package framework.telegram.message.services

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.ideas.common.model.im.StreamCallModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.service.IMessageService
import framework.telegram.message.controller.MessageController
import framework.telegram.message.db.RealmCreator
import framework.telegram.message.controller.StreamCallController
import framework.telegram.message.event.ScreenShotDetectionEvent
import framework.telegram.message.manager.*
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import io.realm.Realm

@Route(path = Constant.ARouter.ROUNTE_SERVICE_MESSAGE, name = "消息服务")
class MessageServiceImpl : IMessageService {


    override fun init(context: Context?) {

    }

    override fun sendGroupNoticeMessageToGroup(
        noticeId: Long,
        noticeContent: String,
        showNotify: Boolean,
        myUid: Long,
        targetGid: Long
    ) {
        SendMessageManager.sendGroupNoticeMessageToGroup(
            noticeId,
            noticeContent,
            showNotify,
            myUid,
            targetGid
        )
    }

    override fun setFileTransferUserChatIsTop() {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        ChatsHistoryManager.checkChatHistoryIsCreated(
            ChatModel.CHAT_TYPE_PVT,
            myUid = myUid,
            targetId = Constant.Common.FILE_TRANSFER_UID
        ) {

        }
    }

    override fun getCurrentTime(): Long {
        return System.currentTimeMillis() + ReceiveMessageManager.serverDifferenceTime
    }

    override fun socketIsLogin(): Boolean {
        return ReceiveMessageManager.socketIsLogin
    }

    override fun sendScreenShotsPackage(sendUid: Long, receiveUid: Long) {
        SendMessageManager.sendScreenShotsPackage(sendUid, receiveUid)
        EventBus.publishEvent(ScreenShotDetectionEvent(sendUid, receiveUid))
    }

    override fun getChatTopStatus(
        chaterType: Int,
        targetId: Long,
        complete: ((Boolean) -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        ChatsHistoryManager.getChatTopStatus(myUid, chaterType, targetId, complete, error)
    }

    override fun setChatTopStatus(
        chaterType: Int,
        targetId: Long,
        isTop: Boolean,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        ChatsHistoryManager.checkChatHistoryIsCreated(chaterType, myUid, targetId, complete = {
            ChatsHistoryManager.setChatTopStatus(
                myUid,
                chaterType,
                targetId,
                isTop,
                complete,
                error
            )
        })
    }

    override fun setChatIsReaded(
        chaterType: Int,
        targetId: Long,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        ChatsHistoryManager.setChatMessagesAllReaded(myUid, chaterType, targetId, complete, error)
    }

    override fun setChatIsUnreaded(
        chaterType: Int,
        targetId: Long,
        unreadCount: Int,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        ChatsHistoryManager.setChatUnReadCount(
            myUid,
            chaterType,
            targetId,
            unreadCount,
            complete,
            error
        )
    }

    override fun setChatIsDisturb(
        chaterType: Int,
        targetId: Long,
        disturb: Boolean,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, {
            val chatModel = it.where(ChatModel::class.java).equalTo("chaterType", chaterType).and()
                .equalTo("chaterId", targetId).findFirst()
            chatModel?.let {
                chatModel.bfDisturb = if (disturb) 1 else 0
            }
            it.copyToRealmOrUpdate(chatModel)
        }, complete, error)
    }

    override fun getChatIsDisturb(
        chaterType: Int,
        targetId: Long,
        complete: ((Boolean) -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        var bfDisturb = false
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, {
            val chatModel = it.where(ChatModel::class.java).equalTo("chaterType", chaterType).and()
                .equalTo("chaterId", targetId).findFirst()
            chatModel?.let {
                bfDisturb = chatModel.bfDisturb == 1
            }
        }, {
            complete?.invoke(bfDisturb)
        })
    }

    override fun insertSystemTipMsg(
        chaterType: Int,
        targetId: Long,
        createTime: Long,
        content: String,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        // 插入一条系统消息到消息列表中，并生成会话记录
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        MessageController.executeChatTransactionAsyncWithResult(
            chaterType,
            myUid,
            targetId,
            { realm ->
                val msgModel = MessageModel.parseSystemTipMessage(
                    myUid,
                    content,
                    if (createTime > 0L) createTime else ArouterServiceManager.messageService.getCurrentTime(),
                    targetId,
                    "",
                    "",
                    targetId,
                    myUid,
                    chaterType
                )
                msgModel?.let {
                    realm.copyToRealm(msgModel)
                }

                msgModel.copyMessage()
            },
            { copyMsgModel ->
                // 更新会话
                ChatsHistoryManager.checkChatHistoryIsCreated(
                    chaterType,
                    myUid,
                    targetId,
                    copyMsgModel
                ) {
                    complete?.invoke()
                }
            },
            error
        )
    }

    override fun clearMessageHistory(
        chaterType: Int,
        targetId: Long,
        complete: (() -> Unit)?,
        error: (() -> Unit)?
    ) {
        MessageController.clearMessageHistory(chaterType, targetId)
    }

    override fun deleteAllChat(complete: (() -> Unit)?, error: (() -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        MessageController.deleteAllChat(myUid)
    }

    override fun deleteAccountData(myUid: Long) {
        MessageController.deleteAccountData(myUid)
    }

    override fun getAllChat(complete: ((List<ChatModel>) -> Unit)?, error: ((Throwable) -> Unit)?) {
        MessageController.getAllChat(complete, error)
    }

    override fun deleteChat(
        chaterType: Int,
        targetId: Long,
        complete: (() -> Unit)?,
        error: (() -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        MessageController.deleteChat(myUid, chaterType, targetId)
    }

    override fun deleteToGroupAllMessageReceipt(
        targetGid: Long,
        targerUid: Long,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        MessagesManager.deleteToGroupAllMessageReceipt(myUid, targetGid, targerUid, complete, error)
    }

    override fun resetChaterInfo(
        chaterType: Int,
        targetId: Long,
        chaterName: String?,
        chaterNickName: String?,
        chaterIcon: String?,
        bfDisturb: Boolean?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        ChatsHistoryManager.updateChaterInfo(
            myUid,
            chaterType,
            targetId,
            chaterName,
            chaterNickName,
            chaterIcon,
            bfDisturb
        ) {}
    }

    override fun resetStreamCallChaterInfo(
        targetId: Long,
        chaterName: String?,
        chaterNickName: String?,
        chaterIcon: String?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeStreamCallsHistoryTransactionAsync(myUid, {
            val msgModels =
                it.where(StreamCallModel::class.java).equalTo("chaterId", targetId).findAll()
            msgModels?.forEach { item ->
                if (chaterName != null) {
                    item.chaterName = chaterName
                }
                if (chaterNickName != null) {
                    item.chaterNickName = chaterNickName
                }
                if (chaterIcon != null) {
                    item.chaterIcon = chaterIcon
                }
            }
            it.copyToRealmOrUpdate(msgModels)
        })
    }

    override fun deleteStreamCallHistory(
        targetId: Long,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        StreamCallController.deleteStreamCallHistory(myUid, targetId, complete, error)
    }

    override fun getAllUnreadMessageCount(): Int {
        return ChatsHistoryManager.syncGetAllUnreadMessageCount()
    }

    override fun insertGroupTipMessage(
        groupId: Long,
        msg: String,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        MessagesManager.insertGroupTipMessage(groupId, msg, complete, error)
    }

    override fun newStreamCallRealm(): Realm {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        return RealmCreator.getStreamCallHistoryRealm(myUid)
    }

    override fun newSearchContentRealm(targetGid: Long): Realm {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        return RealmCreator.getPvtChatMessagesRealm(myUid, targetGid)
    }

    override fun newSearchGroupContentRealm(targetGid: Long): Realm {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        return RealmCreator.getGroupChatMessagesRealm(myUid, targetGid)
    }

    override fun updateChatLastMsg(
        myUid: Long,
        chaterType: Int,
        chaterId: Long,
        lastMsg: MessageModel?,
        unreadCount: Int?,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?,
        changeTime: Boolean
    ) {
        ChatsHistoryManager.updateChatLastMsg(
            myUid,
            chaterType,
            chaterId,
            lastMsg,
            changeTime = changeTime
        )
    }

    override fun findChatLastMsg(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        complete: (lastBizMessage: MessageModel?) -> Unit,
        error: ((Throwable) -> Unit)?
    ) {
        MessagesManager.findChatLastMsg(chatType, myUid, targetId, complete, error)
    }

    override fun executeChatsHistoryTransactionAsync(
        myUid: Long,
        cmd: (Realm) -> Unit,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, cmd, complete, error)
    }

    override fun createChatHistory(
        chaterType: Int,
        myUid: Long,
        targetId: Long,
        forcedCreate: Boolean
    ) {
        ChatsHistoryManager.checkChatHistoryIsCreated(
            chaterType,
            myUid,
            targetId,
            forcedCreate = true
        ) {}
    }

    override fun syncAllChatMessage(myUid: Long, finish: () -> Unit) {
        SearchSyncManager.syncAllChatMessage(myUid, finish)
    }

    override fun findChatMsg(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        msgId: Long,
        complete: (message: MessageModel?) -> Unit,
        error: ((Throwable) -> Unit)?
    ) {
        MessagesManager.findChatMsg(chatType, myUid, targetId, msgId, complete, error)
    }

    override fun recallMessage(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        msgId: Long,
        isMine: Boolean
    ) {
        MessageController.recallMessage(
            chatType,
            myUid,
            targetId,
            msgId
        )
    }

    override fun recallMessages(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        clearTime: Long,
        deleteChat: Boolean
    ) {
        MessageController.recallMessages(
            chatType,
            myUid,
            targetId,
            clearTime,
            deleteChat
        )
    }
}