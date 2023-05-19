package framework.telegram.message.ui.group

import framework.ideas.common.bean.RefMessageBean
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView
import io.realm.RealmResults

interface GroupChatContract {

    interface Presenter : BasePresenter {
        fun start(isLoadAll: Boolean)

        fun loadMessageHistory()

        fun loadAllMessageHistory()

        fun sendTextMessage(
            msg: String,
            atUids: List<Long>?,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            groupInfoModel: GroupInfoModel?
        )

        fun sendVoiceMessage(
            recordTime: Int,
            recordFilePath: String,
            highDArr: Array<Int>,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            groupInfoModel: GroupInfoModel?
        )

        fun sendImageMessage(
            imageFilePath: String,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            groupInfoModel: GroupInfoModel?
        )

        fun sendDynamicImageMessage(
            emoticonId: Long,
            imageFilePath: String,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            groupInfoModel: GroupInfoModel?
        )

        fun sendDynamicImageUrlMessage(
            emoticonId: Long,
            imageFileUrl: String,
            width: Int,
            height: Int,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            groupInfoModel: GroupInfoModel?
        )

        fun sendVideoMessage(
            videoFilePath: String,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            groupInfoModel: GroupInfoModel?
        )

        fun sendNameCardMessage(
            uid: Long,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            groupInfoModel: GroupInfoModel?
        )

        fun sendLocationMessage(
            lat: Long,
            lng: Long,
            address: String,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            groupInfoModel: GroupInfoModel?
        )

        fun sendFileMessage(
            filePath: String,
            mimeType: String,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            groupInfoModel: GroupInfoModel?
        )

        fun clickAtMeButton()

        fun setAllMessageReaded()

        fun getAddToken(
            identify: String,
            userId: Long,
            findSign: String
        )

        fun isLoadAll(): Boolean
    }

    interface View : BaseView<Presenter> {

        fun getAdapterItemCount(): Int

        fun onMessagesLoadComplete(messageModels: RealmResults<MessageModel>?)

        //显示加载中
        fun onMessagesChanged(result: RealmResults<MessageModel>)

        fun refreshAtMeButton(count: Int)

        fun scrollToPosition(position: Int)

        fun isActivityPause(): Boolean

        fun getVisibleItemCount(): Int

        fun isShutup(): Boolean

        // 显示未读消息提醒
        fun showNewMsgTip(unreadCount: Int, firstMsgLocalId: Long)

        fun getAddToken(userId: Long, addToken: String)

        fun showError(msg: String)
    }
}