package framework.telegram.message.db

import android.annotation.SuppressLint
import android.text.TextUtils
import com.umeng.analytics.MobclickAgent
import framework.ideas.common.db.RealmCreatorManager
import framework.telegram.message.sp.CommonPref
import framework.telegram.support.BaseApp
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm

object RealmCreator {

    @Synchronized
    fun getPvtChatMessagesRealm(myUid: Long, targetUid: Long): Realm {
        val sp = SharePreferencesStorage.createStorageInstance(
            CommonPref::class.java,
            "${myUid}_pvt_msg_to_${targetUid}_db"
        )
        val realmDataVersion = sp.getRealmDataVersion()
        val errorRealm = sp.getErrorRealm()
        val config = if (!TextUtils.isEmpty(errorRealm)) {
            val newVersion = realmDataVersion + 1
            sp.putRealmDataVersion(newVersion)
            sp.putErrorRealm("")
            RealmCreatorManager.getPvtConfig(myUid, targetUid, newVersion)
        } else {
            RealmCreatorManager.getPvtConfig(myUid, targetUid, realmDataVersion)
        }

        sp.putErrorRealm(config.realmFileName)
        val instance = Realm.getInstance(config)
        sp.putErrorRealm("")
        return instance
    }

    @Synchronized
    fun getGroupChatMessagesRealm(myUid: Long, groupId: Long): Realm {
        val sp = SharePreferencesStorage.createStorageInstance(
            CommonPref::class.java,
            "${myUid}_group_msg_to_${groupId}_db"
        )
        val realmDataVersion = sp.getRealmDataVersion()
        val errorRealm = sp.getErrorRealm()
        val config = if (!TextUtils.isEmpty(errorRealm)) {
            val newVersion = realmDataVersion + 1
            sp.putRealmDataVersion(newVersion)
            sp.putErrorRealm("")
            RealmCreatorManager.getGroupConfig(myUid, groupId, newVersion)
        } else {
            RealmCreatorManager.getGroupConfig(myUid, groupId, realmDataVersion)
        }

        sp.putErrorRealm(config.realmFileName)
        val instance = Realm.getInstance(config)
        sp.putErrorRealm("")
        return instance
    }

    @Synchronized
    private fun getCommonRealm(myUid: String): Realm {
        val sp = SharePreferencesStorage.createStorageInstance(
            CommonPref::class.java,
            "${myUid}_common_db"
        )
        val realmDataVersion = sp.getRealmDataVersion()
        val errorRealm = sp.getErrorRealm()
        val config = if (!TextUtils.isEmpty(errorRealm)) {
            val newVersion = realmDataVersion + 1
            sp.putRealmDataVersion(newVersion)
            sp.putErrorRealm("")
            RealmCreatorManager.getCommonConfig(myUid, newVersion)
        } else {
            RealmCreatorManager.getCommonConfig(myUid, realmDataVersion)
        }

        sp.putErrorRealm(config.realmFileName)
        val instance = Realm.getInstance(config)
        sp.putErrorRealm("")
        return instance
    }

    fun getStreamCallHistoryRealm(myUid: Long): Realm {
        return getCommonRealm(myUid.toString())
    }

    fun getChatsHistoryRealm(myUid: Long): Realm {
        return getCommonRealm(myUid.toString())
    }

    fun getDeleteChatsHistoryRealm(myUid: Long): Realm {
        return getCommonRealm("${myUid}_backup")
    }

    fun getSearchDbRealm(myUid: Long): Realm {
        return getCommonRealm(myUid.toString())
    }

