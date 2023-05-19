package framework.telegram.business.ui.login.presenter

import android.content.Context
import com.im.domain.pb.CommonProto
import com.im.domain.pb.LoginProto
import com.im.domain.pb.SysProto
import com.trello.rxlifecycle3.android.ActivityEvent
import com.umeng.analytics.MobclickAgent
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.BusinessApplication
import framework.telegram.business.R
import framework.telegram.business.UserDHKeysHelper
import framework.telegram.business.bridge.Constant
import framework.telegram.business.http.HttpException
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.LoginHttpReqCreator
import framework.telegram.business.http.creator.SysHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.LoginHttpProtocol
import framework.telegram.business.sp.CommonPref
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.HexString
import io.reactivex.Observable

class SmsCodePresenterImpl : SmsCodeContract.Presenter {
    private val mContext: Context
    private val mView: SmsCodeContract.View
    private val mViewObservalbe: Observable<ActivityEvent>
    private var mSendSmsIndex = 0

    constructor(view: SmsCodeContract.View, context: Context, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {

    }

    override fun sendCode(phone: String, countryCode: String, type: Int) {
        if (type == 0)
            sendLoginCode(phone, countryCode)
        else
            sendRegisterCode(phone, countryCode)
    }

    override fun sendLoginCode(phone: String, countryCode: String) {
        HttpManager.getStore(LoginHttpProtocol::class.java)
                .getSmsCode(object : HttpReq<SysProto.GetSmsCodeReq>() {
                    override fun getData(): SysProto.GetSmsCodeReq {
                        return SysHttpReqCreator.createGetSmsCodeReq(phone, CommonProto.GetSmsCodeType.LOGIN, countryCode, mSendSmsIndex)
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

    override fun sendRegisterCode(phone: String, countryCode: String) {
        HttpManager.getStore(LoginHttpProtocol::class.java)
                .getSmsCode(object : HttpReq<SysProto.GetSmsCodeReq>() {
                    override fun getData(): SysProto.GetSmsCodeReq {
                        return SysHttpReqCreator.createGetSmsCodeReq(phone, CommonProto.GetSmsCodeType.REG, countryCode, mSendSmsIndex)
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

    override fun loginBySmsCode(phone: String, smsCode: String, countryCode: String) {
        mView.showLoading()
        HttpManager.getStore(LoginHttpProtocol::class.java)
                .login(object : HttpReq<LoginProto.LoginReq>() {
                    override fun getData(): LoginProto.LoginReq {
                        return LoginHttpReqCreator.createLoginBySmdCodeReq(phone, smsCode, countryCode)
                    }
                })
                .getResult(mViewObservalbe, {
                    SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()).putLastManualLoginTime(it.disableTime)
                    BusinessApplication.saveAccountInfoByLogin(0, it.user.uid, phone, "", "", countryCode, it)
                    mView.loginSuccess(mContext.getString(R.string.bus_login_login_success))
                }, {
                    //请求失败
                    if (it is HttpException) {
                        when {
                            it.errCode == Constant.Result.MOBILE_NO_REGISTER -> register(phone, smsCode, countryCode)
                            it.errCode == Constant.Result.ACCOUNT_IS_CANCEL -> {
                                ArouterServiceManager.messageService.deleteAccountData(it.flag.toLong())
                                BaseApp.app.onUserLogout("")
                                register(phone, smsCode, countryCode)
                            }
                            else -> mView.showErrMsg(it.message)
                        }
                    } else {
                        mView.showErrMsg(it.message)
                    }
                })
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
                            MobclickAgent.reportError(BaseApp.app, "SmsCodePresenterImpl--->register->saveUserKeyPair失败   error->>>${e.localizedMessage}")
                        }
                    }, {
                        //请求失败
                        mView.showErrMsg(it.message)
                    })
        } catch (e: Exception) {
            MobclickAgent.reportError(BaseApp.app, "SmsCodePresenterImpl--->register->newKeyPair失败   error->>>${e.localizedMessage}")
        }
    }
}