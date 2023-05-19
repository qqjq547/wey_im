package framework.telegram.message.controller

import android.text.TextUtils
import android.util.Log
import com.im.domain.pb.CommonProto
import com.im.pb.IMPB
import framework.ideas.common.model.common.SearchChatModel
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.ideas.common.model.im.StreamCallModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.*
import framework.telegram.message.db.RealmCreator
import framework.telegram.message.manager.*
import framework.telegram.message.ui.group.GroupChatActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.BitUtils
import framework.telegram.support.tools.Helper
import io.realm.Realm
import java.lang.IllegalStateException

object MessageController {

    fun receiveUserMessage(
        myUid: Long,
        targetUid: Long,
        msgModels: List<MessageModel>,
        complete: () -> Unit
    ) {
        if (msgModels.isNullOrEmpty()) {
            return
        }

        var unReadCount = 0L
        RealmCreator.executePvtChatTransactionAsync(myUid, targetUid, { realm ->
            val msgs = mutableListOf<MessageModel>()
            msgModels.forEach {
                val model =
                    realm.where(MessageModel::class.java)?.equalTo("msgId", it.msgId)?.findFirst()
                if (model == null) {
                    msgs.add(it)
                }
            }

            unReadCount = realm.where(MessageModel::class.java).equalTo("isSend", 0.toInt()).and()
                .equalTo("isRead", 0.toInt()).count() + msgs.size
            realm.copyToRealm(msgs)
        }, {
            //保存数据到全文搜索数据库
            saveMessageToSearchDB(msgModels)
            // 创建会话或更新会话
            msgModels.last().let { lastMsg ->
                ChatsHistoryManager.checkChatHistoryIsCreated(
                    ChatModel.CHAT_TYPE_PVT,
                    myUid,
                    targetUid,
                    lastMsg.copyMessage(),
                    unReadCount.toInt()
                ) {
                    if (unReadCount > 0) {
                        EventBus.publishEvent(
                            UnreadMessageEvent(
                                ChatModel.CHAT_TYPE_PVT,
                                targetUid
                            )
                        )
                    }
                    // 发送收到新消息的事件
                    EventBus.publishEvent(ReciveMessageEvent(ChatModel.CHAT_TYPE_PVT, targetUid))


                    // 获取对方的好友资料(如果本地已存在是不会重复从网络获取的)
                    ArouterServiceManager.contactService.getContactInfo(
                        null,
                        targetUid,
                        { contactInfoModel, _ ->
                            //Log.i("lzh", "chaterName ${contactInfoModel.displayName}  bfDisturb ${contactInfoModel.isBfDisturb}")
                            if (!contactInfoModel.isBfDisturb || lastMsg.isAtMe == 1) {
                                // 发送通知栏事件
                                EventBus.publishEvent(
                                    NotificationEvent(
                                        msgModels.last().senderId,
                                        contactInfoModel.displayName,
                                        Constant.Push.PUSH_TYPE.MSG_PVT
                                    )
                                )
                            }
                        })
                }
            }
            complete.invoke()
        })
    }

    fun receiveMyselfWebUserMessage(
        myUid: Long,
        targetUid: Long,
        msgModels: List<MessageModel>,
        complete: () -> Unit
    ) {
        if (msgModels.isNullOrEmpty()) {
            return
        }

        RealmCreator.executePvtChatTransactionAsync(myUid, targetUid, { realm ->
            val msgs = mutableListOf<MessageModel>()
            msgModels.forEach {
                val model =
                    realm.where(MessageModel::class.java)?.equalTo("msgId", it.msgId)?.findFirst()
                if (model == null) {
                    msgs.add(it)
                }
            }

            realm.copyToRealm(msgs)
        }, {
            //保存数据到全文搜索数据库
            saveMessageToSearchDB(msgModels)

            // 创建会话或更新会话
            msgModels.last().let { lastMsg ->
                ChatsHistoryManager.checkChatHistoryIsCreated(
                    ChatModel.CHAT_TYPE_PVT,
                    myUid,
                    targetUid,
                    lastMsg.copyMessage()
                ) {

                }
            }
            complete.invoke()
        })
    }

