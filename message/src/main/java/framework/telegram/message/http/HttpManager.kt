package framework.telegram.message.http

import android.text.TextUtils
import com.im.domain.pb.CommonProto
import com.im.domain.pb.LoginProto
import com.trello.rxlifecycle3.RxLifecycle
import com.trello.rxlifecycle3.android.ActivityEvent
import com.trello.rxlifecycle3.android.FragmentEvent
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.http.creator.LoginHttpReqCreator
import framework.telegram.message.http.protocol.LoginHttpProtocol
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.ideas.common.http.BackupBizHostInterceptor
import framework.ideas.common.http.BackupLoginHostInterceptor
import framework.ideas.common.rlog.RLogManager
import framework.telegram.message.http.protocol.SystemHttpProtocol
import framework.telegram.support.BuildConfig
import framework.telegram.support.system.cache.data.CacheResult
import framework.telegram.support.system.network.http.HttpProtocolCreater
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.VersionUtils
import framework.telegram.support.tools.language.LocalManageUtil
import framework.telegram.ui.videoplayer.utils.NetworkUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by lzh on 19-5-23.
 * INFO:
 */
object HttpManager {

    private val defaultLoggingInterceptor: framework.telegram.support.system.network.http.log.LoggingInterceptor by lazy {
        framework.telegram.support.system.network.http.log.LoggingInterceptor.Builder()
                .loggable(BuildConfig.DEBUG)
                .setLevel(framework.telegram.support.system.network.http.log.Level.HEADERS)
                .request("Request")
                .response("Response")
                .logger { _, tag, msg ->
                    RLogManager.d(tag, msg)
                }
                .addHeader("versionName", BaseApp.VERSION_NAME)
                .addHeader("versionCode", BaseApp.VERSION_CODE.toString()).build()
    }

    private var backupLoginHostInterceptor = BackupLoginHostInterceptor()

    private var backupBizHostInterceptor = object : BackupBizHostInterceptor({
        val call = getStore(framework.telegram.message.http.protocol.LoginHttpProtocol::class.java).getUrlsCall(object : framework.telegram.support.system.network.http.HttpReq<com.im.domain.pb.LoginProto.GetUrlsReq>() {
            override fun getData(): LoginProto.GetUrlsReq {
                return LoginHttpReqCreator.createGetUrlsReq()
            }
        })

        val resp = call.execute()
        if (resp.code() == 200) {
            val urls = resp.body()?.urls
            if (urls != null && !TextUtils.isEmpty(urls.biz)) {
                RLogManager.d("Http", "${call.request().url()}请求成功，设置urls--->")
                ArouterServiceManager.systemService.saveUrls(urls)
                urls.biz
            } else {
                ""
            }
        } else {
            ""
        }
    }) {}

    private val loginHttpProtocol by lazy { HttpProtocolCreater.createProtoBufProtocol(Constant.Common.LOGIN_HTTP_HOST, LoginHttpProtocol::class.java, backupLoginHostInterceptor, defaultLoggingInterceptor) }

    private val otherHttpProtocolCache by lazy { HashMap<String, Any?>() }

    @Synchronized
    fun <T> getStore(httpStoreClazz: Class<T>): T {
        return if (httpStoreClazz == LoginHttpProtocol::class.java) {
            loginHttpProtocol as T
        } else {
            if (!otherHttpProtocolCache.containsKey(httpStoreClazz.name)) {
                val protocol = HttpProtocolCreater.createProtoBufProtocol(Constant.Common.BIZ_HTTP_HOST, httpStoreClazz, backupBizHostInterceptor, defaultLoggingInterceptor)
                otherHttpProtocolCache[httpStoreClazz.name] = protocol
            }

            otherHttpProtocolCache[httpStoreClazz.name] as T
        }
    }
}

fun getClientInfo(): CommonProto.ClientInfo {
    val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
    return CommonProto.ClientInfo.newBuilder()
            .setPlat(CommonProto.Platform.ANDROID)
            .setLanguage(7)
            .setAppVer(VersionUtils.getVersionName(BaseApp.HTTP_VERSION_NAME))
            .setPackageCode(BaseApp.app.getPackageChannel().channelCode)
            .setSessionId(accountInfo.getSessionId())
            .build()
}

fun getClientInfoWithOutSessionId(): CommonProto.ClientInfo {
    return CommonProto.ClientInfo.newBuilder()
            .setPlat(CommonProto.Platform.ANDROID)
            .setLanguage(7)
            .setAppVer(VersionUtils.getVersionName(BaseApp.HTTP_VERSION_NAME))
            .setPackageCode(BaseApp.app.getPackageChannel().channelCode)
            .build()
}

fun getAppLanguage(): Int {
    var lang = 1
    when (LocalManageUtil.getCurLanguaue()) {
        LocalManageUtil.SIMPLIFIED_CHINESE -> {
            lang = 2
        }
        LocalManageUtil.TRADITIONAL_CHINESE -> {
            lang = 3
        }
        LocalManageUtil.ENGLISH -> {
            lang = 1
        }
        LocalManageUtil.VI -> {
            lang = 4
        }
        LocalManageUtil.THAI -> {
            lang = 5
        }
    }
    return lang
}

