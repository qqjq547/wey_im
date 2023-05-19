package framework.telegram.business.ui.search.expand.multipleSearch

import android.text.Editable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.common.SearchChatModel
import framework.ideas.common.model.common.SearchChatModel.SEARCH_CHAT
import framework.ideas.common.model.common.SearchMergeChatModel
import framework.ideas.common.model.common.SearchMergeChatModel.SEARCH_MERGE_CHAT
import framework.ideas.common.model.common.SearchMoreModel
import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.contacts.ContactDataModel.CONTACT_ITEM_TYPE
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.ChatModel.CHAT_TYPE_GROUP
import framework.ideas.common.model.im.MessageModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.Search.SEARCH_BACK
import framework.telegram.business.bridge.Constant.Search.SEARCH_ITEM_GROUP
import framework.telegram.business.bridge.search.StringUtil
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_GROUP_CHAT_ACTIVITY
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY
import framework.telegram.support.BaseApp
import framework.telegram.support.system.pinyin.FastPinyin
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.TimeUtils
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView
import kotlin.math.abs

/**
 * Created by lzh on 19-5-21.
 * INFO:通讯录 adapter
 */
class SearchMultChatAdapter : BaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(null) {

    init {
        addItemType(SEARCH_CHAT, R.layout.bus_search_chat_item)
        addItemType(Constant.Search.SEARCH_ITEM_TITLE, R.layout.bus_title2)
        addItemType(SearchMoreModel.TITLE_MORE_CONTACTS, R.layout.bus_search_more_title)
        addItemType(SearchMoreModel.TITLE_MORE_GROUP, R.layout.bus_search_more_title)
        addItemType(SearchMoreModel.TITLE_MORE_CHAT, R.layout.bus_search_more_title)
        addItemType(CONTACT_ITEM_TYPE, R.layout.bus_search_contacts_item_2)
        addItemType(SEARCH_ITEM_GROUP, R.layout.bus_serach_group_item)
        addItemType(SEARCH_MERGE_CHAT, R.layout.bus_search_chat_item)
        addItemType(CHAT_TYPE_GROUP, R.layout.bus_serach_group_item)
    }

    private val mCacheIcon = mutableMapOf<Long, String>()
    private val mCacheName = mutableMapOf<Long, String>()

    private var mKeyword = ""

    public fun setKeyword(keyword:String){
        mKeyword = keyword
    }

