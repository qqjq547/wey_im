package framework.telegram.business.ui.other

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.dds.fingerprintidentify.FingerprintIdentifyActivity
import com.dds.gestureunlock.GestureUnlock
import com.manusunny.pinlock.PinCodeUnlock

import framework.telegram.business.sp.CommonPref
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.system.storage.sp.SharePreferencesStorage

class AppFingerprintIdentifyActivity : FingerprintIdentifyActivity() {

    private val commonPref by lazy { SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()) }

    companion object {
        fun gotoVerifyFingerprint(context: Context, key: String, icon: String) {
            val intent = Intent(context, AppFingerprintIdentifyActivity::class.java)
            intent.putExtra(KEY, key)
            intent.putExtra(ICON, icon)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        fun gotoOpenFingerprintForResult(activity: Activity, key: String) {
            val intent = Intent(activity, AppFingerprintIdentifyActivity::class.java)
            intent.putExtra(OPERATION, true)
            intent.putExtra(KEY, key)
            activity.startActivityForResult(intent, REQUEST_CODE)
        }
    }

    override fun onFingerDataChange() {
        commonPref.putAppLockFingerPrintIsOn(false)
    }

    override fun showAppUnlockActivity(key: String, icon: String) {
        // 是否设置Pin密码
        if (PinCodeUnlock.getInstance().isPinCodeSet(this@AppFingerprintIdentifyActivity, key)) {
            // 验证Pin密码
            AppConfirmPinUnlockActivity.gotoVerifyPinCode(this@AppFingerprintIdentifyActivity, key, icon)
        } else {
            // 没有设置Pin密码，是否设置手势密码
            if (GestureUnlock.getInstance().isGestureCodeSet(this@AppFingerprintIdentifyActivity, key)) {
                // 验证手势密码
                AppGestureUnlockActivity.gotoVerifyGestureCode(this@AppFingerprintIdentifyActivity, key, icon, false)
            }
        }
    }

    override fun onUnlockSuccess() {
    }
}
