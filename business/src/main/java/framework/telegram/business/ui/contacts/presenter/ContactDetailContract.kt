package framework.telegram.business.ui.contacts.presenter

import com.im.domain.pb.ContactsProto
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface ContactDetailContract {

    interface Presenter : BasePresenter {

        fun getUpdateDetailInfo()

        fun addFriend(
            msg: String,
            groupId: Long,
            type: ContactsProto.ContactsAddType,
            op: ContactsProto.ContactsOperator,
            addToken: String
        )

        fun setBlack(isBlack: Boolean)

        fun deleteFriend(op: ContactsProto.ContactsOperator)

        fun setNoteName(nickName: String, note: String)

        fun setDescribe(describe: String)

        fun setStarFriend(star: Boolean)

        fun setDisturb(disturb: Boolean)

        fun setBanWord(banTime: Int)
    }

    interface View : BaseView<Presenter> {

        // 显示错误消息
        fun showErrorMsg(errStr: String?)

        // 显示添加好友的提示消息
        fun showAddFriendMsg()

        // 显示用户信息
        fun showUserInfo(info: ContactDataModel)

        fun showBlackInfo(bfMyBlack: Boolean)

        fun showNoteName(noteName: String)

        fun showDescribe(describe: String)

        fun showDisturb(disturb: Boolean)

        fun showStarFriend(bfStar: Boolean)

        fun showLoading()

        fun deleteFriend()

        fun showBanWord(banTime: Int)

        fun setAddToken(addToken: String)
    }
}