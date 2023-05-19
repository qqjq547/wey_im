package framework.telegram.business.ui.qr

import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import cn.bingoogolapple.qrcode.core.QRCodeView
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.support.BaseApp
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.ThreadUtils
import kotlinx.android.synthetic.main.bus_qr_analyze_activity.*

/**
 * 自定义实现的扫描Fragment
 * zbar 对某些本地图片识别效果不好，所以暂时用之前的，
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_QR_ANALYZE)
class QrAnalyzeActivity : AppCompatActivity(), QRCodeView.Delegate {

    private val imagePath by lazy { intent.getStringExtra("image_path") }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (TextUtils.isEmpty(imagePath)) {
            finish()
            return
        }
        setContentView(R.layout.bus_qr_analyze_activity)
        zbarview.setDelegate(this)
    }

    override fun onResume() {
        super.onResume()
        zbarview.decodeQRCode(imagePath)
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onDestroy() {
        super.onDestroy()
        zbarview.onDestroy() // 销毁二维码扫描控件
    }

    override fun onScanQRCodeSuccess(result: String?) {
        if (result == null || TextUtils.isEmpty(result)) {
            BaseApp.app.toast(getString(R.string.identification_of_failure))
            finish()
        } else {
            ArouterServiceManager.qrService.resultHandler(result, this, {}, {
                ThreadUtils.runOnIOThread(1000) {
                    finish()
                    overridePendingTransition(0, 0)
                }
            }) {
                BaseApp.app.toast(it)
                finish()
            }
        }
    }

    override fun onCameraAmbientBrightnessChanged(isDark: Boolean) {
    }

    override fun onScanQRCodeOpenCameraError() {
    }
}