package framework.telegram.business.ui.group.presenter

import com.im.domain.pb.GroupProto
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.group.GroupInfoModel
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface SameGroupChatsContract {

    interface Presenter : BasePresenter {
        fun getDataList()

        fun getFirstDataList()
    }

    interface View : BaseView<Presenter> {
        //显示加载中
        fun showLoading()

        //刷新界面
        fun refreshListUI(list: List<GroupProto.GroupBase>,isMore:Boolean)

        //显示加载中
        fun showEmpty()

        //显示错误界面
        fun showError(errStr :String?)

    }
}