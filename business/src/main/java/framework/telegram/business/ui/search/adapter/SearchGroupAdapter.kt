package framework.telegram.business.ui.search.adapter

import android.content.Context
import android.text.TextUtils
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.group.GroupInfoModel
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.Search.SEARCH_CONTACTS
import framework.telegram.business.bridge.Constant.Search.SEARCH_GROUP_EXPAND
import framework.telegram.business.bridge.Constant.Search.SEARCH_ITEM_GROUP
import framework.telegram.business.bridge.Constant.Search.SEARCH_MY_GROUP
import framework.telegram.business.bridge.search.BaseSearchAdapter
import framework.telegram.business.bridge.search.StringUtil
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_GROUP_CHAT_ACTIVITY
import framework.telegram.support.system.pinyin.FastPinyin
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView

/**
 * Created by lzh on 19-5-21.
 * INFO:通讯录 adapter
 */
@Route(path = Constant.ARouter.ROUNTE_SERVICE_ADAPTER_GROUP, name = "群")
class SearchGroupAdapter : BaseSearchAdapter<MultiItemEntity, BaseViewHolder>(null) {
    private var mKeyword = ""
    private var mTargetPic :String ?= ""
    private var mTargetName:String ?= ""

    override fun setSearchTargetId(tagerId: Long) {
    }

    override fun setExtra(dataSet: Set<Long>) {
    }

    override fun setKeyword(keyword: String) {
        mKeyword = keyword
    }

    override fun setExtra(mapList: List<MutableMap<String, String>>) {
        mapList.forEach {
            mTargetName = it[SEARCH_USER_NAME]
            mTargetPic = it[SEARCH_USER_ICON]
        }
    }

    private var mSearchType = 0

    override fun setSearchType(searchType: Int) {
        mSearchType = searchType
    }

    override fun getAdapter(): BaseSearchAdapter<MultiItemEntity, BaseViewHolder>  = this

    override fun init(context: Context?) {
    }

    override fun addItems() {
         putLayout(SEARCH_ITEM_GROUP, R.layout.bus_serach_group_item)
    }

    override fun convertImpl(helper: BaseViewHolder, item: MultiItemEntity) {
        when (item.itemType) {
            SEARCH_ITEM_GROUP -> {
                if (item is GroupInfoModel) {
                    var lightWord = mKeyword
                    if (!TextUtils.isEmpty(item.searchName) && item.searchName.contains(mKeyword.toUpperCase())) {
                        lightWord = FastPinyin.findWordFromPinyin(mKeyword,item.name)
                    }else if (!TextUtils.isEmpty(item.shortName)&& item.shortName.contains(mKeyword.toUpperCase())) {
                        lightWord = FastPinyin.findWordFromPinyin(mKeyword,item.name)
                    }

                    val name = item.name
                    helper.getView<AppTextView>(R.id.app_text_view_name)?.text = StringUtil.setHitTextColor(lightWord,name)
                    helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item.pic)
                    helper.itemView.setOnClickListener {
                        when(mSearchType){
                            SEARCH_CONTACTS,SEARCH_MY_GROUP->{
                                ARouter.getInstance().build(ROUNTE_MSG_GROUP_CHAT_ACTIVITY)
                                        .withLong("targetGid", item.groupId).navigation()
                            }
                            SEARCH_GROUP_EXPAND->{
                                ARouter.getInstance().build(ROUNTE_MSG_GROUP_CHAT_ACTIVITY)
                                        .withLong("targetGid", item.groupId).navigation()
                            }
                            Constant.Search.SEARCH_CHAT->{
                                ARouter.getInstance().build(ROUNTE_MSG_GROUP_CHAT_ACTIVITY).withLong("targetGid", item.groupId).navigation()
                            }

                        }
                    }
                }
            }

        }
    }
}

