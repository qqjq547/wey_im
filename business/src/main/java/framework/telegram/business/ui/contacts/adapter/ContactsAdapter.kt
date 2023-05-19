package framework.telegram.business.ui.contacts.adapter

import android.widget.LinearLayout
import android.widget.SectionIndexer
import android.widget.TextView
import com.chad.library.adapter.base.BaseViewHolder
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.ui.contacts.bean.ContactItemBean
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.EMPTY_TITLE
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.ITEM_CONTACT
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.ITEM_CONTACT_REQ
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.ITEM_FOOT
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.ITEM_GROUPS
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.ITEM_HEAD
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.ITEM_OFFICIAL
import framework.telegram.business.ui.widget.ViewUtils
import framework.telegram.message.bridge.event.JoinContactReqEvent
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView
import framework.telegram.ui.utils.ScreenUtils
import java.util.*

/**
 * Created by lzh on 19-5-21.
 * INFO:通讯录 adapter
 */
class ContactsAdapter : AppBaseMultiItemQuickAdapter<ContactItemBean, BaseViewHolder>(null), SectionIndexer {
    private var mSectionPositions: ArrayList<Int>? = null

    override fun getSectionForPosition(position: Int): Int = 0

    init {
        addItemType(ITEM_HEAD, R.layout.bus_contacts_item_head)
        addItemType(ITEM_CONTACT_REQ, R.layout.bus_contacts_item)
        addItemType(ITEM_GROUPS, R.layout.bus_contacts_item)
        addItemType(ITEM_OFFICIAL, R.layout.bus_contacts_item)
        addItemType(ITEM_CONTACT, R.layout.bus_contacts_item)
        addItemType(ITEM_FOOT, R.layout.bus_contacts_footview)
    }

    override fun convert(helper: BaseViewHolder, item: ContactItemBean?) {
        when (item?.itemType) {
            ITEM_HEAD -> {
                val name = item.getTitle()
                val rootView = helper.getView<LinearLayout>(R.id.linear_layout_root)
                if (name == EMPTY_TITLE) {
                    rootView?.layoutParams?.height = 1
                    rootView?.setBackgroundColor(rootView.context.getSimpleColor(R.color.white))
                } else {
                    rootView?.layoutParams?.height = ScreenUtils.dp2px(rootView.context, 24f)
                    rootView?.setBackgroundColor(rootView.context.getSimpleColor(R.color.edeff2))
                    if (helper.itemView?.context?.getString(R.string.string_star).equals(name)) {
                        helper.getView<TextView>(R.id.text_view_head_name)?.text = helper.itemView?.context?.getString(R.string.string_star_sign)
                    } else {
                        helper.getView<TextView>(R.id.text_view_head_name)?.text = name
                    }
                }
            }
            ITEM_CONTACT_REQ, ITEM_GROUPS, ITEM_OFFICIAL -> {
                val name = item.getTitle()
                helper.getView<AppTextView>(R.id.app_text_view_name)?.text = name
                var bitmap = 0
                when (item.itemType) {
                    ITEM_CONTACT_REQ -> {
                        bitmap = R.drawable.bus_contacts_icon_add

                        if (JoinContactReqEvent.contactReqCount <= 0) {
                            helper.setGone(R.id.text_view_msg_count, false)
                        } else {
                            helper.setGone(R.id.text_view_msg_count, true)
                            helper.setText(R.id.text_view_msg_count, if (JoinContactReqEvent.contactReqCount <= 99) "${JoinContactReqEvent.contactReqCount}" else "99+")
                        }
                    }
                    ITEM_GROUPS -> {
                        bitmap = R.drawable.common_contacts_icon_group

                        helper.setGone(R.id.text_view_msg_count, false)
                    }
                    ITEM_OFFICIAL -> {
                        bitmap = R.drawable.bus_contacts_icon_official

                        helper.setGone(R.id.text_view_msg_count, false)
                    }
                }
                helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(UriUtils.newWithResourceId(bitmap))
                helper.setGone(R.id.text_view_online_status_point, false)
                helper.setGone(R.id.text_view_online_status, false)
            }
            ITEM_FOOT -> {
                helper.getView<TextView>(R.id.text_view_count)?.text = String.format(mContext.getString(R.string.a_contact), item.getTitle())
            }
            else -> {
                val data = item?.getInfo()
                helper.getView<AppTextView>(R.id.app_text_view_name)?.text = data?.displayName
                helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(data?.icon)
                helper.setGone(R.id.text_view_msg_count, false)

                if (data?.isShowLastOnlineTime == true && data.isOnlineStatus) {
                    helper.setGone(R.id.text_view_online_status_point, true)
                } else {
                    helper.setGone(R.id.text_view_online_status_point, false)
                }

                data?.let {
                    ViewUtils.showOnlineStatus(ArouterServiceManager.messageService, data.uid, data.isShowLastOnlineTime, data.isOnlineStatus, data.lastOnlineTime, helper.getView(R.id.text_view_online_status))
                }

                helper.setGone(R.id.text_view_online_status, true)
            }
        }
    }


    override fun getSections(): Array<String> {
        val sections = ArrayList<String>(29)
        mSectionPositions = ArrayList(29)
        var i = 0
        val size = mData?.size ?: 0
        while (i < size) {
            val section = mData?.get(i)
            if (section?.itemType == ITEM_HEAD) {
                if (!sections.contains(section.getTitle())) {
                    sections.add(section.getTitle() ?: "")
                    mSectionPositions!!.add(i)
                }
            }
            i++
        }
        return sections.toTypedArray()
    }

    override fun getPositionForSection(sectionIndex: Int): Int {
        return mSectionPositions?.get(sectionIndex) ?: 0
    }
}

