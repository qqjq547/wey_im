package framework.telegram.business.ui.contacts.bean

import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.contacts.ContactReqModel

class ContactReqItemBean : MultiItemEntity {

    private var type = 0
    private var title = ""
    private var info: ContactReqModel? = null

    override fun getItemType(): Int {
        return type
    }

    constructor(info: ContactReqModel, type: Int) {
        this.info = info
        this.type = type
    }

    constructor(title: String) {
        this.title = title
        this.type = NEW_FRIEND_TITLE_TYPE
    }


    companion object {

        const val NEW_FRIEND_TYPE = 1
        const val NEW_FRIEND_FINISH_TYPE = 2
        const val NEW_FRIEND_TITLE_TYPE = 3

        fun createNewFriend(info: ContactReqModel): ContactReqItemBean {
            return ContactReqItemBean(info, NEW_FRIEND_TYPE)
        }

        fun createNewFriendFinish(info: ContactReqModel): ContactReqItemBean {
            return ContactReqItemBean(info, NEW_FRIEND_FINISH_TYPE)
        }

        fun createNewFriendTitle(title: String): ContactReqItemBean {
            return ContactReqItemBean(title)
        }
    }

    fun getInfo(): ContactReqModel? = info

    fun getTitle(): String? = title
}
