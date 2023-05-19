package framework.telegram.business.ui.me.presenter

import com.im.domain.pb.SysProto
import com.im.domain.pb.UserProto
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface InfoContract {

    interface Presenter : BasePresenter {

        /**
         * 保存完善账户信息
         * @param info
         * @param opList
         * @param userParam 需要调用特殊方式来生成
         *
         */
        fun savePerfectInfo(info:String,opList: List<UserProto.UserOperator>, userParam: UserProto.UserParam)

        fun logout()
    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        /**
         * 保存完善账户信息成功
         */
        fun savePerfectInfoSuccess(str: String)

        fun showErrMsg(str: String?)

    }
}