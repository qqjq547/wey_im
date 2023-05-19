package framework.telegram.business.ui.me.presenter

import com.im.domain.pb.CommonProto
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface PrivacyContract {

    interface Presenter : BasePresenter {

        fun savePerfectInfo(index: Int, pos: Int, value: Boolean,callBack:(()->Unit)? = null)

        fun saveClearAccountTime(index: Int, callBack:((Int)->Unit)? = null)
    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        fun savePerfectInfoSuccess(index: Int, pos: Int, value: Boolean)

        fun showErrMsg(str: String?)
    }
}