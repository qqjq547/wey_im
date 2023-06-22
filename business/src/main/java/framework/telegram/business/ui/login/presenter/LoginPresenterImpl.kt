package framework.telegram.business.ui.login.presenter

import android.content.Context
import android.text.TextUtils
import android.widget.Toast
import com.im.domain.pb.CommonProto
import com.im.domain.pb.LoginProto
import com.im.domain.pb.SysProto
import com.trello.rxlifecycle3.android.ActivityEvent
import com.umeng.analytics.MobclickAgent
import framework.telegram.business.*
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.Result.ACCOUNT_IS_BANNED
import framework.telegram.business.bridge.Constant.Result.MOBILE_NO_REGISTER
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
import framework.telegram.support.tools.DeviceUtils
import framework.telegram.support.tools.HexString
import framework.telegram.support.tools.MD5
import io.reactivex.Observable
import yourpet.client.android.sign.NativeLibUtil


class LoginPresenterImpl : LoginContract.Presenter {

    private val mContext: Context
    private val mView: LoginContract.View
    private val mViewObservalbe: Observable<ActivityEvent>
    private var mSendSmsIndex = 0

    private var mLastPhone = ""
    private var mLastCountryCode= ""

    private var mCodeCountTime = 0L

    constructor(view: LoginContract.View, context: Context, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {

    }

    override fun loginByPassword(phone: String, password: String, countryCode: String) {
        mView.showLoading()
        val realPassword = if (TextUtils.isEmpty(password)) "" else getPassword(password)
        HttpManager.getStore(LoginHttpProtocol::class.java)
            .login(object : HttpReq<LoginProto.LoginReq>() {
                override fun getData(): LoginProto.LoginReq {
                    return LoginHttpReqCreator.createLoginByPasswordReq(phone, realPassword, countryCode)
                }
            })
            .getResult(mViewObservalbe, {
                SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()).putLastManualLoginTime(it.disableTime)
                BusinessApplication.saveAccountInfoByLogin(1, it.user.uid, phone, realPassword, password, countryCode, it)
                mView.loginSuccess(mContext.getString(R.string.bus_login_login_success))
            }, {
                //请求失败
                if (it is HttpException) {
                    when {
                        it.errCode == Constant.Result.ACCOUNT_IS_CANCEL -> {
                            ArouterServiceManager.messageService.deleteAccountData(it.flag.toLong())
                            BaseApp.app.onUserLogout(BaseApp.app.getString(R.string.account_unregistration))
                        }
                        it.errCode == ACCOUNT_IS_BANNED -> mView.showErrMsg(it.message, true)
                        else -> mView.showErrMsg(it.message, false)
                    }
                } else {
                    mView.showErrMsg(it.message, false)
                }
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
                        it.errCode == MOBILE_NO_REGISTER -> register(phone, smsCode, countryCode)
                        it.errCode == Constant.Result.ACCOUNT_IS_CANCEL -> {
                            ArouterServiceManager.messageService.deleteAccountData(it.flag.toLong())
                            BaseApp.app.onUserLogout("")
                            register(phone, smsCode, countryCode)
                        }
                        it.errCode == ACCOUNT_IS_BANNED -> mView.showErrMsg(it.message, true)
                        else -> mView.showErrMsg(it.message, false)
                    }
                } else {
                    mView.showErrMsg(it.message, false)
                }
            })
    }

    override fun sendCode(phone: String, countryCode: String) {

        if(DeviceUtils.isEmulator()){
            mView.sendCodeSuccess(mContext.getString(R.string.bus_login_sms_code_send),60)
        }else{
            val curTime =  System.currentTimeMillis()
            if (phone == mLastPhone && countryCode ==mLastCountryCode && curTime- mCodeCountTime  <60*1000){
                mView.sendCodeSuccess("",60- ((curTime- mCodeCountTime)/1000).toInt())
            }else{
                HttpManager.getStore(LoginHttpProtocol::class.java)
                    .getSmsCode(object : HttpReq<SysProto.GetSmsCodeReq>() {
                        override fun getData(): SysProto.GetSmsCodeReq {
                            return SysHttpReqCreator.createGetSmsCodeReq(phone, CommonProto.GetSmsCodeType.LOGIN, countryCode, mSendSmsIndex)
                        }
                    })
                    .getResult(mViewObservalbe, {
                        //请求成功
                        mView.sendCodeSuccess(mContext.getString(R.string.bus_login_sms_code_send),60)
                        mSendSmsIndex++
                    }, {
                        //请求失败
                        mView.showErrMsg(it.message, false)
                    })
            }
            mLastPhone = phone
            mLastCountryCode = countryCode
        }
    }

    override fun register(phone: String, smsCode: String, countryCode: String) {
        mView.showLoading()
        try {
            val keyPair = UserDHKeysHelper.newKeyPair()
            HttpManager.getStore(LoginHttpProtocol::class.java)
                .register(object : HttpReq<LoginProto.RegReq>() {
                    override fun getData(): LoginProto.RegReq {
                        return LoginHttpReqCreator.createRegisterReq(
                            0,
                            "",
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
                        mView.showErrMsg(e.message, false)
                        MobclickAgent.reportError(BaseApp.app, "LoginPresenterImpl--->register->saveUserKeyPair失败   error->>>${e.localizedMessage}")
                    }
                }, {
                    //请求失败
                    mView.showErrMsg(it.message, false)
                })
        } catch (e: Exception) {
            MobclickAgent.reportError(BaseApp.app, "LoginPresenterImpl--->register->newKeyPair失败   error->>>${e.localizedMessage}")
        }
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

    override fun setCountDown(downTime :Long) {
        mCodeCountTime = downTime
    }
}