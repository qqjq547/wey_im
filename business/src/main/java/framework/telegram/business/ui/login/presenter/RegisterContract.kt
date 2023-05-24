package framework.telegram.business.ui.login.presenter

import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface RegisterContract {

    interface Presenter : BasePresenter {
        fun register(phone: String, smsCode: String, countryCode: String)

        fun registerByPwd(countryCode: String,phone: String, pwd:String)

        fun sendCode(phone: String,countryCode:String)

        fun setCountDown(downTime:Long)
    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        fun registerSuccess(str: String?)

        fun sendCodeSuccess(str: String?,time:Int)

        fun showErrMsg(str: String?)
    }
}