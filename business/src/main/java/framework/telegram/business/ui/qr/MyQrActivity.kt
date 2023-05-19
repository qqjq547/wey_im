package framework.telegram.business.ui.qr

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_QR_SCAN
import framework.telegram.business.bridge.Constant.Protocol.PROTOCOL_QRCODE
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.FileHelper
import framework.telegram.support.tools.SafeBitmapUtils
import framework.telegram.support.tools.file.DirManager
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.qr.activity.CodeUtils
import framework.telegram.ui.qr.decoding.Intents
import kotlinx.android.synthetic.main.bus_me_activity_my_qr.*
import java.io.File


/**
 * Created by lzh on 19-6-7.
 * INFO:
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_QR_SWEEP)
class MyQrActivity : BaseBusinessActivity<BasePresenter>() {

    companion object {
        const val QRCODE_REQUEST_CODE = 0x1999
        const val GET_PERMISSIONS_REQUEST_CODE = 124
    }

    val accountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    override fun getLayoutId() = R.layout.bus_me_activity_my_qr

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        image_view_icon.setImageURI(accountInfo.getAvatar())
        app_text_view_name.text = accountInfo.getNickName()
        val bitmap = CodeUtils.createImage( accountInfo.getQrCode(), 400, 400, null)
        app_compat_image_view.setImageBitmap(bitmap)
        text1.text = getString(R.string.open_68_add_me)
        custom_toolbar.showCenterTitle(getString(R.string.my_qr_code))

        setMaxLight()
    }

    private fun setMaxLight() {
        //设置当前窗口的亮度，退出这个activity无效
        val lp = window.attributes
        lp.screenBrightness = 1f//1=100%亮度
        window.attributes = lp
    }

    override fun initListen() {
        textview_1.setOnClickListener {
            if (checkPermission()) {
                ARouter.getInstance().build(ROUNTE_BUS_QR_SCAN)
                        .withString(Intents.Scan.SCAN_FORMATS, "QR_CODE")
                        .navigation(this@MyQrActivity, QRCODE_REQUEST_CODE)
                // 到MainActivity中解析返回的数据
            }
        }

        textview_2.setOnClickListener {
            val myQrFile = File(DirManager.getImageFileDir(BaseApp.app, AccountManager.getLoginAccountUUid()), "qr_my_${System.currentTimeMillis()}.jpg")
            val bitmap = SafeBitmapUtils.makeViewBitmap(relative_layout, relative_layout.width, relative_layout.height)
            if (bitmap != null) {
                if (SafeBitmapUtils.saveBitmap(bitmap, myQrFile)) {
                    FileHelper.insertImageToGallery(this@MyQrActivity, myQrFile)
                    toast(getString(R.string.save_success))
                } else {
                    toast(getString(R.string.save_fail))
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this@MyQrActivity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), GET_PERMISSIONS_REQUEST_CODE)
                return false
            }
        }
        return true
    }

    override fun initData() {

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            GET_PERMISSIONS_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (this@MyQrActivity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        toast(getString(R.string.no_permissions_were_obtained))
                    } else {
                        ARouter.getInstance().build(ROUNTE_BUS_QR_SCAN)
                                .withString(Intents.Scan.SCAN_FORMATS, "QR_CODE")
                                .navigation(this@MyQrActivity, QRCODE_REQUEST_CODE)
                        // 到MainActivity中解析返回的数据
                    }
                }
            }
        }
    }

}