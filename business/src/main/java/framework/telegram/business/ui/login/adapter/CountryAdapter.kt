package framework.telegram.business.ui.login.adapter

import android.widget.SectionIndexer
import com.chad.library.adapter.base.BaseViewHolder
import framework.telegram.business.R
import framework.telegram.business.bean.CountryCodeInfoBean
import framework.telegram.business.bridge.Constant
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import java.util.*

/**
 * Created by lzh on 19-5-21.
 * INFO:
 */
class CountryAdapter : AppBaseMultiItemQuickAdapter<CountryCodeInfoBean, BaseViewHolder>(null), SectionIndexer {


    private var mSectionPositions: ArrayList<Int>? = null

    init {
        addItemType(Constant.Search.SEARCH_HEAD,R.layout.bus_login_item_select_country_head)
        addItemType(Constant.Search.SEARCH_COUNTRY,R.layout.bus_login_item_select_country)
    }

    override fun convert(helper: BaseViewHolder, item: CountryCodeInfoBean?) {
        helper?.let {
            when (it.itemViewType){
                Constant.Search.SEARCH_HEAD -> {
                    it.setText(R.id.text_view_head_name,item?.getLetterByLanguage())
                }
                Constant.Search.SEARCH_COUNTRY -> {
                    it.setText(R.id.text_view_name,item?.countryNameUS)
                    it.setText(R.id.text_view_code,"+"+item?.getCountryCode())
                }else -> {}
            }
        }
    }

    override fun getSectionForPosition(position: Int): Int {
        return 0
    }

    override fun getSections(): Array<String> {
        val sections = ArrayList<String>(26)
        mSectionPositions = ArrayList(26)
        var i = 0
        val size = data?.size ?: 0
        while (i < size) {
           val data =  data?.get(i)
            var section =""
            if (data is CountryCodeInfoBean){
                section = data.getLetterByLanguage()!![0].toString().toUpperCase()
            }else{
                section =  data as String
            }
            if (!sections.contains(section)) {
                sections.add(section)
                mSectionPositions!!.add(i)
            }
            i++
        }
        return sections.toTypedArray()
    }

    override fun getPositionForSection(sectionIndex: Int): Int {
        return mSectionPositions?.get(sectionIndex) ?: 0
    }
}

