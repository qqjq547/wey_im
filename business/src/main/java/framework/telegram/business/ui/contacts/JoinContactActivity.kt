package framework.telegram.business.ui.contacts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.telegram.business.bridge.Constant
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_QR_SCAN
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.qr.ScanQrActivity
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.qr.decoding.Intents
import kotlinx.android.synthetic.main.bus_contacts_activity_add_friend.*

/**
 * Created by lzh on 19-5-27.
 * INFO:新朋友
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_CONTACTS_ADD_FRIEND)
class JoinContactActivity : BaseBusinessActivity<BasePresenter>(){

    override fun getLayoutId() = R.layout.bus_contacts_activity_add_friend

    companion object {
        const val QRCODE_REQUEST_CODE =  0x1999
        const val GET_PERMISSIONS_REQUEST_CODE = 123
    }

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        custom_toolbar.showCenterTitle(getString(R.string.common_contacts_make_friend))
    }

    override fun initListen() {
        relative_layout_search.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACTS_SEARCH_FRIEND).navigation()
        }

        relative_layout_add.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACTS_PHONE_CONTACTS).navigation()
        }

        relative_layout_add_friend.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_ME_PHONE_CONTACTS).navigation()
        }

        relative_layout_scan.setOnClickListener {
            if (checkPermission()) {
                ARouter.getInstance().build(ROUNTE_BUS_QR_SCAN)
                        .withString(Intents.Scan.SCAN_FORMATS, "QR_CODE")
                        .navigation(this, ContactsFragment.QRCODE_REQUEST_CODE)
            }
        }
    }

    override fun initData() {

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            GET_PERMISSIONS_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        toast(getString(R.string.no_permissions_were_obtained))
                    } else {
                        ARouter.getInstance().build(ROUNTE_BUS_QR_SCAN)
                                .withString(Intents.Scan.SCAN_FORMATS, "QR_CODE")
                                .navigation(this, QRCODE_REQUEST_CODE)
                    }
                }
            }
        }
    }


    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), GET_PERMISSIONS_REQUEST_CODE)
                return false
            }
        }
        return true
    }
}