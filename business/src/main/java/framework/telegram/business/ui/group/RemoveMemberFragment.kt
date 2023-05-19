package framework.telegram.business.ui.group

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.trello.rxlifecycle3.RxLifecycle
import com.trello.rxlifecycle3.android.ActivityEvent
import com.trello.rxlifecycle3.android.FragmentEvent
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.group.GroupMemberModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.service.IGroupService
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.event.RemoveSelectMemberEvent
import framework.telegram.business.ui.group.adapter.RemoveGroupMemberAdapter
import framework.telegram.support.BaseFragment
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.bus_recycler_inside_linear.*

/**
 * 显示来源为联系人
 *
 * 如果携带groupId(long)参数打开页面，则为一个群添加群成员
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_REMOVE_FRAGMENT)
class RemoveMemberFragment : BaseFragment() {

    override val fragmentName: String
        get() = "RemoveMemberFragment"

    private val mGroupId by lazy { arguments?.getLong("groupId", 0) }

    private val mAdapter by lazy { RemoveGroupMemberAdapter() }

    private var mPageNum = 1

    private var mDisplayMemberModels = mutableListOf<GroupMemberModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bus_recycler_inside_linear, container, false)
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()

        ArouterServiceManager.groupService.syncGroupAllMemberInfoNew(null, 1, 200,
                mGroupId ?: 0, 3,{ _,hasMore, list ->
            mDisplayMemberModels.clear()
            mPageNum++
            mDisplayMemberModels.addAll(list)
            refreshData(mDisplayMemberModels, hasMore,false)
        }) {
            loadMembersByCache()
        }
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        common_recycler.initSingleTypeRecycleView(LinearLayoutManager(this@RemoveMemberFragment.context), mAdapter, true)
        common_recycler.refreshController().setEnablePullToRefresh(false)
        common_recycler.loadMoreController().setOnLoadMoreListener {
            ArouterServiceManager.groupService.syncGroupAllMemberInfoNew(null, mPageNum, 200,
                    mGroupId ?: 0, 3,{ _,hasMore, list ->
                mPageNum++
                mDisplayMemberModels.addAll(list)
                refreshData(mDisplayMemberModels, hasMore,false)
            })
        }

        mAdapter.setNewData(mDisplayMemberModels)

        EventBus.getFlowable(RemoveSelectMemberEvent::class.java)
                .bindToLifecycle(this@RemoveMemberFragment)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (activity != null) {
                        if (it.from == 2) {
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

    private fun loadMembersByCache() {
        val groupMemberInfos = ArrayList<GroupMemberModel>()
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeGroupMembersTransactionAsync(myUid, mGroupId ?: 0, { realm ->
            val groupMemberModels = realm.where(GroupMemberModel::class.java)?.sort("sortScore", Sort.ASCENDING)?.limit(Long.MAX_VALUE)?.findAll()
            groupMemberModels?.forEach {
                groupMemberInfos.add(it.copyGroupMemberModel())
            }
        }, {
            refreshData(groupMemberInfos, false,true)
        }, {
        })
    }

    private fun refreshData(list: MutableList<GroupMemberModel>,hasMore:Boolean, isCache: Boolean) {
        if (activity == null) {
            return
        }

        common_recycler.refreshController().refreshComplete()
        if (isCache) {
            common_recycler.loadMoreController().loadMoreEnd()
        } else {
            if (!hasMore) {
                common_recycler.loadMoreController().loadMoreEnd()
            } else {
                common_recycler.loadMoreController().loadMoreComplete()
            }
        }
    }

}
