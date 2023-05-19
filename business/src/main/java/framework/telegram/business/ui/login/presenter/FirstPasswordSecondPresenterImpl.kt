package framework.telegram.business.ui.login.presenter

import android.content.Context
import android.text.TextUtils
import com.im.domain.pb.CommonProto
import com.im.domain.pb.LoginProto
import com.im.domain.pb.SysProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.R
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.LoginHttpReqCreator
import framework.telegram.business.http.creator.SysHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.LoginHttpProtocol
import framework.telegram.support.BaseApp
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.MD5
import io.reactivex.Observable
import yourpet.client.android.sign.NativeLibUtil

class FirstPasswordSecondPresenterImpl : FindPasswordSecondContract.Presenter {

    private val mContext: Context
    private val mView: FindPasswordSecondContract.View
    private val mViewObservalbe: Observable<ActivityEvent>

    constructor(view: FindPasswordSecondContract.View, context: Context, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {

    }

    override fun sure(phone: String, countryCode: String,smsCode:String,password:String) {
        val realPassword = if (TextUtils.isEmpty(password)) "" else getPassword(password)
        HttpManager.getStore(LoginHttpProtocol::class.java)
                .findPassword(object : HttpReq<LoginProto.ForgetPasswordReq>() {
                    override fun getData(): LoginProto.ForgetPasswordReq {
                        return LoginHttpReqCreator.createFindPasswordReq(phone, smsCode, countryCode,realPassword)
                    }
                })
                .getResult(mViewObservalbe, {
                    //请求成功
                    mView.success(mContext.getString(R.string.reset_password_s))
                }, {
                    //请求失败
                    mView.showErrMsg(it.message)
                })
    }

    private fun getPassword(data: String): String {
        return try {
            NativeLibUtil.getInstance().sign2(BaseApp.app, false, MD5.md5(data))
        } catch (e: Exception) {
            ""
        }
    }
}