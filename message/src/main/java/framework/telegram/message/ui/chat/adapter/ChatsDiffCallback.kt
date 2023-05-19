package framework.telegram.message.ui.chat.adapter

import com.chad.library.adapter.base.diff.BaseQuickDiffCallback
import framework.telegram.message.ui.chat.ChatCacheModel

class ChatsDiffCallback(newList: List<ChatCacheModel>) : BaseQuickDiffCallback<ChatCacheModel>(newList) {
    companion object {
        /**
         *  关联  lastMsg, lastMsgTime, bfDisturb, unReadCount, atMeCount
         *  消息内容
         */
        const val CODE_MSG = 1
        /**
         *  关联  isTop, chaterIcon, chaterName, chaterNickName
         *  对方的状态
         */
        const val CODE_CHATER_INFO = 3

        const val CODE_ONLINE = 4
        const val CODE_STATUS = 5
        const val CODE_FIRE = 6
    }

    override fun areItemsTheSame(oldItem: ChatCacheModel, newItem: ChatCacheModel): Boolean {
        return (oldItem.chatModel.chaterId == newItem.chatModel.chaterId
                && oldItem.chatModel.chaterType == newItem.chatModel.chaterType)
    }

    override fun areContentsTheSame(oldItem: ChatCacheModel, newItem: ChatCacheModel): Boolean {
        val result = (oldItem.chatModel.lastMsg == newItem.chatModel.lastMsg
                        && oldItem.chatModel.lastMsgTime == newItem.chatModel.lastMsgTime
                        && oldItem.chatModel.chaterIcon == newItem.chatModel.chaterIcon
                        && oldItem.chatModel.chaterName == newItem.chatModel.chaterName
                        && oldItem.chatModel.chaterNickName == newItem.chatModel.chaterNickName
                        && oldItem.chatModel.atMeCount == newItem.chatModel.atMeCount
                        && oldItem.chatModel.isTop == newItem.chatModel.isTop
                        && oldItem.chatModel.bfDisturb == newItem.chatModel.bfDisturb
                        && oldItem.msgStatus == newItem.msgStatus
                        && oldItem.isOnlineStatus == newItem.isOnlineStatus
                        && oldItem.isFireStatus == newItem.isFireStatus
                        && oldItem.chatModel.unReadCount == newItem.chatModel.unReadCount)
        return result
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        var code = -1
        if (oldItem.isOnlineStatus != newItem.isOnlineStatus)
            code = CODE_ONLINE
        if (oldItem.msgStatus != newItem.msgStatus)
            code = CODE_STATUS
        if (oldItem.isFireStatus != newItem.isFireStatus)
            code = CODE_FIRE
        if (oldItem.chatModel.atMeCount != newItem.chatModel.atMeCount
                || oldItem.chatModel.lastMsg != newItem.chatModel.lastMsg
                || oldItem.chatModel.lastMsgTime != newItem.chatModel.lastMsgTime
                || oldItem.chatModel.bfDisturb != newItem.chatModel.bfDisturb
                || oldItem.chatModel.unReadCount != newItem.chatModel.unReadCount)
            code = CODE_MSG

        if (oldItem.chatModel.isTop != newItem.chatModel.isTop
                || oldItem.chatModel.chaterIcon != newItem.chatModel.chaterIcon
                || oldItem.chatModel.chaterName != newItem.chatModel.chaterName
                || oldItem.chatModel.chaterNickName != newItem.chatModel.chaterNickName)
            code = CODE_CHATER_INFO
        return code
    }

}