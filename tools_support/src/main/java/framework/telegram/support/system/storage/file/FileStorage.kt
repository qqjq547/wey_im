package framework.telegram.support.system.storage.file

import framework.telegram.support.BaseApp
import framework.telegram.support.account.Account
import framework.telegram.support.system.storage.file.core.DiskStorage

object FileStorage {

    fun createStorageInstance(): DiskStorage {
        return createStorageInstance("default")
    }

    fun createStorageInstancebyUuid(uuid: String): DiskStorage {
        return createStorageInstance(uuid)
    }

    fun createStorageInstance(stroageName: String): DiskStorage {
        return DiskStorage(BaseApp.app)
    }
}
