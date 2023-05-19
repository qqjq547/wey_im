package framework.telegram.business.ui.contacts.adapter

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.chad.library.adapter.base.BaseViewHolder
import framework.telegram.business.R
import framework.telegram.business.ui.contacts.bean.ContactReqItemBean
import framework.telegram.business.utils.TimeUtil
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView

class ContactReqListAdapter(private val mAgreeClick: ((recordId: Long) -> Unit),
                            private val mItemClick: ((type: Int, applyUid: Long) -> Unit),
                            private val mItemLongClick: (( applyUid: Long) -> Unit)) : AppBaseMultiItemQuickAdapter<ContactReqItemBean, BaseViewHolder>(null) {

    init {
        addItemType(ContactReqItemBean.NEW_FRIEND_TYPE, R.layout.bus_contacts_item_new_friend)
        addItemType(ContactReqItemBean.NEW_FRIEND_FINISH_TYPE, R.layout.bus_contacts_item_new_friend)
        addItemType(ContactReqItemBean.NEW_FRIEND_TITLE_TYPE, R.layout.bus_title)
    }

    override fun convert(helper: BaseViewHolder, item: ContactReqItemBean?) {
        when (helper?.itemViewType) {
            ContactReqItemBean.NEW_FRIEND_TYPE -> {
                val info = item?.getInfo()
                helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(info?.icon)
                helper.getView<AppTextView>(R.id.app_text_view_name).text = info?.nickName
                helper.getView<TextView>(R.id.text_view_time).text = "· ${TimeUtil.timeFormat(info?.modifyTime ?: 0)}"
                helper.getView<AppTextView>(R.id.text_view_introduce).text = info?.msg

                helper.getView<ImageView>(R.id.text_view_image).visibility= View.VISIBLE
                val textView = helper.getView<TextView>(R.id.text_view_button)
                textView.setBackgroundResource(R.drawable.common_corners_trans_178aff_6_0)
                textView.text = ""
                textView.setTextColor(mContext.getSimpleColor(R.color.white))

                helper.getView<FrameLayout>(R.id.frame_layout_button).setOnClickListener {
                    mAgreeClick.invoke(item?.getInfo()?.uid ?: 0)
                    it.isClickable = false
                }

                helper.getView<RelativeLayout>(R.id.relative_layout).setOnClickListener {
                    if (item?.getInfo() != null)
                        mItemClick.invoke(0, item?.getInfo()?.uid ?: 0)
                }
                helper.itemView.setOnLongClickListener {
                    mItemLongClick.invoke( item?.getInfo()?.uid ?: 0)
                    return@setOnLongClickListener false
                }

            }
            ContactReqItemBean.NEW_FRIEND_FINISH_TYPE -> {
                val info = item?.getInfo()
                helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(info?.icon)
                helper.getView<AppTextView>(R.id.app_text_view_name).text = info?.nickName
                helper.getView<TextView>(R.id.text_view_time).text = "· ${TimeUtil.timeFormat(info?.modifyTime
                        ?: 0)}"
                helper.getView<AppTextView>(R.id.text_view_introduce).text = info?.msg
                helper.getView<ImageView>(R.id.text_view_image).visibility= View.GONE

                val textView = helper.getView<TextView>(R.id.text_view_button)
                textView.setBackgroundResource(R.drawable.common_corners_trans_edeff2_6_0)
                textView.text = mContext.getString(R.string.agreed)
                textView.setTextColor(mContext.getSimpleColor(R.color.a2a4a7))

                helper.getView<RelativeLayout>(R.id.relative_layout).setOnClickListener {
                    if (item?.getInfo() != null)
                        mItemClick.invoke(1, item?.getInfo()?.uid ?: 0)
                }

                helper.itemView.setOnLongClickListener {
                    mItemLongClick.invoke( item?.getInfo()?.uid ?: 0)
                    return@setOnLongClickListener false
                }
            }
            ContactReqItemBean.NEW_FRIEND_TITLE_TYPE -> {
                val title = item?.getTitle()
                helper.getView<TextView>(R.id.text_view_title).text = title

            }
        }
    }
}
