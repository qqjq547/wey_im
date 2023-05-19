package framework.telegram.business.ui.login.presenter

import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface FindPasswordFirstContract {

    interface Presenter : BasePresenter {

        fun sendCode(phone:String,countryCode:String)

        fun checkCode(phone:String,countryCode:String,smsCod: String)

    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        fun sendCodeSuccess(str: String)

        fun checkCodeSuccess(phone: String,countryCode: String,smsCod: String)

        fun showErrMsg(str: String?)
    }
}