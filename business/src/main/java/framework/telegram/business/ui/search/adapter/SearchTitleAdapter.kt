package framework.telegram.business.ui.search.adapter

import android.content.Context
import android.widget.TextView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.common.SearchMoreModel
import framework.ideas.common.model.common.SearchMoreModel.*
import framework.ideas.common.model.common.TitleModel
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.Search.KEYWORD
import framework.telegram.business.bridge.Constant.Search.SEARCH_ITEM_TITLE
import framework.telegram.business.bridge.search.BaseSearchAdapter

/**
 * Created by lzh on 19-5-21.
 * INFO:通讯录 adapter
 */
@Route(path = Constant.ARouter.ROUNTE_SERVICE_ADAPTER_TITLE, name = "联系人")
class SearchTitleAdapter : BaseSearchAdapter<MultiItemEntity, BaseViewHolder>(null) {
    private var mkeyword = ""
    override fun setSearchType(searchType: Int) {
    }

    override fun setExtra(dataSet: Set<Long>) {
    }

    override fun setSearchTargetId(tagerId: Long) {
    }

    override fun setKeyword(keyword: String) {
        mkeyword = keyword
    }

    override fun getAdapter(): BaseSearchAdapter<MultiItemEntity, BaseViewHolder> = this

    override fun init(context: Context?) {
    }

    override fun setExtra(mapList: List<MutableMap<String, String>>) {

    }

    override fun addItems() {
        putLayout(SEARCH_ITEM_TITLE, R.layout.bus_title2)
        putLayout(TITLE_MORE_CONTACTS, R.layout.bus_search_more_title)
        putLayout(TITLE_MORE_GROUP, R.layout.bus_search_more_title)
        putLayout(TITLE_MORE_CHAT, R.layout.bus_search_more_title)
    }

    override fun convertImpl(helper: BaseViewHolder, item: MultiItemEntity) {
        when (item.itemType) {
            SEARCH_ITEM_TITLE -> {
                if (item is TitleModel) {
                    helper.setGone(R.id.space,item.getmIsSpace())
                    helper.getView<TextView>(R.id.text_view_title)?.text = item.title
                }
            }
            TITLE_MORE_CONTACTS ->{
                if (item is SearchMoreModel) {
                    helper.getView<TextView>(R.id.text_view_title)?.text = item.title
                    helper.itemView.setOnClickListener {
                        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_EXPAND)
                                .withString(KEYWORD,mkeyword)
                                .withInt(Constant.Search.SEARCH_TYPE, Constant.Search.SEARCH_CONTACTS_EXPAND).navigation()
                    }
                }
            }
            TITLE_MORE_GROUP ->{
                if (item is SearchMoreModel) {
                    helper.getView<TextView>(R.id.text_view_title)?.text = item.title
                    helper.itemView.setOnClickListener {
                        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_EXPAND)
                                .withString(KEYWORD,mkeyword)
                                .withInt(Constant.Search.SEARCH_TYPE, Constant.Search.SEARCH_GROUP_EXPAND).navigation()
                    }
                }
            }
            TITLE_MORE_CHAT  ->{
                if (item is SearchMoreModel) {
                    helper.getView<TextView>(R.id.text_view_title)?.text = item.title
                    helper.itemView.setOnClickListener {
                        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_EXPAND)
                                .withString(KEYWORD,mkeyword)
                                .withInt(Constant.Search.SEARCH_TYPE, Constant.Search.SEARCH_CHAT_EXPAND).navigation()
                    }
                }
            }
        }
    }
}

