package framework.telegram.message.controller

import com.liulishuo.okdownload.OkDownload
import framework.ideas.common.model.im.MessageModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.manager.DownloadAttachmentManager
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.ThreadUtils
import java.util.concurrent.CopyOnWriteArrayList
import java.io.File

object DownloadAttachmentController {

    private val mListeners by lazy { CopyOnWriteArrayList<DownloadAttachmentListener>() }

    fun attachDownloadListener(listener: DownloadAttachmentListener) {
        if (mListeners.contains(listener)) {
            return
        }

        mListeners.add(listener)
    }

    fun detachDownloadListener(listener: DownloadAttachmentListener) {
        mListeners.remove(listener)
    }

    fun hasCacheFile(msgModel: MessageModel): File? {
        return DownloadAttachmentManager.hasCacheFile(msgModel)
    }

    fun hasCacheFile(cacheFileUri: String?, downloadUrl: String?, msgModel: MessageModel): File? {
        return DownloadAttachmentManager.hasCacheFile(cacheFileUri, downloadUrl, msgModel)
    }

    fun isDownloading(chatType: Int, msgModel: MessageModel): Boolean {
        return DownloadAttachmentManager.isDownloading(chatType, msgModel)
    }

    fun isIdeling(chatType: Int, msgModel: MessageModel): Boolean {
        return DownloadAttachmentManager.isIdeling(chatType, msgModel)
    }

    fun downloadAttachment(chatType: Int, msgModel: MessageModel) {
        DownloadAttachmentManager.downloadAttachment(chatType, msgModel)
    }

    fun cancelDownload(downloadUrl: String?, chatType: Int, msgModel: MessageModel) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val targetId = if (myUid == msgModel.senderId) msgModel.targetId else msgModel.senderId
        DownloadAttachmentManager.stopDownload(chatType, msgModel)

        downloadUrl?.let {
            notifyItemChangedWithCancel(downloadUrl, chatType, targetId, msgModel.id)
        }
    }

    fun getDownloadSize( chatType: Int, msgModel: MessageModel):Float {
        return DownloadAttachmentManager.getDownloadSize(chatType, msgModel)

    }

    fun cancelAllDownload() {
        DownloadAttachmentManager.stopAllDownload()
        OkDownload.with().downloadDispatcher().cancelAll()
    }

    fun notifyItemChangedWithStart(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long) {
        ThreadUtils.runOnUIThread {
            mListeners.forEach { downloadAttachmentListener ->
                downloadAttachmentListener.downloadStart(downloadUrl, chatType, targetId, msgLocalId)
            }
        }
    }

    fun notifyItemChangedWithProgress(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long, percent: Double, currentOffset: Long, totalLength: Long) {
        ThreadUtils.runOnUIThread {
            mListeners.forEach { downloadAttachmentListener ->
                downloadAttachmentListener.downloadProgress(downloadUrl, chatType, targetId, msgLocalId, percent, currentOffset, totalLength)
            }
        }
    }

    fun notifyItemChangedWithCancel(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long) {
        ThreadUtils.runOnUIThread {
            mListeners.forEach { downloadAttachmentListener ->
                downloadAttachmentListener.downloadCancel(downloadUrl, chatType, targetId, msgLocalId)
            }
        }
    }

    fun notifyItemChangedWithComplete(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long, file: File) {
        ThreadUtils.runOnUIThread {
            mListeners.forEach { downloadAttachmentListener ->
                downloadAttachmentListener.downloadComplete(downloadUrl, chatType, targetId, msgLocalId, file)
            }
        }
    }

    fun notifyItemChangedWithFail(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long) {
        ThreadUtils.runOnUIThread {
            mListeners.forEach { downloadAttachmentListener ->
                downloadAttachmentListener.downloadFail(downloadUrl, chatType, targetId, msgLocalId)
            }
        }
    }

    interface DownloadAttachmentListener {
        fun downloadStart(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long)

        fun downloadProgress(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long, percent: Double, currentOffset: Long, totalLength: Long)

        fun downloadCancel(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long)

        fun downloadComplete(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long, file: File)

        fun downloadFail(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long)
    }
}