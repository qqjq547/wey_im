package framework.telegram.message.ui.forward.adapter

/**
 * Created by lzh on 19-6-27.
 * INFO:
 */

import android.widget.TextView
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.message.R
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView

class ForwardChatAdapter(private val clickSend: ((ChatModel) -> Unit)) : AppBaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(null) {

    init {
        addItemType(TitleModel.TITLE_HEAD, R.layout.msg_title2)
        addItemType(TitleModel.TITLE_SELECT_FRIEND, R.layout.msg_select_contacts_item)
        addItemType(TitleModel.TITLE_SELECT_GROUP, R.layout.msg_select_contacts_item)
        addItemType(ChatModel.CHAT_TYPE_PVT, R.layout.msg_forward_contacts_item)
        addItemType(ChatModel.CHAT_TYPE_GROUP, R.layout.msg_forward_group_item)
    }

    private val mSendedModelId = mutableSetOf<Long>()

    fun addSendedId(id:Long){
        mSendedModelId.add(id)
    }

    fun getSendedModleId():Set<Long>{
        return mSendedModelId
    }

    override fun convert(helper: BaseViewHolder, content: MultiItemEntity?) {
        if (content == null) {
            return
        }

        when (content.itemType) {//
            ChatModel.CHAT_TYPE_PVT, ChatModel.CHAT_TYPE_GROUP -> {
                if (content is ChatModel) {
                    helper.getView<AppTextView>(R.id.app_text_view_name)?.text = content.chaterName
                    helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(UriUtils.parseUri(content.chaterIcon))
                    val sendTextView = helper.getView<TextView>(R.id.text_send)
                    if(!mSendedModelId.contains(content.chaterId) ){
                        sendTextView.text =sendTextView.context.getString(R.string.pet_text_1172)
                        sendTextView.setTextColor(sendTextView.context.getSimpleColor(R.color.white))
                        sendTextView.background = sendTextView.context.getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
                    }else{
                        sendTextView.text =sendTextView.context.getString(R.string.string_sended)
                        sendTextView.setTextColor(sendTextView.context.getSimpleColor(R.color.a2a4a7))
                        sendTextView.background = null
                    }
                    sendTextView.setOnClickListener {
                        if(!mSendedModelId.contains(content.chaterId) ){
                            clickSend.invoke(content)
                        }
                    }
                }
            }
            TitleModel.TITLE_HEAD -> {
                if (content is TitleModel) {
                    helper.setText(R.id.text_view_title, content.title)
                }
            }
            TitleModel.TITLE_SELECT_FRIEND, TitleModel.TITLE_SELECT_GROUP -> {
                if (content is TitleModel) {
                    helper.getView<AppTextView>(R.id.app_text_view_name)?.text = content.title
                    helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(UriUtils.newWithResourceId(content.drawable))
                }
            }
        }
    }
}