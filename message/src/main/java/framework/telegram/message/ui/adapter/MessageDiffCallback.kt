package framework.telegram.message.ui.adapter

import com.chad.library.adapter.base.diff.BaseQuickDiffCallback
import framework.ideas.common.model.im.MessageModel

//TODO  getChangePayload来分发
class MessageDiffCallback(newList: List<MessageModel>): BaseQuickDiffCallback<MessageModel>(newList){
    companion object {
        const val CODE_CONTENT = 1
        const val CODE_STATUS = 2
    }

    override fun areItemsTheSame(oldItem: MessageModel, newItem: MessageModel): Boolean {
        return (oldItem.msgId == newItem.msgId
                && oldItem.targetId == newItem.targetId)
    }

    override fun areContentsTheSame(oldItem: MessageModel, newItem: MessageModel): Boolean {
        return (oldItem.ownerName == newItem.ownerName
                && oldItem.ownerIcon == newItem.ownerIcon
                && oldItem.ownerUid == newItem.ownerUid
                && oldItem.keyVersion == newItem.keyVersion
                && oldItem.type == newItem.type
                && oldItem.isSend == newItem.isSend
                && oldItem.time == newItem.time
                && oldItem.content == newItem.content
                && oldItem.status == newItem.status
                && oldItem.isDeliver == newItem.isDeliver
                && oldItem.isRead == newItem.isRead
                && oldItem.isRetry == newItem.isRetry
                && oldItem.readTime == newItem.readTime
                && oldItem.snapchatTime == newItem.snapchatTime
                && oldItem.expireTime == newItem.expireTime
                && oldItem.isShowAlreadyRead == newItem.isShowAlreadyRead)
    }

//    override fun getChangePayload(oldItem: MessageModel, newItem: MessageModel): Any? {
//        var code = 0
//        if ((oldItem.type == newItem.type) && (
//                        oldItem.isSend != newItem.isSend
//                                || oldItem.status != newItem.status
//                                || oldItem.isDeliver != newItem.isDeliver
//                                || oldItem.isRead != newItem.isRead
//                                || oldItem.isRetry != newItem.isRetry
//                                || oldItem.readTime != newItem.readTime
//                                || oldItem.isShowAlreadyRead != newItem.isShowAlreadyRead))
//            code =  CODE_STATUS
//        else
//            code = CODE_CONTENT
//        return code
//    }

}