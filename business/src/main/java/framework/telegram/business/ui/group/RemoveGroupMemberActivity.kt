package framework.telegram.business.ui.group

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.GroupProto
import com.trello.rxlifecycle3.kotlin.bindToLifecycle
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_REMOVE_FRAGMENT
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_REMOVE_SEARCH_FRAGMENT
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.event.RemoveGroupSearchMemberEvent
import framework.telegram.business.event.RemoveSelectMemberEvent
import framework.telegram.business.http.HttpException
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.GroupHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.GroupHttpProtocol
import framework.telegram.business.ui.contacts.OperateContactActivity
import framework.telegram.business.ui.group.adapter.OperateGroupMemberItemAdapter
import framework.telegram.message.bridge.event.GroupMemberChangeEvent
import framework.telegram.support.BaseActivity
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.KeyboardktUtils
import framework.telegram.ui.utils.ScreenUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_group_activity_remove_member.*


/**
 * 显示来源为群成员
 *
 * operateType          操作类型(0显示所有成员,1删除成员,2@成员,3转让群主)
 * groupId              群id
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_REMOVE)
class RemoveGroupMemberActivity : BaseActivity() {

    companion object {
        const val REMOVE = "REMOVE"
        const val SEARCH = "SEARCH"
    }

    private val mMineUid by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
    }

    private val mGroupId by lazy { intent.getLongExtra("groupId", 0) }

    private val mAdapter by lazy {
        OperateGroupMemberItemAdapter {
            EventBus.publishEvent(
                RemoveSelectMemberEvent(
                    mSelectedUsers[it].uid,
                    mSelectedUsers[it].icon,
                    2,
                    3
                )
            )
        }
    }

    private val mSelectedUsers by lazy { java.util.ArrayList<OperateContactActivity.SelectUser>() }

    private var mCurrentFragment: Fragment? = null

    private var mCurrentTag = REMOVE

    private var mKeyword = ""

    private var mSureTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (mGroupId <= 0) {
            finish()
            return
        }

        setContentView(R.layout.bus_group_activity_remove_member)
        KeyboardktUtils.hideKeyboard(this.linear_layout_all)

        initView()
        initData()
        registerEvent()
    }

    private fun initData() {
    }

    private fun initView() {
        initToolsBar()
        initRecyclerView()
        replaceFragment(mCurrentTag)
    }

    @SuppressLint("CheckResult")
    private fun registerEvent() {
        EventBus.getFlowable(RemoveSelectMemberEvent::class.java)
            .bindToLifecycle(this@RemoveGroupMemberActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.op == 1) {
                    addToSelectedUser(it.uid, it.pic, "")
                    mAdapter.setNewData(mSelectedUsers)
                } else {
                    removeSelectedUser(it.uid)
                    mAdapter.setNewData(mSelectedUsers)
                }
                setSureBottom()
            }

        EventBus.getFlowable(RemoveGroupSearchMemberEvent::class.java)
            .bindToLifecycle(this@RemoveGroupMemberActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.from == 2) {
                    custom_search_bar.setSearchText(it.keyword)
                }
            }
    }

    private fun initToolsBar() {
        custom_search_bar.setSearBarListen {
            mKeyword = it
            if (TextUtils.isEmpty(it)) {
                KeyboardktUtils.hideKeyboard(this.linear_layout_all)
                if (mCurrentTag != REMOVE)
                    replaceFragment(REMOVE)
            } else {
                if (mCurrentTag != SEARCH)
                    replaceFragment(SEARCH)
            }
            EventBus.publishEvent(RemoveGroupSearchMemberEvent(it, 1))
        }
        custom_toolbar.showCenterTitle(getString(R.string.remove_element))
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        custom_toolbar.showRightTextView("", {
            val uids = getSelectedUids()
            if (!uids.isNullOrEmpty()) {
                AppDialog.show(this@RemoveGroupMemberActivity, this@RemoveGroupMemberActivity) {
                    message(
                        text = String.format(
                            getString(R.string.make_sure_you_want_to_delete_the_selected),
                            uids.size
                        )
                    )
                    negativeButton(R.string.common_cancel)
                    positiveButton(text = getString(R.string.confirm), click = {
                        // 删除成员
                        HttpManager.getStore(GroupHttpProtocol::class.java)
                            .updateGroupMember(object : HttpReq<GroupProto.GroupMemberReq>() {
                                override fun getData(): GroupProto.GroupMemberReq {
                                    return GroupHttpReqCreator.createUpdateGroupMemberReq(
                                        mGroupId,
                                        GroupProto.GroupOperator.DEL,
                                        uids
                                    )
                                }
                            })
                            .getResult(lifecycle(), {
                                toast(getString(R.string.group_member_deleted_successfully))

                                uids.forEach { uid ->
                                    ArouterServiceManager.groupService.deleteGroupMemberInfo(
                                        mGroupId,
                                        uid, {
                                            EventBus.publishEvent(GroupMemberChangeEvent(mGroupId))
                                        })
                                }

                                finish()
                            }, {
                                if (it is HttpException) {
                                    toast(it.errMsg)
                                } else {
                                    toast(getString(R.string.failed_to_delete_group_members))
                                }
                            })
                    })
                }
            }
        }) {
            val size = ScreenUtils.dp2px(this, 10f)
            mSureTextView = it
            val lp = mSureTextView?.layoutParams as LinearLayout.LayoutParams
            lp.rightMargin = ScreenUtils.dp2px(this, 10f)
            mSureTextView?.setPadding(size, 0, size, 0)
            setSureBottom()
        }
    }

    private fun initRecyclerView() {
        common_recycler_head?.initSingleTypeRecycleView(
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            ), mAdapter, false
        )
    }

    private fun getSelectedUids(): List<Long> {
        val uids = mutableListOf<Long>()
        mSelectedUsers.forEach {
            uids.add(it.uid)
        }
        return uids
    }

    private fun addToSelectedUser(uid: Long, pic: String, name: String) {
        var position = -1
        mSelectedUsers.forEachIndexed { index, selectUser ->
            if (uid == selectUser.uid) {
                position = index
            }
        }

        if (position < 0) {
            mSelectedUsers.add(OperateContactActivity.SelectUser(uid, pic, ""))
        }
    }

    private fun removeSelectedUser(uid: Long) {
        var position = -1
        mSelectedUsers.forEachIndexed { index, selectUser ->
            if (uid == selectUser.uid) {
                position = index
            }
        }

        if (position >= 0) {
            mSelectedUsers.removeAt(position)
        }
    }

    private fun setSureBottom() {
        if (mSelectedUsers.size == 0) {
            mSureTextView?.setTextColor(getSimpleColor(R.color.d4d6d9))
            mSureTextView?.text = getString(R.string.confirm)
        } else {
            mSureTextView?.setTextColor(getSimpleColor(R.color.c178aff))
            mSureTextView?.text =
                String.format(getString(R.string.confirm_match), mSelectedUsers.size)
        }
    }

    private fun replaceFragment(tag: String) {
        mCurrentTag = tag
        if (mCurrentFragment != null) {
            supportFragmentManager.beginTransaction().hide(mCurrentFragment!!).commit()
        }
        mCurrentFragment = supportFragmentManager?.findFragmentByTag(tag)
        if (mCurrentFragment == null) {
            when (tag) {
                REMOVE -> mCurrentFragment =
                    ARouter.getInstance().build(ROUNTE_BUS_GROUP_MEMBER_REMOVE_FRAGMENT)
                        .withLong("groupId", mGroupId).navigation() as Fragment
                SEARCH -> mCurrentFragment =
                    ARouter.getInstance().build(ROUNTE_BUS_GROUP_MEMBER_REMOVE_SEARCH_FRAGMENT)
                        .withParcelableArrayList(
                            "selectList",
                            getSelectedUids() as ArrayList<Parcelable>
                        )
                        .withLong("groupId", mGroupId).withString("keyword", mKeyword)
                        .navigation() as Fragment
            }

            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout_add_and_search, mCurrentFragment!!, tag).commit()
        } else {
            supportFragmentManager.beginTransaction().show(mCurrentFragment!!).commit()
        }
    }
}
