package framework.telegram.business.ui.search.expand.chatExpand

import android.widget.TextView
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.common.SearchChatModel
import framework.ideas.common.model.common.SearchChatModel.SEARCH_CHAT
import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant.Search.SEARCH_ITEM_TITLE
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.search.StringUtil
import framework.telegram.message.bridge.Constant
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.TimeUtils
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView
import kotlin.math.abs

/**
 * Created by lzh on 19-5-21.
 * INFO:通讯录 adapter
 */
class SearchChatAdapter : BaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(null) {

    init {
        addItemType(SEARCH_CHAT, R.layout.bus_search_chat_item_2)
        addItemType(SEARCH_ITEM_TITLE, R.layout.bus_title3)
    }

    private val mCacheIcon = mutableMapOf<Long, String>()
    private val mCacheName = mutableMapOf<Long, String>()

    private var mKeyword = ""

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    fun setKeyword(keyword:String){
        mKeyword = keyword
    }


    override fun convert(helper: BaseViewHolder, item: MultiItemEntity?) {
        if (item is SearchChatModel) {
            var content  = item.chatContent.replace("\n","")
            val index = content.indexOf(mKeyword)
            if (index>10){
                content = content.substring(index - 10)
                content = "...$content"
            }
            helper.getView<AppTextView>(R.id.app_text_view_content)?.text = StringUtil.setHitTextColor(mKeyword, content)
            if (mCacheIcon[item.senderId] != null) {
                helper.getView<AppTextView>(R.id.app_text_view_name)?.text = mCacheName[item.senderId]
                helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(mCacheIcon[item.senderId])
            } else {
                helper.getView<AppTextView>(R.id.app_text_view_name)?.text = ""
                helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI("")
                getIconAndName(item.indexId,item.senderId,item.msgId,helper.position)
            }
            helper.setText(R.id.text_view_time, TimeUtils.timeFormatToChat(BaseApp.app, item.msgTime))
            helper.itemView.setOnClickListener {
                if (item.indexId >0 ){
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY)
                            .withLong("localMsgId", item.msgLocalId)
                            .withLong("targetUid", item.indexId).navigation()
                }else{
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_GROUP_CHAT_ACTIVITY)
                            .withLong("localMsgId", item.msgLocalId)
                            .withLong("targetGid",abs(item.indexId)).navigation()
                }
            }
        }else if (item is TitleModel){
            val textView = helper.getView<TextView>(R.id.text_view_title)
            textView.text = item.title
        }
    }

    private fun getIconAndName(indexId: Long,senderId:Long,msgId:Long,  position: Int) {//小于0是群id，大与零是用户id
        val realTargetId = abs(indexId)
        if (indexId > 0) {
            ArouterServiceManager.contactService.getContactInfo(null, senderId, { contactInfo, _ ->
                mCacheIcon[senderId] = contactInfo.icon
                mCacheName[senderId] = contactInfo.displayName
                ThreadUtils.runOnUIThread {
                    notifyItemChanged(position)
                }
            })
        } else if (indexId < 0 ) {
            ArouterServiceManager.groupService.getGroupMembersInfoByCache(realTargetId, mutableListOf(senderId), { groupMembers ->
                if (groupMembers.isNotEmpty()){
                    val groupMember = groupMembers[0]
                    mCacheIcon[senderId] = groupMember.icon
                    mCacheName[senderId] = groupMember.displayName
                    ThreadUtils.runOnUIThread {
                        notifyItemChanged(position)
                    }
                }else{
                    ArouterServiceManager.messageService.findChatMsg(ChatModel.CHAT_TYPE_GROUP,mMineUid,realTargetId,msgId,{message->
                        if (message != null){
                            mCacheIcon[senderId] = message.ownerIcon
                            mCacheName[senderId] = message.ownerName
                        }
                    })
                }

            })
        }
    }
}

