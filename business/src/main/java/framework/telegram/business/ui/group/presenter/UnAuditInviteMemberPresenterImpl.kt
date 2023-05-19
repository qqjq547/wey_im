package framework.telegram.business.ui.group.presenter

import android.content.Context
import com.im.domain.pb.GroupProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.R
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.GroupHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.GroupHttpProtocol
import framework.telegram.support.system.network.http.HttpReq
import io.reactivex.Observable
import framework.telegram.support.tools.ExpandClass.toast

class UnAuditInviteMemberPresenterImpl : UnAuditInviteMemberContract.Presenter {

    private var mContext: Context?
    private var mView: UnAuditInviteMemberContract.View
    private var mObservalbe: Observable<ActivityEvent>

    constructor(view: UnAuditInviteMemberContract.View, context: Context?, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {
        mView.refreshUI()
    }

    override fun makeOperate(groupReqId: Long, op: Boolean) {
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .groupUserCheckJoin(object : HttpReq<GroupProto.GroupUserCheckJoinReq>() {
                    override fun getData(): GroupProto.GroupUserCheckJoinReq {
                        return GroupHttpReqCreator.createGroupUserCheckJoinReq(groupReqId, op)
                    }
                })
                .getResult(mObservalbe, {
                    mView.operateReq(op)
                }, {
                    //出错了
                    mView.showError(it.message)
                })
    }
}

