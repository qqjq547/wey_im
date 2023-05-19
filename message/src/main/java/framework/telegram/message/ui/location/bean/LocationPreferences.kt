package framework.telegram.message.ui.location.bean

import framework.telegram.support.system.storage.sp.core.Default
import framework.telegram.support.system.storage.sp.core.Key
import framework.telegram.support.system.storage.sp.core.SpName

/**
 * Created by lzh on 19-5-30.
 * INFO:
 */
@SpName("client_location_preferences")
interface LocationPreferences {

    @Key("client_location")
    fun putLocationPreferences(value: String)

    @Key("client_location")
    fun getLocationPreferences(@Default defaultValue: String = ""): String

    @Key("client_latlng")
    fun putLatLngPreferences(value: String)

    @Key("client_latlng")
    fun getLatLngPreferences(@Default defaultValue: String = ""): String


}
