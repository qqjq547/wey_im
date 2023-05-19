package framework.telegram.business.db

import android.annotation.SuppressLint
import android.text.TextUtils
import com.umeng.analytics.MobclickAgent
import framework.ideas.common.db.RealmCreatorManager
import framework.telegram.business.sp.CommonPref
import framework.telegram.support.BaseApp
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm

object RealmCreator {

    fun getChatsHistoryRealm(myUid: Long): Realm {
        return getCommonRealm(myUid.toString())
    }

    fun getLogRealm(): Realm {
        return getCommonRealm("0")
    }

    fun getContactsRealm(myUid: Long): Realm {
        return getCommonRealm(myUid.toString())
    }

    fun getSecretKeyRealm(myUid: Long): Realm {
        return getCommonRealm(myUid.toString())
    }

    fun getContactReqsRealm(myUid: Long): Realm {
        return getCommonRealm(myUid.toString())
    }

    fun getContactExRealm(myUid: Long): Realm {
        return getCommonRealm(myUid.toString())
    }

    fun getGroupsRealm(myUid: Long): Realm {
        return getCommonRealm(myUid.toString())
    }

    @Synchronized
    fun getGroupMembersRealm(myUid: Long, groupId: Long): Realm {
        val sp = SharePreferencesStorage.createStorageInstance(
            CommonPref::class.java,
            "${myUid}_group_msg_to_${groupId}_db"
        )
        val realmDataVersion = sp.getRealmDataVersion()
        val errorRealm = sp.getErrorRealm()
        val config =
            if (!TextUtils.isEmpty(errorRealm)) {
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

    @SuppressLint("CheckResult")
    fun executeContactsTransactionAsync(
        myUid: Long,
        cmd: (Realm) -> Unit,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        Observable.create<Realm> {
            it.onNext(getContactsRealm(myUid))
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
                MobclickAgent.reportError(BaseApp.app, it)
            })
    }

    @SuppressLint("CheckResult")
    fun executeContactsExTransactionAsync(
        myUid: Long,
        cmd: (Realm) -> Unit,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        Observable.create<Realm> {
            it.onNext(getContactExRealm(myUid))
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
                MobclickAgent.reportError(BaseApp.app, it)
            })
    }

    @SuppressLint("CheckResult")
    fun executeSecretKeyTransactionAsync(
        myUid: Long,
        cmd: (Realm) -> Unit,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        Observable.create<Realm> {
            it.onNext(getSecretKeyRealm(myUid))
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
                MobclickAgent.reportError(BaseApp.app, it)
            })
    }

    @SuppressLint("CheckResult")
    fun executeContactReqsTransactionAsync(
        myUid: Long,
        cmd: (Realm) -> Unit,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        Observable.create<Realm> {
            it.onNext(getContactReqsRealm(myUid))
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
                MobclickAgent.reportError(BaseApp.app, it)
            })
    }

    @SuppressLint("CheckResult")
    fun executeGroupsTransactionAsync(
        myUid: Long,
        cmd: (Realm) -> Unit,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        Observable.create<Realm> {
            it.onNext(getGroupsRealm(myUid))
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
                MobclickAgent.reportError(BaseApp.app, it)
            })
    }

    @SuppressLint("CheckResult")
    fun executeGroupMembersTransactionAsync(
        myUid: Long,
        groupId: Long,
        cmd: (Realm) -> Unit,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        Observable.create<Realm> {
            it.onNext(getGroupMembersRealm(myUid, groupId))
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
                MobclickAgent.reportError(BaseApp.app, it)
            })
    }


    @SuppressLint("CheckResult")
    fun executeChatHistroyTransactionAsync(
        myUid: Long,
        cmd: (Realm) -> Unit,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        Observable.create<Realm> {
            it.onNext(getGroupsRealm(myUid))
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
                MobclickAgent.reportError(BaseApp.app, it)
            })
    }

    @SuppressLint("CheckResult")
    fun executeRLogTransactionAsync(
        cmd: (Realm) -> Unit,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        Observable.create<Realm> {
            it.onNext(getLogRealm())
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
                MobclickAgent.reportError(BaseApp.app, it)
            })
    }
}