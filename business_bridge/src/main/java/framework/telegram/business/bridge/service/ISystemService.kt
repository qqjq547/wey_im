package framework.telegram.business.bridge.service

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.template.IProvider
import com.im.domain.pb.LoginProto

interface ISystemService : IProvider {

    fun intercept(errCode: Int, oldToken: String): Boolean

    fun getAccountKeyVersion(uid: Long): Int

    fun getLoginAccountWebSecretKey(complete: (String, Int) -> Unit)

    fun getUserSecretKey(targetUid: Long, appVer: Int = -1, webVer: Int = -1, complete: (String, Int, String, Int) -> Unit, error: ((Throwable) -> Unit)? = null)

    fun getGroupSecretKey(targetGid: Long, complete: (String, Int) -> Unit, error: ((Throwable) -> Unit)? = null)

    fun updateUserPublicKey(targetUid: Long, appkeyVer: Int, webKeyVer: Int, complete: (String, Int, String, Int) -> Unit, error: ((Throwable) -> Unit)?)

    fun updateGroupPublicKey(targetGid: Long, complete: (String, Int, String) -> Unit, error: ((Throwable) -> Unit)?)

    fun updateAccountKey(publicKey: String, complete: () -> Unit, error: ((Throwable) -> Unit)? = null)

    fun clearMyselfWebSecretKeysCache()

    fun saveUrls(it: LoginProto.UrlInfo)

    fun openAppFingerprintIdentifyActivity()

    fun openAppGestureUnlockActivity()

    fun openAppConfirmPinUnlockActivity()

    fun checkAppUnlockActivity(activity: Activity)

    fun isAppLockOn(): Boolean

    fun disableAccount(activity: AppCompatActivity, tipMsg: String = "")

    fun clearAccount(activity: AppCompatActivity, tipMsg: String = "")
}
