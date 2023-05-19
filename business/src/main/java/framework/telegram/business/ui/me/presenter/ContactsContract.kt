package framework.telegram.business.ui.me.presenter

import android.content.Intent
import framework.telegram.business.ui.me.bean.ContactsBean
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView
import framework.telegram.ui.widget.indicator.TitlePageIndicator

interface ContactsContract {

    interface Presenter : BasePresenter{

        /**
         * 获取系统的联系人
         */
        fun getAllContacts()

        /**
         * 选择联系人，然后进行跳转到发短信页面
         */
        fun sendSMS()

        /**
         * 监听跳转回来的Intent，然后分发跳转结果
         */
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

        /**
         * 权限的校验结果
         */
        fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)


        /**
         * 校验是否拥有打开联系人目录和跳转到短信页面的权限
         */
        fun checkSysContactsAndSMSPermission() : Boolean

        fun getItem(position: Int): ContactsBean
    }

    interface View : BaseView<BasePresenter>{

        fun showLoading()

        fun showFinish()

        /**
         * 更新数据
         */
        fun refreshUI(newData: MutableList<ContactsBean>)
    }


}