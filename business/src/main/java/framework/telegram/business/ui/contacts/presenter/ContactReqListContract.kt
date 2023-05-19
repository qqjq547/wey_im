package framework.telegram.business.ui.contacts.presenter

import com.im.domain.pb.ContactsProto
import framework.ideas.common.model.contacts.ContactReqModel
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface ContactReqListContract {

    interface Presenter : BasePresenter {
        fun updateData(data: List<ContactReqModel>)

        fun makeFriend(applyUid: Long, op: ContactsProto.ContactsOperator)
    }

    interface View : BaseView<Presenter> {
        //显示加载中
        fun showLoading()

        //刷新界面
        fun refreshListUI(list: MutableList<Any>)

        //显示加载中
        fun showEmpty()

        //显示错误界面
        fun showError(errStr: String?)


    }
}