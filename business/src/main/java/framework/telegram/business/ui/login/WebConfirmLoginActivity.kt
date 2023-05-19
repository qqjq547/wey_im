package framework.telegram.business.ui.login

import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.im.domain.pb.CommonProto
import com.im.domain.pb.LoginProto
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.http.HttpException
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.LoginHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.UserHttpProtocol
import framework.telegram.support.BaseActivity
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.ExpandClass.toast
import kotlinx.android.synthetic.main.bus_web_confirm_login_activity.*

/**
 * Created by yanggl on 2019/10/22 14:59
 */
@Route(path = Constant.ARouter.ROUNTE_MSG_WEB_CONFIRM_LOGIN)
class WebConfirmLoginActivity : BaseActivity() {

    private val mDefaultQrCode by lazy {
        intent.getStringExtra(Constant.ARouter_Key.KEY_QRCODE) ?: ""
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bus_web_confirm_login_activity)

        custom_toolbar.setToolbarColor(R.color.white)
        custom_toolbar.setBackIcon(R.drawable.close_blue) {
            cancelScanQrcodeLogin()
        }

        btn_login.setOnClickListener {
            //登陆桌面端
            HttpManager.getStore(UserHttpProtocol::class.java)
                .webLoginByQrCode(object : HttpReq<LoginProto.WebLoginByQrCodeReq>() {
                    override fun getData(): LoginProto.WebLoginByQrCodeReq {
                        return LoginHttpReqCreator.createWebLoginByQrCodeReq(
                            mDefaultQrCode,
                            CommonProto.WebLoginStatus.ALREADY_LOGIN
                        )
                    }
                })
                .getResult(null, {
                    //请求成功
                    finish()

                    ArouterServiceManager.messageService.setFileTransferUserChatIsTop()
                }, {
                    //请求失败
                    if (it is HttpException) {
                        if (it.errCode == 10003) {
                            toast(it.message.toString())
                            finish()
                        }
                    } else {
                        toast(getString(R.string.web_login_fail_by_code))
                    }
                })
        }
        scanQrcodeSuccess()
    }

    private fun cancelScanQrcodeLogin() {
        HttpManager.getStore(UserHttpProtocol::class.java)
            .webLoginByQrCode(object : HttpReq<LoginProto.WebLoginByQrCodeReq>() {
                override fun getData(): LoginProto.WebLoginByQrCodeReq {
                    return LoginHttpReqCreator.createWebLoginByQrCodeReq(
                        mDefaultQrCode,
                        CommonProto.WebLoginStatus.CANCEL_LOGIN
                    )
                }
            })
            .getResult(null, {
                //请求成功
            }, {
                //请求失败
            })
        finish()
    }

    private fun scanQrcodeSuccess() {
        HttpManager.getStore(UserHttpProtocol::class.java)
            .webLoginByQrCode(object : HttpReq<LoginProto.WebLoginByQrCodeReq>() {
                override fun getData(): LoginProto.WebLoginByQrCodeReq {
                    return LoginHttpReqCreator.createWebLoginByQrCodeReq(
                        mDefaultQrCode,
                        CommonProto.WebLoginStatus.SCANNED
                    )
                }
            })
            .getResult(null, {
                //请求成功
            }, {
                //请求失败
            })
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.anim_down_out)
    }

    override fun isPortraitScreen(): Boolean = false //这里不设置竖屏，因为windowIsTranslucent 的问题，这里其实还是竖屏
}