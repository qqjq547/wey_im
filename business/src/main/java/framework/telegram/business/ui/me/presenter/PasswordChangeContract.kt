package framework.telegram.business.ui.me.presenter

import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface PasswordChangeContract {

    interface Presenter : BasePresenter {

        fun sendCode(phone:String,countryCode:String)

        fun updataPasswordByPassword(oldPassword: String, password: String)

        fun updataPasswordBySmsCode(countryCode: String,phone: String,smsCode: String, password: String)

    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        fun sendCodeSuccess(str: String)

        fun updataPasswordByPasswordSuccess(password: String)

        fun updataPasswordBySmsCode(countryCode: String,phone: String,smsCode: String, password: String)

        fun showErrMsg(str: String?)
    }
}