package framework.telegram.business.ui.me.presenter

import android.content.Intent
import android.net.Uri
import com.im.domain.pb.CommonProto
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface MeInfoContract {

    interface Presenter : BasePresenter {

        /**
         * 更新性别
         * sex 跟 gender 存在序号差别
         */
        fun saveSexInfo(sex: Int, name :String,gender: CommonProto.Gender)

        /**
         * 监听跳转回来的Intent，然后分发跳转结果
         */
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

        /**
         * 请求权限
         * 需判断当前 android版本 >= Build.VERSION_CODES.M ?
         */
        fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)

        fun onDestroy()

        /**
         * 拍照
         */
        fun clickTakePhoto()

        /**
         * 选择图片
         */
        fun clickPickPhoto()
    }

    interface View : BaseView<Presenter> {
        fun showLoading()

        /**
         * 上传图片成功
         * 然后发送账户信息更新通知
         */
        fun savePicInfoSuccess(str: String)

        /**
         * 更新性别信息成功
         * 然后发送账户信息更新通知
         */
        fun saveSexSuccess(info: Int,name :String)

        fun showErrMsg(str: String?)

    }
}