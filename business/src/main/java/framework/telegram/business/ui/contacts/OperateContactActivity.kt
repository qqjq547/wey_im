package framework.telegram.business.ui.contacts

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.GroupProto
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.BuildConfig
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE_FRAGMENT
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE_SEARCH_FRAGMENT
import framework.telegram.business.bridge.bean.SelectedUsersModel
import framework.telegram.business.event.CreateGroupAllSelectMemberEvent
import framework.telegram.business.event.CreateGroupSearchMemberEvent
import framework.telegram.business.event.CreateGroupSelectMemberEvent
import framework.telegram.business.event.CreateGroupSuccessEvent
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.GroupHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.GroupHttpProtocol
import framework.telegram.business.ui.contacts.adapter.OperateContactItemAdapter
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_PRIVATE_SEND_CHAT_ACTIVITY
import framework.telegram.support.BaseActivity
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.KeyboardktUtils
import framework.telegram.ui.utils.ScreenUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_group_activity_create_or_add_group_member.*


/**
 * 显示来源为联系人
 *
 * 如果携带groupId(long)参数打开页面，则为一个群添加群成员
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE)
class OperateContactActivity : BaseActivity() {

    companion object {
        const val ADD_AND_CREATE = "ADD_AND_CREATE"
        const val SEARCH = "SEARCH"
    }

    // 1创建群 2添加群成员 3添加到不可见列表(黑名单)  4.群发
    private val mOperate by lazy { intent.getIntExtra("operate", 1) }

    private val mGroupId by lazy { intent.getLongExtra("groupId", 0) }

    private val mGroupShowTip by lazy { intent.getBooleanExtra("groupTip", false) }

    private val mAddedUserId by lazy { intent.getLongExtra("addedUserId", 0) }

    private val mAddUserIcon by lazy { intent.getStringExtra("addedUserIcon") ?: "" }

    private val mAddUserName by lazy { intent.getStringExtra("addUserName") ?: "" }

    private val mAdapter by lazy {
        OperateContactItemAdapter {
            EventBus.publishEvent(
                CreateGroupSelectMemberEvent(
                    mSelectedUsers[it].uid,
                    mSelectedUsers[it].icon,
                    mSelectedUsers[it].name,
                    2,
                    3
                )
            )
        }
    }

    private val mSelectedUsers by lazy { ArrayList<SelectUser>() }

    private var mCurrentFragment: Fragment? = null

    private var mCurrentTag = ADD_AND_CREATE

    private var mKeyword = ""

    private var mSureTextView: TextView? = null

    private var mDownAnimation: TranslateAnimation? = null
    private var mUpAnimation: TranslateAnimation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (mOperate == 2 && mGroupId <= 0) {
            finish()
            return
        }

        setContentView(R.layout.bus_group_activity_create_or_add_group_member)
        KeyboardktUtils.hideKeyboard(this.linear_layout_all)
        initView()
        registerEvent()

        // 已有添加成员，显示确定按钮
        if (mAddedUserId > 0) {
            setSureButtom()
        }
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        initToolsBar()
        initRecyclerView()
        replaceFragment(mCurrentTag)

        if (mGroupShowTip) {
            linear_layout_group_tip.visibility = View.VISIBLE
        }

        image_view_close.setOnClickListener {
            linear_layout_group_tip.visibility = View.GONE
        }

        mDownAnimation = TranslateAnimation(0f, 0f, 0f, ScreenUtils.dp2px(this, 60f).toFloat())
        mDownAnimation?.duration = 300
        mDownAnimation?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {

            }

            override fun onAnimationEnd(animation: Animation?) {
                frame_layout_add_and_search.clearAnimation()
                mAdapter.setNewData(mSelectedUsers)
            }

            override fun onAnimationStart(animation: Animation?) {
            }
        })
        mUpAnimation = TranslateAnimation(0f, 0f, ScreenUtils.dp2px(this, 60f).toFloat(), 0f)
        mUpAnimation?.duration = 300
        mUpAnimation?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {

            }

            override fun onAnimationEnd(animation: Animation?) {
                frame_layout_add_and_search.clearAnimation()
            }

            override fun onAnimationStart(animation: Animation?) {
            }
        })
    }

    private fun initToolsBar() {
        custom_search_bar.setSearBarListen({
            mKeyword = it
            if (TextUtils.isEmpty(it)) {
                KeyboardktUtils.hideKeyboard(this.linear_layout_all)
                if (mCurrentTag != ADD_AND_CREATE)
                    replaceFragment(ADD_AND_CREATE)
            } else {
                if (mCurrentTag != SEARCH)
                    replaceFragment(SEARCH)
            }
            EventBus.publishEvent(CreateGroupSearchMemberEvent(it, 1))
        }) {
            val fragment =
                supportFragmentManager?.findFragmentByTag(ADD_AND_CREATE) as OperateContactFragment
            fragment.selectAllContact(!fragment.getIsAllSelect())
            toolbarSelectStatu()
        }
        custom_toolbar.showCenterTitle(getString(R.string.select_contact))
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showRightTextView("", {
            operateContact()
        }) {
            val size = ScreenUtils.dp2px(this, 10f)
            mSureTextView = it
            val lp = mSureTextView?.layoutParams as LinearLayout.LayoutParams
            lp.rightMargin = ScreenUtils.dp2px(this, 10f)
            mSureTextView?.setPadding(size, 0, size, 0)
            setSureButtom()
        }
        if (mOperate == 4) {
            custom_search_bar.setAllSelect(getString(R.string.string_all_select))
        }
    }

    /**
     * 全选/取消全选
     */
    private fun toolbarSelectStatu() {
        val fragment =
            supportFragmentManager?.findFragmentByTag(ADD_AND_CREATE) as OperateContactFragment
        if (fragment.getIsAllSelect()) {
            custom_search_bar.setAllSelect(getString(R.string.string_all_unselect))
        } else {
            custom_search_bar.setAllSelect(getString(R.string.string_all_select))
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

    private fun operateContact() {
        if (mSelectedUsers.isNotEmpty()) {
            if (mOperate == 1) {
                val list = arrayListOf<SelectedUsersModel>()
                if (mAddedUserId != 0L)
                    list.add(SelectedUsersModel(mAddedUserId, mAddUserIcon, mAddUserName))
                mSelectedUsers.forEach {
                    val tmp = SelectedUsersModel(it.uid, it.icon, it.name)
                    list.add(tmp)
                }
                if (list.size >= 2) {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_CREATE)
                        .withParcelableArrayList("selectList", list)
                        .navigation()
                } else if (list.size == 1) {
                    ARouter.getInstance().build(ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY)
                        .withLong("targetUid", list[0].uid).navigation()
                }
            } else if (mOperate == 2) {
                //添加群成员
                if (mGroupId > 0) {
                    addGroupMember()
                }
            } else if (mOperate == 4) {
                val list = arrayListOf<SelectedUsersModel>()
                mSelectedUsers.forEach {
                    val tmp = SelectedUsersModel(it.uid, it.icon, it.name)
                    list.add(tmp)
                }
                if (list.size > 0) {
                    Constant.TmpData.groupSendContactList = list
                    ARouter.getInstance().build(ROUNTE_MSG_PRIVATE_SEND_CHAT_ACTIVITY).navigation()
                }
            } else {
                addDisViewOnlineList()
            }
        }
    }

    private fun getSelectedUids(): MutableList<Long> {
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
            mSelectedUsers.add(SelectUser(uid, pic, name))
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

    private fun addDisViewOnlineList() {
        val appDialog =
            AppDialog.showLoadingView(this@OperateContactActivity, this@OperateContactActivity)
        val uids = getSelectedUids()
        ArouterServiceManager.contactService.addDisShowOnlineContacts(lifecycle(), uids, {
            appDialog.dismiss()
            finish()
        }, {
            appDialog.dismiss()
            if (BuildConfig.DEBUG) {
                toast(getString(R.string.adding_invisible_failed_sign) + it.message)
            } else {
                toast(getString(R.string.adding_invisible_failed))
            }
        })
    }

    private fun addGroupMember() {
        val appDialog =
            AppDialog.showLoadingView(this@OperateContactActivity, this@OperateContactActivity)
        val uids = getSelectedUids()
        ArouterServiceManager.contactService.getContactsInfoCache(
            lifecycle(),
            uids,
            { contactModels ->
                HttpManager.getStore(GroupHttpProtocol::class.java)
                    .updateGroupMember(object : HttpReq<GroupProto.GroupMemberReq>() {
                        override fun getData(): GroupProto.GroupMemberReq {
                            return GroupHttpReqCreator.createUpdateGroupMemberReq(
                                mGroupId,
                                GroupProto.GroupOperator.ADD,
                                uids
                            )
                        }
                    })
                    .getResult(lifecycle(), {
                        appDialog.dismiss()

                        val notFriendUidsList = it.notFriendUidsList
                        val needCheckUidsList = it.needCheckUidsList
                        val notFriendList = ArrayList<ContactDataModel>()
                        val needCheckList = ArrayList<ContactDataModel>()
                        if (!notFriendUidsList.isNullOrEmpty() || !needCheckUidsList.isNullOrEmpty()) {
                            contactModels.forEach { contactModel ->
                                if (notFriendUidsList.contains(contactModel.uid)) {
                                    notFriendList.add(contactModel)
                                } else if (needCheckUidsList.contains(contactModel.uid)) {
                                    needCheckList.add(contactModel)
                                }
                            }
                        }

                        if (notFriendList.isNotEmpty() || needCheckList.isNotEmpty()) {
                            // 提示未成功邀请的联系人
                            val tipMsg = getTipMessage(
                                getContactsNames(needCheckList),
                                getContactsNames(notFriendList)
                            )
                            showCreateGroupTipDialog(getString(R.string.request_sent), tipMsg) {
                                finish()
                            }
                        } else {
                            finish()
                        }
                    }, {
                        appDialog.dismiss()
                        toast(it.message.toString())//getString(R.string.failed_to_add_group_member)
                    })
            })
    }

    private fun getContactsNames(list: List<ContactDataModel>): String {
        val sb = StringBuilder()
        list.forEachIndexed { index, contactModel ->
            val displayName = contactModel.displayName
            when {
                index == 0 -> sb.append(displayName)
                index == 5 -> sb.append(getString(R.string.rank))
                index < 5 -> sb.append("、").append(displayName)
            }
        }
        return sb.toString()
    }

    private fun getTipMessage(needCheckUserNames: String, notFriendUserNames: String): String {
        return if (!TextUtils.isEmpty(needCheckUserNames) && !TextUtils.isEmpty(notFriendUserNames)) {
            String.format(
                getString(R.string.tip_message_one),
                notFriendUserNames,
                needCheckUserNames
            )
        } else if (!TextUtils.isEmpty(needCheckUserNames)) {
            String.format(getString(R.string.tip_message_three), needCheckUserNames)
        } else {
            String.format(getString(R.string.tip_message_four), notFriendUserNames)
        }
    }

    private fun showCreateGroupTipDialog(title: String, tipMsg: String, clickButton: () -> Unit) {
        AppDialog.show(this@OperateContactActivity, this@OperateContactActivity) {
            positiveButton(text = getString(R.string.confirm), click = {
                clickButton.invoke()
            })
            title(text = title)
            cancelOnTouchOutside(false)
            cancelable(false)
            message(text = tipMsg)
        }
    }

    @SuppressLint("CheckResult")
    private fun registerEvent() {
        EventBus.getFlowable(CreateGroupSelectMemberEvent::class.java)
            .bindToLifecycle(this@OperateContactActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.op == 1) {
                    addToSelectedUser(it.uid, it.pic, it.name)
                    if (mSelectedUsers.size == 1) {
                        frame_layout_add_and_search.startAnimation(mDownAnimation)
                    } else {
                        mAdapter.setNewData(mSelectedUsers)
                    }
                } else {
                    removeSelectedUser(it.uid)
                    mAdapter.setNewData(mSelectedUsers)
                    if (mSelectedUsers.size == 0) {
                        frame_layout_add_and_search.startAnimation(mUpAnimation)
                    }
                }
                setSureButtom()
                if (mOperate == 4) {
                    toolbarSelectStatu()
                }
            }

        EventBus.getFlowable(CreateGroupAllSelectMemberEvent::class.java)
            .bindToLifecycle(this@OperateContactActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.op == 1) {
                    it.uidList.forEachIndexed { index, id ->
                        addToSelectedUser(id, it.picList[index], it.nameList[index])
                    }
                    if (mSelectedUsers.size == 1) {
                        frame_layout_add_and_search.startAnimation(mDownAnimation)
                    } else {
                        mAdapter.setNewData(mSelectedUsers)
                    }
                } else {
                    it.uidList.forEachIndexed { index, id ->
                        removeSelectedUser(id)
                    }
                    mAdapter.setNewData(mSelectedUsers)
                    if (mSelectedUsers.size == 0) {
                        frame_layout_add_and_search.startAnimation(mUpAnimation)
                    }
                }
                setSureButtom()
                if (mOperate == 4) {
                    toolbarSelectStatu()
                }
            }

        EventBus.getFlowable(CreateGroupSearchMemberEvent::class.java)
            .bindToLifecycle(this@OperateContactActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.from == 2) {
                    custom_search_bar.setSearchText(it.keyword)
                }
            }

        EventBus.getFlowable(CreateGroupSuccessEvent::class.java)
            .bindToLifecycle(this@OperateContactActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                finish()
            }
    }

    private fun setSureButtom() {
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
                ADD_AND_CREATE -> mCurrentFragment =
                    ARouter.getInstance().build(ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE_FRAGMENT)
                        .withInt("operate", mOperate)
                        .withLong("groupId", mGroupId)
                        .withLong("addedUserId", mAddedUserId)
                        .navigation() as Fragment
                SEARCH -> mCurrentFragment = ARouter.getInstance()
                    .build(ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE_SEARCH_FRAGMENT)
                    .withParcelableArrayList(
                        "selectList",
                        getSelectedUids() as ArrayList<Parcelable>
                    )
                    .withInt("operate", mOperate)
                    .withLong("groupId", mGroupId)
                    .withLong("addedUserId", mAddedUserId)
                    .withString("keyword", mKeyword)
                    .navigation() as Fragment
            }

            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout_add_and_search, mCurrentFragment!!, tag).commit()
        } else {
            supportFragmentManager.beginTransaction().show(mCurrentFragment!!).commit()
        }
    }

    class SelectUser(val uid: Long, val icon: String, val name: String)
}
