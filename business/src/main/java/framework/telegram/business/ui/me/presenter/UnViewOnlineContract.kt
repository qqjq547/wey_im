package framework.telegram.business.ui.me.presenter

import com.im.domain.pb.CommonProto
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface UnViewOnlineContract {

    interface Presenter : BasePresenter {

        fun destory()
    }

    interface View : BaseView<Presenter> {

        fun showLoading()

        fun showData(list: MutableList<ContactDataModel>)

        fun showEmpty()

        fun showErrMsg(str: String?)

    }
}