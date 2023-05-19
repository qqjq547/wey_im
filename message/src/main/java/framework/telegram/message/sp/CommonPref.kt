package framework.telegram.message.sp

import framework.telegram.support.system.storage.sp.core.Key
import framework.telegram.support.system.storage.sp.core.SpName

/**
 * Created by lzh on 19-5-30.
 * INFO:
 */
@SpName("msg_common_preferences")
interface CommonPref {

    /**
     * ----------------------------------------以下是与账号无关联的配置项----------------------------------------
     */
    @Key("filter_words")
    fun putFilterWords(value: String)

    @Key("filter_words")
    fun getFilterWords(): String

    @Key("filter_words_time")
    fun putFilterWordsTime(value: Long)

    @Key("filter_words_time")
    fun getFilterWordsTime(): Long

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

    @Key("audio_draft")
    fun putAudioRecordDraft(value: String)

    @Key("audio_draft")
    fun getAudioRecordDraft(): String

    @Key("text_draft")
    fun putTextDraft(value: String)

    @Key("text_draft")
    fun getTextDraft(): String

    @Key("last_show_group_notice")
    fun putLastShowGroupNotice(value: Long)

    @Key("last_show_group_notice")
    fun getLastShowGroupNotice(): Long

    @Key("has_new_group_notice")
    fun putHasNewGroupNotice(value: Boolean)

    @Key("has_new_group_notice")
    fun getHasNewGroupNotice(): Boolean

    @Key("new_group_notice_number_time")
    fun putHasNewGroupNumberTime(value: Long)

    @Key("new_group_notice_number_time")
    fun getHasNewGroupNumberTime(): Long

    @Key("last_auto_delete_fire_msg_time")
    fun putLastAutoDeleteFireMsgTime(value: Long)

    @Key("last_auto_delete_fire_msg_time")
    fun getLastAutoDeleteFireMsgTime(): Long
}
