package framework.telegram.business.ui.contacts

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bigkoo.pickerview.view.OptionsPickerView
import com.facebook.common.util.UriUtil
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.bridge.event.FriendInfoChangeEvent
import framework.telegram.message.bridge.event.SearchChatEvent
import framework.telegram.support.BaseActivity
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.MsgFireTimePickerUtil
import framework.telegram.ui.dialog.AppDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_group_activity_group_contact_setting.*
import kotlinx.android.synthetic.main.bus_me_item_switch_button.view.*

@Route(path = Constant.ARouter.ROUNTE_BUS_CONTACT_SETTING)
class ContactSettingActivity : BaseActivity() {

    private var mUserId: Long = 0
    private var mContactInfo: ContactDataModel? = null
    private val mMineUid by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
    }

    private var mPv: OptionsPickerView<String>? = null

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mUserId = intent.getLongExtra("userId", 0)
        if (mUserId <= 0) {
            finish()
            return
        }

        setContentView(R.layout.bus_group_activity_group_contact_setting)

        initView()

        initContactInfo()

        EventBus.getFlowable(SearchChatEvent::class.java)
            .bindToLifecycle(this@ContactSettingActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.chatType == ChatModel.CHAT_TYPE_PVT || it.targetId == mUserId) {
                    finish()
                }
            }

        EventBus.getFlowable(FriendInfoChangeEvent::class.java)
            .bindToLifecycle(this@ContactSettingActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.user.uid == mUserId) {
                    mContactInfo?.let { info ->
                        info.isBfReadCancel = it.user.isBfReadCancel
                        info.msgCancelTime = it.user.msgCancelTime
                        info.isBfScreenshot = it.user.isBfScreenshot
                        info.isBfDisturb = it.user.isBfDisturb
                        updateContactInfo()
                    }
                }
            }
    }

    private fun initContactInfo() {
        ArouterServiceManager.contactService.updateContactInfo(lifecycle(), mUserId, {
            //获取到了网络信息
            mContactInfo = it
            updateContactInfo()
        }, {
            mContactInfo = it
            updateContactInfo()
        }, {
        })
    }

    private fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.chat_settings))
    }

    private fun updateContactInfo() {
        mContactInfo?.let { contactInfo ->
            app_image_view_icon.setImageURI(contactInfo.icon)
            app_image_view_name.text = contactInfo.displayName
            app_image_view_icon.setOnClickListener {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACT_DETAIL)
                    .withLong(Constant.ARouter_Key.KEY_TARGET_UID, contactInfo.uid).navigation()
            }
            image_view_create_group.setImageURI(UriUtil.getUriForResourceId(R.drawable.bus_group_member_add))

            image_view_create_group.setOnClickListener {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE)
                    .withInt("operate", 1)
                    .withLong("addedUserId", mUserId)
                    .withString("addedUserIcon", contactInfo.icon)
                    .withString("addUserName", contactInfo.nickName)
                    .navigation()
            }

            switch_button_burn_after_read.setChecked(contactInfo.isBfReadCancel, false)
            switch_button_burn_after_read.setOnCheckedChangeListener { _, isChecked ->
                ArouterServiceManager.contactService.setBurnAfterRead(
                    lifecycle(),
                    mUserId,
                    isChecked,
                    {
                        updateBurnAfterReadView(isChecked, contactInfo.msgCancelTime)
                    },
                    {
                        switch_button_burn_after_read.setChecked(!isChecked, false)
                        toast(String.format(getString(R.string.setup_failed), it))
                    })
            }

            updateBurnAfterReadView(contactInfo.isBfReadCancel, contactInfo.msgCancelTime)

            setting_item_view_des_time.setData(
                getString(R.string.message_destruction_time),
                MsgFireTimePickerUtil.timeValue2TimeName(contactInfo.msgCancelTime)
            ) {
                showPicker(contactInfo.msgCancelTime)
            }
            switch_button_screenshot.setData(
                getString(R.string.screenshots_to_inform),
                contactInfo.isBfScreenshot
            ) {
                ArouterServiceManager.contactService.setContactScreenshot(
                    lifecycle(),
                    mUserId,
                    it,
                    {
                        toast(getString(R.string.successfully_set))
                    },
                    { errorMsg ->
                        switch_button_screenshot.switch_button.setChecked(!it, false)
                        toast(String.format(getString(R.string.setup_failed_sign), errorMsg))
                    })
            }

            setting_item_view_0.setData(name = getString(R.string.find_chat_content), listen = {
                if (mContactInfo?.isBfReadCancel == true) {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_CHAT_EXPAND)
                        .withInt(
                            Constant.Search.SEARCH_TYPE,
                            Constant.Search.SEARCH_CHAT_CONTENT_PRIVATE
                        )
                        .withLong(Constant.Search.INDEX_ID, mUserId).navigation()
                } else {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_CHAT_EXPAND)
                        .withInt(Constant.Search.SEARCH_TYPE, Constant.Search.SEARCH_CHAT_CONTENT)
                        .withLong(Constant.Search.INDEX_ID, mUserId).navigation()
                }
            })

            setting_item_view_4.setData(name = getString(R.string.media_documents_etc), listen = {
                ARouter.getInstance()
                    .build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_MEDIA_MANAGER)
                    .withInt("chatType", ChatModel.CHAT_TYPE_PVT)
                    .withLong("chaterId", contactInfo.uid)
                    .navigation()
            })

            ArouterServiceManager.messageService.getChatTopStatus(
                ChatModel.CHAT_TYPE_PVT,
                contactInfo.uid,
                {
                    switch_button_1.setData(
                        name = getString(R.string.top_chat),
                        defaultChecked = it,
                        listen = { newValue ->
                            ArouterServiceManager.messageService.setChatTopStatus(
                                ChatModel.CHAT_TYPE_PVT,
                                contactInfo.uid,
                                newValue
                            )
                        })
                })

            switch_button_2.setData(
                name = getString(R.string.message_do_not_disturb),
                defaultChecked = contactInfo.isBfDisturb,
                listen = {
                    ArouterServiceManager.contactService.setContactMessageQuiet(
                        lifecycle(),
                        contactInfo.uid,
                        it, {
                            ArouterServiceManager.messageService.setChatIsDisturb(
                                ChatModel.CHAT_TYPE_PVT,
                                contactInfo.uid,
                                it
                            )
                        })
                })

            setting_item_view_1.setData(name = getString(R.string.clear_chat_logs), listen = {
                AppDialog.showBottomListView(
                    this@ContactSettingActivity,
                    "",
                    arrayListOf(
                        getString(R.string.clear_and_delete_msgs_local),
                        if (contactInfo.uid == framework.telegram.message.bridge.Constant.Common.FILE_TRANSFER_UID) {
                            getString(R.string.clear_and_delete_file_tran_msgs)
                        } else {
                            String.format(
                                getString(R.string.clear_and_delete_pvt_msgs_local),
                                contactInfo.displayName
                            )
                        }, getString(R.string.cancel)
                    )
                ) { dialog, index, _ ->
                    when (index) {
                        0 -> {
                            ArouterServiceManager.messageService.clearMessageHistory(
                                ChatModel.CHAT_TYPE_PVT,
                                contactInfo.uid
                            )
                        }
                        1 -> {
                            ArouterServiceManager.messageService.recallMessages(
                                ChatModel.CHAT_TYPE_PVT,
                                mMineUid,
                                contactInfo.uid,
                                ArouterServiceManager.messageService.getCurrentTime(),
                                deleteChat = false
                            )
                        }
                        else -> {
                            dialog.dismiss()
                        }
                    }
                }
            })

            setting_item_view_2.setData(name = getString(R.string.complaint), listen = {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACTS_COMPLAINT)
                    .withInt("type", 0)
                    .withLong("targetUId", mUserId).navigation()
            })

            setting_item_view_3.setDataNonePoint(
                name = getString(R.string.encryption),
                rid = R.drawable.common_icon_lock,
                listen = {
                    AppDialog.show(this@ContactSettingActivity, this@ContactSettingActivity) {
                        message(text = getString(R.string.encryption_message))
                        positiveButton(text = getString(R.string.fine))
                    }
                })
        }
    }

    private fun updateBurnAfterReadView(isChecked: Boolean, msgCancelTime: Int) {
        if (isChecked) {
            tv_burn_after_read.visibility = View.GONE
            setting_item_view_des_time.visibility = View.VISIBLE
            switch_button_screenshot.visibility = View.VISIBLE
            tv_screenshot.visibility = View.VISIBLE

            if (msgCancelTime == 0) {
                //如果销毁时间为0，则设置成1分钟
                ArouterServiceManager.contactService.setBurnAfterReadTime(
                    lifecycle(),
                    mUserId,
                    60,
                    {
                        setting_item_view_des_time.setDataValue(getString(R.string.one_minute))
                    },
                    {
                        setting_item_view_des_time.setDataValue(
                            MsgFireTimePickerUtil.timeValue2TimeName(
                                msgCancelTime
                            )
                        )
                    })
            }
        } else {
            tv_burn_after_read.visibility = View.VISIBLE
            setting_item_view_des_time.visibility = View.GONE
            mPv?.let {
                if (it.isShowing) {
                    it.dismiss()
                }
            }
            switch_button_screenshot.visibility = View.GONE
            tv_screenshot.visibility = View.GONE
        }
    }

    private fun showPicker(defaultTimeValue: Int) {
        mPv = MsgFireTimePickerUtil.showSelectTimePicker(
            this,
            defaultTimeValue
        ) { timeName, timeValue ->
            ArouterServiceManager.contactService.setBurnAfterReadTime(
                lifecycle(),
                mUserId,
                timeValue, {
                    setting_item_view_des_time.setDataValue(timeName)
                }, {
                    toast(String.format(getString(R.string.setup_failed_sign), it))
                })
        }
    }

    override fun onResume() {
        super.onResume()
        initContactInfo()
    }
}