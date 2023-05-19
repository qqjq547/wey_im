package framework.telegram.business.ui.group.presenter

import android.content.Context
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.group.GroupInfoModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.db.RealmCreator
import framework.telegram.support.account.AccountManager
import io.reactivex.Observable

class MyGroupChatsPresenterImpl : MyGroupChatsContract.Presenter {
    private var mContext: Context?
    private var mView: MyGroupChatsContract.View
    private var mObservalbe: Observable<ActivityEvent>

    constructor(view: MyGroupChatsContract.View, context: Context?, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {
        mView.showLoading()
        ArouterServiceManager.groupService.updateMyGroupChats(mObservalbe,complete={
            val list = mutableListOf<GroupInfoModel>()
            val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
            RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
                val results = realm.where(GroupInfoModel::class.java)?.equalTo("bfAddress", true)?.sort("name")?.findAll()
                results?.forEach {
                    val currentItem = it.copyGroupInfoModel()
                    list.add(currentItem)
                }
            }, {
                updateData(list)
            })
        })
    }

    override fun updateData(list: MutableList<GroupInfoModel>) {
        mView.refreshListUI(list)
    }
}

