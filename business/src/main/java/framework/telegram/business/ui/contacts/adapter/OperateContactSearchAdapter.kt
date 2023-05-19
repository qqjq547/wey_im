package framework.telegram.business.ui.contacts.adapter

import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.R
import framework.telegram.business.bridge.search.StringUtil
import framework.telegram.business.event.CreateGroupSearchMemberEvent
import framework.telegram.business.event.CreateGroupSelectMemberEvent
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.image.AppImageView
import java.util.*

class OperateContactSearchAdapter(var mKeyword: String) : AppBaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(null), CompoundButton.OnCheckedChangeListener {

    init {
        addItemType(0, R.layout.bus_contacts_selectable_item)
    }

    private val mSelectedUids by lazy { ArrayList<Long>() }

    private val mUnSelectUids by lazy { ArrayList<Long>() }


    fun addSelectedUid(uid: Long) {
        if (!mSelectedUids.contains(uid))
            mSelectedUids.add(uid)
    }

    fun removeSelectedUid(uid: Long) {
        if (mSelectedUids.contains(uid))
            mSelectedUids.remove(uid)
    }

    fun setSelectedUid(list: ArrayList<Long>) {
        mSelectedUids.clear()
        mSelectedUids.addAll(list)
    }

    fun setUnSelectUids(uids: List<Long>) {
        mUnSelectUids.clear()
        mUnSelectUids.addAll(uids)
    }

    fun addUnSelectUid(uid: Long) {
        mUnSelectUids.add(uid)
    }

    override fun convert(helper: BaseViewHolder, item: MultiItemEntity?) {
        item?.let {
            if (item.itemType == 0) {
                val itemData = (item as ItemData).data as ContactDataModel

                helper?.setText(R.id.app_text_view_name, StringUtil.setHitTextColor(mKeyword, itemData.displayName))
                helper?.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(UriUtils.parseUri(itemData.icon))
                val checkBottom =  helper?.getView<CheckBox>(R.id.check_box_selected)
                if (mUnSelectUids.contains(itemData.uid)) {
                    //不可选且默认未选
                    checkBottom?.setOnCheckedChangeListener(null)
                    checkBottom?.isChecked = false
                    checkBottom?.isEnabled = false
                    helper?.itemView?.setOnClickListener(null)
                    helper?.getView<View>(R.id.view_mask)?.visibility = View.VISIBLE
                } else {
                    //可选
                    checkBottom?.isEnabled = true
                    checkBottom?.setOnCheckedChangeListener(null)
                    checkBottom?.isChecked = mSelectedUids.contains(itemData.uid)
                    checkBottom?.setOnCheckedChangeListener(this@OperateContactSearchAdapter)
                    helper?.getView<View>(R.id.view_mask)?.visibility = View.GONE
                    checkBottom?.tag = itemData
                    helper?.itemView?.setOnClickListener{
                        checkBottom?.setOnCheckedChangeListener(null)
                        val check = !(checkBottom?.isChecked ?:false)
                        checkBottom?.isChecked = check
                        setCheckChanged(itemData,check)
                        checkBottom?.setOnCheckedChangeListener(this@OperateContactSearchAdapter)
                    }
                }
            }
        }
    }

    private fun setCheckChanged(itemData:ContactDataModel,isChecked:Boolean){
        if (isChecked) {
            mSelectedUids.add(itemData.uid)
            EventBus.publishEvent(CreateGroupSearchMemberEvent("", 2))
            EventBus.publishEvent(CreateGroupSelectMemberEvent(itemData.uid, itemData.icon, itemData.displayName,1, 2))
        } else {
            mSelectedUids.remove(itemData.uid)
            EventBus.publishEvent(CreateGroupSelectMemberEvent(itemData.uid, itemData.icon, itemData.displayName,2, 2))
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (buttonView?.tag is ContactDataModel) {
            val itemData = buttonView.tag as ContactDataModel
            setCheckChanged(itemData,isChecked)
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

    fun setKeyword(keyword: String) {
        mKeyword = keyword
    }
}