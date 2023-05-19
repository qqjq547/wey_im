package framework.telegram.message.ui.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import android.text.format.Formatter
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.facebook.common.util.UriUtil
import com.im.pb.IMPB
import com.qmuiteam.qmui.widget.QMUIProgressBar
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.group.GroupMemberModel
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.ChatModel.CHAT_TYPE_PVT
import framework.ideas.common.model.im.MessageModel
import framework.ideas.common.widget.PointerCountDownView
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.Constant.Common.MAP_HTTP_HOST
import framework.telegram.message.bridge.event.SnapMessageEvent
import framework.telegram.message.controller.AudioPlayController
import framework.telegram.message.controller.DownloadAttachmentController
import framework.telegram.message.controller.MessageController
import framework.telegram.message.controller.UploadAttachmentController
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.manager.SendMessageManager
import framework.telegram.message.ui.pvt.PrivateChatActivity
import framework.telegram.message.ui.widget.MessageInputView
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.TimeUtils
import framework.telegram.ui.emoji.EmojiTextView
import framework.telegram.ui.filepicker.utils.FileResUtils
import framework.telegram.ui.glide.BlurTransformation
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.utils.ScreenUtils
import framework.telegram.ui.utils.SizeUtils
import framework.telegram.ui.utils.UriUtils
import java.io.File
import java.lang.ref.WeakReference
import kotlin.math.roundToLong

