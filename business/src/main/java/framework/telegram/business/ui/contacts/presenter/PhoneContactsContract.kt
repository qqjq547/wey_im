package framework.telegram.business.ui.contacts.presenter

import com.im.domain.pb.ContactsProto
import framework.telegram.business.ui.contacts.bean.PhoneContactsBean
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface PhoneContactsContract {

    interface Presenter : BasePresenter {
        fun getContactsList(pageNum: Int, pageSize: Int)

        fun updateData()

        fun makeFriend(applyUid: Long, op: ContactsProto.ContactsOperator)
    }

    interface View : BaseView<Presenter> {
        //显示加载中
        fun showLoading()

        //刷新界面
        fun refreshListUI(pageNum:Int,list: MutableList<PhoneContactsBean>)

        //显示加载中
        fun showEmpty()

        //显示错误界面
        fun showError(errStr: String?)
    }
}