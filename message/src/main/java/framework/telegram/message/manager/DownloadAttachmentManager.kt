package framework.telegram.message.manager

import android.net.Uri
import android.text.TextUtils
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.dispatcher.DownloadDispatcher
import com.liulishuo.okdownload.core.listener.DownloadListener3
import framework.ideas.common.model.im.MessageModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.controller.DownloadAttachmentController
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.AESHelper
import framework.telegram.support.tools.MD5
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.download.DownloadManager
import framework.telegram.support.tools.file.DirManager
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import java.io.File

object DownloadAttachmentManager {

    init {
        DownloadDispatcher.setMaxParallelRunningCount(5)
    }

    fun downloadAttachment(chatType: Int, msgModel: MessageModel) {
        var cacheFile = hasCacheFile(msgModel)
        if (cacheFile == null) {
            when (msgModel.type) {
                MessageModel.MESSAGE_TYPE_VOICE -> {
                    val contentBean = msgModel.voiceMessageContent
                    val downloadUrl = contentBean?.recordFileUri ?: ""
                    val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                    val targetId = if (myUid == msgModel.senderId) msgModel.targetId else msgModel.senderId
                    cacheFile = File(File(DirManager.getVoiceFileDir(BaseApp.app, AccountManager.getLoginAccountUUid()), "$targetId"), "${msgModel.id}_${MD5.md5(downloadUrl)}.mp3")
                    download(chatType, msgModel, downloadUrl, cacheFile) { ct, mm, file ->
                        MessagesManager.executeChatTransactionAsync(ct, myUid, targetId, { realm ->
                            val msg = realm.where(MessageModel::class.java).equalTo("id", mm.id).findFirst()
                            msg?.let {
                                val voiceMessageContent = it.voiceMessageContent
                                voiceMessageContent.recordFileBackupUri = Uri.fromFile(file).toString()
                                it.voiceMessageContent = voiceMessageContent
                            }
                        }, {
                            DownloadAttachmentController.notifyItemChangedWithComplete(downloadUrl, ct, targetId, mm.id, file)
                        }, {
                            DownloadAttachmentController.notifyItemChangedWithFail(downloadUrl, ct, targetId, mm.id)
                        })
                    }
                }
                MessageModel.MESSAGE_TYPE_VIDEO -> {
                    val contentBean = msgModel.videoMessageContent
                    val downloadUrl = contentBean?.videoFileUri ?: ""
                    val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                    val targetId = if (myUid == msgModel.senderId) msgModel.targetId else msgModel.senderId
                    cacheFile = File(File(DirManager.getVideoFileDir(BaseApp.app, AccountManager.getLoginAccountUUid()), "$targetId"), "${msgModel.id}_${MD5.md5(downloadUrl)}.mp4")
                    download(chatType, msgModel, downloadUrl, cacheFile) { ct, mm, file ->
                        MessagesManager.executeChatTransactionAsync(ct, myUid, targetId, { realm ->
                            val msg = realm.where(MessageModel::class.java).equalTo("id", mm.id).findFirst()
                            msg?.let {
                                val videoMessageContent = it.videoMessageContent
                                videoMessageContent.videoFileBackupUri = Uri.fromFile(file).toString()
                                it.videoMessageContent = videoMessageContent
                            }
                        }, {
                            DownloadAttachmentController.notifyItemChangedWithComplete(downloadUrl, ct, targetId, mm.id, file)
                        }, {
                            DownloadAttachmentController.notifyItemChangedWithFail(downloadUrl, ct, targetId, mm.id)
                        })
                    }
                }
                MessageModel.MESSAGE_TYPE_FILE -> {
                    val contentBean = msgModel.fileMessageContentBean
                    val downloadUrl = contentBean?.fileUri ?: ""
                    val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                    val targetId = if (myUid == msgModel.senderId) msgModel.targetId else msgModel.senderId
                    cacheFile = File(File(DirManager.getDownloadFileDir(BaseApp.app, AccountManager.getLoginAccountUUid()), "$targetId"), "${msgModel.id}_${contentBean.name}")
                    download(chatType, msgModel, downloadUrl, cacheFile) { ct, mm, file ->
                        MessagesManager.executeChatTransactionAsync(ct, myUid, targetId, { realm ->
                            val msg = realm.where(MessageModel::class.java).equalTo("id", mm.id).findFirst()
                            msg?.let {
                                val fileMessageContentBean = it.fileMessageContentBean
                                fileMessageContentBean.fileBackupUri = Uri.fromFile(file).toString()
                                it.fileMessageContentBean = fileMessageContentBean
                            }
                        }, {
                            DownloadAttachmentController.notifyItemChangedWithComplete(downloadUrl, ct, targetId, mm.id, file)
                        }, {
                            DownloadAttachmentController.notifyItemChangedWithFail(downloadUrl, ct, targetId, mm.id)
                        })
                    }
                }
                MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE -> {
                    val contentBean = msgModel.dynamicImageMessageBean
                    val downloadUrl = contentBean?.imageFileUri ?: ""
                    val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                    val targetId = if (myUid == msgModel.senderId) msgModel.targetId else msgModel.senderId
                    cacheFile = File(File(DirManager.getDownloadFileDir(BaseApp.app, AccountManager.getLoginAccountUUid()), "$targetId"), "${msgModel.id}_${MD5.md5(downloadUrl)}.gif")
                    download(chatType, msgModel, downloadUrl, cacheFile) { ct, mm, file ->
                        MessagesManager.executeChatTransactionAsync(ct, myUid, targetId, { realm ->
                            val msg = realm.where(MessageModel::class.java).equalTo("id", mm.id).findFirst()
                            msg?.let {
                                val imageMessageContentBean = it.dynamicImageMessageBean
                                imageMessageContentBean.imageFileBackupUri = Uri.fromFile(file).toString()
                                it.dynamicImageMessageBean = imageMessageContentBean
                            }
                        }, {
                            DownloadAttachmentController.notifyItemChangedWithComplete(downloadUrl, ct, targetId, mm.id, file)
                        }, {
                            DownloadAttachmentController.notifyItemChangedWithFail(downloadUrl, ct, targetId, mm.id)
                        })
                    }
                }
            }
        }
    }

