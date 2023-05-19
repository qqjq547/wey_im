package framework.telegram.business.ui.me.presenter

import android.content.Context
import android.text.TextUtils
import com.im.domain.pb.UserProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.UserHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.UserHttpProtocol
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.MD5
import io.reactivex.Observable
import yourpet.client.android.sign.NativeLibUtil

class PasswordSetSecondPresenterImpl : PasswordSetSecondContract.Presenter {


    private val mContext: Context
    private val mView: PasswordSetSecondContract.View
    private val mViewObservalbe: Observable<ActivityEvent>

    constructor(view: PasswordSetSecondContract.View, context: Context, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {

    }

    override fun setPassword(password: String) {
        mView.showLoading()
        val realPassword = if (TextUtils.isEmpty(password)) "" else getPassword(password)
        HttpManager.getStore(UserHttpProtocol::class.java)
                .setPassword(object : HttpReq<UserProto.PasswordReq>() {
                    override fun getData(): UserProto.PasswordReq {
                        return UserHttpReqCreator.createSetPasswordReq(realPassword)
                    }
                })
                .getResult(mViewObservalbe, {
                    //请求成功
                    mView.setPasswordSuccess(password)
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