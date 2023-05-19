package framework.telegram.business.services

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.codersun.fingerprintcompat.FingerManager
import com.dds.gestureunlock.GestureUnlock
import com.im.domain.pb.LoginProto
import com.im.domain.pb.SysProto
import com.manusunny.pinlock.PinCodeUnlock
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.BusinessApplication
import framework.telegram.business.TokenReqInterceptor
import framework.telegram.business.UserDHKeysHelper
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.service.ISystemService
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.SysHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.SystemHttpProtocol
import framework.telegram.business.sp.CommonPref
import framework.telegram.business.ui.other.AppConfirmPinUnlockActivity
import framework.telegram.business.ui.other.AppFingerprintIdentifyActivity
import framework.telegram.business.ui.other.AppGestureUnlockActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog

@Route(path = Constant.ARouter.ROUNTE_SERVICE_SYSTEM, name = "其它服务")
class SystemServiceImpl : ISystemService {

    override fun init(context: Context?) {

    }

    override fun intercept(errCode: Int, oldToken: String): Boolean {
        return TokenReqInterceptor.intercept(errCode, oldToken)
    }

    override fun getAccountKeyVersion(uid: Long): Int {
        return UserDHKeysHelper.getAccountSecretKey(uid.toString())?.keyVersion ?: 0
    }

    override fun getLoginAccountWebSecretKey(complete: (String, Int) -> Unit) {
        UserDHKeysHelper.getLoginAccountWebSecretKey(complete)
    }

    override fun getUserSecretKey(targetUid: Long, appVer: Int, webVer: Int, complete: (String, Int, String, Int) -> Unit, error: ((Throwable) -> Unit)?) {
        UserDHKeysHelper.getUserSecretKey(targetUid, appVer, webVer, complete, error)
    }

    override fun getGroupSecretKey(targetGid: Long, complete: (String, Int) -> Unit, error: ((Throwable) -> Unit)?) {
        UserDHKeysHelper.getGroupSecretKey(targetGid, complete, error)
    }

    override fun updateUserPublicKey(targetUid: Long, appkeyVer: Int, webKeyVer: Int, complete: (String, Int, String, Int) -> Unit, error: ((Throwable) -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        UserDHKeysHelper.updateUserPublicKey(myUid, targetUid, appkeyVer, webKeyVer, complete, error)
    }

    override fun updateGroupPublicKey(targetGid: Long, complete: (String, Int, String) -> Unit, error: ((Throwable) -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        UserDHKeysHelper.updateGroupPublicKey(myUid, targetGid, complete, error)
    }

    override fun updateAccountKey(publicKey: String, complete: () -> Unit, error: ((Throwable) -> Unit)?) {
        UserDHKeysHelper.updateAccountPublicKey(publicKey, complete, error)
    }

    override fun clearMyselfWebSecretKeysCache() {
        UserDHKeysHelper.clearMyselfWebSecretKeysCache()
    }

    override fun saveUrls(it: LoginProto.UrlInfo) {
        BusinessApplication.saveUrls(it)
    }

    override fun openAppFingerprintIdentifyActivity() {
        val userId = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val userIcon = AccountManager.getLoginAccount(AccountInfo::class.java).getAvatar()
        val topActivity = ActivitiesHelper.getInstance().topActivity
        if (topActivity != null) {
            AppFingerprintIdentifyActivity.gotoVerifyFingerprint(topActivity, userId.toString(), userIcon)
        } else {
            AppGestureUnlockActivity.gotoVerifyGestureCode(BaseApp.app, userId.toString(), userIcon, false)
        }
    }

    override fun openAppGestureUnlockActivity() {
        val userId = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val userIcon = AccountManager.getLoginAccount(AccountInfo::class.java).getAvatar()
        val topActivity = ActivitiesHelper.getInstance().topActivity
        if (topActivity != null) {
            AppGestureUnlockActivity.gotoVerifyGestureCode(topActivity, userId.toString(), userIcon, false)
        } else {
            AppGestureUnlockActivity.gotoVerifyGestureCode(BaseApp.app, userId.toString(), userIcon, false)
        }
    }

    override fun openAppConfirmPinUnlockActivity() {
        val userId = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val userIcon = AccountManager.getLoginAccount(AccountInfo::class.java).getAvatar()
        val topActivity = ActivitiesHelper.getInstance().topActivity
        if (topActivity != null) {
            AppConfirmPinUnlockActivity.gotoVerifyPinCode(topActivity, userId.toString(), userIcon)
        } else {
            AppConfirmPinUnlockActivity.gotoVerifyPinCode(BaseApp.app, userId.toString(), userIcon)
        }
    }

    override fun checkAppUnlockActivity(activity: Activity) {
        val commonPref = SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid())
        checkAppLock(activity, commonPref)
    }

    override fun isAppLockOn(): Boolean {
        val commonPref = SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid())
        return commonPref.getAppLockIsOn()
    }

