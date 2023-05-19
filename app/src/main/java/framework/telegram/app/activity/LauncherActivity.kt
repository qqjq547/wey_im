package framework.telegram.app.activity

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.telegram.app.Constant.ARouter.ROUNTE_APP_LAUNCHER
import framework.telegram.app.R
import framework.telegram.app.push.PushProtocol
import framework.telegram.business.bridge.Constant
import framework.telegram.business.sp.CommonPref
import framework.telegram.support.BaseActivity
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.NotificationUtils
import kotlin.concurrent.timer
import android.content.Intent
import android.widget.Toast
import com.fm.openinstall.OpenInstall
import com.fm.openinstall.listener.AppWakeUpAdapter
import com.fm.openinstall.model.AppData
import framework.telegram.app.Constant.ARouter.ROUNTE_APP_WELCOME
import framework.telegram.business.BuildConfig
import framework.telegram.support.BaseApp
import framework.telegram.support.system.log.core.utils.SysUtils
import kotlinx.android.synthetic.main.app_launcher_activity.*

/**
 * Created by lzh on 19-5-22.
 * INFO: 阿里云 第三方通道推送 需要集成AndroidPopupActivity
 */
@Route(path = ROUNTE_APP_LAUNCHER)
class LauncherActivity : BaseActivity() {

    private var mNoticeMap: Map<String, String>? = null

    private val mSp by lazy { SharePreferencesStorage.createStorageInstance(CommonPref::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_launcher_activity)
        app_text_view_version.text = "v${SysUtils.getAppVersionName(this)}.${BuildConfig.versionDateTime}"

        //第一次安装应用，如果通知 未打开,且从未设置过，就红点显示
        if (NotificationUtils.notificationSwitchOn(this@LauncherActivity)) {
            SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()).putFirstOpenMessagePermission(true)
        }

        if (!TextUtils.isEmpty(mSp.getErrorRealm())) {
            Toast.makeText(this@LauncherActivity, getString(R.string.system_crash_tip), Toast.LENGTH_LONG).show()
        }

        OpenInstall.getWakeUp(intent, wakeUpAdapter)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 此处要调用，否则App在后台运行时，会无法截获
        OpenInstall.getWakeUp(intent, wakeUpAdapter)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            var time = 2
            timer(period = 1000, action = {
                time--
                if (time == 0 && hasWindowFocus()) {
                    cancel()
                    if (!AccountManager.hasLoginAccount()) {
                        //没有登录账户
                        if (TextUtils.isEmpty(AccountManager.getLastLoginAccountUuid())) {
                            //第一次登录
                            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_LOGIN_FIRST).navigation()
                        } else {
                            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_LOGIN_SECOND).navigation()
                        }
                    } else {
                        val time = SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()).getLastManualLoginTime()
                        if (time != 0L && (System.currentTimeMillis() > time)) {
                            BaseApp.app.onUserLogout(BaseApp.app.getString(R.string.long_time_no_login))
                        } else {
                            //有已登录账户
                            ARouter.getInstance().build(framework.telegram.app.Constant.ARouter.ROUNTE_APP_MAIN).navigation()
                            if (mNoticeMap != null) {
                                PushProtocol.pushProtocolManager(mNoticeMap)
                            }
                        }
                    }
                    if (!mSp.getCheckWelcomePage()) {
                        ARouter.getInstance().build(ROUNTE_APP_WELCOME).navigation()
                        mSp.putCheckWelcomePage(true)
                    }
                    finish()
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeUpAdapter = null
    }

    private var wakeUpAdapter: AppWakeUpAdapter? = object : AppWakeUpAdapter() {
        override fun onWakeUp(appData: AppData) {
            //获取渠道数据
            val channelCode = appData.getChannel()
            //获取绑定数据
            val bindData = appData.getData()
            mSp.putWakeupBindData(bindData)
            mSp.putChannelCode(channelCode)
            Log.d("OpenInstall", "getWakeUp : wakeupData = $appData")
        }
    }
}