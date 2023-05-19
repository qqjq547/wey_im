package framework.telegram.business.ui.contacts.presenter

import android.content.Intent
import android.net.Uri
import com.im.domain.pb.UserProto
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface ComplainEditContract {

    interface Presenter : BasePresenter {

        fun submitComplain(reportStr:String,mTargetUId :Long, mType:Int,reportType:Int ,picture:String)

        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

        fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)

        fun onDestroy()

        fun clickTakePhoto(taskIndex :Int)

        fun clickPickPhoto(taskIndex :Int)
    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        fun submitComplainSuccess(str: String?)

        fun showErrMsg(str: String?)

        fun setUri(taskIndex:Int ,url: Uri?)

    }
}