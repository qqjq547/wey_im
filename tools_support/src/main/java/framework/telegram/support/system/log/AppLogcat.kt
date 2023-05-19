package framework.telegram.support.system.log

import framework.telegram.support.BaseApp
import framework.telegram.support.BuildConfig
import framework.telegram.support.system.log.core.Logger
import framework.telegram.support.system.log.core.constant.LogLevel
import framework.telegram.support.system.log.core.constant.LogSegment
import framework.telegram.support.system.log.core.storage.DiskConfigs
import framework.telegram.support.system.log.core.utils.TimeUtils
import java.util.*

object AppLogcat {

    fun newLogger(packagedLevel: Int): Logger {
        val logLevels = ArrayList<String>()
        logLevels.add(LogLevel.ERROR)
        logLevels.add(LogLevel.WTF)

        val diskConfigs = DiskConfigs.Builder.newBuilder()
                .setCapacity((20 * 1024 * 1024).toLong())
                .build()

        return Logger.Builder.newBuilder(BaseApp.app, "default")
                .setDebug(BuildConfig.DEBUG)
                .setWriteToFile(false)
                .setLogLevelsForFile(logLevels)
                .setLogDir(BaseApp.LOG_DIR_NAME)
                .setLogSegment(LogSegment.TWENTY_FOUR_HOURS)
                .setZoneOffset(TimeUtils.ZoneOffset.P0800)
                .setTimeFormat("yyyy-MM-dd HH:mm:ss")
                .setPackagedLevel(0.coerceAtLeast(packagedLevel))
                .setStorage(framework.telegram.support.system.log.core.storage.DiskStorage(diskConfigs))
                .build()
    }

    val logger by lazy {
        val logLevels = ArrayList<String>()
        logLevels.add(LogLevel.ERROR)

        val diskConfigs = DiskConfigs.Builder.newBuilder()
                .setCapacity((20 * 1024 * 1024).toLong())
                .build()

        Logger.Builder.newBuilder(BaseApp.app, "default")
                .setDebug(BuildConfig.DEBUG)
                .setWriteToFile(false)
                .setLogLevelsForFile(logLevels)
                .setLogDir(BaseApp.LOG_DIR_NAME)
                .setLogSegment(LogSegment.TWENTY_FOUR_HOURS)
                .setZoneOffset(TimeUtils.ZoneOffset.P0800)
                .setTimeFormat("yyyy-MM-dd HH:mm:ss")
                .setPackagedLevel(0)
                .setStorage(framework.telegram.support.system.log.core.storage.DiskStorage(diskConfigs))
                .build()
    }
}
