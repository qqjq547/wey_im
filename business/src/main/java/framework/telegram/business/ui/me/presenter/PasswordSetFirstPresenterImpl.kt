package framework.telegram.business.ui.me.presenter

import android.content.Context
import com.im.domain.pb.CommonProto
import com.im.domain.pb.SysProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.R
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.SysHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.LoginHttpProtocol
import framework.telegram.business.http.protocol.SystemHttpProtocol
import framework.telegram.support.BaseApp
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.MD5
import io.reactivex.Observable
import yourpet.client.android.sign.NativeLibUtil

class PasswordSetFirstPresenterImpl : PasswordSetFirstContract.Presenter {


    private val mContext: Context
    private val mView: PasswordSetFirstContract.View
    private val mViewObservalbe: Observable<ActivityEvent>
    private var mSendSmsIndex = 0

    constructor(view: PasswordSetFirstContract.View, context: Context, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {

    }

    override fun sendCode(phone: String, countryCode: String) {
        mView.showLoading()
        HttpManager.getStore(LoginHttpProtocol::class.java)
                .getSmsCode(object : HttpReq<SysProto.GetSmsCodeReq>() {
                    override fun getData(): SysProto.GetSmsCodeReq {
                        return SysHttpReqCreator.createGetSmsCodeReq(phone, CommonProto.GetSmsCodeType.VALIDATE_PASSWORD, countryCode,mSendSmsIndex)
                    }
                })
                .getResult(mViewObservalbe, {
                    //请求成功
                    mView.sendCodeSuccess(mContext.getString(R.string.bus_login_sms_code_send))
                    mSendSmsIndex++
                }, {
                    //请求失败
                    mView.showErrMsg(it.message)
                })
    }

    override fun checkPassword(phone: String, countryCode: String, smsCod: String) {
        HttpManager.getStore(LoginHttpProtocol::class.java)
                .checkSmsCode(object : HttpReq<SysProto.CheckSmsCodeReq>() {
                    override fun getData(): SysProto.CheckSmsCodeReq {
                        return SysHttpReqCreator.createCheckSmsCodeReq(countryCode, phone, smsCod, CommonProto.GetSmsCodeType.VALIDATE_PASSWORD)
                    }
                })
                .getResult(mViewObservalbe, {
                    //请求成功
                    mView.checkPasswordSuccess("")
                }, {
                    //请求失败
                    mView.showErrMsg(it.message)
                })
    }

    /**
     * 计算密码
     *
     * @param data
     * @return
     */
    private fun getPassword(data: String): String {
        return try {
            NativeLibUtil.getInstance().sign2(BaseApp.app, false, MD5.md5(data))
        } catch (e: Exception) {
            ""
        }
    }

}