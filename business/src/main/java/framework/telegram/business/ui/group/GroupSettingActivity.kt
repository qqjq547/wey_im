package framework.telegram.business.ui.group

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.ContactsProto
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_ADD_FRIEND_FROM
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.ui.contacts.bean.GroupMemberItemBean
import framework.telegram.business.ui.group.adapter.GroupMemberAdapter
import framework.telegram.business.ui.qr.GroupQrActivity.Companion.CHAT_GROUP_ICON
import framework.telegram.business.ui.qr.GroupQrActivity.Companion.CHAT_GROUP_ID
import framework.telegram.business.ui.qr.GroupQrActivity.Companion.CHAT_GROUP_NAME
import framework.telegram.business.ui.qr.GroupQrActivity.Companion.CHAT_GROUP_OWNER
import framework.telegram.message.bridge.event.*
import framework.telegram.support.BaseActivity
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_group_activity_group_setting.*

@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_SETTING)
class GroupSettingActivity : BaseActivity() {

    companion object {
        const val REQUEST_CODE_MANAGE = 0x1000
        const val REQUEST_CODE_INFO = 0x1002
        const val EDIT_NAME_REQUEST_CODE = 0x1001
    }

    private val mGroupId by lazy { intent.getLongExtra("groupId", 0) }
    private var mGroupInfoModel: GroupInfoModel? = null

    private val mAdapter by lazy { GroupMemberAdapter() }

