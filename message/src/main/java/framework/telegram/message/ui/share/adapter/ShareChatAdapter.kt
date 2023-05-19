package framework.telegram.message.ui.share.adapter

/**
 * Created by lzh on 19-6-27.
 * INFO:
 */

import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.message.R
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView

class ShareChatAdapter : AppBaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(null) {

    init {
        addItemType(TitleModel.TITLE_HEAD, R.layout.msg_title2)
        addItemType(TitleModel.TITLE_SELECT_FRIEND, R.layout.msg_select_contacts_item)
        addItemType(TitleModel.TITLE_SELECT_GROUP, R.layout.msg_select_contacts_item)
        addItemType(ChatModel.CHAT_TYPE_PVT, R.layout.msg_select_contacts_item)
        addItemType(ChatModel.CHAT_TYPE_GROUP, R.layout.msg_select_group_item)
    }

    override fun convert(helper: BaseViewHolder, content: MultiItemEntity?) {
        if (helper == null || content == null) {
            return
        }

        when (content.itemType) {//
            ChatModel.CHAT_TYPE_PVT, ChatModel.CHAT_TYPE_GROUP -> {
                if (content is ChatModel) {
                    helper?.getView<AppTextView>(R.id.app_text_view_name)?.text = content.chaterName
                    helper?.getView<AppImageView>(R.id.image_view_icon).setImageURI(UriUtils.parseUri(content.chaterIcon))
                }
            }
            TitleModel.TITLE_HEAD -> {
                if (content is TitleModel) {
                    helper.setText(R.id.text_view_title, content.title)
                }
            }
            TitleModel.TITLE_SELECT_FRIEND, TitleModel.TITLE_SELECT_GROUP -> {
                if (content is TitleModel) {
                    helper?.getView<AppTextView>(R.id.app_text_view_name)?.text = content.title
                    helper?.getView<AppImageView>(R.id.image_view_icon).setImageURI(UriUtils.newWithResourceId(content.drawable))
                }
            }
        }
    }
}