package framework.telegram.business.ui.contacts.presenter

import android.content.Context
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.ContactsProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.R
import framework.telegram.business.ui.contacts.bean.ContactReqItemBean
import framework.ideas.common.model.contacts.ContactReqModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.bridge.service.IContactService
import io.reactivex.Observable

class ContactReqListPresenterImpl : ContactReqListContract.Presenter {


    private var mContext: Context?
    private var mView: ContactReqListContract.View
    private var mObservalbe: Observable<ActivityEvent>

    private val mRecordList by lazy { ArrayList<Any>() }

    constructor(view: ContactReqListContract.View, context: Context?, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {
        ArouterServiceManager.contactService.updateAllContactReq(mObservalbe)//更新请求列表
        mView.showLoading()
    }

    override fun updateData(data: List<ContactReqModel>) {
        if (data.isNullOrEmpty()) {
            mRecordList.clear()
            mView.showEmpty()
        } else {
            mRecordList.clear()

            val unRecordList = ArrayList<ContactReqModel>()
            val recordedList = ArrayList<ContactReqModel>()

            data.forEach {
                if (it.type == 0) {
                    unRecordList.add(it)
                } else {
                    recordedList.add(it)
                }
            }

            if (unRecordList.isNotEmpty()) {
                mRecordList.add(ContactReqItemBean.createNewFriendTitle(mContext?.getString(R.string.pending)?:""))
                unRecordList.forEach { info ->
                    mRecordList.add(ContactReqItemBean.createNewFriend(info))
                }
            }
            if (recordedList.isNotEmpty()) {
                mRecordList.add(ContactReqItemBean.createNewFriendTitle(mContext?.getString(R.string.a_recent_request)?:""))
                recordedList.forEach { info ->
                    mRecordList.add(ContactReqItemBean.createNewFriendFinish(info))
                }
            }
        }
        mView.refreshListUI(mRecordList)
    }

    override fun makeFriend(applyUid: Long, op: ContactsProto.ContactsOperator) {
        when (op) {
            ContactsProto.ContactsOperator.ADD_REQ -> {
                ArouterServiceManager.contactService.agreeContactReq(mObservalbe, applyUid, {

                }, {
                    mView.showError(mContext?.getString(R.string.common_fail) + "  " + it.message)
                })
            }
            else -> {
                ArouterServiceManager.contactService.deleteContactReq(mObservalbe, applyUid, {

                }, {
                    mView.showError(mContext?.getString(R.string.common_fail) + "  " + it.message)
                })
            }
        }
    }
}

