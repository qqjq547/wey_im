package framework.telegram.business

import android.os.Environment
import android.text.TextUtils
import com.google.gson.reflect.TypeToken
import com.im.domain.pb.CommonProto
import com.im.domain.pb.SysProto
import com.umeng.analytics.MobclickAgent
import de.greenrobot.common.io.FileUtils
import framework.ideas.common.model.common.SecretKeyModel
import framework.ideas.common.rlog.RLogManager
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.SysHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.SystemHttpProtocol
import framework.telegram.business.sp.CommonPref
import framework.telegram.message.bridge.Constant
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.gson.GsonInstanceCreater
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.*
import framework.telegram.ui.dialog.AppDialog
import io.realm.Sort
import org.whispersystems.curve25519.Curve25519
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Created by yanggl on 2019/7/31 16:21
 * 注意，需要WRITE_EXTERNAL_STORAGE和READ_EXTERNAL_STORAGE权限
 */
object UserDHKeysHelper {
    private const val TAG: String = "DHKeys"

    private const val mTestSecret: String = "1234567812345678"
    private val mUserDHKeysFileBasePath: String = Environment.getExternalStorageDirectory().path
    private const val mFileNameHead = ".key_"

    private val userAppKeyCacheMap by lazy { ConcurrentHashMap<String, ConcurrentHashMap<Int, SecretKeyModel>>() }
    private val userWebKeyCacheMap by lazy { ConcurrentHashMap<String, ConcurrentHashMap<Int, SecretKeyModel>>() }
    private val userAppKeyLastCache by lazy { ConcurrentHashMap<String, SecretKeyModel>() }
    private val userWebKeyLastCache by lazy { ConcurrentHashMap<String, SecretKeyModel>() }

    private val groupKeyLastCache by lazy { ConcurrentHashMap<String, SecretKeyModel>() }

    private val accountDHKeysCache by lazy { ConcurrentHashMap<Long, UserDHKeys>() }

    private val makeSecretKeysCache by lazy { ConcurrentHashMap<String, String>() }
    private val makeMyselfWebSecretKeysCache by lazy { ConcurrentHashMap<Long, String>() }

    fun clearCaches() {
        userAppKeyCacheMap.clear()
        userWebKeyCacheMap.clear()
        userAppKeyLastCache.clear()
        userWebKeyLastCache.clear()
        groupKeyLastCache.clear()
        accountDHKeysCache.clear()
        makeSecretKeysCache.clear()
        makeMyselfWebSecretKeysCache.clear()
    }

    /**
     * 生成文件与文本加密密钥
     */
    fun makeSecretKey(targetPublicKey: ByteArray, selfPrivateKey: ByteArray): ByteArray {
        val cipher = Curve25519.getInstance(Curve25519.BEST)
        return cipher.calculateAgreement(targetPublicKey, selfPrivateKey)
    }

    /**
     * 生成公私钥对
     */
    fun newKeyPair(): KeyPair {
        val curve25519KeyPair = Curve25519.getInstance(Curve25519.BEST).generateKeyPair()
        return KeyPair(curve25519KeyPair.publicKey, curve25519KeyPair.privateKey)
    }

    fun saveUserKeyPair(uid: String, keyPair: KeyPair, keyVersion: Int): Boolean {
        if (TextUtils.isEmpty(uid)) return false
        val publicKeySecretStr = HexString.bufferToHex(keyPair.publicKey)
        val privateKeySecretStr = HexString.bufferToHex(keyPair.privateKey)
        return saveUserDHKeys(uid, publicKeySecretStr, privateKeySecretStr, keyVersion)
    }

    private fun saveUserDHKeys(
        uid: String,
        publicKeySecretStr: String,
        privateKeySecretStr: String,
        keyVersion: Int
    ): Boolean {
        val userDHKeys = UserDHKeys()
        userDHKeys.uid = uid
        userDHKeys.publicKey = publicKeySecretStr
        userDHKeys.privateKey = privateKeySecretStr
        userDHKeys.keyVersion = keyVersion
        return multipleWrite(userDHKeys)
    }

    private fun multipleWrite(userDHKeys: UserDHKeys): Boolean {
        val result = kvWrite(userDHKeys)
        val result2 = fileWrite(userDHKeys)
        return result || result2
    }

    /*************sp储存**************/
    private fun kvWrite(userDHKeys: UserDHKeys): Boolean {
        val list = kvReadAll()
        try {
            if (kvRead(userDHKeys.uid) != null) {
                //更新
                list.forEach {
                    if (it.uid == userDHKeys.uid) {
                        return if (userDHKeys.keyVersion > it.keyVersion) {
                            it.keyVersion = userDHKeys.keyVersion
                            it.privateKey = userDHKeys.privateKey
                            it.publicKey = userDHKeys.publicKey
                            it.uid = userDHKeys.uid
                            val typeToken = object : TypeToken<MutableList<UserDHKeys>>() {}
                            //加密写入
                            val sDate = AESHelper.encrypt(
                                GsonInstanceCreater.defaultGson.toJson(
                                    list,
                                    typeToken.type
                                ).toByteArray(), mTestSecret
                            )
                            SharePreferencesStorage.createStorageInstance(
                                CommonPref::class.java,
                                AccountManager.getLoginAccountUUid()
                            ).putUserDhKeys(sDate)
                            RLogManager.d(TAG, "写入新的公私秘钥到sp缓存")
                            true
                        } else {
                            RLogManager.e(TAG, "写入新的公私秘钥到sp缓存失败")
                            false
                        }
                    }
                }

                RLogManager.e(TAG, "写入新的公私秘钥到sp缓存失败")
                return false
            } else {
                //添加
                list.add(userDHKeys)
                val typeToken = object : TypeToken<MutableList<UserDHKeys>>() {}
                //加密写入
                val sDate = AESHelper.encrypt(
                    GsonInstanceCreater.defaultGson.toJson(list, typeToken.type).toByteArray(),
                    mTestSecret
                )
                SharePreferencesStorage.createStorageInstance(
                    CommonPref::class.java,
                    AccountManager.getLoginAccountUUid()
                ).putUserDhKeys(sDate)

                RLogManager.d(TAG, "写入新的公私秘钥到sp缓存")
                return true
            }
        } catch (e: java.lang.Exception) {
            RLogManager.e(TAG, "写入新的公私秘钥到sp缓存失败", e)
            return false
        }
    }

    private fun kvRead(uid: String): UserDHKeys? {
        val list = kvReadAll()
        if (list.isNotEmpty()) {
            list.forEach {
                if (it.uid == uid) {
                    return it
                }
            }
        }
        return null
    }

