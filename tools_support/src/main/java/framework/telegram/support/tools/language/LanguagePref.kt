package framework.telegram.support.tools.language

import framework.telegram.support.system.storage.sp.core.Default
import framework.telegram.support.system.storage.sp.core.Key
import framework.telegram.support.system.storage.sp.core.SpName

/**
 * Created by yanggl on 2019/10/14 10:50
 */
@SpName("tools_language")
interface LanguagePref {
    @Key("language")
    fun putLanguage(value: Int)

    @Key("language")
    fun getLanguage(@Default defaultValue: Int =0): Int
}