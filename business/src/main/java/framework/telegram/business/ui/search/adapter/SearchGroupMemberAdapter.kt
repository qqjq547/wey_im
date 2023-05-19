package framework.telegram.business.ui.search.adapter

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.group.GroupMemberModel
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.Search.SEARCH_GROUP_ADD_ADMIN
import framework.telegram.business.bridge.Constant.Search.SEARCH_GROUP_ALL_MEMBER
import framework.telegram.business.bridge.Constant.Search.SEARCH_GROUP_AT_MEMBER
import framework.telegram.business.bridge.Constant.Search.SEARCH_GROUP_TAN_OWNER
import framework.telegram.business.bridge.Constant.Search.SEARCH_ITEM_GROUP_MEMBER
import framework.telegram.business.bridge.event.SearchGroupOperateEvent
import framework.telegram.business.bridge.search.BaseSearchAdapter
import framework.telegram.business.bridge.search.StringUtil
import framework.telegram.business.ui.group.OperateGroupMemberActivity.Companion.OPERATE_TYPE_ADD_ADMIN
import framework.telegram.support.system.event.EventBus
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView

/**
 * Created by lzh on 19-5-21.
 * INFO:通讯录 adapter
 */
@Route(path = Constant.ARouter.ROUNTE_SERVICE_ADAPTER_GROUP_MEMBER, name = "联系人")
class SearchGroupMemberAdapter : BaseSearchAdapter<MultiItemEntity, BaseViewHolder>(null) {

    private var mKeyword = ""

    override fun setSearchTargetId(tagerId: Long) {
    }

    override fun setExtra(dataSet: Set<Long>) {
    }

    override fun setKeyword(keyword: String) {
        mKeyword = keyword
    }

    private var mSearchType = 0

    override fun setSearchType(searchType: Int) {
        mSearchType = searchType
    }

    override fun getAdapter(): BaseSearchAdapter<MultiItemEntity, BaseViewHolder> = this

    override fun init(context: Context?) {
    }

    override fun setExtra(mapList: List<MutableMap<String, String>>) {

    }

    override fun addItems() {
        putLayout(SEARCH_ITEM_GROUP_MEMBER, R.layout.bus_search_contacts_item)
    }

    override fun convertImpl(helper: BaseViewHolder, item: MultiItemEntity) {
        when (item.itemType) {
            SEARCH_ITEM_GROUP_MEMBER -> {
                if (item is GroupMemberModel) {
                    helper.getView<AppTextView>(R.id.app_text_view_name)?.text = StringUtil.setHitTextColor(mKeyword, item.displayName ?: "")
                    helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item?.icon)
                    helper.itemView.setOnClickListener {

                        when (mSearchType) {
                            SEARCH_GROUP_ALL_MEMBER -> {
                                EventBus.publishEvent(SearchGroupOperateEvent(0, item))
                            }
                            SEARCH_GROUP_AT_MEMBER -> {
                                EventBus.publishEvent(SearchGroupOperateEvent(2, item))
                            }
                            SEARCH_GROUP_TAN_OWNER -> {
                                EventBus.publishEvent(SearchGroupOperateEvent(3, item))
                            }
                            SEARCH_GROUP_ADD_ADMIN->{
                                EventBus.publishEvent(SearchGroupOperateEvent(OPERATE_TYPE_ADD_ADMIN, item))
                            }
                        }

                    }
                }
            }
        }
    }
}

