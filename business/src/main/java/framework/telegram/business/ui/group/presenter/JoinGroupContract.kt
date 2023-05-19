package framework.telegram.business.ui.group.presenter

import com.im.domain.pb.ContactsProto
import com.im.domain.pb.GroupProto
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface JoinGroupContract {

    interface Presenter : BasePresenter {

        fun getDataDetail(groupId: Long, qrCode: String,idCode: String)

        fun joinGroup(groupId: Long, msg: String)

        fun isJoinCheck(): Boolean
    }

    interface View : BaseView<Presenter> {

        //刷新界面
        fun refreshUI(groupBase: GroupProto.GroupBase,isBan:Boolean)

        fun showLoading()

        fun dissmissLoading()

        //显示错误界面
        fun showError(errStr: String)

        fun destory()

        fun joinSuccess(bfJoinCheck: Boolean)

        fun jumpToChat()
    }
}