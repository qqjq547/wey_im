package framework.telegram.business.ui.group.adapter

import com.chad.library.adapter.base.BaseViewHolder
import framework.telegram.business.R
import framework.telegram.business.bridge.bean.SelectedUsersModel
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.doubleclick.recycler.AppBaseQuickAdapter
import framework.telegram.ui.image.AppImageView

class SelectedUsersAdapter : AppBaseQuickAdapter<SelectedUsersModel, BaseViewHolder>(R.layout.bus_group_member_item) {

    override fun convert(helper: BaseViewHolder, item: SelectedUsersModel?) {
        item?.let {
            helper?.setText(R.id.app_text_view_name, it.name)
            helper?.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(UriUtils.parseUri(it.icon))
        }
    }
}