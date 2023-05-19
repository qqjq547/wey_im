package framework.telegram.message.manager

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.text.TextUtils
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener3
import com.umeng.analytics.MobclickAgent
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.controller.MessageController
import framework.telegram.support.tools.file.DirManager
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.tools.AESHelper
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.MD5
import framework.telegram.support.tools.download.DownloadManager
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.utils.FileUtils
import java.io.File
import java.lang.Exception

object ForwardMessageManager {

    fun getForwardMessageTitle(msgModel: MessageModel): String {
        return when {
            msgModel.type == MessageModel.MESSAGE_TYPE_TEXT ->
                msgModel.content
            msgModel.type == MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE ->
                BaseApp.app.getString(R.string.string_image_tag)
            msgModel.type == MessageModel.MESSAGE_TYPE_VOICE ->
                BaseApp.app.getString(R.string.voice_sign)
            msgModel.type == MessageModel.MESSAGE_TYPE_IMAGE ->
                BaseApp.app.getString(R.string.picture_sign)
            msgModel.type == MessageModel.MESSAGE_TYPE_VIDEO ->
                BaseApp.app.getString(R.string.video_sign)
            msgModel.type == MessageModel.MESSAGE_TYPE_NAMECARD ->
                BaseApp.app.getString(R.string.business_card_sign)
            msgModel.type == MessageModel.MESSAGE_TYPE_LOCATION ->
                BaseApp.app.getString(R.string.location_sign)
            msgModel.type == MessageModel.MESSAGE_TYPE_FILE ->
                BaseApp.app.getString(R.string.file_sign) + msgModel.fileMessageContentBean.name
            msgModel.type == MessageModel.MESSAGE_TYPE_NOTICE ->
                BaseApp.app.getString(R.string.group_of_announcement_sign)
            else ->
                ""
        }
    }

    fun getForwardFileTitle(type: String, path: String): String {
        return when (type) {
            "text/plain", "text/txt", "text/TXT" ->
                path
            "image/jpg", "image/png", "image/gif" ->
                BaseApp.app.getString(R.string.picture_sign)
            "video/mp4" ->
                BaseApp.app.getString(R.string.video_sign)
            else ->
                BaseApp.app.getString(R.string.file_sign)
        }
    }

