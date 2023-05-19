package framework.telegram.business.ui.group.adapter

import com.chad.library.adapter.base.BaseViewHolder
import com.facebook.common.util.UriUtil
import framework.ideas.common.model.group.GroupMemberModel
import framework.telegram.business.R
import framework.telegram.business.ui.contacts.bean.GroupMemberItemBean
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.image.AppImageView

class GroupMemberAdapter : AppBaseMultiItemQuickAdapter<GroupMemberItemBean, BaseViewHolder>(null) {

    init {
        addItemType(GroupMemberItemBean.TYPE_GROUP_MEMBER, R.layout.bus_group_member_item)
        addItemType(GroupMemberItemBean.TYPE_OPERATE_ADD_MEMBER, R.layout.bus_group_member_add_item)
        addItemType(GroupMemberItemBean.TYPE_OPERATE_REMOVE_MEMBER, R.layout.bus_group_member_del_item)
    }

    override fun convert(helper: BaseViewHolder, item: GroupMemberItemBean?) {
        item?.let {
            when {
                item.itemType == GroupMemberItemBean.TYPE_GROUP_MEMBER -> {
                    val itemData = item.getData() as GroupMemberModel
                    helper?.setText(R.id.app_text_view_name, itemData.displayName)
                    helper?.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(UriUtils.parseUri(itemData.icon))
                }
                item.itemType == GroupMemberItemBean.TYPE_OPERATE_ADD_MEMBER -> {
                    helper?.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(UriUtil.getUriForResourceId(R.drawable.bus_group_member_add))
                }
                item.itemType == GroupMemberItemBean.TYPE_OPERATE_REMOVE_MEMBER -> {
                    helper?.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(UriUtil.getUriForResourceId(R.drawable.bus_group_member_del))
                }
                else -> {

                }
            }
        }
    }
}