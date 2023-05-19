package framework.telegram.message.bridge.service


import com.alibaba.android.arouter.facade.template.IProvider
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import io.realm.Realm

interface IMessageService : IProvider {

    /**
     * 获取当前服务器时间
     */
    fun getCurrentTime(): Long

    fun socketIsLogin(): Boolean

    fun sendScreenShotsPackage(sendUid: Long, receiveUid: Long)

    fun sendGroupNoticeMessageToGroup(
        noticeId: Long,
        noticeContent: String,
        showNotify: Boolean,
        myUid: Long,
        targetGid: Long
    )

    fun setFileTransferUserChatIsTop()

    fun getChatTopStatus(
        chaterType: Int,
        targetId: Long,
        complete: ((Boolean) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    fun setChatTopStatus(
        chaterType: Int,
        targetId: Long,
        isTop: Boolean,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    fun setChatIsReaded(
        chaterType: Int,
        targetId: Long,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    fun setChatIsUnreaded(
        chaterType: Int,
        targetId: Long,
        unreadCount: Int,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    fun setChatIsDisturb(
        chaterType: Int,
        targetId: Long,
        disturb: Boolean,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    fun getChatIsDisturb(
        chaterType: Int,
        targetId: Long,
        complete: ((Boolean) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    fun insertSystemTipMsg(
        chaterType: Int,
        targetId: Long,
        createTime: Long,
        content: String,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    fun clearMessageHistory(
        chaterType: Int,
        targetId: Long,
        complete: (() -> Unit)? = null,
        error: (() -> Unit)? = null
    )

    fun deleteChat(
        chaterType: Int,
        targetId: Long,
        complete: (() -> Unit)? = null,
        error: (() -> Unit)? = null
    )

    fun deleteAllChat(complete: (() -> Unit)? = null, error: (() -> Unit)? = null)

    fun deleteAccountData(myUid: Long)

    fun deleteToGroupAllMessageReceipt(
        targetGid: Long,
        targerUid: Long,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    fun getAllChat(
        complete: ((List<ChatModel>) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    fun resetChaterInfo(
        chaterType: Int,
        targetId: Long,
        chaterName: String? = null,
        chaterNickName: String? = null,
        chaterIcon: String? = null,
        bfDisturb: Boolean? = null
    )

    fun deleteStreamCallHistory(
        targetId: Long,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    fun resetStreamCallChaterInfo(
        targetId: Long,
        chaterName: String? = null,
        chaterNickName: String? = null,
        chaterIcon: String? = null
    )

    fun getAllUnreadMessageCount(): Int

    fun newStreamCallRealm(): Realm

    fun newSearchContentRealm(targetGid: Long): Realm

    fun newSearchGroupContentRealm(targetGid: Long): Realm

    fun insertGroupTipMessage(
        groupId: Long,
        msg: String,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    fun updateChatLastMsg(
        myUid: Long,
        chaterType: Int,
        chaterId: Long,
        lastMsg: MessageModel?,
        unreadCount: Int? = 0,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null,
        changeTime: Boolean = true
    )

    fun findChatLastMsg(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        complete: (lastBizMessage: MessageModel?) -> Unit,
        error: ((Throwable) -> Unit)? = null
    )

    fun executeChatsHistoryTransactionAsync(
        myUid: Long,
        cmd: (Realm) -> Unit,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    fun createChatHistory(
        chaterType: Int,
        myUid: Long,
        targetId: Long,
        forcedCreate: Boolean = true
    )

    fun syncAllChatMessage(myUid: Long, finish: () -> Unit)

    fun findChatMsg(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        msgId: Long,
        complete: (message: MessageModel?) -> Unit,
        error: ((Throwable) -> Unit)? = null
    )

    fun recallMessage(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        msgId: Long,
        isMine: Boolean
    )

    fun recallMessages(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        clearTime: Long,
        deleteChat: Boolean
    )
}