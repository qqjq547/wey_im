package framework.telegram.business.ui.qr.presenter

import android.content.Context
import com.im.domain.pb.GroupProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.GroupHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.GroupHttpProtocol
import framework.telegram.support.system.network.http.HttpReq
import io.reactivex.Observable

class GroupQrPresenterImpl : GroupQrContract.Presenter {


    private val mContext: Context
    private val mView: GroupQrContract.View
    private val mViewObservalbe: Observable<ActivityEvent>

    constructor(view: GroupQrContract.View, context: Context, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {

    }

    override fun getGroupQr(groupId: Long, isReset: Boolean) {
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .getGroupQrCode(object : HttpReq<GroupProto.GroupQrCodeReq>() {
                    override fun getData(): GroupProto.GroupQrCodeReq {
                        return GroupHttpReqCreator.createGroupQrReq(groupId, isReset)
                    }
                })
                .getResult(mViewObservalbe, {
                    //请求成功
                    mView.getGroupQrSuccess(it.qrUrl, it.qrExpire)
                }, {
                    //请求失败
                    mView.showError(it.message)
                })
    }
}