    fun receiveGroupMessage(
        myUid: Long,
        targetGid: Long,
        msgModels: List<MessageModel>,
        complete: () -> Unit
    ) {
        if (msgModels.isNullOrEmpty()) {
            return
        }

        //查询@数量
        var atMeMsgCount = 0
        msgModels.forEach { msg ->
            if (msg.isAtMe == 1) {
                atMeMsgCount++
            }
        }

        //查询未读数量
        var unReadCount = 0L
        RealmCreator.executeGroupChatTransactionAsync(myUid, targetGid, { realm ->
            val msgs = mutableListOf<MessageModel>()
            msgModels.forEach {
                val model =
                    realm.where(MessageModel::class.java)?.equalTo("msgId", it.msgId)?.findFirst()
                if (model == null) {
                    msgs.add(it)
                }
            }

            unReadCount = realm.where(MessageModel::class.java).equalTo("isSend", 0.toInt()).and()
                .equalTo("isRead", 0.toInt()).count() + msgs.size
            realm.copyToRealm(msgs)
        }, {
            //保存数据到全文搜索数据库
            saveMessageToSearchDB(msgModels)

            msgModels.last().let {
                // 更新会话
                val lastMsg = it.copyMessage()
                val memberName = lastMsg.ownerName
                ArouterServiceManager.groupService.getGroupInfo(null, targetGid, { data, _ ->
                    if (it.noticeMessageBean != null && it.noticeMessageBean.showNotify) {
                        EventBus.publishEvent(
                            NotificationEvent(
                                it.senderId,
                                data.copyGroupInfoModel().name,
                                Constant.Push.PUSH_TYPE.MSG_GROUP_AT_NOTICE,
                                it.noticeMessageBean.content
                            )
                        )
                    } else if (it.isAtMe == 1) {
                        EventBus.publishEvent(
                            NotificationEvent(
                                it.senderId,
                                data.copyGroupInfoModel().name,
                                Constant.Push.PUSH_TYPE.MSG_GROUP_AT,
                                BaseApp.app.getString(R.string.some_one_at_me)
                            )
                        )
                    } else if (!data.bfDisturb || lastMsg.isAtMe == 1) {
                        EventBus.publishEvent(
                            NotificationEvent(
                                it.senderId,
                                data.copyGroupInfoModel().name,
                                Constant.Push.PUSH_TYPE.MSG_GROUP
                            )
                        )
                    }
                })

                val topActivity = ActivitiesHelper.getInstance().topActivity
                if (topActivity != null && topActivity.javaClass == GroupChatActivity::class.java) {//如果当前页面是 GroupChatActivity ,如要把当前的unReadCount 清零
                    if ((topActivity as GroupChatActivity).mTargetGid == targetGid) {
                        unReadCount = 0
                    }
                }

                // 获取到信息时
                ChatsHistoryManager.checkChatHistoryIsCreated(
                    ChatModel.CHAT_TYPE_GROUP,
                    myUid,
                    targetGid,
                    lastMsg,
                    unReadCount.toInt(),
                    atMeMsgCount,
                    memberName = memberName
                ) {
                    if (unReadCount > 0) {
                        EventBus.publishEvent(
                            UnreadMessageEvent(
                                ChatModel.CHAT_TYPE_GROUP,
                                targetGid
                            )
                        )
                    }

                    if (atMeMsgCount > 0) {
                        EventBus.publishEvent(AtMeMessageEvent(targetGid))
                    }
                    EventBus.publishEvent(ReciveMessageEvent(ChatModel.CHAT_TYPE_GROUP, targetGid))
                }
            }
            complete.invoke()
        })
    }

