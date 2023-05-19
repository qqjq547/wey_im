package framework.telegram.support.system.storage.sp

import framework.telegram.support.BaseApp
import framework.telegram.support.system.storage.sp.core.IdeasPreference

object SharePreferencesStorage {

    @JvmStatic fun <T> createStorageInstance(clazz: Class<T>): T {
        return createStorageInstance(clazz, "default")
    }

    fun <T> createStorageInstance(clazz: Class<T>, uuid: String): T {
        return IdeasPreference().create(BaseApp.app, clazz, uuid)
    }
}