    @SuppressLint("CheckResult")
    fun executePvtChatTransactionAsync(
        myUid: Long,
        targetUid: Long,
        cmd: (Realm) -> Unit,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        Observable.create<Realm> {
            it.onNext(getPvtChatMessagesRealm(myUid, targetUid))
            it.onComplete()
        }.subscribeOn(AndroidSchedulers.mainThread()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({ realm ->
                realm.executeTransactionAsync({ r ->
                    cmd.invoke(r)
                }, {
                    realm.close()
                    complete?.invoke()
                }, {
                    realm.close()
                    it.printStackTrace()
                    error?.invoke(it)
                })
            }, {
                AppLogcat.logger.e(it)
                MobclickAgent.reportError(BaseApp.app, it)
            })
    }

    @SuppressLint("CheckResult")
    fun <T> executePvtChatTransactionAsyncWithResult(
        myUid: Long,
        targetUid: Long,
        cmd: (Realm) -> T?,
        complete: ((T?) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        var result: T? = null
        Observable.create<Realm> {
            it.onNext(getPvtChatMessagesRealm(myUid, targetUid))
            it.onComplete()
        }.subscribeOn(AndroidSchedulers.mainThread()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({ realm ->
                realm.executeTransactionAsync({ r ->
                    result = cmd.invoke(r)
                }, {
                    realm.close()
                    complete?.invoke(result)
                }, {
                    realm.close()
                    it.printStackTrace()
                    error?.invoke(it)
                })
            }, {
                AppLogcat.logger.e(it)
                MobclickAgent.reportError(BaseApp.app, it)
            })
    }

    @SuppressLint("CheckResult")
    fun executeGroupChatTransactionAsync(
        myUid: Long,
        targetGid: Long,
        cmd: (Realm) -> Unit,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        Observable.create<Realm> {
            it.onNext(getGroupChatMessagesRealm(myUid, targetGid))
            it.onComplete()
        }.subscribeOn(AndroidSchedulers.mainThread()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({ realm ->
                realm.executeTransactionAsync({ r ->
                    cmd.invoke(r)
                }, {
                    realm.close()
                    complete?.invoke()
                }, {
                    realm.close()
                    it.printStackTrace()
                    error?.invoke(it)
                })
            }, {
                AppLogcat.logger.e(it)
                MobclickAgent.reportError(BaseApp.app, it)
            })
    }

    @SuppressLint("CheckResult")
    fun <T> executeGroupChatTransactionAsyncWithResult(
        myUid: Long,
        targetGid: Long,
        cmd: (Realm) -> T?,
        complete: ((T?) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        var result: T? = null
        Observable.create<Realm> {
            it.onNext(getGroupChatMessagesRealm(myUid, targetGid))
            it.onComplete()
        }.subscribeOn(AndroidSchedulers.mainThread()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({ realm ->
                realm.executeTransactionAsync({ r ->
                    result = cmd.invoke(r)
                }, {
                    realm.close()
                    complete?.invoke(result)
                }, {
                    realm.close()
                    it.printStackTrace()
                    error?.invoke(it)
                })
            }, {
                AppLogcat.logger.e(it)
                MobclickAgent.reportError(BaseApp.app, it)
            })
    }

    @SuppressLint("CheckResult")
    fun executeStreamCallsHistoryTransactionAsync(
        myUid: Long,
        cmd: (Realm) -> Unit,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        Observable.create<Realm> {
            it.onNext(getStreamCallHistoryRealm(myUid))
            it.onComplete()
        }.subscribeOn(AndroidSchedulers.mainThread()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({ realm ->
                realm.executeTransactionAsync({ r ->
                    cmd.invoke(r)
                }, {
                    realm.close()
                    complete?.invoke()
                }, {
                    realm.close()
                    it.printStackTrace()
                    error?.invoke(it)
                })
            }, {
                AppLogcat.logger.e(it)
                MobclickAgent.reportError(BaseApp.app, it)
            })
    }

    @SuppressLint("CheckResult")
    fun executeChatsHistoryTransactionAsync(
        myUid: Long,
        cmd: (Realm) -> Unit,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        Observable.create<Realm> {
            it.onNext(getChatsHistoryRealm(myUid))
            it.onComplete()
        }.subscribeOn(AndroidSchedulers.mainThread()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({ realm ->
                realm.executeTransactionAsync({ r ->
                    cmd.invoke(r)
                }, {
                    realm.close()
                    complete?.invoke()
                }, {
                    realm.close()
                    it.printStackTrace()
                    error?.invoke(it)
                })
            }, {
                AppLogcat.logger.e(it)
                MobclickAgent.reportError(BaseApp.app, it)
            })
    }

    @SuppressLint("CheckResult")
    fun executeDeleteChatsHistoryTransactionAsync(
        myUid: Long,
        cmd: (Realm) -> Unit,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        Observable.create<Realm> {
            it.onNext(getDeleteChatsHistoryRealm(myUid))
            it.onComplete()
        }.subscribeOn(AndroidSchedulers.mainThread()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({ realm ->
                realm.executeTransactionAsync({ r ->
                    cmd.invoke(r)
                }, {
                    realm.close()
                    complete?.invoke()
                }, {
                    realm.close()
                    it.printStackTrace()
                    error?.invoke(it)
                })
            }, {
                AppLogcat.logger.e(it)
                MobclickAgent.reportError(BaseApp.app, it)
            })
    }
}