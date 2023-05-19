package framework.telegram.business.ui.search.presenter

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.search.BaseSearchAdapter
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.ui.search.contract.SearchContract
import framework.telegram.support.account.AccountManager
import io.reactivex.Observable
import io.realm.Case
import io.realm.Realm

class SearchContactsPresenterImpl : SearchContract.Presenter {

    private var mContext: Context?
    private var mView: SearchContract.View
    private var mObservalbe: Observable<ActivityEvent>
    private var mTargetPic :String ?= ""
    private var mTargetName:String ?= ""

    constructor(view: SearchContract.View, context: Context?, observable: Observable<ActivityEvent>,targetPic:String="",targetName:String ="") {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        this.mTargetPic = targetPic
        this.mTargetName = targetName
        view.setPresenter(this)
    }

    override fun start() {
        if (!TextUtils.isEmpty(mTargetPic) ||!TextUtils.isEmpty(mTargetName)){
            val mMapList =  mutableListOf<MutableMap<String, String>>()
            val map = mutableMapOf<String, String>()
            map[BaseSearchAdapter.SEARCH_USER_NAME] = mTargetName?:""
            map[BaseSearchAdapter.SEARCH_USER_ICON] = mTargetPic?:""
            mMapList.add(map)
            mView.setMapListData(mMapList)
        }

    }

    @SuppressLint("CheckResult")
    override fun getDataSearchList(keyword: String, dataList: MutableList<MultiItemEntity>,pageNum:Int) {
        if (!TextUtils.isEmpty(keyword)){
            val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
            RealmCreator.executeContactsTransactionAsync(myUid, {
                val contacts = it.where(ContactDataModel::class.java)?.
                        equalTo("bfMyContacts", true)?.
                        equalTo("bfMyBlack", false)?.
                        beginGroup()?.
                        like("nickName", "*$keyword*", Case.INSENSITIVE)?.
                        or()?.
                        like("noteName", "*$keyword*", Case.INSENSITIVE)?.
                        or()?.
                        like("searchPhone", "*$keyword*", Case.INSENSITIVE)?.
                        or()?.
                        like("identify", "*$keyword*", Case.INSENSITIVE)?.
                        endGroup()?.
                        findAll()

                contacts?.forEach { model ->
                    dataList.add(model.copyContactDataModel())
                }
            },  {
                mView.getDataListSuccess(dataList,false)
            })
        }else{
            mView.getDataListSuccess(mutableListOf(),false)
        }
    }

    override fun destroy() {
    }
}

