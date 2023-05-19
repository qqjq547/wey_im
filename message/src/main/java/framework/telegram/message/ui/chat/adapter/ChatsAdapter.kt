package framework.telegram.message.ui.chat.adapter

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.*
import com.chad.library.adapter.base.BaseViewHolder
import com.facebook.common.util.UriUtil
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.FireStatus
import framework.telegram.message.bridge.event.FireStatusChangeEvent
import framework.telegram.message.bridge.event.MessageSendStatusEvent
import framework.telegram.message.bridge.event.OnlineStatusChangeEvent
import framework.telegram.message.controller.MessageController
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.manager.MessagesManager
import framework.telegram.message.sp.CommonPref
import framework.telegram.message.ui.chat.ChatCacheModel
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.TimeUtils
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView
import framework.telegram.ui.utils.ScreenUtils
import io.reactivex.Observable
import java.util.concurrent.atomic.AtomicBoolean


class ChatsAdapter(val observable: Observable<ActivityEvent>?) :
    AppBaseMultiItemQuickAdapter<ChatCacheModel, BaseViewHolder>(null) {


    init {
        addItemType(ChatModel.CHAT_TYPE_PVT, R.layout.msg_chat_history_pvt_item)
        addItemType(ChatModel.CHAT_TYPE_GROUP, R.layout.msg_chat_history_group_item)
        addItemType(ChatModel.CHAT_TYPE_GROUP_NOTIFY, R.layout.msg_chat_history_pvt_item)
    }

    private val mMineUid by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
    }

    private val mCheckedMessageList by lazy { LinkedHashMap<String, ChatModel>() }

    private var mCheckedMessageListener: ((Int) -> Unit)? = null

    var isMultiCheckMode = false

    fun setCheckable(msg: ChatModel, checkedMessageListener: ((Int) -> Unit)? = null) {
        isMultiCheckMode = true
        mCheckedMessageList.clear()
        mCheckedMessageList["${msg.chaterType}_${msg.chaterId}"] = msg.copyChat()
        mCheckedMessageListener = checkedMessageListener
        mCheckedMessageListener?.invoke(mCheckedMessageList.size)
        notifyDataSetChanged()
    }

    fun setUnCheckable() {
        isMultiCheckMode = false
        mCheckedMessageList.clear()
//        mCheckedMessageListener?.invoke(mCheckedMessageList.size, false)
        mCheckedMessageListener = null
        notifyDataSetChanged()
    }

    fun setAllChecked(isChecked: Boolean): Int {
        if (isChecked) {
            mCheckedMessageList.clear()
            data.forEach { msgModel ->
                mCheckedMessageList["${msgModel.chatModel.chaterType}_${msgModel.chatModel.chaterId}"] =
                    msgModel.chatModel.copyChat()
            }
            notifyDataSetChanged()
        } else {
            mCheckedMessageList.clear()
            notifyDataSetChanged()
        }

        return mCheckedMessageList.size
    }

    fun getCheckableMessages(): ArrayList<ChatModel> {
        return ArrayList(mCheckedMessageList.values)
    }

    fun hasUnread(): Boolean {
        var hasUnread = false
        mCheckedMessageList.forEach {
            if (it.value.unReadCount > 0) {
                hasUnread = true
                return@forEach
            }
        }
        return hasUnread
    }

    private val mOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            val msgModel = buttonView.tag as ChatModel
            if (!isChecked) {
                mCheckedMessageList.remove("${msgModel.chaterType}_${msgModel.chaterId}")
            } else {
                mCheckedMessageList["${msgModel.chaterType}_${msgModel.chaterId}"] =
                    msgModel.copyChat()
            }

            mCheckedMessageListener?.invoke(mCheckedMessageList.size)
        }

    private fun bindCheckBox(helper: BaseViewHolder, chatModel: ChatModel) {
        val checkbox = helper.getView<CheckBox>(R.id.check_box_msg)
        if (checkbox != null && isMultiCheckMode) {
            checkbox.visibility = View.VISIBLE
            checkbox.tag = chatModel.copyChat()
            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked =
                mCheckedMessageList.containsKey("${chatModel.chaterType}_${chatModel.chaterId}")
            checkbox.setOnCheckedChangeListener(mOnCheckedChangeListener)
        } else {
            checkbox?.visibility = View.GONE
        }
    }

    override fun convert(helper: BaseViewHolder, content: ChatCacheModel?) {
        if (!BaseApp.IS_JENKINS_BUILD) Log.i(
            this.javaClass.simpleName,
            "convert - update" + content?.chatModel?.lastMsg
        )
        if (content == null) {
            return
        }

        helper.addOnClickListener(R.id.layout_root_view)
        helper.addOnLongClickListener(R.id.layout_root_view)
        bindView(helper, content)
        bindCheckBox(helper, content.chatModel)
    }

    override fun convertPayloads(
        helper: BaseViewHolder,
        item: ChatCacheModel,
        payloads: List<Any>
    ) {
        if (!BaseApp.IS_JENKINS_BUILD) Log.i(this.javaClass.simpleName, "convertPayloads - update")
        for (p in payloads) {
            // 如果更新状态则同时更新消息
            bindView(helper, item)
            bindCheckBox(helper, item.chatModel)
        }
    }

    private fun bindView(helper: BaseViewHolder, content: ChatCacheModel) {
        showIsTop(helper, content)

        showUnReadCount(helper, content)

        if (content.chatModel.chaterType == ChatModel.CHAT_TYPE_GROUP_NOTIFY) {
            helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(
                UriUtil.getUriForResourceId(framework.ideas.common.R.drawable.common_contacts_icon_group)
                    .toString()
            )
        } else {
            helper.getView<AppImageView>(R.id.image_view_icon)
                .setImageURI(UriUtils.parseUri(content.chatModel.chaterIcon))
        }

        helper.setText(R.id.app_text_view_name, content.chatModel.chaterName)
        helper.getView<AppTextView>(R.id.app_text_view_name).maxWidth =
            ScreenUtils.getScreenWidth(BaseApp.app) - ScreenUtils.dp2px(BaseApp.app, 210.0f)
        val textViewName = helper.getView<TextView>(R.id.app_text_view_name)
        val layoutParams = textViewName.layoutParams
        if (content.chatModel.chaterType == ChatModel.CHAT_TYPE_PVT && content.chatModel.chaterId < Constant.Common.SYSTEM_USER_MAX_UID) {
            if (layoutParams is LinearLayout.LayoutParams) {
                layoutParams.weight = 0f
            }
            framework.telegram.ui.widget.ViewUtils.setRightDrawable(
                textViewName,
                R.drawable.common_official_flag
            )
        } else {
            if (layoutParams is LinearLayout.LayoutParams) {
                layoutParams.weight = 1f
            }
            framework.telegram.ui.widget.ViewUtils.setRightDrawable(textViewName, null)
        }

        if (content.chatModel.bfDisturb == 1) {
            helper.getView<ImageView>(R.id.image_view_disturb).visibility = View.VISIBLE
        } else {
            helper.getView<ImageView>(R.id.image_view_disturb).visibility = View.GONE
        }

        helper.setText(
            R.id.text_view_time,
            TimeUtils.timeFormatToChat(BaseApp.app, content.chatModel.lastMsgTime)
        )

        val hasDraft = AtomicBoolean(false)
        if (content.chatModel.atMeCount > 0) {
            showPvtAtMeCount(helper, content)
        } else {
            showGroupNoticeIfExist(helper, content, hasDraft)
        }

        // 显示隐藏在线状态
        showOnlineStatusIfExist(helper, content)

        // 显示阅后既焚的标识showPvtAtMeCount
        showFireStatusIfExist(helper, content)

        // 显示隐藏发送状态
        showSendStatusIfExist(helper, content, hasDraft)
    }

    /**
     * 显示私聊对话@我的
     */
    private fun showPvtAtMeCount(helper: BaseViewHolder, content: ChatCacheModel) {
        try {
            val lastMsg = content.chatModel.lastMsg
            val builder = SpannableStringBuilder(
                String.format(
                    mContext.getString(R.string.some_one_at_me_sign_mat),
                    lastMsg
                )
            )
            builder.setSpan(
                ForegroundColorSpan(Color.RED),
                0,
                mContext.getString(R.string.some_one_at_me_sign).length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            helper.setText(R.id.app_text_view_last_msg, builder)
        } catch (e: Exception) {
            helper.setText(R.id.app_text_view_last_msg, content.chatModel.lastMsg)
        }
    }

    /**
     * 显示置顶ChatFragment
     */
    private fun showIsTop(helper: BaseViewHolder, content: ChatCacheModel) {
        if (content.chatModel.isTop == 1) {
            helper.setBackgroundRes(R.id.layout_root_view, R.color.edeff2)
        } else {
            helper.setBackgroundRes(R.id.layout_root_view, R.color.white)
        }
    }

    /**
     * 显示未读数量
     */
    private fun showUnReadCount(helper: BaseViewHolder, content: ChatCacheModel) {
        if (content.chatModel.unReadCount <= 0) {
            helper.setGone(R.id.text_view_msg_count, false)
            helper.setGone(R.id.text_view_msg_count_disturb, false)
        } else {
            if (content.chatModel.bfDisturb == 1) {
                helper.setText(R.id.text_view_msg_count, "")
                helper.setBackgroundRes(R.id.text_view_msg_count, R.drawable.common_oval_f50d2e_8)
                helper.setGone(R.id.text_view_msg_count, false)
                helper.setGone(R.id.text_view_msg_count_disturb, true)
            } else {
                val countText = when {
                    content.chatModel.unReadCount >= 1000 -> "..."
                    content.chatModel.unReadCount >= 100 -> "99+"
                    else -> "${content.chatModel.unReadCount}"
                }
                helper.setText(R.id.text_view_msg_count, countText)
                helper.setGone(R.id.text_view_msg_count, true)
                helper.setGone(R.id.text_view_msg_count_disturb, false)
            }
        }
    }

    private fun showSendStatusIfExist(
        helper: BaseViewHolder?,
        content: ChatCacheModel,
        hasDraft: AtomicBoolean
    ) {
        val imageView = helper?.getView<ImageView>(R.id.image_view_last_msg_status)
        if (hasDraft.get() || (content.chatModel.chaterType == ChatModel.CHAT_TYPE_PVT && content.chatModel.chaterId < Constant.Common.SYSTEM_USER_MAX_UID)) {
            imageView?.visibility = View.GONE
        } else {
            if (content.msgStatus == -1001) {//别人发的
                imageView?.visibility = View.GONE
            } else if (TextUtils.isEmpty(content.chatModel.lastMsg)) {
                imageView?.visibility = View.GONE
            } else if (content.msgStatus != -1000) {//初始化过
                try {
                    imageView?.setImageResource(content.msgStatus)
                    imageView?.visibility = View.VISIBLE
                } catch (e: Exception) {

                }
            } else {//自己发的,不同状态下的回执
                val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                MessageController.executeChatTransactionAsyncWithResult(
                    content.chatModel.chaterType,
                    myUid,
                    content.chatModel.chaterId,
                    { realm ->
                        MessagesManager.findLastBizMessage(realm)
                    },
                    { msg ->
                        msg?.copyMessage()?.let {
                            EventBus.publishEvent(MessageSendStatusEvent(it))
                        }
                    })
                imageView?.visibility = View.GONE
            }
        }
    }

    private fun showOnlineStatusIfExist(helper: BaseViewHolder?, content: ChatCacheModel) {
        val online = helper?.getView<View>(R.id.text_view_online_status_point)
        if (content.chatModel.chaterType == ChatModel.CHAT_TYPE_PVT && content.chatModel.chaterId != Constant.Common.FILE_TRANSFER_UID) {
            if (content.isOnlineStatus != null) {
                if (content.isOnlineStatus == true) {
                    online?.visibility = View.VISIBLE
                } else {
                    online?.visibility = View.GONE
                }
            } else {
                ArouterServiceManager.contactService.getContactInfo(
                    observable,
                    content.chatModel.chaterId,
                    { contactDataModel, _ ->
                        EventBus.publishEvent(
                            OnlineStatusChangeEvent(
                                content.chatModel.chaterId,
                                contactDataModel.isOnlineStatus
                            )
                        )
                    })
            }
        } else {
            // 隐藏在线状态
            online?.visibility = View.GONE
        }
    }

    private fun showFireStatusIfExist(helper: BaseViewHolder?, content: ChatCacheModel) {
        val fire = helper?.getView<View>(R.id.image_view_fire)
        when (content.chatModel.chaterType) {
            ChatModel.CHAT_TYPE_PVT -> {
                if (content.isFireStatus != null) {
                    if (content.isFireStatus == true) {
                        fire?.visibility = View.VISIBLE
                    } else {
                        fire?.visibility = View.GONE
                    }
                } else {
                    ArouterServiceManager.contactService.getContactInfo(
                        observable,
                        content.chatModel.chaterId,
                        { contactInfoModel, _ ->
                            EventBus.publishEvent(
                                FireStatusChangeEvent(
                                    mutableListOf(
                                        FireStatus(
                                            content.chatModel.chaterId,
                                            contactInfoModel.isBfReadCancel
                                        )
                                    )
                                )
                            )
                        })
                }
            }
            ChatModel.CHAT_TYPE_GROUP -> {
                if (content.isFireStatus != null) {
                    if (content.isFireStatus == true) {
                        fire?.visibility = View.VISIBLE
                    } else {
                        fire?.visibility = View.GONE
                    }
                } else {
                    ArouterServiceManager.groupService.getGroupInfo(
                        observable,
                        content.chatModel.chaterId,
                        { groupInfoModel, _ ->
                            EventBus.publishEvent(
                                FireStatusChangeEvent(
                                    mutableListOf(
                                        FireStatus(
                                            content.chatModel.chaterId,
                                            groupInfoModel.bfGroupReadCancel
                                        )
                                    )
                                )
                            )
                        })
                }
            }
            else -> {
                fire?.visibility = View.GONE
            }
        }
    }

    private fun showGroupNoticeIfExist(
        helper: BaseViewHolder?,
        content: ChatCacheModel,
        hasDraft: AtomicBoolean
    ) {
        val lastMsg = content.chatModel.lastMsg
        if (content.chatModel.chaterType == ChatModel.CHAT_TYPE_GROUP) {
            val commonPref = SharePreferencesStorage.createStorageInstance(
                CommonPref::class.java,
                "group_notice_${mMineUid}_${content.chatModel.chaterId}"
            )
            val hasNewGroupNotice: Boolean? = commonPref.getHasNewGroupNotice()
            if (hasNewGroupNotice == true) {
                val builder = SpannableStringBuilder(
                    String.format(
                        mContext.getString(R.string.there_is_a_new_announcement_sign_mat),
                        lastMsg
                    )
                )
                builder.setSpan(
                    ForegroundColorSpan(Color.RED),
                    0,
                    mContext.getString(R.string.there_is_a_new_announcement_sign).length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                helper?.setText(R.id.app_text_view_last_msg, builder)
            } else {
                showDraftIfExist(helper, content, lastMsg, hasDraft)
            }
        } else {
            showDraftIfExist(helper, content, lastMsg, hasDraft)
        }
    }

    private fun showDraftIfExist(
        helper: BaseViewHolder?,
        content: ChatCacheModel?,
        lastMsg: String?,
        hasDraft: AtomicBoolean? = null
    ) {
        if (content != null) {
            val audioDraft = getDraftCommonPref(
                content.chatModel.chaterType,
                content.chatModel.chaterId
            ).getAudioRecordDraft()
            val textDraft = getDraftCommonPref(
                content.chatModel.chaterType,
                content.chatModel.chaterId
            ).getTextDraft()
            if (!TextUtils.isEmpty(audioDraft)) {
                val builder =
                    SpannableStringBuilder(mContext.getString(R.string.draft_sign_voice_sign))
                builder.setSpan(
                    ForegroundColorSpan(Color.RED),
                    0,
                    mContext.getString(R.string.draft_sign).length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                helper?.setText(R.id.app_text_view_last_msg, builder)
                hasDraft?.set(true)
            } else if (!TextUtils.isEmpty(textDraft)) {
                val builder = SpannableStringBuilder(
                    String.format(
                        mContext.getString(R.string.draft_sign_mat),
                        textDraft
                    )
                )
                builder.setSpan(
                    ForegroundColorSpan(Color.RED),
                    0,
                    mContext.getString(R.string.draft_sign).length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                helper?.setText(R.id.app_text_view_last_msg, builder)
                hasDraft?.set(true)
            } else {
                if (content.chatModel.bfDisturb == 1 && content.chatModel.unReadCount > 1) {
                    helper?.setText(
                        R.id.app_text_view_last_msg,
                        String.format(
                            mContext.getString(R.string.strip),
                            content.chatModel.unReadCount,
                            lastMsg
                        )
                    )
                } else {
                    helper?.setText(R.id.app_text_view_last_msg, lastMsg)
                }
            }
        }
    }

    private fun getDraftCommonPref(chatType: Int, targetId: Long): CommonPref {
        return SharePreferencesStorage.createStorageInstance(
            CommonPref::class.java,
            when (chatType) {
                ChatModel.CHAT_TYPE_PVT -> "pvt_draft_${mMineUid}_$targetId"
                ChatModel.CHAT_TYPE_GROUP -> "group_draft_${mMineUid}_$targetId"
                else -> "error_draft_no_storage_name"
            }
        )
    }
}