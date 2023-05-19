package framework.telegram.business.ui.group

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.GroupProto
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
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.GroupHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.GroupHttpProtocol
import framework.telegram.business.ui.group.adapter.RemoveGroupMemberAdapter
import framework.telegram.business.ui.widget.PopWindow.CommonPopupWindow
import framework.telegram.support.BaseActivity
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.ActivitiesHelper
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.bus_group_activity_remove_member.*
import kotlinx.android.synthetic.main.bus_group_activity_remove_member.custom_toolbar
import kotlinx.android.synthetic.main.bus_search.*
import framework.telegram.support.tools.ExpandClass.toast


/**
 * 显示来源为群成员
 * 这个准备用 popupWindow来实现，之后再改，先用fragment来实现
 */
@Deprecated("这个准备用 popupWindow来实现，之后再改，先用fragment来实现")
class RemoveGroupMemberActivity1 : BaseActivity(), RealmChangeListener<RealmResults<GroupMemberModel>>, CommonPopupWindow.ViewInterface {

    private val mRealm: Realm by lazy { RealmCreator.getGroupMembersRealm(mMineUid, mGroupId) }

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private val mGroupId by lazy { intent.getLongExtra("groupId", 0) }

    private val mAdapter by lazy { RemoveGroupMemberAdapter() }

    private val mSearchAdapter by lazy { RemoveGroupMemberAdapter() }

    private var mAllModels: RealmResults<GroupMemberModel>? = null

    private val mDisplayMemberModels by lazy { ArrayList<GroupMemberModel>() }

    private var popupWindow: CommonPopupWindow? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (mGroupId <= 0) {
            finish()
            return
        }

        setContentView(R.layout.bus_group_activity_remove_member)

        initView()

        loadMembers()

        syncMemberInfo()
    }

    private fun syncMemberInfo() {
//        ArouterServiceManager.groupService.syncGroupAllMemberInfo(lifecycle(), mGroupId)
    }

    private fun initView() {
        mAdapter.setNewData(mDisplayMemberModels)
//        recycler_view_members?.adapter = mAdapter
//        recycler_view_members?.layoutManager = LinearLayoutManager(this@RemoveGroupMemberActivity1)

        custom_toolbar.showCenterTitle(getString(R.string.remove_element))

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        custom_toolbar.showRightTextView(getString(R.string.confirm), {
            val uids = mAdapter.getSelectedUids()
            if (uids.isNotEmpty()) {
                // 删除成员
                HttpManager.getStore(GroupHttpProtocol::class.java)
                        .updateGroupMember(object : HttpReq<GroupProto.GroupMemberReq>() {
                            override fun getData(): GroupProto.GroupMemberReq {
                                return GroupHttpReqCreator.createUpdateGroupMemberReq(mGroupId, GroupProto.GroupOperator.DEL, uids)
                            }
                        })
                        .getResult(lifecycle(), {
                            AppLogcat.logger.d("demo", getString(R.string.group_member_deleted_successfully))
                            finish()
                        }, {
                            toast(getString(R.string.failed_to_delete_group_members))
                        })
            }
        })

        text_view_search_icon.setOnClickListener {
            showDownPop(common_recycler_head)
        }
    }

    private fun showDownPop(view: View) {
        if (popupWindow != null && popupWindow?.isShowing == true) return;
        popupWindow = CommonPopupWindow.Builder(this)
                .setView(R.layout.bus_recycler_inside_linear)
                .setWidthAndHeight(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                .setViewOnclickListener(this)
                .setOutsideTouchable(true)
                .create()
        popupWindow?.showAsDropDown(view)
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.removeAllChangeListeners()
        mRealm.close()
    }

    @SuppressLint("CheckResult")
    private fun loadMembers() {
        Flowable.just<Realm>(mRealm)
                .bindToLifecycle(this@RemoveGroupMemberActivity1)
                .subscribeOn(AndroidSchedulers.mainThread())
                .map { it.where(GroupMemberModel::class.java)?.sort("sortScore", Sort.ASCENDING)?.findAllAsync() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    mAllModels = it
                    mAllModels?.addChangeListener(this@RemoveGroupMemberActivity1)
                }
    }

    override fun onChange(t: RealmResults<GroupMemberModel>) {
        if (!t.isValid)
            return

        if (ActivitiesHelper.isDestroyedActivity(this@RemoveGroupMemberActivity1)) {
            return
        }

        mDisplayMemberModels.clear()

        t.forEach {
            if (it.uid != mMineUid) {
                mDisplayMemberModels.add(it)
            }
        }

        if (mDisplayMemberModels.isNotEmpty()) {
            mAdapter.notifyDataSetChanged()
        } else {
            finish()
        }
    }


    private fun loadSearchData() {
//        Flowable.just<Realm>(mRealm).subscribeOn(AndroidSchedulers.mainThread())
//                .map {
//                    it.where(GroupMemberModel::class.java).like("nickName", "*$keyword*")?.findAllAsync()
//                }
//                .observeOn(AndroidSchedulers.mainThread()).subscribe {
//                    it?.let{ t ->
//                        val list = mutableListOf<MultiItemEntity>()
//                        t.forEach { info ->
//                            list.add(info)
//                        }
//                        dataList.addAll(list)
//                        mView.getDataListSuccess(dataList)
//                    }
//                }
    }

//    private val mSearchDataList

    override fun getChildView(view: View?, layoutResId: Int) {
        if (R.layout.bus_recycler_inside_linear == layoutResId) {

        }

        mSearchAdapter
    }

}
