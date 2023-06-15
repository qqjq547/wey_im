package framework.telegram.support

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Configuration
import androidx.multidex.MultiDexApplication
import com.squareup.leakcanary.RefWatcher
import framework.telegram.support.BuildConfig.IS_JENKINS
import framework.telegram.support.BuildConfig.JENKINS_IS_TEST_SERVER
import framework.telegram.support.tools.language.LocalManageUtil
import framework.telegram.support.tools.language.MultiLanguage

open class BaseApp : MultiDexApplication() {

    var refWatcher: RefWatcher? = null

    override fun onCreate() {
        super.onCreate()
        app = this

        val packageInfo: PackageInfo? = try {
            packageManager.getPackageInfo(packageName, 0)
        } catch (e: NameNotFoundException) {
            e.printStackTrace()
            null
        }

        val index: Int = packageInfo?.versionName?.indexOf(" ") ?: -1
        VERSION_NAME = if (index > 0) {
            packageInfo?.versionName?.substring(0, index) ?: ""
        } else {
            packageInfo?.versionName ?: ""
        }

        VERSION_CODE = packageInfo?.versionCode ?: 0
    }

    open fun onUserLogin() {
    }

    open fun onUserChangeToken() {
    }

    open fun onUserLogout(tipMsg: String, jumpToNewAccountLogin: Boolean = false) {
    }

    open fun onUserInfoChange() {
    }

    open fun getPackageChannel(): ChannelInfoBean {
        return ChannelInfoBean()
    }

    companion object {
        lateinit var app: BaseApp

        var IS_TEST_SERVER = JENKINS_IS_TEST_SERVER
        var IS_JENKINS_BUILD = IS_JENKINS

        var LOG_DIR_NAME = "68telegram"

        /**
         * 应用版本名
         */
        var VERSION_NAME: String = ""


        /**
         * 应用版本号
         */
        var VERSION_CODE: Int = 0
    }
}