package framework.telegram.message.ui.share.adapter

import android.widget.SectionIndexer
import android.widget.TextView
import com.afollestad.materialdialogs.customview.getCustomView
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.common.TitleModel.TITLE_HEAD
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.bridge.event.ShareFinishEvent
import framework.telegram.business.bridge.event.ShareMessageEvent
import framework.telegram.message.R
import framework.telegram.support.system.event.EventBus
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView
import java.util.*


class ShareContactsAdapter : AppBaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(null), SectionIndexer {
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
        addItemType(ContactDataModel.CONTACT_ITEM_TYPE, R.layout.msg_select_contacts_item)
        addItemType(TITLE_HEAD, R.layout.msg_contacts_item_head)
        addItemType(TitleModel.TITLE_SELECT_FRIEND, R.layout.msg_select_contacts_item)
        addItemType(TitleModel.TITLE_SELECT_GROUP, R.layout.msg_select_contacts_item)
    }

    override fun convert(helper: BaseViewHolder, item: MultiItemEntity?) {
        if (helper == null || item == null) {
            return
        }

        when (item?.itemType) {
            ContactDataModel.CONTACT_ITEM_TYPE -> {
                if (item is ContactDataModel) {
                    helper?.getView<AppTextView>(R.id.app_text_view_name)?.text = item?.displayName
                    helper?.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item.icon)
                    helper?.itemView?.setOnClickListener {
                        AppDialog.showCustomView(helper?.itemView.context, R.layout.common_dialog_forward_item, null) {
                            getCustomView().findViewById<AppImageView>(R.id.image_view_icon).setImageURI(item.icon)
                            getCustomView().findViewById<AppTextView>(R.id.app_text_view_name).text =  item.displayName
                            positiveButton(text = mContext.getString(R.string.confirm), click = {
                                EventBus.publishEvent(ShareFinishEvent(item.uid,ChatModel.CHAT_TYPE_PVT))
                                EventBus.publishEvent(ShareMessageEvent(item.uid,ChatModel.CHAT_TYPE_PVT))
                            })
                            negativeButton(text = mContext.getString(R.string.cancel))
                            title(text = mContext.getString(R.string.share_content_to))
                        }
                    }
                }
            }
            TITLE_HEAD -> {
                if (item is TitleModel) {
                    if (helper?.itemView?.context?.getString(R.string.string_star).equals(item?.title)){
                        helper?.getView<TextView>(R.id.text_view_head_name)?.text = helper?.itemView?.context?.getString(R.string.string_star_sign)
                    }else{
                        helper?.getView<TextView>(R.id.text_view_head_name)?.text = item?.title
                    }

                }
            }
        }
    }
}

