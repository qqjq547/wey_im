package framework.telegram.business.ui.qr.presenter

import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface GroupQrContract {

    interface Presenter : BasePresenter {
        fun getGroupQr(groupId: Long, isReset: Boolean)
    }

    interface View : BaseView<Presenter> {
        //显示加载中
        fun showLoading()

        //刷新界面
        fun getGroupQrSuccess(qrUrl: String, outTime: Long)

        //显示错误界面
        fun showError(errStr: String?)


    }
}