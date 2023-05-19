package framework.telegram.business.ui.contacts.presenter

import com.im.domain.pb.ContactsProto
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface UnAuditContactContract {

    interface Presenter : BasePresenter {
        fun getDataDetail(recordId: Long)

        fun makeFriend(applyUid: Long, op: ContactsProto.ContactsOperator)

        fun setBlack(targetUid: Long, isBlack: Boolean)

    }

    interface View : BaseView<Presenter> {
        //显示加载中
        fun showLoading()

        //刷新界面
        fun refreshUI(info: ContactsProto.ContactsRecordBase)

        //显示加载中
        fun showEmpty()

        fun showSetBlackUI(isBlack: Boolean)

        //显示错误界面
        fun showError(errStr: String?)

        fun destory()
    }
}