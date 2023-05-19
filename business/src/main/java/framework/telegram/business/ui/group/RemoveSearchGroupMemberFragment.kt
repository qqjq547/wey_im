package framework.telegram.business.ui.group

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.im.domain.pb.CommonProto
import com.im.domain.pb.GroupProto
import com.trello.rxlifecycle3.RxLifecycle
import com.trello.rxlifecycle3.android.FragmentEvent
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.group.GroupMemberModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.service.IGroupService
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.event.RemoveGroupSearchMemberEvent
import framework.telegram.business.event.RemoveSelectMemberEvent
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.ContactsHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.GroupHttpProtocol
import framework.telegram.business.ui.group.adapter.RemoveGroupSearchAdapter
import framework.telegram.support.BaseFragment
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.network.http.HttpReq
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.realm.Case
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import kotlinx.android.synthetic.main.bus_group_fragment_search_member.*

/**
 * 显示来源为联系人
 *
 * 如果携带groupId(long)参数打开页面，则为一个群添加群成员
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_REMOVE_SEARCH_FRAGMENT)
class RemoveSearchGroupMemberFragment : BaseFragment(){
    override val fragmentName: String
        get() = "RemoveSearchGroupMemberFragment"

    private val mRealm: Realm by lazy { RealmCreator.getGroupMembersRealm(mMineUid, mGroupId ?: 0) }

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private val mGroupId by lazy { arguments?.getLong("groupId", 0) }

    private val mSelectList by lazy { arguments?.getParcelableArrayList<Parcelable>("selectList") }

    private val mAdapter by lazy { RemoveGroupSearchAdapter(mKeyword) }

    private val mListDatas by lazy { ArrayList<MultiItemEntity>() }

    private var mKeyword = ""

    private var mLastDisposable : Disposable?=null

    private var mPageNum = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bus_group_fragment_search_member, container, false)
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        mKeyword = arguments?.getString("keyword") ?: ""
        mAdapter.setSelectedUid(mSelectList as java.util.ArrayList<Long>)
        if (mGroupId ?: 0 > 0) {
            // 添加联系人到群中
            mPageNum = 1
            getDataSearchList(mKeyword, mutableListOf(),mPageNum)
        }
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        common_recycler?.initSingleTypeRecycleView(LinearLayoutManager(this.context), mAdapter, true)
        common_recycler.refreshController().setEnablePullToRefresh(false)
        common_recycler.loadMoreController().setOnLoadMoreListener {
            mPageNum++
            getDataSearchList(mKeyword,mListDatas,mPageNum)
        }

        EventBus.getFlowable(RemoveGroupSearchMemberEvent::class.java)
                .bindToLifecycle(this@RemoveSearchGroupMemberFragment)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (activity != null) {
                        if (it.from == 1) {
                            mKeyword = it.keyword
                            mAdapter.setKeyword(mKeyword)
                            mPageNum = 1
                            getDataSearchList(mKeyword, mutableListOf(),mPageNum)
                        }
                    }
                }

        EventBus.getFlowable(RemoveSelectMemberEvent::class.java)
                .bindToLifecycle(this@RemoveSearchGroupMemberFragment)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (activity != null) {
                        if (it.from == 1) {
                            if (it.op == 1) {
                                mAdapter.addSelectedUid(it.uid)
                            } else {
                                mAdapter.removeSelectedUid(it.uid)
                            }
                        } else if (it.from == 3) {
                            mAdapter.removeSelectedUid(it.uid)
                        }
                        mAdapter.notifyDataSetChanged()
                    }
                }
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.removeAllChangeListeners()
        mRealm.close()
    }

    @SuppressLint("CheckResult")

    private fun getDataByNet(keyword: String, dataList: MutableList<MultiItemEntity>,pageNum:Int){
        val pageSize = 15
        mLastDisposable?.dispose()
        mLastDisposable = HttpManager.getStore(GroupHttpProtocol::class.java)
                .getSearchGroupMember(object : HttpReq<GroupProto.SearchGroupMemberReq>() {
                    override fun getData(): GroupProto.SearchGroupMemberReq {
                        return ContactsHttpReqCreator.createGroupSearch( mGroupId?:0L,keyword, pageNum,pageSize,CommonProto.FilterType.NOT_ONESELF)
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
                    refreshData(dataList, it.memberListList.size >= pageSize)
                }, {

                })
    }

    private fun getDataByCahce(keyword: String,call:((MutableList<MultiItemEntity>)->Unit)){
        val dataList = mutableListOf<MultiItemEntity>()
        RealmCreator.executeGroupMembersTransactionAsync(mMineUid, mGroupId?:0L, {
            val contacts = it.where(GroupMemberModel::class.java)?.
                    limit(100)?.
                    like("nickName", "*$keyword*", Case.INSENSITIVE)?.
                    or()?.
                    like("groupNickName", "*$keyword*", Case.INSENSITIVE)?.
                    or()?.
                    like("remarkName", "*$keyword*", Case.INSENSITIVE)?.findAll()
            contacts?.forEach { model ->
                val info = model.copyGroupMemberModel()
                if (mMineUid == info.uid) {
                    mAdapter.setGroupType(info.type)
                }
                dataList.add(info)
            }
        }, {
            refreshData(dataList,false)
            call.invoke(dataList)
        })
    }


    private fun refreshData(list: MutableList<MultiItemEntity>,hasMore:Boolean) {
        if (activity == null) {
            return
        }
        mListDatas.clear()
        mListDatas.addAll(list)
        mAdapter.setKeyword(mKeyword)
        mAdapter.setNewData(mListDatas)

        if (!hasMore) {
            common_recycler.loadMoreController().loadMoreEnd()
        } else {
            common_recycler.loadMoreController().loadMoreComplete()
        }
    }

    private fun getDataSearchList(keyword: String, dataList: MutableList<MultiItemEntity>,pageNum:Int) {
        if (!TextUtils.isEmpty(keyword)){
            if (pageNum == 1){
                getDataByCahce(keyword){
                    getDataByNet(keyword,it,pageNum)
                }
            }else{
                getDataByNet(keyword,dataList,pageNum)
            }
        }else{
            refreshData(mutableListOf(),false)
        }
    }
}