inline fun <reified T> Observable<T>.getResult(observable: Observable<ActivityEvent>?
                                               , noinline onNext: (t: T) -> Unit, noinline onError: (t: Throwable) -> Unit): Disposable {
    var oldToken = AccountManager.getLoginAccount(AccountInfo::class.java).getSessionId()
    return if (observable == null) {
        subscribeOn(Schedulers.newThread())
    } else {
        compose(RxLifecycle.bindUntilEvent(observable, ActivityEvent.DESTROY)).subscribeOn(Schedulers.newThread())
    }.map {
        val method = T::class.java.getMethod("getCommonResult")
        val result = method.invoke(it) as CommonProto.CommonResult
        if (result.errCode == framework.telegram.business.bridge.Constant.Result.RESULT_SUCCESS) {
            it
        } else {
            throw HttpException(result.errCode, result.errMsg, result.flag)
        }
    }.retryWhen { throwableObservable ->
        throwableObservable.flatMap {
            if (it is HttpException) {
                if (ArouterServiceManager.systemService.intercept(it.errCode, oldToken)) {
                    oldToken = AccountManager.getLoginAccount(AccountInfo::class.java).getSessionId()
                    RLogManager.d("HttpManager", "重试一次--->")
                    Observable.just(0)
                } else {
                    Observable.error(it)
                }
            } else {
                if (!NetworkUtils.isAvailable(BaseApp.app)) {
                    Observable.error(Exception(BaseApp.app.getString(R.string.network_error)))
                } else {
                    Observable.error(Exception("${BaseApp.app.getString(R.string.network_error)} ${it.localizedMessage}"))
                }
            }
        }
    }.observeOn(AndroidSchedulers.mainThread()).subscribe({
        onNext.invoke(it)
    }, onError)
}

inline fun <reified T> Observable<CacheResult<T>>.getResultWithCache(observable: Observable<ActivityEvent>?
                                                                     , noinline onNext: (t: CacheResult<T>) -> Unit, noinline onError: (t: Throwable) -> Unit): Disposable {
    var oldToken = AccountManager.getLoginAccount(AccountInfo::class.java).getSessionId()
    return if (observable == null) {
        subscribeOn(Schedulers.newThread())
    } else {
        compose(RxLifecycle.bindUntilEvent(observable, ActivityEvent.DESTROY)).subscribeOn(Schedulers.newThread())
    }.map {
        val method = T::class.java.getMethod("getCommonResult")
        val result = method.invoke(it.data) as CommonProto.CommonResult
        if (result.errCode == framework.telegram.business.bridge.Constant.Result.RESULT_SUCCESS) {
            it
        } else {
            throw HttpException(result.errCode, result.errMsg, result.flag)
        }
    }.retryWhen { throwableObservable ->
        throwableObservable.flatMap {
            if (it is HttpException) {
                if (ArouterServiceManager.systemService.intercept(it.errCode, oldToken)) {
                    oldToken = AccountManager.getLoginAccount(AccountInfo::class.java).getSessionId()
                    RLogManager.d("HttpManager", "重试一次--->")
                    Observable.just(0)
                } else {
                    Observable.error(it)
                }
            } else {
                if (!NetworkUtils.isAvailable(BaseApp.app)) {
                    Observable.error(Exception(BaseApp.app.getString(R.string.network_error)))
                } else {
                    Observable.error(Exception("${BaseApp.app.getString(R.string.network_error)} ${it.localizedMessage}"))
                }
            }
        }
    }.observeOn(AndroidSchedulers.mainThread()).subscribe({
        onNext.invoke(it)
    }, onError)
}

inline fun <reified T> Observable<T>.getResultForFragment(observable: Observable<FragmentEvent>?
                                                          , noinline onNext: (t: T) -> Unit, noinline onError: (t: Throwable) -> Unit): Disposable {
    var oldToken = AccountManager.getLoginAccount(AccountInfo::class.java).getSessionId()
    return if (observable == null) {
        subscribeOn(Schedulers.newThread())
    } else {
        compose(RxLifecycle.bindUntilEvent(observable, FragmentEvent.DESTROY)).subscribeOn(Schedulers.newThread())
    }.map {
        val method = T::class.java.getMethod("getCommonResult")
        val result = method.invoke(it) as CommonProto.CommonResult
        if (result.errCode == framework.telegram.business.bridge.Constant.Result.RESULT_SUCCESS) {
            it
        } else {
            throw HttpException(result.errCode, result.errMsg, result.flag)
        }
    }.retryWhen { throwableObservable ->
        throwableObservable.flatMap {
            if (it is HttpException) {
                if (ArouterServiceManager.systemService.intercept(it.errCode, oldToken)) {
                    oldToken = AccountManager.getLoginAccount(AccountInfo::class.java).getSessionId()
                    RLogManager.d("HttpManager", "重试一次--->")
                    Observable.just(0)
                } else {
                    Observable.error(it)
                }
            } else {
                if (!NetworkUtils.isAvailable(BaseApp.app)) {
                    Observable.error(Exception(BaseApp.app.getString(R.string.network_error)))
                } else {
                    Observable.error(Exception("${BaseApp.app.getString(R.string.network_error)} ${it.localizedMessage}"))
                }
            }
        }
    }.observeOn(AndroidSchedulers.mainThread()).subscribe({
        onNext.invoke(it)
    }, onError)
}