    override fun convert(helper: BaseViewHolder, item: MultiItemEntity?) {
        when (item?.itemType) {
            Constant.Search.SEARCH_ITEM_TITLE -> {
                if (item is TitleModel) {
                    helper.setGone(R.id.space,item.getmIsSpace())
                    helper.getView<TextView>(R.id.text_view_title)?.text = item.title
                }
            }
            SearchMoreModel.TITLE_MORE_CONTACTS ->{
                if (item is SearchMoreModel) {
                    helper.getView<TextView>(R.id.text_view_title)?.text = item.title
                    helper.itemView.setOnClickListener {
                        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_EXPAND)
                                .withString(Constant.Search.KEYWORD,mKeyword)
                                .withInt(Constant.Search.SEARCH_TYPE, Constant.Search.SEARCH_CONTACTS_EXPAND).navigation()
                    }
                }
            }
            SearchMoreModel.TITLE_MORE_GROUP ->{
                if (item is SearchMoreModel) {
                    helper.getView<TextView>(R.id.text_view_title)?.text = item.title
                    helper.itemView.setOnClickListener {
                        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_EXPAND)
                                .withString(Constant.Search.KEYWORD,mKeyword)
                                .withInt(Constant.Search.SEARCH_TYPE, Constant.Search.SEARCH_GROUP_EXPAND).navigation()
                    }
                }
            }
            SearchMoreModel.TITLE_MORE_CHAT ->{
                if (item is SearchMoreModel) {
                    helper.getView<TextView>(R.id.text_view_title)?.text = item.title
                    helper.itemView.setOnClickListener {
                        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_EXPAND)
                                .withString(Constant.Search.KEYWORD,mKeyword)
                                .withInt(Constant.Search.SEARCH_TYPE, Constant.Search.SEARCH_CHAT_EXPAND).navigation()
                    }
                }
            }
            CONTACT_ITEM_TYPE -> {
                if (item is ContactDataModel) {
                    var lightWord = mKeyword
                    val view = helper.getView<AppTextView>(R.id.app_text_view_name)
                    var name = item.displayName
                    var secondName = ""
                    var secondHeadName = ""
                    if (!TextUtils.isEmpty(item.displayName) && item.displayName.contains(mKeyword)) {
                        name = item.displayName
                        secondName = ""
                    }else if (!TextUtils.isEmpty(item.searchNoteName) &&item.searchNoteName.contains(mKeyword.toUpperCase())) {
                        lightWord = FastPinyin.findWordFromPinyin(mKeyword,item.noteName)
                        name = item.noteName
                        secondName = ""
                    }else if (!TextUtils.isEmpty(item.shortNoteName) &&item.shortNoteName.contains(mKeyword.toUpperCase())) {
                        lightWord = FastPinyin.findWordFromPinyinToFirst(mKeyword,item.noteName)
                        name = item.noteName
                        secondName = ""
                    }else if (!TextUtils.isEmpty(item.shortNickName) &&item.shortNickName.contains(mKeyword.toUpperCase())) {
                        lightWord = FastPinyin.findWordFromPinyinToFirst(mKeyword,item.nickName)
                        name = item.nickName
                        secondName = ""
                    }else if (!TextUtils.isEmpty(item.searchNickName) &&item.searchNickName.contains(mKeyword.toUpperCase())) {
                        lightWord = FastPinyin.findWordFromPinyin(mKeyword,item.nickName)
                        name = item.nickName
                        secondName = ""
                    } else if (!TextUtils.isEmpty(item.nickName) &&item.nickName.contains(mKeyword)) {
                        name = item.displayName
                        secondHeadName = view.context.getString(R.string.string_search_name) + " "
                        secondName = item.nickName
                    } else if (!TextUtils.isEmpty(item.searchPhone) &&item.searchPhone.contains(mKeyword)) {
                        name = item.displayName
                        secondHeadName = view.context.getString(R.string.string_search_phone) + " "
                        secondName = item.phone
                    } else if (!TextUtils.isEmpty(item.identify) &&item.identify.contains(mKeyword)) {
                        name = item.displayName
                        secondHeadName = view.context.getString(R.string.string_search_68) + " "
                        secondName = item.identify
                    } else {
                        secondName = ""
                    }

                    try {
                        helper.getView<AppTextView>(R.id.app_text_view_name)?.text = StringUtil.setHitTextColor(lightWord, name)
                        helper.getView<TextView>(R.id.app_head_name)?.text = secondHeadName
                        helper.getView<AppTextView>(R.id.app_text_view_name_2)?.text = StringUtil.setHitTextColor(lightWord, secondName)
                        helper.setGone(R.id.layout_other, !TextUtils.isEmpty(secondName))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    if (item.uid == framework.telegram.message.bridge.Constant.Common.FILE_TRANSFER_UID){
                        helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(UriUtils.newWithResourceId(R.drawable.transmission_assistant))
                    }else{
                        helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item.icon)
                    }

                    helper.itemView.setOnClickListener {
                        ARouter.getInstance().build(ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY).withLong("targetUid", item.uid).navigation()
                    }
                }
            }
            SEARCH_ITEM_GROUP -> {
                if (item is GroupInfoModel) {
                    var lightWord = mKeyword
                    if (!TextUtils.isEmpty(item.searchName) && item.searchName.contains(mKeyword.toUpperCase())) {
                        lightWord = FastPinyin.findWordFromPinyin(mKeyword,item.name)
                    }else if (!TextUtils.isEmpty(item.shortName) &&item.shortName.contains(mKeyword.toUpperCase())) {
                        lightWord = FastPinyin.findWordFromPinyin(mKeyword,item.name)
                    }

                    val name = item.name
                    helper.getView<AppTextView>(R.id.app_text_view_name)?.text = StringUtil.setHitTextColor(lightWord,name)
                    helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item.pic)
                    helper.itemView.setOnClickListener {
                        ARouter.getInstance().build(ROUNTE_MSG_GROUP_CHAT_ACTIVITY)
                                .withLong("targetGid", item.groupId).navigation()
                    }
                }
            }
            CHAT_TYPE_GROUP->{
                if (item is ChatModel) {
                    val name = item.chaterName
                    helper.getView<AppTextView>(R.id.app_text_view_name)?.text = StringUtil.setHitTextColor(mKeyword,name)
                    helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item.chaterIcon)
                    helper.itemView.setOnClickListener {
                        ARouter.getInstance().build(ROUNTE_MSG_GROUP_CHAT_ACTIVITY)
                                .withLong("targetGid", item.chaterId).navigation()
                    }
                }
            }
            SEARCH_MERGE_CHAT -> {
                if (item is SearchMergeChatModel) {
                    val textView = helper.getView<AppTextView>(R.id.app_text_view_content)
                    if(item.matchCount>1){
                        val str = SpannableStringBuilder(item.matchCount.toString() +" "+ textView.context.getString(R.string.string_search_match1))
                        str.append(StringUtil.setHitTextColor(mKeyword, mKeyword))
                        str.append(SpannableString(textView.context.getString(R.string.string_search_match2)))
                        textView.text = str
                    }else{
                        val head = when(item.type){
                            MessageModel.MESSAGE_TYPE_NOTICE ->{
                                textView.context.getString(R.string.group_of_announcement_sign)
                            }
                            MessageModel.MESSAGE_TYPE_TEXT ->{
                                ""
                            }
                            MessageModel.MESSAGE_TYPE_FILE ->{
                                textView.context.getString(R.string.file_sign)
                            }
                            MessageModel.MESSAGE_TYPE_LOCATION ->{
                                textView.context.getString(R.string.location_sign)
                            }else->{ ""
                            }
                        }
                        val spannable = SpannableStringBuilder(head)
                        var content  = item.chatContent.replace("\n","")
                        val index = content.indexOf(mKeyword)
                        if (index>10){
                            content = content.substring(index - 10)
                            content = "...$content"
                        }
                        spannable.append(StringUtil.setHitTextColor(mKeyword, content))
                        textView.text = spannable
                    }

                    if (mCacheIcon[item.indexId] != null) {
                        if (item.indexId > 0){//联系人
                            helper.getView<AppImageView>(R.id.image_view_icon).visibility = View.VISIBLE
                            helper.getView<AppImageView>(R.id.group_view_icon).visibility = View.GONE
                            helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(mCacheIcon[item.indexId])
                        }else{
                            helper.getView<AppImageView>(R.id.group_view_icon).visibility = View.VISIBLE
                            helper.getView<AppImageView>(R.id.image_view_icon).visibility = View.GONE
                            helper.getView<AppImageView>(R.id.group_view_icon)?.setImageURI(mCacheIcon[item.indexId])
                        }
                        helper.getView<AppTextView>(R.id.app_text_view_name)?.text = mCacheName[item.indexId]
                    } else {
                        helper.getView<AppTextView>(R.id.app_text_view_name)?.text = ""
                        helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI("")
                        helper.getView<AppImageView>(R.id.group_view_icon)?.setImageURI("")

                        getIconAndName(item.indexId,  helper.position)
                    }

                    helper.itemView.setOnClickListener {
                        var targetName = ""
                        if (mCacheName[item.indexId]!=null){
                            targetName = mCacheName[item.indexId]?:""
                        }
                        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_CHAT_EXPAND)
                                .withString(Constant.Search.KEYWORD, mKeyword)
                                .withBoolean(SEARCH_BACK,true)
                                .withInt(Constant.Search.SEARCH_MATCH_COUNT,item.matchCount)
                                .withString(Constant.Search.TARGET_NAME,targetName)
                                .withLong(Constant.Search.INDEX_ID,item.indexId).navigation()
                    }
                }
            }
        }
    }

    private fun getIconAndName(indexId: Long,  position: Int) {//小于0是群id，大与零是用户id
        val realTargetId = abs(indexId)
        if (indexId > 0) {
            ArouterServiceManager.contactService.getContactInfo(null, realTargetId, { contactInfo, _ ->
                mCacheIcon[indexId] = contactInfo.icon
                mCacheName[indexId] = contactInfo.displayName
                ThreadUtils.runOnUIThread {
                    notifyItemChanged(position)
                }
            })
        } else if (indexId < 0 ) {
            ArouterServiceManager.groupService.getGroupInfo(null, realTargetId, { groupInfo, _ ->
                mCacheIcon[indexId] = groupInfo.pic
                mCacheName[indexId] = groupInfo.name
                ThreadUtils.runOnUIThread {
                    notifyItemChanged(position)
                }
            })
        }
    }
}

