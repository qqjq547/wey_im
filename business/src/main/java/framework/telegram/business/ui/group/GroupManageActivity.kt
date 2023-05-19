package framework.telegram.business.ui.group

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bigkoo.pickerview.view.OptionsPickerView
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.group.GroupInfoModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.http.HttpException
import framework.telegram.message.bridge.event.BanGroupMessageEvent
import framework.telegram.message.bridge.event.DisableGroupMessageEvent
import framework.telegram.message.bridge.event.GroupInfoChangeEvent
import framework.telegram.support.BaseActivity
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.MsgFireTimePickerUtil
import framework.telegram.ui.dialog.AppDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_group_activity_group_manage.*
import kotlinx.android.synthetic.main.bus_group_activity_group_manage.custom_toolbar
import kotlinx.android.synthetic.main.bus_group_activity_group_manage.setting_item_view_1
import kotlinx.android.synthetic.main.bus_group_activity_group_manage.setting_item_view_des_time
import kotlinx.android.synthetic.main.bus_group_activity_group_manage.switch_button_1
import kotlinx.android.synthetic.main.bus_group_activity_group_manage.switch_button_2
import kotlinx.android.synthetic.main.bus_group_activity_group_manage.switch_button_burn_after_read
import kotlinx.android.synthetic.main.bus_group_activity_group_manage.tv_burn_after_read

@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_MANAGE)
class GroupManageActivity : BaseActivity() {

    companion object {
        const val REQUEST_CODE_TRANSFER_OWNER = 0x1000
    }

    private val mGroupId by lazy { intent.getLongExtra("groupId", 0) }
    private var mGroupInfoModel: GroupInfoModel? = null
    private var mPv: OptionsPickerView<String>? = null

    private var isLoad = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (mGroupId <= 0) {
            finish()
            return
        }

