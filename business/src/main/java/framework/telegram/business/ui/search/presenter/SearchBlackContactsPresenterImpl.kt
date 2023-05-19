package framework.telegram.business.ui.search.presenter

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.ui.search.contract.SearchContract
import framework.telegram.support.account.AccountManager
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Case
import io.realm.Realm

class SearchBlackContactsPresenterImpl : SearchContract.Presenter {

    private var mContext: Context?
    private var mView: SearchContract.View
    private var mObservalbe: Observable<ActivityEvent>

    constructor(view: SearchContract.View, context: Context?, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {

    }

    @SuppressLint("CheckResult")
    override fun getDataSearchList(keyword: String, dataList: MutableList<MultiItemEntity>,pageNum:Int) {
        if (!TextUtils.isEmpty(keyword)){
            val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
            RealmCreator.executeContactsTransactionAsync(myUid, {
                val contacts = it.where(ContactDataModel::class.java)?.
                        equalTo("bfMyBlack", true)?.
                        beginGroup()?.
                        like("noteName", "*$keyword*", Case.INSENSITIVE)?.
                        or()?.
                        like("nickName", "*$keyword*", Case.INSENSITIVE)?.
                        endGroup()?.
                        findAll()
                contacts?.forEach { model ->
                    dataList.add(model.copyContactDataModel())
                }
            }, {
                mView.getDataListSuccess(dataList,false)
            })
        }else{
            mView.getDataListSuccess(mutableListOf(),false)
        }
    }

    override fun destroy() {
    }
}

