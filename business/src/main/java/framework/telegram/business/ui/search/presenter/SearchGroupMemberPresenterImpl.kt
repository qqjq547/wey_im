package framework.telegram.business.ui.search.presenter

import android.annotation.SuppressLint
import android.content.Context
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.im.domain.pb.CommonProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.group.GroupMemberModel
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.ui.search.contract.SearchContract
import framework.telegram.support.account.AccountManager
import io.reactivex.Observable
import io.realm.Case
import io.realm.Realm

class SearchGroupMemberPresenterImpl : SearchContract.Presenter {
    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private var mContext: Context?
    private var mView: SearchContract.View
    private var mObservalbe: Observable<ActivityEvent>
    private var mGroupId = 0L
    private var mIsNeedMe = false

    private var mSearchType = -1

    constructor(view: SearchContract.View, context: Context?, groupId: Long, isNeedMe: Boolean, observable: Observable<ActivityEvent>, searchType: Int) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        this.mGroupId = groupId
        this.mIsNeedMe = isNeedMe
        this.mSearchType = searchType
        view.setPresenter(this)
    }

    override fun start() {

    }

    @SuppressLint("CheckResult")
    override fun getDataSearchList(keyword: String, dataList: MutableList<MultiItemEntity>,pageNum:Int) {
        RealmCreator.executeGroupMembersTransactionAsync(mMineUid, mGroupId, {
            val contacts = it.where(GroupMemberModel::class.java)?.
                    like("nickName", "*$keyword*", Case.INSENSITIVE)?.
                    or()?.
                    like("groupNickName", "*$keyword*", Case.INSENSITIVE)?.
                    or()?.
                    like("remarkName", "*$keyword*", Case.INSENSITIVE)?.findAll()
            contacts?.forEach { model ->
                val info = model.copyGroupMemberModel()

                if (mSearchType == Constant.Search.SEARCH_GROUP_ADD_ADMIN) {
                    if (model.type != CommonProto.GroupMemberType.HOST.number && model.type != CommonProto.GroupMemberType.MANAGE.number) {
                        if (mIsNeedMe) {
                            dataList.add(info)
                        } else {
                            if (info.uid != mMineUid) {
                                dataList.add(info)
                            }
                        }
                    }
                } else {
                    if (mIsNeedMe) {
                        dataList.add(info)
                    } else {
                        if (info.uid != mMineUid) {
                            dataList.add(info)
                        }
                    }
                }
            }
        }, {
            mView.getDataListSuccess(dataList,false)
        })
    }

    override fun destroy() {
    }
}

