package framework.telegram.business.ui.group.presenter

import com.im.domain.pb.ContactsProto
import com.im.domain.pb.GroupProto
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface UnAuditMemberContract {

    interface Presenter : BasePresenter {
        fun getUserDetail()

        fun makeOperate(groupReqId: Long, op: Boolean)
    }

    interface View : BaseView<Presenter> {

        fun showLoading()

        fun dissmissLoading()

        //刷新界面
        fun refreshUI(contact: ContactDataModel)

        //显示错误界面
        fun showError(errStr: String)

        fun destory()

        fun operateReq(op: Boolean)

    }
}