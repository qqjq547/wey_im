package framework.telegram.business.ui.me.presenter

import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface PasswordSetSecondContract {

    interface Presenter : BasePresenter {

        fun setPassword(password: String)

    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        fun setPasswordSuccess(password: String)

        fun showErrMsg(str: String?)
    }
}