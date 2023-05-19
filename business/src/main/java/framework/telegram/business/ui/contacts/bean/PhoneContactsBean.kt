package framework.telegram.business.ui.contacts.bean

import com.chad.library.adapter.base.entity.MultiItemEntity
import com.im.domain.pb.ContactsProto

/**
 * Created by lzh on 19-6-19.
 * INFO:
 */
 class PhoneContactsBean : MultiItemEntity {
    companion object {
        const val ITEM_HEAD = 0
        const val ITEM_UNAUDIT = 1
        const val ITEM_AGREE = 2
    }

    private var title = ""
    private var type = 0
    private var bean: ContactsProto.MobileContactsBase? = null

    override fun getItemType(): Int {
        return type
    }

    constructor(type: Int, bean: ContactsProto.MobileContactsBase) {
        this.type = type
        this.bean = bean
    }

    constructor(type: Int, title:String) {
        this.type = type
        this.title = title
    }


    fun getInfo(): ContactsProto.MobileContactsBase? = bean

    fun getTitle(): String = title

}