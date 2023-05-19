package framework.telegram.business.ui.contacts.adapter

import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.SectionIndexer
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.R
import framework.telegram.business.event.CreateGroupSelectMemberEvent
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.image.AppImageView
import java.util.*

class OperateContactAdapter() : AppBaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(null), SectionIndexer, CompoundButton.OnCheckedChangeListener, Parcelable {

    init {
        addItemType(0, R.layout.bus_contacts_selectable_item)
        addItemType(1, R.layout.bus_contacts_item_head)
    }

    private val mSectionPositions by lazy { ArrayList<Int>(29) }

    private val mSelectedUids by lazy { ArrayList<Long>() }

    private val mUnSelectUids by lazy { ArrayList<Long>() }

    constructor(parcel: Parcel) : this() {
    }

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
        return mSectionPositions.get(sectionIndex)
    }

    fun addSelectedUid(uid: Long) {
        if (!mSelectedUids.contains(uid))
            mSelectedUids.add(uid)
    }

    fun addSelectedUid(uidList: List<Long>) {
        uidList.forEach {
            if (!mSelectedUids.contains(it))
                mSelectedUids.add(it)
        }
    }

    fun removeSelectedUid(uid: Long) {
        if (mSelectedUids.contains(uid))
            mSelectedUids.remove(uid)
    }

    fun removeSelectedUid(uidList: List<Long>) {
        uidList.forEach {
            if (mSelectedUids.contains(it))
                mSelectedUids.remove(it)
        }
    }

    fun setUnSelectUids(uids: List<Long>) {
        mUnSelectUids.clear()
        mUnSelectUids.addAll(uids)
    }

    fun addUnSelectUid(uid: Long) {
        mUnSelectUids.add(uid)
    }

    fun getSelectSize():Int{
        return mSelectedUids.size
    }

    override fun convert(helper: BaseViewHolder, item: MultiItemEntity?) {
        item?.let {
            if (item.itemType == 0) {
                val itemData = (item as ItemData).data as ContactDataModel
                helper?.setText(R.id.app_text_view_name, itemData.displayName)
                helper?.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(UriUtils.parseUri(itemData.icon))

                val checkBottom = helper?.getView<CheckBox>(R.id.check_box_selected)
                when {
                    mUnSelectUids.contains(itemData.uid) -> {
                        //不可选且默认未选
                        checkBottom?.setOnCheckedChangeListener(null)
                        checkBottom?.isChecked = false
                        checkBottom?.isEnabled = false
                        helper?.itemView?.setOnClickListener(null)
                        helper?.getView<View>(R.id.view_mask)?.visibility = View.VISIBLE
                    }
                    else -> {
                        //可选
                        checkBottom?.isEnabled = true
                        checkBottom?.setOnCheckedChangeListener(null)
                        checkBottom?.isChecked = mSelectedUids.contains(itemData.uid)
                        checkBottom?.setOnCheckedChangeListener(this@OperateContactAdapter)
                        checkBottom?.tag = itemData
                        helper?.getView<View>(R.id.view_mask)?.visibility = View.GONE
                        helper?.itemView?.setOnClickListener {
                            checkBottom?.setOnCheckedChangeListener(null)
                            val check = !(checkBottom?.isChecked ?: false)
                            checkBottom?.isChecked = check
                            setCheckChanged(itemData, check)
                            checkBottom?.setOnCheckedChangeListener(this@OperateContactAdapter)
                        }
                    }
                }
            } else {
                val itemData = (item as ItemData).data as String
                if (helper?.itemView?.context?.getString(R.string.string_star).equals(itemData)){
                    helper?.setText(R.id.text_view_head_name, helper.itemView.context?.getString(R.string.string_star_sign))
                }else{
                    helper?.setText(R.id.text_view_head_name, itemData)
                }
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (buttonView?.tag is ContactDataModel) {
            val itemData = buttonView.tag as ContactDataModel
            setCheckChanged(itemData, isChecked)
        }
    }

    private fun setCheckChanged(itemData: ContactDataModel, isChecked: Boolean) {
        if (isChecked) {
            mSelectedUids.add(itemData.uid)
            EventBus.publishEvent(CreateGroupSelectMemberEvent(itemData.uid, itemData.icon,itemData.displayName ,1, 1))
        } else {
            mSelectedUids.remove(itemData.uid)
            EventBus.publishEvent(CreateGroupSelectMemberEvent(itemData.uid, itemData.icon,itemData.displayName, 2, 1))
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

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OperateContactAdapter> {
        override fun createFromParcel(parcel: Parcel): OperateContactAdapter {
            return OperateContactAdapter(parcel)
        }

        override fun newArray(size: Int): Array<OperateContactAdapter?> {
            return arrayOfNulls(size)
        }
    }
}