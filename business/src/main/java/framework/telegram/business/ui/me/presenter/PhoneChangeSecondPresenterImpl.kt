package framework.telegram.business.ui.me.presenter

import android.content.Context
import com.im.domain.pb.CommonProto
import com.im.domain.pb.SysProto
import com.im.domain.pb.UserProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.R
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.SysHttpReqCreator
import framework.telegram.business.http.creator.UserHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.LoginHttpProtocol
import framework.telegram.business.http.protocol.SystemHttpProtocol
import framework.telegram.business.http.protocol.UserHttpProtocol
import framework.telegram.support.system.network.http.HttpReq
import io.reactivex.Observable

class PhoneChangeSecondPresenterImpl : PhoneChangeSecondContract.Presenter {

    private val mContext: Context
    private val mView: PhoneChangeSecondContract.View
    private val mViewObservalbe: Observable<ActivityEvent>
    private var mSendSmsIndex = 0

    constructor(view: PhoneChangeSecondContract.View, context: Context, observable: Observable<ActivityEvent>) {
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
                        return SysHttpReqCreator.createGetSmsCodeReq(phone, CommonProto.GetSmsCodeType.UPDATE_PHONE, countryCode,mSendSmsIndex)
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

    override fun bindPhone(phone: String, countryCode: String, smsCod: String) {
        mView.showLoading()
        HttpManager.getStore(UserHttpProtocol::class.java)
                .updatePhoto(object : HttpReq<UserProto.UpdatePhoneReq>() {
                    override fun getData(): UserProto.UpdatePhoneReq {
                        return UserHttpReqCreator.createUpdatePhoneReq(countryCode, phone, smsCod)
                    }
                })
                .getResult(mViewObservalbe, {
                    //请求成功
                    mView.bindPhoneSuccess(phone, countryCode)
                }, {
                    //请求失败
                    mView.showErrMsg(it.message)
                })
    }
}