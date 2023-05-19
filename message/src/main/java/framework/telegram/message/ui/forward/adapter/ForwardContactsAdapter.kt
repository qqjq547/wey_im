package framework.telegram.message.ui.forward.adapter

import android.widget.SectionIndexer
import android.widget.TextView
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.common.TitleModel.TITLE_HEAD
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.bridge.event.ForwardMessageEvent
import framework.telegram.message.R
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView
import java.util.*

/**
 * Created by lzh on 19-6-27.
 * INFO:
 */
class ForwardContactsAdapter : AppBaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(null), SectionIndexer {
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

    private var mSectionPositions: ArrayList<Int>? = null

    override fun getSections(): Array<String> {
        val sections = ArrayList<String>(29)
        mSectionPositions = ArrayList(29)
        var i = 0
        val size = mData?.size ?: 0
        while (i < size) {
            val section = mData?.get(i)
            if (section?.itemType == TITLE_HEAD && section is TitleModel) {
                if (!sections.contains(section.getTitle())) {
                    sections.add(section.getTitle() ?: "")
                    mSectionPositions!!.add(i)
                }
            }
            i++
        }
        return sections.toTypedArray()
    }

    override fun getSectionForPosition(position: Int): Int = 0

    override fun getPositionForSection(sectionIndex: Int): Int {
        return mSectionPositions?.get(sectionIndex) ?: 0
    }

    init {
        addItemType(ContactDataModel.CONTACT_ITEM_TYPE, R.layout.msg_forward_contacts_item)
        addItemType(TITLE_HEAD, R.layout.msg_contacts_item_head)
    }

    override fun convert(helper: BaseViewHolder, item: MultiItemEntity?) {
        when (item?.itemType) {
            ContactDataModel.CONTACT_ITEM_TYPE -> {
                if (item is ContactDataModel) {
                    helper.getView<AppTextView>(R.id.app_text_view_name)?.text = item.displayName
                    helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item.icon)
                    val sendTextView = helper.getView<TextView>(R.id.text_send)
                    if(!mSendedModelId.contains(item.uid) ){
                        sendTextView.text =sendTextView.context.getString(R.string.pet_text_1172)
                        sendTextView.setTextColor(sendTextView.context.getSimpleColor(R.color.white))
                        sendTextView.background = sendTextView.context.getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
                    }else{
                        sendTextView.text =sendTextView.context.getString(R.string.string_sended)
                        sendTextView.setTextColor(sendTextView.context.getSimpleColor(R.color.a2a4a7))
                        sendTextView.background = null
                    }
                    sendTextView.setOnClickListener {
                        if(!mSendedModelId.contains(item.uid) ){
                            EventBus.publishEvent(ForwardMessageEvent(ChatModel.CHAT_TYPE_PVT,item.uid))
                            mSendedModelId.add(item.uid)
                        }
                        notifyDataSetChanged()
                    }
                }
            }
            TITLE_HEAD -> {
                if (item is TitleModel) {
                    if (helper?.itemView?.context?.getString(R.string.string_star).equals(item?.title)){
                        helper.getView<TextView>(R.id.text_view_head_name)?.text = helper?.itemView?.context?.getString(R.string.string_star_sign)
                    }else{
                        helper?.getView<TextView>(R.id.text_view_head_name)?.text = item?.title
                    }

                }
            }
        }
    }
}

