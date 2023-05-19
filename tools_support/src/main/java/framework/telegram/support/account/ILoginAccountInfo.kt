package framework.telegram.support.account

import framework.telegram.support.system.storage.sp.core.Default
import framework.telegram.support.system.storage.sp.core.Key
import framework.telegram.support.system.storage.sp.core.Remove
import framework.telegram.support.system.storage.sp.core.SpName

@SpName("login_user_preferences")
interface ILoginAccountInfo {

    @Key("uuid")
    fun putUUID(value: String)

    @Key("uuid")
    fun getUUID(@Default defaultValue: String = ""): String

    @Key("uuid")
    @Remove
    fun removeUUID()
}
