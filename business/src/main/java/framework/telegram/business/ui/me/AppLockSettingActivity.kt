package framework.telegram.business.ui.me

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.Toast
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bigkoo.pickerview.view.OptionsPickerView
import com.codersun.fingerprintcompat.FingerManager
import com.dds.fingerprintidentify.FingerprintIdentifyActivity
import com.dds.gestureunlock.GestureUnlock
import com.dds.gestureunlock.GestureUnlockActivity
import com.manusunny.pinlock.ConfirmPinActivity
import com.manusunny.pinlock.PinCodeUnlock
import com.manusunny.pinlock.SetPinActivity
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_ME_APPLOCK_FORGET
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.sp.CommonPref
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.AppLockSettingContract
import framework.telegram.business.ui.other.AppConfirmPinUnlockActivity
import framework.telegram.business.ui.other.AppFingerprintIdentifyActivity
import framework.telegram.business.ui.other.AppGestureUnlockActivity
import framework.telegram.business.ui.other.AppSetPinUnlockActivity
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.AppLockTimePickerUtil
import framework.telegram.ui.dialog.AppDialog
import kotlinx.android.synthetic.main.bus_me_activity_app_lock.*

@Route(path = Constant.ARouter.ROUNTE_BUS_ME_APPLOCK_SETTING)
class AppLockSettingActivity : BaseBusinessActivity<AppLockSettingContract.Presenter>() {

    companion object {
        private const val FORGET_VERIFY_REQUEST_CODE = 0x00068
    }