    private fun checkAppLock(activity: Activity, commonPref: CommonPref) {
        val userId = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val userIcon = AccountManager.getLoginAccount(AccountInfo::class.java).getAvatar()

        if (commonPref.getAppLockIsOn()) {
            //是否开启指纹解锁
            if (commonPref.getAppLockFingerPrintIsOn()) {
                when (FingerManager.checkSupport(activity)) {
                    FingerManager.SupportResult.SUPPORT -> {
                        // 支持指纹解锁
                        AppFingerprintIdentifyActivity.gotoVerifyFingerprint(activity, userId.toString(), userIcon)
                    }
                    else -> {
                        // 不支持指纹解锁或者没有录入指纹
                        commonPref.putAppLockFingerPrintIsOn(false)
                        showAppUnlock(userId, userIcon, activity)
                    }
                }
            } else {
                showAppUnlock(userId, userIcon, activity)
            }
        }
    }

    private fun showAppUnlock(uid: Long, icon: String, activity: Activity) {
        // 是否设置Pin密码
        if (PinCodeUnlock.getInstance().isPinCodeSet(activity, uid.toString())) {
            // 验证Pin密码
            AppConfirmPinUnlockActivity.gotoVerifyPinCode(activity, uid.toString(), icon)
        } else { // 是否设置手势密码
            if (GestureUnlock.getInstance().isGestureCodeSet(activity, uid.toString())) {
                // 验证手势密码
                AppGestureUnlockActivity.gotoVerifyGestureCode(activity, uid.toString(), icon, false)
            } else {
                // 没有设置手势密码，取消安全锁
                val commonPref = SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid())
                commonPref.putAppLockIsOn(false)
                commonPref.putAppLockFingerPrintIsOn(false)
            }
        }
    }

    override fun disableAccount(activity: AppCompatActivity, tipMsg: String) {
        val dialog = AppDialog.showLoadingView(activity, activity)
        HttpManager.getStore(SystemHttpProtocol::class.java)
                .disableAccount(object : HttpReq<SysProto.DisableAccountReq>() {
                    override fun getData(): SysProto.DisableAccountReq {
                        return SysHttpReqCreator.disableAccount()
                    }
                })
                .getResult(null, {
                    dialog.dismiss()
                    clearAccount(activity, tipMsg)
                }, {
                    dialog.dismiss()
                    BaseApp.app.toast(it.message.toString())
                })
    }

    override fun clearAccount(activity: AppCompatActivity, tipMsg: String) {
        val userId = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()

        ArouterServiceManager.messageService.deleteAccountData(userId)

        val commonPref = SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid())
        commonPref.putAppLockIsOn(false)
        commonPref.putAppLockFingerPrintIsOn(false)
        commonPref.putAppLockExipreTime(0)
        GestureUnlock.getInstance().clearGestureCode(activity, userId.toString())
        PinCodeUnlock.getInstance().clearPinCode(activity, userId.toString())

        try {
            FingerManager.updateFingerData(activity)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        BaseApp.app.onUserLogout(tipMsg)
    }
}