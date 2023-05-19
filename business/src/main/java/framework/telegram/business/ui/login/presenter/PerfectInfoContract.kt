package framework.telegram.business.ui.login.presenter

import android.content.Intent
import android.net.Uri
import com.im.domain.pb.UserProto
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface PerfectInfoContract {

    interface Presenter : BasePresenter {

        fun savePerfectInfo(opList: List<UserProto.UserOperator>, userParam: UserProto.UserParam)

        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

        fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)

        fun onDestroy()

        fun clickTakePhoto()

        fun clickPickPhoto()

    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        fun savePerfectInfoSuccess(str: String?)

        fun showErrMsg(str: String?)

        fun setUserHeadUri(url: Uri?)

    }
}