package framework.telegram.message.ui.telephone.adapter

import android.text.TextPaint
import android.widget.SectionIndexer
import android.widget.TextView
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.message.R
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.image.AppImageView
import java.util.*

class StreamCallContactsAdapter : AppBaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(null), SectionIndexer {

    init {
        addItemType(0, R.layout.msg_contacts_item)
        addItemType(1, R.layout.msg_contacts_item_head)
    }

    private val mSectionPositions by lazy { ArrayList<Int>(29) }

    override fun getSections(): Array<String> {
        mSectionPositions.clear()

        val sections = ArrayList<String>(29)
        var i = 0
        val size = mData?.size ?: 0
        while (i < size) {
            val data = mData?.get(i)
            if (data?.itemType == 1) {
                val section = (data as ItemData).data as String
                if (!sections.contains(section)) {
                    sections.add(section)
                    mSectionPositions.add(i)
                }
            }
            i++
        }
        return sections.toTypedArray()
    }

    override fun getSectionForPosition(position: Int): Int {
        return 0
    }

    override fun getPositionForSection(sectionIndex: Int): Int {
        return mSectionPositions[sectionIndex]
    }

    override fun convert(helper: BaseViewHolder, item: MultiItemEntity?) {
        item?.let {
            if (item.itemType == 0) {
                val itemData = (item as ItemData).data as ContactDataModel
                helper?.setText(R.id.app_text_view_name, itemData.displayName)
                (helper?.getView<TextView>(R.id.app_text_view_name)?.paint as TextPaint).isFakeBoldText = true
                helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(UriUtils.parseUri(itemData.icon))
                helper.addOnClickListener(R.id.image_view_video)
                helper.addOnClickListener(R.id.image_view_audio)
            } else {
                val itemData = (item as ItemData).data as String
                if (helper?.itemView?.context?.getString(R.string.string_star).equals(itemData)){
                    helper?.setText(R.id.text_view_head_name,  helper?.itemView?.context?.getString(R.string.string_star_sign))
                }else {
                    helper?.setText(R.id.text_view_head_name, itemData)
                }
            }
        }
    }

    class ItemData(val data: Any?) : MultiItemEntity {

        override fun getItemType(): Int {
            return if (data is ContactDataModel) {
                0
            } else {
                1
            }
        }
    }
}
