package framework.telegram.business.ui.qr

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_QR_SCAN
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.qr.presenter.GroupQrContract
import framework.telegram.business.ui.qr.presenter.GroupQrPresenterImpl
import framework.telegram.business.utils.TimeUtil
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
import kotlinx.android.synthetic.main.bus_me_activity_group_qr.*
import java.io.File

/**
 * Created by lzh on 19-6-7.
 * INFO: 群的二维码
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_QR_GROUP_SWEEP)
class GroupQrActivity : BaseBusinessActivity<GroupQrContract.Presenter>(), GroupQrContract.View {
    companion object {
        const val CHAT_GROUP_ID = "CHAT_GROUP_ID"
        const val CHAT_GROUP_NAME = "CHAT_GROUP_NAME"
        const val CHAT_GROUP_ICON = "CHAT_GROUP_ICON"
        const val CHAT_GROUP_OWNER = "CHAT_GROUP_OWNER"
        const val QRCODE_REQUEST_CODE = 0x1999
        const val GET_PERMISSIONS_REQUEST_CODE = 124
    }

    private var groupId = 0L
    private var groupName = ""
    private var groupIcon = ""
    private var groupOwner = false

    override fun getLayoutId() = R.layout.bus_me_activity_group_qr

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        setLight(getMaxLight())
    }

    private fun getMaxLight(): Float {
        return if (Build.VERSION.SDK_INT >= 28) {
            try {
                Math.max(
                    0.0f, resources.getInteger(
                        resources
                            .getIdentifier(
                                "config_screenBrightnessSettingMaximum",
                                "integer",
                                "android"
                            )
                    ).toFloat()
                )
            } catch (e: Exception) {
                255.0f
            }
        } else {
            255.0f
        }
    }

    private fun setLight(brightness: Float) {
        if (brightness <= 0f) {
            return
        }

        //设置新的亮度
        val lp = window.attributes
        lp.screenBrightness = brightness * (1f / getMaxLight())
        window.attributes = lp
    }

    override fun initListen() {


        if (groupOwner) {
            textview_1.visibility = View.VISIBLE
        }

        textview_1.setOnClickListener {
            AppDialog.show(this@GroupQrActivity, this@GroupQrActivity) {
                positiveButton(text = getString(R.string.confirm), click = {
                    mPresenter?.getGroupQr(groupId, true)
                })
                negativeButton(text = getString(R.string.cancel))
                message(text = getString(R.string.group_qr_update))
            }
        }

        textview_2.setOnClickListener {
            saveQrFile()
        }
    }

    private fun saveQrFile() {
        val myQrFile = File(
            DirManager.getImageFileDir(BaseApp.app, AccountManager.getLoginAccountUUid()),
            "qr_group_${groupId}_${System.currentTimeMillis()}.jpg"
        )
        val bitmap = SafeBitmapUtils.makeViewBitmap(
            relative_layout,
            relative_layout.width,
            relative_layout.height
        )
        if (bitmap != null) {
            if (SafeBitmapUtils.saveBitmap(bitmap, myQrFile)) {
                FileHelper.insertImageToGallery(this@GroupQrActivity, myQrFile)
                toast(getString(R.string.save_success))
            } else {
                toast(getString(R.string.save_fail))
            }
        }
    }

    override fun initData() {
        groupId = intent.getLongExtra(CHAT_GROUP_ID, 0L)
        groupName = intent.getStringExtra(CHAT_GROUP_NAME) ?: ""
        groupIcon = intent.getStringExtra(CHAT_GROUP_ICON) ?: ""
        groupOwner = intent.getBooleanExtra(CHAT_GROUP_OWNER, false)

        custom_toolbar.showCenterTitle(getString(R.string.group_code))

        if (groupId == 0L)
            finish()
        GroupQrPresenterImpl(this, this, lifecycle()).start()
        mPresenter?.getGroupQr(groupId, false)
    }

    override fun showLoading() {
    }

    override fun getGroupQrSuccess(qrUrl: String, outTime: Long) {
        if (TextUtils.isEmpty(qrUrl)) {
            finish()
        }

        image_view_icon.setImageURI(groupIcon)
        app_text_view_name.text = groupName
        val bitmap = CodeUtils.createImage(qrUrl, 400, 400, null)
        app_compat_image_view.setImageBitmap(bitmap)
        val time = TimeUtil.timeFormat1(outTime)

        text1.text = String.format(getString(R.string.qr_code_on_not_valid_before), time)
    }

    override fun showError(errStr: String?) {
        toast(errStr.toString())
        finish()
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as GroupQrContract.Presenter
    }

    override fun isActive(): Boolean = true


    @SuppressLint("CheckResult")
    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this@GroupQrActivity?.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.CAMERA),
                    GET_PERMISSIONS_REQUEST_CODE
                )
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            GET_PERMISSIONS_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (this@GroupQrActivity?.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        toast(getString(R.string.no_permissions_were_obtained))
                    } else {
                        ARouter.getInstance().build(ROUNTE_BUS_QR_SCAN)
                            .withString(Intents.Scan.SCAN_FORMATS, "QR_CODE")
                            .navigation(this@GroupQrActivity, QRCODE_REQUEST_CODE)
                        // 到MainActivity中解析返回的数据
                    }
                }
            }
        }
    }

}