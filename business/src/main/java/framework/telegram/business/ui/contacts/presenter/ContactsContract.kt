package framework.telegram.business.ui.contacts.presenter

import framework.telegram.business.ui.contacts.bean.ContactItemBean
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface ContactsContract {

    interface Presenter : BasePresenter {
        fun setStarFriend(uid: Long, star: Boolean)

        fun destory()

        fun getItem(position: Int): ContactItemBean
    }

    interface View : BaseView<Presenter> {

        //刷新界面
        fun refreshListUI(list: MutableList<ContactItemBean>)

        //显示错误界面
        fun showError(errStr: String?)
    }
}