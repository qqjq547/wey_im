package framework.telegram.business.ui.me.presenter

import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface ForgetAppLockContract {

    interface Presenter : BasePresenter {

        fun sendCode(phone: String, countryCode: String)

        fun checkCode(phone: String, countryCode: String, smsCod: String)
    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        fun sendCodeSuccess(str: String)

        fun checkCodeSuccess(str: String)

        fun showErrMsg(str: String?)
    }
}