    fun receiveMyselfWebGroupMessage(
        myUid: Long,
        targetGid: Long,
        msgModels: List<MessageModel>,
        complete: () -> Unit
    ) {
        if (msgModels.isNullOrEmpty()) {
            return
        }

        //查询未读数量
        RealmCreator.executeGroupChatTransactionAsync(myUid, targetGid, { realm ->
            val msgs = mutableListOf<MessageModel>()
            msgModels.forEach {
                val model =
                    realm.where(MessageModel::class.java)?.equalTo("msgId", it.msgId)?.findFirst()
                if (model == null) {
                    msgs.add(it)
                }
            }

            realm.copyToRealm(msgs)
        }, {
            //保存数据到全文搜索数据库
            saveMessageToSearchDB(msgModels)

            msgModels.last().let {
                // 更新会话
                val lastMsg = it.copyMessage()
                ChatsHistoryManager.checkChatHistoryIsCreated(
                    ChatModel.CHAT_TYPE_GROUP,
                    myUid,
                    targetGid,
                    lastMsg
                ) {
                }
            }

            complete.invoke()
        })
    }

    fun saveMessage(
        myUid: Long,
        targetId: Long,
        chatType: Int,
        msgModel: MessageModel,
        isGroupSend: Boolean = false,
        contactInfo: ContactDataModel? = null,
        complete: ((MessageModel) -> Unit)? = null
    ) {
        if (chatType == ChatModel.CHAT_TYPE_PVT) {
            val privacy = AccountManager.getLoginAccount(AccountInfo::class.java).getPrivacy()
            val isClose = BitUtils.checkBitValue(Helper.int2Bytes(privacy)[2], 7)

            if (contactInfo != null) {
                if (isClose || !contactInfo.isReadReceipt) {
                    msgModel.isShowAlreadyRead = false
                }

                executeChatTransactionAsyncWithResult(chatType, myUid, targetId, { realm ->
                    realm.copyToRealm(msgModel)
                    msgModel.copyMessage()
                }, { result ->
                    result?.let {
                        // 更新会话 如果是群发，就不检测是否有会话窗
                        ChatsHistoryManager.checkChatHistoryIsCreated(
                            chatType,
                            myUid,
                            targetId,
                            it,
                            forcedCreate = !isGroupSend
                        )
                        // call downloadComplete
                        complete?.invoke(it)
                    }
                })
            } else {
                ArouterServiceManager.contactService.getContactInfo(null, targetId, { info, _ ->
                    if (isClose || !info.isReadReceipt) {
                        msgModel.isShowAlreadyRead = false
                    }

                    executeChatTransactionAsyncWithResult(chatType, myUid, targetId, { realm ->
                        realm.copyToRealm(msgModel)
                        msgModel.copyMessage()
                    }, { result ->
                        result?.let {
                            // 更新会话 如果是群发，就不检测是否有会话窗
                            ChatsHistoryManager.checkChatHistoryIsCreated(
                                chatType,
                                myUid,
                                targetId,
                                it,
                                forcedCreate = !isGroupSend
                            )
                            // call downloadComplete
                            complete?.invoke(it)
                        }
                    })
                }, {
                    executeChatTransactionAsyncWithResult(chatType, myUid, targetId, { realm ->
                        realm.copyToRealm(msgModel)
                        msgModel.copyMessage()
                    }, { result ->
                        result?.let {
                            // 更新会话
                            ChatsHistoryManager.checkChatHistoryIsCreated(
                                chatType,
                                myUid,
                                targetId,
                                it
                            )
                            // call downloadComplete
                            complete?.invoke(it)
                        }
                    })
                })
            }
        } else {
            executeChatTransactionAsyncWithResult(chatType, myUid, targetId, { realm ->
                realm.copyToRealm(msgModel)
                msgModel.copyMessage()
            }, { result ->
                result?.let {
                    // 更新会话
                    ChatsHistoryManager.checkChatHistoryIsCreated(chatType, myUid, targetId, it)
                    // call downloadComplete
                    complete?.invoke(it)
                }
            })
        }
    }

