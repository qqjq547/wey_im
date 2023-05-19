package framework.telegram.message.ui.forward.adapter

import android.widget.TextView
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.bridge.event.ForwardMessageEvent
import framework.telegram.message.R
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView

/**
 * Created by lzh on 19-6-27.
 * INFO:
 */
class ForwardGroupAdapter : AppBaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(null) {

    init {
        addItemType(GroupInfoModel.GROUP_INFO_TYPE, R.layout.msg_forward_group_item)
    }

    private val mSendedModelId = mutableSetOf<Long>()

    fun getSendedModleId():Set<Long>{
        return mSendedModelId
    }

    fun setSendedId(set:Set<Long>){
        mSendedModelId.addAll(set)
    }

    fun addSendedId(id:Long){
        mSendedModelId.add(id)
    }

    override fun convert(helper: BaseViewHolder, item: MultiItemEntity?) {
        when (item?.itemType) {
            GroupInfoModel.GROUP_INFO_TYPE -> {
                if (item is GroupInfoModel) {
                    helper?.getView<AppTextView>(R.id.app_text_view_name)?.text = item?.name
                    helper?.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item.pic)

                    val sendTextView = helper.getView<TextView>(R.id.text_send)
                    if(!mSendedModelId.contains(item.groupId) ){
                        sendTextView.text =sendTextView.context.getString(R.string.pet_text_1172)
                        sendTextView.setTextColor(sendTextView.context.getSimpleColor(R.color.white))
                        sendTextView.background = sendTextView.context.getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
                    }else{
                        sendTextView.text =sendTextView.context.getString(R.string.string_sended)
                        sendTextView.setTextColor(sendTextView.context.getSimpleColor(R.color.a2a4a7))
                        sendTextView.background = null
                    }
                    sendTextView.setOnClickListener {
                        if(!mSendedModelId.contains(item.groupId) ){
                            EventBus.publishEvent(ForwardMessageEvent(ChatModel.CHAT_TYPE_GROUP,item.groupId))
                            mSendedModelId.add(item.groupId)
                        }
                        notifyDataSetChanged()
                    }
                }
            }
        }
    }


}

