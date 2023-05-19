package framework.telegram.message.manager

import android.text.TextUtils
import com.im.pb.IMPB
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.message.R
import framework.telegram.message.controller.StreamCallController
import framework.telegram.message.db.RealmCreator
import framework.telegram.support.BaseApp
import io.realm.Realm

object RecallMessageManager {

    /**
     * 撤回消息(私聊)
     */
    fun recallToUserMessage(
        myUid: Long,
        targetUid: Long,
        msgs: List<IMPB.RecallMessage>,
        isReceive: Boolean,
        complete: ((MessageModel?, Int) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        var recallUnreadCount = 0
        var copyLastMessage: MessageModel? = null
        RealmCreator.executePvtChatTransactionAsync(myUid, targetUid, { realm ->
            msgs.forEach { msg ->
                when (msg.clear) {
                    0 -> {
                        //0: 默认删单条消息
                        recallUnreadCount =
                            recallMessage(
                                myUid,
                                targetUid,
                                realm,
                                msg.msgId,
                                msg.channelName,
                                isReceive
                            )
                    }
                    1 -> {
                        //1： 清空所有消息
                        recallUnreadCount =
                            recallAllMessage(
                                myUid, targetUid, realm, msg.clearTime, isReceive
                            )
                    }
                    2 -> {
                        //2： 清空所有消息和会话记录
                        recallUnreadCount =
                            recallAllMessage(
                                myUid, targetUid, realm, msg.clearTime, isReceive
                            )
                        ChatsHistoryManager.deleteChatWithLastMsgTime(
                            myUid,
                            ChatModel.CHAT_TYPE_PVT,
                            targetUid,
                            msg.clearTime
                        )
                    }
                }
            }
            copyLastMessage = MessagesManager.findLastBizMessage(realm)?.copyMessage()
        }, {
            complete?.invoke(copyLastMessage, recallUnreadCount)
        }, error)
    }

    /**
     * 重发消息(群聊)
     */
    fun recallToGroupMessage(
        myUid: Long,
        targetGid: Long,
        msgs: List<IMPB.RecallMessage>,
        isReceive: Boolean,
        complete: ((MessageModel?, Int) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        var recallUnreadCount = 0
        var copyLastMessage: MessageModel? = null
        RealmCreator.executeGroupChatTransactionAsync(myUid, targetGid, { realm ->
            msgs.forEach { msg ->
                when (msg.clear) {
                    0 -> {
                        //0: 默认删单条消息
                        recallUnreadCount =
                            recallMessage(
                                myUid,
                                targetGid,
                                realm,
                                msg.msgId,
                                msg.channelName,
                                isReceive
                            )
                    }
                    1 -> {
                        //1： 清空所有消息
                        recallUnreadCount =
                            recallAllMessage(
                                myUid,
                                targetGid,
                                realm,
                                msg.clearTime,
                                isReceive
                            )
                    }
                    2 -> {
                        //2： 清空所有消息和会话记录
                        recallUnreadCount =
                            recallAllMessage(
                                myUid,
                                targetGid,
                                realm,
                                msg.clearTime,
                                isReceive
                            )
                        ChatsHistoryManager.deleteChatWithLastMsgTime(
                            myUid,
                            ChatModel.CHAT_TYPE_GROUP,
                            targetGid,
                            msg.clearTime
                        )
                    }
                }
            }
            copyLastMessage = MessagesManager.findLastBizMessage(realm)?.copyMessage()
        }, {
            complete?.invoke(copyLastMessage, recallUnreadCount)
        }, error)
    }

    private fun recallMessage(
        myUid: Long,
        targetId: Long,
        realm: Realm,
        msgId: Long,
        channelName: String,
        isReceive: Boolean
    ): Int {
        val model = if (msgId > 0) {
            realm.where(MessageModel::class.java).equalTo("msgId", msgId).findFirst()
        } else if (!TextUtils.isEmpty(channelName)) {
            realm.where(MessageModel::class.java).equalTo("flag", channelName).findFirst()
        } else {
            null
        }

        var recallUnreadCount = 0
        model?.let {
            if (it.type == MessageModel.MESSAGE_TYPE_STREAM) {
                StreamCallController.deleteStreamCall(myUid, targetId, it.flag)
            }

            if (!isReceive) {
                it.type = MessageModel.MESSAGE_TYPE_RECALL
            } else {
                it.type = MessageModel.MESSAGE_TYPE_RECALL_SUCCESS
            }

            if (it.isSend == 0) {
                // 收到的消息
                it.content = String.format(
                    BaseApp.app.getString(R.string.a_message_was_withdrawn),
                    it.ownerName
                )

                it.contentBytes = ArrayList<Byte>().toByteArray()
                recallUnreadCount = if (it.isRead == 0) 1 else 0
                it.isRead = 1
                it.readTime = ArouterServiceManager.messageService.getCurrentTime()
            }

            it.atUids = ""
            it.isAtMe = 0
            realm.copyToRealmOrUpdate(it)

            // 清除文件缓存
            MessagesManager.deleteMessageFileCache(it.copyMessage())
            MessagesManager.deleteMessageFromDB(mutableListOf(it.copyMessage()))
        }

        return recallUnreadCount
    }

    private fun recallAllMessage(
        myUid: Long,
        targetId: Long,
        realm: Realm,
        clearTime: Long,
        isReceive: Boolean
    ): Int {
        val models =
            realm.where(MessageModel::class.java).lessThanOrEqualTo("time", clearTime).findAll()
        var recallUnreadCount = 0
        models?.forEach {
            if (it.type == MessageModel.MESSAGE_TYPE_STREAM) {
                StreamCallController.deleteStreamCall(myUid, targetId, it.flag)
            }

            if (!isReceive) {
                it.type = MessageModel.MESSAGE_TYPE_RECALL
            } else {
                it.type = MessageModel.MESSAGE_TYPE_RECALL_SUCCESS
            }

            if (it.isSend == 0) {
                // 收到的消息
                it.content = String.format(
                    BaseApp.app.getString(R.string.a_message_was_withdrawn),
                    it.ownerName
                )

                it.contentBytes = ArrayList<Byte>().toByteArray()
                if (it.isRead == 0) {
                    recallUnreadCount += 1
                }
                it.isRead = 1
                it.readTime = ArouterServiceManager.messageService.getCurrentTime()
            }

            it.atUids = ""
            it.isAtMe = 0
            realm.copyToRealmOrUpdate(it)

            // 清除文件缓存
            MessagesManager.deleteMessageFileCache(it.copyMessage())
            MessagesManager.deleteMessageFromDB(mutableListOf(it.copyMessage()))
        }

        return recallUnreadCount
    }
}