@SuppressLint("UseSparseArrays")
class MessageAdapter(
    private val layoutManager: LinearLayoutManager,
    private val activityRef: WeakReference<Activity>,
    private val messageInputView: MessageInputView?,
    val chatType: Int, val targetId: Long,
    var isSpeakerphoneOn: Boolean = false
) : BaseMultiItemQuickAdapter<MessageModel, BaseViewHolder>(null),
    DownloadAttachmentController.DownloadAttachmentListener,
    UploadAttachmentController.UploadAttachmentListener {

    init {
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_TEXT, R.layout.msg_chat_item_text_mine)
        addItemType(MessageModel.LOCAL_TYPE_OTHER_TEXT, R.layout.msg_chat_item_text_other)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_VOICE, R.layout.msg_chat_item_voice_mine)
        addItemType(MessageModel.LOCAL_TYPE_OTHER_VOICE, R.layout.msg_chat_item_voice_other)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_IMAGE, R.layout.msg_chat_item_image_mine)
        addItemType(MessageModel.LOCAL_TYPE_OTHER_IMAGE, R.layout.msg_chat_item_image_other)
        addItemType(
            MessageModel.LOCAL_TYPE_MYSELF_DYNAMIC_IMAGE,
            R.layout.msg_chat_item_dynamic_image_mine
        )
        addItemType(
            MessageModel.LOCAL_TYPE_OTHER_DYNAMIC_IMAGE,
            R.layout.msg_chat_item_dynamic_image_other
        )
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_VIDEO, R.layout.msg_chat_item_video_mine)
        addItemType(MessageModel.LOCAL_TYPE_OTHER_VIDEO, R.layout.msg_chat_item_video_other)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_STREAM_MEDIA, R.layout.msg_chat_item_stream_mine)
        addItemType(MessageModel.LOCAL_TYPE_OTHER_STREAM_MEDIA, R.layout.msg_chat_item_stream_other)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_NAMECARD, R.layout.msg_chat_item_namecard_mine)
        addItemType(MessageModel.LOCAL_TYPE_OTHER_NAMECARD, R.layout.msg_chat_item_namecard_other)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_LOCATION, R.layout.msg_chat_item_location_mine)
        addItemType(MessageModel.LOCAL_TYPE_OTHER_LOCATION, R.layout.msg_chat_item_location_other)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_FILE, R.layout.msg_chat_item_file_mine)
        addItemType(MessageModel.LOCAL_TYPE_OTHER_FILE, R.layout.msg_chat_item_file_other)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_NOTICE, R.layout.msg_chat_item_notice_mine)
        addItemType(MessageModel.LOCAL_TYPE_OTHER_NOTICE, R.layout.msg_chat_item_notice_other)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_RECALL, R.layout.msg_chat_item_recall_mine)
        addItemType(MessageModel.LOCAL_TYPE_OTHER_RECALL, R.layout.msg_chat_item_recall_other)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_UNKNOW, R.layout.msg_chat_item_unknow_mine)
        addItemType(MessageModel.LOCAL_TYPE_OTHER_UNKNOW, R.layout.msg_chat_item_unknow_other)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_UNDECRYPT, R.layout.msg_chat_item_undecrypt_mine)
        addItemType(MessageModel.LOCAL_TYPE_OTHER_UNDECRYPT, R.layout.msg_chat_item_undecrypt_other)
        addItemType(MessageModel.LOCAL_TYPE_TIP, R.layout.msg_chat_item_tip)
    }

    private val mTextMsgagnificationTimes by lazy {
        //放大倍数
        ArouterServiceManager.settingService.getFontSize()
    }

    private val mMineUid by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
    }

    val mMessageOwnerList by lazy { HashMap<Long, Any>() }

    private val mCheckedMessageList by lazy { mutableListOf<MessageModel>() }

    private var isClickable = false

    private var mCheckedMessageListener: ((Int) -> Unit)? = null

    var showReceiptStatus: Boolean = true

    val audioPlayController by lazy {
        AudioPlayController(
            activityRef,
            this@MessageAdapter,
            layoutManager,
            messageInputView,
            isSpeakerphoneOn,
            chatType
        )
    }

    var chaterId = 0L

    fun setMessageOwnerList(map: HashMap<Long, Any>) {
        mMessageOwnerList.clear()
        mMessageOwnerList.putAll(map)
        notifyDataSetChanged()
    }

    fun getBindedRecyclerView(): RecyclerView {
        return recyclerView
    }

    fun setCheckable(msg: MessageModel, checkedMessageListener: ((Int) -> Unit)? = null) {
        isClickable = true
        mCheckedMessageList.clear()
        mCheckedMessageList.add(msg.copyMessage())
        mCheckedMessageListener = checkedMessageListener
        mCheckedMessageListener?.invoke(mCheckedMessageList.size)
        notifyDataSetChanged()
    }

    fun setUnCheckable() {
        isClickable = false
        mCheckedMessageList.clear()
        mCheckedMessageListener?.invoke(mCheckedMessageList.size)
        mCheckedMessageListener = null
        notifyDataSetChanged()
    }

    fun getCheckableMessages(): ArrayList<MessageModel> {
        return ArrayList(mCheckedMessageList)
    }

    override fun downloadStart(
        downloadUrl: String,
        chatType: Int,
        targetId: Long,
        msgLocalId: Long
    ) {
        notifyItemChangedWithReset(chatType, targetId, msgLocalId)
    }

    override fun downloadProgress(
        downloadUrl: String,
        chatType: Int,
        targetId: Long,
        msgLocalId: Long,
        percent: Double,
        currentOffset: Long,
        totalLength: Long
    ) {
        notifyItemChangedWithProgress(chatType, targetId, msgLocalId, percent, true)
    }

    override fun downloadCancel(
        downloadUrl: String,
        chatType: Int,
        targetId: Long,
        msgLocalId: Long
    ) {
//        notifyItemChangedPause(chatType, targetId, msgLocalId)
        notifyItemChangedWithReset(chatType, targetId, msgLocalId)
    }

    override fun downloadComplete(
        downloadUrl: String,
        chatType: Int,
        targetId: Long,
        msgLocalId: Long,
        file: File
    ) {
        notifyItemChangedWithReset(chatType, targetId, msgLocalId)
    }

    override fun downloadFail(
        downloadUrl: String,
        chatType: Int,
        targetId: Long,
        msgLocalId: Long
    ) {
        notifyItemChangedWithReset(chatType, targetId, msgLocalId)
    }

    override fun uploadStart(chatType: Int, targetId: Long, msgLocalId: Long) {
        notifyItemChangedWithReset(chatType, targetId, msgLocalId)
    }

    override fun uploadProgress(
        chatType: Int,
        targetId: Long,
        msgLocalId: Long,
        percent: Double,
        currentOffset: Long,
        totalLength: Long
    ) {
        notifyItemChangedWithProgress(chatType, targetId, msgLocalId, percent, false)
    }

    override fun uploadCancel(chatType: Int, targetId: Long, msgLocalId: Long) {
        notifyItemChangedWithReset(chatType, targetId, msgLocalId)
    }

    override fun uploadComplete(chatType: Int, targetId: Long, msgLocalId: Long) {
        notifyItemChangedWithReset(chatType, targetId, msgLocalId)
    }

    override fun uploadFail(chatType: Int, targetId: Long, msgLocalId: Long) {
        notifyItemChangedWithReset(chatType, targetId, msgLocalId)
    }

    private fun notifyItemChangedWithProgress(
        chatType: Int,
        targetId: Long,
        msgLocalId: Long,
        percent: Double,
        isDownload: Boolean
    ) {
        if (this.chatType == chatType && chaterId == targetId) {
            val first = layoutManager.findFirstVisibleItemPosition()
            val last = layoutManager.findLastVisibleItemPosition()
            for (adapterPosition in last downTo first) {
                val dataPosition = adapterPosition - headerLayoutCount
                if (dataPosition >= 0 && dataPosition < data.size) {
                    val data = data[dataPosition]
                    if (data.id == msgLocalId) {
                        val holder =
                            getBindedRecyclerView().findViewHolderForAdapterPosition(adapterPosition)
                        if (holder is BaseViewHolder) {
                            updateLoadProgress(holder, percent, data, isDownload)
                        }
                    }
                }
            }
        }
    }

    private fun notifyItemChangedWithReset(chatType: Int, targetId: Long, msgLocalId: Long) {
        if (this.chatType == chatType && chaterId == targetId) {
            val first = layoutManager.findFirstVisibleItemPosition()
            val last = layoutManager.findLastVisibleItemPosition()
            for (adapterPosition in last downTo first) {
                val dataPosition = adapterPosition - headerLayoutCount
                if (dataPosition >= 0) {
                    val data = data[dataPosition]
                    if (data.id == msgLocalId) {
                        notifyItemChanged(adapterPosition)
                    }
                }
            }
        }
    }

    private fun notifyItemChangedPause(chatType: Int, targetId: Long, msgLocalId: Long) {
        if (this.chatType == chatType && chaterId == targetId) {
            val first = layoutManager.findFirstVisibleItemPosition()
            val last = layoutManager.findLastVisibleItemPosition()
            for (adapterPosition in last downTo first) {
                val dataPosition = adapterPosition - headerLayoutCount
                if (dataPosition >= 0) {
                    val data = data[dataPosition]
                    if (data.id == msgLocalId) {
                        val holder =
                            getBindedRecyclerView().findViewHolderForAdapterPosition(dataPosition)
                        holder?.let {
                            val bar = it.itemView.findViewById<QMUIProgressBar>(R.id.progress_bar)
                            bar.tag = bar.progress
                            it.itemView.findViewById<ImageView>(R.id.image_view_pause)
                                .setImageResource(R.drawable.msg_icon_readed)
                        }
                    }
                }
            }
        }
    }

    fun notifyItemExpireStatus() {
        val first = layoutManager.findFirstVisibleItemPosition()
        val last = layoutManager.findLastVisibleItemPosition()
        val floatMenu = (activityRef.get() as PrivateChatActivity).floatMenu
        val findTag = floatMenu?.tag
        var finded = false
        for (adapterPosition in last downTo first) {
            val dataPosition = adapterPosition - headerLayoutCount
            if (dataPosition >= 0) {
                val data = data.getOrNull(dataPosition)
                data?.let {
                    if (data.snapchatTime > 0 && data.expireTime > 0L && chatType == CHAT_TYPE_PVT) {
                        if (System.currentTimeMillis() < data.expireTime) {
                            // 未焚毁
                            val holder = getBindedRecyclerView().findViewHolderForAdapterPosition(
                                adapterPosition
                            )
                            if (holder is BaseViewHolder) {
                                if (data.type == MessageModel.MESSAGE_TYPE_IMAGE
                                    || data.type == MessageModel.MESSAGE_TYPE_VIDEO
                                ) {
                                    holder.getView<PointerCountDownView>(R.id.pointer_count_down)
                                        .setCurProgress((data.expireTime - System.currentTimeMillis()).toInt())
                                }
                            }
                        } else {
                            // 已焚毁
                            if (floatMenu != null && data.msgId == findTag) {
                                floatMenu.dismiss()
                            }
                            EventBus.publishEvent(SnapMessageEvent(data.targetId, data.msgId))
                            notifyItemChanged(adapterPosition)
                        }
                    }

                    if (data.msgId == findTag) {
                        finded = true
                    }
                }
            }
        }

        if (!finded) {
            floatMenu?.dismiss()
        }

        val checkedMsgs = ArrayList(mCheckedMessageList)
        checkedMsgs.forEach {
            if (it.snapchatTime > 0 && it.expireTime > 0L && chatType == CHAT_TYPE_PVT) {
                if (System.currentTimeMillis() > it.expireTime) {
                    // 已焚毁
                    mCheckedMessageList.remove(it)
                }
            }
        }
        if (checkedMsgs.size != mCheckedMessageList.size) {
            mCheckedMessageListener?.invoke(mCheckedMessageList.size)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateLoadProgress(
        helper: BaseViewHolder,
        percent: Double,
        msgModel: MessageModel,
        isDownload: Boolean
    ) {
        val progressBar = helper.getView<QMUIProgressBar>(R.id.progress_bar)
        if (msgModel.itemType == MessageModel.LOCAL_TYPE_OTHER_DYNAMIC_IMAGE ||
            msgModel.itemType == MessageModel.LOCAL_TYPE_MYSELF_DYNAMIC_IMAGE
        ) {
            val minSize =
                if (msgModel.dynamicImageMessageBean.height < msgModel.dynamicImageMessageBean.width) msgModel.dynamicImageMessageBean.height else msgModel.dynamicImageMessageBean.width
            if (minSize <= ScreenUtils.dp2px(BaseApp.app, 80f)) {//gif图小过 52 就不显示进度条
                progressBar.visibility = View.GONE
            } else {
                progressBar.visibility = View.VISIBLE
            }

            progressBar.setProgress((percent * 100.0f).toInt(), false)
            progressBar.maxValue = 100

            val pauseView = helper.getView<View>(R.id.image_view_pause)
            pauseView?.post {
                pauseView.visibility = View.VISIBLE
            }

            //动图的
            val textProgress = helper.getView<TextView>(R.id.text_progress)
            helper.setVisible(R.id.image_view_gif_2, false)
            textProgress?.post {
                if (isDownload) {
                    val totalPercent = (percent * 100).toInt()
                    textProgress.text = "${totalPercent}%"
                }
            }
        } else {
            progressBar?.post {
                progressBar.visibility = View.VISIBLE
                progressBar.setProgress((percent * 100.0f).toInt(), false)
                progressBar.maxValue = 100
            }

            val pauseView = helper.getView<View>(R.id.image_view_pause)
            pauseView?.post {
                pauseView.visibility = View.VISIBLE
            }

            val playView = helper.getView<View>(R.id.image_view_play)
            playView?.post {
                playView.visibility = View.GONE
            }

            val sizeView = helper.getView<TextView>(R.id.text_view_file_size)
            sizeView?.post {
                val size = Formatter.formatFileSize(mContext, msgModel.fileMessageContentBean.size)
                val progressSize = Formatter.formatFileSize(
                    mContext,
                    (msgModel.fileMessageContentBean.size * percent).roundToLong()
                )
                if (isDownload) {
                    sizeView.text =
                        String.format(mContext.getString(R.string.downloading), progressSize, size)
                } else {
                    sizeView.text =
                        String.format(mContext.getString(R.string.on_the_cross), progressSize, size)
                }
            }
        }
    }

    override fun convert(helper: BaseViewHolder, item: MessageModel?) {
        when (helper.itemViewType) {
            MessageModel.LOCAL_TYPE_MYSELF_TEXT -> {
                item?.let {
                    bindTextItem(helper, item)
                    bindRefMessage(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_OTHER_TEXT -> {
                item?.let {
                    bindUserInfoItem(helper, item)
                    bindTextItem(helper, item)
                    bindRefMessage(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_VOICE -> {
                item?.let {
                    bindVoiceItemMine(helper, item)
                    bindRefMessage(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_OTHER_VOICE -> {
                item?.let {
                    bindUserInfoItem(helper, item)
                    bindVoiceItemOther(helper, item)
                    bindRefMessage(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_IMAGE -> {
                item?.let {
                    bindImageItem(helper, item)
                    bindRefMessage(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_OTHER_IMAGE -> {
                item?.let {
                    bindUserInfoItem(helper, item)
                    bindImageItem(helper, item)
                    bindRefMessage(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_DYNAMIC_IMAGE -> {
                item?.let {
                    val isRef = bindRefMessage(helper, item)
                    bindDynamicImageItem(helper, item, isRef)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_OTHER_DYNAMIC_IMAGE -> {
                item?.let {
                    bindUserInfoItem(helper, item)
                    val isRef = bindRefMessage(helper, item)
                    bindDynamicImageItem(helper, item, isRef)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_VIDEO -> {
                item?.let {
                    bindVideoItem(helper, item)
                    bindRefMessage(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_OTHER_VIDEO -> {
                item?.let {
                    bindUserInfoItem(helper, item)
                    bindVideoItem(helper, item)
                    bindRefMessage(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_NAMECARD -> {
                item?.let {
                    bindNameCardItem(helper, item)
                    bindRefMessage(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_OTHER_NAMECARD -> {
                item?.let {
                    bindUserInfoItem(helper, item)
                    bindNameCardItem(helper, item)
                    bindRefMessage(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_LOCATION -> {
                item?.let {
                    bindLocationItem(helper, item)
                    bindRefMessage(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_OTHER_LOCATION -> {
                item?.let {
                    bindUserInfoItem(helper, item)
                    bindLocationItem(helper, item)
                    bindRefMessage(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_FILE -> {
                item?.let {
                    bindFileItem(helper, item)
                    bindRefMessage(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_OTHER_FILE -> {
                item?.let {
                    bindUserInfoItem(helper, item)
                    bindFileItem(helper, item)
                    bindRefMessage(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_NOTICE -> {
                item?.let {
                    bindNoticeItem(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_OTHER_NOTICE -> {
                item?.let {
                    bindUserInfoItem(helper, item)
                    bindNoticeItem(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_STREAM_MEDIA -> {
                item?.let {
                    bindStreamItem(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_OTHER_STREAM_MEDIA -> {
                item?.let {
                    bindUserInfoItem(helper, item)
                    bindStreamItem(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                    bindCheckBox(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_RECALL -> {
                item?.let {
                    bindRecallItem(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_OTHER_RECALL -> {
                item?.let {
                    bindRecallItem(helper, item)
                    bindUserInfoItem(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_TIP -> {
                item?.let {
                    helper.setText(R.id.text_view_msg, it.tipMessageContentBean?.msgTip)
                    bindTimeline(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_UNKNOW -> {
                item?.let {
                    helper.addOnLongClickListener(R.id.text_view)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_OTHER_UNKNOW -> {
                item?.let {
                    helper.addOnLongClickListener(R.id.text_view)
                    bindUserInfoItem(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_UNDECRYPT -> {
                item?.let {
                    helper.addOnClickListener(R.id.text_view)
                    helper.addOnLongClickListener(R.id.text_view)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                }
            }
            MessageModel.LOCAL_TYPE_OTHER_UNDECRYPT -> {
                item?.let {
                    helper.addOnClickListener(R.id.text_view)
                    helper.addOnLongClickListener(R.id.text_view)
                    bindUserInfoItem(helper, item)
                    bindStatus(helper, item)
                    bindTimeline(helper, item)
                }
            }
        }
    }

    private fun bindUserInfoItem(helper: BaseViewHolder, messageModel: MessageModel) {
        if (chatType == ChatModel.CHAT_TYPE_GROUP) {
            helper.getView<View>(R.id.app_image_view_icon).visibility = View.VISIBLE
            helper.getView<View>(R.id.text_view_nickname).visibility = View.VISIBLE
            helper.getView<EmojiTextView>(R.id.text_view_nickname)
                .setEmojiSizeRes(R.dimen.emoji_size_small, false)

            val owner = mMessageOwnerList[messageModel.ownerUid]
            if (owner != null && owner is GroupMemberModel) {
                helper.setText(R.id.text_view_nickname, owner.displayName)
                helper.getView<AppImageView>(R.id.app_image_view_icon).setImageURI(owner.icon)
            } else if (owner != null && owner is ContactDataModel) {
                helper.setText(R.id.text_view_nickname, owner.displayName)
                helper.getView<AppImageView>(R.id.app_image_view_icon).setImageURI(owner.icon)
            } else {
                if (!TextUtils.isEmpty(messageModel.ownerName)) {
                    helper.setText(R.id.text_view_nickname, messageModel.ownerName)
                    helper.getView<AppImageView>(R.id.app_image_view_icon)
                        .setImageURI(messageModel.ownerIcon)
                } else {
                    helper.setText(R.id.text_view_nickname, "${messageModel.ownerUid}")
                    helper.getView<AppImageView>(R.id.app_image_view_icon).setImageURI(Uri.EMPTY)
                }
            }
            helper.addOnClickListener(R.id.app_image_view_icon)
            helper.addOnClickListener(R.id.text_view_nickname)
            helper.addOnLongClickListener(R.id.app_image_view_icon)
        } else {
            helper.getView<View>(R.id.app_image_view_icon).visibility = View.GONE
            helper.getView<View>(R.id.text_view_nickname).visibility = View.GONE
        }
    }

    private fun bindStreamItem(helper: BaseViewHolder, messageModel: MessageModel) {
        helper.getView<TextView>(R.id.text_view)?.let {
            it.textSize = 15 * mTextMsgagnificationTimes
        }
        val content = messageModel.streamMessageContent

        if (content.streamType == 0) {
            helper.setImageResource(
                R.id.image_view_stream_type_icon,
                R.drawable.msg_icon_stream_audio
            )
        } else {
            helper.setImageResource(
                R.id.image_view_stream_type_icon,
                R.drawable.msg_icon_stream_video
            )
        }

        if (content.status == 0) {
            if (content.isSend == 1) {
                helper.setText(R.id.text_view, mContext.getString(R.string.no_answer))
            } else {
                helper.setText(R.id.text_view, mContext.getString(R.string.did_not_answer))
            }
        } else if (content.status == 1 || content.status == 5) {
            val calledTime =
                if (content.endTime > content.startTime) content.endTime - content.startTime else 0
            helper.setText(
                R.id.text_view,
                String.format(
                    mContext.getString(R.string.call_duration_mat),
                    TimeUtils.timeFormatToMediaDuration(calledTime)
                )
            )
        } else if (content.status == 2) {
            if (content.isSend == 1) {
                helper.setText(
                    R.id.text_view,
                    mContext.getString(R.string.the_other_party_has_refused)
                )
            } else {
                helper.setText(R.id.text_view, mContext.getString(R.string.denied))
            }
        } else if (content.status == 3) {
            if (content.isSend == 1) {
                helper.setText(R.id.text_view, mContext.getString(R.string.canceled))
            } else {
                helper.setText(R.id.text_view, mContext.getString(R.string.counterparty_canceled))
            }
        } else if (content.status == 4) {
            helper.setText(R.id.text_view, mContext.getString(R.string.the_other_is_busy))
        } else {
            helper.setText(R.id.text_view, mContext.getString(R.string.error_condition))
        }

        helper.addOnClickListener(R.id.text_view)
        helper.addOnLongClickListener(R.id.text_view)
        helper.addOnLongClickListener(R.id.layout_msg_content)
    }

    private fun bindRefMessage(helper: BaseViewHolder, messageModel: MessageModel): Boolean {
        if (messageModel.refMessageBean != null && messageModel.refMessageBean.msgId > 0) {
            val refView = helper.getView<View>(R.id.layout_ref_msg)
            val layoutParams = refView.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            refView.layoutParams = layoutParams
            refView.visibility = View.VISIBLE

            helper.addOnClickListener(R.id.layout_ref_msg)

            if (messageModel.refMessageBean.uid == mMineUid) {
                helper.setText(R.id.emoji_text_view_ref_nickname, mContext.getString(R.string.you))
            } else {
                val owner = mMessageOwnerList[messageModel.refMessageBean.uid]
                if (owner != null && owner is GroupMemberModel) {
                    helper.setText(R.id.emoji_text_view_ref_nickname, "${owner.displayName}")
                } else if (owner != null && owner is ContactDataModel) {
                    helper.setText(R.id.emoji_text_view_ref_nickname, "${owner.displayName}")
                } else {
                    if (!TextUtils.isEmpty(messageModel.refMessageBean.nickname)) {
                        helper.setText(
                            R.id.emoji_text_view_ref_nickname,
                            "${messageModel.refMessageBean.nickname}"
                        )
                    } else {
                        helper.setText(
                            R.id.emoji_text_view_ref_nickname,
                            "${messageModel.refMessageBean.uid}"
                        )
                    }
                }
            }

            when {
                messageModel.refMessageBean.type == IMPB.MessageType.text_VALUE -> helper.setText(
                    R.id.emoji_text_view_ref_content,
                    messageModel.refMessageBean.content
                )
                messageModel.refMessageBean.type == IMPB.MessageType.image_VALUE -> helper.setText(
                    R.id.emoji_text_view_ref_content,
                    mContext.getString(R.string.picture_sign)
                )
                messageModel.refMessageBean.type == IMPB.MessageType.dynamicImage_VALUE -> helper.setText(
                    R.id.emoji_text_view_ref_content,
                    mContext.getString(R.string.picture_dynamic_sign)
                )
                messageModel.refMessageBean.type == IMPB.MessageType.audio_VALUE -> helper.setText(
                    R.id.emoji_text_view_ref_content,
                    mContext.getString(R.string.voice_sign)
                )
                messageModel.refMessageBean.type == IMPB.MessageType.video_VALUE -> helper.setText(
                    R.id.emoji_text_view_ref_content,
                    mContext.getString(R.string.video_sign)
                )
                messageModel.refMessageBean.type == IMPB.MessageType.location_VALUE -> helper.setText(
                    R.id.emoji_text_view_ref_content,
                    mContext.getString(R.string.geographic_position)
                )
                messageModel.refMessageBean.type == IMPB.MessageType.file_VALUE -> helper.setText(
                    R.id.emoji_text_view_ref_content,
                    String.format(
                        mContext.getString(R.string.file_sign_mat),
                        messageModel.refMessageBean.content
                    )
                )
                messageModel.refMessageBean.type == IMPB.MessageType.nameCard_VALUE -> helper.setText(
                    R.id.emoji_text_view_ref_content,
                    mContext.getString(R.string.business_card_sign)
                )
                messageModel.refMessageBean.type == IMPB.MessageType.notice_VALUE -> helper.setText(
                    R.id.emoji_text_view_ref_content,
                    mContext.getString(R.string.group_of_announcement_sign)
                )
                else -> helper.setText(
                    R.id.emoji_text_view_ref_content,
                    mContext.getString(R.string.unknown_sign)
                )
            }

            if (messageModel.type == MessageModel.MESSAGE_TYPE_TEXT) {
                helper.getView<View>(R.id.emoji_text_view).minimumWidth =
                    ScreenUtils.dp2px(BaseApp.app, 120.0f)
            }
            return true
        } else {
            val refView = helper.getView<View>(R.id.layout_ref_msg)
            val layoutParams = refView.layoutParams
            layoutParams.width = RelativeLayout.LayoutParams.WRAP_CONTENT
            layoutParams.height = 0
            refView.layoutParams = layoutParams
            refView.visibility = View.GONE

            if (messageModel.type == MessageModel.MESSAGE_TYPE_TEXT) {
                helper.getView<View>(R.id.emoji_text_view).minimumWidth =
                    ScreenUtils.dp2px(BaseApp.app, 0.0f)
            }
            return false
        }
    }

    private fun bindTextItem(helper: BaseViewHolder, messageModel: MessageModel) {
        helper?.getView<EmojiTextView>(R.id.emoji_text_view)?.let {
            it.textSize = 16 * mTextMsgagnificationTimes
            it.setEmojiSize(
                (ScreenUtils.sp2px(
                    BaseApp.app,
                    16f
                ) * mTextMsgagnificationTimes).toInt()
            )
        }
        helper.getView<EmojiTextView>(R.id.emoji_text_view)?.isFindUrl = true
        helper.setText(R.id.emoji_text_view, messageModel.textMessageContent)
        helper.addOnClickListener(R.id.emoji_text_view)
        helper.addOnLongClickListener(R.id.emoji_text_view)
    }

    private fun bindNoticeItem(helper: BaseViewHolder, messageModel: MessageModel) {
        //设置消息内容字体大小
        helper.getView<EmojiTextView>(R.id.emoji_text_view)?.let {
            it.textSize = 16 * mTextMsgagnificationTimes
            it.setEmojiSize(
                (ScreenUtils.sp2px(
                    BaseApp.app,
                    16f
                ) * mTextMsgagnificationTimes).toInt()
            )
        }
        helper.getView<EmojiTextView>(R.id.emoji_text_view)?.isFindUrl = true

        helper.setText(R.id.emoji_text_view, messageModel.noticeMessageBean.content)
        helper.addOnClickListener(R.id.layout_msg_content)
        helper.addOnClickListener(R.id.emoji_text_view)
        helper.addOnLongClickListener(R.id.layout_msg_content)
        helper.addOnLongClickListener(R.id.emoji_text_view)
    }

    private fun bindNameCardItem(helper: BaseViewHolder, messageModel: MessageModel) {
        helper.getView<AppImageView>(R.id.image_view_card_icon)
            .setImageURI(messageModel.nameCardMessageContent.icon)
        helper.setText(R.id.emoji_text_view_card_name, messageModel.nameCardMessageContent.nickName)
        helper.addOnClickListener(R.id.layout_msg_content)
        helper.addOnLongClickListener(R.id.layout_msg_content)
    }

    private fun bindLocationItem(helper: BaseViewHolder, messageModel: MessageModel) {
        var thumbUrl = MAP_HTTP_HOST
        thumbUrl = thumbUrl.replace(
            "\\{lng\\}".toRegex(),
            (messageModel.locationMessageContentBean.lng / 1000000.0f).toString()
        )
        thumbUrl = thumbUrl.replace(
            "\\{lat\\}".toRegex(),
            (messageModel.locationMessageContentBean.lat / 1000000.0f).toString()
        )
        thumbUrl = thumbUrl.replace(
            "\\{width\\}".toRegex(),
            (ScreenUtils.dp2px(BaseApp.app, 210f)).toString()
        )
        thumbUrl = thumbUrl.replace(
            "\\{height\\}".toRegex(),
            (ScreenUtils.dp2px(BaseApp.app, 135f)).toString()
        )
        helper.getView<AppImageView>(R.id.image_view_location_pic).setImageURI(thumbUrl)
        helper.setText(
            R.id.text_view_location_address,
            messageModel.locationMessageContentBean.address
        )

        helper.addOnClickListener(R.id.image_view_location_pic)
        helper.addOnLongClickListener(R.id.image_view_location_pic)
    }

    private fun bindFileItem(helper: BaseViewHolder, messageModel: MessageModel) {
        val fileBean = messageModel.fileMessageContentBean
        val iconUri = UriUtil.getUriForResourceId(FileResUtils.get(fileBean.name))
        helper.getView<AppImageView>(R.id.image_view_file_icon).setImageURI(iconUri)
        helper.setText(R.id.text_view_file_name, fileBean.name)

        helper.setVisible(R.id.progress_bar, false)
        helper.setVisible(R.id.image_view_pause, false)
        helper.setText(
            R.id.text_view_file_size,
            "${Formatter.formatFileSize(mContext, fileBean.size)}"
        )

        val progress = DownloadAttachmentController.getDownloadSize(chatType, messageModel)
        if (messageModel.status == MessageModel.STATUS_ATTACHMENT_UPLOADING ||
            (DownloadAttachmentController.isDownloading(
                chatType,
                messageModel.copyMessage()
            ) && progress > 0)
        ) {
            /**
             * UploadAttachmentManager控制上传进度条,DownloadAttachmentManager控制下载进度条
             * 具体查看下面类
             * @see UploadAttachmentController
             * @see framework.telegram.message.manager.UploadAttachmentManager
             * @see DownloadAttachmentController
             * @see framework.telegram.message.manager.DownloadAttachmentManager
             */
            helper.setVisible(R.id.progress_bar, true)
            helper.setVisible(R.id.image_view_pause, true)

            if (!DownloadAttachmentController.isDownloading(
                    chatType,
                    messageModel.copyMessage()
                ) && progress > 0
            ) {
                helper.getView<QMUIProgressBar>(R.id.progress_bar).maxValue = 100
                helper.getView<QMUIProgressBar>(R.id.progress_bar)
                    .setProgress((progress * 100).toInt(), false)

                val sizeView = helper.getView<TextView>(R.id.text_view_file_size)
                sizeView?.post {
                    val size = messageModel.fileMessageContentBean.size
                    val finalSize = Formatter.formatFileSize(mContext, size)
                    val progressSize =
                        Formatter.formatFileSize(mContext, (size * progress).toLong())
                    sizeView.text = String.format(
                        mContext.getString(R.string.downloading),
                        progressSize,
                        finalSize
                    )
                }
            }

        } else {
            helper.setVisible(R.id.progress_bar, false)
            helper.setVisible(R.id.image_view_pause, false)
        }

        helper.addOnClickListener(R.id.image_view_pause)
        helper.addOnClickListener(R.id.layout_msg_content)
        helper.addOnLongClickListener(R.id.layout_msg_content)
    }

    private fun bindRecallItem(helper: BaseViewHolder, messageModel: MessageModel) {
        if (messageModel.isSend == 0) {
            val owner = mMessageOwnerList[messageModel.ownerUid]
            if (owner != null && owner is GroupMemberModel) {
                helper.setText(
                    R.id.text_view,
                    String.format(
                        mContext.getString(R.string.a_message_was_withdrawn),
                        owner.displayName
                    )
                )
            } else if (owner != null && owner is ContactDataModel) {
                helper.setText(
                    R.id.text_view,
                    String.format(
                        mContext.getString(R.string.a_message_was_withdrawn),
                        owner.displayName
                    )
                )
            } else {
                helper.setText(
                    R.id.text_view,
                    String.format(
                        mContext.getString(R.string.a_message_was_withdrawn),
                        messageModel.ownerUid
                    )
                )
            }
        } else {
            helper.setText(R.id.text_view, mContext.getString(R.string.you_withdrew_a_message))
        }
    }

    private fun bindVoiceItemOther(helper: BaseViewHolder, messageModel: MessageModel) {
        audioPlayController.bindVoiceItem(helper, messageModel)
        helper.addOnLongClickListener(R.id.layout_msg_content)
        if (messageModel.isSend == 0 && messageModel.isReadedAttachment == 0) {
            helper.getView<View>(R.id.text_view_read_attachment_flag).visibility = View.VISIBLE
        } else {
            helper.getView<View>(R.id.text_view_read_attachment_flag).visibility = View.GONE
        }
    }

    private fun bindVoiceItemMine(helper: BaseViewHolder, messageModel: MessageModel) {
        audioPlayController.bindVoiceItem(helper, messageModel)
        helper.addOnLongClickListener(R.id.layout_msg_content)
    }

    private fun bindImageItem(helper: BaseViewHolder, messageModel: MessageModel) {
        val image = messageModel.imageMessageContent
        val imageView = helper.getView<ImageView>(R.id.app_image_view)
        val width = image?.width?.toFloat() ?: ScreenUtils.dp2px(BaseApp.app, 180.0f).toFloat()
        val height = image?.height?.toFloat() ?: ScreenUtils.dp2px(BaseApp.app, 120.0f).toFloat()
        val maxSize = ScreenUtils.dp2px(BaseApp.app, 180.0f)
        val minSize = ScreenUtils.dp2px(BaseApp.app, 120.0f)
        val newSize =
            SizeUtils.calculateNewSize(width, height, maxSize.toFloat(), minSize.toFloat())

        val layoutParams = imageView.layoutParams
        layoutParams.width = newSize.first()
        layoutParams.height = newSize.last()
        imageView.layoutParams = layoutParams

        activityRef.get()?.let {
            val drawable = if (!TextUtils.isEmpty(image.imageThumbFileBackupUri)) {
                try {
                    val file = File(UriUtil.parseUriOrNull(image.imageThumbFileBackupUri)?.path)
                    if (file.exists()) {
                        Drawable.createFromStream(file.inputStream(), image.imageThumbFileBackupUri)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            if (TextUtils.isEmpty(image.imageThumbFileUri)) {
                Glide.with(it).clear(imageView)
                if (messageModel.snapchatTime > 0 && chatType == CHAT_TYPE_PVT) {
                } else {
                    imageView.setImageDrawable(drawable)
                }
            } else {
                val requestBuilder = Glide.with(it)
                    .load(GlideUrl(image.imageThumbFileUri, messageModel.attachmentKey))
                if (messageModel.snapchatTime > 0 && chatType == CHAT_TYPE_PVT) {
                    requestBuilder
                        .transform(BlurTransformation(it, 14, 3))
                        .into(imageView)
                } else {
                    requestBuilder.placeholder(drawable).into(imageView)
                }
            }
        }

        if (helper.itemViewType == MessageModel.LOCAL_TYPE_MYSELF_IMAGE) {
            if (messageModel.status == MessageModel.STATUS_ATTACHMENT_UPLOADING) {
                helper.setVisible(R.id.image_view_pause, true)
            } else if (messageModel.isSend == 1 && messageModel.status == MessageModel.STATUS_SEND_FAIL) {
                helper.setVisible(R.id.image_view_pause, true)
            } else {
                helper.setVisible(R.id.progress_bar, false)
                helper.setVisible(R.id.image_view_pause, false)
            }
        }

        if (messageModel.snapchatTime > 0 && chatType == CHAT_TYPE_PVT) {
            helper.getView<PointerCountDownView>(R.id.pointer_count_down)
                .initCountDownMaxValue(messageModel.snapchatTime)
            if (messageModel.expireTime == 0L) {
                helper.getView<PointerCountDownView>(R.id.pointer_count_down)
                    .initCountDownText(messageModel.snapchatTime)
            }
            helper.setVisible(R.id.pointer_count_down, true)
        } else {
            helper.setVisible(R.id.pointer_count_down, false)
        }

        helper.addOnClickListener(R.id.image_view_pause)
        helper.addOnClickListener(R.id.app_image_view)
        helper.addOnLongClickListener(R.id.app_image_view)
    }

    private fun bindDynamicImageItem(
        helper: BaseViewHolder,
        msgModel: MessageModel,
        isRef: Boolean
    ) {
        val messageModel = msgModel.copyMessage()
        val image = messageModel.dynamicImageMessageBean
        val imageView = helper.getView<ImageView>(R.id.app_image_view)

        var newSize = SizeUtils.calculateDynamicNewSize(image.width, image.height)
        if (isRef && image.width < ScreenUtils.dp2px(BaseApp.app, 100.0f)) {
            val new1Size = ScreenUtils.dp2px(BaseApp.app, 100.0f)
            newSize = intArrayOf(new1Size, new1Size)
        }
        val layoutParams = imageView.layoutParams
        layoutParams.width = newSize.first()
        layoutParams.height = newSize.last()
        imageView.layoutParams = layoutParams

        activityRef.get()?.let {
            if (!TextUtils.isEmpty(image.imageFileBackupUri)) {
                helper.setGone(R.id.image_view_gif, false)
                Glide.with(it).load(image.imageFileBackupUri)
                    .placeholder(ColorDrawable(ContextCompat.getColor(it, R.color.d4d6d9)))
                    .into(imageView)
                if (messageModel.isSend == 0) {
                    // 修改为已播放
                    MessageController.sendMsgPlayedReceipt(
                        chatType,
                        messageModel.senderId,
                        messageModel.id
                    )
                } else {
                }
            } else {
                val minSize =
                    if (newSize.first() < newSize.last()) newSize.first() else newSize.last()
                if (image.size >= 2 * 1024 * 1024 && minSize >= ScreenUtils.dp2px(
                        BaseApp.app,
                        80f
                    )
                ) {
                    helper.setGone(R.id.image_view_gif, true)
                    helper.setText(
                        R.id.text_progress,
                        Formatter.formatFileSize(
                            mContext,
                            messageModel.dynamicImageMessageBean.size
                        )
                    )
                    if (!DownloadAttachmentController.isDownloading(chatType, messageModel)) {
                        DownloadAttachmentController.downloadAttachment(chatType, messageModel)
                    }
                    imageView.setImageDrawable(
                        ColorDrawable(
                            ContextCompat.getColor(
                                it,
                                R.color.d4d6d9
                            )
                        )
                    )
                } else {
                    helper.setGone(R.id.image_view_gif, false)
                    Glide.with(it)
                        .load(GlideUrl(image.imageFileUri, messageModel.attachmentKey))
                        .placeholder(ColorDrawable(ContextCompat.getColor(it, R.color.d4d6d9)))
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                p0: GlideException?,
                                p1: Any?,
                                p2: Target<Drawable>?,
                                p3: Boolean
                            ): Boolean {
                                return false
                            }

                            override fun onResourceReady(
                                p0: Drawable?,
                                p1: Any?,
                                p2: Target<Drawable>?,
                                p3: DataSource?,
                                p4: Boolean
                            ): Boolean {
                                if (messageModel.isSend == 0) {
                                    // 修改为已播放
                                    MessageController.sendMsgPlayedReceipt(
                                        chatType,
                                        messageModel.senderId,
                                        messageModel.id
                                    )
                                }
                                return false
                            }
                        })
                        .into(imageView)
                }
            }
        }

        val minSize = if (newSize.first() < newSize.last()) newSize.first() else newSize.last()
        if ((minSize >= ScreenUtils.dp2px(BaseApp.app, 80f)) &&
            (messageModel.status == MessageModel.STATUS_ATTACHMENT_UPLOADING || DownloadAttachmentController.isDownloading(
                chatType,
                messageModel.copyMessage()
            ))
        ) {
            helper.setVisible(R.id.image_view_pause, true)
        } else if (messageModel.isSend == 1 && messageModel.status == MessageModel.STATUS_SEND_FAIL) {
            helper.setVisible(R.id.image_view_pause, true)
        } else {
            helper.setGone(R.id.progress_bar, false)
            helper.setVisible(R.id.image_view_pause, false)
        }

        helper.addOnClickListener(R.id.image_view_gif)
        helper.addOnClickListener(R.id.app_image_view)
        helper.addOnLongClickListener(R.id.app_image_view)
    }

    private fun bindVideoItem(helper: BaseViewHolder, messageModel: MessageModel) {
        val video = messageModel.videoMessageContent
        val imageView = helper.getView<ImageView>(R.id.app_image_view)
        val width = video?.width?.toFloat() ?: ScreenUtils.dp2px(BaseApp.app, 160.0f).toFloat()
        val height = video?.height?.toFloat() ?: ScreenUtils.dp2px(BaseApp.app, 64.0f).toFloat()
        val maxSize = ScreenUtils.dp2px(BaseApp.app, 160.0f)
        val minSize = ScreenUtils.dp2px(BaseApp.app, 64.0f)
        val newSize =
            SizeUtils.calculateNewSize(width, height, maxSize.toFloat(), minSize.toFloat())
        imageView.layoutParams = FrameLayout.LayoutParams(newSize.first(), newSize.last())

        activityRef.get()?.let {
            val drawable = if (!TextUtils.isEmpty(video.videoThumbFileBackupUri)) {
                try {
                    val file = File(UriUtils.parseUri(video.videoThumbFileBackupUri).path)
                    if (file.exists()) {
                        Drawable.createFromStream(file.inputStream(), video.videoThumbFileBackupUri)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            if (TextUtils.isEmpty(video.videoThumbFileUri)) {
                Glide.with(it).clear(imageView)
                if (messageModel.snapchatTime > 0 && chatType == CHAT_TYPE_PVT) {
                } else {
                    imageView.setImageDrawable(drawable)
                }
            } else {
                val requestBuilder = Glide.with(it)
                    .load(GlideUrl(video.videoThumbFileUri, messageModel.attachmentKey))

                if (messageModel.snapchatTime > 0 && chatType == CHAT_TYPE_PVT) {
                    requestBuilder
                        .transform(BlurTransformation(it, 14, 3))
                        .into(imageView)
                } else {
                    requestBuilder.placeholder(drawable).into(imageView)
                }
            }
        }

        if (messageModel.status == MessageModel.STATUS_ATTACHMENT_UPLOADING || DownloadAttachmentController.isDownloading(
                chatType,
                messageModel.copyMessage()
            )
        ) {
            helper.setVisible(R.id.image_view_pause, true)
            helper.setVisible(R.id.image_view_play, false)
        } else if (messageModel.isSend == 1 && messageModel.status == MessageModel.STATUS_SEND_FAIL) {
            helper.setVisible(R.id.image_view_pause, true)
            helper.setVisible(R.id.progress_bar, false)
            helper.setVisible(R.id.image_view_play, false)
        } else {
            helper.setVisible(R.id.progress_bar, false)
            helper.setVisible(R.id.image_view_pause, false)
            helper.setVisible(R.id.image_view_play, true)
        }

        if (messageModel.snapchatTime > 0 && chatType == CHAT_TYPE_PVT) {
            val time =
                if (messageModel.videoMessageContent.videoTime > messageModel.snapchatTime) (messageModel.videoMessageContent.videoTime).toInt() else messageModel.snapchatTime
            helper.getView<PointerCountDownView>(R.id.pointer_count_down)
                .initCountDownMaxValue(time)
            if (messageModel.expireTime == 0L) {
                helper.getView<PointerCountDownView>(R.id.pointer_count_down)
                    .initCountDownText(time)
            }
            helper.setVisible(R.id.pointer_count_down, true)
        } else {
            helper.setVisible(R.id.pointer_count_down, false)
        }

        helper.addOnClickListener(R.id.image_view_pause)
        helper.addOnClickListener(R.id.image_view_play)
        helper.addOnClickListener(R.id.app_image_view)
        helper.addOnLongClickListener(R.id.app_image_view)
        helper.addOnLongClickListener(R.id.image_view_play)
    }

    private val mOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            val msgModel = buttonView.tag as MessageModel
            if (!isChecked) {
                mCheckedMessageList.remove(msgModel)
            } else {
                mCheckedMessageList.add(msgModel.copyMessage())
            }
            mCheckedMessageListener?.invoke(mCheckedMessageList.size)
        }

    private fun bindCheckBox(helper: BaseViewHolder, messageModel: MessageModel) {
        val checkbox = helper.getView<CheckBox>(R.id.check_box_msg)
        if (checkbox != null && isClickable) {
            checkbox.visibility = View.VISIBLE
            checkbox.tag = messageModel.copyMessage()
            checkbox.setOnCheckedChangeListener(null)
            var finded = false
            mCheckedMessageList.forEach {
                if (it.id == messageModel.id) {
                    finded = true
                }
            }
            checkbox.isChecked = finded
            checkbox.setOnCheckedChangeListener(mOnCheckedChangeListener)
        } else {
            checkbox?.visibility = View.GONE
        }
    }

    private fun bindStatus(helper: BaseViewHolder, messageModel: MessageModel) {
        val fire = helper.getView<ImageView>(R.id.image_view_fire)
        if (messageModel.snapchatTime > 0) {
            fire.visibility = View.VISIBLE
        } else {
            fire.visibility = View.GONE
        }

        // 先隐藏
        val status = helper.getView<ImageView>(R.id.image_view_status)
        status.visibility = View.GONE

        if (messageModel.isSend == 0) {
            return
        }

        // 再判断是否需要显示
        if (messageModel.type == MessageModel.MESSAGE_TYPE_STREAM
            || messageModel.type == MessageModel.MESSAGE_TYPE_RECALL
            || messageModel.type == MessageModel.MESSAGE_TYPE_RECALL_SUCCESS
            || messageModel.type == MessageModel.MESSAGE_TYPE_UNKNOW
            || messageModel.type == MessageModel.MESSAGE_TYPE_UNDECRYPT
            || messageModel.type == MessageModel.MESSAGE_TYPE_SYSTEM_TIP
            || messageModel.type == MessageModel.MESSAGE_TYPE_ERROR_TIP
            || messageModel.type == MessageModel.MESSAGE_TYPE_GROUP_TIP
        ) {
            return
        }

        when (messageModel.status) {
            MessageModel.STATUS_SENDING,
            MessageModel.STATUS_ATTACHMENT_PROCESSING,
            MessageModel.STATUS_ATTACHMENT_UPLOADING,
            MessageModel.STATUS_SENDED_NO_RESP -> {
                status.visibility = View.VISIBLE
                if (ArouterServiceManager.messageService.getCurrentTime() - messageModel.time > 10 * 1000) {
                    status.setImageResource(R.drawable.msg_icon_reload)
                } else {
                    status.setImageResource(R.drawable.msg_icon_sending)
                }
                status.setOnClickListener(null)
            }
            MessageModel.STATUS_SEND_FAIL -> {
                status.visibility = View.VISIBLE
                status.setImageResource(R.drawable.msg_icon_reload)
                status.setOnClickListener(retrySendClickListener)
                status.tag = messageModel.copyMessage()
            }
            MessageModel.STATUS_SENDED_HAS_RESP -> {
                if (chatType == ChatModel.CHAT_TYPE_PVT && targetId < Constant.Common.SYSTEM_USER_MAX_UID) {
                    return
                }

                status.visibility = View.VISIBLE
                status.setOnClickListener(null)
                status.tag = null

                if (!showReceiptStatus) {
                    when {
                        messageModel.isReadedAttachment == 1 -> status.setImageResource(R.drawable.msg_icon_deliver)
                        messageModel.isRead == 1 -> status.setImageResource(R.drawable.msg_icon_deliver)
                        messageModel.isDeliver == 1 -> status.setImageResource(R.drawable.msg_icon_deliver)
                        else -> status.setImageResource(R.drawable.msg_icon_sended)
                    }
                } else {
                    if (messageModel.type == MessageModel.MESSAGE_TYPE_VIDEO || messageModel.type == MessageModel.MESSAGE_TYPE_VOICE) {
                        when {
                            messageModel.isReadedAttachment == 1 -> status.setImageResource(R.drawable.msg_icon_readed)
                            messageModel.isRead == 1 -> status.setImageResource(R.drawable.msg_icon_deliver)
                            messageModel.isDeliver == 1 -> status.setImageResource(R.drawable.msg_icon_deliver)
                            else -> status.setImageResource(R.drawable.msg_icon_sended)
                        }
                    } else {
                        when {
                            messageModel.isReadedAttachment == 1 -> status.setImageResource(R.drawable.msg_icon_readed)
                            messageModel.isRead == 1 -> status.setImageResource(R.drawable.msg_icon_readed)
                            messageModel.isDeliver == 1 -> status.setImageResource(R.drawable.msg_icon_deliver)
                            else -> status.setImageResource(R.drawable.msg_icon_sended)
                        }
                    }
                }
            }
        }
    }

    private fun bindTimeline(helper: BaseViewHolder, messageModel: MessageModel) {
        helper.setGone(R.id.text_view_time_line, false)
        val adapterPosition = helper.adapterPosition
        val dataPosition = adapterPosition - headerLayoutCount
        var preItem: MessageModel? = null
        run outside@{
            for (index in dataPosition downTo 1) {
                val item = getItem(index - 1)
                if (item != null && item.type != MessageModel.MESSAGE_TYPE_RECALL && item.type != MessageModel.MESSAGE_TYPE_RECALL_SUCCESS) {
                    preItem = item
                    return@outside
                }
            }
        }

        if (preItem != null) {
            if (messageModel.time - preItem!!.time > 5 * 60 * 1000) {
                helper.setVisible(R.id.text_view_time_line, true)
                helper.setText(
                    R.id.text_view_time_line,
                    TimeUtils.timeFormatToMessageTimeline(BaseApp.app, messageModel.time)
                )
            }
        } else {
            helper.setVisible(R.id.text_view_time_line, true)
            helper.setText(
                R.id.text_view_time_line,
                TimeUtils.timeFormatToMessageTimeline(BaseApp.app, messageModel.time)
            )
        }
    }

    private val retrySendClickListener = View.OnClickListener { view ->
        val msg = view.tag as MessageModel?
        msg?.let {
            SendMessageManager.resendMessage(chatType, it)
        }
    }
}
