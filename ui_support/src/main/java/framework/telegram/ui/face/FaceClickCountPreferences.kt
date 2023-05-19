package framework.telegram.ui.face

import framework.telegram.support.system.storage.sp.core.Default
import framework.telegram.support.system.storage.sp.core.Key
import framework.telegram.support.system.storage.sp.core.SpName

/**
 * Created by lzh on 19-5-30.
* INFO:
*/
@SpName("face_click_count_preferences_v2")
interface FaceClickCountPreferences {

    @Key("count")
    fun puCount(value: String)

    @Key("count")
    fun getCount(@Default defaultValue: String = ""): String

}
