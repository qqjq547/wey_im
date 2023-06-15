package framework.telegram.app

//import com.fm.openinstall.OpenInstall
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.LauncherActivity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.load.engine.cache.DiskCache
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.codersun.fingerprintcompat.FingerManager
import com.dds.gestureunlock.GestureUnlock
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory
import com.fm.openinstall.OpenInstall
import com.manusunny.pinlock.PinCodeUnlock
import com.meituan.android.walle.WalleChannelReader
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import de.greenrobot.common.Base64
import framework.ideas.common.http.BackupLoginHostInterceptor
import framework.ideas.common.rlog.RLogManager
import framework.telegram.app.keepalive.utils.isMainProcess
import framework.telegram.app.keepalive.utils.isTargetProcess
import framework.telegram.app.push.PushManager
import framework.telegram.app.push.PushManagerEventHandler
import framework.telegram.app.push.PushService
import framework.telegram.business.BusinessApplication
import framework.telegram.business.TokenReqInterceptor
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.event.UserInfoChangeEvent
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.sp.CommonPref
import framework.telegram.business.ui.login.GetSmsCodeActivity
import framework.telegram.business.ui.login.LoginFirstActivity
import framework.telegram.business.ui.login.LoginSecondActivity
import framework.telegram.business.ui.other.AppConfirmPinUnlockActivity
import framework.telegram.business.ui.other.AppFingerprintIdentifyActivity
import framework.telegram.business.ui.other.AppGestureUnlockActivity
import framework.telegram.message.MessageApplication
import framework.telegram.message.bridge.event.NotificationEvent
import framework.telegram.message.connect.MessageSocketService
import framework.telegram.message.manager.ReceiveMessageManager
import framework.telegram.message.manager.SoundPoolManager
import framework.telegram.message.ui.location.ClientLocationManager
import framework.telegram.message.ui.location.bean.ClientLatLng
import framework.telegram.message.ui.location.bean.ClientLocation
import framework.telegram.message.ui.telephone.core.RtcEngineHolder
import framework.telegram.support.BaseApp
import framework.telegram.support.ChannelInfoBean
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.gson.GsonInstanceCreater
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.system.network.http.HttpClientCreater
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.system.upload.Constant.Common.BASE_URL_UPLOAD
import framework.telegram.support.system.upload.Constant.Common.UPLOAD_WAY_TYPE
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.download.DownloadEventHandler
import framework.telegram.support.tools.exception.ClientException
import framework.telegram.support.tools.file.DirManager
import framework.telegram.support.tools.language.LocalManageUtil
import framework.telegram.support.tools.language.MultiLanguage
import framework.telegram.support.tools.wordfilter.WordFilter
import framework.telegram.ui.doubleclick.helper.ViewDoubleHelper
import framework.telegram.ui.emoji.EmojiManager
import framework.telegram.ui.emoji.ios.IosEmojiProvider
import io.reactivex.plugins.RxJavaPlugins
import io.realm.Realm
import java.io.File
import java.io.IOException
import java.util.*

class App : BaseApp() {

