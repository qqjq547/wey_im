package framework.telegram.message.manager

import android.text.TextUtils
import android.util.Log
import com.im.domain.pb.CommonProto
import com.im.pb.IMPB
import framework.ideas.common.bean.*
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.ideas.common.model.im.MessageReceiptModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.bridge.Constant
import framework.telegram.message.controller.MessageController
import framework.telegram.message.db.RealmCreator
import framework.telegram.message.sp.CommonPref
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.gson.GsonInstanceCreater
import framework.telegram.support.system.storage.sp.SharePreferencesStorage.createStorageInstance
import framework.telegram.support.tools.AESHelper
import framework.telegram.support.tools.FileUtils
import framework.telegram.support.tools.HexString
import framework.telegram.support.tools.ThreadUtils
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import java.util.*
import kotlin.collections.ArrayList

/**
 * 消息状态维护
 */
object MessagesManager {

    var isChecking = false

    fun findAtMeMessages(
        myUid: Long,
        targetGid: Long,
        complete: ((List<Long>) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        val atMeMessageLocalIds = ArrayList<Long>()
        RealmCreator.executeGroupChatTransactionAsync(myUid, targetGid, { realm ->
            val atMeMessages = realm.where(MessageModel::class.java).equalTo("isAtMe", 1.toInt())
                .sort("id", Sort.DESCENDING).findAll()
            atMeMessages?.forEach {
                it.isAtMe = 0
                atMeMessageLocalIds.add(it.id)
            }
            realm.copyToRealmOrUpdate(atMeMessages)
        }, {
            complete?.invoke(atMeMessageLocalIds)
        }, error)
    }

    fun updateMessagesAtMeStatus(
        myUid: Long,
        targetGid: Long,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        RealmCreator.executeGroupChatTransactionAsync(myUid, targetGid, { realm ->
            val atMeMessages =
                realm.where(MessageModel::class.java).equalTo("isAtMe", 1.toInt()).findAll()
            atMeMessages?.forEach {
                it.isAtMe = 0
            }
            realm.copyToRealmOrUpdate(atMeMessages)
        }, complete, error)
    }

    /**
     * 设置指定用户所有消息为已读
     */
    fun setAllUserMessageReaded(
        myUid: Long,
        targetUid: Long,
        complete: ((Int, Long) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        Log.d("SendMessageManager", "触发全部已阅")
        var count = 0
        var firstMsgLocalId = 0L
        RealmCreator.executePvtChatTransactionAsync(myUid, targetUid, { realm ->
            setAllMessageReaded(realm, ChatModel.CHAT_TYPE_PVT, targetUid) { c, id ->
                count = c
                firstMsgLocalId = id
            }
        }, {
            complete?.invoke(count, firstMsgLocalId)
        }, error)
    }

    /**
     * 设置指定群所有消息为已读
     */
    fun setAllGroupMessageReaded(
        myUid: Long,
        targetGid: Long,
        complete: ((Int, Long) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        var count = 0
        var firstMsgLocalId = 0L
        RealmCreator.executeGroupChatTransactionAsync(myUid, targetGid, { realm ->
            setAllMessageReaded(realm, ChatModel.CHAT_TYPE_GROUP, targetGid) { c, id ->
                count = c
                firstMsgLocalId = id
            }
        }, {
            complete?.invoke(count, firstMsgLocalId)
        }, error)
    }

    private fun setAllMessageReaded(
        realm: Realm,
        chatType: Int,
        targetId: Long,
        callback: ((Int, Long) -> Unit)? = null
    ) {
        val models = realm.where(MessageModel::class.java)?.equalTo("isSend", 0.toInt())?.and()
            ?.equalTo("isRead", 0.toInt())?.findAll()
        val count = models?.size ?: 0
        var firstMsgLocalId = 0L
        if (count > 0) {
            firstMsgLocalId = models?.first()?.id ?: 0L
            val time = ArouterServiceManager.messageService.getCurrentTime()
            val receiptStatus = CommonProto.MsgReceiptStatusBase.newBuilder()
                .setStatus(CommonProto.MsgReceiptStatus.VIEWED).setTime(time)
            val receipts = mutableListOf<IMPB.ReceiptMessage>()
            models?.forEach { model ->
                // 发送回执
                val msgReceipt = if (chatType == ChatModel.CHAT_TYPE_GROUP) {
                    IMPB.ReceiptMessage.newBuilder().setMsgId(model.msgId)
                        .setType(IMPB.ChatMessageType.GROUP)
                        .setMessageTypeValue(model.originalMessageType)
                        .setSnapchatTime(model.snapchatTime)
                        .setDuration(model.durationTime)
                        .setGroupId(targetId).setSendUid(model.ownerUid)
                        .setReceiptStatus(receiptStatus).build()
                } else {
                    IMPB.ReceiptMessage.newBuilder().setMsgId(model.msgId)
                        .setType(IMPB.ChatMessageType.ONE_TO_ONE)
                        .setMessageTypeValue(model.originalMessageType)
                        .setSnapchatTime(model.snapchatTime)
                        .setDuration(model.durationTime)
                        .setSendUid(targetId).setReceiptStatus(receiptStatus).build()
                }

                if (model.type != MessageModel.MESSAGE_TYPE_VOICE && model.type != MessageModel.MESSAGE_TYPE_VIDEO && model.type != MessageModel.MESSAGE_TYPE_IMAGE && model.type != MessageModel.MESSAGE_TYPE_UNDECRYPT) {
                    if (model.snapchatTime > 0 && model.expireTime <= 0) {
                        model.expireTime = System.currentTimeMillis() + model.snapchatTime * 1000
                    }
                }

                // 设置为需要发送回执
                model.isRead = -1
                model.readTime = time
                realm.copyToRealmOrUpdate(model)

                receipts.add(msgReceipt)
            }

            // 发送回执
            Log.d("SendMessageManager", "启动发送回执")
            ThreadUtils.runOnIOThread {
                Log.d("SendMessageManager", "调用发送回执")
                receipts?.let {
                    if (chatType == ChatModel.CHAT_TYPE_GROUP) {
                        ArouterServiceManager.groupService.getGroupInfo(
                            null,
                            targetId,
                            { groupInfoModel, _ ->
                                if (groupInfoModel.memberCount <= Constant.Common.SHOW_RECEIPT_MAX_GROUP_MEMBER_COUNT) {
                                    SendMessageManager.sendReceiptMessagePackage(it)
                                }
                            })
                    } else {
                        SendMessageManager.sendReceiptMessagePackage(it)
                    }
                }
            }
        }

        callback?.invoke(count, firstMsgLocalId)
    }

    /**
     * 删除指定id的私聊消息（能删除所有人发送的消息）
     */
    fun deleteToUserMessage(
        myUid: Long,
        targetUid: Long,
        id: Long,
        complete: ((preMessage: MessageModel?, deleteIsUnread: Boolean) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        var preMessage: MessageModel? = null
        var deleteIsUnread = false
        RealmCreator.executePvtChatTransactionAsync(myUid, targetUid, { realm ->
            realm.where(MessageModel::class.java).equalTo("id", id).findFirst()
                ?.let { deleteMessage ->
                    deleteIsUnread = deleteMessage.isRead == 1
                    preMessage = deleteMessage(realm, deleteMessage)
                }
        }, {
            complete?.invoke(preMessage, deleteIsUnread)
        }, error)
    }

    /**
     * 删除指定flag的私聊消息（只能删除己方发送的消息）
     */
    fun deleteToUserMessage(
        myUid: Long,
        targetUid: Long,
        flag: String,
        complete: ((preMessage: MessageModel?, deleteIsUnread: Boolean) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        var preMessage: MessageModel? = null
        var deleteIsUnread = false
        RealmCreator.executePvtChatTransactionAsync(myUid, targetUid, { realm ->
            realm.where(MessageModel::class.java).equalTo("flag", flag).findFirst()
                ?.let { deleteMessage ->
                    deleteIsUnread = deleteMessage.isRead == 1
                    preMessage = deleteMessage(realm, deleteMessage)
                }
        }, {
            complete?.invoke(preMessage, deleteIsUnread)
        }, error)
    }

    /**
     * 删除指定id的群聊消息
     */
    fun deleteToGroupMessage(
        myUid: Long,
        targetGid: Long,
        id: Long,
        complete: ((preMessage: MessageModel?, deleteIsUnread: Boolean) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        var preMessage: MessageModel? = null
        var deleteIsUnread = false
        RealmCreator.executeGroupChatTransactionAsync(myUid, targetGid, { realm ->
            realm.where(MessageModel::class.java).equalTo("id", id).findFirst()
                ?.let { deleteMessage ->
                    deleteIsUnread = deleteMessage.isRead == 1
                    preMessage = deleteMessage(realm, deleteMessage)
                }
        }, {
            complete?.invoke(preMessage, deleteIsUnread)
        }, error)
    }

    fun deleteToUserFireMessage(
        myUid: Long,
        targetUid: Long,
        complete: ((lastBizMessage: MessageModel?) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        var lastModel: MessageModel? = null
        RealmCreator.executePvtChatTransactionAsync(myUid, targetUid, { realm ->
            realm.where(MessageModel::class.java)
                ?.notEqualTo("expireTime", 0L)
                ?.and()
                ?.lessThan("expireTime", ArouterServiceManager.messageService.getCurrentTime())
                ?.sort("time", Sort.DESCENDING)
                ?.findAll()?.let { deleteMessages ->
                    if (deleteMessages.isNotEmpty()) {
                        val commonPref = createStorageInstance(
                            CommonPref::class.java,
                            "user_${AccountManager.getLoginAccountUUid()}_${targetUid}"
                        )

                        val lastAutoDeleteFireMsgTime = commonPref.getLastAutoDeleteFireMsgTime()
                        val currentAutoDeleteFireMsgTime = deleteMessages.first()?.time ?: 0
                        if (lastAutoDeleteFireMsgTime < currentAutoDeleteFireMsgTime) {
                            commonPref.putLastAutoDeleteFireMsgTime(currentAutoDeleteFireMsgTime)
                        }

                        lastModel = findLastBizMessage(realm)
                        deleteMessages.deleteAllFromRealm()
                    }
                }
        }, {
            complete?.invoke(lastModel)
        }, error)
    }

    fun deleteToGroupFireMessage(
        myUid: Long,
        targetGid: Long,
        complete: ((lastBizMessage: MessageModel?) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        var lastModel: MessageModel? = null
        RealmCreator.executeGroupChatTransactionAsync(myUid, targetGid, { realm ->
            realm.where(MessageModel::class.java)
                ?.notEqualTo("expireTime", 0L)
                ?.and()
                ?.lessThan("expireTime", ArouterServiceManager.messageService.getCurrentTime())
                ?.sort("time", Sort.DESCENDING)
                ?.findAll()?.let { deleteMessages ->
                    if (deleteMessages.isNotEmpty()) {
                        val commonPref = createStorageInstance(
                            CommonPref::class.java,
                            "group_${AccountManager.getLoginAccountUUid()}_${targetGid}"
                        )

                        val lastAutoDeleteFireMsgTime = commonPref.getLastAutoDeleteFireMsgTime()
                        val currentAutoDeleteFireMsgTime = deleteMessages.first()?.time ?: 0
                        if (lastAutoDeleteFireMsgTime < currentAutoDeleteFireMsgTime) {
                            commonPref.putLastAutoDeleteFireMsgTime(currentAutoDeleteFireMsgTime)
                        }

                        lastModel = findLastBizMessage(realm)
                        deleteMessageFilesCache(deleteMessages)
                        deleteMessageFromDB2(deleteMessages)
                        deleteMessages.deleteAllFromRealm()
                    }
                }
        }, {
            complete?.invoke(lastModel)
        }, error)
    }

    fun findChatLastMsg(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        complete: (lastBizMessage: MessageModel?) -> Unit,
        error: ((Throwable) -> Unit)? = null
    ) {
        var lastModel: MessageModel? = null
        if (chatType == ChatModel.CHAT_TYPE_PVT) {
            RealmCreator.executePvtChatTransactionAsync(myUid, targetId, {
                lastModel = findLastBizMessage(it)
            }, {
                complete.invoke(lastModel)
            }, error)
        } else {
            RealmCreator.executeGroupChatTransactionAsync(myUid, targetId, {
                lastModel = findLastBizMessage(it)
            }, {
                complete.invoke(lastModel)
            }, error)
        }
    }

    private fun deleteMessage(realm: Realm, deleteModel: MessageModel?): MessageModel? {
        var lastModel: MessageModel? = null
        deleteModel?.let {
            // 最后一条消息
            lastModel = findLastBizMessage(realm)
            lastModel?.let {
                if (deleteModel.id == it.id) {
                    // 删除的是最后一条业务消息
                    // 获取上一条业务消息
                    lastModel = findPrevBizMessage(realm, deleteModel)?.copyMessage()
                }
            }

            deleteMessageFileCache(deleteModel)
            deleteMessageFromDB(mutableListOf(deleteModel.copyMessage()))
            deleteModel.deleteFromRealm()
        }

        return lastModel
    }

    fun findPvtLastBizMessage(
        myUid: Long,
        targetUid: Long,
        findCallback: ((MessageModel?) -> Unit)? = null
    ) {
        MessageController.executeChatTransactionAsyncWithResult(
            ChatModel.CHAT_TYPE_PVT,
            myUid,
            targetUid,
            { realm ->
                realm.where(MessageModel::class.java)
                    ?.lessThan("type", 200)
                    ?.and()
                    ?.beginGroup()
                    ?.equalTo("expireTime", 0L)
                    ?.or()
                    ?.greaterThan("expireTime", System.currentTimeMillis())
                    ?.endGroup()
                    ?.and()
                    ?.equalTo("targetId", myUid)
                    ?.sort("time", Sort.DESCENDING)
                    ?.findFirst()?.copyMessage()
            },
            {
                findCallback?.invoke(it)
            })
    }

    /**
     * 查找当前记录中的最后一条业务消息（type小于200的）
     */
    fun findLastBizMessage(realm: Realm): MessageModel? {
        return realm.where(MessageModel::class.java)
            ?.lessThan("type", 200)
            ?.and()
            ?.beginGroup()
            ?.equalTo("expireTime", 0L)
            ?.or()
            ?.greaterThan("expireTime", System.currentTimeMillis())
            ?.endGroup()
            ?.sort("time", Sort.DESCENDING)
            ?.findFirst()?.copyMessage()
    }

    /**
     * 查找当前给定的消息的上一条业务消息（type小于200的）
     */
    fun findPrevBizMessage(realm: Realm, msgModel: MessageModel): MessageModel? {
        return realm.where(MessageModel::class.java)
            ?.lessThan("type", 200)
            ?.and()
            ?.lessThan("time", msgModel.time)
            ?.and()
            ?.beginGroup()
            ?.equalTo("expireTime", 0L)
            ?.or()
            ?.greaterThan("expireTime", System.currentTimeMillis())
            ?.endGroup()
            ?.sort("time", Sort.DESCENDING)?.findFirst()?.copyMessage()
    }

    /**
     * 删除指定用户的所有消息
     */
    fun deleteToUserAllMessage(
        myUid: Long,
        targetUid: Long,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        RealmCreator.executePvtChatTransactionAsync(myUid, targetUid, { realm ->
            deleteAllMessage(realm)
        }, complete, error)
    }

    /**
     * 删除指定群的所有消息
     */
    fun deleteToGroupAllMessage(
        myUid: Long,
        targetGid: Long,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        RealmCreator.executeGroupChatTransactionAsync(myUid, targetGid, { realm ->
            deleteAllMessage(realm)
            deleteAllMessageReceipt(realm)
        }, complete, error)
    }

    fun deleteToGroupAllMessageReceipt(
        myUid: Long,
        targetGid: Long,
        targerUid: Long,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        RealmCreator.executeGroupChatTransactionAsync(myUid, targetGid, { realm ->
            val models =
                realm.where(MessageReceiptModel::class.java).equalTo("senderUid", targerUid)
                    .findAll()
            models?.deleteAllFromRealm()
        }, complete, error)
    }

    private fun deleteAllMessage(realm: Realm) {
        val models = realm.where(MessageModel::class.java).findAll()
        deleteMessageFilesCache(models)
        deleteMessageFromDB2(models)
        realm.delete(MessageModel::class.java)
    }

    private fun deleteAllMessageReceipt(realm: Realm) {
        realm.delete(MessageReceiptModel::class.java)
    }

    /**
     *  查询所有消息状态，并进行状态重置
     *  查询所有未知的消息进行解密
     */
    fun resetMessagesByReboot(
        parseUnKnowMessage: Boolean = true,
        clearRedundantMessageReceipts: Boolean = true
    ) {
        val currentUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val allChats = arrayListOf<ChatModel>()
        RealmCreator.executeChatsHistoryTransactionAsync(currentUid, { realm ->
            val chats = realm.where(ChatModel::class.java).findAll()
            chats?.forEach {
                allChats.add(it.copyChat())
            }
        }, {
            allChats.forEach { chat ->
                val hasUnDecryptChats = arrayListOf<ChatModel>()
                val deleteModels = arrayListOf<MessageModel>()
                var isChangeChat = false
                executeChatTransactionAsync(chat.chaterType, currentUid, chat.chaterId, { realm ->
                    // 重置所有未发送成功消息的发送状态为失败
                    resetMessageSendStatusToFail(realm)

                    // 找出需要清除的阅后即焚消息
                    val autoDeleteFireMessages = checkMessageFireStatus(chat.chaterType, realm)
                    if (autoDeleteFireMessages.isNotEmpty()) {
                        isChangeChat = true
                        deleteModels.addAll(autoDeleteFireMessages)
                    }

                    // 找出有未解密消息的会话
                    if (checkHasMessageUnDecrypt(chat.chaterType, realm)) {
                        hasUnDecryptChats.add(chat)
                    }

                    // 解析未解析的消息
                    if (parseUnKnowMessage && parseUnKnowMessage(realm)) {
                        isChangeChat = true
                    }

                    // 清除溢出的消息回执
                    if (clearRedundantMessageReceipts && chat.chaterType == ChatModel.CHAT_TYPE_GROUP) {
                        clearRedundantMessageReceipts(currentUid, chat.chaterId)
                    }
                }, {
                    // 解密消息
                    checkMessageUnDecrypt(currentUid, hasUnDecryptChats)

                    // 删除被删除的阅后既焚消息的缓存文件
                    deleteMessageFilesCacheImpl(deleteModels)
                    deleteMessageFromDB(deleteModels)

                    if (isChangeChat) {
                        MessageController.executeChatTransactionAsyncWithResult(
                            chat.chaterType,
                            currentUid,
                            chat.chaterId,
                            { realm ->
                                findLastBizMessage(realm)
                            },
                            {
                                ChatsHistoryManager.updateChatLastMsg(
                                    currentUid,
                                    chat.chaterType,
                                    chat.chaterId,
                                    it,
                                    changeTime = false
                                )
                            })
                    }
                })
            }
        })
    }

    fun resetMessagesByUserLogin() {
        val currentUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val allChats = arrayListOf<ChatModel>()
        RealmCreator.executeChatsHistoryTransactionAsync(currentUid, { realm ->
            val chats = realm.where(ChatModel::class.java).findAll()
            chats?.forEach {
                allChats.add(it.copyChat())
            }
        }, {
            allChats.forEach { chat ->
                var recallMsgs: List<MessageModel>? = null
                executeChatTransactionAsync(chat.chaterType, currentUid, chat.chaterId, { realm ->
                    // 检测消息撤回状态
                    recallMsgs = checkSpecifiedTimeMessageRecallStatus(realm)
                }, {
                    ThreadUtils.runOnIOThread {
                        recallMsgs?.forEach {
                            if (chat.chaterType == ChatModel.CHAT_TYPE_PVT) {
                                SendMessageManager.sendRecallPvtMessagePackage(
                                    it.msgId,
                                    it.flag,
                                    chat.chaterId
                                )
                            } else if (chat.chaterType == ChatModel.CHAT_TYPE_GROUP) {
                                SendMessageManager.sendRecallGroupMessagePackage(
                                    it.msgId,
                                    chat.chaterId
                                )
                            }
                        }
                    }
                })
            }
        })

        val allDeleteChats = arrayListOf<ChatModel>()
        RealmCreator.executeDeleteChatsHistoryTransactionAsync(currentUid, { realm ->
            val chats = realm.where(ChatModel::class.java).findAll()
            chats?.forEach {
                allDeleteChats.add(it.copyChat())
            }
        }, {
            allDeleteChats.forEach { chat ->
                Log.d("demo", "已将删除 ${chat.chaterName}-${chat.chaterId} 的会话记录命令发送给服务器")

                if (chat.chaterType == ChatModel.CHAT_TYPE_PVT) {
                    SendMessageManager.sendRecallPvtMessagesPackage(
                        2,
                        chat.lastMsgTime,
                        chat.chaterId
                    )
                } else if (chat.chaterType == ChatModel.CHAT_TYPE_GROUP) {
                    SendMessageManager.sendRecallGroupMessagesPackage(
                        2,
                        chat.lastMsgTime,
                        chat.chaterId
                    )
                }
            }
        })
    }

    private fun clearRedundantMessageReceipts(myUid: Long, targetId: Long) {
        executeChatTransactionAsync(ChatModel.CHAT_TYPE_GROUP, myUid, targetId, { realm ->
            val availMsgs = realm.where(MessageModel::class.java).equalTo("isSend", 1.toInt())
                .sort("msgId", Sort.DESCENDING)
                .limit(Constant.Common.SHOW_RECEIPT_MAX_GROUP_MEMBER_COUNT.toLong()).findAll()
            if (availMsgs.isNotEmpty() && availMsgs.size == Constant.Common.SHOW_RECEIPT_MAX_GROUP_MEMBER_COUNT) {
                availMsgs.last()?.let {
                    val distinctMsgReceipts =
                        realm.where(MessageReceiptModel::class.java).lessThan("msgId", it.msgId)
                            .findAll()
                    distinctMsgReceipts?.deleteAllFromRealm()
                }
            }
        })
    }

    private fun checkMessageFireStatus(chatType: Int, realm: Realm): List<MessageModel> {
        val copyMsgs = arrayListOf<MessageModel>()

        // 正常阅后既焚的消息
        var fireMsgModels = realm.where(MessageModel::class.java)
            ?.notEqualTo("expireTime", 0L)
            ?.and()
            ?.lessThan("expireTime", System.currentTimeMillis())
            ?.findAll()

        fireMsgModels?.forEach {
            if (it != null) {
                copyMsgs.add(it.copyMessage())
            }
        }
        fireMsgModels?.deleteAllFromRealm()

        // 非正常阅后既焚的消息
        if (chatType == ChatModel.CHAT_TYPE_PVT) {
            // 收到的阅后既焚消息，阅读超过7天的都删除
            fireMsgModels = realm.where(MessageModel::class.java)
                ?.greaterThan("snapchatTime", 0L)
                ?.and()
                ?.equalTo("expireTime", 0L)
                ?.and()
                ?.equalTo("isSend", 0.toInt())
                ?.and()
                ?.greaterThan("readTime", 0.toInt())
                ?.and()
                ?.lessThan(
                    "readTime",
                    ArouterServiceManager.messageService.getCurrentTime() - Constant.Common.AUTO_CLEAR_MESSAGE_MAX_TIME
                )
                ?.findAll()

            fireMsgModels?.forEach {
                if (it != null) {
                    copyMsgs.add(it.copyMessage())
                }
            }
            fireMsgModels?.deleteAllFromRealm()

            // 发送的阅后既焚消息，发送超过7天的都删除
            fireMsgModels = realm.where(MessageModel::class.java)
                ?.greaterThan("snapchatTime", 0L)
                ?.and()
                ?.equalTo("expireTime", 0L)
                ?.and()
                ?.equalTo("isSend", 1.toInt())
                ?.and()
                ?.greaterThan("time", 0.toInt())
                ?.and()
                ?.lessThan(
                    "time",
                    ArouterServiceManager.messageService.getCurrentTime() - Constant.Common.AUTO_CLEAR_MESSAGE_MAX_TIME
                )
                ?.findAll()

            fireMsgModels?.forEach {
                if (it != null) {
                    copyMsgs.add(it.copyMessage())
                }
            }
            fireMsgModels?.deleteAllFromRealm()
        } else if (chatType == ChatModel.CHAT_TYPE_GROUP) {
            // 群的阅后既焚消息，发送超过7天的都删除
            fireMsgModels = realm.where(MessageModel::class.java)
                ?.greaterThan("snapchatTime", 0L)
                ?.and()
                ?.equalTo("expireTime", 0L)
                ?.and()
                ?.greaterThan("time", 0.toInt())
                ?.and()
                ?.lessThan(
                    "time",
                    ArouterServiceManager.messageService.getCurrentTime() - Constant.Common.AUTO_CLEAR_MESSAGE_MAX_TIME
                )
                ?.findAll()

            fireMsgModels?.forEach {
                if (it != null) {
                    copyMsgs.add(it.copyMessage())
                }
            }
            fireMsgModels?.deleteAllFromRealm()
        }

        return copyMsgs
    }

    private fun checkHasMessageUnDecrypt(chatType: Int, realm: Realm): Boolean {
        if (chatType == ChatModel.CHAT_TYPE_GROUP) {
            return realm.where(MessageModel::class.java)
                .equalTo("type", MessageModel.MESSAGE_TYPE_UNDECRYPT).and()
                .equalTo("isSend", 0.toInt()).count() > 0
        }

        return false
    }

    private fun checkMessageUnDecrypt(myUid: Long, hasUnDecryptChats: List<ChatModel>) {
        hasUnDecryptChats.forEach { hasUnDecryptChat ->
            if (hasUnDecryptChat.chaterType == ChatModel.CHAT_TYPE_GROUP) {
                ArouterServiceManager.systemService.getGroupSecretKey(
                    hasUnDecryptChat.chaterId,
                    { secretKey, keyVersion ->
                        var isChangeData = false
                        executeChatTransactionAsync(
                            ChatModel.CHAT_TYPE_GROUP,
                            myUid,
                            hasUnDecryptChat.chaterId,
                            { realm ->
                                val msgs = realm.where(MessageModel::class.java)
                                    .equalTo("type", MessageModel.MESSAGE_TYPE_UNDECRYPT).and()
                                    .equalTo("isSend", 0.toInt())
                                    .findAll()
                                msgs?.forEach { model ->
                                    if (model.keyVersion == keyVersion) {
                                        var data: ByteArray? = null
                                        var attachmentKey: String? = ""
                                        try {
                                            data = AESHelper.decryptToBytes(
                                                model.contentBytes,
                                                secretKey
                                            )
                                            attachmentKey =
                                                if (!TextUtils.isEmpty(model.attachmentKey)) AESHelper.decrypt(
                                                    HexString.hexToBuffer(model.attachmentKey),
                                                    secretKey
                                                ) else ""
                                        } catch (e: Exception) {
                                            // 解密失败
                                        }

                                        if (data != null) {
                                            when {
                                                model.originalType == IMPB.MessageType.text.number -> {
                                                    model.content =
                                                        IMPB.TextObj.parseFrom(data).content
                                                    model.type = MessageModel.MESSAGE_TYPE_TEXT
                                                    isChangeData = true
                                                }
                                                model.originalType == IMPB.MessageType.image.number -> {
                                                    val content = IMPB.ImageObj.parseFrom(data)
                                                    val imageMessageContentBean =
                                                        ImageMessageContentBean()
                                                    imageMessageContentBean.imageFileUri =
                                                        content.url
                                                    imageMessageContentBean.imageThumbFileUri =
                                                        content.thumbUrl
                                                    imageMessageContentBean.width = content.width
                                                    imageMessageContentBean.height = content.height
                                                    model.content =
                                                        GsonInstanceCreater.defaultGson.toJson(
                                                            imageMessageContentBean
                                                        )
                                                    model.type = MessageModel.MESSAGE_TYPE_IMAGE
                                                    model.attachmentKey = attachmentKey
                                                    isChangeData = true
                                                }
                                                model.originalType == IMPB.MessageType.dynamicImage.number -> {
                                                    val content =
                                                        IMPB.DynamicImageObj.parseFrom(data)
                                                    val imageMessageContentBean =
                                                        ImageMessageContentBean()
                                                    imageMessageContentBean.imageFileUri =
                                                        content.url
                                                    imageMessageContentBean.imageThumbFileUri =
                                                        content.thumbUrl
                                                    imageMessageContentBean.width = content.width
                                                    imageMessageContentBean.height = content.height
                                                    model.content =
                                                        GsonInstanceCreater.defaultGson.toJson(
                                                            imageMessageContentBean
                                                        )
                                                    model.type =
                                                        MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE
                                                    model.attachmentKey = attachmentKey
                                                    isChangeData = true
                                                }
                                                model.originalType == IMPB.MessageType.video.number -> {
                                                    val content = IMPB.VideoObj.parseFrom(data)
                                                    val videoMessageContentBean =
                                                        VideoMessageContentBean()
                                                    videoMessageContentBean.videoFileUri =
                                                        content.url
                                                    videoMessageContentBean.videoThumbFileUri =
                                                        content.thumbUrl
                                                    videoMessageContentBean.width = content.width
                                                    videoMessageContentBean.height = content.height
                                                    model.content =
                                                        GsonInstanceCreater.defaultGson.toJson(
                                                            videoMessageContentBean
                                                        )
                                                    model.type = MessageModel.MESSAGE_TYPE_VIDEO
                                                    model.attachmentKey = attachmentKey
                                                    isChangeData = true
                                                }
                                                model.originalType == IMPB.MessageType.audio.number -> {
                                                    val content = IMPB.AudioObj.parseFrom(data)
                                                    val voiceMessageContentBean =
                                                        VoiceMessageContentBean()
                                                    voiceMessageContentBean.recordTime =
                                                        content.duration
                                                    voiceMessageContentBean.recordFileUri =
                                                        content.url
                                                    model.content =
                                                        GsonInstanceCreater.defaultGson.toJson(
                                                            voiceMessageContentBean
                                                        )
                                                    model.type = MessageModel.MESSAGE_TYPE_VOICE
                                                    model.attachmentKey = attachmentKey
                                                    isChangeData = true
                                                }
                                                model.originalType == IMPB.MessageType.nameCard.number -> {
                                                    val content = IMPB.NameCardObj.parseFrom(data)
                                                    val nameCardMessageContentBean =
                                                        NameCardMessageContentBean()
                                                    nameCardMessageContentBean.uid = content.uid
                                                    nameCardMessageContentBean.nickName =
                                                        content.nickName
                                                    nameCardMessageContentBean.icon = content.icon
                                                    model.content =
                                                        GsonInstanceCreater.defaultGson.toJson(
                                                            nameCardMessageContentBean
                                                        )
                                                    model.type = MessageModel.MESSAGE_TYPE_NAMECARD
                                                    isChangeData = true
                                                }
                                                model.originalType == IMPB.MessageType.location.number -> {
                                                    val content = IMPB.LocationObj.parseFrom(data)
                                                    val locationMessageContentBean =
                                                        LocationMessageContentBean()
                                                    locationMessageContentBean.lat = content.lat
                                                    locationMessageContentBean.lng = content.lng
                                                    locationMessageContentBean.address =
                                                        content.address
                                                    model.content =
                                                        GsonInstanceCreater.defaultGson.toJson(
                                                            locationMessageContentBean
                                                        )
                                                    model.type = MessageModel.MESSAGE_TYPE_LOCATION
                                                    isChangeData = true
                                                }
                                                model.originalType == IMPB.MessageType.file.number -> {
                                                    val content = IMPB.FileObj.parseFrom(data)
                                                    val fileMessageContentBean =
                                                        FileMessageContentBean()
                                                    fileMessageContentBean.fileUri = content.fileUrl
                                                    fileMessageContentBean.name = content.name
                                                    fileMessageContentBean.size = content.size
                                                    fileMessageContentBean.mimeType =
                                                        content.mimeType
                                                    model.content =
                                                        GsonInstanceCreater.defaultGson.toJson(
                                                            fileMessageContentBean
                                                        )
                                                    model.type = MessageModel.MESSAGE_TYPE_FILE
                                                    model.attachmentKey = attachmentKey
                                                    isChangeData = true
                                                }
                                            }
                                            realm.copyToRealmOrUpdate(model)
                                        }
                                    }
                                }
                            },
                            {
                                if (isChangeData) {
                                    MessageController.executeChatTransactionAsyncWithResult(
                                        hasUnDecryptChat.chaterType,
                                        myUid,
                                        hasUnDecryptChat.chaterId,
                                        { realm ->
                                            findLastBizMessage(realm)
                                        },
                                        {
                                            ChatsHistoryManager.updateChatLastMsg(
                                                myUid,
                                                hasUnDecryptChat.chaterType,
                                                hasUnDecryptChat.chaterId,
                                                it,
                                                changeTime = false
                                            )
                                        })
                                }
                            })
                    })
            }
        }
    }

    /**
     * 设置所有没有发送成功的消息为发送失败（重启应用时调用一次）
     */
    private fun resetMessageSendStatusToFail(realm: Realm) {
        val failMsgModels = realm.where(MessageModel::class.java)
            .notEqualTo("status", MessageModel.STATUS_SENDED_HAS_RESP).findAll()
        failMsgModels.forEach { model ->
            model.status = MessageModel.STATUS_SEND_FAIL//状态不为发送成功的，全部置为发送失败
            realm.copyToRealmOrUpdate(model)
        }
    }

    /**
     *  查询所有消息状态，并进行状态重置
     */
    fun checkToMessageAllStatus() {
        val currentUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val allChats = arrayListOf<ChatModel>()
        RealmCreator.executeChatsHistoryTransactionAsync(currentUid, { realm ->
            val chats = realm.where(ChatModel::class.java).findAll()
            chats?.forEach {
                allChats.add(it.copyChat())
            }
        }, {
            allChats.forEach {
                checkToMessageAllStatus(it.chaterType, currentUid, it.chaterId)
            }
        })
    }

    /**
     * 查询所有消息状态，并做对应的操作
     */
    private fun checkToMessageAllStatus(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        var receiptMsgs: List<IMPB.ReceiptMessage>? = null
        var lastModel: MessageModel? = null
        executeChatTransactionAsync(chatType, myUid, targetId, { realm ->
            // 检测消息回执状态
            receiptMsgs = checkSpecifiedTimeMessageReceiptStatus(chatType, targetId, realm)

            // 检测消息阅后即焚状态
            checkSpecifiedTimeMessageFireStatus(chatType, targetId, realm)

            // 获取最后一条消息
            lastModel = findLastBizMessage(realm)
        }, {
            //  删除阅后即焚消息后，重设会话记录的消息
            ChatsHistoryManager.updateChatLastMsg(
                myUid,
                chatType,
                targetId,
                lastModel,
                changeTime = false
            )

            ThreadUtils.runOnIOThread {
                // 发送上次未发出的回执
                receiptMsgs?.let {
                    if (chatType == ChatModel.CHAT_TYPE_GROUP) {
                        SendMessageManager.sendReceiptMessagePackage(it)
                    } else {
                        SendMessageManager.sendReceiptMessagePackage(it)
                    }
                }
            }

            complete?.invoke()
        }, error)
    }

    private fun checkSpecifiedTimeMessageRecallStatus(realm: Realm): List<MessageModel> {
        val recallMsgs = mutableListOf<MessageModel>()
        val recallingMsgModel = realm.where(MessageModel::class.java)
            .equalTo("type", MessageModel.MESSAGE_TYPE_RECALL)
            .findAll()
        recallingMsgModel.forEach { model ->
            recallMsgs.add(model.copyMessage())
        }
        return recallMsgs
    }

    private fun checkSpecifiedTimeMessageReceiptStatus(
        chatType: Int,
        targetId: Long,
        realm: Realm
    ): List<IMPB.ReceiptMessage> {
        val msgReceipts = ArrayList<IMPB.ReceiptMessage>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR, -24)//只处理24小时内的消息
        val readedMsgModel = realm.where(MessageModel::class.java)
            .greaterThan("time", cal.timeInMillis)
            .and()
            .equalTo("isRead", (-1).toInt())
            .and()
            .equalTo("isSend", 0.toInt())
            .findAll()
        readedMsgModel.forEach { model ->
            if (model.originalMessageType != -1) {
                val receiptStatus = CommonProto.MsgReceiptStatusBase.newBuilder()
                    .setStatus(CommonProto.MsgReceiptStatus.VIEWED).setTime(model.readTime)
                val receiptMsgPackage = if (chatType == ChatModel.CHAT_TYPE_GROUP) {
                    IMPB.ReceiptMessage.newBuilder().setMsgId(model.msgId)
                        .setType(IMPB.ChatMessageType.GROUP)
                        .setMessageTypeValue(model.originalMessageType)
                        .setSnapchatTime(model.snapchatTime)
                        .setDuration(model.durationTime)
                        .setGroupId(targetId).setSendUid(model.ownerUid)
                        .setReceiptStatus(receiptStatus).build()
                } else {
                    IMPB.ReceiptMessage.newBuilder().setMsgId(model.msgId)
                        .setType(IMPB.ChatMessageType.ONE_TO_ONE)
                        .setMessageTypeValue(model.originalMessageType)
                        .setSnapchatTime(model.snapchatTime)
                        .setDuration(model.durationTime)
                        .setSendUid(targetId).setReceiptStatus(receiptStatus).build()
                }
                msgReceipts.add(receiptMsgPackage)
            } else {
                if (ArouterServiceManager.messageService.getCurrentTime() - model.readTime > Constant.Common.AUTO_CLEAR_MESSAGE_MAX_TIME) {
                    model.isRead = 1
                }
            }
        }

        val readedAttachmentMsgModel = realm.where(MessageModel::class.java)
            .greaterThan("time", cal.timeInMillis)
            .and()
            .equalTo("isReadedAttachment", (-1).toInt())
            .and()
            .equalTo("isSend", 0.toInt())
            .findAll()
        readedAttachmentMsgModel.forEach { model ->
            if (model.originalMessageType != -1) {
                val receiptStatus = CommonProto.MsgReceiptStatusBase.newBuilder()
                    .setStatus(CommonProto.MsgReceiptStatus.PLAYED)
                    .setTime(model.readedAttachmentTime)
                val receiptMsgPackage = if (chatType == ChatModel.CHAT_TYPE_GROUP) {
                    IMPB.ReceiptMessage.newBuilder().setMsgId(model.msgId)
                        .setType(IMPB.ChatMessageType.GROUP)
                        .setMessageTypeValue(model.originalMessageType)
                        .setSnapchatTime(model.snapchatTime)
                        .setDuration(model.durationTime)
                        .setGroupId(targetId).setSendUid(model.ownerUid)
                        .setReceiptStatus(receiptStatus).build()
                } else {
                    IMPB.ReceiptMessage.newBuilder().setMsgId(model.msgId)
                        .setType(IMPB.ChatMessageType.ONE_TO_ONE)
                        .setMessageTypeValue(model.originalMessageType)
                        .setSnapchatTime(model.snapchatTime)
                        .setDuration(model.durationTime)
                        .setSendUid(targetId).setReceiptStatus(receiptStatus).build()
                }
                msgReceipts.add(receiptMsgPackage)
            } else {
                if (ArouterServiceManager.messageService.getCurrentTime() - model.readedAttachmentTime > Constant.Common.AUTO_CLEAR_MESSAGE_MAX_TIME) {
                    model.isReadedAttachment = 1
                }
            }
        }

        return msgReceipts
    }

    private fun checkSpecifiedTimeMessageFireStatus(
        chatType: Int,
        targetId: Long,
        realm: Realm
    ): Int {
        val fireMsgModels = realm.where(MessageModel::class.java)
            ?.notEqualTo("expireTime", 0L)
            ?.and()
            ?.lessThan("expireTime", ArouterServiceManager.messageService.getCurrentTime())
            ?.sort("time", Sort.DESCENDING)
            ?.findAll()
        val count = fireMsgModels?.size ?: 0
        if (count > 0) {
            val commonPref = if (chatType == ChatModel.CHAT_TYPE_PVT) {
                createStorageInstance(
                    CommonPref::class.java,
                    "user_${AccountManager.getLoginAccountUUid()}_${targetId}"
                )
            } else {
                createStorageInstance(
                    CommonPref::class.java,
                    "group_${AccountManager.getLoginAccountUUid()}_${targetId}"
                )
            }
            val lastAutoDeleteFireMsgTime = commonPref.getLastAutoDeleteFireMsgTime()
            val currentAutoDeleteFireMsgTime = fireMsgModels?.first()?.time ?: 0
            if (lastAutoDeleteFireMsgTime < currentAutoDeleteFireMsgTime) {
                commonPref.putLastAutoDeleteFireMsgTime(currentAutoDeleteFireMsgTime)
            }

            deleteMessageFilesCache(fireMsgModels)
            deleteMessageFromDB2(fireMsgModels)
            fireMsgModels?.deleteAllFromRealm()
        }

        return count
    }

    private fun parseUnKnowMessage(realm: Realm): Boolean {
        var isChangeData = false
        val msgs =
            realm.where(MessageModel::class.java).equalTo("type", MessageModel.MESSAGE_TYPE_UNKNOW)
                .and().equalTo("isSend", 0.toInt()).findAll()
        msgs?.forEach { model ->
            val data = model.contentBytes
            if (data != null) {
                when {
                    model.originalType == IMPB.MessageType.location.number -> {
                        val content = IMPB.LocationObj.parseFrom(data)
                        val locationMessageContentBean = LocationMessageContentBean()
                        locationMessageContentBean.lat = content.lat
                        locationMessageContentBean.lng = content.lng
                        locationMessageContentBean.address = content.address
                        model.content =
                            GsonInstanceCreater.defaultGson.toJson(locationMessageContentBean)
                        model.type = MessageModel.MESSAGE_TYPE_LOCATION
                        isChangeData = true
                    }
                    model.originalType == IMPB.MessageType.file.number -> {
                        val content = IMPB.FileObj.parseFrom(data)
                        val fileMessageContentBean = FileMessageContentBean()
                        fileMessageContentBean.fileUri = content.fileUrl
                        fileMessageContentBean.name = content.name
                        fileMessageContentBean.size = content.size
                        fileMessageContentBean.mimeType = content.mimeType
                        model.content =
                            GsonInstanceCreater.defaultGson.toJson(fileMessageContentBean)
                        model.type = MessageModel.MESSAGE_TYPE_FILE
                        isChangeData = true
                    }
                    model.originalType == IMPB.MessageType.dynamicImage.number -> {
                        val content = IMPB.DynamicImageObj.parseFrom(data)
                        val imageMessageContentBean = ImageMessageContentBean()
                        imageMessageContentBean.imageFileUri = content.url
                        imageMessageContentBean.imageThumbFileUri = content.thumbUrl
                        imageMessageContentBean.width = content.width
                        imageMessageContentBean.height = content.height
                        model.content =
                            GsonInstanceCreater.defaultGson.toJson(imageMessageContentBean)
                        model.type = MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE
                        isChangeData = true
                    }
                }
                realm.copyToRealmOrUpdate(model)
            }
        }

        return isChangeData
    }

    fun insertGroupTipMessage(
        groupId: Long,
        msg: String,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeGroupChatTransactionAsync(myUid, groupId, { realm ->
            val msgModel = MessageModel.parseGroupTipMessage(
                msg,
                ArouterServiceManager.messageService.getCurrentTime(),
                groupId,
                groupId,
                myUid,
                ChatModel.CHAT_TYPE_GROUP
            )
            realm.copyToRealm(msgModel)
        }, complete, error)
    }

    fun executeChatTransactionAsync(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        cmd: (Realm) -> Unit,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        if (chatType == ChatModel.CHAT_TYPE_PVT) {
            RealmCreator.executePvtChatTransactionAsync(myUid, targetId, {
                cmd.invoke(it)
            }, complete, error)
        } else {
            RealmCreator.executeGroupChatTransactionAsync(myUid, targetId, {
                cmd.invoke(it)
            }, complete, error)
        }
    }

    /**
     * 检测chat列表里是否有阅后即分 消息
     */
    fun checkToMessageFireStatus(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        complete: ((Boolean) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        executeChatTransactionAsync(chatType, myUid, targetId, { realm ->
            // 检测消息阅后即焚状态
            if (chatType == ChatModel.CHAT_TYPE_PVT) {
                val fireMsgModels = realm.where(MessageModel::class.java)
                    ?.greaterThan("snapchatTime", 0L)
                    ?.findAll()
                val fireCount = fireMsgModels?.size ?: 0
                complete?.invoke(fireCount > 0)
            }
        })
    }


    fun setMsgIsRead(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        msgId: Long,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        executeChatTransactionAsync(chatType, myUid, targetId, { realm ->
            // 检测消息阅后即焚状态
            if (chatType == ChatModel.CHAT_TYPE_PVT) {
                val msgModels = realm.where(MessageModel::class.java)
                    ?.lessThanOrEqualTo("msgId", msgId)
                    ?.equalTo("isRead", 0L)
                    ?.findAll()
                msgModels?.forEach {
                    it.isRead = 1
                }
                realm.copyToRealmOrUpdate(msgModels)
                complete?.invoke()
            } else if (chatType == ChatModel.CHAT_TYPE_GROUP) {
                val msgModels = realm.where(MessageModel::class.java)
                    ?.lessThanOrEqualTo("msgId", msgId)
                    ?.equalTo("isRead", 0L)
                    ?.findAll()
                msgModels?.forEach {
                    it.isRead = 1
                }
                realm.copyToRealmOrUpdate(msgModels)
                complete?.invoke()
            }
        })
    }

    fun deleteMessageFileCache(msg: MessageModel) {
        deleteMessageFilesCacheImpl(arrayListOf(msg.copyMessage()))
        deleteMessageFromDB(mutableListOf(msg))
    }

    private fun deleteMessageFilesCache(msgs: RealmResults<MessageModel?>?) {
        val copyMsgs = arrayListOf<MessageModel>()
        msgs?.forEach {
            if (it != null) {
                copyMsgs.add(it.copyMessage())
            }
        }

        deleteMessageFilesCacheImpl(copyMsgs)
    }

    private fun deleteMessageFilesCacheImpl(copyMsgs: List<MessageModel>) {
        ThreadUtils.runOnIOThread {
            copyMsgs.forEach { copyMsg ->
                when (copyMsg.type) {
                    MessageModel.MESSAGE_TYPE_IMAGE -> {
                        if (copyMsg.imageMessageContent?.imageFileBackupUri?.indexOf(BaseApp.app.packageName) ?: -1 > 0) {
                            FileUtils.deleteQuietly(copyMsg.imageMessageContent?.imageFileBackupUri)
                        }

                        if (copyMsg.imageMessageContent?.imageThumbFileBackupUri?.indexOf(BaseApp.app.packageName) ?: -1 > 0) {
                            FileUtils.deleteQuietly(copyMsg.imageMessageContent?.imageThumbFileBackupUri)
                        }
                    }
                    MessageModel.MESSAGE_TYPE_VOICE -> {
                        if (copyMsg.voiceMessageContent?.recordFileBackupUri?.indexOf(BaseApp.app.packageName) ?: -1 > 0) {
                            FileUtils.deleteQuietly(copyMsg.voiceMessageContent?.recordFileBackupUri)
                        }
                    }
                    MessageModel.MESSAGE_TYPE_VIDEO -> {
                        if (copyMsg.videoMessageContent?.videoFileBackupUri?.indexOf(BaseApp.app.packageName) ?: -1 > 0) {
                            FileUtils.deleteQuietly(copyMsg.videoMessageContent?.videoFileBackupUri)
                        }

                        if (copyMsg.videoMessageContent?.videoThumbFileBackupUri?.indexOf(BaseApp.app.packageName) ?: -1 > 0) {
                            FileUtils.deleteQuietly(copyMsg.videoMessageContent?.videoThumbFileBackupUri)
                        }
                    }
                    MessageModel.MESSAGE_TYPE_FILE -> {
                        if (copyMsg.fileMessageContentBean?.fileBackupUri?.indexOf(BaseApp.app.packageName) ?: -1 > 0) {
                            FileUtils.deleteQuietly(copyMsg.fileMessageContentBean?.fileBackupUri)
                        }
                    }
                    MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE -> {
                        if (copyMsg.dynamicImageMessageBean?.imageFileBackupUri?.indexOf(BaseApp.app.packageName) ?: -1 > 0) {
                            FileUtils.deleteQuietly(copyMsg.dynamicImageMessageBean?.imageFileBackupUri)
                        }

                        if (copyMsg.dynamicImageMessageBean?.imageThumbFileBackupUri?.indexOf(
                                BaseApp.app.packageName
                            ) ?: -1 > 0
                        ) {
                            FileUtils.deleteQuietly(copyMsg.dynamicImageMessageBean?.imageThumbFileBackupUri)
                        }
                    }
                }
            }
        }
    }

    private fun deleteMessageFromDB2(msgs: RealmResults<MessageModel?>?) {
        val searchIDs = mutableListOf<Long>()
        msgs?.forEach {
            if (it != null) {
                searchIDs.add(it.msgId)
            }
        }
        ArouterServiceManager.searchService.deleteMessage(searchIDs)
    }

    fun deleteMessageFromDB(msgs: MutableList<MessageModel>) {
        val searchIDs = mutableListOf<Long>()
        msgs.forEach {
            searchIDs.add(it.msgId)
        }
        ArouterServiceManager.searchService.deleteMessage(searchIDs)
    }


    fun findChatMsg(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        msgId: Long,
        complete: (message: MessageModel?) -> Unit,
        error: ((Throwable) -> Unit)? = null
    ) {
        val model: MessageModel? = null
        if (chatType == ChatModel.CHAT_TYPE_PVT) {
            RealmCreator.executePvtChatTransactionAsync(myUid, targetId, { realm ->
                realm.where(MessageModel::class.java)
                    ?.equalTo("msgId", msgId)
                    ?.findFirst()?.copyMessage()
            }, {
                complete.invoke(model)
            }, error)
        } else {
            RealmCreator.executeGroupChatTransactionAsync(myUid, targetId, { realm ->
                realm.where(MessageModel::class.java)
                    ?.equalTo("msgId", msgId)
                    ?.findFirst()?.copyMessage()
            }, {
                complete.invoke(model)
            }, error)
        }
    }
}