    private val mMineUid by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
    }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (mGroupId <= 0) {
            finish()
            return
        }

        setContentView(R.layout.bus_group_activity_group_setting)
        initView()
        initData()

        EventBus.getFlowable(GroupMemberChangeEvent::class.java)
            .bindToLifecycle(this@GroupSettingActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.groupId == mGroupId) {
                    refreshGroupMemberList()
                }
            }

        EventBus.getFlowable(GroupInfoChangeEvent::class.java)
            .bindToLifecycle(this@GroupSettingActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.groupId == mGroupId) {
                    refreshGroupInfo(syncGroupInfo = true, syncMembers = false)
                }
            }

        EventBus.getFlowable(SearchChatEvent::class.java)
            .bindToLifecycle(this@GroupSettingActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.chatType == ChatModel.CHAT_TYPE_GROUP || it.targetId == mGroupId) {
                    finish()
                }
            }

        EventBus.getFlowable(BanGroupMessageEvent::class.java)
            .bindToLifecycle(this@GroupSettingActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.groupId == mGroupId) {
                    finish()
                }
            }

        EventBus.getFlowable(DisableGroupMessageEvent::class.java)
            .bindToLifecycle(this@GroupSettingActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.groupId == mGroupId) {
                    if (ActivitiesHelper.getInstance().topActivity == this@GroupSettingActivity) {
                        AppDialog.show(this@GroupSettingActivity, this@GroupSettingActivity) {
                            positiveButton(text = getString(R.string.confirm), click = {
                                //清空聊天记录
                                finish()
                            })
                            cancelOnTouchOutside(false)
                            message(text = getString(R.string.string_group_dismiss_title))
                        }
                    } else {
                        finish()
                    }
                }
            }
    }

    private fun refreshGroupInfo(syncGroupInfo: Boolean, syncMembers: Boolean) {
        if (syncGroupInfo) {
            // 获取群信息
            ArouterServiceManager.groupService.updateGroupInfo(
                lifecycle(),
                mGroupId,
                { groupInfoModel ->
                    mGroupInfoModel = groupInfoModel
                    refreshGroupInfoView()
                    updateGroupManager()
                },
                { groupInfoModel ->
                    mGroupInfoModel = groupInfoModel
                    refreshGroupInfoView()
                    updateGroupManager()
                })
        }

        if (syncMembers) {
            updateGroupManager()
            syncGroupAllMemberInfo()
        }
    }

    private fun refreshGroupMemberList() {
        ArouterServiceManager.groupService.updateGroupInfoByCache(
            lifecycle(),
            mGroupId,
            { groupInfoModel ->
                mGroupInfoModel = groupInfoModel
                updateGroupManager()
                refreshMember()
            })
    }

    private fun syncGroupAllMemberInfo() {
        ArouterServiceManager.groupService.syncGroupAllMemberInfoNew(
            lifecycle(),
            1,
            100,
            mGroupId,
            2,
            { _, _, _ ->
                refreshMember()
            },
            { _, _ ->
                refreshMember()
            },
            {
                refreshMember()
            })
    }

    private fun refreshMember() {
        ArouterServiceManager.groupService.getAllGroupMembersInfoByCache(
            mGroupId,
            9,
            { groupInfoModels, _ ->
                val list = mutableListOf<GroupMemberItemBean>()
                groupInfoModels.forEach {
                    list.add(GroupMemberItemBean.createGroupMember(it))
                }

                if ((mGroupInfoModel?.memberRole ?: 2) < 2) {
                    if (list.size == 9) {
                        list.remove(list.last())
                    }
                    // 添加"+"操作
                    list.add(GroupMemberItemBean.createAddMemberOperate())
                    // 添加"-"操作
                    list.add(GroupMemberItemBean.createDelMemberOperate())
                } else {
                    // 添加"+"操作
                    list.add(GroupMemberItemBean.createAddMemberOperate())
                }

                mAdapter.replaceData(list)
                //设置群成员数量
                setting_item_view_0.setData(
                    name = getString(R.string.cluster_member),
                    value = String.format(
                        getString(R.string.general_mat),
                        mGroupInfoModel?.memberCount
                    ),
                    listen = {
                        val joinFriend = mGroupInfoModel?.forbidJoinFriend == true
                        ARouter.getInstance()
                            .build(Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_OPERATE)
                            .withInt(
                                "operateType",
                                OperateGroupMemberActivity.OPERATE_TYPE_DISPLAY_ALL_MEMBER
                            )
                            .withBoolean("joinFriend", joinFriend)
                            .withInt("myRole", mGroupInfoModel?.memberRole ?: 2)
                            .withLong("groupId", mGroupId)
                            .navigation()
                    })
            },
            {
                finish()
            })
    }

    private fun initData() {
        refreshGroupInfo(syncGroupInfo = true, syncMembers = true)
    }

    private fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        //群公告
        ll_notice.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_NOTICE)
                .withLong("groupId", mGroupId)
                .withLong("noticeId", mGroupInfoModel?.noticeId!!)
                .withBoolean(
                    "bfPushNotice", mGroupInfoModel?.bfPushNotice
                        ?: false
                ).navigation()
        }
        tv_notice_part.text = ""

        mAdapter.setOnItemClickListener { _, _, position ->
            val data = mAdapter.getItem(position)
            data?.let {
                when (it.itemType) {
                    GroupMemberItemBean.TYPE_OPERATE_ADD_MEMBER -> {
                        val isShowTip =
                            if (mGroupInfoModel?.bfJoinCheck == true) {
                                !(mGroupInfoModel?.memberRole == 0 || mGroupInfoModel?.memberRole == 1)
                            } else {
                                false
                            }
                        ARouter.getInstance()
                            .build(Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE)
                            .withInt("operate", 2)
                            .withBoolean("groupTip", isShowTip)
                            .withLong("groupId", mGroupId).navigation()
                    }
                    GroupMemberItemBean.TYPE_OPERATE_REMOVE_MEMBER -> {
                        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_REMOVE)
                            .withLong("groupId", mGroupId)
                            .navigation()
                    }
                    else -> data.getData()?.let { model ->
                        val isForbidJoinFriend =
                            if (mGroupInfoModel?.forbidJoinFriend == true) {
                                mGroupInfoModel?.memberRole == 2 && model.type == 2
                            } else false
                        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACT_DETAIL)
                            .withSerializable(
                                KEY_ADD_FRIEND_FROM,
                                ContactsProto.ContactsAddType.CROWD
                            )
                            .withLong(Constant.ARouter_Key.KEY_TARGET_GID, mGroupId)
                            .withBoolean("isForbidJoinFriend", isForbidJoinFriend)
                            .withLong(Constant.ARouter_Key.KEY_TARGET_UID, model.uid)
                            .withString(
                                Constant.ARouter_Key.KEY_GROUP_NICKNAME,
                                model.groupNickName
                            )
                            .navigation()
                    }
                }
            }
        }
        recycler_view_member.adapter = mAdapter
        recycler_view_member.layoutManager = GridLayoutManager(this@GroupSettingActivity, 5)
    }

    private fun refreshGroupInfoView() {
        mGroupInfoModel?.let { groupInfoModel ->
            app_text_view_group_name.text = groupInfoModel.name
            app_image_view_group_icon.setImageURI(groupInfoModel.pic)

            setting_item_view_1.setData(
                name = getString(R.string.group_of_qr_code),
                rid = R.drawable.bus_icon_qr,
                listen = {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_QR_GROUP_SWEEP)
                        .withLong(CHAT_GROUP_ID, mGroupId)
                        .withString(CHAT_GROUP_NAME, groupInfoModel.name)
                        .withString(CHAT_GROUP_ICON, groupInfoModel.pic)
                        .withBoolean(CHAT_GROUP_OWNER, groupInfoModel.hostId == mMineUid)
                        .navigation()
                })

            //群公告
            if (TextUtils.isEmpty(groupInfoModel.notice)) {
                tv_notice_none.visibility = View.VISIBLE
                tv_notice_part.visibility = View.GONE
            } else {
                tv_notice_none.visibility = View.GONE
                tv_notice_part.visibility = View.VISIBLE
                tv_notice_part.text = groupInfoModel.notice
            }

            updateGroupManager()

            setting_item_view_3.setData(name = getString(R.string.find_chat_content), listen = {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_CHAT_EXPAND)
                    .withInt(Constant.Search.SEARCH_TYPE, Constant.Search.SEARCH_GROUP_CHAT_CONTENT)
                    .withLong(Constant.Search.INDEX_ID, -mGroupId).navigation()
            })

            setting_item_view_8.setData(name = getString(R.string.media_documents_etc), listen = {
                ARouter.getInstance()
                    .build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_MEDIA_MANAGER)
                    .withInt("chatType", ChatModel.CHAT_TYPE_GROUP)
                    .withLong("chaterId", mGroupId)
                    .navigation()
            })

            setting_item_view_4.setData(name = getString(R.string.my_nickname_in_this_group),
                value = groupInfoModel.groupNickName
                    ?: "",
                listen = {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_EDIT_NAME)
                        .withString("defaultValue", mGroupInfoModel?.groupNickName)
                        .withString("title", getString(R.string.edit_group_nickname))
                        .withInt("editType", 2)
                        .withInt("max_count", Constant.Bus.MAX_TEXT_NAME)
                        .navigation(this@GroupSettingActivity, EDIT_NAME_REQUEST_CODE)
                })

            ArouterServiceManager.messageService.getChatTopStatus(
                ChatModel.CHAT_TYPE_GROUP,
                groupInfoModel.groupId,
                {
                    switch_button_1.setData(
                        name = getString(R.string.top_chat),
                        defaultChecked = it,
                        listen = { newValue ->
                            ArouterServiceManager.messageService.setChatTopStatus(
                                ChatModel.CHAT_TYPE_GROUP,
                                groupInfoModel.groupId,
                                newValue
                            )
                        })
                })

            switch_button_2.setData(
                name = getString(R.string.message_do_not_disturb),
                defaultChecked = groupInfoModel.bfDisturb,
                listen = {
                    ArouterServiceManager.groupService.setGroupMessageQuiet(
                        lifecycle(),
                        mGroupId,
                        it,
                        {
                            ArouterServiceManager.messageService.setChatIsDisturb(
                                ChatModel.CHAT_TYPE_GROUP,
                                mGroupId,
                                it
                            )
                        })
                })

            switch_button_3.setData(
                name = getString(R.string.save_to_address_book),
                defaultChecked = groupInfoModel.bfAddress,
                listen = {
                    ArouterServiceManager.groupService.saveGroupToContacts(
                        lifecycle(),
                        mGroupId,
                        it
                    )
                })

            setting_item_view_5.setDataTextColor(Color.RED)
            setting_item_view_5.setDataNonePoint(
                name = getString(R.string.clear_chat_logs),
                listen = {
                    if (groupInfoModel.memberRole > 1) {
                        AppDialog.showBottomListView(
                            this@GroupSettingActivity,
                            "",
                            arrayListOf(
                                getString(R.string.clear_and_delete_msgs_local),
                                getString(R.string.cancel)
                            )
                        ) { dialog, index, _ ->
                            when (index) {
                                0 -> {
                                    ArouterServiceManager.messageService.clearMessageHistory(
                                        ChatModel.CHAT_TYPE_GROUP,
                                        groupInfoModel.groupId
                                    )
                                }
                                else -> {
                                    dialog.dismiss()
                                }
                            }
                        }
                    } else {
                        AppDialog.showBottomListView(
                            this@GroupSettingActivity,
                            "",
                            arrayListOf(
                                getString(R.string.clear_and_delete_msgs_local),
                                getString(R.string.clear_and_delete_group_msgs_local),
                                getString(R.string.cancel)
                            )
                        ) { dialog, index, _ ->
                            when (index) {
                                0 -> {
                                    ArouterServiceManager.messageService.clearMessageHistory(
                                        ChatModel.CHAT_TYPE_GROUP,
                                        groupInfoModel.groupId
                                    )
                                }
                                1 -> {
                                    ArouterServiceManager.messageService.recallMessages(
                                        ChatModel.CHAT_TYPE_GROUP,
                                        mMineUid,
                                        groupInfoModel.groupId,
                                        ArouterServiceManager.messageService.getCurrentTime(),
                                        deleteChat = false
                                    )
                                }
                                else -> {
                                    dialog.dismiss()
                                }
                            }
                        }
                    }
                })

            setting_item_view_6.setDataTextColor(Color.RED)
            setting_item_view_6.setDataNonePoint(
                name = getString(R.string.delete_and_exit),
                listen = {
                    if ((mGroupInfoModel?.memberRole ?: 2) == 0) {
                        AppDialog.show(this@GroupSettingActivity, this@GroupSettingActivity) {
                            positiveButton(
                                text = getString(R.string.transfer_the_possession_of),
                                click = {
                                    ARouter.getInstance()
                                        .build(Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_OPERATE)
                                        .withInt(
                                            "operateType",
                                            OperateGroupMemberActivity.OPERATE_TYPE_TRANSFER_OWNER
                                        )
                                        .withLong("groupId", mGroupId)
                                        .navigation(
                                            this@GroupSettingActivity,
                                            GroupManageActivity.REQUEST_CODE_TRANSFER_OWNER
                                        )
                                })
                            negativeButton(text = getString(R.string.cancel))
                            title(text = getString(R.string.transfer_group_chat))
                            message(text = getString(R.string.group_chat_needs_to_be_transferred_first))
                        }
                    } else {
                        AppDialog.show(this@GroupSettingActivity, this@GroupSettingActivity) {
                            positiveButton(text = getString(R.string.confirm), click = {
                                //退出群组
                                ArouterServiceManager.groupService.quitGroup(lifecycle(), mGroupId)

                                //退出页面
                                finish()
                            })
                            negativeButton(text = getString(R.string.cancel))
                            title(text = getString(R.string.exit_the_group_chat))
                            message(text = getString(R.string.the_chat_records_of_this_group_will_be_deleted))
                        }
                    }
                })

            setting_item_view_7.setDataNonePoint(
                name = getString(R.string.encryption),
                rid = R.drawable.common_icon_lock,
                listen = {
                    AppDialog.show(this@GroupSettingActivity, this@GroupSettingActivity) {
                        message(text = getString(R.string.encryption_message))
                        positiveButton(text = getString(R.string.fine))
                    }
                })

            setting_item_view_9.setData(name = getString(R.string.complaint), listen = {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACTS_COMPLAINT)
                    .withInt("type", 1)
                    .withLong("targetUId", mGroupId).navigation()
            })

            //暂时屏蔽此功能 2021-8-26
