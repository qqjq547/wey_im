package framework.telegram.business.ui.contacts.adapter


import android.widget.TextView
import com.chad.library.adapter.base.BaseViewHolder
import framework.telegram.business.R
import framework.telegram.business.ui.contacts.bean.PhoneContactsBean
import framework.telegram.business.ui.contacts.bean.PhoneContactsBean.Companion.ITEM_AGREE
import framework.telegram.business.ui.contacts.bean.PhoneContactsBean.Companion.ITEM_HEAD
import framework.telegram.business.ui.contacts.bean.PhoneContactsBean.Companion.ITEM_UNAUDIT
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView

/**
 * Created by lzh on 19-5-21.
 */

class PhoneContactsAdapter(private val mAgreeClick:((Long)->Unit), private val mItemClick:((Long,String)->Unit)) : AppBaseMultiItemQuickAdapter<PhoneContactsBean, BaseViewHolder>(null) {
    init {
        addItemType(ITEM_HEAD, R.layout.bus_contacts_item_head)
        addItemType(ITEM_AGREE, R.layout.bus_contacts_item_new_friend)
        addItemType(ITEM_UNAUDIT, R.layout.bus_contacts_item_new_friend)
    }

    override fun convert(helper: BaseViewHolder, item: PhoneContactsBean?) {
        when (item?.itemType) {
            ITEM_UNAUDIT  -> {
                val info = item?.getInfo()
                helper?.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(info?.icon)
                helper?.getView<AppTextView>(R.id.app_text_view_name)?.text = info?.nickName
                helper?.getView<AppTextView>(R.id.text_view_introduce)?.text = info?.mobileName

                val textView = helper?.getView<TextView>(R.id.text_view_button)
                textView?.setBackgroundResource(R.drawable.common_corners_trans_178aff_6_0)
                textView?.text = mContext.getString(R.string.add)
                textView?.setTextColor(mContext.getSimpleColor(R.color.white))

//                helper?.getView<FrameLayout>(R.id.frame_layout_button) ?.setOnClickListener {
//                    mAgreeClick.invoke(item?.getInfo()?.contactsId ?: 0)
//                }

                helper?.itemView?.setOnClickListener {
                    if (item.getInfo() != null){}
                        mItemClick.invoke( item?.getInfo()?.contactsId ?: 0,item?.getInfo()?.phone?:"")
                }

            }
            ITEM_AGREE-> {
                val info = item?.getInfo()
                helper?.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(info?.icon)
                helper?.getView<AppTextView>(R.id.app_text_view_name)?.text = info?.nickName
                helper?.getView<AppTextView>(R.id.text_view_introduce)?.text = info?.mobileName

                val textView = helper?.getView<TextView>(R.id.text_view_button)
                textView?.setBackgroundResource(R.drawable.common_corners_trans_edeff2_6_0)
                textView?.text = mContext.getString(R.string.added)
                textView?.setTextColor(mContext.getSimpleColor(R.color.a2a4a7))

                helper?.itemView?.setOnClickListener {
                    if (item.getInfo() != null){}
                        mItemClick.invoke( item?.getInfo()?.contactsId ?: 0,item?.getInfo()?.phone?:"")
                }
            }
            ITEM_HEAD -> {
                val title = item?.getTitle()
                helper?.getView<TextView>(R.id.text_view_head_name)?.text = title

            }
        }
    }


}

