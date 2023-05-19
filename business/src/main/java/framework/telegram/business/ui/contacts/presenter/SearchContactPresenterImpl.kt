package framework.telegram.business.ui.contacts.presenter

import android.content.Context
import com.im.domain.pb.ContactsProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.R
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.ContactsHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.FriendHttpProtocol
import framework.telegram.support.system.network.http.HttpReq
import io.reactivex.Observable

class SearchContactPresenterImpl : SearchContactContract.Presenter {

    private var mContext: Context?
    private var mView: SearchContactContract.View
    private var mObservalbe: Observable<ActivityEvent>

    constructor(
        view: SearchContactContract.View,
        context: Context?,
        observable: Observable<ActivityEvent>
    ) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {
    }

    override fun getDataList(phone: String) {
        mView.showLoading()
        HttpManager.getStore(FriendHttpProtocol::class.java)
            .findContacts(object : HttpReq<ContactsProto.FindContactsListReq>() {
                override fun getData(): ContactsProto.FindContactsListReq {
                    return ContactsHttpReqCreator.findContacts(phone, 0, "")
                }
            })
            .getResult(mObservalbe, {
                if (it.detailListCount == 0) {
                    mView.showEmpty(mContext?.getString(R.string.can_t_find) ?: "")
                } else {
                    mView.refreshUI(it)
                }
            }, {
                //请求失败
                mView.showError(it.message)
            })
    }
}