    fun executeChatTransactionAsyncWithResult(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        cmd: (Realm) -> MessageModel?,
        complete: ((MessageModel?) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        when (chatType) {
            ChatModel.CHAT_TYPE_PVT -> RealmCreator.executePvtChatTransactionAsyncWithResult(
                myUid,
                targetId,
                {
                    cmd.invoke(it)
                },
                complete,
                error
            )
            ChatModel.CHAT_TYPE_GROUP -> RealmCreator.executeGroupChatTransactionAsyncWithResult(
                myUid,
                targetId,
                {
                    cmd.invoke(it)
                },
                complete,
                error
            )
            else -> error?.invoke(IllegalStateException())
        }
    }

    fun deleteMessage(chatType: Int, myUid: Long, targetUid: Long, msgLocalId: Long) {
        when (chatType) {
            ChatModel.CHAT_TYPE_PVT -> {
                MessagesManager.deleteToUserMessage(
                    myUid,
                    targetUid,
                    msgLocalId,
                    { preMessage, deleteIsUnread ->
                        // 更新会话，并减去未读数量1个
                        ChatsHistoryManager.updateChatLastMsg(
                            myUid,
                            ChatModel.CHAT_TYPE_PVT,
                            targetUid,
                            preMessage,
                            if (deleteIsUnread) -1 else null
                        )
                    })
            }
            ChatModel.CHAT_TYPE_GROUP -> {
                MessagesManager.deleteToGroupMessage(
                    myUid,
                    targetUid,
                    msgLocalId,
                    { preMessage, deleteIsUnread ->
                        // 更新会话，并减去未读数量1个
                        ChatsHistoryManager.updateChatLastMsg(
                            myUid,
                            ChatModel.CHAT_TYPE_GROUP,
                            targetUid,
                            preMessage,
                            if (deleteIsUnread) -1 else null
                        )
                    })
            }
        }
    }

    fun deleteStreamCallMessage(myUid: Long, targetUid: Long, flag: String) {
        StreamCallController.deleteStreamCall(
            myUid,
            targetUid,
            flag,
            { preMessage, deleteIsUnread ->
                ChatsHistoryManager.updateChatLastMsg(
                    myUid,
                    ChatModel.CHAT_TYPE_PVT,
                    targetUid,
                    preMessage,
                    if (deleteIsUnread) -1 else null
                )
            })
    }

    fun recallMessage(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        msgId: Long,
        channelName: String? = ""
    ) {
        val recallMessage: IMPB.RecallMessage? = if (msgId > 0) {
            IMPB.RecallMessage.newBuilder().setMsgId(msgId).setClear(0).build()
        } else if (!TextUtils.isEmpty(channelName)) {
            IMPB.RecallMessage.newBuilder().setChannelName(channelName).setClear(0).build()
        } else {
            null
        }

        recallMessage?.let {
            when (chatType) {
                ChatModel.CHAT_TYPE_PVT -> {
                    RecallMessageManager.recallToUserMessage(
                        myUid,
                        targetId,
                        arrayListOf(recallMessage),
                        false,
                        { lastMsg, recallUnreadCount ->
                            ChatsHistoryManager.updateChatLastMsg(
                                myUid,
                                chatType,
                                targetId,
                                lastMsg,
                                if (recallUnreadCount > 0) -recallUnreadCount else null, {
                                    EventBus.publishEvent(UnreadMessageEvent())
                                })

                            SendMessageManager.sendRecallPvtMessagePackage(
                                msgId,
                                channelName,
                                targetId
                            )
                        })
                }
                ChatModel.CHAT_TYPE_GROUP -> {
                    RecallMessageManager.recallToGroupMessage(
                        myUid,
                        targetId,
                        arrayListOf(recallMessage),
                        false,
                        { lastMsg, recallUnreadCount ->
                            ChatsHistoryManager.updateChatLastMsg(
                                myUid,
                                chatType,
                                targetId,
                                lastMsg,
                                if (recallUnreadCount > 0) -recallUnreadCount else null, {
                                    EventBus.publishEvent(UnreadMessageEvent())
                                })

                            SendMessageManager.sendRecallGroupMessagePackage(msgId, targetId)
                        })
                }
            }
        }
    }

    fun recallMessages(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        clearTime: Long,
        deleteChat: Boolean
    ) {
        val recallMessage =
            IMPB.RecallMessage.newBuilder()
                .setClearTime(clearTime)
                .setClear(if (deleteChat) 2 else 1)
                .build()
        when (chatType) {
            ChatModel.CHAT_TYPE_PVT -> {
                RecallMessageManager.recallToUserMessage(
                    myUid,
                    targetId,
                    arrayListOf(recallMessage),
                    false,
                    { lastMsg, recallUnreadCount ->
                        ChatsHistoryManager.updateChatLastMsg(
                            myUid,
                            chatType,
                            targetId,
                            lastMsg,
                            if (recallUnreadCount > 0) -recallUnreadCount else null, {
                                EventBus.publishEvent(UnreadMessageEvent())
                            }
                        )

                        SendMessageManager.sendRecallPvtMessagesPackage(
                            if (deleteChat) 2 else 1,
                            ArouterServiceManager.messageService.getCurrentTime(),
                            targetId
                        )
                    })
            }
            ChatModel.CHAT_TYPE_GROUP -> {
                RecallMessageManager.recallToGroupMessage(
                    myUid,
                    targetId,
                    arrayListOf(recallMessage),
                    false,
                    { lastMsg, recallUnreadCount ->
                        ChatsHistoryManager.updateChatLastMsg(
                            myUid,
                            chatType,
                            targetId,
                            lastMsg,
                            if (recallUnreadCount > 0) -recallUnreadCount else null, {
                                EventBus.publishEvent(UnreadMessageEvent())
                            }
                        )

                        SendMessageManager.sendRecallGroupMessagesPackage(
                            if (deleteChat) 2 else 1,
                            ArouterServiceManager.messageService.getCurrentTime(),
                            targetId
                        )
                    })
            }
        }
    }

    fun receiveRecallMessage(
        chatType: Int,
        myUid: Long,
        sendId: Long,
        msgs: List<IMPB.RecallMessage>
    ) {
        when (chatType) {
            ChatModel.CHAT_TYPE_PVT -> {
                RecallMessageManager.recallToUserMessage(myUid, sendId, msgs,
                    isReceive = true,
                    complete = { lastMsg, recallUnreadCount ->
                        msgs.forEach { msg ->
                            SendMessageManager.sendRecvRecallPvtMessagePackage(
                                msg.msgId,
                                msg.channelName,
                                msg.clear,
                                msg.clearTime,
                                myUid
                            )
                            EventBus.publishEvent(RecallMessageEvent(chatType, sendId, msg.msgId))
                        }

                        ChatsHistoryManager.updateChatLastMsg(
                            myUid,
                            ChatModel.CHAT_TYPE_PVT,
                            sendId,
                            lastMsg,
                            if (recallUnreadCount > 0) -recallUnreadCount else null, {
                                EventBus.publishEvent(UnreadMessageEvent())
                            }
                        )
                    })
            }
            ChatModel.CHAT_TYPE_GROUP -> {
                RecallMessageManager.recallToGroupMessage(myUid, sendId, msgs,
                    isReceive = true,
                    complete = { lastMsg, recallUnreadCount ->
                        msgs.forEach { msg ->
                            SendMessageManager.sendRecvRecallGroupMessagePackage(msg.msgId, myUid)
                            EventBus.publishEvent(RecallMessageEvent(chatType, sendId, msg.msgId))
                        }

                        ChatsHistoryManager.updateChatLastMsg(
                            myUid,
                            ChatModel.CHAT_TYPE_GROUP,
                            sendId,
                            lastMsg,
                            if (recallUnreadCount > 0) -recallUnreadCount else null, {
                                EventBus.publishEvent(UnreadMessageEvent())
                            }
                        )
                    })
            }
        }
    }

    fun clearMessageHistory(chaterType: Int, targetId: Long) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        ChatsHistoryManager.clearChat(myUid, chaterType, targetId, {
            if (chaterType == ChatModel.CHAT_TYPE_PVT) {
                MessagesManager.deleteToUserAllMessage(myUid, targetId)
            } else if (chaterType == ChatModel.CHAT_TYPE_GROUP) {
                MessagesManager.deleteToGroupAllMessage(myUid, targetId)
            }

            EventBus.publishEvent(UnreadMessageEvent())
            EventBus.publishEvent(ChatHistoryChangeEvent())
        })
    }

