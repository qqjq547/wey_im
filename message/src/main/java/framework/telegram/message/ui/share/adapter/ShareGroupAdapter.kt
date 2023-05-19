package framework.telegram.message.ui.share.adapter

import com.afollestad.materialdialogs.customview.getCustomView
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.bridge.event.ShareFinishEvent
import framework.telegram.business.bridge.event.ShareMessageEvent
import framework.telegram.message.R
import framework.telegram.support.system.event.EventBus
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView

/**
 * Created by lzh on 19-6-27.
 * INFO:
 */
class ShareGroupAdapter : AppBaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(null) {

    init {
        addItemType(GroupInfoModel.GROUP_INFO_TYPE, R.layout.msg_select_group_item)
    }

    override fun convert(helper: BaseViewHolder, item: MultiItemEntity?) {
        when (item?.itemType) {
            GroupInfoModel.GROUP_INFO_TYPE -> {
                if (item is GroupInfoModel) {
                    helper?.getView<AppTextView>(R.id.app_text_view_name)?.text = item?.name
                    helper?.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item.pic)
                    helper?.itemView?.setOnClickListener {
                        AppDialog.showCustomView(helper?.itemView.context, R.layout.common_dialog_forward_item, null) {
                            getCustomView().findViewById<AppImageView>(R.id.image_view_icon).setImageURI(item.pic)
                            getCustomView().findViewById<AppTextView>(R.id.app_text_view_name).text = item.name
                            positiveButton(text = mContext.getString(R.string.confirm), click = {
                                EventBus.publishEvent(ShareFinishEvent(item.groupId,ChatModel.CHAT_TYPE_GROUP))
                                EventBus.publishEvent(ShareMessageEvent(item.groupId,ChatModel.CHAT_TYPE_GROUP))
                            })
                            negativeButton(text = mContext.getString(R.string.cancel))
                            title(text = mContext.getString(R.string.forward_content_to))
                        }
                    }
                }
            }
        }
    }


}

