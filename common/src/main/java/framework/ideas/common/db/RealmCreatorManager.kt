package framework.ideas.common.db

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import framework.telegram.support.BaseApp
import framework.telegram.support.tools.AppInfo
import io.realm.RealmConfiguration
import java.io.File

/**
 * Created by lzh on 19-6-29.
 * INFO:
 */
object RealmCreatorManager {

    /**
     * debug   版本,isSaveSdCard 等于 true 数据库将保存到sd卡
     * release 版本,isSaveSdCard 无效
     */
    private const val isSaveSdCard = false

    private const val sdCardDir = "/realmDb"

    private const val mTestSecret: String =
        "1234567812345678123456781234567812345678123456781234567812345678"

    private const val versions: Long = 18

    /**
     *  公共库
     */
    fun getCommonConfig(myUid: String, version: Int): RealmConfiguration {
        val name = if (version == 0) {
            "${myUid}_common_db"
        } else {
            "${version}_${myUid}_common_db"
        }
        val realmConfigurationBuilder =
            RealmConfiguration.Builder().name(name)
                .schemaVersion(versions)
                .migration(DBMigration())
                .deleteRealmIfMigrationNeeded()
                .encryptionKey(mTestSecret.toByteArray())
        setConfigureSdCard(realmConfigurationBuilder)
        return realmConfigurationBuilder.build()
    }

    /**
     *  群成员和群消息、群回执放在同一个库中
     */
    fun getGroupConfig(myUid: Long, groupId: Long, version: Int): RealmConfiguration {
        val name = if (version == 0) {
            "${myUid}_group_msg_to_${groupId}_db"
        } else {
            "${version}_${myUid}_group_msg_to_${groupId}_db"
        }
        val realmConfigurationBuilder =
            RealmConfiguration.Builder().name(name)
                .schemaVersion(versions)
                .migration(DBMigration())
                .deleteRealmIfMigrationNeeded()
                .encryptionKey(mTestSecret.toByteArray())
        setConfigureSdCard(realmConfigurationBuilder)
        return realmConfigurationBuilder.build()
    }

    /**
     *  放在私聊库中（私聊库包含私聊消息表和私聊对象信息表）
     */
    fun getPvtConfig(myUid: Long, targetUid: Long, version: Int): RealmConfiguration {
        val name = if (version == 0) {
            "${myUid}_pvt_msg_to_${targetUid}_db"
        } else {
            "${version}_${myUid}_pvt_msg_to_${targetUid}_db"
        }
        val realmConfigurationBuilder =
            RealmConfiguration.Builder().name(name)
                .schemaVersion(versions)
                .migration(DBMigration())
                .deleteRealmIfMigrationNeeded()
                .encryptionKey(mTestSecret.toByteArray())
        setConfigureSdCard(realmConfigurationBuilder)
        return realmConfigurationBuilder.build()
    }

    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return !(BaseApp.app.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || BaseApp.app.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        }
        return true
    }

    private fun setConfigureSdCard(realmConfiguration: RealmConfiguration.Builder) {
        if (!AppInfo.isDebug(BaseApp.app) || !isSaveSdCard)
            return

        val sdPath = getSDCardPath()
        if (checkPermission() && sdPath.isNotEmpty()) {
            val dir = File(sdPath + sdCardDir)
            realmConfiguration.directory(dir)
        }
    }

    private fun getSDCardPath(): String {
        var sdDir: File? = null
        val sdCardExist =
            Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED//判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory()//获取跟目录
        }
        return sdDir.toString()
    }
}