    fun deleteChat(myUid: Long, chaterType: Int, targetId: Long) {
        ChatsHistoryManager.deleteChat(myUid, chaterType, targetId, {
            if (chaterType == ChatModel.CHAT_TYPE_PVT) {
                MessagesManager.deleteToUserAllMessage(myUid, targetId)
            } else if (chaterType == ChatModel.CHAT_TYPE_GROUP) {
                MessagesManager.deleteToGroupAllMessage(myUid, targetId, {
                }) {
                    Log.i("lzh", "it " + it.message)
                }
            }

            EventBus.publishEvent(UnreadMessageEvent())
        })
    }

    fun deleteAllChat(myUid: Long) {
        val chatModels = mutableListOf<ChatModel>()
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, { realm ->
            realm.where(ChatModel::class.java).findAll()?.let {
                it.forEach { chatModel ->
                    chatModels.add(chatModel.copyChat())
                }
            }
        }, {
            chatModels.forEach { chat ->
                deleteChat(myUid, chat.chaterType, chat.chaterId)
            }

            EventBus.publishEvent(UnreadMessageEvent())
        })
    }

    fun deleteAccountData(myUid: Long) {
        //删除通话信息
        RealmCreator.executeStreamCallsHistoryTransactionAsync(myUid, { realm ->
            realm.where(StreamCallModel::class.java).findAll()?.let {
                it.deleteAllFromRealm()
            }
        })

        //删除会话 和 聊天信息
        deleteAllChat(myUid)
    }

    fun getAllChat(complete: ((List<ChatModel>) -> Unit)?, error: ((Throwable) -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val chatModels = mutableListOf<ChatModel>()
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, { realm ->
            realm.where(ChatModel::class.java).findAll()?.let {
                it.forEach { chatModel ->
                    chatModels.add(chatModel.copyChat())
                }
            }
        }, {
            complete?.invoke(chatModels)
        }, error)
    }

    fun sendMsgPlayedReceipt(
        chatType: Int,
        targetId: Long,
        messageLocalId: Long,
        exporeTimeCallback: ((Long) -> Unit)? = null
    ) {
        if (chatType == ChatModel.CHAT_TYPE_GROUP) {
            ArouterServiceManager.groupService.getGroupInfo(null, targetId, { groupInfoModel, _ ->
                if (groupInfoModel.memberCount <= Constant.Common.SHOW_RECEIPT_MAX_GROUP_MEMBER_COUNT) {
                    sendMsgPlayedReceiptImpl(chatType, targetId, messageLocalId, exporeTimeCallback)
                }
            })
        } else {
            sendMsgPlayedReceiptImpl(chatType, targetId, messageLocalId, exporeTimeCallback)
        }
    }

    private fun sendMsgPlayedReceiptImpl(
        chatType: Int,
        targetId: Long,
        messageLocalId: Long,
        exporeTimeCallback: ((Long) -> Unit)? = null
    ) {
        var msgReceipt: IMPB.ReceiptMessage? = null
        var expireTime = 0L
        val time = ArouterServiceManager.messageService.getCurrentTime()
        val receiptStatus = CommonProto.MsgReceiptStatusBase.newBuilder()
            .setStatus(CommonProto.MsgReceiptStatus.PLAYED).setTime(time)
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        MessagesManager.executeChatTransactionAsync(chatType, myUid, targetId, { realm ->
            val msg = realm.where(MessageModel::class.java)
                ?.equalTo("id", messageLocalId)
                ?.findFirst()
            msg?.let {
                if (it.isSend == 0 && it.isReadedAttachment != -1 && it.isReadedAttachment != 1) {
                    if (msg.snapchatTime > 0) {
                        if (msg.type == MessageModel.MESSAGE_TYPE_VOICE) {
                            if (msg.voiceMessageContent.recordTime > msg.snapchatTime) {
                                msg.expireTime =
                                    System.currentTimeMillis() + (msg.voiceMessageContent.recordTime + 2) * 1000
                            } else {
                                msg.expireTime =
                                    System.currentTimeMillis() + msg.snapchatTime * 1000
                            }
                        } else if (msg.type == MessageModel.MESSAGE_TYPE_VIDEO) {
                            if (msg.videoMessageContent.videoTime > msg.snapchatTime) {
                                msg.expireTime =
                                    System.currentTimeMillis() + (msg.videoMessageContent.videoTime + 2) * 1000
                            } else {
                                msg.expireTime =
                                    System.currentTimeMillis() + msg.snapchatTime * 1000
                            }
                        } else if (msg.type == MessageModel.MESSAGE_TYPE_IMAGE) {
                            msg.expireTime = System.currentTimeMillis() + msg.snapchatTime * 1000
                        } else if (msg.type == MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE) {
                            msg.expireTime = System.currentTimeMillis() + msg.snapchatTime * 1000
                        } else if (msg.type == MessageModel.MESSAGE_TYPE_UNDECRYPT) {
                            msg.expireTime = System.currentTimeMillis() + msg.snapchatTime * 1000
                        }
                    }

                    msg.isReadedAttachment = -1
                    msg.readedAttachmentTime = ArouterServiceManager.messageService.getCurrentTime()
                    realm.copyToRealmOrUpdate(msg)

                    msgReceipt = if (chatType == ChatModel.CHAT_TYPE_GROUP) {
                        IMPB.ReceiptMessage.newBuilder().setMsgId(msg.msgId)
                            .setType(IMPB.ChatMessageType.GROUP)
                            .setMessageTypeValue(msg.originalMessageType)
                            .setSnapchatTime(msg.snapchatTime)
                            .setDuration(msg.durationTime)
                            .setGroupId(targetId).setSendUid(msg.ownerUid)
                            .setReceiptStatus(receiptStatus).build()
                    } else {
                        IMPB.ReceiptMessage.newBuilder().setMsgId(msg.msgId)
                            .setType(IMPB.ChatMessageType.ONE_TO_ONE)
                            .setMessageTypeValue(msg.originalMessageType)
                            .setSnapchatTime(msg.snapchatTime)
                            .setDuration(msg.durationTime)
                            .setSendUid(targetId).setReceiptStatus(receiptStatus).build()
                    }
                }

                expireTime = msg.expireTime
            }
        }, {
            msgReceipt?.let {
                SendMessageManager.sendReceiptMessagePackage(listOf(it))
            }

            exporeTimeCallback?.invoke(expireTime)
        })
    }

    private fun saveMessageToSearchDB(msgModels: List<MessageModel>) {
        val searchList = mutableListOf<SearchChatModel>()
        msgModels.forEach {
            if (it.snapchatTime == 0) {
                val content = when (it.type) {
                    MessageModel.MESSAGE_TYPE_NOTICE -> {
                        it.noticeMessageBean.content
                    }
                    MessageModel.MESSAGE_TYPE_TEXT -> {
                        it.content
                    }
                    MessageModel.MESSAGE_TYPE_FILE -> {
                        it.fileMessageContentBean.name
                    }
                    MessageModel.MESSAGE_TYPE_LOCATION -> {
                        it.locationMessageContentBean.address
                    }
                    else -> {
                        ""
                    }
                }
                if (!TextUtils.isEmpty(content)) {
                    val id = if (it.isSend == 1) {
                        it.targetId
                    } else {
                        it.senderId
                    }
                    searchList.add(
                        SearchChatModel(
                            id,
                            it.chatType,
                            content,
                            it.msgId,
                            it.id,
                            it.time,
                            it.type,
                            if (it.chatType == ChatModel.CHAT_TYPE_PVT) id else -id,
                            it.ownerUid
                        )
                    )
                }
            }
        }
        if (searchList.size > 0)
            ArouterServiceManager.searchService.insertMessage(searchList)
    }
}