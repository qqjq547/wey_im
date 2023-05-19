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
import framework.telegram.support.system.network.http.HttpReq
import io.reactivex.Observable

class ForgetAppLockPresenterImpl : ForgetAppLockContract.Presenter {

    private val mContext: Context
    private val mView: ForgetAppLockContract.View
    private val mViewObservalbe: Observable<ActivityEvent>
    private var mSendSmsIndex = 0

    constructor(view: ForgetAppLockContract.View, context: Context, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {

    }

    override fun sendCode(phone: String, countryCode: String) {
        HttpManager.getStore(LoginHttpProtocol::class.java)
                .getSmsCode(object : HttpReq<SysProto.GetSmsCodeReq>() {
                    override fun getData(): SysProto.GetSmsCodeReq {
                        return SysHttpReqCreator.createGetSmsCodeReq(phone, CommonProto.GetSmsCodeType.VALIDATE_PHONE, countryCode,mSendSmsIndex)
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

    override fun checkCode(phone: String, countryCode: String, smsCod: String) {
        mView.showLoading()
        HttpManager.getStore(LoginHttpProtocol::class.java)
                .checkSmsCode(object : HttpReq<SysProto.CheckSmsCodeReq>() {
                    override fun getData(): SysProto.CheckSmsCodeReq {
                        return SysHttpReqCreator.createCheckSmsCodeReq(countryCode, phone, smsCod, CommonProto.GetSmsCodeType.VALIDATE_PHONE)
                    }
                })
                .getResult(mViewObservalbe, {
                    //请求成功
                    mView.checkCodeSuccess("")
                }, {
                    //请求失败
                    mView.showErrMsg(it.message)
                })
    }
}