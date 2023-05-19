package framework.telegram.business.ui.search.adapter

import android.content.Context
import android.widget.TextView
import com.alibaba.android.arouter.facade.annotation.Route
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.telegram.business.bridge.search.BaseSearchAdapter
import framework.telegram.business.R
import framework.telegram.business.bean.CountryCodeInfoBean
import framework.telegram.business.bridge.Constant
import framework.telegram.business.event.SelectCountryEvent
import framework.telegram.support.system.event.EventBus

/**
 * Created by lzh on 19-5-21.
 * INFO:通讯录 adapter
 */
@Route(path = Constant.ARouter.ROUNTE_SERVICE_ADAPTER_COUNTRY, name = "国家搜索")
class SearchCountryAdapter : BaseSearchAdapter<MultiItemEntity, BaseViewHolder>(null) {

    override fun setSearchTargetId(tagerId: Long) {
    }

    override fun setExtra(dataSet: Set<Long>) {
    }

    override fun setSearchType(searchType: Int) {
    }

    private var mKeyword = ""

    override fun setKeyword(keyword: String) {
        mKeyword = keyword
    }

    override fun setExtra(mapList: List<MutableMap<String, String>>) {

    }

    override fun getAdapter(): BaseSearchAdapter<MultiItemEntity, BaseViewHolder> = this

    override fun init(context: Context?) {
    }

    override fun addItems() {
        putLayout(Constant.Search.SEARCH_COUNTRY, R.layout.bus_login_item_select_country)
    }

    override fun convertImpl(helper: BaseViewHolder, item: MultiItemEntity) {
        when (item.itemType) {
            Constant.Search.SEARCH_COUNTRY -> {
                if (item is CountryCodeInfoBean) {
                    helper.getView<TextView>(R.id.text_view_name)?.text = item.countryNameUS
                    helper.getView<TextView>(R.id.text_view_code)?.text = "+" + item.getCountryCode()
                    helper.itemView.setOnClickListener {
                        EventBus.publishEvent(SelectCountryEvent("+" + item.getCountryCode()))
                    }
                }
            }
        }
    }
}

