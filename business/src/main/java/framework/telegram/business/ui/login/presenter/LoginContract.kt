package framework.telegram.business.ui.login.presenter

import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface LoginContract {

    interface Presenter : BasePresenter {
        fun loginByPassword(phone:String, password:String ,countryCode :String)

        fun loginBySmsCode(phone:String, smsCode:String ,countryCode :String)

        fun sendCode(phone:String,countryCode:String)

        fun register(phone: String, smsCode: String, countryCode: String)

        fun setCountDown(downTime:Long)
    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        fun loginSuccess(str: String?)

        fun loginSuccessToPerfect(str: String?)

        fun sendCodeSuccess(str: String?,time:Int)

        fun showErrMsg(str: String?,showDialog:Boolean)

        fun registerSuccess(str: String?)
    }
}