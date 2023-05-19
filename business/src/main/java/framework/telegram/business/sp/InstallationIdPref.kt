package framework.telegram.business.sp

import framework.telegram.support.system.storage.sp.core.Default
import framework.telegram.support.system.storage.sp.core.Key
import framework.telegram.support.system.storage.sp.core.SpName

/**
 * Created by lzh on 19-5-30.
 * INFO:
 */
@SpName("install_Id_preferences")
interface InstallationIdPref {

    @Key("install_ID")
    fun putInstallId(value: String)

    @Key("install_ID")
    fun getInstallId(@Default defaultValue: String = ""): String

}
