package framework.telegram.message.ui.preview

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.view.View
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.facebook.common.util.UriUtil
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_PREVIEW_FILE
import framework.telegram.message.bridge.event.RecallMessageEvent
import framework.telegram.message.controller.DownloadAttachmentController
import framework.telegram.message.controller.MessageController
import framework.telegram.message.manager.MessagesManager
import framework.telegram.support.BaseActivity
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.FileUtils
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.filepicker.utils.FileResUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.msg_activity_download_file_preview.*
import java.io.File

@Route(path = ROUNTE_MSG_PREVIEW_FILE)
class FilePreviewActivity : BaseActivity(),
    DownloadAttachmentController.DownloadAttachmentListener {

    private val mMineUid by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
    }

    private val mMessageLocalId by lazy { intent.getLongExtra("messageLocalId", -1) }

    private val mChatType by lazy { intent.getIntExtra("chatType", -1) }

    private val mTargetId by lazy { intent.getLongExtra("targetId", -1) }

    //下载文件的类型
    private val mMimeType by lazy { intent.getStringExtra("mimetype") ?: "" }

    //全名，带文件后缀
    private val mDownloadName: String by lazy { intent.getStringExtra("fileName") ?: "" }

    //文件大小
    private val mSize: Long by lazy { intent.getLongExtra("fileSize", 0L) }

    private var mMessageModel: MessageModel? = null

    // 是否阅后即焚消息
    private val mPrivate by lazy { intent?.getBooleanExtra("private", false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (mMessageLocalId <= 0 || mTargetId <= 0 || (mChatType != ChatModel.CHAT_TYPE_PVT && mChatType != ChatModel.CHAT_TYPE_GROUP)) {
            finish()
            return
        }

        var msgModel: MessageModel? = null
        MessagesManager.executeChatTransactionAsync(mChatType, mMineUid, mTargetId, { realm ->
            msgModel =
                realm.where(MessageModel::class.java).equalTo("id", mMessageLocalId).findFirst()
                    ?.copyMessage()
        }, {
            findMessageModel(msgModel)
            bindEvents(msgModel)

            mMessageModel?.let {
                val progress = DownloadAttachmentController.getDownloadSize(mChatType, it)
                if (!DownloadAttachmentController.isDownloading(
                        mChatType,
                        it.copyMessage()
                    ) && progress > 0
                ) {
                    val size = it.fileMessageContentBean.size
                    val finalSize = Formatter.formatFileSize(this@FilePreviewActivity, size)
                    val progressSize = Formatter.formatFileSize(
                        this@FilePreviewActivity,
                        (size * progress).toLong()
                    )
                    text_view_progress.text = String.format(
                        getString(R.string.downloading_sign_mat),
                        progressSize,
                        finalSize
                    )
                }
            }
        }, {
            finish()
        })

        setContentView(R.layout.msg_activity_download_file_preview)

        DownloadAttachmentController.attachDownloadListener(this@FilePreviewActivity)

        initView()
        initData()
    }

    override fun onDestroy() {
        super.onDestroy()
        DownloadAttachmentController.detachDownloadListener(this@FilePreviewActivity)
    }

    private fun initView() {
        showToolbarRightView()
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            onBackPressed()
        }
    }

    private fun initData() {
        text_view_filename.text = mDownloadName
        text_view_filesize.text = "${Formatter.formatFileSize(this, mSize)}"

        text_view_progress.text = ""

        image_view_mimetype.setImageURI(
            UriUtil.getUriForResourceId(
                FileResUtils.get(
                    mDownloadName,
                    false
                )
            )
        )
    }

    private fun showToolbarRightView() {
        if (mPrivate != true) {
            custom_toolbar.showRightImageView(rid = R.drawable.common_icon_more, onClickCallback = {
                val list = mutableListOf<String>()
                list.add(getString(R.string.forward))

                AppDialog.showBottomListView(
                    this@FilePreviewActivity,
                    this@FilePreviewActivity,
                    list
                ) { _, index, _ ->
                    when (index) {
                        0 -> {
                            if (mChatType == ChatModel.CHAT_TYPE_PVT) {
                                ARouter.getInstance()
                                    .build(Constant.ARouter.ROUNTE_MSG_FORWARD_CHATS)
                                    .withLong("messageLocalId", mMessageLocalId)
                                    .withInt("chatType", ChatModel.CHAT_TYPE_PVT)
                                    .withLong("targetId", mTargetId)
                                    .navigation()
                            } else {
                                ARouter.getInstance()
                                    .build(Constant.ARouter.ROUNTE_MSG_FORWARD_CHATS)
                                    .withLong("messageLocalId", mMessageLocalId)
                                    .withInt("chatType", ChatModel.CHAT_TYPE_GROUP)
                                    .withLong("targetId", mTargetId)
                                    .navigation()
                            }
                        }
                    }
                }
            })
        }
    }

    @SuppressLint("CheckResult")
    private fun bindEvents(msgModel: MessageModel?) {
        EventBus.getFlowable(RecallMessageEvent::class.java)
            .bindToLifecycle(this@FilePreviewActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (mTargetId == it.targetId && msgModel?.msgId == it.msgId) {
                    AppDialog.show(this@FilePreviewActivity, this@FilePreviewActivity) {
                        title(text = getString(R.string.hint))
                        message(text = getString(R.string.the_message_you_are_viewing_has_been_deleted))
                        positiveButton(text = getString(R.string.confirm), click = {
                            finish()
                        })
                    }
                }
            }
    }

    private fun findMessageModel(msgModel: MessageModel?) {
        if (msgModel == null || msgModel.type != MessageModel.MESSAGE_TYPE_FILE) {
            finish()
        } else {
            mMessageModel = msgModel

            val cacheFileUri = msgModel.fileMessageContentBean?.fileBackupUri
            val downloadUrl = msgModel.fileMessageContentBean?.fileUri
            val cacheFile =
                DownloadAttachmentController.hasCacheFile(cacheFileUri, downloadUrl, msgModel)
            if (cacheFile != null) {
                showOpenButton(cacheFile)
            } else {
                when {
                    DownloadAttachmentController.isDownloading(mChatType, msgModel) -> {
                        showDownloading()
                    }
                    DownloadAttachmentController.isIdeling(mChatType, msgModel) -> {
                        showDownloadResume()
                    }
                    else -> {
                        showDownloadButton()
                    }
                }
            }
        }
    }

    private fun showOpenButton(cacheFile: File) {
        text_view_progress.text = ""
        text_view_progress.setTextColor(resources.getColor(R.color.a2a4a7))

        progress_bar_progress.progress = 0

        btn_open_download_file.text = getString(R.string.open_it_with_another_application)
        btn_open_download_file.visibility = View.VISIBLE
        btn_open_download_file.setOnClickListener {
            val intent = Intent("android.intent.action.VIEW")
            intent.addCategory("android.intent.category.DEFAULT")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val uri = FileProvider.getUriForFile(
                this@FilePreviewActivity,
                this@FilePreviewActivity.packageName,
                cacheFile
            )

            try {
                if (mMimeType != "")
                    intent.setDataAndType(uri, mMimeType)
                else
                    intent.setDataAndType(
                        uri,
                        MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.path))
                    )
            } catch (e: java.lang.Exception) {
                intent.setDataAndType(uri, mMimeType)
            }

            try {
                //  直接跳转到系统默认打开方式
//                startActivity(intent)
                //  跳转到应用选择器
                startActivity(Intent.createChooser(intent, mDownloadName))

                // 已阅
                MessageController.sendMsgPlayedReceipt(mChatType, mTargetId, mMessageLocalId)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    private fun showDownloading() {
        mMessageModel?.let { msgModel ->
            val downloadUrl = msgModel.fileMessageContentBean?.fileUri

            text_view_progress.text = getString(R.string.downloading_sign)
            text_view_progress.setTextColor(resources.getColor(R.color.a2a4a7))

            btn_open_download_file.visibility = View.VISIBLE
            btn_open_download_file.text = getString(R.string.pause_to_download)
            btn_open_download_file.setOnClickListener {
                DownloadAttachmentController.cancelDownload(downloadUrl, mChatType, msgModel)
            }
        }
    }

    private fun showDownloadRetry() {
        text_view_progress.text = getString(R.string.download_failed)
        text_view_progress.setTextColor(resources.getColor(R.color.f50d2e))

        progress_bar_progress.progress = 0
        btn_open_download_file.text = getString(R.string.re_download)
        btn_open_download_file.visibility = View.VISIBLE
        btn_open_download_file.setOnClickListener {
            mMessageModel?.let {
                DownloadAttachmentController.downloadAttachment(mChatType, it)
                showDownloading()
            }
        }
    }

    private fun showDownloadResume() {
        btn_open_download_file.text = getString(R.string.continue_to_download)
        btn_open_download_file.visibility = View.VISIBLE
        btn_open_download_file.setOnClickListener {
            mMessageModel?.let {
                DownloadAttachmentController.downloadAttachment(mChatType, it)
                showDownloading()
            }
        }
    }

    private fun showDownloadButton() {
        progress_bar_progress.progress = 0
        btn_open_download_file.text = getString(R.string.download_the_file)
        btn_open_download_file.visibility = View.VISIBLE
        btn_open_download_file.setOnClickListener {
            mMessageModel?.let {
                DownloadAttachmentController.downloadAttachment(mChatType, it)
                showDownloading()
            }
        }
    }

    override fun downloadStart(
        downloadUrl: String,
        chatType: Int,
        targetId: Long,
        msgLocalId: Long
    ) {
        if (chatType == mChatType && targetId == mTargetId && msgLocalId == mMessageLocalId) {
            text_view_progress.text = getString(R.string.download_start)
            text_view_progress.setTextColor(resources.getColor(R.color.a2a4a7))
        }
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
        if (chatType == mChatType && targetId == mTargetId && msgLocalId == mMessageLocalId) {
            text_view_progress.text = String.format(
                getString(R.string.downloading_sign_mat),
                FileUtils.byteCountToDisplaySize(currentOffset),
                FileUtils.byteCountToDisplaySize(totalLength)
            )
            text_view_progress.setTextColor(resources.getColor(R.color.a2a4a7))

            if (currentOffset > 0 && totalLength > 0) {
                progress_bar_progress.max = 100
                progress_bar_progress.secondaryProgress = 100
                progress_bar_progress.progress =
                    (currentOffset.toFloat() / totalLength.toFloat() * 100).toInt()
            }
        }
    }

    override fun downloadComplete(
        downloadUrl: String,
        chatType: Int,
        targetId: Long,
        msgLocalId: Long,
        file: File
    ) {
        if (chatType == mChatType && targetId == mTargetId && msgLocalId == mMessageLocalId) {
            showOpenButton(file)
        }
    }

    override fun downloadCancel(
        downloadUrl: String,
        chatType: Int,
        targetId: Long,
        msgLocalId: Long
    ) {
        if (chatType == mChatType && targetId == mTargetId && msgLocalId == mMessageLocalId) {
            showDownloadResume()
        }
    }

    override fun downloadFail(
        downloadUrl: String,
        chatType: Int,
        targetId: Long,
        msgLocalId: Long
    ) {
        if (chatType == mChatType && targetId == mTargetId && msgLocalId == mMessageLocalId) {
            showDownloadRetry()
            toast(getString(R.string.file_download_failed))
        }
    }
}