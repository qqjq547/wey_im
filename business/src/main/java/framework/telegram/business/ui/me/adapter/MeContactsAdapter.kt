package framework.telegram.business.ui.me.adapter


import android.content.Context
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RelativeLayout
import android.widget.SectionIndexer
import android.widget.TextView
import androidx.annotation.NonNull
import com.chad.library.adapter.base.BaseViewHolder
import framework.telegram.business.R
import framework.telegram.business.bridge.search.StringUtil
import framework.telegram.business.ui.me.bean.ContactsBean
import framework.telegram.business.ui.me.bean.ContactsBean.Companion.ITEM_HEAD
import framework.telegram.business.ui.me.bean.ContactsBean.Companion.ITEM_INFO
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter

class MeContactsAdapter(private val listener:(ContactsBean)->Unit) : AppBaseMultiItemQuickAdapter<ContactsBean, BaseViewHolder>(null), SectionIndexer {

    private var _keyword: String = ""

    private var _checkList: ArrayList<ContactsBean> = ArrayList()

    private var mSectionList : ArrayList<Int>? = null

    init {
        addItemType(ITEM_HEAD,R.layout.bus_me_contacts_item_head)
        addItemType(ITEM_INFO, R.layout.bus_me_contacts_item_content)
    }

    override fun convert(helper: BaseViewHolder, item: ContactsBean?) {
        if (_keyword != "") {
            item?.let {
                if (item.getName().indexOf(_keyword) != -1 && item.getPhone() != "" && item.itemType == ITEM_INFO) {
                    helper?.getView<RelativeLayout>(R.id.relative_layout_item)?.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(mContext, 61f))
                    helper?.getView<RelativeLayout>(R.id.relative_layout_item)?.setOnClickListener { _ -> listener(item) }
                    helper?.setText(R.id.text_view_name, StringUtil.setRedHitTextColor(_keyword, item.getName()))
                    helper?.getView<TextView>(R.id.text_view_first_char)?.text = item.getName().first().toString()
                    helper?.getView<TextView>(R.id.text_view_phone)?.text = item.getPhone()
                    helper?.getView<CheckBox>(R.id.check_box)?.isChecked = _checkList.contains(item)
                    helper?.getView<CheckBox>(R.id.check_box)?.setOnClickListener { _ ->
                        listener(item)
                    }
                } else
                    helper?.getView<RelativeLayout>(R.id.relative_layout_item)?.layoutParams = ViewGroup.LayoutParams(0, 0)

            }
        } else {


            when (item?.itemType) {

                ITEM_HEAD -> {
                    /**
                     * 如果是ITEM_HEAD的话，title就相当于字母
                     */
                    helper?.getView<TextView>(R.id.text_view_letter)?.text = item.getTitle().toString()

                    helper?.getView<RelativeLayout>(R.id.relative_layout_item)?.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(mContext, 24f))
                }

                ITEM_INFO -> {
                    helper?.getView<RelativeLayout>(R.id.relative_layout_item)?.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(mContext, 61f))
                    helper?.getView<RelativeLayout>(R.id.relative_layout_item)?.setOnClickListener { _ -> listener(item) }
                    helper?.getView<TextView>(R.id.text_view_first_char)?.text = item.getName().first().toString()
                    helper?.getView<TextView>(R.id.text_view_name)?.text = item.getName()
                    helper?.getView<TextView>(R.id.text_view_phone)?.text = item.getPhone()
                    helper?.getView<CheckBox>(R.id.check_box)?.isChecked = _checkList.contains(item)
                    helper?.getView<CheckBox>(R.id.check_box)?.setOnClickListener { _ ->
                        listener(item)
                    }
                }
            }
        }
    }

    override fun getSections(): Array<String> {
        if (_keyword == "") {
            val sections = ArrayList<String>(26)
            mSectionList = ArrayList<Int>(26)
            var i = 0
            val size = mData?.size ?: 0
            while (i < size) {
                val section = mData?.get(i)
                section?.let {
                    if (!sections.toString().contains(section.getTitle())) {
                        sections.add(section.getTitle().toString() ?: "")
                        mSectionList!!.add(i)
                    }
                    i++
                }
            }
            return sections.toTypedArray()
        }else
            return arrayOf("")
    }

    override fun getPositionForSection(sectionIndex: Int): Int {
        return if (_keyword == "")
            mSectionList?.get(sectionIndex) ?: 0
        else
            0
    }

    override fun getSectionForPosition(position: Int): Int {
        return if (_keyword == "")
            mSectionList?.get(position) ?: 0
        else
            0
    }


    fun filter(keyword: String, checkList: ArrayList<ContactsBean>){
        _keyword = keyword
        _checkList = checkList
        notifyDataSetChanged()
    }

    private fun dpToPx(@NonNull context: Context, dp: Float): Int {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.resources.displayMetrics) + 0.5f)
    }
}

