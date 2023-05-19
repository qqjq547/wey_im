package framework.telegram.business.ui.me.presenter

import android.content.Context
import android.text.TextUtils
import com.im.domain.pb.CommonProto
import com.im.domain.pb.SysProto
import com.im.domain.pb.UserProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.BuildConfig
import framework.telegram.business.R
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.SysHttpReqCreator
import framework.telegram.business.http.creator.UserHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.LoginHttpProtocol
import framework.telegram.business.http.protocol.SystemHttpProtocol
import framework.telegram.business.http.protocol.UserHttpProtocol
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.MD5
import io.reactivex.Observable
import yourpet.client.android.sign.NativeLibUtil

class PasswordChangePresenterImpl : PasswordChangeContract.Presenter {

    private val mContext: Context
    private val mView: PasswordChangeContract.View
    private val mViewObservalbe: Observable<ActivityEvent>
    private var mSendSmsIndex = 0

    constructor(view: PasswordChangeContract.View, context: Context, observable: Observable<ActivityEvent>) {
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
                        return SysHttpReqCreator.createGetSmsCodeReq(phone, CommonProto.GetSmsCodeType.UPDATE_PASSWORD, countryCode,mSendSmsIndex)
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

    override fun updataPasswordByPassword(oldPassword: String, password: String) {
        mView.showLoading()
        val realOldPassword = if (TextUtils.isEmpty(oldPassword)) "" else getPassword(oldPassword)
        val realPassword = if (TextUtils.isEmpty(password)) "" else getPassword(password)
        HttpManager.getStore(UserHttpProtocol::class.java)
                .updatePassword(object : HttpReq<UserProto.UpdatePasswordReq>() {
                    override fun getData(): UserProto.UpdatePasswordReq {
                        return UserHttpReqCreator.createUpdatePasswordReq(realOldPassword, realPassword)
                    }
                })
                .getResult(mViewObservalbe, {
                    //请求成功
                    mView.updataPasswordByPasswordSuccess(password)
                }, {
                    //请求失败
                    mView.showErrMsg(it.message)
                })
    }

    override fun updataPasswordBySmsCode(countryCode: String, phone: String, smsCode: String, password: String) {
        mView.showLoading()
        val realPassword = if (TextUtils.isEmpty(password)) "" else getPassword(password)
        HttpManager.getStore(UserHttpProtocol::class.java)
                .updatePasswordFromSmsCode(object : HttpReq<UserProto.UpdatePasswordFromSmsCodeReq>() {
                    override fun getData(): UserProto.UpdatePasswordFromSmsCodeReq {
                        return UserHttpReqCreator.createUpdatePasswordFromSmsCodeReq(countryCode, phone, smsCode, realPassword)
                    }
                })
                .getResult(mViewObservalbe, {
                    //请求成功
                    mView.updataPasswordBySmsCode(countryCode, phone, smsCode, password)
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