    /**
     * 转发一条消息
     */
    fun forwardMessage(oldMsgModel: MessageModel, chatType: Int, targetId: Long) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        when {
            oldMsgModel.snapchatTime > 0 -> {//阅后即焚
                forwardTextMessage(myUid, chatType, targetId, BaseApp.app.getString(R.string.snapchatTime_msg))
            }
            oldMsgModel.type == MessageModel.MESSAGE_TYPE_VOICE -> {//语音
                forwardTextMessage(myUid, chatType, targetId, BaseApp.app.getString(R.string.voice_sign))
            }
            oldMsgModel.type == MessageModel.MESSAGE_TYPE_NOTICE -> {//群公告
                forwardTextMessage(myUid, chatType, targetId, BaseApp.app.getString(R.string.group_of_announcement_sign))
            }
            oldMsgModel.type == MessageModel.MESSAGE_TYPE_STREAM -> {//音视频通话
                forwardTextMessage(myUid, chatType, targetId, BaseApp.app.getString(R.string.stream_msg))
            }
            oldMsgModel.type == MessageModel.MESSAGE_TYPE_TEXT -> {
                //转发文本消息
                forwardTextMessage(myUid, chatType, targetId, oldMsgModel.textMessageContent)
            }
            oldMsgModel.type == MessageModel.MESSAGE_TYPE_NAMECARD -> {
                //转发名片
                forwardNameCardMessage(myUid, chatType, targetId, oldMsgModel.nameCardMessageContent.uid)
            }
            oldMsgModel.type == MessageModel.MESSAGE_TYPE_LOCATION -> {
                //转发位置消息
                val lat = oldMsgModel.locationMessageContentBean.lat
                val lng = oldMsgModel.locationMessageContentBean.lng
                val address = oldMsgModel.locationMessageContentBean.address
                forwardLocationMessage(myUid, chatType, targetId, lat, lng, address)
            }
            else -> {
                // 收到的消息，或者是已上传附件的消息，直接转发，无需再次上传下载
                when (oldMsgModel.type) {
                    MessageModel.MESSAGE_TYPE_IMAGE -> {
                        //转发图片消息
                        val imageUri: Uri? = UriUtils.parseUri(oldMsgModel.imageMessageContent?.imageFileUri)
                        val imageThumbUri: Uri? = UriUtils.parseUri(oldMsgModel.imageMessageContent?.imageThumbFileUri)
                        if (imageUri != null && imageUri != Uri.EMPTY) {
                            val imageWidth: Int = oldMsgModel.imageMessageContent?.width ?: 0
                            val imageHeight: Int = oldMsgModel.imageMessageContent?.height ?: 0
                            forwardImageMessageNoDownload(myUid, chatType, targetId, imageUri.toString(), imageThumbUri.toString(), imageWidth, imageHeight, oldMsgModel.attachmentKey)
                        } else {
                            val imageBackupUri: Uri? = UriUtils.parseUri(oldMsgModel.imageMessageContent?.imageFileBackupUri)
                            if (imageBackupUri != null && imageBackupUri != Uri.EMPTY && File(imageBackupUri.path).exists()) {
                                //本地存在缓存文件
                                forwardImageMessage(myUid, chatType, targetId, File(imageBackupUri.path))
                            } else {
                                //本地不存在缓存文件，也不存在已上传文件，忽略转发
                            }
                        }
                    }
                    MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE -> {
                        //转发图片消息
                        val imageUri: Uri? = UriUtils.parseUri(oldMsgModel.dynamicImageMessageBean?.imageFileUri)
                        val imageThumbUri: Uri? = UriUtils.parseUri(oldMsgModel.dynamicImageMessageBean?.imageThumbFileUri)
                        val imageBackupUri: Uri? = UriUtils.parseUri(oldMsgModel.dynamicImageMessageBean?.imageFileBackupUri)
                        val imageWidth: Int = oldMsgModel.dynamicImageMessageBean?.width ?: 0
                        val imageHeight: Int = oldMsgModel.dynamicImageMessageBean?.height ?: 0
                        val size: Long = oldMsgModel.dynamicImageMessageBean?.size ?: 0
                        val emoticonId = oldMsgModel.dynamicImageMessageBean.emoticonId
                        if (imageUri != null && imageUri != Uri.EMPTY) {
                            forwardDynamicImageMessageNoDownload(myUid, chatType, targetId, emoticonId, imageUri.toString(), imageThumbUri.toString(), imageBackupUri.toString(), imageWidth, imageHeight, oldMsgModel.attachmentKey, size)
                        } else {
                            if (imageBackupUri != null && imageBackupUri != Uri.EMPTY && File(imageBackupUri.path).exists()) {
                                //本地存在缓存文件
                                forwardDynamicImageMessage(myUid, chatType, targetId, emoticonId, File(imageBackupUri.path))
                            } else {
                                //本地不存在缓存文件，也不存在已上传文件，忽略转发
                            }
                        }
                    }
                    MessageModel.MESSAGE_TYPE_VIDEO -> {
                        //转发视频消息
                        val videoUri: Uri? = UriUtils.parseUri(oldMsgModel.videoMessageContent?.videoFileUri)
                        val videoThumbUri: Uri? = UriUtils.parseUri(oldMsgModel.videoMessageContent?.videoThumbFileUri)
                        val videoBackupUri: Uri? = UriUtils.parseUri(oldMsgModel.videoMessageContent?.videoFileBackupUri)
                        val videoWidth: Int = oldMsgModel.videoMessageContent?.width ?: 0
                        val videoHeight: Int = oldMsgModel.videoMessageContent?.height ?: 0
                        val videoDuration: Int = oldMsgModel.videoMessageContent?.videoTime ?: 0
                        if (videoUri != null && videoUri != Uri.EMPTY) {
                            forwardVideoMessageNoDownload(myUid, chatType, targetId, videoUri.toString(), videoThumbUri.toString(), videoWidth, videoHeight, videoDuration, oldMsgModel.attachmentKey)
                        } else {
                            if (videoBackupUri != null && videoBackupUri != Uri.EMPTY && File(videoBackupUri.path).exists()) {
                                //本地存在缓存文件
                                forwardVideoMessage(myUid, chatType, targetId, File(videoBackupUri.path))
                            } else {
                                //本地不存在缓存文件，也不存在已上传文件，忽略转发
                            }
                        }
                    }
                    MessageModel.MESSAGE_TYPE_FILE -> {
                        //转发文件消息
                        val size = oldMsgModel.fileMessageContentBean.size
                        val fileUri: Uri? = UriUtils.parseUri(oldMsgModel.fileMessageContentBean.fileUri)
                        val name = oldMsgModel.fileMessageContentBean.name
                        val mimeType = oldMsgModel.fileMessageContentBean.mimeType
                        if (fileUri != null && fileUri != Uri.EMPTY) {
                            forwardFileMessageNoDownload(myUid, chatType, targetId, fileUri.toString(), name, size, mimeType, oldMsgModel.attachmentKey)
                        } else {
                            val fileBackupUri = UriUtils.parseUri(oldMsgModel.fileMessageContentBean?.fileBackupUri)
                            if (fileBackupUri != Uri.EMPTY && File(fileBackupUri.path).exists()) {
                                //本地存在缓存文件
                                forwardFileMessage(myUid, chatType, targetId, fileBackupUri.path!!, mimeType)
                            } else {
                                //本地不存在缓存文件，也不存在已上传文件，忽略转发
                            }
                        }
                    }
                }
            }
        }
    }

    private fun forwardTextMessage(myUid: Long, chatType: Int, targetId: Long, text: String) {
        if (chatType == ChatModel.CHAT_TYPE_PVT) {
            ArouterServiceManager.contactService.getContactInfo(null, targetId, { contactInfo, _ ->
                SendMessageManager.sendTextMessageToUser(text, null, myUid, targetId, contactInfo)
            }, {
                SendMessageManager.sendTextMessageToUser(text, null, myUid, targetId)
            })
        } else if (chatType == ChatModel.CHAT_TYPE_GROUP) {
            ArouterServiceManager.groupService.getGroupInfo(null, targetId, { groupInfo, _ ->
                SendMessageManager.sendTextMessageToGroup(text, null, null, myUid, targetId, groupInfo)
            }, {
                SendMessageManager.sendTextMessageToGroup(text, null, null, myUid, targetId)
            })
        }
    }

    private fun forwardNameCardMessage(myUid: Long, chatType: Int, targetId: Long, uid: Long) {
        if (chatType == ChatModel.CHAT_TYPE_PVT) {
            ArouterServiceManager.contactService.getContactInfo(null, targetId, { contactInfo, _ ->
                SendMessageManager.sendNameCardMessageToUser(uid, null, myUid, targetId, contactInfo)
            }, {
                SendMessageManager.sendNameCardMessageToUser(uid, null, myUid, targetId)
            })
        } else if (chatType == ChatModel.CHAT_TYPE_GROUP) {
            ArouterServiceManager.groupService.getGroupInfo(null, targetId, { groupInfo, _ ->
                SendMessageManager.sendNameCardMessageToGroup(uid, null, myUid, targetId, groupInfo)
            }, {
                SendMessageManager.sendNameCardMessageToGroup(uid, null, myUid, targetId)
            })
        }
    }

    private fun forwardLocationMessage(myUid: Long, chatType: Int, targetId: Long, lat: Long, lng: Long, address: String) {
        if (chatType == ChatModel.CHAT_TYPE_PVT) {
            ArouterServiceManager.contactService.getContactInfo(null, targetId, { contactInfo, _ ->
                SendMessageManager.sendLocationMessageToUser(lat, lng, address, null, myUid, targetId, contactInfo)
            }, {
                SendMessageManager.sendLocationMessageToUser(lat, lng, address, null, myUid, targetId)
            })
        } else if (chatType == ChatModel.CHAT_TYPE_GROUP) {
            ArouterServiceManager.groupService.getGroupInfo(null, targetId, { groupInfo, _ ->
                SendMessageManager.sendLocationMessageToGroup(lat, lng, address, null, myUid, targetId, groupInfo)
            }, {
                SendMessageManager.sendLocationMessageToGroup(lat, lng, address, null, myUid, targetId)
            })

        }
    }

    private fun forwardFileMessage(myUid: Long, chatType: Int, targetId: Long, filePath: String, mimeType: String) {
        if (chatType == ChatModel.CHAT_TYPE_PVT) {
            ArouterServiceManager.contactService.getContactInfo(null, targetId, { contactInfo, _ ->
                SendMessageManager.sendFileMessageToUser(filePath, mimeType, null, myUid, targetId, contactInfo)
            }, {
                SendMessageManager.sendFileMessageToUser(filePath, mimeType, null, myUid, targetId)
            })
        } else if (chatType == ChatModel.CHAT_TYPE_GROUP) {
            ArouterServiceManager.groupService.getGroupInfo(null, targetId, { groupInfo, _ ->
                SendMessageManager.sendFileMessageToGroup(filePath, mimeType, null, myUid, targetId, groupInfo)
            }, {
                SendMessageManager.sendFileMessageToGroup(filePath, mimeType, null, myUid, targetId)
            })
        }
    }

    private fun forwardImageMessage(myUid: Long, chatType: Int, targetId: Long, imageFile: File) {
        if (chatType == ChatModel.CHAT_TYPE_PVT) {
            ArouterServiceManager.contactService.getContactInfo(null, targetId, { contactInfo, _ ->
                SendMessageManager.sendImageMessageToUser(imageFile.absolutePath, null, myUid, targetId, contactInfo)
            }, {
                SendMessageManager.sendImageMessageToUser(imageFile.absolutePath, null, myUid, targetId)
            })
        } else if (chatType == ChatModel.CHAT_TYPE_GROUP) {
            ArouterServiceManager.groupService.getGroupInfo(null, targetId, { groupInfo, _ ->
                SendMessageManager.sendImageMessageToGroup(imageFile.absolutePath, null, myUid, targetId, groupInfo)
            }, {
                SendMessageManager.sendImageMessageToGroup(imageFile.absolutePath, null, myUid, targetId)
            })
        }
    }

    private fun forwardDynamicImageMessage(myUid: Long, chatType: Int, targetId: Long, emoticonId: Long, imageFile: File) {
        if (chatType == ChatModel.CHAT_TYPE_PVT) {
            ArouterServiceManager.contactService.getContactInfo(null, targetId, { contactInfo, _ ->
                SendMessageManager.sendDynamicImageMessageToUser(emoticonId, imageFile.absolutePath, null, myUid, targetId, contactInfo)
            }, {
                SendMessageManager.sendDynamicImageMessageToUser(emoticonId, imageFile.absolutePath, null, myUid, targetId)
            })
        } else if (chatType == ChatModel.CHAT_TYPE_GROUP) {
            ArouterServiceManager.groupService.getGroupInfo(null, targetId, { groupInfo, _ ->
                SendMessageManager.sendDynamicImageMessageToGroup(emoticonId, imageFile.absolutePath, null, myUid, targetId, groupInfo)
            }, {
                SendMessageManager.sendDynamicImageMessageToGroup(emoticonId, imageFile.absolutePath, null, myUid, targetId)
            })
        }
    }

    private fun forwardVideoMessage(myUid: Long, chatType: Int, targetId: Long, videoFile: File) {
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(BaseApp.app, Uri.fromFile(videoFile))

            // 获取视频尺寸和时长
            val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    ?: 0 //时长(毫秒)
            val rotation = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()
                    ?: 0//方向
            var width = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                    ?: 0//宽
            var height = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                    ?: 0//高
            if (rotation == 90 || rotation == 270) {
                val tmp = width
                width = height
                height = tmp
            }

            // 生成视频缩略图
            val firstFrame = mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
            val videoThumbFile = File(videoFile.absolutePath + "___thumb")
            FileUtils.saveBitmap(firstFrame, videoThumbFile)

            if (chatType == ChatModel.CHAT_TYPE_PVT) {
                ArouterServiceManager.contactService.getContactInfo(null, targetId, { contactInfo, _ ->
                    SendMessageManager.sendVideoMessageToUser(videoFile, videoThumbFile, width, height, (duration / 1000).toInt(), null, myUid, targetId, contactInfo)
                }, {
                    SendMessageManager.sendVideoMessageToUser(videoFile, videoThumbFile, width, height, (duration / 1000).toInt(), null, myUid, targetId)
                })
            } else if (chatType == ChatModel.CHAT_TYPE_GROUP) {
                ArouterServiceManager.groupService.getGroupInfo(null, targetId, { groupInfo, _ ->
                    SendMessageManager.sendVideoMessageToGroup(videoFile, videoThumbFile, width, height, (duration / 1000).toInt(), null, myUid, targetId, groupInfo)
                }, {
                    SendMessageManager.sendVideoMessageToGroup(videoFile, videoThumbFile, width, height, (duration / 1000).toInt(), null, myUid, targetId)
                })
            }
        } catch (ex: Exception) {
            BaseApp.app.toast(BaseApp.app.getString(R.string.video_conversion_has_an_exception))
            AppLogcat.logger.e(ex)
            MobclickAgent.reportError(BaseApp.app, ex)
        } finally {
            mmr.release()
        }
    }

    private fun forwardImageMessageNoDownload(myUid: Long, chatType: Int, targetId: Long, imageUrl: String, imageThumbUrl: String, imageWidth: Int, imageHeight: Int, attachmentKey: String) {
        getSnapchatTime(chatType, targetId) { snapchatTime ->
            val msgModel = MessageModel.createForwardImageMessage(imageUrl, imageThumbUrl, imageWidth, imageHeight, attachmentKey,
                    ArouterServiceManager.messageService.getCurrentTime(), myUid, targetId, chatType, snapchatTime)
            MessageController.saveMessage(myUid, targetId, chatType, msgModel) {
                SendMessageManager.sendMessagePackage(chatType, myUid, it)
            }
        }
    }

    private fun forwardDynamicImageMessageNoDownload(myUid: Long, chatType: Int, targetId: Long, emoticonId: Long, imageUrl: String, imageThumbUrl: String, imageFileBackUri: String, imageWidth: Int, imageHeight: Int, attachmentKey: String, size: Long) {
        getSnapchatTime(chatType, targetId) { snapchatTime ->
            val msgModel = MessageModel.createForwardDynamicImageMessage(emoticonId, imageUrl, imageThumbUrl, imageFileBackUri, imageWidth, imageHeight, attachmentKey,
                    ArouterServiceManager.messageService.getCurrentTime(), myUid, targetId, size, chatType, snapchatTime)
            MessageController.saveMessage(myUid, targetId, chatType, msgModel) {
                SendMessageManager.sendMessagePackage(chatType, myUid, it)
            }
        }
    }

    private fun forwardVideoMessageNoDownload(myUid: Long, chatType: Int, targetId: Long, videoUrl: String, videoThumbUrl: String, videoWidth: Int, videoHeight: Int, duration: Int, attachmentKey: String) {
        getSnapchatTime(chatType, targetId) { snapchatTime ->
            val msgModel = MessageModel.createForwardVideoMessage(videoUrl, videoThumbUrl, videoWidth, videoHeight, duration, attachmentKey, ArouterServiceManager.messageService.getCurrentTime(), myUid, targetId, chatType, snapchatTime)
            MessageController.saveMessage(myUid, targetId, chatType, msgModel) {
                SendMessageManager.sendMessagePackage(chatType, myUid, it)
            }
        }
    }

    private fun forwardFileMessageNoDownload(myUid: Long, chatType: Int, targetId: Long, fileUrl: String, name: String, size: Long, mimeType: String, attachmentKey: String) {
        getSnapchatTime(chatType, targetId) { snapchatTime ->
            val msgModel = MessageModel.createForwardFileMessage(name, size, mimeType, fileUrl, attachmentKey, ArouterServiceManager.messageService.getCurrentTime(), myUid, targetId, chatType, snapchatTime)
            MessageController.saveMessage(myUid, targetId, chatType, msgModel) {
                SendMessageManager.sendMessagePackage(chatType, myUid, it)
            }
        }
    }

    private fun downloadFile(secretKey: String?, downloadUri: Uri, saveFile: File, chatType: Int, msgModel: MessageModel, complete: (String) -> Unit) {
        val downloadUrl = downloadUri.toString()
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val targetId = if (myUid == msgModel.senderId) msgModel.targetId else msgModel.senderId
        DownloadManager.download("${chatType}_${myUid}_${targetId}_${msgModel.id}", downloadUrl, saveFile, object : DownloadListener3() {
            override fun warn(task: DownloadTask) {

            }

            override fun connected(task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long) {

            }

            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {

            }

            override fun started(task: DownloadTask) {

            }

            override fun error(task: DownloadTask, e: Exception) {

            }

            override fun canceled(task: DownloadTask) {

            }

            override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {

            }

            override fun completed(task: DownloadTask) {
                val file = task.file
                file?.let {
                    val realFile = File(file.absolutePath.replace("___download", ""))
                    file.renameTo(realFile)

                    if (TextUtils.isEmpty(secretKey)) {
                        // 下载并解密完成
                        decryptComplete(realFile)
                    } else {
                        // 重命名下载的文件
                        val encryptFile = File("${realFile.absolutePath}___encrypt")
                        realFile.renameTo(encryptFile)

                        try {
                            // 解密下载的文件，并使用原下载路径作为解密文件的路径
                            val result = AESHelper.decryptFile(secretKey, encryptFile.absolutePath, realFile.absolutePath, null)
                            if (result) {
                                // 删除加密文件
                                FileUtils.deleteQuietly(encryptFile)

                                // 下载并解密完成
                                decryptComplete(realFile)
                            } else {
                                // 删除加密文件
                                FileUtils.deleteQuietly(encryptFile)
                            }
                        } catch (e: Exception) {
                            // 删除加密文件
                            FileUtils.deleteQuietly(encryptFile)
                        }
                    }
                }
            }

            private fun decryptComplete(file: File?) {
                complete.invoke(file?.absolutePath ?: "")
            }
        })
    }

    private fun getSnapchatTime(chatType: Int, targetId: Long, complete: ((Int) -> Unit)) {//snapchatTime
        var snapchatTime = 0
        if (chatType == ChatModel.CHAT_TYPE_PVT) {
            ArouterServiceManager.contactService.getContactInfo(null, targetId, { contactInfo, _ ->
                if (contactInfo.isBfReadCancel)
                    snapchatTime = contactInfo.msgCancelTime
                complete.invoke(snapchatTime)
            })
        } else if (chatType == ChatModel.CHAT_TYPE_GROUP) {
            ArouterServiceManager.groupService.getGroupInfo(null, targetId, { groupInfo, _ ->
                if (groupInfo.bfGroupReadCancel)
                    snapchatTime = groupInfo.groupMsgCancelTime
                complete.invoke(snapchatTime)
            })
        }
    }
}
