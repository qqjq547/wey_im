package framework.telegram.business.ui.login.presenter

import android.content.Context
import com.im.domain.pb.CommonProto
import com.im.domain.pb.LoginProto
import com.im.domain.pb.SysProto
import com.trello.rxlifecycle3.android.ActivityEvent
import com.umeng.analytics.MobclickAgent
import framework.telegram.business.BusinessApplication
import framework.telegram.business.R
import framework.telegram.business.UserDHKeysHelper
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.LoginHttpReqCreator
import framework.telegram.business.http.creator.SysHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.LoginHttpProtocol
import framework.telegram.support.BaseApp
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.HexString
import io.reactivex.Observable

class RegisterPresenterImpl : RegisterContract.Presenter {

    private val mContext: Context
    private val mView: RegisterContract.View
    private val mViewObservalbe: Observable<ActivityEvent>
    private var mSendSmsIndex = 0

    private var mLastPhone = ""
    private var mLastCountryCode= ""

    private var mCodeCountTime = 0L

    constructor(view: RegisterContract.View, context: Context, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {

    }

    override fun register(phone: String, smsCode: String, countryCode: String) {
        mView.showLoading()
        try {
            val keyPair = UserDHKeysHelper.newKeyPair()
            HttpManager.getStore(LoginHttpProtocol::class.java)
                    .register(object : HttpReq<LoginProto.RegReq>() {
                        override fun getData(): LoginProto.RegReq {
                            return LoginHttpReqCreator.createRegisterReq(
                                    phone,
                                    smsCode,
                                    countryCode,
                                    HexString.bufferToHex(keyPair.publicKey))
                        }
                    })
                    .getResult(mViewObservalbe, {
                        BusinessApplication.saveAccountInfoByRegister(0, it.user.uid, phone, "", countryCode, it)
                        try {
                            if (UserDHKeysHelper.saveUserKeyPair(it.user.uid.toString(), keyPair, it.keyVersion)) {
                                mView.registerSuccess(mContext.getString(R.string.bus_login_register_success))
                            }
                        } catch (e: Exception) {
                            mView.showErrMsg(e.message)
                            MobclickAgent.reportError(BaseApp.app, "RegisterPresenterImpl--->register->saveUserKeyPair失败   error->>>${e.localizedMessage}")
                        }
                    }, {
                        //请求失败
                        mView.showErrMsg(it.message)
                    })
        } catch (e: Exception) {
            MobclickAgent.reportError(BaseApp.app, "RegisterPresenterImpl--->register->newKeyPair失败   error->>>${e.localizedMessage}")
        }
    }

    override fun sendCode(phone: String, countryCode: String) {
        val curTime =  System.currentTimeMillis()
        if (phone == mLastPhone && countryCode ==mLastCountryCode && curTime- mCodeCountTime  <60*1000){
            mView.sendCodeSuccess("",60- ((curTime- mCodeCountTime)/1000).toInt())
        }else {
            HttpManager.getStore(LoginHttpProtocol::class.java)
                    .getSmsCode(object : HttpReq<SysProto.GetSmsCodeReq>() {
                        override fun getData(): SysProto.GetSmsCodeReq {
                            return SysHttpReqCreator.createGetSmsCodeReq(phone, CommonProto.GetSmsCodeType.REG, countryCode, mSendSmsIndex)
                        }
                    })
                    .getResult(mViewObservalbe, {
                        //请求成功
                        mView.sendCodeSuccess(mContext.getString(R.string.bus_login_sms_code_send),60)
                        mSendSmsIndex++
                    }, {
                        //请求失败
                        mView.showErrMsg(it.message)
                    })
        }
        mLastPhone = phone
        mLastCountryCode = countryCode
    }

    override fun setCountDown(downTime :Long) {
        mCodeCountTime = downTime
    }
}