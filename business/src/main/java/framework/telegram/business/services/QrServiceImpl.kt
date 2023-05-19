package framework.telegram.business.services

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.service.IQrService
import framework.telegram.business.ui.qr.ScanQrActivity

@Route(path = Constant.ARouter.ROUNTE_SERVICE_QR, name = "设置服务")
class QrServiceImpl : IQrService {

    override fun resultHandler(url: String, context: Context?,loading :((Boolean)->Unit), complete :(()->Unit), error: (String) -> Unit) {
        ScanQrActivity.analyzeUrl(url, context,loading,complete, error)
    }

    override fun init(context: Context?) {

    }

}