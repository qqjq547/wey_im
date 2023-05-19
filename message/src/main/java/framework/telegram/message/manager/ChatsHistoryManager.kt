package framework.telegram.message.manager

import android.text.TextUtils
import android.util.Log
import framework.ideas.common.model.group.GroupMemberModel
import framework.telegram.message.db.RealmCreator
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.event.MessageStateChangeEvent
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import io.realm.Realm
import java.lang.Exception

object ChatsHistoryManager {

    /**
     * 检查是否创建了会话记录，如果没有创建自动创建
     */
    fun checkChatHistoryIsCreated(
        chaterType: Int,
        myUid: Long,
        targetId: Long,
        lastMsg: MessageModel? = null,
        unReadCount: Int? = null,
        atMeCount: Int? = null,
        ownerMember: GroupMemberModel? = null,
        memberName: String? = "",
        forcedCreate: Boolean = true,
        complete: (() -> Unit)? = null
    ) {
        var chaterInfoIsEmpty = false
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, { realm ->
            val chat = realm.where(ChatModel::class.java).equalTo("chaterType", chaterType).and()
                .equalTo("chaterId", targetId).findFirst()
            if (chat != null) {
                // 如果icon或者name为空，则需要重新获取个人资料
                if (TextUtils.isEmpty(chat.chaterIcon) && TextUtils.isEmpty(chat.chaterName)) {
                    chaterInfoIsEmpty = true
                }

                // 设置最后一条消息
                if (lastMsg != null) {
                    chat.lastMsgLocalId = lastMsg.id
                    if (chaterType == ChatModel.CHAT_TYPE_GROUP) {
                        if (ownerMember != null) {
                            chat.lastMsg =
                                "${ownerMember.displayName}:${lastMsg.chatContentDescribe}"
                        } else if (!TextUtils.isEmpty(memberName)) {
                            chat.lastMsg = "$memberName:${lastMsg.chatContentDescribe}"
                        } else {
                            chat.lastMsg = lastMsg.chatContentDescribe
                        }
                    } else {
                        chat.lastMsg = lastMsg.chatContentDescribe
                    }
                    chat.lastMsgTime = lastMsg.time
                } else {
                    chat.lastMsgTime = ArouterServiceManager.messageService.getCurrentTime()
                }

                // 设置未读数量
                if (unReadCount != null) {
                    chat.unReadCount = unReadCount
                }

                // 设置@me数量
                if (atMeCount != null) {
                    chat.atMeCount = chat.atMeCount + (atMeCount)
                }
                realm.copyToRealmOrUpdate(chat)
            } else {
                if (forcedCreate) {
                    chaterInfoIsEmpty = true
                    val chatModel = if (lastMsg != null) {
                        val lastMsgContent = if (chaterType == ChatModel.CHAT_TYPE_GROUP) {
                            if (ownerMember != null) {
                                "${ownerMember.displayName}:${lastMsg.chatContentDescribe}"
                            } else if (!TextUtils.isEmpty(memberName)) {
                                "$memberName:${lastMsg.chatContentDescribe}"
                            } else {
                                lastMsg.chatContentDescribe
                            }
                        } else {
                            lastMsg.chatContentDescribe
                        }

                        ChatModel.createChat(
                            chaterType, targetId, "", "", "",
                            lastMsg.id, lastMsgContent, lastMsg.time,
                            unReadCount ?: 0, atMeCount ?: 0
                        )
                    } else {
                        ChatModel.createChat(
                            chaterType, targetId, "", "", "",
                            0, null, ArouterServiceManager.messageService.getCurrentTime(),
                            unReadCount ?: 0, atMeCount ?: 0
                        )
                    }
                    realm.copyToRealm(chatModel)
                }
            }
        }, {
            if (forcedCreate) {
                if (chaterInfoIsEmpty) {
                    // 获取聊天对象的资料并更新到数据库
                    updateChaterInfo(myUid, chaterType, targetId) {
                        complete?.invoke()
                    }
                } else {
                    complete?.invoke()
                }
            } else {
                complete?.invoke()
            }
        })
    }

    /**
     * 更新会话记录中的最后一条消息记录
     */
    fun updateChatLastMsg(
        myUid: Long,
        chaterType: Int,
        chaterId: Long,
        lastMsg: MessageModel?,
        unreadCount: Int? = null,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null,
        changeTime: Boolean = true
    ) {
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, { realm ->
            val chatModel =
                realm.where(ChatModel::class.java).equalTo("chaterType", chaterType).and()
                    .equalTo("chaterId", chaterId).findFirst()
            chatModel?.let { model ->
                model.lastMsgLocalId = lastMsg?.id ?: 0
                if (unreadCount != null) {
                    if (unreadCount < 0) {
                        model.unReadCount = model.unReadCount + unreadCount
                    } else {
                        model.unReadCount = unreadCount
                    }
                }

                if (lastMsg != null) {
                    if ((lastMsg.type == MessageModel.MESSAGE_TYPE_RECALL || lastMsg.type == MessageModel.MESSAGE_TYPE_RECALL_SUCCESS) && lastMsg.isSend == 1) {
                        model.lastMsg = BaseApp.app.getString(R.string.you_withdrew_a_message)
                    } else {
                        if (chaterType == ChatModel.CHAT_TYPE_GROUP) {
                            if (lastMsg.ownerName != null) {
                                model.lastMsg =
                                    "${lastMsg.ownerName}:${lastMsg.chatContentDescribe}"
                            } else {
                                model.lastMsg = lastMsg.chatContentDescribe ?: ""
                            }
                        } else {
                            model.lastMsg = lastMsg.chatContentDescribe ?: ""
                        }
                    }

                    if (changeTime) {
                        model.lastMsgTime = lastMsg.time
                    }
                } else {
                    model.lastMsg = ""
                }

                realm.copyToRealmOrUpdate(model)

                lastMsg?.copyMessage()?.let {
                    EventBus.publishEvent(MessageStateChangeEvent(mutableListOf(it)))
                }
            }
        }, complete, error)
    }

    private fun updateChaterInfo(
        myUid: Long,
        chaterType: Int,
        targetId: Long,
        complete: (() -> Unit)
    ) {
        if (chaterType == ChatModel.CHAT_TYPE_PVT) {
            ArouterServiceManager.contactService.getContactInfo(
                null,
                targetId,
                { contactInfoModel, _ ->
                    updateChaterInfo(
                        myUid,
                        chaterType,
                        targetId,
                        if (TextUtils.isEmpty(contactInfoModel.noteName)) contactInfoModel.nickName else contactInfoModel.noteName,
                        contactInfoModel.nickName,
                        contactInfoModel.icon,
                        contactInfoModel.isBfDisturb,
                        complete
                    )
                })
        } else if (chaterType == ChatModel.CHAT_TYPE_GROUP) {
            ArouterServiceManager.groupService.getGroupInfo(null, targetId, { groupInfoModel, _ ->
                updateChaterInfo(
                    myUid,
                    chaterType,
                    targetId,
                    groupInfoModel.name,
                    groupInfoModel.name,
                    groupInfoModel.pic,
                    groupInfoModel.bfDisturb,
                    complete
                )
            })
        }
    }

    /**
     * 更新聊天对象的资料
     */
    fun updateChaterInfo(
        myUid: Long,
        chaterType: Int,
        targetId: Long,
        chaterName: String?,
        chaterNickName: String?,
        chaterIcon: String?,
        bfDisturb: Boolean?,
        complete: (() -> Unit)
    ) {
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, { realm ->
            val chat = realm.where(ChatModel::class.java).equalTo("chaterType", chaterType).and()
                .equalTo("chaterId", targetId).findFirst()
            chat?.let {
                if (!TextUtils.isEmpty(chaterName)) {
                    it.chaterName = chaterName
                } else {
                    it.chaterName = chaterNickName
                }
                if (!TextUtils.isEmpty(chaterNickName)) {
                    it.chaterNickName = chaterNickName
                }
                if (chaterIcon != null) {
                    it.chaterIcon = chaterIcon
                }
                if (bfDisturb != null) {
                    it.bfDisturb = if (bfDisturb) 1 else 0
                }
                realm.copyToRealmOrUpdate(it)
                complete.invoke()
            }
        })
    }

    /**
     * 查询聊天记录中有没有群通知，没有就建立
     */
    fun selectChatHistoryHasGroupNotify(
        myUid: Long,
        lastMsg: String,
        time: Long,
        unReadCount: Int,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, { realm ->
            val chat = realm.where(ChatModel::class.java)
                .equalTo("chaterType", ChatModel.CHAT_TYPE_GROUP_NOTIFY).and().findFirst()
            if (chat != null) {
                chat.lastMsgTime = time
                chat.unReadCount = unReadCount
                chat.lastMsg = lastMsg
                realm.copyToRealmOrUpdate(chat)
            } else {
                val msgModel = ChatModel.createChat(
                    ChatModel.CHAT_TYPE_GROUP_NOTIFY,
                    0,
                    BaseApp.app.getString(R.string.group_of_notice),
                    "",
                    "",
                    0,
                    lastMsg,
                    time,
                    unReadCount
                )
                realm.copyToRealm(msgModel)
            }
        }, complete, error)
    }

    /**
     * 将指定会话的聊天信息全部置为已读
     */
    fun setChatMessagesAllReaded(
        myUid: Long,
        chaterType: Int,
        chaterId: Long,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, { realm ->
            val chat = realm.where(ChatModel::class.java).equalTo("chaterType", chaterType).and()
                .equalTo("chaterId", chaterId).findFirst()
            chat?.let {
                chat.unReadCount = 0
                realm.copyToRealmOrUpdate(chat)
            }
        }, complete, error)
    }

    fun setChatUnReadCount(
        myUid: Long,
        chaterType: Int,
        chaterId: Long,
        unReadCount: Int,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, { realm ->
            val chat = realm.where(ChatModel::class.java).equalTo("chaterType", chaterType).and()
                .equalTo("chaterId", chaterId).findFirst()
            chat?.let {
                chat.unReadCount = 0.coerceAtLeast(unReadCount)
                realm.copyToRealmOrUpdate(chat)
            }
        }, complete, error)
    }

    /**
     * 获取指定会话的置顶状态
     */
    fun getChatTopStatus(
        myUid: Long,
        chaterType: Int,
        chaterId: Long,
        complete: ((Boolean) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        var isTop = false
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, { realm ->
            val chatModel =
                realm.where(ChatModel::class.java).equalTo("chaterType", chaterType).and()
                    .equalTo("chaterId", chaterId).findFirst()
            chatModel?.let { model ->
                isTop = model.isTop == 1
            }
        }, {
            complete?.invoke(isTop)
        }, error)
    }

    /**
     * 设置指定会话的指定状态
     */
    fun setChatTopStatus(
        myUid: Long,
        chaterType: Int,
        chaterId: Long,
        isTop: Boolean,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, { realm ->
            val chatModel =
                realm.where(ChatModel::class.java).equalTo("chaterType", chaterType).and()
                    .equalTo("chaterId", chaterId).findFirst()
            chatModel?.let { model ->
                model.isTop = if (isTop) 1 else 0
                realm.copyToRealmOrUpdate(model)
            }
        }, complete, error)
    }

    /**
     * 删除一条会话记录
     */
    fun deleteChat(
        myUid: Long,
        chaterType: Int,
        chaterId: Long,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, { realm ->
            val model =
                realm.where(ChatModel::class.java).equalTo("chaterType", chaterType).and()
                    .equalTo("chaterId", chaterId).findFirst()
            model?.let {
                it.deleteFromRealm()
            }
        }, complete, error)
    }

    fun deleteChatWithLastMsgTime(
        myUid: Long,
        chaterType: Int,
        chaterId: Long,
        time: Long,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        var chatModel: ChatModel? = null
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, { realm ->
            val model =
                realm.where(ChatModel::class.java).equalTo("chaterType", chaterType).and()
                    .equalTo("chaterId", chaterId).findFirst()
            if (model != null && model.lastMsgTime > 0 && model.lastMsgTime <= time) {
                chatModel = model.copyChat()
                model.deleteFromRealm()
            }
        }, {
            chatModel?.let {
                RealmCreator.executeDeleteChatsHistoryTransactionAsync(myUid, { realm ->
                    realm.where(ChatModel::class.java).equalTo("chaterType", chaterType).and()
                        .equalTo("chaterId", chaterId).findAll()?.deleteAllFromRealm()

                    val chat = it.copyChat()
                    chat.id = 0
                    chat.lastMsgTime = ArouterServiceManager.messageService.getCurrentTime()
                    realm.copyToRealm(chat)

                    Log.d("demo", "已保存 ${chat.chaterName}-${chat.chaterId} 至需远程删除的会话记录中")
                })
            }

            complete?.invoke()
        }, error)
    }

    /**
     * 清空一条会话记录
     */
    fun clearChat(
        myUid: Long,
        chaterType: Int,
        chaterId: Long,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, { realm ->
            val chatModel =
                realm.where(ChatModel::class.java).equalTo("chaterType", chaterType).and()
                    .equalTo("chaterId", chaterId).findFirst()
            chatModel?.let {
                chatModel.lastMsgLocalId = 0
                chatModel.lastMsg = ""
                chatModel.unReadCount = 0
                realm.copyToRealmOrUpdate(chatModel)
            }
        }, complete, error)
    }

    /**
     * 清除会话中的@me数量
     */
    fun clearChatAtMeCount(
        myUid: Long,
        chaterType: Int,
        chaterId: Long,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        RealmCreator.executeChatsHistoryTransactionAsync(myUid, { realm ->
            val chatModel =
                realm.where(ChatModel::class.java).equalTo("chaterType", chaterType).and()
                    .equalTo("chaterId", chaterId).findFirst()
            chatModel?.let { model ->
                model.atMeCount = 0
                realm.copyToRealmOrUpdate(model)
            }
        }, complete, error)
    }

    /**
     * 已同步方式获取所有未读消息的数量
     */
    fun syncGetAllUnreadMessageCount(): Int {
        var unreadCount = 0
        var realm: Realm? = null
        try {
            val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
            realm = RealmCreator.getChatsHistoryRealm(myUid)
            realm.where(ChatModel::class.java).findAll()?.forEach {
                if (it.bfDisturb == 0) {
                    unreadCount += it.unReadCount
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            realm?.close()
        }
        return unreadCount
    }
}