    private val commonPref by lazy { SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()) }

    private val accountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    private var mPv: OptionsPickerView<String>? = null

    private var mLastClickTime = 0L

    override fun getLayoutId() = R.layout.bus_me_activity_app_lock

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.app_lock))
    }

    override fun initListen() {
        // 安全锁开关赋值
        switch_button_applock_on_off.setData(getString(R.string.app_lock), commonPref.getAppLockIsOn()) {
            if (System.currentTimeMillis() - mLastClickTime < 1000) {
                switch_button_applock_on_off.setData(!it)
            } else {
                mLastClickTime = System.currentTimeMillis()

                if (it) {
                    AppDialog.show(this@AppLockSettingActivity, this@AppLockSettingActivity) {
                        message(text = getString(R.string.open_app_lock_tip))
                        cancelable(false)
                        negativeButton(text = context.getString(R.string.confirm_open), click = {
                            AppSetPinUnlockActivity.gotoCreatePinCodeForResult(this@AppLockSettingActivity, accountInfo.getUserId().toString())
                        })
                        positiveButton(text = context.getString(R.string.cancel), click = {
                            this@AppLockSettingActivity.switch_button_applock_on_off.setData(false)
                        })
                    }
                } else {
                    if (PinCodeUnlock.getInstance().isPinCodeSet(this@AppLockSettingActivity, accountInfo.getUserId().toString())) {
                        AppConfirmPinUnlockActivity.gotoVerifyPinCodeForResult(this@AppLockSettingActivity, accountInfo.getUserId().toString())
                    } else {
                        AppGestureUnlockActivity.gotoVerifyGestureCodeForResult(this@AppLockSettingActivity, accountInfo.getUserId().toString(), accountInfo.getAvatar(), true)
                    }
                }
            }
        }

        switch_button_applock_disable_accout.setData(getString(R.string.disable_account_title), commonPref.getDisableAccount()) {
            if (it) {
                AppDialog.show(this@AppLockSettingActivity, this@AppLockSettingActivity) {
                    message(text = getString(R.string.disable_account_tip))
                    cancelable(false)
                    negativeButton(text = context.getString(R.string.confirm_open), click = {
                        commonPref.putDisableAccount(true)
                    })
                    positiveButton(text = context.getString(R.string.cancel), click = {
                        this@AppLockSettingActivity.switch_button_applock_disable_accout.setData(false)
                    })
                }
            } else {
                commonPref.putDisableAccount(false)
            }
        }
        if (commonPref.getAppLockIsOn()) {
            switch_button_applock_disable_accout.visibility = View.VISIBLE
            switch_button_applock_disable_accout_tips.visibility = View.VISIBLE
        } else {
            switch_button_applock_disable_accout.visibility = View.GONE
            switch_button_applock_disable_accout_tips.visibility = View.GONE
        }

        // 指纹锁开关赋值
        switch_button_applock_fingerprint.setData(getString(R.string.app_lock_fingerprint), commonPref.getAppLockFingerPrintIsOn()) {
            if (it) {
                AppFingerprintIdentifyActivity.gotoOpenFingerprintForResult(this@AppLockSettingActivity, accountInfo.getUserId().toString())
            } else {
                try {
                    FingerManager.updateFingerData(this@AppLockSettingActivity)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                commonPref.putAppLockFingerPrintIsOn(false)
            }
        }

        if (commonPref.getAppLockIsOn()) {
            if (FingerManager.checkSupport(this@AppLockSettingActivity) == FingerManager.SupportResult.DEVICE_UNSUPPORTED) {
                commonPref.putAppLockFingerPrintIsOn(false)
                switch_button_applock_fingerprint.visibility = View.GONE
            } else {
                switch_button_applock_fingerprint.visibility = View.VISIBLE
            }
        } else {
            commonPref.putAppLockFingerPrintIsOn(false)
            switch_button_applock_fingerprint.visibility = View.GONE
        }

        // 修改密码赋值
        val isPinCodeSet = PinCodeUnlock.getInstance().isPinCodeSet(this@AppLockSettingActivity, accountInfo.getUserId().toString())
        me_item_view_applock_update_pwd.setData(if (isPinCodeSet) getString(R.string.change_applock_pincode) else getString(R.string.change_applock_password)) {
            if (PinCodeUnlock.getInstance().isPinCodeSet(this@AppLockSettingActivity, accountInfo.getUserId().toString())) {
                AppConfirmPinUnlockActivity.gotoModifyPinCode(this@AppLockSettingActivity, accountInfo.getUserId().toString())
            } else {
                AppGestureUnlockActivity.gotoModifyGestureCode(this@AppLockSettingActivity, accountInfo.getUserId().toString())
            }
        }
        if (commonPref.getAppLockIsOn()) {
            me_item_view_applock_update_pwd.visibility = View.VISIBLE
        } else {
            me_item_view_applock_update_pwd.visibility = View.GONE
        }

        // 忘记密码赋值
        me_item_view_applock_forget_pwd.setData(if (isPinCodeSet) getString(R.string.forget_applock_pincode) else getString(R.string.forget_applock_password)) {
            ARouter.getInstance().build(ROUNTE_BUS_ME_APPLOCK_FORGET).navigation(this@AppLockSettingActivity, FORGET_VERIFY_REQUEST_CODE)
        }
        if (commonPref.getAppLockIsOn()) {
            me_item_view_applock_forget_pwd.visibility = View.VISIBLE
        } else {
            me_item_view_applock_forget_pwd.visibility = View.GONE
        }

        // 修改锁定时间赋值
        val timeName = AppLockTimePickerUtil.timeValue2TimeName(commonPref.getAppLockExipreTime())
        me_item_view_applock_time.setData(getString(R.string.app_lock_time_title), String.format(getString(R.string.app_lock_time_value), timeName)) {
            showPicker(commonPref.getAppLockExipreTime())
        }
        if (commonPref.getAppLockIsOn()) {
            me_item_view_applock_time.visibility = View.VISIBLE
        } else {
            me_item_view_applock_time.visibility = View.GONE
        }
    }

    private fun showPicker(defaultTimeValue: Int) {
        mPv = AppLockTimePickerUtil.showSelectTimePicker(this, defaultTimeValue) { timeName, timeValue ->
            me_item_view_applock_time.setDataValue(String.format(getString(R.string.app_lock_time_value), timeName))
            commonPref.putAppLockExipreTime(timeValue)
        }
    }

    override fun initData() {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SetPinActivity.CREATE_REQUEST_CODE) {
            // 创建密码
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this@AppLockSettingActivity, getString(R.string.app_lock_setting_success), Toast.LENGTH_SHORT).show()

                val isPinCodeSet = PinCodeUnlock.getInstance().isPinCodeSet(this@AppLockSettingActivity, accountInfo.getUserId().toString())
                me_item_view_applock_update_pwd.setDataName(if (isPinCodeSet) getString(R.string.change_applock_pincode) else getString(R.string.change_applock_password))
                me_item_view_applock_forget_pwd.setDataName(if (isPinCodeSet) getString(R.string.forget_applock_pincode) else getString(R.string.forget_applock_password))

                commonPref.putAppLockExipreTime(5 * 60)// 默认300秒后
                commonPref.putAppLockIsOn(true)

                val timeName = AppLockTimePickerUtil.timeValue2TimeName(commonPref.getAppLockExipreTime())
                me_item_view_applock_time.setDataValue(String.format(getString(R.string.app_lock_time_value), timeName))

                switch_button_applock_on_off.setData(true)
                switch_button_applock_disable_accout.visibility = View.VISIBLE
                switch_button_applock_disable_accout_tips.visibility = View.VISIBLE
                me_item_view_applock_update_pwd.visibility = View.VISIBLE
                me_item_view_applock_forget_pwd.visibility = View.VISIBLE
                me_item_view_applock_time.visibility = View.VISIBLE

                if (FingerManager.checkSupport(this@AppLockSettingActivity) == FingerManager.SupportResult.DEVICE_UNSUPPORTED) {
                    switch_button_applock_fingerprint.visibility = View.GONE
                } else {
                    switch_button_applock_fingerprint.visibility = View.VISIBLE
                }
            } else {
                commonPref.putAppLockIsOn(false)
                switch_button_applock_on_off.setData(false)
            }
        } else if (requestCode == GestureUnlockActivity.VERIFY_REQUEST_CODE || requestCode == ConfirmPinActivity.VERIFY_REQUEST_CODE) {
            // 关闭密码
            if (resultCode == Activity.RESULT_OK) {
                // 清除手势密码
                GestureUnlock.getInstance().clearGestureCode(applicationContext, accountInfo.getUserId().toString())
                // 清除pin密码
                PinCodeUnlock.getInstance().clearPinCode(applicationContext, accountInfo.getUserId().toString())

                commonPref.putAppLockIsOn(false)
                commonPref.putAppLockFingerPrintIsOn(false)
                me_item_view_applock_update_pwd.visibility = View.GONE
                me_item_view_applock_forget_pwd.visibility = View.GONE
                me_item_view_applock_time.visibility = View.GONE

                switch_button_applock_disable_accout.visibility = View.GONE
                switch_button_applock_disable_accout_tips.visibility = View.GONE
                switch_button_applock_disable_accout.setData(false)

                switch_button_applock_fingerprint.visibility = View.GONE
                switch_button_applock_fingerprint.setData(false)
            } else {
                switch_button_applock_on_off.setData(true)
            }
        } else if (requestCode == FORGET_VERIFY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // 忘记密码

            // 清除手势密码
            GestureUnlock.getInstance().clearGestureCode(applicationContext, accountInfo.getUserId().toString())
            GestureUnlock.getInstance().clearUnlockErrorMax(applicationContext, accountInfo.getUserId().toString())
            // 清除pin密码
            PinCodeUnlock.getInstance().clearPinCode(applicationContext, accountInfo.getUserId().toString())
            PinCodeUnlock.getInstance().clearUnlockErrorMax(applicationContext, accountInfo.getUserId().toString())

            commonPref.putAppLockIsOn(false)
            switch_button_applock_on_off.setData(false)

            switch_button_applock_fingerprint.visibility = View.GONE
            commonPref.putAppLockFingerPrintIsOn(false)
            switch_button_applock_fingerprint.setData(false)

            me_item_view_applock_time.visibility = View.GONE
            me_item_view_applock_update_pwd.visibility = View.GONE
            me_item_view_applock_forget_pwd.visibility = View.GONE

            switch_button_applock_disable_accout.visibility = View.GONE
            switch_button_applock_disable_accout_tips.visibility = View.GONE
            switch_button_applock_disable_accout.setData(false)

            // 重新创建密码
            AppSetPinUnlockActivity.gotoCreatePinCodeForResult(this@AppLockSettingActivity, accountInfo.getUserId().toString(), false)
        } else if (requestCode == FingerprintIdentifyActivity.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                commonPref.putAppLockFingerPrintIsOn(true)
                switch_button_applock_fingerprint.setData(true)
            } else {
                commonPref.putAppLockFingerPrintIsOn(false)
                switch_button_applock_fingerprint.setData(false)
            }
        }
    }
}