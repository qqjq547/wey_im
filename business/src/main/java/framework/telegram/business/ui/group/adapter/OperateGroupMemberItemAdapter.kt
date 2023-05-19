package framework.telegram.business.ui.group.adapter


import com.chad.library.adapter.base.BaseViewHolder
import framework.telegram.business.R
import framework.telegram.business.ui.contacts.OperateContactActivity
import framework.telegram.ui.doubleclick.recycler.AppBaseQuickAdapter
import framework.telegram.ui.image.AppImageView

class OperateGroupMemberItemAdapter(private val listen: ((Int) -> Unit)) : AppBaseQuickAdapter<OperateContactActivity.SelectUser, BaseViewHolder>(R.layout.bus_group_item_create_select) {

    override fun convert(helper: BaseViewHolder, item: OperateContactActivity.SelectUser) {
        helper.itemView.setOnClickListener {
            listen.invoke(data.indexOf(item))
        }
        helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item.icon)
    }
}