    private fun kvReadAll(): MutableList<UserDHKeys> {
        //解密读取
        var keys = SharePreferencesStorage.createStorageInstance(
            CommonPref::class.java,
            AccountManager.getLoginAccountUUid()
        ).getUserDhKeys()
        if (TextUtils.isEmpty(keys)) {
            // 旧版本（v1.7以前）都是从与账号不关联的sp中读取，之后改成与账户有关，这里需要做兼容
            keys = SharePreferencesStorage.createStorageInstance(CommonPref::class.java)
                .getUserDhKeys()
            if (!TextUtils.isEmpty(keys)) {
                SharePreferencesStorage.createStorageInstance(
                    CommonPref::class.java,
                    AccountManager.getLoginAccountUUid()
                ).putUserDhKeys(keys)
            }
        }

        return if (TextUtils.isEmpty(keys)) {
            mutableListOf<UserDHKeys>()
        } else {
            try {
                val typeToken = object : TypeToken<MutableList<UserDHKeys>>() {}
                val keyStr = AESHelper.decrypt(HexString.hexToBuffer(keys), mTestSecret)
                GsonInstanceCreater.defaultGson.fromJson(keyStr, typeToken.type)
                    ?: mutableListOf<UserDHKeys>()
            } catch (e: Exception) {
                e.printStackTrace()
                mutableListOf<UserDHKeys>()
            }
        }
    }
    /*************sp储存  end**************/

    /*************文件储存**************/
    private fun fileWrite(userDHKeys: UserDHKeys): Boolean {
        try {
            val path = uid2Path(userDHKeys.uid)
            return if (existFile(path)) {
                //更新
                val ks = fileRead(userDHKeys.uid)
                if (ks != null) {
                    if (userDHKeys.keyVersion > ks.keyVersion) {
                        val writeStr = GsonInstanceCreater.defaultGson.toJson(userDHKeys)
                        encryptWriteFile(File(path), writeStr)
                    } else {
                        false
                    }
                } else {
                    //文件内容无法识别，直接覆盖
                    val writeStr = GsonInstanceCreater.defaultGson.toJson(userDHKeys)
                    encryptWriteFile(File(path), writeStr)
                }
            } else {
                //添加
                val f = File(path)
                if (!f.parentFile.exists()) {
                    f.parentFile.mkdirs()
                }
                f.createNewFile()
                encryptWriteFile(f, GsonInstanceCreater.defaultGson.toJson(userDHKeys))
            }
        } catch (e: java.lang.Exception) {
            RLogManager.e(TAG, "写入新的公私秘钥到file缓存失败", e)
            return false
        }
    }

    private fun encryptWriteFile(file: File, s: String): Boolean {
        if (!EnvironmentUtils.isExternalStorageMountedReadWrite()) return false
        return try {
            if (TextUtils.isEmpty(s)) {
                file.writeText(s)
            } else {
                file.writeText(AESHelper.encrypt(s.toByteArray(), mTestSecret))
            }

            RLogManager.d(TAG, "写入新的公私秘钥到文件缓存")

            true
        } catch (e: Exception) {
            e.printStackTrace()
            RLogManager.e(TAG, "写入新的公私秘钥到文件缓存失败", e)
            file.delete()
            false
        }
    }

    private fun existFile(file: File): Boolean {
        return file.exists() && file.isFile && file.length() > 0
    }

    private fun existFile(path: String): Boolean {
        val f = File(path)
        return f.exists() && f.length() > 0
    }

    private fun uid2Path(uid: String): String {
        return "$mUserDHKeysFileBasePath${File.separator}$mFileNameHead$uid.ks"
    }