    override fun attachBaseContext(base: Context) {
        //第一次进入app时保存系统选择语言(为了选择随系统语言时使用，如果不保存，切换语言后就拿不到了）
        LocalManageUtil.saveSystemCurrentLanguage(base)
        super.attachBaseContext(MultiLanguage.setLocal(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        //用户在系统设置页面切换语言时保存系统选择语言(为了选择随系统语言时使用，如果不保存，切换语言后就拿不到了）
        /*LocalManageUtil.saveSystemCurrentLanguage(applicationContext, newConfig)
        MultiLanguage.onConfigurationChanged(applicationContext)*/
    }

    override fun onCreate() {
        super.onCreate()
        if (isMainProcess(this@App)) {
            // 初始化数据库，必须在第一时间
            initRealm()

            // 初始化RLog模块
            RLogManager.init { cmd, complete, error ->
                RealmCreator.executeRLogTransactionAsync(cmd, complete, error)
            }
            RLogManager.i("Application", "应用已重启------------>")

            // 初始化Activity监听
            initActivityLifecycle()

            // 初始化业务用到的Url
            initEnvironmentUrl()

            // 初始化RX异常打印和上报
            RxJavaPlugins.setErrorHandler {
                //异常处理
//                RLogManager.e("Error", it)
                MobclickAgent.reportError(app, it)
            }

            //初始化各种组件
            initArouter()
            initFresco()
            initGlide()
            initUmeng()
            initOpeninstall()
            initLeakCanary()

            // 初始化emoji表情
            EmojiManager.install(IosEmojiProvider())
            // 初始化音效管理
            SoundPoolManager.init()

            //初始化手势功能
            GestureUnlock.getInstance().init(applicationContext)
            PinCodeUnlock.getInstance().init(applicationContext)

            //初始化多语言
            MultiLanguage.init {
                return@init LocalManageUtil.getSetLanguageLocale()
            }
            MultiLanguage.setApplicationLanguage(this)

            //注册事件
            registerEvents()

            // 初始化业务模块
            BusinessApplication.init(this@App)
            MessageApplication.init(this@App)

            // 异步初始化
            initThread()

            // 自动登录当前账号
            TokenReqInterceptor.autoLoginCurrentAccount {
                MessageApplication.startMessageSocket()
            }

            initFilterWords()
        }

        //这个要主进程 和 channel进程初始化
        initPushService()
    }

    private fun initFilterWords() {
        WordFilter.addDetaultSensitiveWords()
//        try {
//            val sp = SharePreferencesStorage.createStorageInstance(
//                framework.telegram.message.sp.CommonPref::class.java
//            )
//
//            if (System.currentTimeMillis() - sp.getFilterWordsTime() > 2 * 24 * 60 * 60 * 1000) {
//                HttpClientCreater.newOkHttpClient().newCall(
//                    Request.Builder().get()
//                        .url("http://profile-resource.oss-cn-hongkong.aliyuncs.com/config/keyword.txt")
//                        .build()
//                ).enqueue(object : Callback {
//
//                    override fun onFailure(call: Call, e: IOException) {
//                        WordFilter.addDetaultSensitiveWords()
//                    }
//
//                    override fun onResponse(call: Call, response: Response) {
//                        try {
//                            if (response.isSuccessful) {
//                                val words = response.body()?.string() ?: ""
//                                val keywords = String(Base64.decode(words))
//
//                                sp.putFilterWords(words)
//                                sp.putFilterWordsTime(System.currentTimeMillis())
//                                WordFilter.addSensitiveWords(keywords)
//                            } else {
//                                WordFilter.addDetaultSensitiveWords()
//                            }
//                        } catch (e: Exception) {
//                        }
//                    }
//                })
//            } else {
//                WordFilter.addSensitiveWords(String(Base64.decode(sp.getFilterWords())))
//            }
//        } catch (e: Exception) {
//        }
    }

    private fun initFresco() {
        val config = OkHttpImagePipelineConfigFactory
            .newBuilder(this, HttpClientCreater.imageLoaderOkHttpClient)
            .setDownsampleEnabled(true)
            .build()

        Fresco.initialize(this@App, config)
    }

    private fun initGlide() {
//        val uuid = if (!AccountManager.hasLoginAccount()) UUID.nameUUIDFromBytes("default".toByteArray()).toString() else AccountManager.getLoginAccountUUid()
        val uuid = UUID.nameUUIDFromBytes("default".toByteArray()).toString()
        val cacheDir =
            File(DirManager.getImageCacheDir(this, uuid), DiskCache.Factory.DEFAULT_DISK_CACHE_DIR)
        Glide.init(
            this,
            GlideBuilder().setDiskCache(
                DiskLruCacheFactory(
                    cacheDir.absolutePath,
                    100 * 1000 * 1000L
                )
            )
        )
    }

    @SuppressLint("CheckResult")
    private fun registerEvents() {
        PushManagerEventHandler().initEvent()
        DownloadEventHandler().initEvent()
    }

    private fun initActivityLifecycle() {
        app.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks)
    }

    private fun initArouter() {
        if (BuildConfig.DEBUG) {
            // 这两行必须写在init之前，否则这些配置在init过程中将无效
            ARouter.openLog()     // 打印日志
            ARouter.openDebug()   // 开启调试模式(如果在InstantRun模式下运行，必须开启调试模式！线上版本需要关闭,否则有安全风险)
        }
        ARouter.init(this@App) // 尽可能早，推荐在Application中初始化
    }

    private fun initEnvironmentUrl() {
        val sp = SharePreferencesStorage.createStorageInstance(CommonPref::class.java)
        // 普通业务用到的URLs
        Constant.Common.LOGIN_HTTP_HOST = sp.getLoginUrl()
        Constant.Common.BIZ_HTTP_HOST = sp.getBizUrl()
        Constant.Common.BIZ_HTTP_CONTACT = sp.getFriendUrl()
        Constant.Common.BIZ_HTTP_GROUP = sp.getGroupUrl()
        Constant.Common.MAP_HTTP_HOST = sp.getMapUrl()
        Constant.Common.DOWNLOAD_HTTP_HOST = sp.getDownloadUrl()

        // 消息用到的URLs
        framework.telegram.message.bridge.Constant.Common.LOGIN_HTTP_HOST = sp.getLoginUrl()
        framework.telegram.message.bridge.Constant.Common.BIZ_HTTP_HOST = sp.getBizUrl()
        framework.telegram.message.bridge.Constant.Common.MAP_HTTP_HOST = sp.getMapUrl()

        BASE_URL_UPLOAD = sp.getUploadUrl()
        UPLOAD_WAY_TYPE = sp.getUploadServer()

        // 备用host使用到的URLs
        BackupLoginHostInterceptor.CONFIG_HTTP_HOST = sp.getConfigUrl()

        RLogManager.d("Http", "初始化LOGIN_HTTP_HOST--->${Constant.Common.LOGIN_HTTP_HOST}")
        RLogManager.d("Http", "初始化BIZ_HTTP_HOST--->${Constant.Common.BIZ_HTTP_HOST}")
        RLogManager.d("Http", "初始化BIZ_HTTP_CONTACT--->${Constant.Common.BIZ_HTTP_CONTACT}")
        RLogManager.d("Http", "初始化BIZ_HTTP_GROUP--->${Constant.Common.BIZ_HTTP_GROUP}")
        RLogManager.d("Http", "初始化DOWNLOAD_HTTP_HOST--->${Constant.Common.DOWNLOAD_HTTP_HOST}")
        RLogManager.d(
            "Http",
            "初始化CONFIG_HTTP_HOST--->${BackupLoginHostInterceptor.CONFIG_HTTP_HOST}"
        )
        RLogManager.d("Http", "初始化MAP_HTTP_HOST--->${Constant.Common.MAP_HTTP_HOST}")
    }

    private fun initPushService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PushManager.initChannel()
        }

