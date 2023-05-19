package framework.telegram.business.ui.me.presenter

import com.im.domain.pb.CommonProto
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface PrivacyDisShowOnlineContract {

    interface Presenter : BasePresenter {

        fun savePerfectInfo(viewType: CommonProto.LastOnlineTimeViewType)
    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        fun savePerfectInfoSuccess()

        fun showErrMsg(str: String?)
    }
}