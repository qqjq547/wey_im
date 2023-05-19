package framework.telegram.business.ui.me.presenter

import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface PasswordSetFirstContract {

    interface Presenter : BasePresenter {

        fun sendCode(phone:String,countryCode:String)

        fun checkPassword(phone:String,countryCode:String,smsCod: String)

    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        fun sendCodeSuccess(str: String)

        fun checkPasswordSuccess(password: String)

        fun showErrMsg(str: String?)
    }
}