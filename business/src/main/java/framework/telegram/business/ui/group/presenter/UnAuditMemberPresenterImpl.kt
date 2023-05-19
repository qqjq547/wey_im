package framework.telegram.business.ui.group.presenter

import android.content.Context
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.GroupProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.service.IContactService
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.GroupHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.GroupHttpProtocol
import framework.telegram.support.system.network.http.HttpReq
import io.reactivex.Observable

class UnAuditMemberPresenterImpl : UnAuditMemberContract.Presenter {

    private val mContext: Context?
    private val mView: UnAuditMemberContract.View
    private val mObservalbe: Observable<ActivityEvent>
    private val mUserId: Long

    constructor(view: UnAuditMemberContract.View, context: Context?, observable: Observable<ActivityEvent>, userId: Long) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        this.mUserId = userId
        view.setPresenter(this)
    }

    override fun start() {
        getUserDetail()
    }

    override fun getUserDetail() {
        mView.showLoading()
        ArouterServiceManager.contactService.updateContactInfo(mObservalbe, mUserId, { contactInfoModel ->
//            mView.dissmissLoading()
            //这个不要获取本地数据了，因为非联系在其他页面不会同步数据，如果数据库添加字段，本地缓存数据会差一些新加字段，导致崩溃
//            mView.refreshUI(contactInfoModel)
        }, {
            mView.dissmissLoading()
            mView.refreshUI(it)
        },{
            mView.showError(mContext?.getString(R.string.user_information_cannot_be_obtained)?:"")
            mView.dissmissLoading()
            mView.destory()
        })
    }

    override fun makeOperate(groupReqId: Long, op: Boolean) {
        mView.showLoading()
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .groupCheckJoin(object : HttpReq<GroupProto.GroupCheckJoinReq>() {
                    override fun getData(): GroupProto.GroupCheckJoinReq {
                        return GroupHttpReqCreator.createGroupCheckJoinReq(groupReqId, op)
                    }
                })
                .getResult(mObservalbe, {
                    mView.dissmissLoading()
                    mView.operateReq(op)
                }, {
                    //出错了
                    mView.dissmissLoading()
                    mView.showError(mContext?.let { c ->
                        String.format(c.getString(R.string.operation_failure_sign),it.message)
                    }?:"")
                })
    }
}

