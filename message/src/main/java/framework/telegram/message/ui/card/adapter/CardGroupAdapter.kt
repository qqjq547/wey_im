package framework.telegram.message.ui.card.adapter

import com.afollestad.materialdialogs.customview.getCustomView
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.message.R
import framework.telegram.message.bridge.event.ShareCardEvent
import framework.telegram.support.system.event.EventBus
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView


/**
 * Created by lzh on 19-6-27.
 * INFO:
 */
class CardGroupAdapter(private val mTargetPic:String, private val mTargetName:String) : AppBaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(null) {

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
                        AppDialog.showCustomView(helper?.itemView.context, R.layout.common_dialog_share_item, null) {
                            getCustomView().findViewById<AppImageView>(R.id.image_view_icon1).setImageURI(mTargetPic)
                            getCustomView().findViewById<AppImageView>(R.id.image_view_icon2).setImageURI(item.pic)
                            getCustomView().findViewById<AppTextView>(R.id.app_text_view_name).text = String.format(helper?.itemView.context.getString(R.string.recommend_mat),mTargetName,item.name)
                            positiveButton(text = helper?.itemView.context.getString(R.string.confirm), click = {
                                EventBus.publishEvent(ShareCardEvent(item.groupId,ChatModel.CHAT_TYPE_GROUP))
                            })
                            negativeButton(text = helper?.itemView.context.getString(R.string.cancel))
                            title(text = helper?.itemView.context.getString(R.string.send_a_card))
                        }
                    }
                }
            }
        }
    }


}