//            if ((mGroupInfoModel?.memberRole ?: 2) < 2) {
//                setting_item_view_link.visibility = View.VISIBLE
//                setting_item_view_link.setData(
//                    getString(R.string.string_group_link),
//                    "",
//                    rid = R.drawable.bus_icon_link
//                ) {
//                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_COMMON_LINK)
//                        .withInt("type", 1)
//                        .withLong("targetId", mGroupId).navigation()
//                }
//            } else {
                setting_item_view_link.visibility = View.GONE
//            }

            if (mMineUid == mGroupInfoModel?.hostId ?: 0L) {
                setting_item_view_10.setDataTextColor(Color.RED)
                setting_item_view_10.visibility = View.VISIBLE
                setting_item_view_10.setDataNonePoint(
                    getString(R.string.string_group_dismiss),
                    ""
                ) {
                    AppDialog.show(this@GroupSettingActivity, this@GroupSettingActivity) {
                        title(text = getString(R.string.string_group_tip))
                        message(text = getString(R.string.string_group_message))
                        positiveButton(text = getString(R.string.confirm), click = {
                            ArouterServiceManager.groupService.disableGroup(lifecycle(), mGroupId, {
                                ArouterServiceManager.groupService.deleteGroup(
                                    lifecycle(),
                                    mGroupId,
                                    {
                                        EventBus.publishEvent(DisableGroupMessageEvent(mGroupId))
                                    }) {}
                            }) {
                                toast(it.message.toString())
                            }
                        })
                        negativeButton(text = getString(R.string.cancel)) {
                            cancel()
                        }
                    }
                }
            } else {
                setting_item_view_10.visibility = View.GONE
            }
        }
    }

    private fun updateGroupManager() {
        setting_item_view_2.visibility = View.GONE
        if ((mGroupInfoModel?.memberRole ?: 2) < 2) {
            setting_item_view_2.visibility = View.VISIBLE
            setting_item_view_2.setData(name = getString(R.string.group_membership), listen = {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_MANAGE)
                    .withLong("groupId", mGroupId)
                    .navigation(this@GroupSettingActivity, REQUEST_CODE_MANAGE)
            })

            setGroupInfo(mGroupInfoModel?.bfUpdateData ?: false)
        } else {
            setGroupInfo(false)
        }
    }

    private fun setGroupInfo(bfUpdateData: Boolean) {
        if (bfUpdateData) {
            image_view_info_point.visibility = View.VISIBLE
            layout_group_info.setOnClickListener {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_INFO)
                    .withLong("groupId", mGroupId)
                    .withString("groupName", mGroupInfoModel?.name)
                    .withString("groupIcon", mGroupInfoModel?.pic)
                    .navigation(this@GroupSettingActivity, REQUEST_CODE_INFO)
            }
            app_image_view_group_icon.setOnClickListener(null)
        } else {
            image_view_info_point.visibility = View.GONE
            layout_group_info.setOnClickListener(null)
            app_image_view_group_icon.setOnClickListener {
                ARouter.getInstance()
                    .build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_AVATAR_PREVIEW)
                    .withString("imageUrl", mGroupInfoModel?.pic)
                    .navigation()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MANAGE && resultCode == Activity.RESULT_OK) {
            refreshGroupInfo(syncGroupInfo = false, syncMembers = true)
        } else if (requestCode == REQUEST_CODE_INFO && resultCode == Activity.RESULT_OK) {
            refreshGroupInfo(syncGroupInfo = true, syncMembers = false)
        } else if (requestCode == EDIT_NAME_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val text = data?.getStringExtra("text") ?: ""
            ArouterServiceManager.groupService.saveGroupUserName(lifecycle(), mGroupId, text, {
                toast(getString(R.string.modified_group_nickname_successfully))
                setting_item_view_4.setData(text)
                mGroupInfoModel?.groupNickName = text
            }, {
                toast(it.message.toString())
            })
        }
    }
}