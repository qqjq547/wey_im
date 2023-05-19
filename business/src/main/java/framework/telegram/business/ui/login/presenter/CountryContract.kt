package framework.telegram.business.ui.login.presenter

import framework.telegram.business.bean.CountryCodeInfoBean
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface CountryContract {

    interface Presenter : BasePresenter {
        fun getCountryList()

    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        fun getListSuccess(str: List<CountryCodeInfoBean> ?)

        fun showErrMsg(str: String?)
    }
}