package framework.telegram.message.ui.pvt

import com.im.domain.pb.ContactsProto
import framework.ideas.common.bean.RefMessageBean
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView
import io.realm.RealmResults

interface PrivateChatContract {

    interface Presenter : BasePresenter {
        fun start(isLoadAll: Boolean)

        fun loadMessageHistory()

        fun loadAllMessageHistory()

        fun sendTextMessage(
            msg: String,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            contactDataModel: ContactDataModel?
        )

        fun sendVoiceMessage(
            recordTime: Int,
            recordFilePath: String,
            highDArr: Array<Int>,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            contactDataModel: ContactDataModel?
        )

        fun sendImageMessage(
            imageFilePath: String,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            contactDataModel: ContactDataModel?
        )

        fun sendDynamicImageMessage(
            emoticonId: Long,
            imageFilePath: String,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            contactDataModel: ContactDataModel?
        )

        fun sendDynamicImageUrlMessage(
            emoticonId: Long,
            imageFileUrl: String,
            width: Int,
            height: Int,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            contactDataModel: ContactDataModel?
        )

        fun sendVideoMessage(
            videoFilePath: String,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            contactDataModel: ContactDataModel?
        )

        fun sendNameCardMessage(
            uid: Long,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            contactDataModel: ContactDataModel?
        )

        fun sendLocationMessage(
            lat: Long,
            lng: Long,
            address: String,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            contactDataModel: ContactDataModel?
        )

        fun sendFileMessage(
            filePath: String,
            mimeType: String,
            refMessageBean: MessageModel?,
            myUid: Long,
            targetUid: Long,
            contactDataModel: ContactDataModel?
        )

        fun setAllMessageReaded()

        fun addFriend(
            identify: String
        )

        fun getAddToken(
            identify: String,
            userId: Long,
            findSign: String
        )

        fun checkFriendShip(
            myUid: Long,
            targetUid: Long,
            complete: (Boolean) -> Unit,
            error: ((Throwable) -> Unit)?
        )

        fun updataFriendShip(
            myUid: Long,
            targetUid: Long,
            isFriend: Boolean,
            complete: () -> Unit,
            error: ((Throwable) -> Unit)?
        )

        fun isLoadAll(): Boolean
    }

    interface View : BaseView<Presenter> {

        fun getAdapterItemCount(): Int

        fun onMessagesLoadComplete(messageModels: RealmResults<MessageModel>?)

        //显示加载中
        fun onMessagesChanged(result: RealmResults<MessageModel>)

        fun isActivityPause(): Boolean

        fun getAddToken(userId: Long, addToken: String)

        // 显示添加好友的提示消息
        fun showAddFriendMsg()

        // 显示未读消息提醒
        fun showNewMsgTip(unreadCount: Int, firstMsgLocalId: Long)

        // 重置输入状态
        fun resetInputingStatus()

        // 显示错误消息
        fun showErrorMsg(code: Int, errStr: String?)
    }
}