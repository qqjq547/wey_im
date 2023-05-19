package framework.telegram.business.ui.contacts.bean

import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.contacts.ContactDataModel

/**
 * Created by lzh on 19-6-17.
 * INFO:
 */

class ContactItemBean : MultiItemEntity {

    private var type = 0
    private var title = ""
    private var modle: ContactDataModel? = null

    override fun getItemType(): Int {
        return type
    }

    constructor(type: Int, title: String) {
        this.type = type
        this.title = title
    }

    constructor(type: Int, modle: ContactDataModel) {
        this.type = type
        this.modle = modle
    }

    companion object {
        const val EMPTY_TITLE = " "
        const val ITEM_HEAD = 0
        const val ITEM_CONTACT_REQ = 2
        const val ITEM_GROUPS = 3
        const val ITEM_OFFICIAL = 4
        const val ITEM_CONTACT = 1
        const val ITEM_FOOT = 5
    }

    fun getInfo(): ContactDataModel? = modle

    fun getTitle(): String? = title
}
