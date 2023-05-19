package framework.telegram.business.ui.group.presenter

import com.im.domain.pb.ContactsProto
import com.im.domain.pb.GroupProto
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface UnAuditInviteMemberContract {

    interface Presenter : BasePresenter {
        fun makeOperate(groupReqId: Long, op: Boolean)
    }

    interface View : BaseView<Presenter> {
        //刷新界面
        fun refreshUI()

        //显示错误界面
        fun showError(errStr: String?)

        fun destory()

        fun operateReq(op: Boolean)
    }
}