    private fun fileRead(uid: String): UserDHKeys? {
        val f = File(uid2Path(uid))
        return try {
            if (existFile(f)) {
                GsonInstanceCreater.defaultGson.fromJson(decodeReadFile(f), UserDHKeys::class.java)
            } else {
                return null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeReadFile(f: File): String {
        if (!EnvironmentUtils.isExternalStorageMountedReadWrite()) {
            return ""
        }

        val s = f.readText()
        return if (TextUtils.isEmpty(s)) {
            ""
        } else {
            AESHelper.decrypt(HexString.hexToBuffer(f.readText()), mTestSecret)
        }
    }
    /*************文件储存 end**************/

    /*****************service*******************/
    private fun clearUserDHKeys(uid: String) {
        try {
            encryptWriteFile(File(uid2Path(uid)), "")
            RLogManager.d(TAG, "清除本地公私秘钥file缓存")

            //清除指定sp
            val list = kvReadAll()
            val iterator = list.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().uid == uid) {
                    iterator.remove()
                }
            }
            val typeToken = object : TypeToken<MutableList<UserDHKeys>>() {}
            //加密写入
            val sDate = AESHelper.encrypt(
                GsonInstanceCreater.defaultGson.toJson(list, typeToken.type).toByteArray(),
                mTestSecret
            )
            SharePreferencesStorage.createStorageInstance(
                CommonPref::class.java,
                AccountManager.getLoginAccountUUid()
            ).putUserDhKeys(sDate)
            RLogManager.d(TAG, "清除本地公私秘钥sp缓存")
        } catch (e: Exception) {
            e.printStackTrace()
            RLogManager.e(TAG, "清除本地公私秘钥缓存失败", e)
        }
    }

    fun getAccountSecretKey(uid: String): UserDHKeys? {
        try {
            if (accountDHKeysCache[uid.toLong()] != null) {
                RLogManager.d(TAG, "读取到key缓存")
                return accountDHKeysCache[uid.toLong()]
            }

            val userDHKeysKV: UserDHKeys? = kvRead(uid)//从sp读取
            if (userDHKeysKV != null) {
                RLogManager.d(TAG, "读取到key sp缓存")
                accountDHKeysCache[uid.toLong()] = userDHKeysKV
                return userDHKeysKV
            }

            val userDHKeysFile = fileRead(uid)//从文件读取
            if (userDHKeysFile != null) {
                RLogManager.d(TAG, "读取到key file缓存")
                accountDHKeysCache[uid.toLong()] = userDHKeysFile
                kvWrite(userDHKeysFile)// 写入到sp
                return userDHKeysFile
            }

            RLogManager.d(TAG, "没有读取到任何key缓存")
        } catch (e: Exception) {
            e.printStackTrace()
            MobclickAgent.reportError(
                BaseApp.app,
                "UserDHKeysHelper--->getAccountSecretKey失败 ${e.localizedMessage}"
            )

            RLogManager.e(TAG, "读取到key缓存时发生异常", e)
        }
        return null
    }

    fun getLoginAccountWebSecretKey(complete: (String, Int) -> Unit) {
        val loginAccount = AccountManager.getLoginAccount(AccountInfo::class.java)
        val webPublicKey = loginAccount.getWebPublicKey()
        val webPublicKeyVersion = loginAccount.getWebPublicKeyVersion()
        if (TextUtils.isEmpty(webPublicKey)) {
            RLogManager.w(TAG, "当前用户没有web key")
            complete.invoke("", 0)
        } else {
            val myUid = loginAccount.getUserId()
            if (makeMyselfWebSecretKeysCache[myUid] != null) {
                RLogManager.d(TAG, "获取到用户web key cache")
                complete.invoke(makeMyselfWebSecretKeysCache[myUid]!!, webPublicKeyVersion)
            } else {
                val myKeyPair = getAccountSecretKey(myUid.toString())
                if (myKeyPair != null) {
                    try {
                        val webSk = HexString.bufferToHex(
                            makeSecretKey(
                                HexString.hexToBuffer(webPublicKey),
                                HexString.hexToBuffer(myKeyPair.privateKey)
                            )
                        )
                        makeMyselfWebSecretKeysCache[myUid] = webSk
                        RLogManager.d(TAG, "获取到用户web key")
                        complete.invoke(webSk, webPublicKeyVersion)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        MobclickAgent.reportError(
                            BaseApp.app,
                            "UserDHKeysHelper--->getLoginAccountWebSecretKey失败 myPrivateKey->${myKeyPair.privateKey} webPublicKey->${webPublicKey} ${e.localizedMessage}"
                        )
                        RLogManager.e(TAG, "获取用户web key时发生异常", e)
                        complete.invoke("", 0)
                    }
                } else {
                    // 先上传自己的key
                    RLogManager.w(TAG, "获取当前用户的web端key，当前用户本地没有公私秘钥，需要重新上传")
                    uploadAccountPublicKey(myUid, { privateKey ->
                        try {
                            val webSk = HexString.bufferToHex(
                                makeSecretKey(
                                    HexString.hexToBuffer(webPublicKey),
                                    HexString.hexToBuffer(privateKey)
                                )
                            )
                            makeMyselfWebSecretKeysCache[myUid] = webSk
                            RLogManager.d(TAG, "获取到用户web key")
                            complete.invoke(webSk, webPublicKeyVersion)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            MobclickAgent.reportError(
                                BaseApp.app,
                                "UserDHKeysHelper--->getLoginAccountWebSecretKey失败 myPrivateKey->${privateKey} webPublicKey->${webPublicKey} ${e.localizedMessage}"
                            )
                            RLogManager.e(TAG, "获取用户web key时发生异常", e)
                            complete.invoke("", 0)
                        }
                    }, {
                        MobclickAgent.reportError(
                            BaseApp.app,
                            "UserDHKeysHelper--->getLoginAccountWebSecretKey失败 ${it.localizedMessage}"
                        )
                        RLogManager.e(TAG, "获取用户web key时发生异常 ", it)
                        complete.invoke("", 0)
                    })
                }
            }
        }
    }

    fun clearMyselfWebSecretKeysCache() {
        RLogManager.w(TAG, "清除当前用户web key cache")
        makeMyselfWebSecretKeysCache.clear()
    }

    fun getUserSecretKey(
        targetUid: Long,
        appVer: Int = -1,
        webVer: Int = -1,
        complete: (String, Int, String, Int) -> Unit,
        error: ((Throwable) -> Unit)? = null
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val myKeyPair = getAccountSecretKey(myUid.toString())
        if (myKeyPair == null) {
            // 先上传自己的key
            RLogManager.w(TAG, "获取用户${targetUid}的key，但当前用户本地没有公私秘钥，需要重新上传")
            uploadAccountPublicKey(myUid, { privateKey ->
                getUserSecretKey(myUid, privateKey, targetUid, appVer, webVer, complete, error)
            }, error)
        } else {
            getUserSecretKey(
                myUid,
                myKeyPair.privateKey,
                targetUid,
                appVer,
                webVer,
                complete,
                error
            )
        }
    }

    fun getGroupSecretKey(
        targetId: Long,
        complete: (String, Int) -> Unit,
        error: ((Throwable) -> Unit)? = null
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val myKeyPair = getAccountSecretKey(myUid.toString())
        if (myKeyPair == null) {
            // 先上传自己的key
            RLogManager.w(TAG, "获取群${targetId}的key，但当前用户本地没有公私秘钥，需要重新上传")
            uploadAccountPublicKey(myUid, { privateKey ->
                getGroupSecretKey(myUid, privateKey, targetId, complete, error)
            }, error)
        } else {
            getGroupSecretKey(myUid, myKeyPair.privateKey, targetId, complete, error)
        }
    }

    private fun getGroupSecretKey(
        myUid: Long,
        myPrivateKey: String,
        targetId: Long,
        complete: (String, Int) -> Unit,
        error: ((Throwable) -> Unit)? = null
    ) {
        var publicKey: String? = null
        var secretKey: String? = null
        var keyVersion: Int = 0

        // 从数据库中查询目标公钥
        RealmCreator.executeSecretKeyTransactionAsync(myUid, { realm ->
            val secretKeyModel = if (groupKeyLastCache["${myUid}_${targetId}"] != null) {
                RLogManager.d(TAG, "getGroupSecretKey(${targetId}) 内存缓存有最新key")
                groupKeyLastCache["${myUid}_${targetId}"]
            } else {
                RLogManager.d(TAG, "getGroupSecretKey(${targetId}) 数据库查询最新key")
                realm.where(SecretKeyModel::class.java)?.equalTo("targetType", 1.toInt())?.and()
                    ?.equalTo("targetId", targetId)?.findFirst()
            }

            if (secretKeyModel != null) {
                publicKey = secretKeyModel.publicKey
                keyVersion = secretKeyModel.keyVersion
                secretKey = secretKeyModel.secretKey
                RLogManager.d(TAG, "getGroupSecretKey(${targetId}) 获取到了本地存储最新key")
                groupKeyLastCache["${myUid}_${targetId}"] = secretKeyModel.copySecretKeyModel()
            }
        }, {
            if (!TextUtils.isEmpty(publicKey)) {
                try {
                    val sk = if (makeSecretKeysCache["${myPrivateKey}_${publicKey}"] != null) {
                        RLogManager.d(TAG, "getGroupSecretKey(${targetId}) 获取到了key的计算缓存")
                        makeSecretKeysCache["${myPrivateKey}_${publicKey}"]!!
                    } else {
                        val result = HexString.bufferToHex(
                            makeSecretKey(
                                HexString.hexToBuffer(publicKey),
                                HexString.hexToBuffer(myPrivateKey)
                            )
                        )
                        makeSecretKeysCache["${myPrivateKey}_${publicKey}"] = result
                        RLogManager.d(TAG, "getGroupSecretKey(${targetId}) 计算key成功")
                        result
                    }

                    if (!TextUtils.isEmpty(sk)) {
                        val groupSk = AESHelper.decrypt(
                            "AES/ECB/PKCS5Padding",
                            HexString.hexToBuffer(secretKey),
                            sk
                        )
                        complete.invoke(groupSk, keyVersion)
                    } else {
                        MobclickAgent.reportError(
                            BaseApp.app,
                            "UserDHKeysHelper--->getGroupSecretKey失败1"
                        )
                        RLogManager.e(TAG, "getGroupSecretKey(${targetId}) 获取key失败")
                        error?.invoke(java.lang.IllegalArgumentException())
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    MobclickAgent.reportError(
                        BaseApp.app,
                        "UserDHKeysHelper--->getGroupSecretKey失败2 myPrivateKey->${myPrivateKey} publicKey->${publicKey} secretKey->${secretKey} ${e.localizedMessage}"
                    )
                    RLogManager.e(TAG, "getGroupSecretKey(${targetId}) 获取key失败", e)
                    error?.invoke(e)
                }
            } else {
                // 数据库中未查询到，从服务器获取
                updateGroupPublicKey(myUid, targetId, { pk, version, msgKey ->
                    try {
                        val sk = if (makeSecretKeysCache["${myPrivateKey}_${pk}"] != null) {
                            RLogManager.d(TAG, "getGroupSecretKey(${targetId}) 获取到了key的计算缓存")
                            makeSecretKeysCache["${myPrivateKey}_${pk}"]!!
                        } else {
                            val result = HexString.bufferToHex(
                                makeSecretKey(
                                    HexString.hexToBuffer(pk),
                                    HexString.hexToBuffer(myPrivateKey)
                                )
                            )
                            makeSecretKeysCache["${myPrivateKey}_${pk}"] = result
                            RLogManager.d(TAG, "getGroupSecretKey(${targetId}) 计算key成功")
                            result
                        }

                        if (!TextUtils.isEmpty(sk)) {
                            val groupSk = AESHelper.decrypt(
                                "AES/ECB/PKCS5Padding",
                                HexString.hexToBuffer(msgKey),
                                sk
                            )
                            complete.invoke(groupSk, version)
                        } else {
                            MobclickAgent.reportError(
                                BaseApp.app,
                                "UserDHKeysHelper--->getGroupSecretKey失败1"
                            )
                            RLogManager.e(TAG, "getGroupSecretKey(${targetId}) 获取key失败")
                            error?.invoke(java.lang.IllegalArgumentException())
                        }
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        error?.invoke(e)
                        MobclickAgent.reportError(
                            BaseApp.app,
                            "UserDHKeysHelper--->getGroupSecretKey失败2 myPrivateKey->${myPrivateKey} publicKey->${publicKey} secretKey->${secretKey} ${e.localizedMessage}"
                        )
                        RLogManager.e(TAG, "getGroupSecretKey(${targetId}) 获取key失败", e)
                    }
                }, error)
            }
        }, {
            RLogManager.e(TAG, "getGroupSecretKey(${targetId}) 获取key失败 数据库错误", it)
            error?.invoke(it)
        })
    }

    private fun getUserSecretKey(
        myUid: Long,
        myPrivateKey: String,
        targetId: Long,
        reqAppVer: Int = -1,
        reqWebVer: Int = -1,
        complete: (String, Int, String, Int) -> Unit,
        error: ((Throwable) -> Unit)? = null
    ) {
        var cacheAppPublicKey = ""
        var cacheAppKeyVersion = -1
        var cacheWebPublicKey = ""
        var cacheWebKeyVersion = -1
        var isNeedUpdate = false

        if (targetId < Constant.Common.SYSTEM_USER_MAX_UID && targetId != Constant.Common.FILE_TRANSFER_UID) {
            RLogManager.d(TAG, "getUserSecretKey(${targetId}) 获取特殊用户的公钥")
            complete.invoke("", 0, "", 0)
        } else {
            // 从数据库中查询目标公钥
            RLogManager.d(
                TAG,
                "getUserSecretKey(${targetId}) reqAppVer->${reqAppVer}   reqWebVer->${reqWebVer}"
            )
            if (reqAppVer >= 0) {
                if (reqAppVer == 0) {
                    val secretKeyModel = if (userAppKeyLastCache["${myUid}_${targetId}"] != null) {
                        RLogManager.d(
                            TAG,
                            "getUserSecretKey(${targetId}) reqAppVer->${reqAppVer} 内存缓存有key"
                        )
                        userAppKeyLastCache["${myUid}_${targetId}"]
                    } else {
                        null
                    }

                    if (secretKeyModel != null) {
                        cacheAppPublicKey = secretKeyModel.publicKey
                        cacheAppKeyVersion = secretKeyModel.keyVersion
                    }
                } else {
                    val secretKeyModel =
                        if (userAppKeyCacheMap["${myUid}_${targetId}"]?.get(reqAppVer) != null) {
                            RLogManager.d(
                                TAG,
                                "getUserSecretKey(${targetId}) reqAppVer->${reqAppVer} 内存缓存有key"
                            )
                            userAppKeyCacheMap["${myUid}_${targetId}"]?.get(reqAppVer)
                        } else {
                            null
                        }

                    if (secretKeyModel != null) {
                        cacheAppPublicKey = secretKeyModel.publicKey
                        cacheAppKeyVersion = secretKeyModel.keyVersion
                    }
                }
            }

            if (reqWebVer >= 0) {
                if (reqWebVer == 0) {
                    val secretKeyModel = if (userWebKeyLastCache["${myUid}_${targetId}"] != null) {
                        RLogManager.d(
                            TAG,
                            "getUserSecretKey(${targetId}) reqWebVer->${reqWebVer} 内存缓存有key"
                        )
                        userWebKeyLastCache["${myUid}_${targetId}"]
                    } else {
                        null
                    }

                    if (secretKeyModel != null) {
                        cacheWebPublicKey = secretKeyModel.publicKey
                        cacheWebKeyVersion = secretKeyModel.keyVersion
                    }
                } else {
                    val secretKeyModel =
                        if (userWebKeyCacheMap["${myUid}_${targetId}"]?.get(reqWebVer) != null) {
                            RLogManager.d(
                                TAG,
                                "getUserSecretKey(${targetId}) reqWebVer->${reqWebVer} 内存缓存有key"
                            )
                            userWebKeyCacheMap["${myUid}_${targetId}"]?.get(reqWebVer)
                        } else {
                            null
                        }

                    if (secretKeyModel != null) {
                        cacheWebPublicKey = secretKeyModel.publicKey
                        cacheWebKeyVersion = secretKeyModel.keyVersion
                    }
                }
            }

            if ((reqAppVer >= 0 && cacheAppKeyVersion < 0) || (reqWebVer >= 0 && cacheWebKeyVersion < 0)) {
                // 缓存中未查询到，从数据库获取
                RealmCreator.executeSecretKeyTransactionAsync(myUid, { realm ->
                    if (reqAppVer >= 0) {
                        if (reqAppVer == 0) {
                            RLogManager.d(
                                TAG,
                                "getUserSecretKey(${targetId}) reqAppVer->${reqAppVer} 数据库查询key"
                            )
                            val secretKeyModel = realm.where(SecretKeyModel::class.java)
                                ?.equalTo("targetType", 0L)
                                ?.and()
                                ?.equalTo("targetId", targetId)
                                ?.and()
                                ?.notEqualTo("isWeb", true)
                                ?.sort("keyVersion", Sort.DESCENDING)?.findFirst()
                            if (secretKeyModel != null) {
                                cacheAppPublicKey = secretKeyModel.publicKey
                                cacheAppKeyVersion = secretKeyModel.keyVersion
                                RLogManager.d(
                                    TAG,
                                    "getUserSecretKey(${targetId}) reqAppVer->${reqAppVer} 获取到了本地存储key"
                                )
                                userAppKeyLastCache["${myUid}_${targetId}"] =
                                    secretKeyModel.copySecretKeyModel()
                                if (System.currentTimeMillis() - secretKeyModel.time > 60 * 60 * 1000) {
                                    isNeedUpdate = true
                                }
                            }
                        } else {
                            RLogManager.d(
                                TAG,
                                "getUserSecretKey(${targetId}) reqAppVer->${reqAppVer} 数据库查询key"
                            )
                            val secretKeyModel = realm.where(SecretKeyModel::class.java)
                                ?.equalTo("targetType", 0.toInt())
                                ?.and()
                                ?.equalTo("targetId", targetId)
                                ?.and()
                                ?.equalTo("keyVersion", reqAppVer)
                                ?.and()
                                ?.notEqualTo("isWeb", true)?.findFirst()
                            if (secretKeyModel != null) {
                                cacheAppPublicKey = secretKeyModel.publicKey
                                cacheAppKeyVersion = secretKeyModel.keyVersion
                                RLogManager.d(
                                    TAG,
                                    "getUserSecretKey(${targetId}) reqAppVer->${reqAppVer} 获取到了本地存储key"
                                )
                                if (userAppKeyCacheMap["${myUid}_${targetId}"] == null) {
                                    userAppKeyCacheMap["${myUid}_${targetId}"] = ConcurrentHashMap()
                                }
                                userAppKeyCacheMap["${myUid}_${targetId}"]?.put(
                                    reqAppVer,
                                    secretKeyModel.copySecretKeyModel()
                                )
                            }
                        }
                    }

                    if (reqWebVer >= 0) {
                        if (reqWebVer == 0) {
                            RLogManager.d(
                                TAG,
                                "getUserSecretKey(${targetId}) reqWebVer->${reqWebVer} 数据库查询key"
                            )
                            val secretKeyModel = realm.where(SecretKeyModel::class.java)
                                ?.equalTo("targetType", 0.toInt())
                                ?.and()
                                ?.equalTo("targetId", targetId)
                                ?.and()
                                ?.equalTo("isWeb", true)
                                ?.sort("keyVersion", Sort.DESCENDING)?.findFirst()

                            if (secretKeyModel != null) {
                                cacheWebPublicKey = secretKeyModel.publicKey
                                cacheWebKeyVersion = secretKeyModel.keyVersion
                                RLogManager.d(
                                    TAG,
                                    "getUserSecretKey(${targetId}) reqWebVer->${reqWebVer} 获取到了本地存储key"
                                )
                                userWebKeyLastCache["${myUid}_${targetId}"] =
                                    secretKeyModel.copySecretKeyModel()
                                if (System.currentTimeMillis() - secretKeyModel.time > 60 * 60 * 1000) {
                                    isNeedUpdate = true
                                }
                            }
                        } else {
                            RLogManager.d(
                                TAG,
                                "getUserSecretKey(${targetId}) reqWebVer->${reqWebVer} 数据库查询key"
                            )
                            val secretKeyModel = realm.where(SecretKeyModel::class.java)
                                ?.equalTo("targetType", 0.toInt())
                                ?.and()
                                ?.equalTo("targetId", targetId)
                                ?.and()
                                ?.equalTo("keyVersion", reqWebVer)
                                ?.and()
                                ?.equalTo("isWeb", true)?.findFirst()

                            if (secretKeyModel != null) {
                                cacheWebPublicKey = secretKeyModel.publicKey
                                cacheWebKeyVersion = secretKeyModel.keyVersion
                                RLogManager.d(
                                    TAG,
                                    "getUserSecretKey(${targetId}) reqWebVer->${reqWebVer} 获取到了本地存储key"
                                )
                                if (userWebKeyCacheMap["${myUid}_${targetId}"] == null) {
                                    userWebKeyCacheMap["${myUid}_${targetId}"] = ConcurrentHashMap()
                                }
                                userWebKeyCacheMap["${myUid}_${targetId}"]?.put(
                                    reqWebVer,
                                    secretKeyModel.copySecretKeyModel()
                                )
                            }
                        }
                    }
                }, {
                    getUserSecretKey(
                        myUid,
                        myPrivateKey,
                        targetId,
                        cacheAppPublicKey,
                        cacheAppKeyVersion,
                        cacheWebPublicKey,
                        cacheWebKeyVersion,
                        isNeedUpdate,
                        reqAppVer,
                        reqWebVer,
                        complete,
                        error
                    )
                }, {
                    MobclickAgent.reportError(
                        BaseApp.app,
                        "UserDHKeysHelper--->getUserSecretKey executeSecretKeyTransactionAsync 失败  targetId->>>${targetId}  reqAppVer->>>${reqAppVer}   reqWebVer->>>${reqWebVer}   error->>>${it.localizedMessage}"
                    )
                    RLogManager.e(
                        TAG,
                        "getUserSecretKey(${targetId}) reqAppVer->>>${reqAppVer}   reqWebVer->>>${reqWebVer}   获取key失败 数据库错误",
                        it
                    )
                    error?.invoke(it)
                })
            } else {
                getUserSecretKey(
                    myUid,
                    myPrivateKey,
                    targetId,
                    cacheAppPublicKey,
                    cacheAppKeyVersion,
                    cacheWebPublicKey,
                    cacheWebKeyVersion,
                    isNeedUpdate,
                    reqAppVer,
                    reqWebVer,
                    complete,
                    error
                )
            }
        }
    }

    private fun getUserSecretKey(
        myUid: Long,
        myPrivateKey: String,
        targetId: Long,
        cacheAppPublicKey: String,
        cacheAppKeyVersion: Int,
        cacheWebPublicKey: String,
        cacheWebKeyVersion: Int,
        isNeedUpdate: Boolean,
        reqAppVer: Int = -1,
        reqWebVer: Int = -1,
        complete: (String, Int, String, Int) -> Unit,
        error: ((Throwable) -> Unit)? = null
    ) {
        if ((reqAppVer >= 0 && cacheAppKeyVersion < 0) || (reqWebVer >= 0 && cacheWebKeyVersion < 0)) {
            // 数据库中未查询到，从服务器获取
            RLogManager.d(TAG, "getUserSecretKey(${targetId}) 数据库中未查询到，从服务器获取")
            updateUserPublicKey(
                myUid,
                targetId,
                0.coerceAtLeast(reqAppVer),
                0.coerceAtLeast(reqWebVer),
                { pk, aVersion, webPk, wVersion ->
                    getUserSecretKey(
                        targetId,
                        reqAppVer,
                        reqWebVer,
                        myPrivateKey,
                        pk,
                        aVersion,
                        webPk,
                        wVersion,
                        complete,
                        error
                    )
                },
                {
                    MobclickAgent.reportError(
                        BaseApp.app,
                        "UserDHKeysHelper--->getUserSecretKey->updateUserPublicKey失败  targetId->>>${targetId}  reqAppVer->>>${reqAppVer}   reqWebVer->>>${reqWebVer}  error->>>${it.localizedMessage}"
                    )
                    RLogManager.e(
                        TAG,
                        "getUserSecretKey(${targetId}) reqAppVer->>>${reqAppVer}   reqWebVer->>>${reqWebVer}   获取服务器的key失败",
                        it
                    )
                    error?.invoke(it)
                })
        } else {
            getUserSecretKey(
                targetId,
                reqAppVer,
                reqWebVer,
                myPrivateKey,
                cacheAppPublicKey,
                cacheAppKeyVersion,
                cacheWebPublicKey,
                cacheWebKeyVersion,
                complete,
                error
            )
        }

        if (isNeedUpdate) {
            RLogManager.w(TAG, "getUserSecretKey(${targetId}) key的有效期超过了1小时，更新一次")
            updateUserPublicKey(myUid, targetId, 0, 0)
        }
    }

    private fun getUserSecretKey(
        targetId: Long,
        reqAppVer: Int,
        reqWebVer: Int,
        myPrivateKey: String,
        appPublicKey: String,
        appKeyVersion: Int,
        webPublicKey: String,
        webKeyVersion: Int,
        complete: (String, Int, String, Int) -> Unit,
        error: ((Throwable) -> Unit)? = null
    ) {
        // 获取到了缓存的key
        var sk = ""
        var webSk = ""

        if (!TextUtils.isEmpty(appPublicKey)) {
            try {
                sk = if (makeSecretKeysCache["${myPrivateKey}_${appPublicKey}"] != null) {
                    RLogManager.d(
                        TAG,
                        "getUserSecretKey(${targetId}) reqAppVer->>>${reqAppVer}   获取到了key的计算缓存"
                    )
                    makeSecretKeysCache["${myPrivateKey}_${appPublicKey}"]!!
                } else {
                    val result = HexString.bufferToHex(
                        makeSecretKey(
                            HexString.hexToBuffer(appPublicKey),
                            HexString.hexToBuffer(myPrivateKey)
                        )
                    )
                    makeSecretKeysCache["${myPrivateKey}_${appPublicKey}"] = result
                    RLogManager.d(
                        TAG,
                        "getUserSecretKey(${targetId}) reqAppVer->>>${reqAppVer}   获取到了key的计算结果"
                    )
                    result
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                MobclickAgent.reportError(
                    BaseApp.app,
                    "UserDHKeysHelper--->getUserSecretKey失败1 make app SecretKey ${e.localizedMessage}"
                )
                RLogManager.e(
                    TAG,
                    "getUserSecretKey(${targetId}) reqAppVer->>>${reqAppVer}   计算key失败"
                )
            }
        }

        if (!TextUtils.isEmpty(webPublicKey)) {
            try {
                webSk = if (makeSecretKeysCache["${myPrivateKey}_${webPublicKey}"] != null) {
                    RLogManager.d(
                        TAG,
                        "getUserSecretKey(${targetId}) reqWebVer->>>${reqWebVer}   获取到了key的计算缓存"
                    )
                    makeSecretKeysCache["${myPrivateKey}_${webPublicKey}"]!!
                } else {
                    val result = HexString.bufferToHex(
                        makeSecretKey(
                            HexString.hexToBuffer(webPublicKey),
                            HexString.hexToBuffer(myPrivateKey)
                        )
                    )
                    makeSecretKeysCache["${myPrivateKey}_${webPublicKey}"] = result
                    RLogManager.d(
                        TAG,
                        "getUserSecretKey(${targetId}) reqWebVer->>>${reqWebVer}   获取到了key的计算结果"
                    )
                    result
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                MobclickAgent.reportError(
                    BaseApp.app,
                    "UserDHKeysHelper--->getUserSecretKey失败2 make web SecretKey ${e.localizedMessage}"
                )
                RLogManager.e(
                    TAG,
                    "getUserSecretKey(${targetId}) reqWebVer->>>${reqWebVer}   计算key失败",
                    e
                )
            }
        }

        if (TextUtils.isEmpty(sk) && TextUtils.isEmpty(webSk)) {
            MobclickAgent.reportError(
                BaseApp.app,
                "UserDHKeysHelper--->getUserSecretKey失败0   reqAppVer->>>${reqAppVer}   reqWebVer->>>${reqWebVer}"
            )
            RLogManager.e(
                TAG,
                "getUserSecretKey(${targetId}) reqAppVer->>>${reqAppVer}   reqWebVer->>>${reqWebVer}   计算两个key都失败"
            )
            error?.invoke(java.lang.IllegalArgumentException())
        } else {
            RLogManager.d(
                TAG,
                "getUserSecretKey(${targetId}) reqAppVer->>>${reqAppVer}   reqWebVer->>>${reqWebVer}   获取key成功"
            )
            complete.invoke(sk, appKeyVersion, webSk, webKeyVersion)
        }
    }

    fun updateGroupPublicKey(
        myUid: Long,
        targetId: Long,
        complete: (String, Int, String) -> Unit,
        error: ((Throwable) -> Unit)? = null
    ) {
        RLogManager.d(TAG, "updateGroupPublicKey(${targetId}) 开始更新公钥")
        HttpManager.getStore(SystemHttpProtocol::class.java)
            .getKeyPair(object : HttpReq<SysProto.GetKeyPairReq>() {
                override fun getData(): SysProto.GetKeyPairReq {
                    return SysHttpReqCreator.createGetKeyPairReq(
                        CommonProto.KeyPairType.KEY_GROUP,
                        targetId
                    )
                }
            })
            .getResult(null, { resp ->
                //请求成功
                RLogManager.d(TAG, "updateGroupPublicKey(${targetId}) 公钥获取成功")

                RealmCreator.executeSecretKeyTransactionAsync(myUid, { realm ->
                    val model =
                        realm.where(SecretKeyModel::class.java).equalTo("targetType", 1.toInt())
                            .and().equalTo("targetId", targetId).findFirst()
                    if (model != null) {
                        //客户端交换秘钥
                        model.publicKey = resp.keyPair.publicKey
                        model.keyVersion = resp.keyPair.keyVersion

                        //群秘钥
                        model.secretKey = resp.keyPair.msgKey

                        model.time = System.currentTimeMillis()

                        realm.copyToRealmOrUpdate(model)
                    } else {
                        realm.copyToRealm(
                            SecretKeyModel.createGroupSecretKeyModel(
                                targetId,
                                resp.keyPair.publicKey,
                                resp.keyPair.keyVersion,
                                resp.keyPair.msgKey,
                                System.currentTimeMillis()
                            )
                        )
                    }
                }, {
                    RLogManager.d(TAG, "updateGroupPublicKey(${targetId}) 清空本地最新的key缓存")
                    groupKeyLastCache.remove("${myUid}_${targetId}")

                    RLogManager.d(TAG, "updateGroupPublicKey(${targetId}) 公钥更新成功")
                    complete.invoke(
                        resp.keyPair.publicKey,
                        resp.keyPair.keyVersion,
                        resp.keyPair.msgKey
                    )
                }, {
                    RLogManager.e(TAG, "updateGroupPublicKey(${targetId}) 公钥获取失败", it)
                    MobclickAgent.reportError(
                        BaseApp.app,
                        "UserDHKeysHelper--->getUserSecretKey失败3   targetId->>>${targetId}  error->>>${it.localizedMessage}"
                    )
                    error?.invoke(it)
                })
            }, {
                //请求失败
                RLogManager.e(TAG, "updateGroupPublicKey(${targetId}) 公钥获取失败", it)
                MobclickAgent.reportError(
                    BaseApp.app,
                    "UserDHKeysHelper--->updateGroupPublicKey失败4   targetId->>>${targetId}  error->>>${it.localizedMessage}"
                )
                error?.invoke(it)
            })
    }

    fun updateUserPublicKey(
        myUid: Long,
        targetId: Long,
        appVer: Int = 0,
        webVer: Int = 0,
        complete: ((String, Int, String, Int) -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    ) {
        if (targetId < Constant.Common.SYSTEM_USER_MAX_UID && targetId != Constant.Common.FILE_TRANSFER_UID) {
            RLogManager.d(TAG, "updateUserPublicKey(${targetId}) 获取特殊用户的公钥")
            complete?.invoke("", 0, "", 0)
        } else {
            RLogManager.d(
                TAG,
                "updateUserPublicKey(${targetId}) 开始获取公钥 appVer->${appVer}   webVer->${webVer}"
            )
            HttpManager.getStore(SystemHttpProtocol::class.java)
                .getKeyPairOfVer(object : HttpReq<SysProto.GetKeyPairOfVerReq>() {
                    override fun getData(): SysProto.GetKeyPairOfVerReq {
                        return SysHttpReqCreator.createGetKeyPairOfVerReq(targetId, appVer, webVer)
                    }
                })
                .getResult(null, { resp ->
                    //请求成功
                    RLogManager.d(TAG, "updateUserPublicKey(${targetId}) 公钥获取成功")

                    RealmCreator.executeSecretKeyTransactionAsync(myUid, { realm ->
                        var model = realm.where(SecretKeyModel::class.java)
                            .equalTo("targetType", 0.toInt())
                            .and()
                            .equalTo("targetId", targetId)
                            .and()
                            .equalTo("keyVersion", resp.appKeyPair.keyVersion)
                            .and()
                            .notEqualTo("isWeb", true).findFirst()
                        if (model == null) {
                            //app端秘钥
                            realm.copyToRealm(
                                SecretKeyModel.createUserSecretKeyModel(
                                    targetId,
                                    resp.appKeyPair.publicKey,
                                    resp.appKeyPair.keyVersion,
                                    false,
                                    System.currentTimeMillis()
                                )
                            )
                        } else {
                            model.time = System.currentTimeMillis()
                            realm.copyToRealmOrUpdate(model)
                        }

                        RLogManager.d(TAG, "updateUserPublicKey(${targetId}) app公钥更新到数据库中")

                        model = realm.where(SecretKeyModel::class.java)
                            .equalTo("targetType", 0.toInt())
                            .and()
                            .equalTo("targetId", targetId)
                            .and()
                            .equalTo("keyVersion", resp.webKeyPair.keyVersion)
                            .and()
                            .equalTo("isWeb", true).findFirst()
                        if (model == null) {
                            //web端秘钥
                            realm.copyToRealm(
                                SecretKeyModel.createUserSecretKeyModel(
                                    targetId,
                                    resp.webKeyPair.publicKey,
                                    resp.webKeyPair.keyVersion,
                                    true,
                                    System.currentTimeMillis()
                                )
                            )
                        } else {
                            model.time = System.currentTimeMillis()
                            realm.copyToRealmOrUpdate(model)
                        }

                        RLogManager.d(TAG, "updateUserPublicKey(${targetId}) web公钥更新到数据库中")
                    }, {
                        RLogManager.d(TAG, "updateUserPublicKey(${targetId}) 清空本地最新的key缓存")

                        if (appVer == 0) {
                            userAppKeyLastCache.remove("${myUid}_${targetId}")
                        }

                        if (webVer == 0) {
                            userWebKeyLastCache.remove("${myUid}_${targetId}")
                        }

                        RLogManager.d(TAG, "updateUserPublicKey(${targetId}) 公钥更新成功")
                        complete?.invoke(
                            resp.appKeyPair.publicKey,
                            resp.appKeyPair.keyVersion,
                            resp.webKeyPair.publicKey,
                            resp.webKeyPair.keyVersion
                        )
                    }, {
                        RLogManager.e(TAG, "updateUserPublicKey(${targetId}) 公钥更新失败", it)
                        MobclickAgent.reportError(
                            BaseApp.app,
                            "UserDHKeysHelper--->getUserSecretKey失败5   targetId->>>${targetId}  appVer->>>${appVer}   webVer->>>${webVer}  error->>>${it.localizedMessage}"
                        )
                        error?.invoke(it)
                    })
                }, {
                    //请求失败
                    RLogManager.e(TAG, "updateUserPublicKey(${targetId}) 公钥更新失败", it)
                    MobclickAgent.reportError(
                        BaseApp.app,
                        "UserDHKeysHelper--->getUserSecretKey失败6   targetId->>>${targetId}  appVer->>>${appVer}   webVer->>>${webVer}  error->>>${it.localizedMessage}"
                    )
                    error?.invoke(it)
                })
        }
    }

    /**
     * 判断自己的publicKey是否与服务器的一致，不一致则重新生成并提交到服务器
     */
    fun updateAccountPublicKey(
        publicKey: String,
        complete: () -> Unit,
        error: ((Throwable) -> Unit)? = null
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        // 本地与下发的进行对比，如果不一致则更新
        val getKeyPair = getAccountSecretKey(myUid.toString())
        if (getKeyPair == null || getKeyPair.publicKey != publicKey) {
            RLogManager.w(TAG, "updateAccountPublicKey 本地key为空 或者与服务器保存的不同，需要重新上传")
            uploadAccountPublicKey(myUid, {
                complete.invoke()
            }, error)
        } else {
            complete.invoke()
        }
    }

    /**
     * 上传新的publicKey
     */
    private var isUploadAccountPublicKeying = AtomicBoolean(false)

    private fun uploadAccountPublicKey(
        myUid: Long,
        complete: (String) -> Unit,
        error: ((Throwable) -> Unit)? = null
    ) {
        try {
            if (isUploadAccountPublicKeying.get()) {
                RLogManager.w(TAG, "uploadAccountPublicKey 新的公钥上传中...抛错")
                error?.invoke(IllegalArgumentException("isUploadAccountPublicKeying is true"))
            } else {
                isUploadAccountPublicKeying.set(true)

                val newKeyPair = newKeyPair()
                RLogManager.d(
                    TAG,
                    "uploadAccountPublicKey 开始上传新的本地用户的公私秘钥 privateKey->${
                        HexString.bufferToHex(newKeyPair.privateKey)
                    } publicKey->${HexString.bufferToHex(newKeyPair.publicKey)}"
                )
                HttpManager.getStore(SystemHttpProtocol::class.java)
                    .updateUserKeyPair(object : HttpReq<SysProto.UpdateKeyPairReq>() {
                        override fun getData(): SysProto.UpdateKeyPairReq {
                            return SysHttpReqCreator.createUpdateKeyPairReq(
                                HexString.bufferToHex(
                                    newKeyPair.publicKey
                                )
                            )
                        }
                    })
                    .getResult(null, { resp ->
                        isUploadAccountPublicKeying.set(false)

                        RLogManager.d(TAG, "uploadAccountPublicKey 本地用户的新公私秘钥上传成功")

                        // 清空本地保存的其他用户的key，让用户使用对方用户最新的key进行加密(防止更新秘钥后发送消息故障)
                        RealmCreator.executeSecretKeyTransactionAsync(myUid, { realm ->
                            realm.delete(SecretKeyModel::class.java)
                        }, {
                            clearCaches()
                            RLogManager.d(TAG, "uploadAccountPublicKey 已清除本地保存的其他用户的key")
                        })

                        // 保存到本地
                        try {
                            val publicKeyStr = HexString.bufferToHex(newKeyPair.publicKey)
                            val privateKeyStr = HexString.bufferToHex(newKeyPair.privateKey)
                            if (saveUserDHKeys(
                                    myUid.toString(),
                                    publicKeyStr,
                                    privateKeyStr,
                                    resp.keyVersion
                                )
                            ) {
                                RLogManager.d(TAG, "uploadAccountPublicKey 新公钥保存成功")
                                complete.invoke(privateKeyStr)
                            } else {
                                clearUserDHKeys(myUid.toString())
                                RLogManager.e(TAG, "uploadAccountPublicKey 新公钥保存失败")
                                MobclickAgent.reportError(
                                    BaseApp.app,
                                    "UserDHKeysHelper--->uploadAccountPublicKey->saveUserDHKeys失败"
                                )
                                error?.invoke(IllegalStateException())
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            RLogManager.e(TAG, "uploadAccountPublicKey 新公钥保存失败", e)
                            MobclickAgent.reportError(
                                BaseApp.app,
                                "UserDHKeysHelper--->uploadAccountPublicKey->saveUserDHKeys失败   error->>>${e.localizedMessage}"
                            )
                            error?.invoke(IllegalStateException())
                        }
                    }, {
                        // 请求失败
                        isUploadAccountPublicKeying.set(false)

                        clearUserDHKeys(myUid.toString())

                        it.printStackTrace()

                        RLogManager.e(TAG, "uploadAccountPublicKey 本地用户的新公私秘钥上传失败", it)
                        MobclickAgent.reportError(
                            BaseApp.app,
                            "UserDHKeysHelper--->uploadAccountPublicKey->updateUserKeyPair失败   error->>>${it.localizedMessage}"
                        )
                        error?.invoke(it)
                    })
            }
        } catch (e: Exception) {
            isUploadAccountPublicKeying.set(false)

            clearUserDHKeys(myUid.toString())

            e.printStackTrace()

            RLogManager.e(TAG, "uploadAccountPublicKey 本地用户的新公私秘钥上传失败", e)
            MobclickAgent.reportError(
                BaseApp.app,
                "UserDHKeysHelper--->uploadAccountPublicKey->newKeyPair失败   error->>>${e.localizedMessage}"
            )
            error?.invoke(e)
        }
    }

    /*****************service end*******************/
    class UserDHKeys {
        var uid: String = ""
        var publicKey: String = ""
        var privateKey: String = ""
        var keyVersion: Int = -1
    }

    class KeyPair(val publicKey: ByteArray, val privateKey: ByteArray)
}