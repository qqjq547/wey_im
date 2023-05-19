package framework.telegram.business.ui.group

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.facebook.common.util.UriUtil
import com.im.domain.pb.CommonProto
import com.im.domain.pb.ContactsProto
import com.im.domain.pb.GroupProto
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.group.GroupMemberModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.BuildConfig
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.event.SearchGroupOperateEvent
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.GroupHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.GroupHttpProtocol
import framework.telegram.business.ui.group.adapter.OperateGroupMemberAdapter
import framework.telegram.message.bridge.event.GroupInfoChangeEvent
import framework.telegram.support.BaseActivity
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import io.realm.Sort
import kotlinx.android.synthetic.main.bus_group_activity_create_or_add_group_member.custom_toolbar
import kotlinx.android.synthetic.main.bus_group_activity_operate_group_member.*
import kotlinx.android.synthetic.main.bus_search.*

/**
 * 显示来源为群成员
 *
 * operateType          操作类型(0显示所有成员,2@成员,3转让群主,4添加管理员)
 * groupId              群id
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_OPERATE)
class OperateGroupMemberActivity : BaseActivity() {

    companion object {
        const val OPERATE_TYPE_DISPLAY_ALL_MEMBER = 0
        const val OPERATE_TYPE_AT_MEMBER = 2
        const val OPERATE_TYPE_TRANSFER_OWNER = 3
        const val OPERATE_TYPE_ADD_ADMIN = 4
    }

    private val mRealm: Realm by lazy { RealmCreator.getGroupMembersRealm(mMineUid, mGroupId) }

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private val mOperateType by lazy { intent.getIntExtra("operateType", OPERATE_TYPE_DISPLAY_ALL_MEMBER) }

    private val mGroupId by lazy { intent.getLongExtra("groupId", 0) }

    private val mJoinFriend by lazy { intent.getBooleanExtra("joinFriend", false) }
    private val mMyRole by lazy { intent.getIntExtra("myRole", 2) }

    private val mGroupPermission by lazy { intent.getIntExtra("groupPermission", 2) }

    private val mAdapter by lazy { OperateGroupMemberAdapter(mOperateType) }

    private val mDisplayMemberModels by lazy { ArrayList<GroupMemberModel>() }

    private val mLinearLayoutManager by lazy { LinearLayoutManager(this@OperateGroupMemberActivity) }

    private var mTime = 0L

    private var mPageNum = 1

    private var mList = mutableListOf<GroupMemberModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (mGroupId <= 0) {
            finish()
            return
        }
        mTime = System.currentTimeMillis()
        setContentView(R.layout.bus_group_activity_operate_group_member)

        initView()

        syncMemberInfo()

        registerEvent()
    }

    private fun syncMemberInfo() {
        ArouterServiceManager.groupService.syncGroupAllMemberInfoNew(lifecycle(), 1, 200, mGroupId,if (mOperateType == OPERATE_TYPE_AT_MEMBER) 4 else 3, { _,hasMore, list ->
            mList.clear()
            mPageNum++
            mList.addAll(list)
            refreshData(mList, hasMore,false)
        }){
            loadMembersByCache()
        }
    }

    private fun initView() {
        mAdapter.setOnItemClickListener { _, _, position ->
            val itemData = mAdapter.getItem(position)
            itemData?.let {
                operateMember(mOperateType, itemData.copyGroupMemberModel())
            }
        }

        mAdapter.setNewData(mDisplayMemberModels)

        recycler_view_members.initSingleTypeRecycleView(mLinearLayoutManager, mAdapter, true)
        recycler_view_members.refreshController().setEnablePullToRefresh(false)
        recycler_view_members.loadMoreController().setOnLoadMoreListener {
            ArouterServiceManager.groupService.syncGroupAllMemberInfoNew(lifecycle(), mPageNum, 200, mGroupId,if (mOperateType == OPERATE_TYPE_AT_MEMBER) 4 else 3, { _,hasMore, list ->
                mPageNum++
                mList.addAll(list)
                refreshData(mList, hasMore,false)
            })
        }

        when (mOperateType) {
            OPERATE_TYPE_DISPLAY_ALL_MEMBER -> {
                custom_toolbar.showCenterTitle(getString(R.string.members_of_the_group_chat))
            }
            OPERATE_TYPE_AT_MEMBER -> {
                custom_toolbar.showCenterTitle(getString(R.string.choose_the_person_to_remind))
            }
            OPERATE_TYPE_TRANSFER_OWNER -> {
                custom_toolbar.showCenterTitle(getString(R.string.transfer_of_the_group_manager))
            }
            OPERATE_TYPE_ADD_ADMIN -> {
                custom_toolbar.showCenterTitle(getString(R.string.add_admin))
            }
        }

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        text_view_search_icon.setOnClickListener {
            when (mOperateType) {
                OPERATE_TYPE_DISPLAY_ALL_MEMBER -> {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT)
                            .withInt(Constant.Search.SEARCH_TYPE, Constant.Search.SEARCH_GROUP_ALL_MEMBER)
                            .withLong(Constant.Search.SEARCH_TARGET_ID, mGroupId).navigation()//todo
                }
                OPERATE_TYPE_AT_MEMBER -> {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT)
                            .withInt(Constant.Search.SEARCH_TYPE, Constant.Search.SEARCH_GROUP_AT_MEMBER)
                            .withLong(Constant.Search.SEARCH_TARGET_ID, mGroupId).navigation()//todo
                }
                OPERATE_TYPE_TRANSFER_OWNER -> {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT)
                            .withInt(Constant.Search.SEARCH_TYPE, Constant.Search.SEARCH_GROUP_TAN_OWNER)
                            .withLong(Constant.Search.SEARCH_TARGET_ID, mGroupId).navigation()
                }
                OPERATE_TYPE_ADD_ADMIN -> {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT)
                            .withInt(Constant.Search.SEARCH_TYPE, Constant.Search.SEARCH_GROUP_ADD_ADMIN)
                            .withLong(Constant.Search.SEARCH_TARGET_ID, mGroupId).navigation()
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun registerEvent() {
        EventBus.getFlowable(SearchGroupOperateEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .bindToLifecycle(this@OperateGroupMemberActivity)
                .subscribe {
                    operateMember(it.type, it.member.copyGroupMemberModel())
                }
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.removeAllChangeListeners()
        mRealm.close()
    }

    @SuppressLint("CheckResult")
    private fun loadMembersByCache() {
        val groupMemberInfos = ArrayList<GroupMemberModel>()
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeGroupMembersTransactionAsync(myUid, mGroupId, { realm ->
            val groupMemberModels = realm.where(GroupMemberModel::class.java)?.sort("sortScore", Sort.ASCENDING)?.limit(Long.MAX_VALUE)?.findAll()
            groupMemberModels?.forEach {
                groupMemberInfos.add(it.copyGroupMemberModel())
            }
        }, {
            AppLogcat.logger.d("demo", "获取群成员信息成功！！！")
            refreshData(groupMemberInfos, false,true)
        }, {
        })
    }

    private fun refreshData(list: MutableList<GroupMemberModel>,hasMore:Boolean, isCache: Boolean) {
        mDisplayMemberModels.clear()

        if (mOperateType == OPERATE_TYPE_AT_MEMBER && mGroupPermission < 2) {
            mDisplayMemberModels.add(GroupMemberModel.createGroupMember(-1, getString(R.string.whole_members), UriUtil.getUriForResourceId(R.drawable.common_contacts_icon_group).toString()))
        }

        list.forEach {
            when (mOperateType) {
                OPERATE_TYPE_TRANSFER_OWNER,
                OPERATE_TYPE_AT_MEMBER -> {
                    if (it.uid != mMineUid) {
                        mDisplayMemberModels.add(it)
                    }
                }
                OPERATE_TYPE_ADD_ADMIN -> {
                    if (it.uid != mMineUid && it.type != CommonProto.GroupMemberType.HOST.number && it.type != CommonProto.GroupMemberType.MANAGE.number) {
                        mDisplayMemberModels.add(it)
                    }
                }
                else -> {
                    mDisplayMemberModels.add(it)
                }
            }
        }

        if (mDisplayMemberModels.isNotEmpty()) {
            mAdapter.notifyDataSetChanged()
        } else {
            if (mOperateType != OPERATE_TYPE_ADD_ADMIN) {
                finish()
            }
        }

        recycler_view_members.refreshController().refreshComplete()
        if (isCache) {
            recycler_view_members.loadMoreController().loadMoreEnd()
        } else {
            if (!hasMore) {
                recycler_view_members.loadMoreController().loadMoreEnd()
            } else {
                recycler_view_members.loadMoreController().loadMoreComplete()
            }
        }
    }

    private fun operateMember(type: Int, member: GroupMemberModel) {
        when (type) {
            OPERATE_TYPE_TRANSFER_OWNER -> {
                // 转让群
                groupTransfer(member)
            }
            OPERATE_TYPE_DISPLAY_ALL_MEMBER -> {
                // 查看成员信息
                val isForbidJoinFriend =
                        if (mJoinFriend) {
                            mMyRole == 2 && member.type == 2
                        } else false
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACT_DETAIL)
                        .withLong(Constant.ARouter_Key.KEY_TARGET_GID, mGroupId)
                        .withBoolean("isForbidJoinFriend", isForbidJoinFriend)
                        .withSerializable(Constant.ARouter_Key.KEY_ADD_FRIEND_FROM, ContactsProto.ContactsAddType.CROWD)
                        .withLong(Constant.ARouter_Key.KEY_TARGET_UID, member.uid)
                        .withString(Constant.ARouter_Key.KEY_GROUP_NICKNAME, member.groupNickName)
                        .navigation()
            }
            OPERATE_TYPE_AT_MEMBER -> {
                // @成员
                val result = Intent()
                result.putExtra("atUids", member.uid.toString())
                result.putExtra("atName", member.displayName)
                setResult(Activity.RESULT_OK, result)
                finish()
            }
            OPERATE_TYPE_ADD_ADMIN -> {
                // 增加管理员
                val result = Intent()
                result.putExtra(Constant.ARouter_Key.KEY_TARGET_UID, member.uid)
                result.putExtra("displayName", member.displayName)
                result.putExtra("memberIcon", member.icon)
                result.putExtra("onlineStatus", member.isOnlineStatus)
                result.putExtra("lastOnlineTime", member.lastOnlineTime)
                result.putExtra("isShowLastOnlineTime", member.isShowLastOnlineTime)
                setResult(Activity.RESULT_OK, result)
                finish()
            }
        }
    }

    private fun groupTransfer(member: GroupMemberModel) {
        AppDialog.show(this@OperateGroupMemberActivity, this@OperateGroupMemberActivity) {
            positiveButton(text = getString(R.string.confirm), click = {
                val appDialog = AppDialog.showLoadingView(this@OperateGroupMemberActivity, this@OperateGroupMemberActivity)
                HttpManager.getStore(GroupHttpProtocol::class.java)
                        .groupTransfer(object : HttpReq<GroupProto.GroupTransferReq>() {
                            override fun getData(): GroupProto.GroupTransferReq {
                                return GroupHttpReqCreator.createGroupTransferReq(mGroupId, member.uid)
                            }
                        })
                        .getResult(lifecycle(), {
                            appDialog.dismiss()
                            EventBus.publishEvent(GroupInfoChangeEvent(mGroupId))
                            setResult(Activity.RESULT_OK)
                            finish()
                        }, {
                            appDialog.dismiss()

                            if (BuildConfig.DEBUG) {
                                toast(String.format(getString(R.string.transfer_group_failure_sign), it.message))
                            } else {
                                toast(getString(R.string.transfer_group_failure))
                            }
                        })
            })
            negativeButton(text = getString(R.string.cancel))
            title(text = getString(R.string.transfer_group_chat))
            message(text = String.format(getString(R.string.transfer_group_chat_message_one), member.displayName))
        }
    }
}
