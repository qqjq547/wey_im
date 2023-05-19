package framework.telegram.business.ui.other

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import com.alibaba.android.arouter.launcher.ARouter
import com.dds.gestureunlock.GestureUnlockActivity
import com.facebook.common.util.UriUtil

import com.manusunny.pinlock.ConfirmPinActivity
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.sp.CommonPref
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.ui.dialog.AppDialog

class AppConfirmPinUnlockActivity : ConfirmPinActivity() {

    private val commonPref by lazy { SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()) }

    companion object {

        fun gotoVerifyPinCodeForResult(activity: Activity, key: String) {
            val intent = Intent(activity, AppConfirmPinUnlockActivity::class.java)
            intent.putExtra(KEY, key)
            intent.putExtra(TYPE, 1)
            activity.startActivityForResult(intent, VERIFY_REQUEST_CODE)
        }

        fun gotoModifyPinCode(context: Context, key: String) {
            val intent = Intent(context, AppConfirmPinUnlockActivity::class.java)
            intent.putExtra(KEY, key)
            intent.putExtra(TYPE, 2)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        fun gotoVerifyPinCode(context: Context, key: String, icon: String) {
            val avatar = if (TextUtils.isEmpty(icon)) {
                UriUtil.getUriForResourceId(R.drawable.common_holder_one_user).toString()
            } else {
                icon
            }
            val intent = Intent(context, AppConfirmPinUnlockActivity::class.java)
            intent.putExtra(KEY, key)
            intent.putExtra(ICON, avatar)
            intent.putExtra(TYPE, 3)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onForgetPinCode() {
        ARouter.getInstance().build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_CLEAR_ACCOUNT).navigation()
    }

    override fun onChangeAccount() {
        BaseApp.app.onUserLogout("", true)
    }

    override fun onConfirmSuccess() {
        if (type == 2) {
            AppSetPinUnlockActivity.gotoCreatePinCodeForResult(this@AppConfirmPinUnlockActivity, key, false)
        }
    }

    override fun onDisableAccount() {
        ArouterServiceManager.systemService.disableAccount(this@AppConfirmPinUnlockActivity, getString(R.string.disable_account_msg))
    }

    override fun isDisableAccountIsOpen(): Boolean {
        return commonPref.getDisableAccount()
    }
}
