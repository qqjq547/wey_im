package framework.telegram.support.account

import framework.telegram.support.BaseApp
import framework.telegram.support.system.storage.sp.core.IdeasPreference
import java.util.*

internal class Account private constructor(val uuid: String) {

    fun <T> getAccountInfo(clazz: Class<T>): T {
        return IdeasPreference().create(BaseApp.app, clazz, uuid)
    }

    companion object {
        fun newAccountByUid(uid: String): Account {
            return Account(UUID.fromString(uid).toString())
        }

        fun newAccountByUUID(uuid: String): Account {
            return Account(uuid)
        }
    }
}
