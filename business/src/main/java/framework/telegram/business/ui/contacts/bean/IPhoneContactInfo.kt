package framework.telegram.business.ui.contacts.bean

import framework.telegram.support.system.storage.sp.core.Key
import framework.telegram.support.system.storage.sp.core.SpName

@SpName("phone_list_preferences")
interface IPhoneContactInfo {

    @Key("phoneList")
    fun putPhoneList(value: String)

    @Key("phoneList")
    fun getPhoneList(): String

//    @Key("phoneList")
//    fun phoneList(@Default value: MutableList<String> = mutableListOf()):MutableList<String>
//
//    @Key("phoneList")
//    fun updatePhoneList(value: MutableList<String>)

}
