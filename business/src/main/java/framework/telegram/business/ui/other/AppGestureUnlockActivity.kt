package framework.telegram.business.ui.other

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import com.alibaba.android.arouter.launcher.ARouter
import com.dds.gestureunlock.GestureUnlockActivity
import com.facebook.common.util.UriUtil
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.sp.CommonPref
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.ui.dialog.AppDialog

class AppGestureUnlockActivity : GestureUnlockActivity() {

    private val commonPref by lazy { SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()) }

    companion object {

        fun gotoCreateGestureCodeForResult(activity: Activity, key: String) {
            val intent = Intent(activity, AppGestureUnlockActivity::class.java)
            intent.putExtra(TYPE, TYPE_GESTURE_CREATE)
            intent.putExtra(KEY, key)
            activity.startActivityForResult(intent, CREATE_REQUEST_CODE)
        }

        fun gotoVerifyGestureCodeForResult(activity: Activity, key: String, icon: String, canClose: Boolean) {
            val avatar = if (TextUtils.isEmpty(icon)) {
                UriUtil.getUriForResourceId(R.drawable.common_holder_one_user).toString()
            } else {
                icon
            }
            val intent = Intent(activity, AppGestureUnlockActivity::class.java)
            intent.putExtra(TYPE, TYPE_GESTURE_VERIFY)
            intent.putExtra(KEY, key)
            intent.putExtra(ICON, avatar)
            intent.putExtra(CLOSE, canClose)
            activity.startActivityForResult(intent, VERIFY_REQUEST_CODE)
        }

        fun gotoVerifyGestureCode(context: Context, key: String, icon: String, canClose: Boolean) {
            val avatar = if (TextUtils.isEmpty(icon)) {
                UriUtil.getUriForResourceId(R.drawable.common_holder_one_user).toString()
            } else {
                icon
            }
            val intent = Intent(context, AppGestureUnlockActivity::class.java)
            intent.putExtra(TYPE, TYPE_GESTURE_VERIFY)
            intent.putExtra(KEY, key)
            intent.putExtra(ICON, avatar)
            intent.putExtra(CLOSE, canClose)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        fun gotoModifyGestureCode(context: Context, key: String) {
            val intent = Intent(context, AppGestureUnlockActivity::class.java)
            intent.putExtra(TYPE, TYPE_GESTURE_MODIFY)
            intent.putExtra(KEY, key)

            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onForgetPwd() {
        ARouter.getInstance().build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_CLEAR_ACCOUNT).navigation()
    }

    override fun onChangeAccount() {
        BaseApp.app.onUserLogout("", true)
    }

    override fun onChangeOtherLock() {
        val list = mutableListOf<String>()
        list.add(getString(R.string.applock_pinlock_type))

        AppDialog.showBottomListView(this@AppGestureUnlockActivity, this@AppGestureUnlockActivity, list) { _, index, text ->
            when (text) {
                getString(R.string.applock_pinlock_type) -> {
                    finish()
                }
            }
        }
    }

    override fun onUnlockSuccess() {

    }

    override fun onDisableAccount() {
        ArouterServiceManager.systemService.disableAccount(this@AppGestureUnlockActivity, getString(R.string.disable_account_msg))
    }

    override fun isDisableAccountIsOpen(): Boolean {
        return commonPref.getDisableAccount()
    }
}
