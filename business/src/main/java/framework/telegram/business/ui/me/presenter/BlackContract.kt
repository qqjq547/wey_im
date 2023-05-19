package framework.telegram.business.ui.me.presenter

import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface BlackContract {

    interface Presenter : BasePresenter {

        fun destory()

        fun setBlack(isBlack: Boolean,targetUid:Long)
    }

    interface View : BaseView<Presenter> {

        fun showLoading()

        fun showData(list: MutableList<ContactDataModel>)

        fun showErrMsg(str: String?)

        fun showBlackInfo(bfMyBlack: Boolean)
    }
}