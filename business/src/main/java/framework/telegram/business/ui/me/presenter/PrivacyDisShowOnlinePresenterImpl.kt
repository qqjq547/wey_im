package framework.telegram.business.ui.me.presenter

import android.content.Context
import com.im.domain.pb.CommonProto
import com.im.domain.pb.UserProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.UserHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.UserHttpProtocol
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.network.http.HttpReq
import io.reactivex.Observable

class PrivacyDisShowOnlinePresenterImpl : PrivacyDisShowOnlineContract.Presenter {
    private val mContext: Context
    private val mView: PrivacyDisShowOnlineContract.View
    private val mViewObservalbe: Observable<ActivityEvent>

    constructor(view: PrivacyDisShowOnlineContract.View, context: Context, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {

    }

    override fun savePerfectInfo(viewType: CommonProto.LastOnlineTimeViewType) {
        val userParam = UserProto.UserParam.newBuilder().setViewType(viewType).build()
        val opList = listOf(UserProto.UserOperator.VIEW_ONLINE_TIME)
        HttpManager.getStore(UserHttpProtocol::class.java)
                .updateUserInfo(object : HttpReq<UserProto.UpdateReq>() {
                    override fun getData(): UserProto.UpdateReq {
                        return UserHttpReqCreator.createUserUpdateReq(opList, userParam)
                    }
                })
                .getResult(mViewObservalbe, {
                    //请求成功
                    val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
                    accountInfo.putOnlineViewType(viewType.number)
                    mView.savePerfectInfoSuccess()
                }, {
                    //请求失败
                    mView.showErrMsg(it.message)
                })
    }

}