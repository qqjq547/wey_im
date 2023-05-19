package framework.telegram.business.ui.contacts.bean

import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.group.GroupMemberModel

class GroupMemberItemBean(private var data: GroupMemberModel? = null, private var type: Int) : MultiItemEntity {

    override fun getItemType(): Int {
        return type
    }

    companion object {

        const val TYPE_GROUP_MEMBER = 1
        const val TYPE_OPERATE_ADD_MEMBER = 2
        const val TYPE_OPERATE_REMOVE_MEMBER = 3

        fun createGroupMember(info: GroupMemberModel): GroupMemberItemBean {
            return GroupMemberItemBean(info, TYPE_GROUP_MEMBER)
        }

        fun createAddMemberOperate(): GroupMemberItemBean {
            return GroupMemberItemBean(type = TYPE_OPERATE_ADD_MEMBER)
        }

        fun createDelMemberOperate(): GroupMemberItemBean {
            return GroupMemberItemBean(type = TYPE_OPERATE_REMOVE_MEMBER)
        }
    }

    fun getData(): GroupMemberModel? {
        return data
    }
}
