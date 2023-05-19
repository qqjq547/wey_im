package framework.telegram.business.ui.me.presenter

import android.content.Context
import com.im.domain.pb.UserProto
import com.trello.rxlifecycle3.android.FragmentEvent
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.UserHttpReqCreator
import framework.telegram.business.http.getResultForFragment
import framework.telegram.business.http.protocol.UserHttpProtocol
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.network.http.HttpReq
import io.reactivex.Observable

class MeDetailPresenterImpl : MeDetailContract.Presenter {

    private val mContext: Context
    private val mView: MeDetailContract.View
    private val mViewObservable: Observable<FragmentEvent>


    constructor(view: MeDetailContract.View, context: Context, observable: Observable<FragmentEvent>) {
        this.mView = view
        this.mViewObservable = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {
        val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
        getDetailInfo(accountInfo.getUserId())
    }

    /**
     * 获取账户详细信息
     * @param uid 账户信息AccountInfo的uid
     */
    override fun getDetailInfo(uid: Long) {
        HttpManager.getStore(UserHttpProtocol::class.java)
                .getUserDetail(object : HttpReq<UserProto.DetailReq>() {
                    override fun getData(): UserProto.DetailReq {
                        return UserHttpReqCreator.createUserUpdateReq(uid)
                    }
                })
                .getResultForFragment(mViewObservable, {
                    //请求成功
                    mView.getDetailInfoSuccess(it)
                }, {
                    mView.showErrMsg(it.message)
                })
    }
}