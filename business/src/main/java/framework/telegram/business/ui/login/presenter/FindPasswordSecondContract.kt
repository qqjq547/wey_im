package framework.telegram.business.ui.login.presenter

import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface FindPasswordSecondContract {

    interface Presenter : BasePresenter {

        fun sure(phone: String, countryCode: String,smsCode:String,password:String)

    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        fun success(str: String)


        fun showErrMsg(str: String?)
    }
}