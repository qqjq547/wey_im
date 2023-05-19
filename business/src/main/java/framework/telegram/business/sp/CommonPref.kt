package framework.telegram.business.sp

import framework.telegram.business.bridge.Constant
import framework.ideas.common.http.BackupLoginHostInterceptor
import framework.telegram.support.system.storage.sp.core.Default
import framework.telegram.support.system.storage.sp.core.Key
import framework.telegram.support.system.storage.sp.core.SpName

/**
 * Created by lzh on 19-5-30.
 * INFO:
 */
@SpName("bus_common_preferences")
interface CommonPref {
    /**
     * ----------------------------------------以下是与账号无关联的配置项----------------------------------------
     */
    @Key("check_versions_time")
    fun putCheckVersionsTime(value: Int)

    @Key("check_versions_time")
    fun getCheckVersionsTime(@Default defaultValue: Int = -1): Int

    @Key("group_2")
    fun putGroupUrl(value: String)

    @Key("group_2")
    fun getGroupUrl(@Default defaultValue: String = Constant.Common.BIZ_HTTP_GROUP): String

    @Key("friend_2")
    fun putFriendUrl(value: String)

    @Key("friend_2")
    fun getFriendUrl(@Default defaultValue: String = Constant.Common.BIZ_HTTP_CONTACT): String

    @Key("login_2")
    fun putLoginUrl(value: String)

    @Key("login_2")
    fun getLoginUrl(@Default defaultValue: String = Constant.Common.LOGIN_HTTP_HOST): String

    @Key("biz_2")
    fun putBizUrl(value: String)

    @Key("biz_2")
    fun getBizUrl(@Default defaultValue: String = Constant.Common.BIZ_HTTP_HOST): String

    @Key("map_2")
    fun putMapUrl(value: String)

    @Key("map_2")
    fun getMapUrl(@Default defaultValue: String = Constant.Common.MAP_HTTP_HOST): String

    @Key("download_2")
    fun putDownloadUrl(value: String)

    @Key("download_2")
    fun getDownloadUrl(@Default defaultValue: String = Constant.Common.DOWNLOAD_HTTP_HOST): String

    @Key("uploadServer_2")
    fun putUploadServer(value: Int)

    @Key("uploadServer_2")
    fun getUploadServer(@Default defaultValue: Int = 0): Int

    @Key("uploadUrl_2")
    fun putUploadUrl(value: String)

    @Key("uploadUrl_2")
    fun getUploadUrl(@Default defaultValue: String = ""): String

    @Key("Config_2")
    fun putConfigUrl(value: String)

    @Key("Config_2")
    fun getConfigUrl(@Default defaultValue: String = BackupLoginHostInterceptor.CONFIG_HTTP_HOST): String

    @Key("font_size")
    fun putFontSize(value: Float)

    @Key("font_size")
    fun getFontSize(@Default defaultValue: Float = 1f): Float

    @Key("first_install")
    fun getFirstInstall(@Default defaultValue: Boolean = false): Boolean

    @Key("first_install")
    fun putFirstInstall(value: Boolean)

    @Key("bind_data")
    fun getWakeupBindData(@Default defaultValue: String = ""): String

    @Key("bind_data")
    fun putWakeupBindData(value: String)

    @Key("channel_code")
    fun getChannelCode(@Default defaultValue: String = ""): String

    @Key("channel_code")
    fun putChannelCode(value: String)

    @Key("check_notification_version_code")
    fun putCheckNotificationVersionCode(value: Int)

    @Key("check_notification_version_code")
    fun getCheckNotificationVersionCode(@Default defaultValue: Int = 0): Int

    @Key("check_welcome_page")
    fun putCheckWelcomePage(value: Boolean)

    @Key("check_welcome_page")
    fun getCheckWelcomePage(@Default defaultValue: Boolean = false): Boolean

    /**
     * ----------------------------------------以下是与账号关联的配置项----------------------------------------
     */
    @Key("error_realm")
    fun putErrorRealm(value: String)

    @Key("error_realm")
    fun getErrorRealm(): String

    @Key("realm_data_version")
    fun putRealmDataVersion(value: Int)

    @Key("realm_data_version")
    fun getRealmDataVersion(): Int

    @Key("user_dh_keys")
    fun putUserDhKeys(value: String)

    @Key("user_dh_keys")
    fun getUserDhKeys(@Default defaultValue: String = ""): String

    @Key("last_manual_login_time")
    fun putLastManualLoginTime(value: Long)

    @Key("last_manual_login_time")
    fun getLastManualLoginTime(@Default defaultValue: Long = 0L): Long

    @Key("web_mute")
    fun getWebMute(@Default defaultValue: Boolean = false): Boolean

    @Key("web_mute")
    fun putWebMute(value: Boolean)

    @Key("voice_default_use_speaker")
    fun putVoiceDefaultUseSpeaker(value: Boolean)

    @Key("voice_default_use_speaker")
    fun getVoiceDefaultUseSpeaker(@Default defaultValue: Boolean = true): Boolean

    @Key("first_open_message_permission")
    fun getFirstOpenMessagePermission(@Default defaultValue: Boolean = false): Boolean

    @Key("first_open_message_permission")
    fun putFirstOpenMessagePermission(value: Boolean)

    @Key("applock_on")
    fun putAppLockIsOn(value: Boolean)

    @Key("applock_on")
    fun getAppLockIsOn(@Default value: Boolean = false): Boolean

    @Key("applock_fingerprint_on")
    fun putAppLockFingerPrintIsOn(value: Boolean)

    @Key("applock_fingerprint_on")
    fun getAppLockFingerPrintIsOn(@Default value: Boolean = false): Boolean

    @Key("applock_time")
    fun putAppLockExipreTime(value: Int)

    @Key("applock_time")
    fun getAppLockExipreTime(@Default value: Int = 0): Int

    @Key("blur_screen")
    fun putBlurScreen(value: Boolean)

    @Key("blur_screen")
    fun getBlurScreen(@Default value: Boolean = false): Boolean


    @Key("group_member_update_time")//后台更新的时间
    fun putGroupMemberUpdateTime(value: Long)

    @Key("group_member_update_time")
    fun getGroupMemberUpdateTime(@Default defaultValue: Long = 0L): Long

    @Key("group_member_temp_update_time")//后台临时更新的时间
    fun putGroupMemberTempUpdateTime(value: Long)

    @Key("group_member_temp_update_time")
    fun getGroupMemberTempUpdateTime(@Default defaultValue: Long = 0L): Long

    @Key("group_member_last_update_time")//本地2分钟更新一次的时间
    fun putGroupMemberLastUpdateTime(value: Long)

    @Key("group_member_last_update_time")
    fun getGroupMemberLastUpdateTime(@Default defaultValue: Long = 0L): Long

    @Key("sync_chat_message_7")//是否通过过本地消息到搜索数据库
    fun putSyncChatMessage(value: Boolean)

    @Key("sync_chat_message_7")
    fun getSyncChatMessage(@Default defaultValue: Boolean = false): Boolean

    @Key("disable_account")
    fun putDisableAccount(value: Boolean)

    @Key("disable_account")
    fun getDisableAccount(@Default value: Boolean = false): Boolean
}
