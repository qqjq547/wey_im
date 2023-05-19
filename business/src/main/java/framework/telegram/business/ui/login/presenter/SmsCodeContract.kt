package framework.telegram.business.ui.login.presenter

import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface SmsCodeContract {

    interface Presenter: BasePresenter{

        fun sendCode(phone: String, countryCode: String, type: Int)

        fun sendLoginCode(phone:String,countryCode:String)

        fun loginBySmsCode(phone:String, smsCode:String ,countryCode :String)

        fun sendRegisterCode(phone: String,countryCode: String)

        fun register(phone: String, smsCode: String, countryCode: String)
    }

    interface View: BaseView<Presenter>{
        fun showLoading()

        fun sendCodeSuccess(str: String?)

        fun loginSuccess(str: String?)

        fun registerSuccess(str: String?)

        fun showErrMsg(str: String?)
    }
}