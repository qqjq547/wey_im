package framework.telegram.business.ui.search.presenter

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.im.StreamCallModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.ui.search.contract.SearchContract
import framework.telegram.message.bridge.bean.StreamCallItem
import framework.telegram.message.bridge.service.IMessageService
import io.reactivex.Observable
import io.realm.Case
import io.realm.Realm
import io.realm.Sort

class SearchCallPresenterImpl : SearchContract.Presenter {
    private val mGroupRealm by lazy { ArouterServiceManager.messageService.newStreamCallRealm() }

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
            var models: Iterable<StreamCallModel>? = null
            mGroupRealm.executeTransactionAsync(Realm.Transaction { realm ->
                models = realm.where(StreamCallModel::class.java).
                        like("chaterName", "*$keyword*", Case.INSENSITIVE)?.
                        or()?.
                        like("chaterNickName", "*$keyword*", Case.INSENSITIVE)?.
                        sort("reqTime", Sort.DESCENDING)?.
                        findAll()
                val list = mutableListOf<MultiItemEntity>()
                models?.forEach {
                    val currentItem = it.copyStream()
                    if (list.size >= 2) {
                        val last = list.last() as StreamCallItem
                        if (last.data.chaterId == currentItem.chaterId && last.data.status == 0 && last.data.isSend == 0 && currentItem.status == 0 && currentItem.isSend == 0) {
                            last.nearCount = last.nearCount + 1
                        } else {
                            list.add(StreamCallItem(currentItem, 0))
                        }
                    } else {
                        list.add(StreamCallItem(currentItem, 0))
                    }
                }
                dataList.addAll(list)
            }, Realm.Transaction.OnSuccess {
                mView.getDataListSuccess(dataList,false)
            })
        }else{
            mView.getDataListSuccess(mutableListOf(),false)
        }
    }

    override fun destroy() {
        mGroupRealm.removeAllChangeListeners()
        mGroupRealm.close()
    }
}