    fun hasCacheFile(msgModel: MessageModel): File? {
        when (msgModel.type) {
            MessageModel.MESSAGE_TYPE_VOICE -> {
                val contentBean = msgModel.voiceMessageContent
                return hasCacheFile(contentBean.recordFileBackupUri, contentBean.recordFileUri, msgModel)
            }
            MessageModel.MESSAGE_TYPE_VIDEO -> {
                val contentBean = msgModel.videoMessageContent
                return hasCacheFile(contentBean.videoFileBackupUri, contentBean.videoFileUri, msgModel)
            }
            MessageModel.MESSAGE_TYPE_FILE -> {
                val contentBean = msgModel.fileMessageContentBean
                return hasCacheFile(contentBean.fileBackupUri, contentBean.fileUri, msgModel)
            }
        }

        return null
    }

    fun hasCacheFile(cacheFileUri: String?, downloadUrl: String?, msgModel: MessageModel): File? {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val targetId = if (myUid == msgModel.senderId) msgModel.targetId else msgModel.senderId
        var cacheFile = File(UriUtils.parseUri(cacheFileUri).path)
        return if (cacheFile.isFile && cacheFile.exists() && cacheFile.length() > 0) {
            cacheFile
        } else {
            if (!TextUtils.isEmpty(downloadUrl)) {
                when (msgModel.type) {
                    MessageModel.MESSAGE_TYPE_VOICE -> {
                        cacheFile = File(File(DirManager.getVoiceFileDir(BaseApp.app, AccountManager.getLoginAccountUUid()), "$targetId"), "${msgModel.id}_${MD5.md5(downloadUrl)}.mp3")
                    }
                    MessageModel.MESSAGE_TYPE_VIDEO -> {
                        cacheFile = File(File(DirManager.getVideoFileDir(BaseApp.app, AccountManager.getLoginAccountUUid()), "$targetId"), "${msgModel.id}_${MD5.md5(downloadUrl)}.mp4")
                    }
                    MessageModel.MESSAGE_TYPE_FILE -> {
                        cacheFile = File(File(DirManager.getDownloadFileDir(BaseApp.app, AccountManager.getLoginAccountUUid()), "$targetId"), "${msgModel.id}_${msgModel.fileMessageContentBean.name}")
                    }
                }

                if (cacheFile.isFile && cacheFile.exists() && cacheFile.length() > 0) {
                    cacheFile
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    fun isDownloading(chatType: Int, msgModel: MessageModel): Boolean {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val targetId = if (myUid == msgModel.senderId) msgModel.targetId else msgModel.senderId
        return DownloadManager.isDownloading("${chatType}_${myUid}_${targetId}_${msgModel.id}")
    }

    fun isIdeling(chatType: Int, msgModel: MessageModel): Boolean {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val targetId = if (myUid == msgModel.senderId) msgModel.targetId else msgModel.senderId
        return DownloadManager.isIdeling("${chatType}_${myUid}_${targetId}_${msgModel.id}")
    }

    fun stopDownload(chatType: Int, msgModel: MessageModel) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val targetId = if (myUid == msgModel.senderId) msgModel.targetId else msgModel.senderId
        DownloadManager.stopDownload("${chatType}_${myUid}_${targetId}_${msgModel.id}")
    }

    fun stopAllDownload() {
        DownloadManager.stopAllDownload()
    }

    fun getDownloadSize(chatType: Int, msgModel: MessageModel): Float {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val targetId = if (myUid == msgModel.senderId) msgModel.targetId else msgModel.senderId
        return DownloadManager.getDownloadSize("${chatType}_${myUid}_${targetId}_${msgModel.id}")
    }

    private fun download(chatType: Int, msgModel: MessageModel, downloadUrl: String, cacheFile: File, complete: ((Int, MessageModel, File) -> Unit)? = null) {
        val tmpCacheFile = File(cacheFile.absolutePath + "___download")
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val targetId = if (myUid == msgModel.senderId) msgModel.targetId else msgModel.senderId
        DownloadManager.download("${chatType}_${myUid}_${targetId}_${msgModel.id}", downloadUrl, tmpCacheFile, object : DownloadListener3() {
            override fun warn(task: DownloadTask) {

            }

            override fun connected(task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long) {

            }

            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {

            }

            override fun started(task: DownloadTask) {
                DownloadAttachmentController.notifyItemChangedWithStart(task.url, chatType, targetId, msgModel.id)
            }

            override fun completed(task: DownloadTask) {
                ThreadUtils.runOnIOThread {
                    val file = task.file
                    file?.let {
                        downloadComplete(chatType, targetId, msgModel, downloadUrl, file, complete, {
                            ThreadUtils.runOnUIThread {
                                DownloadAttachmentController.notifyItemChangedWithFail(task.url, chatType, targetId, msgModel.id)
                            }
                        })
                    }
                }
            }

            override fun error(task: DownloadTask, e: Exception) {
                DownloadAttachmentController.notifyItemChangedWithFail(task.url, chatType, targetId, msgModel.id)
            }

            override fun canceled(task: DownloadTask) {
                DownloadAttachmentController.notifyItemChangedWithCancel(task.url, chatType, targetId, msgModel.id)
            }

            override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
                // 下载进度
                val percent = currentOffset.toDouble() / totalLength.toDouble() * 0.8
                DownloadAttachmentController.notifyItemChangedWithProgress(task.url, chatType, targetId, msgModel.id, percent, (totalLength * percent).toLong(), totalLength)
            }
        })
    }

    private fun downloadComplete(chatType: Int, targetId: Long, msgModel: MessageModel, downloadUrl: String, file: File, complete: ((Int, MessageModel, File) -> Unit)? = null, error: ((String?) -> Unit)? = null) {
        val secretKey = msgModel.attachmentKey
        // 修改为真实文件名
        val realFile = File(file.absolutePath.replace("___download", ""))
        file.renameTo(realFile)

        if (TextUtils.isEmpty(secretKey)) {
            // 下载并解密完成
            complete?.invoke(chatType, msgModel, realFile)
        } else {
            // 重命名下载的文件进行解密
            val encryptFile = File("${realFile.absolutePath}___encrypt")
            realFile.renameTo(encryptFile)

            try {
                // 解密下载的文件，并使用原下载路径作为解密文件的路径
                val result = AESHelper.decryptFile(secretKey, encryptFile.absolutePath, realFile.absolutePath) { current, total ->
                    val percent = current.toDouble() / total.toDouble() * 0.2 + 0.8
                    DownloadAttachmentController.notifyItemChangedWithProgress(downloadUrl, chatType, targetId, msgModel.id, percent, (total * percent).toLong(), total)
                    // 返回是否继续解密
                    true
                }

                if (result) {
                    // 删除加密文件
                    encryptFile.delete()
                    // 下载并解密完成
                    complete?.invoke(chatType, msgModel, realFile)
                } else {

                    // 删除加密文件
                    encryptFile.delete()
                    error?.invoke(BaseApp.app.getString(R.string.decryption_failure))
                }
            } catch (e: Exception) {
                // 删除加密文件
                encryptFile.delete()
                error?.invoke(BaseApp.app.getString(R.string.decryption_failure))
            }
        }
    }
}