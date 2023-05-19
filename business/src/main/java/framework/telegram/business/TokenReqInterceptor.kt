package framework.telegram.business

import android.util.Log
import com.im.domain.pb.CommonProto
import com.im.domain.pb.LoginProto
import framework.ideas.common.rlog.RLogManager
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.Result.ACCOUNT_LOGIN_DISABLE
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.LoginHttpReqCreator
import framework.telegram.business.http.protocol.LoginHttpProtocol
import framework.telegram.business.sp.CommonPref
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.ThreadUtils
import retrofit2.Call
import java.util.concurrent.locks.ReentrantLock

object TokenReqInterceptor {

    private val refreshTokenLock = ReentrantLock()

    private var lastRefreshTokenTime = 0L

    private var refreshTokenFailCount = 0 // 如果失败超过5次，就跳转到登录界面要求重登//再后台跟换业务接口的域名时会出现

    fun intercept(errCode: Int, oldToken: String): Boolean {
        try {
            AppLogcat.logger.d("TokenReqInterceptor", "竞争锁->$oldToken")
            //如果刷新token锁被锁,则等待锁释放
            refreshTokenLock.lock()

            if (!AccountManager.hasLoginAccount()) {
                // 当前没有已登录的账号了
                return false
            }

            val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
            val newToken = accountInfo.getSessionId()
            AppLogcat.logger.d("TokenReqInterceptor", "竞争锁成功oldToken->$oldToken   newToken->$newToken")
            if (oldToken != newToken) {
                AppLogcat.logger.d("TokenReqInterceptor", "token已经更新->")
                //重新发起请求
                return true
            } else {
                if (System.currentTimeMillis() - lastRefreshTokenTime < 60000) {
                    return false
                } else {
                    when (errCode) {
                        Constant.Result.NO_PERMISSION,//无权限
                        Constant.Result.ACCOUNT_TOKEN_ERROR -> { //token过期
                            val refreshResult = refreshToken(accountInfo)
                            if (refreshResult == null) {
                                //更新token成功，重新请求
                                //重新发起请求
                                lastRefreshTokenTime = System.currentTimeMillis()
                                return true
                            } else {
                                //更新token失败
                                return when {
                                    refreshTokenFailCount >= 5 -> {
                                        refreshTokenFailCount = 0
                                        BaseApp.app.onUserLogout(BaseApp.app.getString(R.string.re_login_and_token_past_due))
                                        false
                                    }
                                    refreshResult.errCode < 0 -> {
                                        // http请求失败
                                        false
                                    }
                                    else -> {
                                        // 业务请求失败
                                        BaseApp.app.onUserLogout(BaseApp.app.getString(R.string.re_login_and_token_past_due))
                                        false
                                    }
                                }
                            }
                        }
                        Constant.Result.ACCOUNT_BAN_SMS_CODE_LOGIN,//该账号禁止短信验证码登录
                        Constant.Result.ACCOUNT_DISABLE,//账号被禁止登陆
                        Constant.Result.ACCOUNT_IS_BANNED,//您的账号已被禁止登录
                        Constant.Result.ACCOUNT_IS_CANCEL,//您的账号已注销
                        Constant.Result.ACCOUNT_RELOGIN -> {//账号需要重新登录
                            AppLogcat.logger.d("TokenReqInterceptor", "接口请求失败,退出登录状态,code->$errCode")
                            BaseApp.app.onUserLogout(BaseApp.app.getString(R.string.please_re_login))
                            return false
                        }
                        else -> {
                            //其他错误
                            AppLogcat.logger.d("TokenReqInterceptor", "接口请求失败,code->$errCode")
                            return false
                        }
                    }
                }
            }
        } finally {
            refreshTokenLock.unlock()
        }
    }

