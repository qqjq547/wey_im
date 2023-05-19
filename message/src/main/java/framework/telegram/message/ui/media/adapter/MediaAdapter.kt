package framework.telegram.message.ui.media.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.chad.library.adapter.base.BaseViewHolder
import com.qmuiteam.qmui.widget.QMUIProgressBar
import framework.ideas.common.model.im.MessageModel
import framework.telegram.message.R
import framework.telegram.message.controller.DownloadAttachmentController
import framework.telegram.message.controller.UploadAttachmentController
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.filepicker.utils.FileResUtils
import java.io.File
import java.text.SimpleDateFormat
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import java.util.*
import kotlin.math.roundToLong

class MediaAdapter(val chaterId: Long, val chatType: Int) : AppBaseMultiItemQuickAdapter<MessageModel, BaseViewHolder>(null),
        DownloadAttachmentController.DownloadAttachmentListener,
        UploadAttachmentController.UploadAttachmentListener {

    private enum class FileType {
        READY,
        DOWNLOADING,
        UPLOADING,
        CANCEL,
        FAILED,
        COMPLETE
    }

    init {
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_IMAGE, R.layout.msg_media_item_image)
        addItemType(MessageModel.LOCAL_TYPE_OTHER_IMAGE, R.layout.msg_media_item_image)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_VIDEO, R.layout.msg_media_item_video)
        addItemType(MessageModel.LOCAL_TYPE_OTHER_VIDEO, R.layout.msg_media_item_video)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_FILE, R.layout.msg_media_item_file)
        addItemType(MessageModel.LOCAL_TYPE_OTHER_FILE, R.layout.msg_media_item_file)
        addItemType(MessageModel.LOCAL_TYPE_OTHER_UNKNOW, R.layout.msg_media_item_head)
        addItemType(MessageModel.LOCAL_TYPE_MYSELF_UNKNOW, R.layout.msg_media_item_head)
    }

    @SuppressLint("SetTextI18n")
    override fun convert(helper: BaseViewHolder, item: MessageModel?) {
        item?.let {
            when (helper.itemViewType) {
                MessageModel.LOCAL_TYPE_MYSELF_IMAGE, MessageModel.LOCAL_TYPE_OTHER_IMAGE -> {
                    val imageView = helper.getView<ImageView>(R.id.image_view)
                    val imageContent = it.imageMessageContent
                    if (TextUtils.isEmpty(imageContent.imageThumbFileBackupUri)) {
                        Glide.with(mContext)
                                .load(GlideUrl(imageContent.imageThumbFileUri, it.attachmentKey))
                                .error(mContext.getSimpleDrawable(R.drawable.icon_fail_picture_big))
                                .centerCrop().into(imageView)
                    } else {
                        Glide.with(mContext)
                                .load(imageContent.imageThumbFileBackupUri)
                                .error(mContext.getSimpleDrawable(R.drawable.icon_fail_picture_big))
                                .centerCrop().into(imageView)
                    }
                    helper.addOnClickListener(R.id.image_view)
                }
                MessageModel.LOCAL_TYPE_MYSELF_VIDEO, MessageModel.LOCAL_TYPE_OTHER_VIDEO -> {
                    val videoContent = it.videoMessageContent
                    val imageView = helper.getView<ImageView>(R.id.image_view_video)
                    if (TextUtils.isEmpty(videoContent.videoThumbFileBackupUri)) {
                        Glide.with(mContext)
                                .load(GlideUrl(videoContent.videoThumbFileUri, it.attachmentKey))
                                .error(mContext.getSimpleDrawable(R.drawable.icon_fail_video_big))
                                .centerCrop().into(imageView)
                    } else {
                        Glide.with(mContext)
                                .load(videoContent.videoThumbFileBackupUri)
                                .error(mContext.getSimpleDrawable(R.drawable.icon_fail_video_big))
                                .centerCrop().into(imageView)
                    }
                    helper.setVisible(R.id.image_view_play, true)
                    helper.addOnClickListener(R.id.image_view_video)
                    helper.addOnClickListener(R.id.image_view_play)
                }
                MessageModel.LOCAL_TYPE_MYSELF_FILE, MessageModel.LOCAL_TYPE_OTHER_FILE -> {
                    val fileContent = it.fileMessageContentBean
                    helper.getView<TextView>(R.id.text_view_filename).text = fileContent.name
                    helper.getView<ImageView>(R.id.image_view_file_icon).setImageResource(FileResUtils.get(fileContent.name, false))
                    if (it.ownerName == "" || it.ownerName == null) {
                        ArouterServiceManager.contactService.getContactInfo(null, it.ownerUid, { contactDataModel, _ ->
                            helper.getView<TextView>(R.id.text_view_file_info).text = "${contactDataModel.nickName} ${coverTime(it.time)}"
                        })
                    } else {
                        helper.getView<TextView>(R.id.text_view_file_info).text = "${it.ownerName} ${coverTime(it.time)}"
                    }
                    if (fileContent.fileBackupUri == null)
                        helper.getView<TextView>(R.id.text_view_file_size).text = Formatter.formatFileSize(mContext, fileContent.size) + " 未下载"
                    else
                        helper.getView<TextView>(R.id.text_view_file_size).text = Formatter.formatFileSize(mContext, fileContent.size)
                }
                else -> {
                    val time = it.content
                    helper.getView<TextView>(R.id.text_view_time)?.text = time
                    if (getItem(0) == it) {
                        helper.getView<View>(R.id.view_line)?.visibility = View.GONE
                    } else
                        helper.getView<View>(R.id.view_line)?.visibility = View.VISIBLE
                }
            }
        }
    }


    override fun downloadStart(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long) {
        changeUI(chatType, targetId, msgLocalId, 0.0, 0, FileType.READY)
    }

    override fun downloadProgress(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long, percent: Double, currentOffset: Long, totalLength: Long) {
        changeUI(chatType, targetId, msgLocalId, percent, totalLength, FileType.DOWNLOADING)
    }

    override fun downloadCancel(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long) {
        changeUI(chatType, targetId, msgLocalId, 0.0, 0, FileType.CANCEL)
    }

    override fun downloadComplete(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long, file: File) {
        changeUI(chatType, targetId, msgLocalId, 100.0, 0, FileType.COMPLETE)
    }

    override fun downloadFail(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long) {
        changeUI(chatType, targetId, msgLocalId, 0.0, 0, FileType.FAILED)
    }

    override fun uploadStart(chatType: Int, targetId: Long, msgLocalId: Long) {
        changeUI(chatType, targetId, msgLocalId, 0.0, 0, FileType.READY)
    }

    override fun uploadProgress(chatType: Int, targetId: Long, msgLocalId: Long, percent: Double, currentOffset: Long, totalLength: Long) {
        changeUI(chatType, targetId, msgLocalId, percent, totalLength, FileType.UPLOADING)
    }

    override fun uploadCancel(chatType: Int, targetId: Long, msgLocalId: Long) {
        changeUI(chatType, targetId, msgLocalId, 0.0, 0, FileType.CANCEL)
    }

    override fun uploadComplete(chatType: Int, targetId: Long, msgLocalId: Long) {
        changeUI(chatType, targetId, msgLocalId, 0.0, 0, FileType.COMPLETE)
    }

    override fun uploadFail(chatType: Int, targetId: Long, msgLocalId: Long) {
        changeUI(chatType, targetId, msgLocalId, 0.0, 0, FileType.FAILED)
    }

    @SuppressLint("SetTextI18n")
    private fun changeUI(chatType: Int, targetId: Long, msgLocalId: Long, percent: Double, totalLength: Long, type: FileType) {
        if (chaterId == targetId && this.chatType == chatType) {
            Log.e(this.javaClass.simpleName, "changeUI = $chatType")
            data.forEachIndexed { index, msg ->
                if (msg.id == msgLocalId &&
                        (msg.itemType == MessageModel.LOCAL_TYPE_OTHER_FILE || msg.itemType == MessageModel.LOCAL_TYPE_MYSELF_FILE)
                ) {
                    val holder = recyclerView.findViewHolderForAdapterPosition(index)
                    if (holder is BaseViewHolder) {
                        holder.itemView.also {
                            val pauseView = it.findViewById<ImageView>(R.id.image_view_pause)
                            val progressBar = it.findViewById<QMUIProgressBar>(R.id.progress_bar)
                            val textView = it.findViewById<TextView>(R.id.text_view_file_size)
                            val size = Formatter.formatFileSize(mContext, totalLength)
                            val progressSize = Formatter.formatFileSize(mContext, (totalLength * percent).roundToLong())
                            when (type) {
                                FileType.READY, FileType.CANCEL -> {
                                    pauseView.visibility = View.GONE
                                    progressBar.visibility = View.GONE
                                    textView.text = ""
                                    textView.setTextColor(Color.BLACK)
                                }
                                FileType.DOWNLOADING -> {
                                    pauseView.visibility = View.VISIBLE
                                    progressBar.visibility = View.VISIBLE
                                    progressBar.progress = (percent * 100.0f).toInt()
                                    textView.text = "下载中... $progressSize/$size"
                                    textView.setTextColor(Color.BLACK)
                                }
                                FileType.UPLOADING -> {
                                    pauseView.visibility = View.VISIBLE
                                    progressBar.visibility = View.VISIBLE
                                    progressBar.progress = (percent * 100.0f).toInt()
                                    textView.text = "上传中... $progressSize/$size"
                                    textView.setTextColor(Color.BLACK)
                                }
                                FileType.FAILED -> {
                                    pauseView.visibility = View.GONE
                                    progressBar.visibility = View.GONE
                                    textView.text = "下载失败"
                                    textView.setTextColor(Color.RED)
                                }
                                FileType.COMPLETE -> {
                                    pauseView.visibility = View.GONE
                                    progressBar.visibility = View.GONE
                                    textView.text = Formatter.formatFileSize(mContext, msg.fileMessageContentBean.size)
                                    textView.setTextColor(Color.BLACK)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun coverTime(time: Long): String {
        val date = Date(time)
        val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
        return simpleDateFormat.format(date)
    }

}