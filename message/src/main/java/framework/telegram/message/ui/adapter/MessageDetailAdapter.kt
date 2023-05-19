package framework.telegram.message.ui.adapter


import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import android.text.format.Formatter
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.im.domain.pb.CommonProto
import com.im.pb.IMPB
import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.group.GroupChatDetailModel
import framework.ideas.common.model.group.GroupMemberModel
import framework.ideas.common.model.group.PvtChatDetailModel
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.ideas.common.model.im.MessageReceiptModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.ui.widget.ChatDetailInfoView
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.TimeUtils
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.emoji.EmojiTextView
import framework.telegram.ui.filepicker.utils.FileResUtils
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView
import framework.telegram.ui.utils.ScreenUtils
import framework.telegram.ui.utils.SizeUtils
import java.io.File

/**
 * Created by lzh on 19-7-17.
 * INFO:
 */
@SuppressLint("UseSparseArrays")
class MessageDetailAdapter(val chatType: Int) : AppBaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(null) {

    init {
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_TEXT, R.layout.msg_chat_item_text_mine)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_VOICE, R.layout.msg_chat_item_voice_mine)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_IMAGE, R.layout.msg_chat_item_image_mine)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_DYNAMIC_IMAGE, R.layout.msg_chat_item_dynamic_image_mine)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_VIDEO, R.layout.msg_chat_item_video_mine)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_NAMECARD, R.layout.msg_chat_item_namecard_mine)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_LOCATION, R.layout.msg_chat_item_location_mine)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_FILE, R.layout.msg_chat_item_file_mine)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_NOTICE, R.layout.msg_chat_item_notice_mine)

        addItemType(GroupChatDetailModel.GROUP_CHAT_DETAIL_TYPE, R.layout.msg_chat_group_item_detail)
        addItemType(PvtChatDetailModel.PVT_CHAT_DETAIL_TYPE, R.layout.msg_chat_pvt_item_detail)

        addItemType(TitleModel.TITLE_HEAD, R.layout.msg_title3)
    }

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private val mMessageOwnerList by lazy { HashMap<Long, Any>() }

    private val mTextMsgagnificationTimes by lazy {
        //放大倍数
        ArouterServiceManager.settingService.getFontSize()
    }

    fun setMessageOwnerList(map: HashMap<Long, Any>) {
        mMessageOwnerList.clear()
        mMessageOwnerList.putAll(map)
        notifyDataSetChanged()
    }

    override fun convert(helper: BaseViewHolder, item: MultiItemEntity?) {//MessageModel
        when (helper.itemViewType) {
            MessageModel.LOCAL_TYPE_MYSELF_TEXT -> {
                if (item is MessageModel) {
                    item.let {
                        bindTextItem(helper, item)
                        bindTimeline(helper, item)
                        bindStatus(helper, item)
                        bindRefMessage(helper, item)
                        addMarginsBottom(helper)
                    }
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_VOICE -> {
                if (item is MessageModel) {
                    item.let {
                        bindTimeline(helper, item)
                        bindVoiceItem(helper, item)
                        bindStatus(helper, item)
                        bindRefMessage(helper, item)
                        addMarginsBottom(helper)
                    }
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_IMAGE -> {
                if (item is MessageModel) {
                    item.let {
                        bindImageItem(helper, item)
                        bindTimeline(helper, item)
                        bindStatus(helper, item)
                        bindRefMessage(helper, item)
                        addMarginsBottom(helper)
                    }
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_DYNAMIC_IMAGE -> {
                if (item is MessageModel) {
                    item?.let {
                        bindDynamicImageItem(helper, item)
                        bindRefMessage(helper, item)
                        bindStatus(helper, item)
                        bindTimeline(helper, item)
                        addMarginsBottom(helper)
                    }
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_VIDEO -> {
                if (item is MessageModel) {
                    item.let {
                        bindVideoItem(helper, item)
                        bindTimeline(helper, item)
                        bindStatus(helper, item)
                        bindRefMessage(helper, item)
                        addMarginsBottom(helper)
                    }
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_NAMECARD -> {
                if (item is MessageModel) {
                    item.let {
                        bindNameCardItem(helper, item)
                        bindTimeline(helper, item)
                        bindStatus(helper, item)
                        bindRefMessage(helper, item)
                        addMarginsBottom(helper)
                    }
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_LOCATION -> {
                if (item is MessageModel) {
                    item.let {
                        bindLocationItem(helper, item)
                        bindTimeline(helper, item)
                        bindStatus(helper, item)
                        bindRefMessage(helper, item)
                        addMarginsBottom(helper)
                    }
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_FILE -> {
                if (item is MessageModel) {
                    item.let {
                        bindFileItem(helper, item)
                        bindTimeline(helper, item)
                        bindStatus(helper, item)
                        bindRefMessage(helper, item)
                        addMarginsBottom(helper)
                    }
                }
            }
            MessageModel.LOCAL_TYPE_MYSELF_NOTICE -> {
                if (item is MessageModel) {
                    item?.let {
                        bindNoticeItem(helper, item)
                        bindStatus(helper, item)
                        bindTimeline(helper, item)
                        addMarginsBottom(helper)
                    }
                }
            }
            GroupChatDetailModel.GROUP_CHAT_DETAIL_TYPE -> {
                if (item is GroupChatDetailModel) {
                    item.let {
                        bindGroupMsgReceiptItem(helper, item)
                    }
                }
            }
            PvtChatDetailModel.PVT_CHAT_DETAIL_TYPE -> {
                if (item is PvtChatDetailModel) {
                    item.let {
                        bindPvtMsgReceiptItem(helper, item)
                    }
                }
            }
            TitleModel.TITLE_HEAD -> {
                if (item is TitleModel) {
                    item.let {
                        if (it.title == "") {
                            helper.getView<FrameLayout>(R.id.frame_layout)?.layoutParams?.height = 1
                        } else {
                            helper.getView<FrameLayout>(R.id.frame_layout)?.layoutParams?.height = ScreenUtils.dp2px(helper.itemView.context, 52f)
                            helper.getView<TextView>(R.id.text_view_title)?.text = it.title
                        }
                    }
                }

            }
        }
    }

    private fun setMsgTagView(infoView: ChatDetailInfoView, status: Int, time: Long) {
        when (status) {
            CommonProto.MsgReceiptStatus.PLAYED.number -> {
                infoView.setData(mContext.getString(R.string.play_time), R.drawable.msg_icon_readed_attachment, time)
            }
            CommonProto.MsgReceiptStatus.VIEWED.number -> {
                infoView.setData(mContext.getString(R.string.see_the_time), R.drawable.msg_icon_readed, time)
            }
            CommonProto.MsgReceiptStatus.DELIVERED.number -> {
                infoView.setData(mContext.getString(R.string.delivery_time), R.drawable.msg_icon_deliver, time)
            }
            else -> {
                ""
            }
        }
    }

    private fun setMsgMainView(helper: BaseViewHolder, status: Int, time: Long) {
        var rid = 0
        when (status) {
            CommonProto.MsgReceiptStatus.PLAYED.number -> {
                rid = R.drawable.msg_icon_readed_attachment
            }
            CommonProto.MsgReceiptStatus.VIEWED.number -> {
                rid = R.drawable.msg_icon_readed
            }
            CommonProto.MsgReceiptStatus.DELIVERED.number -> {
                rid = R.drawable.msg_icon_deliver
            }
            else -> {

            }
        }

        helper.getView<ImageView>(R.id.image_view).setImageResource(rid)
        helper.getView<TextView>(R.id.image_view_time1).text = TimeUtils.getYYDFormatTime(time)
        helper.getView<TextView>(R.id.image_view_time2).text = TimeUtils.getHMFormatTime(time)
    }

    private fun bindTextItem(helper: BaseViewHolder, messageModel: MessageModel) {
        //设置消息内容字体大小 ygl todo 去掉魔法值
        helper?.getView<EmojiTextView>(R.id.emoji_text_view)?.let {
            it.setTextSize(16 * mTextMsgagnificationTimes)
            it.setEmojiSize((ScreenUtils.sp2px(BaseApp.app, 16f) * mTextMsgagnificationTimes).toInt())
        }
        helper.setText(R.id.emoji_text_view, messageModel.textMessageContent)
    }

    private fun bindLocationItem(helper: BaseViewHolder, messageModel: MessageModel) {
        var thumbUrl = Constant.Common.MAP_HTTP_HOST
        thumbUrl = thumbUrl.replace("\\{lng\\}".toRegex(), (messageModel.locationMessageContentBean.lng / 1000000.0f).toString())
        thumbUrl = thumbUrl.replace("\\{lat\\}".toRegex(), (messageModel.locationMessageContentBean.lat / 1000000.0f).toString())
        thumbUrl = thumbUrl.replace("\\{width\\}".toRegex(), (ScreenUtils.dp2px(BaseApp.app, 210f)).toString())
        thumbUrl = thumbUrl.replace("\\{height\\}".toRegex(), (ScreenUtils.dp2px(BaseApp.app, 135f)).toString())
        helper.getView<AppImageView>(R.id.image_view_location_pic).setImageURI(thumbUrl)
        helper.setText(R.id.text_view_location_address, messageModel.locationMessageContentBean.address)
    }

    private fun bindFileItem(helper: BaseViewHolder, messageModel: MessageModel) {
        helper.setText(R.id.text_view_file_name, messageModel.fileMessageContentBean.name)
        helper.setText(R.id.text_view_file_size, "${Formatter.formatFileSize(mContext, messageModel.fileMessageContentBean.size)}")
        helper.getView<ImageView>(R.id.image_view_file_icon)?.setImageResource(FileResUtils.get(messageModel.fileMessageContentBean.name))
    }

    private fun bindNameCardItem(helper: BaseViewHolder, messageModel: MessageModel) {
        helper.getView<AppImageView>(R.id.image_view_card_icon).setImageURI(messageModel.nameCardMessageContent.icon)
        helper.setText(R.id.emoji_text_view_card_name, messageModel.nameCardMessageContent.nickName)
    }

    private fun bindGroupMsgReceiptItem(helper: BaseViewHolder, item: GroupChatDetailModel) {
        val owner = mMessageOwnerList[item.bean.senderUid]
        if (owner != null && owner is GroupMemberModel) {
            helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(owner.icon)
            helper.getView<AppTextView>(R.id.image_view_name).text = owner.displayName
        } else if (owner != null && owner is ContactDataModel) {
            helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(owner.icon)
            helper.getView<AppTextView>(R.id.image_view_name).text = owner.displayName
        } else {
            helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(Uri.EMPTY)
            helper.getView<AppTextView>(R.id.image_view_name).text = "${item.bean.senderUid}"
        }

        if (item.isSelect) {
            helper.getView<ChatDetailInfoView>(R.id.chat_detail_info_view2).visibility = View.VISIBLE
            helper.getView<ChatDetailInfoView>(R.id.chat_detail_info_view3).visibility = View.VISIBLE
            helper.getView<ImageView>(R.id.image_view).visibility = View.GONE
            helper.getView<TextView>(R.id.image_view_time1).visibility = View.GONE
            helper.getView<TextView>(R.id.image_view_time2).visibility = View.GONE

            if (item.bean.messageType == MessageModel.LOCAL_TYPE_MYSELF_VOICE) {
                setMsgTagView(helper.getView(R.id.chat_detail_info_view1), CommonProto.MsgReceiptStatus.PLAYED.number, item.bean.readedAttachmentTime)
                helper.getView<ChatDetailInfoView>(R.id.chat_detail_info_view1).visibility = View.VISIBLE
            } else {
                helper.getView<ChatDetailInfoView>(R.id.chat_detail_info_view1).visibility = View.GONE
            }
            setMsgTagView(helper.getView(R.id.chat_detail_info_view2), CommonProto.MsgReceiptStatus.VIEWED.number, item.bean.readTime)
            setMsgTagView(helper.getView(R.id.chat_detail_info_view3), CommonProto.MsgReceiptStatus.DELIVERED.number, item.bean.deliverTime)
        } else {
            helper.getView<ChatDetailInfoView>(R.id.chat_detail_info_view1).visibility = View.GONE
            helper.getView<ChatDetailInfoView>(R.id.chat_detail_info_view2).visibility = View.GONE
            helper.getView<ChatDetailInfoView>(R.id.chat_detail_info_view3).visibility = View.GONE
            helper.getView<AppTextView>(R.id.image_view_name).visibility = View.VISIBLE
            helper.getView<TextView>(R.id.image_view_time1).visibility = View.VISIBLE
            helper.getView<TextView>(R.id.image_view_time2).visibility = View.VISIBLE
            helper.getView<ImageView>(R.id.image_view).visibility = View.VISIBLE

            when {
                item.bean.readedAttachmentTime > 0 -> {
                    if (item.bean.messageType == MessageModel.LOCAL_TYPE_MYSELF_VOICE) {
                        setMsgMainView(helper, CommonProto.MsgReceiptStatus.PLAYED.number, item.bean.readedAttachmentTime)
                    }
                }
                item.bean.readTime > 0 -> setMsgMainView(helper, CommonProto.MsgReceiptStatus.VIEWED.number, item.bean.readTime)
                item.bean.deliverTime > 0 -> setMsgMainView(helper, CommonProto.MsgReceiptStatus.DELIVERED.number, item.bean.deliverTime)
            }
        }
    }

    private fun bindPvtMsgReceiptItem(helper: BaseViewHolder, item: PvtChatDetailModel) {
        helper.getView<TextView>(R.id.image_view_time1).text = TimeUtils.getYYDFormatTime(item.bean?.time
                ?: 0L)
        helper.getView<TextView>(R.id.image_view_time2).text = TimeUtils.getHMFormatTime(item.bean?.time
                ?: 0L)

        when (item.bean?.status?.number) {
            CommonProto.MsgReceiptStatus.PLAYED.number -> {
                helper.getView<ImageView>(R.id.image_view_status_icon).setImageResource(R.drawable.msg_icon_readed_attachment)
                helper.getView<TextView>(R.id.image_view_status_title).text = mContext.getString(R.string.play_time)
            }
            CommonProto.MsgReceiptStatus.VIEWED.number -> {
                helper.getView<ImageView>(R.id.image_view_status_icon).setImageResource(R.drawable.msg_icon_readed)
                helper.getView<TextView>(R.id.image_view_status_title).text = mContext.getString(R.string.see_the_time)
                if (!item.isShowAlreadyRead) {
                    helper.getView<TextView>(R.id.image_view_time1).text = "-"
                    helper.getView<TextView>(R.id.image_view_time2).text = "-"
                }
            }
            CommonProto.MsgReceiptStatus.DELIVERED.number -> {
                helper.getView<ImageView>(R.id.image_view_status_icon).setImageResource(R.drawable.msg_icon_deliver)
                helper.getView<TextView>(R.id.image_view_status_title).text = mContext.getString(R.string.delivery_time)
            }
            else -> {
            }
        }
    }

    private fun bindStatus(helper: BaseViewHolder, messageModel: MessageModel) {
        val status = helper.getView<ImageView>(R.id.image_view_status)
        status.visibility = View.GONE

        val fire = helper.getView<ImageView>(R.id.image_view_fire)
        if (messageModel.snapchatTime > 0) {
            fire.visibility = View.VISIBLE
        } else {
            fire.visibility = View.GONE
        }

        if (messageModel.type == MessageModel.MESSAGE_TYPE_STREAM
                || messageModel.type == MessageModel.MESSAGE_TYPE_RECALL
                || messageModel.type == MessageModel.MESSAGE_TYPE_RECALL_SUCCESS
                || messageModel.type == MessageModel.MESSAGE_TYPE_UNKNOW
                || messageModel.type == MessageModel.MESSAGE_TYPE_UNDECRYPT) {
            return
        }

        when (messageModel.status) {
            MessageModel.STATUS_SENDED_HAS_RESP -> {
                status.visibility = View.VISIBLE
                status.setOnClickListener(null)

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
            else -> {
                status.visibility = View.GONE
                status.setOnClickListener(null)
            }
        }
    }

    private fun bindImageItem(helper: BaseViewHolder, messageModel: MessageModel) {
        val image = messageModel.imageMessageContent
        val imageView = helper.getView<ImageView>(R.id.app_image_view)
        val width = image.width.toFloat()
        val height = image.height.toFloat()
        val maxSize = ScreenUtils.dp2px(BaseApp.app, 180.0f)
        val minSize = ScreenUtils.dp2px(BaseApp.app, 120.0f)
        val newSize = SizeUtils.calculateNewSize(width, height, maxSize.toFloat(), minSize.toFloat())
        imageView.layoutParams = FrameLayout.LayoutParams(newSize.first(), newSize.last())

        var drawable: Drawable? = null

        if (!TextUtils.isEmpty(image.imageThumbFileBackupUri)) {
            val file = File(Uri.parse(image.imageThumbFileBackupUri).path)
            if (file.exists()) {
                drawable = Drawable.createFromStream(file.inputStream(), image.imageThumbFileBackupUri)
            }
        }
        if (drawable == null && !TextUtils.isEmpty(image.imageFileBackupUri)) {
            val file = File(Uri.parse(image.imageFileBackupUri).path)
            if (file.exists()) {
                drawable = Drawable.createFromStream(file.inputStream(), image.imageFileBackupUri)
            }
        }
        if (TextUtils.isEmpty(image.imageThumbFileUri)) {
            Glide.with(imageView).clear(imageView)
            imageView.setImageDrawable(drawable)
        } else {
            val requestBuilder = Glide.with(imageView)
                    .load(GlideUrl(image.imageThumbFileUri, messageModel.attachmentKey))
                    .placeholder(drawable)
            requestBuilder.into(imageView)
        }
    }

    private fun bindDynamicImageItem(helper: BaseViewHolder, messageModel: MessageModel) {
        val image = messageModel.imageMessageContent
        val imageView = helper.getView<ImageView>(R.id.app_image_view)
        val newSize = SizeUtils.calculateDynamicNewSize(image.width, image.height)
        imageView.layoutParams = FrameLayout.LayoutParams(newSize.first(), newSize.last())

        if (!TextUtils.isEmpty(image.imageFileBackupUri)) {
            Glide.with(imageView).load(image.imageFileBackupUri)
                    .placeholder(ColorDrawable(ContextCompat.getColor(imageView.context, R.color.d4d6d9)))
                    .into(imageView)
        } else {
            Glide.with(imageView).load(GlideUrl(image.imageFileUri, messageModel.attachmentKey))
                    .placeholder(ColorDrawable(ContextCompat.getColor(imageView.context, R.color.d4d6d9)))
                    .into(imageView)
        }
    }


    private fun bindVideoItem(helper: BaseViewHolder, messageModel: MessageModel) {
        val video = messageModel.videoMessageContent
        val imageView = helper.getView<ImageView>(R.id.app_image_view)
        val width = video.width.toFloat()
        val height = video.height.toFloat()
        val maxSize = ScreenUtils.dp2px(BaseApp.app, 160.0f)
        val minSize = ScreenUtils.dp2px(BaseApp.app, 64.0f)
        val newSize = SizeUtils.calculateNewSize(width, height, maxSize.toFloat(), minSize.toFloat())
        imageView.layoutParams = FrameLayout.LayoutParams(newSize.first(), newSize.last())

        imageView?.let {
            val drawable = if (!TextUtils.isEmpty(video.videoThumbFileBackupUri)) {
                val file = File(Uri.parse(video.videoThumbFileBackupUri).path)
                if (file.exists()) {
                    Drawable.createFromStream(file.inputStream(), video.videoThumbFileBackupUri)
                } else {
                    null
                }
            } else null
            Glide.with(it)
                    .load(GlideUrl(video.videoThumbFileUri, messageModel.attachmentKey))
                    .placeholder(drawable).into(imageView)
        }

    }

    private fun bindTimeline(helper: BaseViewHolder, messageModel: MessageModel) {
        helper.setGone(R.id.text_view_time_line, false)
        helper.setVisible(R.id.text_view_time_line, true)
        helper.setText(R.id.text_view_time_line, TimeUtils.timeFormatToMessageTimeline(BaseApp.app, messageModel.time))
        helper.setBackgroundRes(R.id.text_view_time_line, 0)
    }

    private fun bindVoiceItem(helper: BaseViewHolder, messageModel: MessageModel) {
        val txtAllTime = helper.getView<TextView>(R.id.txtAllTime)
        val voice = messageModel.voiceMessageContent

        val seekBar = helper.getView<SeekBar>(R.id.seekBar)
        seekBar.progressDrawable.setColorFilter(ContextCompat.getColor(BaseApp.app, R.color.white), PorterDuff.Mode.SRC_IN)
        seekBar.thumb.setColorFilter(ContextCompat.getColor(BaseApp.app, R.color.white), PorterDuff.Mode.SRC_IN)

        txtAllTime.text = TimeUtils.timeFormatToMediaDuration(voice.recordTime * 1000L)
        helper.getView<TextView>(R.id.txtCurrentTime).text = "00:00"
    }

    private fun bindNoticeItem(helper: BaseViewHolder, messageModel: MessageModel) {
        //设置消息内容字体大小 ygl todo 去掉魔法值
        helper?.getView<EmojiTextView>(R.id.emoji_text_view)?.let {
            it.setTextSize(16 * mTextMsgagnificationTimes)
            it.setEmojiSize((ScreenUtils.sp2px(BaseApp.app, 16f) * mTextMsgagnificationTimes).toInt())
        }
        helper.getView<EmojiTextView>(R.id.emoji_text_view)?.isFindUrl = true

//        if (messageModel.isSend == 1) {
//            helper.getView<EmojiTextView>(R.id.emoji_text_view)?.setUrlColor(ContextCompat.getColor(BaseApp.app, R.color.white))
//        } else {
//            helper.getView<EmojiTextView>(R.id.emoji_text_view)?.setUrlColor(ContextCompat.getColor(BaseApp.app, R.color.c178aff))
//        }

        helper.setText(R.id.emoji_text_view, messageModel.noticeMessageBean.content)
        helper.addOnClickListener(R.id.emoji_text_view)
        helper.addOnLongClickListener(R.id.emoji_text_view)
    }

    private fun bindRefMessage(helper: BaseViewHolder, messageModel: MessageModel) {
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
                        helper.setText(R.id.emoji_text_view_ref_nickname, "${messageModel.refMessageBean.nickname}")
                    } else {
                        helper.setText(R.id.emoji_text_view_ref_nickname, "${messageModel.refMessageBean.uid}")
                    }
                }
            }

            when {
                messageModel.refMessageBean.type == IMPB.MessageType.text_VALUE -> helper.setText(R.id.emoji_text_view_ref_content, messageModel.refMessageBean.content)
                messageModel.refMessageBean.type == IMPB.MessageType.image_VALUE -> helper.setText(R.id.emoji_text_view_ref_content, mContext.getString(R.string.picture_sign))
                messageModel.refMessageBean.type == IMPB.MessageType.dynamicImage_VALUE -> helper.setText(R.id.emoji_text_view_ref_content, mContext.getString(R.string.picture_sign))
                messageModel.refMessageBean.type == IMPB.MessageType.audio_VALUE -> helper.setText(R.id.emoji_text_view_ref_content, mContext.getString(R.string.voice_sign))
                messageModel.refMessageBean.type == IMPB.MessageType.video_VALUE -> helper.setText(R.id.emoji_text_view_ref_content, mContext.getString(R.string.video_sign))
                messageModel.refMessageBean.type == IMPB.MessageType.location_VALUE -> helper.setText(R.id.emoji_text_view_ref_content, mContext.getString(R.string.geographic_position))
                messageModel.refMessageBean.type == IMPB.MessageType.file_VALUE -> helper.setText(R.id.emoji_text_view_ref_content, messageModel.refMessageBean.content)
                messageModel.refMessageBean.type == IMPB.MessageType.nameCard_VALUE -> helper.setText(R.id.emoji_text_view_ref_content, mContext.getString(R.string.business_card))
                messageModel.refMessageBean.type == IMPB.MessageType.notice_VALUE -> helper.setText(R.id.emoji_text_view_ref_content, mContext.getString(R.string.group_of_announcement_sign))
                else -> helper.setText(R.id.emoji_text_view_ref_content, mContext.getString(R.string.unknown_sign))
            }

            if (messageModel.type == MessageModel.MESSAGE_TYPE_TEXT) {
                helper.getView<View>(R.id.emoji_text_view).minimumWidth = ScreenUtils.dp2px(BaseApp.app, 120.0f)
            }
        } else {
            val refView = helper.getView<View>(R.id.layout_ref_msg)
            val layoutParams = refView.layoutParams
            layoutParams.width = RelativeLayout.LayoutParams.WRAP_CONTENT
            layoutParams.height = 0
            refView.layoutParams = layoutParams
            refView.visibility = View.GONE

            if (messageModel.type == MessageModel.MESSAGE_TYPE_TEXT) {
                helper.getView<View>(R.id.emoji_text_view).minimumWidth = ScreenUtils.dp2px(BaseApp.app, 0.0f)
            }
        }
    }

    private fun addMarginsBottom(helper: BaseViewHolder) {
        val lp = helper.itemView.layoutParams as RecyclerView.LayoutParams
        if (chatType == ChatModel.CHAT_TYPE_PVT) {
            lp.setMargins(0, 0, 0, ScreenUtils.dp2px(helper.itemView.context, 16f))
        }
        helper.itemView.layoutParams = lp
        helper.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.white))
    }
}
