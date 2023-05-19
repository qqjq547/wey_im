package framework.telegram.message.ui.search

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.alibaba.android.arouter.facade.annotation.Route
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.business.bridge.search.BaseSearchAdapter
import framework.telegram.business.bridge.search.StringUtil
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.SearchChatEvent
import framework.telegram.support.BaseApp
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.TimeUtils
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView

/**
 * Created by lzh on 19-5-21.
 * INFO:通讯录 adapter
 */
@Route(path = Constant.ARouter.ROUNTE_SERVICE_ADAPTER_GROUP_CHATS, name = "搜索个人消息")
class SearchGroupChatsAdapter : BaseSearchAdapter<MultiItemEntity, BaseViewHolder>(null) {

    private var mTargetId: Long = 0L


    override fun setExtra(dataSet: Set<Long>) {
    }


    override fun setSearchTargetId(tagerId: Long) {
        mTargetId = tagerId
    }

    override fun setSearchType(searchType: Int) {
    }

    private val mMapList by lazy { mutableListOf<MutableMap<String, String>>() }

    override fun init(context: Context?) {

    }

    override fun setExtra(mapList: List<MutableMap<String, String>>) {
        mMapList.clear()
        mMapList.addAll(mapList)
    }

    private var mKeyword = ""

    override fun setKeyword(keyword: String) {
        mKeyword = keyword
    }

    override fun getAdapter(): BaseSearchAdapter<MultiItemEntity, BaseViewHolder> = this

    override fun addItems() {
        putLayout(Constant.Search.SEARCH_ITEM_CHATS_CONTENT1, R.layout.msg_chat_history_pvt_item)
        putLayout(Constant.Search.SEARCH_ITEM_CHATS_CONTENT2, R.layout.msg_chat_history_pvt_item)
        putLayout(Constant.Search.SEARCH_ITEM_CHATS_LOCATION1, R.layout.msg_chat_history_pvt_item)
        putLayout(Constant.Search.SEARCH_ITEM_CHATS_LOCATION2, R.layout.msg_chat_history_pvt_item)
        putLayout(Constant.Search.SEARCH_ITEM_CHATS_FILE1, R.layout.msg_chat_history_pvt_item)
        putLayout(Constant.Search.SEARCH_ITEM_CHATS_FILE2, R.layout.msg_chat_history_pvt_item)
    }

    override fun convertImpl(helper: BaseViewHolder, item: MultiItemEntity) {
        when (item.itemType) {
            Constant.Search.SEARCH_ITEM_CHATS_CONTENT1, Constant.Search.SEARCH_ITEM_CHATS_CONTENT2 -> {
                if (item is MessageModel) {
                    run outside@{
                        mMapList.forEach {
                            if (it[SEARCH_USER_ID] == item.ownerUid.toString()) {
                                helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(UriUtils.parseUri(it[SEARCH_USER_ICON]))//item.icon
                                helper.setText(R.id.app_text_view_name, it[SEARCH_USER_NAME])//displayName ?:
                                helper.setText(R.id.app_text_view_last_msg, StringUtil.setHitTextColor(mKeyword, item.content))
                                helper.setText(R.id.text_view_time, TimeUtils.timeFormatToChat(BaseApp.app, item.time))
                                helper.getView<ImageView>(R.id.image_view_disturb).visibility = View.GONE
                                helper.itemView.setOnClickListener {
                                    EventBus.publishEvent(SearchChatEvent(ChatModel.CHAT_TYPE_GROUP, item.id, mTargetId))
                                }
                                return@outside
                            }
                        }
                    }
                }
            }
            Constant.Search.SEARCH_ITEM_CHATS_LOCATION1, Constant.Search.SEARCH_ITEM_CHATS_LOCATION2 -> {
                if (item is MessageModel) {
                    run outside@{
                        mMapList.forEach {
                            if (it[SEARCH_USER_ID] == item.ownerUid.toString()) {
                                helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(UriUtils.parseUri(it[SEARCH_USER_ICON]))//item.icon
                                helper.setText(R.id.app_text_view_name, it[SEARCH_USER_NAME])//displayName ?:
                                val textView = helper.getView<AppTextView>(R.id.app_text_view_last_msg)
                                helper.setText(R.id.app_text_view_last_msg, StringUtil.setHitTextColor(mKeyword,textView.context.getString(R.string.location_sign)+ item.locationMessageContentBean.address))
                                helper.setText(R.id.text_view_time, TimeUtils.timeFormatToChat(BaseApp.app, item.time))
                                helper.getView<ImageView>(R.id.image_view_disturb).visibility = View.GONE
                                helper.itemView.setOnClickListener {
                                    EventBus.publishEvent(SearchChatEvent(ChatModel.CHAT_TYPE_GROUP, item.id, mTargetId))
                                }
                                return@outside
                            }
                        }
                    }
                }
            }
            Constant.Search.SEARCH_ITEM_CHATS_FILE1, Constant.Search.SEARCH_ITEM_CHATS_FILE2 -> {
                if (item is MessageModel) {
                    run outside@{
                        mMapList.forEach {
                            if (it[SEARCH_USER_ID] == item.ownerUid.toString()) {
                                helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(UriUtils.parseUri(it[SEARCH_USER_ICON]))//item.icon
                                helper.setText(R.id.app_text_view_name, it[SEARCH_USER_NAME])//displayName ?:
                                val textView = helper.getView<AppTextView>(R.id.app_text_view_last_msg)
                                helper.setText(R.id.app_text_view_last_msg, StringUtil.setHitTextColor(mKeyword, textView.context.getString(R.string.file_sign)+item.fileMessageContentBean.name))
                                helper.setText(R.id.text_view_time, TimeUtils.timeFormatToChat(BaseApp.app, item.time))
                                helper.getView<ImageView>(R.id.image_view_disturb).visibility = View.GONE
                                helper.itemView.setOnClickListener {
                                    EventBus.publishEvent(SearchChatEvent(ChatModel.CHAT_TYPE_GROUP, item.id, mTargetId))
                                }
                                return@outside
                            }
                        }
                    }
                }
            }
        }
    }
}

