package framework.telegram.business.ui.me.bean

import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.telegram.support.system.pinyin.FastPinyin
import framework.telegram.support.tools.Cn2Spell

class ContactsBean : MultiItemEntity {

    companion object{

        const val ITEM_INFO = 1
        const val ITEM_HEAD = 2

    }

    private var title: Char
    private var phone: String = ""
    private var nickname: String = ""
    private var type: Int = 0

    constructor(_phone: String, _nickname: String, _title: Char){
        phone = _phone
        nickname = _nickname
        title = _title
    }

    constructor(_phone: String, _nickname: String, _type: Int, _title: Char){
        phone = _phone
        nickname = _nickname
        type = _type
        title = _title
    }

    override fun getItemType(): Int {
        return type
    }


    fun getTitle(): Char = title
    fun getPhone(): String = phone
    fun getName(): String = nickname

}