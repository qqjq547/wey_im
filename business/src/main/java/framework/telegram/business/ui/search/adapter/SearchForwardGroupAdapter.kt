package framework.telegram.business.ui.search.adapter

import android.content.Context
import android.widget.TextView
import com.alibaba.android.arouter.facade.annotation.Route
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.Search.SEARCH_FORWARD_CONTACTS_GROUP
import framework.telegram.business.bridge.Constant.Search.SEARCH_FORWARD_GROUP
import framework.telegram.business.bridge.Constant.Search.SEARCH_ITEM_GROUP
import framework.telegram.business.bridge.Constant.Search.SEARCH_SHARE_CONTACTS_GROUP
import framework.telegram.business.bridge.Constant.Search.SEARCH_SHARE_GROUP
import framework.telegram.business.bridge.event.*
import framework.telegram.business.bridge.search.BaseSearchAdapter
import framework.telegram.business.bridge.search.StringUtil
import framework.telegram.message.bridge.event.ShareCardEvent
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView

/**
 * Created by lzh on 19-5-21.
 * INFO:通讯录 adapter
 */
@Route(path = Constant.ARouter.ROUNTE_SERVICE_ADAPTER_FORWARD_GROUP, name = "群")
class SearchForwardGroupAdapter : BaseSearchAdapter<MultiItemEntity, BaseViewHolder>(null) {
    private var mKeyword = ""

    override fun setSearchTargetId(tagerId: Long) {
    }
    private val mSendedModelId = mutableSetOf<Long>()

    override fun setExtra(dataSet: Set<Long>) {
        mSendedModelId.addAll(dataSet)
    }

    override fun setKeyword(keyword: String) {
        mKeyword = keyword
    }

    override fun setExtra(mapList: List<MutableMap<String, String>>) {
    }

    private var mSearchType = 0

    override fun setSearchType(searchType: Int) {
        mSearchType = searchType
    }

    override fun getAdapter(): BaseSearchAdapter<MultiItemEntity, BaseViewHolder>  = this

    override fun init(context: Context?) {
    }

    override fun addItems() {
         putLayout(SEARCH_ITEM_GROUP, R.layout.bus_forward_group_item)
    }

    override fun convertImpl(helper: BaseViewHolder, item: MultiItemEntity) {
        when (item.itemType) {
            SEARCH_ITEM_GROUP -> {
                if (item is GroupInfoModel) {
                    val name = item.name
                    helper.getView<AppTextView>(R.id.app_text_view_name)?.text = StringUtil.setHitTextColor(mKeyword,name)
                    helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item.pic)

                    val sendTextView = helper.getView<TextView>(R.id.text_send)
                    if(!mSendedModelId.contains(item.groupId) ){
                        sendTextView.text =sendTextView.context.getString(R.string.pet_text_1172)
                        sendTextView.setTextColor(sendTextView.context.getSimpleColor(R.color.white))
                        sendTextView.background = sendTextView.context.getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
                    }else{
                        sendTextView.text =sendTextView.context.getString(R.string.string_sended)
                        sendTextView.setTextColor(sendTextView.context.getSimpleColor(R.color.a2a4a7))
                        sendTextView.background = null
                    }
                    sendTextView.setOnClickListener {
                        if(!mSendedModelId.contains(item.groupId) ){
                            when(mSearchType){
                                SEARCH_FORWARD_CONTACTS_GROUP,SEARCH_FORWARD_GROUP->{
                                    EventBus.publishEvent(ForwardMessageEvent(ChatModel.CHAT_TYPE_GROUP, item.groupId))
                                }
                                SEARCH_SHARE_CONTACTS_GROUP,SEARCH_SHARE_GROUP->{
                                    EventBus.publishEvent(ShareMessageEvent(item.groupId,ChatModel.CHAT_TYPE_GROUP))
                                }
                                Constant.Search.SEARCH_SHARE_CARD_CONTACTS_GROUP->{
                                    EventBus.publishEvent(ShareCardEvent(item.groupId,ChatModel.CHAT_TYPE_GROUP))
                                }
                            }
                            mSendedModelId.add(item.groupId)
                        }
                        sendTextView.text =sendTextView.context.getString(R.string.string_sended)
                        sendTextView.setTextColor(sendTextView.context.getSimpleColor(R.color.a2a4a7))
                        sendTextView.background = null
                    }

                }
            }

        }
    }
}

