package framework.telegram.business.ui.me.presenter

import android.content.Intent
import android.net.Uri
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface FeedbackContract {

    interface Presenter : BasePresenter {

        fun setFeedback(type: Int, msg: String,picUrls:String)

        fun clickPickPhoto(taskIndex :Int)

        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

        fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        fun setFeedbackSuccess(msg: String)

        fun showErrMsg(str: String?)

        fun setUri(taskIndex:Int ,url: Uri?)
    }
}