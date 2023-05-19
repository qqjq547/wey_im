package framework.telegram.business.ui.group.presenter

import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.group.GroupInfoModel
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface MyGroupChatsContract {

    interface Presenter : BasePresenter {
        fun updateData(list :MutableList<GroupInfoModel> )
    }

    interface View : BaseView<Presenter> {
        //显示加载中
        fun showLoading()

        //刷新界面
        fun refreshListUI(list: List<GroupInfoModel>)

        //显示加载中
        fun showEmpty()

        //显示错误界面
        fun showError(errStr :String?)


    }
}