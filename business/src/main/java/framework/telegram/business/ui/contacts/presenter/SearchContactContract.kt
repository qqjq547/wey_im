package framework.telegram.business.ui.contacts.presenter

import com.im.domain.pb.CommonProto
import com.im.domain.pb.ContactsProto
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface SearchContactContract {

    interface Presenter : BasePresenter {
        fun getDataList( phone: String)
    }

    interface View : BaseView<Presenter> {
        //显示加载中
        fun showLoading()

        //刷新界面
        fun refreshUI(result : ContactsProto.FindContactsListResp)

        //显示加载中
        fun showEmpty(errStr :String?)

        //显示错误界面
        fun showError(errStr :String?)
    }
}