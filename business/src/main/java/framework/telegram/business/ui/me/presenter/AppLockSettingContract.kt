package framework.telegram.business.ui.me.presenter

import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface AppLockSettingContract {

    interface Presenter : BasePresenter

    interface View : BaseView<Presenter> {
        fun showLoading()

        fun showErrMsg(str: String?)
    }
}