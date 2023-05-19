package framework.telegram.business.ui.contacts.presenter

import android.content.Context
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.ContactsProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_UID
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.ContactsHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.FriendHttpProtocol
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.ExpandClass.toast
import io.reactivex.Observable

class UnAuditContactPresenterImpl : UnAuditContactContract.Presenter {


    private var mContext: Context?
    private var mView: UnAuditContactContract.View
    private var mObservalbe: Observable<ActivityEvent>


    constructor(view: UnAuditContactContract.View, context: Context?, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {
        mView.showLoading()
    }

    override fun getDataDetail(recordId: Long) {
        HttpManager.getStore(FriendHttpProtocol::class.java)
                .getUnAuditDetail(object : HttpReq<ContactsProto.ContactsUnAuditDetailReq>() {
                    override fun getData(): ContactsProto.ContactsUnAuditDetailReq {
                        return ContactsHttpReqCreator.createUnAuditDetail(recordId)
                    }
                })
                .getResult(mObservalbe, {
                    mView.refreshUI(it.unAuditDetail)
                }, {
                    //请求失败
                    mView.showError( it.message)
                })
    }

    override fun makeFriend(applyUid: Long, op: ContactsProto.ContactsOperator) {
        when (op) {
            ContactsProto.ContactsOperator.ADD_REQ -> {
                ArouterServiceManager.contactService.agreeContactReq(mObservalbe, applyUid, {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACT_DETAIL)
                            .withLong(KEY_TARGET_UID, applyUid).navigation()

                    mView.destory()
                }, {
                    mView.showError(mContext?.getString(R.string.common_fail) + "  " + it.message)
                })
            }
            else -> {
                ArouterServiceManager.contactService.deleteContactReq(mObservalbe, applyUid, {
                    mView.destory()
                }, {
                    mView.showError(mContext?.getString(R.string.common_fail) + "  " + it.message)
                })
            }
        }
    }

    override fun setBlack(targetUid: Long, isBlack: Boolean) {
        ArouterServiceManager.contactService.setContactBlack(mObservalbe, targetUid, isBlack, {
            mView.showSetBlackUI(isBlack)
        }, { mContext?.toast(it) })
    }
}

