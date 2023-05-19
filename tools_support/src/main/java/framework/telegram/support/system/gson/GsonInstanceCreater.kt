package framework.telegram.support.system.gson

import com.google.gson.GsonBuilder

object GsonInstanceCreater {

    val defaultGson by lazy { GsonBuilder().serializeNulls().create() }
}
