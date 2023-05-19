package framework.telegram.business.ui.me

import android.os.Build
import android.util.Log
import android.view.View
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.sp.CommonPref
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.PrivacyContract
import framework.telegram.business.ui.me.presenter.PrivacyPresenterImpl
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.BitUtils
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.Helper.int2Bytes
import framework.telegram.support.tools.NotificationChannelUtil
import framework.telegram.support.tools.NotificationUtils
import framework.telegram.support.tools.RomUtils
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.tools.wrapper.Constant.COMMAND_BATTERY_MANAGER
import framework.telegram.ui.tools.wrapper.Constant.COMMAND_START_YOURSELF
import framework.telegram.ui.tools.wrapper.WhiteIntentWrapper
import kotlinx.android.synthetic.main.bus_me_activity_item_edit.custom_toolbar
import kotlinx.android.synthetic.main.bus_me_activity_notice.*

/**
 * Created by lzh on 19-6-7.
 * INFO:
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_INFO_NOTICE)
class NoticeActivity : BaseBusinessActivity<PrivacyContract.Presenter>(), PrivacyContract.View {

    private val mAccountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    override fun getLayoutId() = R.layout.bus_me_activity_notice

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.notifications_settings))

        showSwitch()
        showNotificationChannel()
        showNotificationTip()
    }

    override fun initListen() {
        val privacy = mAccountInfo.getPrivacy()
        val result = NotificationUtils.notificationSwitchOn(this@NoticeActivity)
        switch_button_notice_new_message.setData(getString(R.string.notification_of_new_information), result) {
            val title = (if (!it) getString(R.string.close) else getString(R.string.open)) + " " + getString(R.string.inform)
            val message = (if (!it) getString(R.string.close) else getString(R.string.open2)) + " " + getString(R.string.system_notification_permission_sign) + (if (!it) getString(R.string.message_notifications_will_be_missed) else getString(R.string.not_missing_any_notifications))
            val button = (if (!it) getString(R.string.to_set_up) else getString(R.string.Immediately_jump))
            SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()).putFirstOpenMessagePermission(true)
            AppDialog.show(this@NoticeActivity, this@NoticeActivity) {
                title(text = title)
                message(text = message)
                positiveButton(text = button, click = {
                    NotificationUtils.showNotificationWindow(this@NoticeActivity)
                })
                onDismiss {
                    showNotificationChannel()
                }
            }
        }

        switch_button_notice_new_call.setData(getString(R.string.voice_and_video_call_invitation_reminder), !BitUtils.checkBitValue(int2Bytes(privacy)[3], 1)) {
            mPresenter?.savePerfectInfo(3, 1, !it)
        }

        switch_button_hide_sender.setData(getString(R.string.hide_sender_information),BitUtils.checkBitValue(int2Bytes(privacy)[2], 5)){
            mPresenter?.savePerfectInfo(2, 5, it)
        }

        switch_button_notice_sender_info.setData(getString(R.string.notification_displays_sender_information), BitUtils.checkBitValue(int2Bytes(privacy)[3], 2)) {
            //这里不需要
            mPresenter?.savePerfectInfo(3, 2, it)
        }

        switch_button_notice_voice.setData(getString(R.string.sound), !BitUtils.checkBitValue(int2Bytes(privacy)[3], 3)) {
            mPresenter?.savePerfectInfo(3, 3, !it)
        }

        switch_button_notice_shock.setData(getString(R.string.shake), BitUtils.checkBitValue(int2Bytes(privacy)[3], 4)) {
            mPresenter?.savePerfectInfo(3, 4, it)
        }

        if (RomUtils.isSamsung) {
            text_view_white.visibility = View.VISIBLE
        } else {
            text_view_white.visibility = View.GONE
        }

        text_view_white.setOnClickListener {
            WhiteIntentWrapper.whiteListMatters(this@NoticeActivity, getString(R.string.self_starting), arrayListOf(COMMAND_START_YOURSELF, COMMAND_BATTERY_MANAGER))
        }

    }

    override fun initData() {
        PrivacyPresenterImpl(this, this, lifecycle())
    }

    override fun showLoading() {
    }

    override fun showErrMsg(str: String?) {
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as PrivacyPresenterImpl
    }

    override fun isActive() = true

    override fun savePerfectInfoSuccess(index: Int, pos: Int, value: Boolean) {

    }

    override fun onRestart() {
        super.onRestart()
        showSwitch()
        showNotificationChannel()
    }

    private fun showNotificationTip(){
        val result = NotificationUtils.notificationSwitchOn(this@NoticeActivity)
        if (!result){
            val title = getString(R.string.open_the_notification)
            val message =  getString(R.string.not_missing_any_notifications)
            val button =  getString(R.string.to_set_up)
            SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()).putFirstOpenMessagePermission(true)
            AppDialog.show(this@NoticeActivity, this@NoticeActivity) {
                title(text = title)
                message(text = message)
                positiveButton(text = button, click = {
                    NotificationUtils.showNotificationWindow(this@NoticeActivity)
                })
                onDismiss {
                    showNotificationChannel()
                }
            }
        }
    }

    private fun showNotificationChannel() {
        val noticeResult = NotificationUtils.notificationSwitchOn(this@NoticeActivity)
        switch_button_notice_new_message.setData(noticeResult)
        mPresenter?.savePerfectInfo(3, 0, !noticeResult)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && noticeResult) {
            relative_layout.visibility = View.VISIBLE
            relative_layout.setOnClickListener {
                if (RomUtils.isMiui) {
                    AppDialog.showList(this@NoticeActivity, getString(R.string.do_the_same_configuration), mutableListOf(getString(R.string.notification_of_new_information), getString(R.string.mipush))) { dialog, index, text ->
                        if (index == 0) {
                            NotificationChannelUtil.gotoChannelSetting(this@NoticeActivity, NotificationChannelUtil.getChannelId())
                        } else {
                            NotificationUtils.showNotificationWindow(this@NoticeActivity)
                        }
                    }
                } else {
                    val list = NotificationChannelUtil.getChannelList()
                    val listName = NotificationChannelUtil.getChannelNameList()
                    if (list.size > 1) {
                        AppDialog.showList(this@NoticeActivity, getString(R.string.same_configuration), listName) { dialog, index, text ->
                            if (list.size > index) {
                                NotificationChannelUtil.gotoChannelSetting(this@NoticeActivity, list[index])
                            }
                        }
                    } else {
                        NotificationChannelUtil.gotoChannelSetting(this@NoticeActivity, NotificationChannelUtil.getChannelId())
                    }
                }
            }
        } else {
            relative_layout.visibility = View.GONE
        }
    }

    private fun showSwitch() {
        val noticeResult = NotificationUtils.notificationSwitchOn(this@NoticeActivity)
        text_view_notification.visibility = if (noticeResult) View.VISIBLE else View.GONE
        switch_button_notice_new_call.visibility = if (noticeResult) View.VISIBLE else View.GONE
        switch_button_notice_voice.visibility = if (noticeResult) View.VISIBLE else View.GONE
        switch_button_notice_shock.visibility = if (noticeResult) View.VISIBLE else View.GONE
    }
}