    fun autoLoginCurrentAccount(complete: () -> Unit) {
        ThreadUtils.runOnIOThread {
            if (AccountManager.hasLoginAccount()) {
                try {
                    val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
                    RLogManager.i("User", "Http登录--->${accountInfo.getNickName()}/${accountInfo.getUserId()}")
                    val result = refreshToken(accountInfo)
                    if (result != null) {
                        RLogManager.e("User", "Http登录失败--->${result.errCode}")
                        when (result.errCode) {
                            ACCOUNT_LOGIN_DISABLE -> {
                                // 长时间没有登录错误
                                BaseApp.app.onUserLogout(BaseApp.app.getString(R.string.long_time_no_login))
                            }
                            else -> {
                                if (result.errCode > 0) {
                                    // 其他业务错误
                                    BaseApp.app.onUserLogout(result.errMsg)
                                } else {
                                    // http请求错误
                                }
                            }
                        }
                    } else {
                        RLogManager.d("User", "Http登录成功")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    RLogManager.e("User", "Http登录失败", e)
                } finally {
                    complete.invoke()
                }
            } else {
                RLogManager.e("User", "当前没有账号登录--->")
                complete.invoke()
            }
        }
    }

    /**
     * 刷新Token
     */
    private fun refreshToken(accountInfo: AccountInfo): CommonProto.CommonResult? {
        BaseApp.app.onUserChangeToken()

        val refreshRequest = if (accountInfo.getLoginType() == 1) {
            autoSystemLoginPwd(accountInfo)
        } else {
            autoSystemLoginSms(accountInfo)
        }

        val httpResponse = refreshRequest.execute()
        val loginResp = httpResponse.body()
        if (httpResponse.code() == 200 && loginResp != null) {
            val code = loginResp.commonResult?.errCode
            if (code == 200) {
                // 刷新token成功
                if (accountInfo.getLoginType() == 1) {
                    BusinessApplication.saveAccountInfoByLogin(accountInfo.getLoginType(), loginResp.user.uid,
                            accountInfo.getPhone(), accountInfo.getPassWord(), "", accountInfo.getCountryCode(), loginResp)
                } else {
                    BusinessApplication.saveAccountInfoByLogin(accountInfo.getLoginType(), loginResp.user.uid,
                            accountInfo.getPhone(), "", "", accountInfo.getCountryCode(), loginResp)
                }

                ThreadUtils.runOnUIThread {
                    SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()).putLastManualLoginTime(loginResp.disableTime)
                }
                refreshTokenFailCount = 0
                AppLogcat.logger.d("TokenReqInterceptor", "更新token成功->${loginResp.sessionId}")
                return null
            } else {
                refreshTokenFailCount++
                // 业务请求失败
                AppLogcat.logger.d("TokenReqInterceptor", "更新token失败 resultCode->$code")
                return loginResp.commonResult
            }
        } else {
            // http请求失败
            AppLogcat.logger.d("TokenReqInterceptor", "更新token失败 responseCode->${httpResponse.code()}")
            return CommonProto.CommonResult.newBuilder().setErrCode(-(httpResponse.code())).build()
        }
    }

    private fun autoSystemLoginPwd(accountInfo: AccountInfo): Call<LoginProto.LoginResp> {
        return HttpManager.getStore(LoginHttpProtocol::class.java)
                .autoLogin(object : HttpReq<LoginProto.LoginReq>() {
                    override fun getData(): LoginProto.LoginReq {
                        return LoginHttpReqCreator.createLoginAutoPasswordReq(accountInfo.getPhone(), accountInfo.getPassWord(), accountInfo.getCountryCode())
                    }
                })
    }

    private fun autoSystemLoginSms(accountInfo: AccountInfo): Call<LoginProto.LoginResp> {
        return HttpManager.getStore(LoginHttpProtocol::class.java)
                .autoLogin(object : HttpReq<LoginProto.LoginReq>() {
                    override fun getData(): LoginProto.LoginReq {
                        return LoginHttpReqCreator.createLoginAutoSmsReq(accountInfo.getPhone(), accountInfo.getToken(), accountInfo.getCountryCode())
                    }
                })
    }
}