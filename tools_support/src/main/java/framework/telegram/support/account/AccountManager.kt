package framework.telegram.support.account

import android.text.TextUtils
import framework.telegram.support.BaseApp
import framework.telegram.support.account.Account.Companion.newAccountByUUID
import framework.telegram.support.system.storage.sp.core.IdeasPreference
import java.util.*
import kotlin.collections.HashMap

/**
 * 两种存储账号信息
 * 1.存储当前登录用户的uuid,有且只有一个
 * 2.存储所有用户的所有信息,T代表这个类的实现，具体实现在业务层实现.每个用户将会存不同的sp
 */
object AccountManager {

    private val mILoginAccountInfo: ILoginAccountInfo by lazy { IdeasPreference().create(BaseApp.app, ILoginAccountInfo::class.java) }

    private val mLastLoginAccountInfo: ILoginAccountInfo by lazy { IdeasPreference().create(BaseApp.app, ILoginAccountInfo::class.java, "LastLoginAccountInfo") }

    private val mCacheMap by lazy { HashMap<String, Any>() }

    /**
     * 保存最后一次登录的手机号
     */
    private fun saveLastLoginAccountUuid(uuid: String) {
        AccountManager.mLastLoginAccountInfo.putUUID(uuid)
    }

    /**
     * 保存最后一次登录的手机号(非游客)
     */
    fun getLastLoginAccountUuid() = AccountManager.mLastLoginAccountInfo.getUUID()

    fun hasLoginAccount(): Boolean {
        return !(TextUtils.isEmpty(getLoginAccountUUid()) || "0" == getLoginAccountUUid())
    }

    /**
     * 当前登录的用户的UUID
     */
    fun getLoginAccountUUid(): String = AccountManager.mILoginAccountInfo.getUUID()

    /**
     * 移除当前登录用户的/退出登录
     */
    fun removeLoginAccountUUid() {
        AccountManager.mILoginAccountInfo.removeUUID()
    }

    /**
     * 保存当前登录用户的uuid/更换用户
     */
    fun saveLoginAccountUUid(uuid: UUID) {
        val uuidStr = uuid.toString()
        AccountManager.mILoginAccountInfo.putUUID(uuidStr)
        saveLastLoginAccountUuid(uuidStr)
    }

    /**
     * 获取当前登录用户的详细信息类（如果uuid为空，就是没有登录）
     */
    fun <T> getLoginAccount(clazz: Class<T>): T {
        val key = "${AccountManager.mILoginAccountInfo.getUUID()}_${clazz.name}"
        if (mCacheMap.contains(key)) {
            return mCacheMap[key] as T
        }

        val instance = newAccountByUUID(AccountManager.mILoginAccountInfo.getUUID()).getAccountInfo(clazz)
        instance?.let {
            mCacheMap.put(key, instance)
        }
        return instance
    }

    /**
     * 获取登录用户的详细信息类,通过uuid
     */
    fun <T> getLoginAccountByUuid(uuid: String, clazz: Class<T>): T {
        val key = "${uuid}_${clazz.name}"
        if (mCacheMap.contains(key)) {
            return mCacheMap[key] as T
        }

        val instance = newAccountByUUID(uuid).getAccountInfo(clazz)
        instance?.let {
            mCacheMap.put(key, instance)
        }
        return instance
    }
}
