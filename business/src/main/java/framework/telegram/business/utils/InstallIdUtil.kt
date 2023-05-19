package framework.telegram.business.utils

import android.provider.Settings
import android.text.TextUtils
import de.greenrobot.common.io.FileUtils
import framework.telegram.business.sp.InstallationIdPref
import framework.telegram.support.BaseApp
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.system.storage.sp.core.IdeasPreference
import framework.telegram.support.tools.AndroidUtils
import framework.telegram.support.tools.DeviceInfo
import framework.telegram.support.tools.EnvironmentUtils
import org.json.JSONObject
import java.io.File
import java.util.*

/**
 * Created by lzh on 19-5-31.
 * INFO: 设备唯一iD 的生成与存储
 */
object InstallIdUtil{
    private val mInstallationId by lazy { IdeasPreference().create(BaseApp.app, InstallationIdPref::class.java) }

    /**
     * 获取安装ID
     *
     * @return
     */
    fun getInstallationId(): String? {
        if (TextUtils.isEmpty(mInstallationId.getInstallId())) {
            loadInstallationId()
        }
        AppLogcat.logger.d("INSTALLATION_ID", "获取到的 InstallId---> ${mInstallationId.getInstallId()}")
        return mInstallationId.getInstallId()
    }

    /**
     * 加载安装ID
     */
    private fun loadInstallationId() {
        //从应用内存储中读取
        if (TextUtils.isEmpty(mInstallationId.getInstallId())) {
            try {
                // 应用内存储中都没有,则从文件中读取
                val sdcardRootDir = EnvironmentUtils.getExternalStorageDirectoryIfExist()
                if (sdcardRootDir != null) {
                    val infoFile = File(sdcardRootDir, ".youchong.info")
                    if (infoFile.exists() && infoFile.canRead()) {
                        val infoStr = FileUtils.readUtf8(infoFile)
                        val jb = JSONObject(infoStr)
                        mInstallationId.putInstallId(jb.optString("INSTALLATION_ID"))
                    }
                }
            } catch (e: Exception) {
                AppLogcat.logger.d("INSTALLATION_ID", "从文件中获取INSTALLATION_ID时发生错误--->" + e.message)
            }

            if (TextUtils.isEmpty(mInstallationId.getInstallId())) {
                // 应用内存储和文件中都没有,则去Setting中获取
                try {
                    mInstallationId.putInstallId(Settings.System.getString(BaseApp.app.contentResolver, "YOUTPET_INSTALLATION_ID"))
                } catch (e: Exception) {
                    AppLogcat.logger.d("INSTALLATION_ID", "从Setting中获取INSTALLATION_ID时发生错误--->" + e.message)
                }

                if (TextUtils.isEmpty(mInstallationId.getInstallId())) {
                    // 应用内存储和文件\Setting中都没有,则重新生成
                    val deviceInfo = DeviceInfo(BaseApp.app.applicationContext)
                    var getCount = 0

                    var mac = deviceInfo.wifiMAC
                    var getMAC = true
                    if (AndroidUtils.hasM() || TextUtils.isEmpty(mac) || DeviceInfo.DEFAULT_VALUE == mac) {
                        mac = UUID.randomUUID().toString()
                        getMAC = false
                    } else {
                        getCount++
                    }

                    var blueMac = deviceInfo.bluetoothMAC
                    var getBLUEMAC = true
                    if (AndroidUtils.hasM() || TextUtils.isEmpty(blueMac) || DeviceInfo.DEFAULT_VALUE == blueMac) {
                        blueMac = UUID.randomUUID().toString()
                        getBLUEMAC = false
                    } else {
                        getCount++
                    }

                    var gsfId = deviceInfo.gsfid
                    var getGSFID = true
                    if (TextUtils.isEmpty(gsfId) || DeviceInfo.DEFAULT_VALUE == gsfId) {
                        gsfId = UUID.randomUUID().toString()
                        getGSFID = false
                    }

                    var androidId = deviceInfo.androidID
                    var getAndroidId = true
                    if (TextUtils.isEmpty(androidId) || DeviceInfo.DEFAULT_VALUE == androidId) {
                        androidId = UUID.randomUUID().toString()
                        getAndroidId = false
                    }

                    AppLogcat.logger.d("INSTALLATION_ID", "获取到了mac--->" + mac + " isRandom--->" + !getMAC)
                    AppLogcat.logger.d("INSTALLATION_ID", "获取到了blueMac--->" + blueMac + " isRandom--->" + !getBLUEMAC)
                    AppLogcat.logger.d("INSTALLATION_ID", "获取到了gsfId--->" + gsfId + " isRandom--->" + !getGSFID)
                    AppLogcat.logger.d("INSTALLATION_ID", "获取到了androidId--->" + androidId + " isRandom--->" + !getAndroidId)

                    when {
                        getGSFID -> {
                            mInstallationId.putInstallId(UUID.nameUUIDFromBytes(gsfId.toByteArray()).toString())
                            AppLogcat.logger.d("INSTALLATION_ID", "根据gsfId生成InstallationId--->${mInstallationId.getInstallId()}")
                        }
                        getCount >= 2 -> {
                            val stringBuilder = StringBuilder()
                            if (getMAC) {
                                stringBuilder.append(mac)
                            }
                            if (getBLUEMAC) {
                                stringBuilder.append(blueMac)
                            }
                            mInstallationId.putInstallId(UUID.nameUUIDFromBytes(stringBuilder.toString().toByteArray()).toString())
                            AppLogcat.logger.d("INSTALLATION_ID", "根据获取到的各项值生成InstallationId--->${mInstallationId.getInstallId()}")
                        }
                        getAndroidId -> {
                            mInstallationId.putInstallId(UUID.nameUUIDFromBytes(androidId.toByteArray()).toString())
                            AppLogcat.logger.d("INSTALLATION_ID", "根据androidId生成InstallationId--->${mInstallationId.getInstallId()}")
                        }
                        else -> {
                            mInstallationId.putInstallId(UUID.nameUUIDFromBytes((UUID.randomUUID().toString() + System.currentTimeMillis().toString()).toByteArray()).toString())
                            AppLogcat.logger.d("INSTALLATION_ID", "随机生成InstallationId--->${mInstallationId.getInstallId()}")
                        }
                    }
                } else {
                    AppLogcat.logger.d("INSTALLATION_ID", "从Setting中获取到了INSTALLATION_ID--->${mInstallationId.getInstallId()}")
                }
            } else {
                AppLogcat.logger.d("INSTALLATION_ID", "从文件中获取到了INSTALLATION_ID--->${mInstallationId.getInstallId()}")
            }
        } else {
            AppLogcat.logger.d("INSTALLATION_ID", "从应用的内部存储中获取到了INSTALLATION_ID--->${mInstallationId.getInstallId()}")
        }

        try {
            //保存到文件中
            val sdcardRootDir = EnvironmentUtils.getExternalStorageDirectoryIfExist()
            if (sdcardRootDir != null) {
                val infoFile = File(sdcardRootDir, ".youchong.info")//UID的上级目录，也即youchong目录
                val jb = JSONObject()
                jb.put("INSTALLATION_ID", mInstallationId.getInstallId())
                FileUtils.writeUtf8(infoFile, jb.toString())
                AppLogcat.logger.d("INSTALLATION_ID", "保存INSTALLATION_ID到文件成功--->")
            } else {
                AppLogcat.logger.d("INSTALLATION_ID", "保存INSTALLATION_ID到文件失败--->SD卡不存在")
            }
        } catch (e: Exception) {
            AppLogcat.logger.d("INSTALLATION_ID", "保存INSTALLATION_ID到文件失败--->" + e.message)
        }

        //保存到Setting中
        try {
            Settings.System.putString(BaseApp.app.contentResolver, "YOUTPET_INSTALLATION_ID", mInstallationId.getInstallId())
            AppLogcat.logger.d("INSTALLATION_ID", "保存INSTALLATION_ID到Setting成功--->")
        } catch (e: Exception) {
            AppLogcat.logger.d("INSTALLATION_ID", "保存INSTALLATION_ID到Setting失败--->" + e.message)
        }

    }
}