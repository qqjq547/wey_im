package framework.telegram.business.ui.me.presenter

import com.im.domain.pb.UserProto
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface MeDetailContract {

    interface Presenter : BasePresenter {

        /**
         * 获取账户详细信息
         * @param uid 账户信息AccountInfo的uid
         */
        fun getDetailInfo(uid:Long)
    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        /**
         * 获取账户详细信息成功后，更新账户详情信息并进行是否显示红点的校验
         */
        fun getDetailInfoSuccess(info: UserProto.DetailResp)

        fun showErrMsg(str: String?)
    }
}