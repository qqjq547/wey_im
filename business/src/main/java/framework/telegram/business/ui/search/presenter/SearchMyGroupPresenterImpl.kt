package framework.telegram.business.ui.search.presenter

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.group.GroupInfoModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.ui.search.contract.SearchContract
import framework.telegram.support.account.AccountManager
import io.reactivex.Observable
import io.realm.Case
import io.realm.Realm

class SearchMyGroupPresenterImpl : SearchContract.Presenter {

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
            RealmCreator.executeGroupsTransactionAsync(myUid, {
                val groups = it.where(GroupInfoModel::class.java)
                        ?.equalTo("bfAddress", true)
                        ?.beginGroup()?.like("name", "*$keyword*", Case.INSENSITIVE)
                        ?.or()
                        ?.like("groupNickName", "*$keyword*", Case.INSENSITIVE)
                        ?.endGroup()
                        ?.findAll()

                groups?.forEach { model ->
                    dataList.add(model.copyGroupInfoModel())
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