        //只有release 包才会注册推送
        if (isEnablePush() && (isMainProcess(this@App) || isTargetProcess(
                this@App,
                "${applicationContext.packageName}:channel"
            ))
        ) {
            PushService.registerPushService(this@App)
        }
    }

    private fun initRealm() {
        Realm.init(this@App)
    }

    private fun initUmeng() {
        UMConfigure.init(
            this@App,
            if (BuildConfig.DEBUG) "5ecb86c0570df3c1e7000229" else "5ecb869b570df3cc6c000118",
            "${getPackageChannel().channelCode}",
            UMConfigure.DEVICE_TYPE_PHONE,
            null
        )
        UMConfigure.setEncryptEnabled(true)
        UMConfigure.setLogEnabled(BuildConfig.DEBUG)
        MobclickAgent.setScenarioType(this@App, MobclickAgent.EScenarioType.E_UM_NORMAL)
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.MANUAL)//手动统计页面
        MobclickAgent.setCatchUncaughtExceptions(true)//开启崩溃日志搜集功能
    }

    private fun initOpeninstall() {
        OpenInstall.init(this)
    }

    override fun onUserLogin() {
        super.onUserLogin()

        RLogManager.d("User", "onUserLogin-------->")

        val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
        val userIdStr = "${accountInfo.getUserId()}"
        val hwToken = accountInfo.getHwToken()

        //设置umeng统计的uid
        MobclickAgent.onProfileSignIn(userIdStr)

        // 初始化推送
        if (isEnablePush()) {
            PushService.bindAccountWithPushService(userIdStr, hwToken)
        }

        //其他模块的登录操作
        BusinessApplication.onUserLogin()
        MessageApplication.onUserLogin()
    }

    override fun onUserChangeToken() {
        super.onUserChangeToken()

        RLogManager.d("User", "onUserChangeToken-------->")

        BusinessApplication.onUserChangeToken()
        MessageApplication.onUserChangeToken()
    }

    override fun onUserInfoChange() {
        EventBus.publishEvent(UserInfoChangeEvent())
    }

    override fun onUserLogout(tipMsg: String, jumpToNewAccountLogin: Boolean) {
        super.onUserLogout(tipMsg, jumpToNewAccountLogin)

        RLogManager.d("User", "onUserLogout--------> $tipMsg")

        //其他模块的注销操作
        BusinessApplication.onUserLogout()
        MessageApplication.onUserLogout()

        //删除当前登录者的uuid
        AccountManager.removeLoginAccountUUid()

        //关闭推送
        if (isEnablePush()) {
            PushService.unbindAccountWithPushService()
        }

        //清除umeng统计的uid
        MobclickAgent.onProfileSignOff()

        //关闭所有页面
        ActivitiesHelper.getInstance().closeAll()

        //打开登录页面
        if (jumpToNewAccountLogin) {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_LOGIN_FIRST)
                .withString("tipMsg", tipMsg).withInt("type", 0).navigation()
        } else {
            if (TextUtils.isEmpty(AccountManager.getLastLoginAccountUuid())) {
                //第一次登录
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_LOGIN_FIRST)
                    .withString("tipMsg", tipMsg).navigation()
            } else {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_LOGIN_SECOND)
                    .withString("tipMsg", tipMsg).navigation()
            }
        }
    }

    private var mPackageChannelInfo: ChannelInfoBean? = null
    override fun getPackageChannel(): ChannelInfoBean {
        if (mPackageChannelInfo != null) {
            return mPackageChannelInfo!!
        } else {
            try {
                val copyFileStr = WalleChannelReader.getChannel(app)
                if (!TextUtils.isEmpty(copyFileStr)) {
                    val data = copyFileStr?.split(",".toRegex())?.dropLastWhile { it.isEmpty() }
                        ?.toTypedArray()
                    if (data != null && data.size >= 3) {
                        val channelInfo = ChannelInfoBean()
                        channelInfo.channelName = data[0]
                        channelInfo.channelCode = Integer.valueOf(data[1])
                        channelInfo.isInstallPatch = TextUtils.isEmpty(data[2]) || "1" == data[2]
                        AppLogcat.logger.d(
                            "读取到渠道信息--->" + GsonInstanceCreater.defaultGson.toJson(
                                channelInfo
                            )
                        )
                        mPackageChannelInfo = channelInfo
                        return channelInfo
                    }
                }

                throw IOException()
            } catch (e: IOException) {
                e.printStackTrace()
                AppLogcat.logger.e("读取渠道信息失败")
            }

            val defaultChannel = ChannelInfoBean()
            AppLogcat.logger.d(
                "使用默认渠道信息--->" + GsonInstanceCreater.defaultGson.toJson(
                    defaultChannel
                )
            )
            mPackageChannelInfo = defaultChannel
            return defaultChannel
        }
    }

    companion object {

        private val mActivityLifecycleCallbacks by lazy {
            object : ActivityLifecycleCallbacks {
                override fun onActivityPaused(activity: Activity) {

                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    ActivitiesHelper.getInstance().addActivity(activity)
                }

                override fun onActivityStarted(activity: Activity) {
                    ActivitiesHelper.getInstance().addActivityCount()
                    if (ActivitiesHelper.getInstance().toForeground()) {
                        // 清除通知栏消息
                        EventBus.publishEvent(
                            NotificationEvent(
                                0,
                                "",
                                framework.telegram.message.bridge.Constant.Push.PUSH_TYPE.CLEAR_ALL_NOTIFICATION
                            )
                        )

                        // 登录socket
                        if (ReceiveMessageManager.socketIsLogin && ((System.currentTimeMillis() - ActivitiesHelper.getInstance().lastBackgroundAppTime) > 60 * 1000)) {
                            MessageSocketService.loginAccount()
                        }
                    }

                    // 应用锁逻辑
                    if (AccountManager.hasLoginAccount()) {
                        val commonPref = SharePreferencesStorage.createStorageInstance(
                            CommonPref::class.java,
                            AccountManager.getLoginAccountUUid()
                        )
                        // 如果是从后台切换到前台，则计算用户离开的时间，超过时间则弹出解锁
                        val leftBackgroundTime =
                            (System.currentTimeMillis() - ActivitiesHelper.getInstance().lastBackgroundAppTime) / 1000
                        if (leftBackgroundTime > commonPref.getAppLockExipreTime()) {
                            if (ActivitiesHelper.getInstance().countActivity == 1) {
                                // 当前为从后台切换到前台，且存在已登录的用户
                                if (activity.javaClass.name != AppGestureUnlockActivity::class.java.name
                                    && activity.javaClass.name != AppFingerprintIdentifyActivity::class.java.name
                                ) {
                                    checkAppLock(activity, commonPref)
                                }
                            } else if (ActivitiesHelper.getInstance().countActivity == 2) {
                                val secondActivity = ActivitiesHelper.getInstance().secondActivity
                                if (secondActivity.javaClass.name == LoginSecondActivity::class.java.name
                                    || secondActivity.javaClass.name == LoginFirstActivity::class.java.name
                                    || secondActivity.javaClass.name == GetSmsCodeActivity::class.java.name
                                ) {
                                    checkAppLock(activity, commonPref)
                                }
                            }
                        }
                    }
                }

                override fun onActivityResumed(activity: Activity) {
                    ThreadUtils.runOnUIThread(1000) {
                        if (AccountManager.hasLoginAccount() && RtcEngineHolder.pendingCall != null) {
                            if (activity.javaClass.name != LauncherActivity::class.java.name) {
                                var hasAppGestureUnlockActivity = false
                                var hasAppConfirmPinUnlockActivity = false
                                var hasAppFingerprintIdentifyActivity = false
                                if (ActivitiesHelper.getInstance()
                                        .hasActivity("framework.telegram.business.ui.other.AppGestureUnlockActivity")
                                ) {
                                    hasAppGestureUnlockActivity = true
                                } else if (ActivitiesHelper.getInstance()
                                        .hasActivity("framework.telegram.business.ui.other.AppConfirmPinUnlockActivity")
                                ) {
                                    hasAppConfirmPinUnlockActivity = true
                                } else if (ActivitiesHelper.getInstance()
                                        .hasActivity("framework.telegram.business.ui.other.AppFingerprintIdentifyActivity")
                                ) {
                                    hasAppFingerprintIdentifyActivity = true
                                }

                                if (!hasAppGestureUnlockActivity && !hasAppConfirmPinUnlockActivity && !hasAppFingerprintIdentifyActivity) {
                                    val pendingCall = RtcEngineHolder.pendingCall
                                    RtcEngineHolder.pendingCall = null
                                    if (pendingCall != null && System.currentTimeMillis() - pendingCall.time < 60 * 1000) {
                                        ThreadUtils.runOnUIThread(800) {
                                            ARouter.getInstance()
                                                .build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO)
                                                .withLong("targetUid", pendingCall.targetUid)
                                                .withInt("streamType", pendingCall.streamType)
                                                .withInt("openType", pendingCall.openType)
                                                .navigation()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onActivityStopped(activity: Activity) {
                    ActivitiesHelper.getInstance().reduceActivityCount()

                    if (ActivitiesHelper.getInstance().toBackgroud()) {
                        ActivitiesHelper.getInstance().lastBackgroundAppTime =
                            System.currentTimeMillis()
                    }
                }

                override fun onActivityDestroyed(activity: Activity) {
                    ActivitiesHelper.getInstance().removeActivity(activity)
                }
            }
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
                            AppFingerprintIdentifyActivity.gotoVerifyFingerprint(
                                activity,
                                userId.toString(),
                                userIcon
                            )
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
            } else {
                // 没有设置Pin密码，是否设置手势密码
                if (GestureUnlock.getInstance().isGestureCodeSet(activity, uid.toString())) {
                    // 验证手势密码
                    AppGestureUnlockActivity.gotoVerifyGestureCode(
                        activity,
                        uid.toString(),
                        icon,
                        false
                    )
                } else {
                    // 没有设置手势密码，取消安全锁
                    val commonPref = SharePreferencesStorage.createStorageInstance(
                        CommonPref::class.java,
                        AccountManager.getLoginAccountUUid()
                    )
                    commonPref.putAppLockIsOn(false)
                    commonPref.putAppLockFingerPrintIsOn(false)
                }
            }
        }
    }

    private fun isEnablePush(): Boolean {
        return !IS_TEST_SERVER
    }

    private fun initLeakCanary() {
        if (IS_JENKINS_BUILD)
            return

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            refWatcher = RefWatcher.DISABLED
        }
        refWatcher = LeakCanary.install(this)
    }

    private fun requestLocation() {
        val clientLocationManager =
            ClientLocationManager(this, object : ClientLocationManager.ClientLocationListener {

                override fun onSuccess(
                    clientLocationManager: ClientLocationManager,
                    clientLatLng: ClientLatLng?
                ) {
                }

                override fun onSuccess(
                    clientLocationManager: ClientLocationManager,
                    clientLocation: ClientLocation?
                ) {
                }

                override fun onError(
                    clientLocationManager: ClientLocationManager,
                    e: ClientException
                ) {
                    MobclickAgent.reportError(this@App, e)
                }
            })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                clientLocationManager.requestLocationAndAddress(false)
            }
        } else {
            clientLocationManager.requestLocationAndAddress(false)
        }
    }

    private fun initThread() {
        ThreadUtils.runOnIOThread {
            requestLocation()

            //处理快速重复点击（替换view的点击事件）
            ViewDoubleHelper.init(this, 500, UnifiedDoubleClick::class.java) //默认时间：1秒
        }
    }
}