        setContentView(R.layout.bus_group_activity_group_manage)
        initView()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        custom_toolbar.showCenterTitle(getString(R.string.group_membership))
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        setting_item_administrator.setData(name = getString(R.string.administrator), listen = {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_ADMINISTRATOR)
                    .withLong("hostId",mGroupInfoModel?.hostId?:0L)
                    .withLong("groupId", mGroupId).withBoolean("bfSetAdmin", mGroupInfoModel?.bfSetAdmin
                            ?: false).navigation()
        })

        EventBus.getFlowable(GroupInfoChangeEvent::class.java)
                .bindToLifecycle(this@GroupManageActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.groupId == mGroupId) {
                        initGroupInfo()
                    }
                }

        EventBus.getFlowable(BanGroupMessageEvent::class.java)
                .bindToLifecycle(this@GroupManageActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.groupId == mGroupId) {
                        finish()
                    }
                }


        EventBus.getFlowable(DisableGroupMessageEvent::class.java)
                .bindToLifecycle(this@GroupManageActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.groupId == mGroupId) {
                        if (ActivitiesHelper.getInstance().topActivity == this@GroupManageActivity){
                            AppDialog.show(this@GroupManageActivity, this@GroupManageActivity) {
                                positiveButton(text = getString(R.string.confirm), click = {
                                    //清空聊天记录
                                    finish()
                                })
                                cancelOnTouchOutside(false)
                                message(text = getString(R.string.string_group_dismiss_title))
                            }
                        }else{
                            finish()
                        }
                    }
                }
    }

    override fun onResume() {
        super.onResume()
        initGroupInfo()
    }

    private fun initGroupInfo() {
        ArouterServiceManager.groupService.updateGroupInfo(lifecycle(), mGroupId, {
        }, {data ->
            mGroupInfoModel = data
            updateGroupInfo()
        })
    }

    private fun updateGroupInfo() {
        if ((mGroupInfoModel?.memberRole ?: 2) == 0) {
            switch_button_1.visibility = View.VISIBLE
            switch_button_1.setData(name = getString(R.string.group_entry_shall_be_reviewed), defaultChecked = mGroupInfoModel?.bfJoinCheck, listen = {
                ArouterServiceManager.groupService.setGroupJoinCheck(lifecycle(), mGroupId, it)
            })
        } else {
            switch_button_1.visibility = View.GONE
        }

        if ((mGroupInfoModel?.memberRole ?: 2) == 0) {
            setting_item_view_1.visibility = View.VISIBLE
            setting_item_view_1.setDataTextColor(Color.RED)
            setting_item_view_1.setDataNonePoint(name = getString(R.string.transfer_group_chat), listen = {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_OPERATE)
                        .withInt("operateType", OperateGroupMemberActivity.OPERATE_TYPE_TRANSFER_OWNER)
                        .withLong("groupId", mGroupId)
                        .navigation(this@GroupManageActivity, REQUEST_CODE_TRANSFER_OWNER)
            })
        } else {
            setting_item_view_1.visibility = View.GONE
        }

        switch_button_2.setData(name = getString(R.string.can_not_mutual_friends), defaultChecked = mGroupInfoModel?.forbidJoinFriend, listen = {
            ArouterServiceManager.groupService.saveGroupJoinFriend(lifecycle(), mGroupId, it)
        })

        switch_button_3.setData(name = getString(R.string.total_silence), defaultChecked = mGroupInfoModel?.forShutupGroup, listen = { it ->
            ArouterServiceManager.groupService.shutUpGroup(lifecycle(), mGroupId, it,{

            },{error->
                if(error is HttpException){
                    toast(error.errMsg)
                }else{
                    toast(getString(R.string.total_silence_failed))
                }
            })
        })

        mGroupInfoModel?.let {
            switch_button_burn_after_read.setOnCheckedChangeListener(null)
            switch_button_burn_after_read.isChecked = it.bfGroupReadCancel
            switch_button_burn_after_read.setOnCheckedChangeListener { _, isChecked ->
                ArouterServiceManager.groupService.setBurnAfterRead(lifecycle(), mGroupId, isChecked, {
                    updateBurnAfterReadView(isChecked, it.groupMsgCancelTime)
                }, { t ->
                    switch_button_burn_after_read.setChecked(!isChecked,false)
                    toast(String.format(getString(R.string.setup_failed),t.message) )
                })
            }
            updateBurnAfterReadView(it.bfGroupReadCancel, it.groupMsgCancelTime)
            setting_item_view_des_time.setData(getString(R.string.message_destruction_time), MsgFireTimePickerUtil.timeValue2TimeName(it.groupMsgCancelTime)) {
                showPicker(it.groupMsgCancelTime)
            }
        }
    }

    private fun updateBurnAfterReadView(isChecked: Boolean, msgCancelTime: Int) {
        if (isChecked) {
            tv_burn_after_read.visibility = View.GONE
            setting_item_view_des_time.visibility = View.VISIBLE

            if (msgCancelTime == 0) {
                //如果销毁时间为0，则设置成1分钟
                ArouterServiceManager.groupService.setBurnAfterReadTime(lifecycle(), mGroupId, 60, {
                    setting_item_view_des_time.setDataValue(getString(R.string.one_minute))
                }, {
                    setting_item_view_des_time.setDataValue(MsgFireTimePickerUtil.timeValue2TimeName(msgCancelTime))
                })
            }
        } else {
            tv_burn_after_read.visibility = View.VISIBLE
            setting_item_view_des_time.visibility = View.GONE
            mPv?.let {
                if (it.isShowing){
                    it.dismiss()
                }
            }
        }
    }

    private fun showPicker(defaultTimeValue: Int) {
        mPv = MsgFireTimePickerUtil.showSelectTimePicker(this, defaultTimeValue) { timeName, timeValue ->
            ArouterServiceManager.groupService.setBurnAfterReadTime(lifecycle(), mGroupId, timeValue, {
                setting_item_view_des_time.setDataValue(timeName)
            }, {t ->
                toast(String.format(getString(R.string.setup_failed_sign),t.message))
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_TRANSFER_OWNER && resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}