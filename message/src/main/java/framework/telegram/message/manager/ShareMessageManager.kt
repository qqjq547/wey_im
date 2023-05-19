package framework.telegram.message.manager

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.text.TextUtils
import com.umeng.analytics.MobclickAgent
import framework.ideas.common.model.im.ChatModel
import framework.telegram.message.R
import framework.telegram.support.BaseApp
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.utils.FileUtils
import java.io.File


/*
    分享消息管理
    .jpg、png、gif：以图片消息类型进行转发
    .mp4 以视频消息类型进行转发
    其他后缀：以文件消息类型进行转发
 */
object ShareMessageManager {

    fun shareMessage(filePath: String, mimeType: String, senderUid: Long, recverId: Long, chaterType: Int) {
        if (!TextUtils.isEmpty(filePath))
            when (mimeType) {
                "text/plain" -> {
                    sendTextMessage(filePath, senderUid, recverId, chaterType)
                }
                "image/jpg", "image/png" -> {
                    sendImageMessage(filePath, senderUid, recverId, chaterType)
                }
                "image/gif" -> {
                    sendDynamicImageMessage(filePath, senderUid, recverId, chaterType)
                }
                "video/mp4" -> {
                    sendVideoMessage(filePath, senderUid, recverId, chaterType)
                }

                else -> {
                    sendFileMessage(filePath, mimeType, senderUid, recverId, chaterType)
                }
            }
    }

    private fun sendTextMessage(msg: String, senderUid: Long, recverId: Long, chaterType: Int) {
        if (ChatModel.CHAT_TYPE_PVT == chaterType) {
            ArouterServiceManager.contactService.getContactInfo(null, recverId, { contactInfo, _ ->
                SendMessageManager.sendTextMessageToUser(msg, null, senderUid, recverId, contactInfo)
            }, {
                SendMessageManager.sendTextMessageToUser(msg, null, senderUid, recverId)
            })
        } else if (ChatModel.CHAT_TYPE_GROUP == chaterType) {
            ArouterServiceManager.groupService.getGroupInfo(null, recverId, { groupInfo, _ ->
                SendMessageManager.sendTextMessageToGroup(msg, null, null, senderUid, recverId, groupInfo)
            }, {
                SendMessageManager.sendTextMessageToGroup(msg, null, null, senderUid, recverId)
            })
        }
    }

    private fun sendFileMessage(filePath: String, mimeType: String, senderUid: Long, recverId: Long, chaterType: Int) {
        if (ChatModel.CHAT_TYPE_PVT == chaterType) {
            ArouterServiceManager.contactService.getContactInfo(null, recverId, { contactInfo, _ ->
                SendMessageManager.sendFileMessageToUser(filePath, mimeType, null, senderUid, recverId, contactInfo)
            }, {
                SendMessageManager.sendFileMessageToUser(filePath, mimeType, null, senderUid, recverId)
            })
        } else if (ChatModel.CHAT_TYPE_GROUP == chaterType) {
            ArouterServiceManager.groupService.getGroupInfo(null, recverId, { groupInfo, _ ->
                SendMessageManager.sendFileMessageToGroup(filePath, mimeType, null, senderUid, recverId, groupInfo)
            }, {
                SendMessageManager.sendFileMessageToGroup(filePath, mimeType, null, senderUid, recverId)
            })
        }
    }

    private fun sendImageMessage(filePath: String, senderUid: Long, recverId: Long, chaterType: Int) {
        if (ChatModel.CHAT_TYPE_PVT == chaterType) {
            ArouterServiceManager.contactService.getContactInfo(null, recverId, { contactInfo, _ ->
                SendMessageManager.sendImageMessageToUser(filePath, null, senderUid, recverId, contactInfo)
            }, {
                SendMessageManager.sendImageMessageToUser(filePath, null, senderUid, recverId)
            })
        } else if (ChatModel.CHAT_TYPE_GROUP == chaterType) {
            ArouterServiceManager.groupService.getGroupInfo(null, recverId, { groupInfo, _ ->
                SendMessageManager.sendImageMessageToGroup(filePath, null, senderUid, recverId, groupInfo)
            }, {
                SendMessageManager.sendImageMessageToGroup(filePath, null, senderUid, recverId)
            })
        }
    }

    private fun sendDynamicImageMessage(filePath: String, senderUid: Long, recverId: Long, chaterType: Int) {
        if (ChatModel.CHAT_TYPE_PVT == chaterType) {
            ArouterServiceManager.contactService.getContactInfo(null, recverId, { contactInfo, _ ->
                SendMessageManager.sendDynamicImageMessageToUser(0, filePath, null, senderUid, recverId, contactInfo)
            }, {
                SendMessageManager.sendDynamicImageMessageToUser(0, filePath, null, senderUid, recverId)
            })
        } else if (ChatModel.CHAT_TYPE_GROUP == chaterType) {
            ArouterServiceManager.groupService.getGroupInfo(null, recverId, { groupInfo, _ ->
                SendMessageManager.sendDynamicImageMessageToGroup(0, filePath, null, senderUid, recverId, groupInfo)
            }, {
                SendMessageManager.sendDynamicImageMessageToGroup(0, filePath, null, senderUid, recverId)
            })
        }
    }

    private fun sendVideoMessage(filePath: String, senderUid: Long, recverId: Long, chaterType: Int) {
        val mmr = MediaMetadataRetriever()
        try {
            val videoFile: File = File(filePath)
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

            if (chaterType == ChatModel.CHAT_TYPE_PVT) {
                ArouterServiceManager.contactService.getContactInfo(null, recverId, { contactInfo, _ ->
                    SendMessageManager.sendVideoMessageToUser(videoFile, videoThumbFile, width, height, (duration / 1000).toInt(), null, senderUid, recverId, contactInfo)
                }, {
                    SendMessageManager.sendVideoMessageToUser(videoFile, videoThumbFile, width, height, (duration / 1000).toInt(), null, senderUid, recverId)
                })
            } else if (chaterType == ChatModel.CHAT_TYPE_GROUP) {
                ArouterServiceManager.groupService.getGroupInfo(null, recverId, { groupInfo, _ ->
                    SendMessageManager.sendVideoMessageToGroup(videoFile, videoThumbFile, width, height, (duration / 1000).toInt(), null, senderUid, recverId, groupInfo)
                }, {
                    SendMessageManager.sendVideoMessageToGroup(videoFile, videoThumbFile, width, height, (duration / 1000).toInt(), null, senderUid, recverId)
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


}