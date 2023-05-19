package framework.telegram.business.ui.group.adapter

/**
 * Created by lzh on 19-6-12.
 * INFO:
 */

import android.widget.LinearLayout
import com.chad.library.adapter.base.BaseViewHolder
import com.im.domain.pb.GroupProto
import framework.telegram.business.R
import framework.telegram.ui.doubleclick.recycler.AppBaseQuickAdapter
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView

class SameGroupChatsAdapter(private val mItemClick: ((targetGid: Long) -> Unit)) : AppBaseQuickAdapter<GroupProto.GroupBase, BaseViewHolder>(R.layout.bus_group_item_chat) {

    override fun convert(helper: BaseViewHolder, item: GroupProto.GroupBase?) {
        helper?.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item?.pic)
        helper?.getView<AppTextView>(R.id.app_text_view_name)?.text = item?.name

        helper?.getView<LinearLayout>(R.id.linear_layout_all)?.setOnClickListener {
            if (item != null)
                mItemClick.invoke(item.groupId)
        }
    }
}
