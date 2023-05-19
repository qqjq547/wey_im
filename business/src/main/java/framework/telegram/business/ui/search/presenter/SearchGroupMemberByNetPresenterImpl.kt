package framework.telegram.business.ui.search.presenter

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.im.domain.pb.CommonProto
import com.im.domain.pb.GroupProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.group.GroupMemberModel
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.Search.SEARCH_GROUP_ADD_ADMIN
import framework.telegram.business.bridge.Constant.Search.SEARCH_GROUP_ALL_MEMBER
import framework.telegram.business.bridge.Constant.Search.SEARCH_GROUP_AT_MEMBER
import framework.telegram.business.bridge.Constant.Search.SEARCH_GROUP_TAN_OWNER
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.ContactsHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.GroupHttpProtocol
import framework.telegram.business.ui.search.contract.SearchContract
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.network.http.HttpReq
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.realm.Case
import io.realm.Realm

class SearchGroupMemberByNetPresenterImpl : SearchContract.Presenter {
    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private var mContext: Context?
    private var mView: SearchContract.View
    private var mObservalbe: Observable<ActivityEvent>
    private var mGroupId = 0L
    private var mIsNeedMe = false

    private var mSearchType = -1

    private var mLastDisposable : Disposable?=null

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
        if (!TextUtils.isEmpty(keyword)){
            if (pageNum == 1){
                getDataByCahce(keyword){
                    getDataByNet(keyword,it,pageNum)
                }
            }else{
                getDataByNet(keyword,dataList,pageNum)
            }
        }else{
            mView.getDataListSuccess(mutableListOf(),false)
        }
    }

    private fun getDataByNet(keyword: String, dataList: MutableList<MultiItemEntity>,pageNum:Int){
        val pageSize = 100
        mLastDisposable?.dispose()
        mLastDisposable = HttpManager.getStore(GroupHttpProtocol::class.java)
                .getSearchGroupMember(object : HttpReq<GroupProto.SearchGroupMemberReq>() {
                    override fun getData(): GroupProto.SearchGroupMemberReq {
                        return ContactsHttpReqCreator.createGroupSearch( mGroupId,keyword, pageNum,pageSize,getFilterType())
                    }
                })
                .getResult(null, {
                    val groupMemberModels = mutableListOf<GroupMemberModel>()
                    if (it.memberListList?.isNotEmpty() == true) {
                        it.memberListList.forEach { member ->
                            val netModel = GroupMemberModel.createGroupMember(member.user.uid, member.user.nickName, member.user.gender.number,
                                    member.user.icon, member.user.friendRelation.bfFriend, member.user.friendRelation.remarkName, member.groupId,
                                    member.type.number, member.groupNickName, member.score,
                                    member.user.userOnOrOffline.online, member.user.userOnOrOffline.createTime, member.user.userOnOrOffline.bfShow)
                            if (!dataList.contains(netModel)){
                                groupMemberModels.add(netModel)
                            }
                        }
                        dataList.addAll(groupMemberModels)
                    }
                    mView.getDataListSuccess(dataList, it.memberListList.size >= pageSize)
                }, {

                })
    }

    override fun destroy() {
    }

    private fun getDataByCahce(keyword: String,call:((MutableList<MultiItemEntity>)->Unit)){
        val dataList = mutableListOf<MultiItemEntity>()
        RealmCreator.executeGroupMembersTransactionAsync(mMineUid, mGroupId, {
            val contacts = it.where(GroupMemberModel::class.java)?.
                    limit(100)?.
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
            call.invoke(dataList)
        })
    }

    private fun getFilterType(): CommonProto.FilterType {
        when(mSearchType){
            SEARCH_GROUP_ALL_MEMBER->{
               return CommonProto.FilterType.BE_DEFAULT
            }
            SEARCH_GROUP_AT_MEMBER->{
                return  CommonProto.FilterType.NOT_ONESELF
            }
            SEARCH_GROUP_TAN_OWNER->{
                return CommonProto.FilterType.NOT_ONESELF
            }
            SEARCH_GROUP_ADD_ADMIN->{
                return CommonProto.FilterType.BE_MEMBER
            }
            else ->{
                return CommonProto.FilterType.BE_DEFAULT
            }
        }
    }
}

