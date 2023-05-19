package framework.telegram.message.manager

import android.annotation.SuppressLint
import android.net.Uri
import android.text.TextUtils
import com.im.domain.pb.FriendMessageProto
import com.im.pb.IMPB
import com.umeng.analytics.MobclickAgent
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.ideas.common.rlog.RLogManager
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.MessageStateChangeEvent
import framework.telegram.message.connect.MessageSocketService
import framework.telegram.message.connect.bean.SocketPackageBean
import framework.telegram.message.controller.MessageController
import framework.telegram.message.exception.SendMessageFailException
import framework.telegram.message.http.getClientInfo
import framework.telegram.message.manager.upload.UploadManager
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.AESHelper
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.file.DirManager
import framework.telegram.support.tools.wordfilter.WordFilter
import framework.telegram.ui.utils.BitmapUtils
import framework.telegram.ui.utils.ScreenUtils
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import top.zibin.luban.Luban
import yourpet.client.android.lame.LameUtil
import java.io.File
import java.util.concurrent.TimeUnit

object SendMessageManager {

    fun generateAttachmentKey(chatType: Int, targetId: Long, ignore: Boolean = false): String {
        return if (ignore) {
            AESHelper.generatePassword(16)
        } else if (chatType == ChatModel.CHAT_TYPE_PVT && targetId < Constant.Common.SYSTEM_USER_MAX_UID && targetId != Constant.Common.FILE_TRANSFER_UID) {
            ""
        } else {
            AESHelper.generatePassword(16)
        }
    }

    fun sendTextMessageToGroup(
        msg: String,
        atUids: List<Long>?,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetId: Long,
        groupInfoModel: GroupInfoModel? = null
    ) {
        sendTextMessage(
            ChatModel.CHAT_TYPE_GROUP,
            atUids,
            msg,
            refMessageBean,
            myUid,
            targetId,
            groupInfoModel = groupInfoModel
        )
    }

    fun sendTextMessageToUser(
        msg: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel? = null,
        isGroupSend: Boolean = false
    ) {
        sendTextMessage(
            ChatModel.CHAT_TYPE_PVT,
            null,
            msg,
            refMessageBean,
            myUid,
            targetUid,
            contactDataModel = contactDataModel,
            isGroupSend = isGroupSend
        )
    }

    private fun sendTextMessage(
        chatType: Int,
        atUids: List<Long>?,
        msg: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        groupInfoModel: GroupInfoModel? = null,
        contactDataModel: ContactDataModel? = null,
        isGroupSend: Boolean = false
    ) {
        val atUidsValue = atUids?.toLongArray() ?: ArrayList<Long>().toLongArray()
        val msgModel = MessageModel.createTextMessage(
            WordFilter.doFilter(msg),
            ArouterServiceManager.messageService.getCurrentTime(),
            atUidsValue,
            refMessageBean,
            if (contactDataModel?.isBfReadCancel == true) contactDataModel.msgCancelTime else
                (if (groupInfoModel?.bfGroupReadCancel == true) groupInfoModel.groupMsgCancelTime else 0),
            myUid,
            targetUid,
            chatType
        )
        MessageController.saveMessage(
            myUid,
            targetUid,
            chatType,
            msgModel,
            isGroupSend,
            contactDataModel
        ) {
            sendMessagePackage(
                chatType,
                myUid,
                it,
                groupInfoModel,
                contactDataModel,
                isEncrypt = !isGroupSend
            )
        }
    }

    fun sendNameCardMessageToGroup(
        uid: Long,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        groupInfoModel: GroupInfoModel? = null
    ) {
        sendNameCardMessage(
            ChatModel.CHAT_TYPE_GROUP,
            uid,
            refMessageBean,
            myUid,
            targetUid,
            groupInfoModel = groupInfoModel
        )
    }

    fun sendNameCardMessageToUser(
        uid: Long,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel? = null,
        isGroupSend: Boolean = false
    ) {
        sendNameCardMessage(
            ChatModel.CHAT_TYPE_PVT,
            uid,
            refMessageBean,
            myUid,
            targetUid,
            contactDataModel = contactDataModel,
            isGroupSend = isGroupSend
        )
    }

    private fun sendNameCardMessage(
        chatType: Int,
        uid: Long,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        groupInfoModel: GroupInfoModel? = null,
        contactDataModel: ContactDataModel? = null,
        isGroupSend: Boolean = false
    ) {
        // 获取个人信息
        ArouterServiceManager.contactService.getContactInfo(null, uid, { contactInfoModel, _ ->
            val msgModel = MessageModel.createNameCardMessage(
                uid,
                contactInfoModel.nickName,
                contactInfoModel.icon,
                contactInfoModel.identify,
                ArouterServiceManager.messageService.getCurrentTime(),
                refMessageBean,
                if (contactDataModel?.isBfReadCancel == true) contactDataModel.msgCancelTime else
                    (if (groupInfoModel?.bfGroupReadCancel == true) groupInfoModel.groupMsgCancelTime else 0),
                myUid,
                targetUid,
                chatType
            )
            MessageController.saveMessage(
                myUid,
                targetUid,
                chatType,
                msgModel,
                isGroupSend,
                contactDataModel
            ) {
                sendMessagePackage(
                    chatType,
                    myUid,
                    it,
                    groupInfoModel,
                    contactDataModel,
                    isEncrypt = !isGroupSend
                )
            }
        })
    }

    fun sendVoiceMessageToGroup(
        recordTime: Int,
        attachment: File,
        highDArr: IntArray,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetId: Long,
        groupInfoModel: GroupInfoModel? = null
    ) {
        sendVoiceMessage(
            ChatModel.CHAT_TYPE_GROUP,
            recordTime,
            attachment,
            highDArr,
            refMessageBean,
            myUid,
            targetId,
            generateAttachmentKey(ChatModel.CHAT_TYPE_GROUP, targetId),
            groupInfoModel = groupInfoModel
        )
    }

    fun sendVoiceMessageToUser(
        recordTime: Int,
        attachment: File,
        highDArr: IntArray,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel? = null
    ) {
        sendVoiceMessage(
            ChatModel.CHAT_TYPE_PVT,
            recordTime,
            attachment,
            highDArr,
            refMessageBean,
            myUid,
            targetUid,
            generateAttachmentKey(ChatModel.CHAT_TYPE_PVT, targetUid),
            contactDataModel = contactDataModel
        )
    }

    private fun sendVoiceMessage(
        chatType: Int,
        recordTime: Int,
        attachment: File,
        highDArr: IntArray,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        attachmentKey: String,
        groupInfoModel: GroupInfoModel? = null,
        contactDataModel: ContactDataModel? = null
    ) {
        val msgModel = MessageModel.createVoiceMessage(
            recordTime,
            Uri.fromFile(attachment).toString(),
            highDArr,
            attachmentKey,
            ArouterServiceManager.messageService.getCurrentTime(),
            refMessageBean,
            if (contactDataModel?.isBfReadCancel == true) contactDataModel.msgCancelTime else
                (if (groupInfoModel?.bfGroupReadCancel == true) groupInfoModel.groupMsgCancelTime else 0),
            myUid,
            targetUid,
            chatType
        )
        MessageController.saveMessage(
            myUid,
            targetUid,
            chatType,
            msgModel,
            false,
            contactDataModel
        ) { savedMsgModel ->
            // 转换成mp3
            val msgLocalId = savedMsgModel.id
            compressVoice(attachment, { mp3File ->
                MessageController.executeChatTransactionAsyncWithResult(
                    chatType,
                    myUid,
                    targetUid,
                    { realm ->
                        val targetModel =
                            realm.where(MessageModel::class.java).equalTo("id", msgLocalId)
                                .findFirst()
                        targetModel?.let {
                            val voiceContent = targetModel.voiceMessageContent
                            voiceContent.recordFileBackupUri = Uri.fromFile(mp3File).toString()
                            targetModel.voiceMessageContent = voiceContent
                            targetModel.status = MessageModel.STATUS_ATTACHMENT_UPLOADING
                            realm.copyToRealmOrUpdate(targetModel)
                            targetModel.copyMessage()
                        }
                    },
                    { preSendMsg ->
                        preSendMsg?.let {
                            // 开始发送socket
                            UploadManager.uploadMsgAttachment(
                                chatType,
                                preSendMsg
                            ) { uploadedMsgModel ->
                                uploadedMsgModel?.let {
                                    sendMessagePackage(
                                        chatType,
                                        myUid,
                                        uploadedMsgModel,
                                        groupInfoModel,
                                        contactDataModel
                                    )
                                }
                            }
                        }
                    })
            }, {
                //压缩音频失败
                sendMessageFail(chatType, myUid, msgModel)
                MobclickAgent.reportError(BaseApp.app, it)
            })
        }
    }

    @SuppressLint("CheckResult")
    fun compressVoice(voiceFile: File, next: (File) -> Unit, error: ((Throwable) -> Unit)? = null) {
        Flowable.just<File>(voiceFile)
            .subscribeOn(Schedulers.single())
            .map {
                if (voiceFile.name.endsWith(".mp3")) {
                    voiceFile
                } else {
                    // 压制成mp3
                    val mp3FileName = "${System.currentTimeMillis()}.mp3"
                    val mp3File = File(
                        DirManager.getVoiceCacheDir(
                            BaseApp.app,
                            AccountManager.getLoginAccountUUid()
                        ), mp3FileName
                    )
                    val lameUtil = LameUtil(1, 8000, 48)
                    lameUtil.raw2mp3(voiceFile, mp3File)
                    lameUtil.destroyEncoder()
                    voiceFile.delete()
                    mp3File
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                next.invoke(it)
            }, {
                error?.invoke(it)
            })
    }

    fun sendImageMessageToGroup(
        imageFilePath: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetId: Long,
        groupInfoModel: GroupInfoModel? = null
    ) {
        sendImageMessage(
            ChatModel.CHAT_TYPE_GROUP,
            imageFilePath,
            refMessageBean,
            myUid,
            targetId,
            generateAttachmentKey(ChatModel.CHAT_TYPE_GROUP, targetId),
            groupInfoModel = groupInfoModel
        )
    }

    fun sendImageMessageToUser(
        imageFilePath: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel? = null
    ) {
        sendImageMessage(
            ChatModel.CHAT_TYPE_PVT,
            imageFilePath,
            refMessageBean,
            myUid,
            targetUid,
            generateAttachmentKey(ChatModel.CHAT_TYPE_PVT, targetUid),
            contactDataModel = contactDataModel
        )
    }

    private fun sendImageMessage(
        chatType: Int,
        imageFilePath: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        attachmentKey: String,
        groupInfoModel: GroupInfoModel? = null,
        contactDataModel: ContactDataModel? = null
    ) {
        // 获取图片尺寸
        val imageSize = BitmapUtils.getImageSize(imageFilePath)
        val msgModel = MessageModel.createImageMessage(
            "",
            "",
            imageSize?.first() ?: 0,
            imageSize?.last() ?: 0,
            attachmentKey,
            ArouterServiceManager.messageService.getCurrentTime(),
            refMessageBean,
            if (contactDataModel?.isBfReadCancel == true) contactDataModel.msgCancelTime else
                (if (groupInfoModel?.bfGroupReadCancel == true) groupInfoModel.groupMsgCancelTime else 0),
            myUid,
            targetUid,
            chatType
        )
        MessageController.saveMessage(
            myUid,
            targetUid,
            chatType,
            msgModel,
            false,
            contactDataModel
        ) { savedMsgModel ->
            // 生成图片缩略图
            val msgLocalId = savedMsgModel.id
            compressImage(imageFilePath, { resizeImageFile ->
                val maxThumbSize = ScreenUtils.dp2px(BaseApp.app, 240.0f)
                val thumbFileName = resizeImageFile.absolutePath + "___thumb"
                val resizeImageThumbPath = BitmapUtils.revitionImageSize(
                    resizeImageFile.absolutePath,
                    thumbFileName,
                    maxThumbSize,
                    maxThumbSize
                )
//                FileHelper.insertImageToGallery(BaseApp.app, File(resizeImageThumbPath))
                // 获取图片尺寸
                val resizeImageSize = BitmapUtils.getImageSize(resizeImageFile.absolutePath)
                MessageController.executeChatTransactionAsyncWithResult(
                    chatType,
                    myUid,
                    targetUid,
                    { realm ->
                        val targetModel =
                            realm.where(MessageModel::class.java).equalTo("id", msgLocalId)
                                .findFirst()
                        targetModel?.let {
                            val imageContent = targetModel.imageMessageContent
                            imageContent.imageFileBackupUri =
                                Uri.fromFile(resizeImageFile).toString()
                            imageContent.imageThumbFileBackupUri =
                                Uri.fromFile(File(resizeImageThumbPath)).toString()
                            imageContent.width = resizeImageSize?.first() ?: 0
                            imageContent.height = resizeImageSize?.last() ?: 0
                            targetModel.imageMessageContent = imageContent
                            targetModel.status = MessageModel.STATUS_ATTACHMENT_UPLOADING
                            realm.copyToRealm(targetModel)
                            targetModel.copyMessage()
                        }
                    },
                    { preSendMsg ->
                        preSendMsg?.let {
                            //开始发送socket
                            UploadManager.uploadMsgAttachment(
                                chatType,
                                preSendMsg
                            ) { uploadedMsgModel ->
                                uploadedMsgModel?.let {
                                    sendMessagePackage(
                                        chatType,
                                        myUid,
                                        uploadedMsgModel,
                                        groupInfoModel,
                                        contactDataModel
                                    )
                                }
                            }
                        }
                    })
            }, {
                //压缩图片失败
                sendMessageFail(chatType, myUid, msgModel)
                MobclickAgent.reportError(BaseApp.app, it)
            })
        }
    }

    fun sendDynamicImageMessageToGroup(
        emoticonId: Long,
        imageFileUrl: String,
        width: Int,
        height: Int,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetId: Long,
        groupInfoModel: GroupInfoModel? = null
    ) {
        sendDynamicImageMessage(
            ChatModel.CHAT_TYPE_GROUP,
            emoticonId,
            imageFileUrl,
            width,
            height,
            refMessageBean,
            myUid,
            targetId,
            "",
            groupInfoModel = groupInfoModel
        )
    }

    fun sendDynamicImageMessageToUser(
        emoticonId: Long,
        imageFileUrl: String,
        width: Int,
        height: Int,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel? = null
    ) {
        sendDynamicImageMessage(
            ChatModel.CHAT_TYPE_PVT,
            emoticonId,
            imageFileUrl,
            width,
            height,
            refMessageBean,
            myUid,
            targetUid,
            "",
            contactDataModel = contactDataModel
        )
    }

    private fun sendDynamicImageMessage(
        chatType: Int,
        emoticonId: Long,
        imageFileUrl: String,
        width: Int,
        height: Int,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        attachmentKey: String,
        groupInfoModel: GroupInfoModel? = null,
        contactDataModel: ContactDataModel? = null
    ) {
        val msgModel = MessageModel.createDynamicImageMessage(
            emoticonId,
            imageFileUrl,
            "",
            width,
            height,
            attachmentKey,
            ArouterServiceManager.messageService.getCurrentTime(),
            refMessageBean,
            if (contactDataModel?.isBfReadCancel == true) contactDataModel.msgCancelTime else
                (if (groupInfoModel?.bfGroupReadCancel == true) groupInfoModel.groupMsgCancelTime else 0),
            myUid,
            targetUid,
            chatType
        )
        MessageController.saveMessage(
            myUid,
            targetUid,
            chatType,
            msgModel,
            false,
            contactDataModel
        ) { savedMsgModel ->
            MessageController.executeChatTransactionAsyncWithResult(
                chatType,
                myUid,
                targetUid,
                { realm ->
                    val targetModel =
                        realm.where(MessageModel::class.java).equalTo("id", savedMsgModel.id)
                            .findFirst()
                    targetModel?.let {
                        targetModel.status = MessageModel.STATUS_SENDING
                        realm.copyToRealm(targetModel)
                        targetModel.copyMessage()
                    }
                },
                { preSendMsg ->
                    preSendMsg?.let {
                        //开始发送socket
                        sendMessagePackage(
                            chatType,
                            myUid,
                            preSendMsg,
                            groupInfoModel,
                            contactDataModel
                        )
                    }
                })
        }
    }

    fun sendDynamicImageMessageToGroup(
        emoticonId: Long,
        imageFilePath: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetId: Long,
        groupInfoModel: GroupInfoModel? = null
    ) {
        sendDynamicImageMessage(
            ChatModel.CHAT_TYPE_GROUP,
            emoticonId,
            imageFilePath,
            refMessageBean,
            myUid,
            targetId,
            generateAttachmentKey(ChatModel.CHAT_TYPE_GROUP, targetId),
            groupInfoModel = groupInfoModel
        )
    }

    fun sendDynamicImageMessageToUser(
        emoticonId: Long,
        imageFilePath: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel? = null
    ) {
        sendDynamicImageMessage(
            ChatModel.CHAT_TYPE_PVT,
            emoticonId,
            imageFilePath,
            refMessageBean,
            myUid,
            targetUid,
            generateAttachmentKey(ChatModel.CHAT_TYPE_PVT, targetUid),
            contactDataModel = contactDataModel
        )
    }

    @SuppressLint("CheckResult")
    private fun sendDynamicImageMessage(
        chatType: Int,
        emoticonId: Long,
        imageFilePath: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        attachmentKey: String,
        groupInfoModel: GroupInfoModel? = null,
        contactDataModel: ContactDataModel? = null
    ) {
        // 获取图片尺寸
        val imageFile = File(imageFilePath)
        val imageSize = BitmapUtils.getImageSize(imageFile.absolutePath)
        val msgModel = MessageModel.createDynamicImageMessage(
            emoticonId,
            Uri.fromFile(imageFile).toString(),
            Uri.fromFile(imageFile).toString(),
            imageSize?.first() ?: 0,
            imageSize?.last() ?: 0,
            attachmentKey,
            ArouterServiceManager.messageService.getCurrentTime(),
            refMessageBean,
            if (contactDataModel?.isBfReadCancel == true) contactDataModel.msgCancelTime else
                (if (groupInfoModel?.bfGroupReadCancel == true) groupInfoModel.groupMsgCancelTime else 0),
            myUid,
            targetUid,
            chatType
        )
        MessageController.saveMessage(
            myUid,
            targetUid,
            chatType,
            msgModel,
            false,
            contactDataModel
        ) { savedMsgModel ->
            // 生成动图缩略图
            val msgLocalId = savedMsgModel.id
            val resizeImageSize = BitmapUtils.getImageSize(imageFile.absolutePath)
            MessageController.executeChatTransactionAsyncWithResult(
                chatType,
                myUid,
                targetUid,
                { realm ->
                    val targetModel =
                        realm.where(MessageModel::class.java).equalTo("id", msgLocalId).findFirst()
                    targetModel?.let {
                        val imageContent = targetModel.dynamicImageMessageBean
                        imageContent.imageFileBackupUri = Uri.fromFile(imageFile).toString()
                        //todo gif 没有thumb
//                  imageContent.imageThumbFileBackupUri = Uri.fromFile(imageFile).toString()
                        imageContent.width = resizeImageSize?.first() ?: 0
                        imageContent.height = resizeImageSize?.last() ?: 0
                        imageContent.size = imageFile.length()
                        targetModel.dynamicImageMessageBean = imageContent
                        targetModel.status = MessageModel.STATUS_ATTACHMENT_UPLOADING
                        realm.copyToRealm(targetModel)
                        targetModel.copyMessage()
                    }
                },
                { preSendMsg ->
                    preSendMsg?.let {
                        //开始发送socket
                        UploadManager.uploadMsgAttachment(
                            chatType,
                            preSendMsg
                        ) { uploadedMsgModel ->
                            uploadedMsgModel?.let {
                                sendMessagePackage(
                                    chatType,
                                    myUid,
                                    uploadedMsgModel,
                                    groupInfoModel,
                                    contactDataModel
                                )
                            }
                        }
                    }
                })
        }
    }

    @SuppressLint("CheckResult")
    fun compressImage(imageFilePath: String, next: (File) -> Unit, error: ((Throwable) -> Unit)?) {
        Flowable.just<Any>(imageFilePath)
            .subscribeOn(Schedulers.io())
            .map {
                Luban.with(BaseApp.app).ignoreBy(1024)//1M以内不压缩
                    .setFocusAlpha(false)//不保留透明通道
                    .load(it as String).get().first()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                next.invoke(it)
            }, {
                //图片压缩失败
                error?.invoke(it)
            })
    }

    fun sendVideoMessageToGroup(
        videoFile: File,
        videoThumbFile: File,
        width: Int,
        height: Int,
        duration: Int,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetId: Long,
        groupInfoModel: GroupInfoModel? = null
    ) {
        sendVideoMessage(
            ChatModel.CHAT_TYPE_GROUP,
            videoFile,
            videoThumbFile,
            width,
            height,
            duration,
            refMessageBean,
            myUid,
            targetId,
            generateAttachmentKey(ChatModel.CHAT_TYPE_GROUP, targetId),
            groupInfoModel = groupInfoModel
        )
    }

    fun sendVideoMessageToUser(
        videoFile: File,
        videoThumbFile: File,
        width: Int,
        height: Int,
        duration: Int,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel? = null
    ) {
        sendVideoMessage(
            ChatModel.CHAT_TYPE_PVT,
            videoFile,
            videoThumbFile,
            width,
            height,
            duration,
            refMessageBean,
            myUid,
            targetUid,
            generateAttachmentKey(ChatModel.CHAT_TYPE_PVT, targetUid),
            contactDataModel = contactDataModel
        )
    }

    private fun sendVideoMessage(
        chatType: Int,
        videoFile: File,
        videoThumbFile: File,
        width: Int,
        height: Int,
        duration: Int,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        attachmentKey: String,
        groupInfoModel: GroupInfoModel? = null,
        contactDataModel: ContactDataModel? = null
    ) {
        val msgModel = MessageModel.createVideoMessage(
            Uri.fromFile(videoFile).toString(),
            Uri.fromFile(videoThumbFile).toString(),
            width,
            height,
            duration,
            attachmentKey,
            ArouterServiceManager.messageService.getCurrentTime(),
            refMessageBean,
            if (contactDataModel?.isBfReadCancel == true) contactDataModel.msgCancelTime else
                (if (groupInfoModel?.bfGroupReadCancel == true) groupInfoModel.groupMsgCancelTime else 0),
            myUid,
            targetUid,
            chatType
        )
        MessageController.saveMessage(
            myUid,
            targetUid,
            chatType,
            msgModel,
            false,
            contactDataModel
        ) { savedMsgModel ->
            val msgLocalId = savedMsgModel.id
            MessageController.executeChatTransactionAsyncWithResult(
                chatType,
                myUid,
                targetUid,
                { realm ->
                    val targetModel =
                        realm.where(MessageModel::class.java).equalTo("id", msgLocalId).findFirst()
                    targetModel?.let {
                        targetModel.status = MessageModel.STATUS_ATTACHMENT_UPLOADING
                        realm.copyToRealm(targetModel)
                        targetModel.copyMessage()
                    }
                },
                { preSendMsg ->
                    preSendMsg?.let {
                        //开始发送socket
                        UploadManager.uploadMsgAttachment(
                            chatType,
                            preSendMsg
                        ) { uploadedMsgModel ->
                            uploadedMsgModel?.let {
                                sendMessagePackage(
                                    chatType,
                                    myUid,
                                    uploadedMsgModel,
                                    groupInfoModel,
                                    contactDataModel
                                )
                            }
                        }
                    }
                })
        }
    }

    fun sendLocationMessageToGroup(
        lat: Long,
        lng: Long,
        address: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        groupInfoModel: GroupInfoModel? = null
    ) {
        sendLocationMessage(
            ChatModel.CHAT_TYPE_GROUP,
            lat,
            lng,
            address,
            refMessageBean,
            myUid,
            targetUid,
            groupInfoModel = groupInfoModel
        )
    }

    fun sendLocationMessageToUser(
        lat: Long,
        lng: Long,
        address: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel? = null,
        isGroupSend: Boolean = false
    ) {
        sendLocationMessage(
            ChatModel.CHAT_TYPE_PVT,
            lat,
            lng,
            address,
            refMessageBean,
            myUid,
            targetUid,
            contactDataModel = contactDataModel,
            isGroupSend = isGroupSend
        )
    }

    private fun sendLocationMessage(
        chatType: Int,
        lat: Long,
        lng: Long,
        address: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        groupInfoModel: GroupInfoModel? = null,
        contactDataModel: ContactDataModel? = null,
        isGroupSend: Boolean = false
    ) {
        val msgModel = MessageModel.createLocationMessage(
            lat,
            lng,
            address,
            ArouterServiceManager.messageService.getCurrentTime(),
            refMessageBean,
            if (contactDataModel?.isBfReadCancel == true) contactDataModel.msgCancelTime else
                (if (groupInfoModel?.bfGroupReadCancel == true) groupInfoModel.groupMsgCancelTime else 0),
            myUid,
            targetUid,
            chatType
        )
        MessageController.saveMessage(
            myUid,
            targetUid,
            chatType,
            msgModel,
            isGroupSend,
            contactDataModel
        ) {
            sendMessagePackage(
                chatType,
                myUid,
                it,
                groupInfoModel,
                contactDataModel,
                isEncrypt = !isGroupSend
            )
        }
    }

    fun sendFileMessageToGroup(
        filePath: String,
        mimeType: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetId: Long,
        groupInfoModel: GroupInfoModel? = null
    ) {
        sendFileMessage(
            ChatModel.CHAT_TYPE_GROUP,
            filePath,
            mimeType,
            refMessageBean,
            myUid,
            targetId,
            generateAttachmentKey(ChatModel.CHAT_TYPE_GROUP, targetId),
            groupInfoModel = groupInfoModel
        )
    }

    fun sendFileMessageToUser(
        filePath: String,
        mimeType: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel? = null
    ) {
        sendFileMessage(
            ChatModel.CHAT_TYPE_PVT,
            filePath,
            mimeType,
            refMessageBean,
            myUid,
            targetUid,
            generateAttachmentKey(ChatModel.CHAT_TYPE_PVT, targetUid),
            contactDataModel = contactDataModel
        )
    }

    private fun sendFileMessage(
        chatType: Int,
        filePath: String,
        mimeType: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        attachmentKey: String,
        groupInfoModel: GroupInfoModel? = null,
        contactDataModel: ContactDataModel? = null
    ) {
        val file = File(filePath)
        val msgModel = MessageModel.createFileMessage(
            file.name,
            file.length(),
            mimeType,
            Uri.fromFile(file).toString(),
            attachmentKey,
            ArouterServiceManager.messageService.getCurrentTime(),
            refMessageBean,
            if (contactDataModel?.isBfReadCancel == true) contactDataModel.msgCancelTime else
                (if (groupInfoModel?.bfGroupReadCancel == true) groupInfoModel.groupMsgCancelTime else 0),
            myUid,
            targetUid,
            chatType
        )
        MessageController.saveMessage(
            myUid,
            targetUid,
            chatType,
            msgModel,
            false,
            contactDataModel
        ) { savedMsgModel ->
            val msgLocalId = savedMsgModel.id
            MessageController.executeChatTransactionAsyncWithResult(
                chatType,
                myUid,
                targetUid,
                { realm ->
                    val targetModel =
                        realm.where(MessageModel::class.java).equalTo("id", msgLocalId).findFirst()
                    targetModel?.let {
                        targetModel.status = MessageModel.STATUS_ATTACHMENT_UPLOADING
                        realm.copyToRealm(targetModel)
                        targetModel.copyMessage()
                    }
                },
                { preSendMsg ->
                    preSendMsg?.let {
                        //开始发送socket
                        UploadManager.uploadMsgAttachment(
                            chatType,
                            preSendMsg
                        ) { uploadedMsgModel ->
                            uploadedMsgModel?.let {
                                sendMessagePackage(
                                    chatType,
                                    myUid,
                                    uploadedMsgModel,
                                    groupInfoModel,
                                    contactDataModel
                                )
                            }
                        }
                    }
                })
        }
    }

    fun resendMessage(chatType: Int, msgModel: MessageModel) {
        val copyMsgModel = msgModel.copyMessage()
        MessageController.executeChatTransactionAsyncWithResult(
            chatType,
            copyMsgModel.senderId,
            copyMsgModel.targetId,
            { realm ->
                val msg = realm.where(MessageModel::class.java).equalTo("flag", copyMsgModel.flag)
                    .findFirst()
                msg?.let {
                    if (it.hasAttachment()) {
                        it.status = MessageModel.STATUS_ATTACHMENT_UPLOADING
                    } else {
                        it.status = MessageModel.STATUS_SENDING
                    }
                    it.time = ArouterServiceManager.messageService.getCurrentTime()
                    it.isRetry = 1
                    realm.copyToRealmOrUpdate(it)
                    msg.copyMessage()
                }
            },
            { findMsgModel ->
                findMsgModel?.let {
                    if (copyMsgModel.hasAttachment()) {
                        UploadManager.uploadMsgAttachment(
                            chatType,
                            findMsgModel
                        ) { uploadedMsgModel ->
                            uploadedMsgModel?.let {
                                sendMessagePackage(
                                    chatType,
                                    copyMsgModel.senderId,
                                    uploadedMsgModel
                                )
                            }
                        }
                    } else {
                        sendMessagePackage(chatType, copyMsgModel.senderId, copyMsgModel)
                    }
                }
            })
    }

    /**
     * 发送消息数据包
     */
    fun sendMessagePackage(
        chatType: Int,
        myUid: Long,
        msgModel: MessageModel,
        groupInfo: GroupInfoModel? = null,
        contactInfo: ContactDataModel? = null,
        isEncrypt: Boolean = true
    ) {
        when (chatType) {
            ChatModel.CHAT_TYPE_PVT -> {
                //判断是否开启阅后即焚
                if (contactInfo != null) {
                    if (contactInfo.isBfReadCancel) {
                        sendMessagePackageReal(
                            chatType,
                            myUid,
                            msgModel,
                            contactInfo.msgCancelTime,
                            isEncrypt
                        )
                    } else {
                        sendMessagePackageReal(chatType, myUid, msgModel, isEncrypt = isEncrypt)
                    }
                } else {
                    ArouterServiceManager.contactService.getContactInfo(
                        null,
                        msgModel.targetId,
                        { contactInfo, _ ->
                            if (contactInfo.isBfReadCancel) {
                                sendMessagePackageReal(
                                    chatType,
                                    myUid,
                                    msgModel,
                                    contactInfo.msgCancelTime,
                                    isEncrypt
                                )
                            } else {
                                sendMessagePackageReal(
                                    chatType,
                                    myUid,
                                    msgModel,
                                    isEncrypt = isEncrypt
                                )
                            }
                        },
                        {
                            sendMessagePackageReal(chatType, myUid, msgModel, isEncrypt = isEncrypt)
                        })
                }
            }
            ChatModel.CHAT_TYPE_GROUP -> {
                // 判断群是否禁言
                if (groupInfo != null) {
                    if (groupInfo.forShutupGroup && groupInfo.memberRole > 1) {
                        sendMessageFail(chatType, myUid, msgModel)
                    } else if (groupInfo.bfGroupReadCancel) {
                        sendMessagePackageReal(
                            chatType,
                            myUid,
                            msgModel,
                            groupInfo.groupMsgCancelTime,
                            isEncrypt
                        )
                    } else {
                        sendMessagePackageReal(chatType, myUid, msgModel, isEncrypt = isEncrypt)
                    }
                } else {
                    ArouterServiceManager.groupService.getGroupInfo(
                        null,
                        msgModel.targetId,
                        { groupInfo, _ ->
                            if (groupInfo.forShutupGroup && groupInfo.memberRole > 1) {
                                sendMessageFail(chatType, myUid, msgModel)
                            } else if (groupInfo.bfGroupReadCancel) {
                                sendMessagePackageReal(
                                    chatType,
                                    myUid,
                                    msgModel,
                                    groupInfo.groupMsgCancelTime,
                                    isEncrypt
                                )
                            } else {
                                sendMessagePackageReal(
                                    chatType,
                                    myUid,
                                    msgModel,
                                    isEncrypt = isEncrypt
                                )
                            }
                        },
                        {
                            sendMessagePackageReal(chatType, myUid, msgModel, isEncrypt = isEncrypt)
                        })
                }
            }
            else -> sendMessageFail(chatType, myUid, msgModel)
        }
    }

    private fun sendMessagePackageReal(
        chatType: Int,
        myUid: Long,
        msgModel: MessageModel,
        snapchatTime: Int = 0,
        isEncrypt: Boolean = true
    ) {
        if (chatType == ChatModel.CHAT_TYPE_PVT) {
            if (msgModel.targetId < Constant.Common.SYSTEM_USER_MAX_UID) {
                // 系统用户
                if (msgModel.targetId != Constant.Common.FILE_TRANSFER_UID) {
                    // 不是传输助手消息不加密
                    sendPvtMessagePackageUnDetrypt(msgModel, snapchatTime)
                } else {
                    // 是传输助手消息使用自己的webkey加密即可，对方的解密key全部填null
                    sendPvtMessagePackage(
                        null,
                        null,
                        0,
                        msgModel.attachmentKey,
                        msgModel,
                        snapchatTime
                    )
                }
            } else {
                // 普通用户
                if (isEncrypt) {
                    // 需要加密
                    ArouterServiceManager.systemService.getUserSecretKey(
                        msgModel.targetId,
                        appVer = 0,
                        webVer = 0,
                        complete = { sk, _, webSk, _ ->
                            if (TextUtils.isEmpty(sk) && TextUtils.isEmpty(webSk)) {
                                // 没获取到对方的key，发送失败
                                MobclickAgent.reportError(
                                    BaseApp.app,
                                    "SendMessageManager--->sendMessagePackageReal.Group.getGroupSecretKey sk and webSk is null"
                                )
                                sendMessageFail(chatType, myUid, msgModel)
                            } else {
                                // 获取到了对方的key
                                val myKeyVersion =
                                    ArouterServiceManager.systemService.getAccountKeyVersion(
                                        msgModel.senderId
                                    )

                                sendPvtMessagePackage(
                                    sk,
                                    webSk,
                                    myKeyVersion,
                                    msgModel.attachmentKey,
                                    msgModel,
                                    snapchatTime
                                )
                            }
                        },
                        error = {
                            MobclickAgent.reportError(
                                BaseApp.app,
                                "SendMessageManager--->sendMessagePackageReal.User.getUserSecretKey  ${it.localizedMessage}"
                            )
                            sendMessageFail(chatType, myUid, msgModel)
                        })
                } else {
                    // 不需要加密
                    sendPvtMessagePackage(
                        null,
                        null,
                        0,
                        msgModel.attachmentKey,
                        msgModel,
                        snapchatTime
                    )
                }
            }

            //发送消息时候的状态
            EventBus.publishEvent(MessageStateChangeEvent(mutableListOf(msgModel.copyMessage())))
        } else if (chatType == ChatModel.CHAT_TYPE_GROUP) {
            if (isEncrypt) {
                // 需要加密
                ArouterServiceManager.systemService.getGroupSecretKey(
                    msgModel.targetId,
                    { sk, version ->
                        if (TextUtils.isEmpty(sk)) {
                            // 没获取到群的key，发送失败
                            MobclickAgent.reportError(
                                BaseApp.app,
                                "SendMessageManager--->sendMessagePackageReal.Group.getGroupSecretKey sk is null"
                            )
                            sendMessageFail(chatType, myUid, msgModel)
                        } else {
                            // 获取到了群的key
                            sendGroupMessagePackage(
                                sk,
                                version,
                                msgModel.attachmentKey,
                                msgModel,
                                snapchatTime
                            )
                        }
                    },
                    {
                        MobclickAgent.reportError(
                            BaseApp.app,
                            "SendMessageManager--->sendMessagePackageReal.Group.getGroupSecretKey  ${it.localizedMessage}"
                        )
                        sendMessageFail(chatType, myUid, msgModel)
                    })
            } else {
                // 不需要加密
                sendGroupMessagePackage(
                    "",
                    0,
                    msgModel.attachmentKey,
                    msgModel,
                    snapchatTime
                )
            }
        }

    }

    private fun sendMessageFail(chatType: Int, myUid: Long, msgModel: MessageModel) {
        MessageController.executeChatTransactionAsyncWithResult(
            chatType,
            myUid,
            msgModel.targetId,
            { realm ->
                val targetModel =
                    realm.where(MessageModel::class.java).equalTo("id", msgModel.id).findFirst()
                targetModel?.let {
                    targetModel.status = MessageModel.STATUS_SEND_FAIL
                    realm.copyToRealmOrUpdate(targetModel)
                }

                targetModel?.copyMessage()
            },
            {
                //发送错误信息 的状态
                it?.let { msg ->
                    EventBus.publishEvent(MessageStateChangeEvent(mutableListOf(msg)))
                }
            })
    }

    private fun sendPvtMessagePackage(
        appSecretKey: String? = null,
        webSecretKey: String? = null,
        myKeyVersion: Int = 0,
        attachmentKey: String? = null,
        msgModel: MessageModel,
        snapchatTime: Int = 0,
        complete: (() -> Unit)? = null,
        error: (() -> Unit)? = null
    ) {
        ArouterServiceManager.systemService.getLoginAccountWebSecretKey { myWebSecretKey, _ ->
            val socketPackageBean = SocketPackageBean.toOneToOneMsgSocketPackage(
                appSecretKey,
                webSecretKey,
                myWebSecretKey,
                myKeyVersion,
                attachmentKey,
                msgModel,
                snapchatTime
            )
            sendMessagePackageByImpl(
                ChatModel.CHAT_TYPE_PVT,
                socketPackageBean,
                msgModel.senderId,
                msgModel.targetId,
                msgModel.flag,
                complete,
                error
            )
        }
    }

    private fun sendGroupMessagePackage(
        secretKey: String,
        keyVersion: Int,
        attachmentKey: String?,
        msgModel: MessageModel,
        snapchatTime: Int = 0,
        complete: (() -> Unit)? = null,
        error: (() -> Unit)? = null
    ) {
        val socketPackageBean = SocketPackageBean.toGroupMsgSocketPackage(
            secretKey,
            keyVersion,
            attachmentKey,
            msgModel,
            snapchatTime
        )
        sendMessagePackageByImpl(
            ChatModel.CHAT_TYPE_GROUP,
            socketPackageBean,
            msgModel.senderId,
            msgModel.targetId,
            msgModel.flag,
            complete,
            error
        )
    }

    private fun sendPvtMessagePackageUnDetrypt(
        msgModel: MessageModel,
        snapchatTime: Int = 0,
        complete: (() -> Unit)? = null,
        error: (() -> Unit)? = null
    ) {
        val socketPackageBean =
            SocketPackageBean.toOneToOneMsgSocketPackageUnDetrypt(msgModel, snapchatTime)
        sendMessagePackageByImpl(
            ChatModel.CHAT_TYPE_PVT,
            socketPackageBean,
            msgModel.senderId,
            msgModel.targetId,
            msgModel.flag,
            complete,
            error
        )
    }

    private fun sendGroupMessagePackageUnDetrypt(
        msgModel: MessageModel,
        complete: (() -> Unit)? = null,
        error: (() -> Unit)? = null
    ) {
        val socketPackageBean = SocketPackageBean.toGroupMsgSocketPackageUnDetrypt(msgModel)
        sendMessagePackageByImpl(
            ChatModel.CHAT_TYPE_GROUP,
            socketPackageBean,
            msgModel.senderId,
            msgModel.targetId,
            msgModel.flag,
            complete,
            error
        )
    }

    @SuppressLint("CheckResult")
    private fun sendMessagePackageByImpl(
        chatType: Int,
        socketPackageBean: SocketPackageBean?,
        senderUid: Long,
        targetId: Long,
        flag: String,
        complete: (() -> Unit)? = null,
        error: (() -> Unit)? = null
    ) {
        if (socketPackageBean != null) {
            var retryCouter = -1
            Observable.just(socketPackageBean).map {
                retryCouter++

                val result = sendMessagePackageToSocket(it)
                if (result) {
                    true
                } else {
                    throw SendMessageFailException()
                }
            }.retryWhen { throwableObservable ->
                throwableObservable.flatMap {
                    if (it is SendMessageFailException) {
                        if (retryCouter == 6 || !AccountManager.hasLoginAccount()) {
                            Observable.error(it)
                        } else {
                            Observable.timer(10, TimeUnit.SECONDS)
                        }
                    } else {
                        Observable.error(it)
                    }
                }
            }.subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread())
                .subscribe({ _ ->
                    MessageController.executeChatTransactionAsyncWithResult(
                        chatType,
                        senderUid,
                        targetId,
                        { realm ->
                            val msg = realm.where(MessageModel::class.java).equalTo("flag", flag)
                                .findFirst()
                            msg?.let {
                                if (msg.status == MessageModel.STATUS_SENDING) {
                                    msg.status = MessageModel.STATUS_SENDED_NO_RESP
                                }

                                realm.copyToRealmOrUpdate(msg)
                            }

                            msg?.copyMessage()
                        },
                        { msg ->
                            msg?.let {
                                EventBus.publishEvent(MessageStateChangeEvent(mutableListOf(msg)))
                            }

                            complete?.invoke()
                        },
                        {
                            MobclickAgent.reportError(
                                BaseApp.app,
                                "SendMessageManager--->sendMessagePackageByImpl ${it.localizedMessage}"
                            )
                            error?.invoke()
                        })
                }, {
                    MessageController.executeChatTransactionAsyncWithResult(
                        chatType,
                        senderUid,
                        targetId,
                        { realm ->
                            val msg = realm.where(MessageModel::class.java).equalTo("flag", flag)
                                .findFirst()
                            msg?.let {
                                if (msg.status == MessageModel.STATUS_SENDING) {
                                    msg.status = MessageModel.STATUS_SEND_FAIL
                                    realm.copyToRealmOrUpdate(msg)
                                }
                            }

                            msg?.copyMessage()
                        },
                        { msg ->
                            msg?.let {
                                EventBus.publishEvent(MessageStateChangeEvent(mutableListOf(msg)))
                            }

                            error?.invoke()
                        },
                        {
                            MobclickAgent.reportError(
                                BaseApp.app,
                                "SendMessageManager--->sendMessagePackageByImpl ${it.localizedMessage}"
                            )
                            error?.invoke()
                        })
                })
        } else {
            MessageController.executeChatTransactionAsyncWithResult(
                chatType,
                senderUid,
                targetId,
                { realm ->
                    val msg =
                        realm.where(MessageModel::class.java).equalTo("flag", flag).findFirst()
                    msg?.let {
                        if (msg.status == MessageModel.STATUS_SENDING) {
                            msg.status = MessageModel.STATUS_SEND_FAIL
//                        msg.time = ArouterServiceManager.messageService.getCurrentTime()
                            realm.copyToRealmOrUpdate(msg)
                        }
                    }

                    msg?.copyMessage()
                },
                { msg ->
                    msg?.let {
                        EventBus.publishEvent(MessageStateChangeEvent(mutableListOf(msg)))
                    }

                    error?.invoke()
                },
                {
                    MobclickAgent.reportError(
                        BaseApp.app,
                        "SendMessageManager--->sendMessagePackageByImpl ${it.localizedMessage}"
                    )
                    error?.invoke()
                })
        }
    }

    fun sendStreamRequestPackage(receiveUid: Long, streamType: IMPB.StreamType, secretKey: String) {
        val sessionMessage = IMPB.SendOneToOneStreamReq.newBuilder().setReceiveUid(receiveUid)
            .setStreamType(streamType).setSecretKey(secretKey).build()
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_SEND_ONE_TO_ONE_STREAM_REQ,
            sessionMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(MessageSocketService.TAG, "stream request package发送成功->$sessionMessage")
        } else {
            RLogManager.d(MessageSocketService.TAG, "stream request package发送失败")
        }
    }

    fun sendRefuseStreamRequestPackage(
        sessionId: String,
        openType: Int,
        myUid: Long,
        targetUid: Long,
        streamType: IMPB.StreamType
    ) {
        val sessionMessage = if (openType == 1) {
            //接收者
            IMPB.SendOneToOneStreamOperateMessage.newBuilder().setChannelName(sessionId)
                .setSendUid(targetUid).setReceiveUid(myUid).setStreamType(streamType)
                .setOperate(IMPB.StreamOperateType.refuse).build()
        } else {
            //发起者
            IMPB.SendOneToOneStreamOperateMessage.newBuilder().setChannelName(sessionId)
                .setSendUid(myUid).setReceiveUid(targetUid).setStreamType(streamType)
                .setOperate(IMPB.StreamOperateType.refuse).build()
        }
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_SEND_ONE_TO_ONE_STREAM_OPETARE_REQ,
            sessionMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(
                MessageSocketService.TAG,
                "stream refuse opetare request package发送成功->$sessionMessage"
            )
        } else {
            RLogManager.d(MessageSocketService.TAG, "stream refuse opetare request package发送失败")
        }
    }

    fun sendAgreeStreamRequestPackage(
        sessionId: String,
        openType: Int,
        myUid: Long,
        targetUid: Long,
        streamType: IMPB.StreamType
    ) {
        val sessionMessage = if (openType == 1) {
            IMPB.SendOneToOneStreamOperateMessage.newBuilder().setChannelName(sessionId)
                .setSendUid(targetUid).setReceiveUid(myUid).setChannelName(sessionId)
                .setStreamType(streamType).setOperate(IMPB.StreamOperateType.agree).build()
        } else {
            IMPB.SendOneToOneStreamOperateMessage.newBuilder().setChannelName(sessionId)
                .setSendUid(myUid).setReceiveUid(targetUid).setChannelName(sessionId)
                .setStreamType(streamType).setOperate(IMPB.StreamOperateType.agree).build()
        }
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_SEND_ONE_TO_ONE_STREAM_OPETARE_REQ,
            sessionMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(
                MessageSocketService.TAG,
                "stream agree opetare request package发送成功->$sessionMessage"
            )
        } else {
            RLogManager.d(MessageSocketService.TAG, "stream agree opetare request package发送失败")
        }
    }

    fun sendCancelStreamRequestPackage(
        sessionId: String,
        openType: Int,
        myUid: Long,
        targetUid: Long,
        streamType: IMPB.StreamType
    ) {
        val sessionMessage = if (openType == 1) {
            IMPB.SendOneToOneStreamOperateMessage.newBuilder().setChannelName(sessionId)
                .setSendUid(targetUid).setReceiveUid(myUid).setChannelName(sessionId)
                .setStreamType(streamType).setOperate(IMPB.StreamOperateType.cancel).build()
        } else {
            IMPB.SendOneToOneStreamOperateMessage.newBuilder().setChannelName(sessionId)
                .setSendUid(myUid).setReceiveUid(targetUid).setChannelName(sessionId)
                .setStreamType(streamType).setOperate(IMPB.StreamOperateType.cancel).build()
        }
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_SEND_ONE_TO_ONE_STREAM_OPETARE_REQ,
            sessionMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(
                MessageSocketService.TAG,
                "stream downloadCancel opetare request package发送成功->$sessionMessage"
            )
        } else {
            RLogManager.d(
                MessageSocketService.TAG,
                "stream downloadCancel opetare request package发送失败"
            )
        }
    }

    fun sendBusyNowStreamRequestPackage(
        sessionId: String,
        openType: Int,
        myUid: Long,
        targetUid: Long,
        streamType: IMPB.StreamType
    ) {
        val sessionMessage = if (openType == 1) {
            IMPB.SendOneToOneStreamOperateMessage.newBuilder().setChannelName(sessionId)
                .setSendUid(targetUid).setReceiveUid(myUid).setChannelName(sessionId)
                .setStreamType(streamType).setOperate(IMPB.StreamOperateType.calling).build()
        } else {
            IMPB.SendOneToOneStreamOperateMessage.newBuilder().setChannelName(sessionId)
                .setSendUid(myUid).setReceiveUid(targetUid).setChannelName(sessionId)
                .setStreamType(streamType).setOperate(IMPB.StreamOperateType.calling).build()
        }
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_SEND_ONE_TO_ONE_STREAM_OPETARE_REQ,
            sessionMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(
                MessageSocketService.TAG,
                "stream isActive opetare request package发送成功->$sessionMessage"
            )
        } else {
            RLogManager.d(MessageSocketService.TAG, "stream isActive opetare request package发送失败")
        }
    }

    fun sendOverStreamRequestPackage(sessionId: String, sendUid: Long, receiveUid: Long) {
        val sessionMessage = IMPB.SendOneToOneStreamOperateMessage.newBuilder().setSendUid(sendUid)
            .setReceiveUid(receiveUid).setChannelName(sessionId)
            .setOperate(IMPB.StreamOperateType.over).build()
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_SEND_ONE_TO_ONE_STREAM_OPETARE_REQ,
            sessionMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(
                MessageSocketService.TAG,
                "stream over opetare request package发送成功->$sessionMessage"
            )
        } else {
            RLogManager.d(MessageSocketService.TAG, "stream over opetare request package发送失败")
        }
    }

    fun sendRefreshTokenPackage(sessionId: String) {
        val sessionMessage = IMPB.RenewStreamToken.newBuilder().setChannelName(sessionId).build()
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_SEND_ONE_TO_ONE_STREAM_REFRESH_TOKEN_REQ,
            sessionMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(
                MessageSocketService.TAG,
                "stream refresh token request package发送成功->$sessionMessage"
            )
        } else {
            RLogManager.d(MessageSocketService.TAG, "stream refresh token request package发送失败")
        }
    }

    fun sendLoginMessagePackage(): Boolean {
        val loginSessionMessage =
            IMPB.LoginSessionMessage.newBuilder().setClinetInfo(getClientInfo())
                .setLatestLoginTime(System.currentTimeMillis()).build()
        val msg = SocketPackageBean(SocketPackageBean.MESSAGE_TYPE_LOGIN_REQ, loginSessionMessage)
        return if (sendMessagePackageToSocket(msg, false, false)) {
            RLogManager.d(MessageSocketService.TAG, "login package发送成功")
            true
        } else {
            RLogManager.d(MessageSocketService.TAG, "login package发送失败")
            false
        }
    }

    fun sendReceiptMessagePackage(receiptMsgs: List<IMPB.ReceiptMessage>): Boolean {
        if (receiptMsgs.isNullOrEmpty()) {
            return false
        }

        val loginSessionMessage =
            IMPB.SendReceiptMessage.newBuilder().addAllReceipts(receiptMsgs).build()
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_RECEIPT_MSG_COMMEND,
            loginSessionMessage
        )
        return if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(MessageSocketService.TAG, "msg receipt package发送成功")
            true
        } else {
            RLogManager.d(MessageSocketService.TAG, "msg receipt package发送失败")
            false
        }
    }

    fun sendRecvPvtReceiptMessagePackage(receiptMsgs: List<IMPB.ReceiptMessage>): Boolean {
        if (receiptMsgs.isEmpty()) {
            return false
        }

        val loginSessionMessage =
            IMPB.ReceiveReceiptMessage.newBuilder().addAllReceipts(receiptMsgs).build()
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_RECV_PVT_RECEIPT_MSG_COMMEND,
            loginSessionMessage
        )
        return if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(MessageSocketService.TAG, "recv msg receipt package发送成功")
            true
        } else {
            RLogManager.d(MessageSocketService.TAG, "recv msg receipt package发送失败")
            false
        }
    }

    fun sendRecvGroupReceiptMessagePackage(receiptMsgs: List<IMPB.ReceiptMessage>): Boolean {
        if (receiptMsgs.isEmpty()) {
            return false
        }

        val loginSessionMessage =
            IMPB.ReceiveReceiptMessage.newBuilder().addAllReceipts(receiptMsgs).build()
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_RECV_GROUP_RECEIPT_MSG_COMMEND,
            loginSessionMessage
        )
        return if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(MessageSocketService.TAG, "recv msg receipt package发送成功")
            true
        } else {
            RLogManager.d(MessageSocketService.TAG, "recv msg receipt package发送失败")
            false
        }
    }

    fun sendRecallPvtMessagePackage(msgId: Long, channelName: String? = "", receiveUid: Long) {
        val recallMessage = if (msgId > 0) {
            IMPB.RecallMessage.newBuilder().setClear(0).setMsgId(msgId).setMsgTargetId(receiveUid)
        } else if (!TextUtils.isEmpty(channelName)) {
            IMPB.RecallMessage.newBuilder().setClear(0).setChannelName(channelName)
                .setMsgTargetId(receiveUid)
        } else {
            null
        }

        recallMessage?.let {
            val loginSessionMessage =
                IMPB.SendRecallOneToOneMessage.newBuilder().setRecallOneToOneMessage(recallMessage)
                    .build()
            val msg = SocketPackageBean(
                SocketPackageBean.MESSAGE_TYPE_RECALL_PVT_MSG_COMMEND,
                loginSessionMessage
            )
            if (sendMessagePackageToSocket(msg)) {
                RLogManager.d(MessageSocketService.TAG, "recall pvt msg package发送成功")
            } else {
                RLogManager.d(MessageSocketService.TAG, "recall pvt msg package发送失败")
            }
        }
    }

    fun sendRecallPvtMessagesPackage(
        clear: Int,
        clearTime: Long,
        receiveUid: Long
    ) {
        val recallMessage =
            IMPB.RecallMessage.newBuilder().setClear(clear).setMsgId(-1).setClearTime(clearTime)
                .setMsgTargetId(receiveUid)
        val loginSessionMessage =
            IMPB.SendRecallOneToOneMessage.newBuilder().setRecallOneToOneMessage(recallMessage)
                .build()
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_RECALL_PVT_MSG_COMMEND,
            loginSessionMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(MessageSocketService.TAG, "recall pvt msgs package发送成功")
        } else {
            RLogManager.d(MessageSocketService.TAG, "recall pvt msgs package发送失败")
        }
    }

    fun sendRecallGroupMessagePackage(msgId: Long, groupId: Long) {
        val recallMessage =
            IMPB.RecallMessage.newBuilder().setClear(0).setMsgId(msgId).setMsgTargetId(groupId)
        val loginSessionMessage =
            IMPB.SendRecallGroupMessage.newBuilder().setRecallGroupMessage(recallMessage).build()
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_RECALL_GROUP_MSG_COMMEND,
            loginSessionMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(MessageSocketService.TAG, "recall group msg package发送成功")
        } else {
            RLogManager.d(MessageSocketService.TAG, "recall group msg package发送失败")
        }
    }

    fun sendRecallGroupMessagesPackage(
        clear: Int,
        clearTime: Long,
        groupId: Long
    ) {
        val recallMessage =
            IMPB.RecallMessage.newBuilder().setClear(clear).setMsgId(-1).setClearTime(clearTime)
                .setMsgTargetId(groupId)
        val loginSessionMessage =
            IMPB.SendRecallGroupMessage.newBuilder().setRecallGroupMessage(recallMessage).build()
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_RECALL_GROUP_MSG_COMMEND,
            loginSessionMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(MessageSocketService.TAG, "recall group msgs package发送成功")
        } else {
            RLogManager.d(MessageSocketService.TAG, "recall group msgs package发送失败")
        }
    }

    fun sendRecvRecallPvtMessagePackage(
        msgId: Long,
        channelName: String,
        clear: Int,
        clearTime: Long,
        receiveUid: Long
    ) {
        val recallMessage =
            IMPB.RecallMessage.newBuilder()
                .setMsgId(msgId).setChannelName(channelName)
                .setClear(clear).setClearTime(clearTime)
                .setMsgTargetId(receiveUid)
        val loginSessionMessage =
            IMPB.ReceiveRecallOneToOneMessage.newBuilder().addRecallOneToOneMessages(recallMessage)
                .build()
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_RECV_RECALL_PVT_MSG_COMMEND,
            loginSessionMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(MessageSocketService.TAG, "recv recall pvt msg package发送成功")
        } else {
            RLogManager.d(MessageSocketService.TAG, "recv recall pvt msg package发送失败")
        }
    }

    fun sendRecvRecallGroupMessagePackage(msgId: Long, groupId: Long) {
        val recallMessage =
            IMPB.RecallMessage.newBuilder().setClear(0).setMsgId(msgId).setMsgTargetId(groupId)
        val loginSessionMessage =
            IMPB.ReceiveRecallGroupMessage.newBuilder().addRecallGroupMessages(recallMessage)
                .build()
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_RECV_RECALL_GROUP_MSG_COMMEND,
            loginSessionMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(MessageSocketService.TAG, "recv recall group msg package发送成功")
        } else {
            RLogManager.d(MessageSocketService.TAG, "recv recall group msg package发送失败")
        }
    }

    fun sendRecvGroupOperateMessagesPackage(groupReqIds: List<String>) {
        val recallMessage = IMPB.ReceiveGroupReqMessage.newBuilder().addAllRIds(groupReqIds).build()
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_RECV_GROUP_OPERATE_MSG_COMMEND,
            recallMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(MessageSocketService.TAG, "recv group operate msg package发送成功")
        } else {
            RLogManager.d(MessageSocketService.TAG, "recv group operate msg package发送失败")
        }
    }

    fun sendRecvKeypairMessagesPackage(version: Int, sendUid: Long) {
        val recallMessage =
            IMPB.ReceiveKeyPairMessage.newBuilder().setSendUid(sendUid).setVersion(version).build()
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_RECV_KEYPAIR_CHANGE_COMMEND,
            recallMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(MessageSocketService.TAG, "recv keypair change msg package发送成功")
        } else {
            RLogManager.d(MessageSocketService.TAG, "recv keypair change msg package发送失败")
        }
    }

    fun sendRecvFriendRecordMessagesPackage(sendUid: Long, receiveUid: Long, type: Int) {
        val friendMessage = FriendMessageProto.FriendRecordMsgDto.newBuilder()
            .setSendUid(sendUid)
            .setReceiveUid(receiveUid)
            .setCreateTime(ArouterServiceManager.messageService.getCurrentTime())
            .setDoTypeValue(type)
            .build()
        val recallMessage =
            IMPB.SendReceiveFriendRecordMessage.newBuilder().addFriendRecordmsg(friendMessage)
                .build()
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_RECV_ADD_CONTACT_OPERATE_COMMEND,
            recallMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(MessageSocketService.TAG, "recv friend record msg package发送成功")
        } else {
            RLogManager.d(MessageSocketService.TAG, "recv friend record msg package发送失败")
        }
    }

    fun sendScreenShotsPackage(sendUid: Long, receiveUid: Long) {
        val friendMessage = FriendMessageProto.FriendRecordMsgDto.newBuilder()
            .setSendUid(sendUid)
            .setReceiveUid(receiveUid)
            .setCreateTime(ArouterServiceManager.messageService.getCurrentTime())
            .setDoType(FriendMessageProto.FriendDoType.SCREENSHOT)
            .build()
        val recallMessage =
            IMPB.SendScreenHotsMessage.newBuilder().setFreindMsg(friendMessage).build()
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_RECV_SCREEN_SHOTS_COMMEND,
            recallMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(MessageSocketService.TAG, "recv screenshots msg package发送成功")
        } else {
            RLogManager.d(MessageSocketService.TAG, "recv screenshots msg package发送失败")
        }
    }

    fun sendInputtingStatusPackage(receiveUid: Long) {
        val recallMessage =
            IMPB.SendOneToOneInputMessage.newBuilder().setReceiveUid(receiveUid).build()
        val msg = SocketPackageBean(
            SocketPackageBean.MESSAGE_TYPE_INPUTTING_STATUS_COMMEND,
            recallMessage
        )
        if (sendMessagePackageToSocket(msg)) {
            RLogManager.d(MessageSocketService.TAG, "inputting status package发送成功")
        } else {
            RLogManager.d(MessageSocketService.TAG, "inputting status package发送失败")
        }
    }

    fun sendLogoutMessagePackage() {
        val msg = SocketPackageBean(SocketPackageBean.MESSAGE_TYPE_LOGOUT_REQ)
        if (sendMessagePackageToSocket(msg, checkHeart = false)) {
            RLogManager.d(MessageSocketService.TAG, "logout package发送成功")
        } else {
            RLogManager.d(MessageSocketService.TAG, "logout package发送失败")
        }
    }

    fun sendHeartMessagePackage(): Boolean {
        val msg = SocketPackageBean(SocketPackageBean.MESSAGE_TYPE_HEART_REQ)
        return if (sendMessagePackageToSocket(msg, checkHeart = false)) {
            RLogManager.d(MessageSocketService.TAG, "heart package发送成功")
            true
        } else {
            RLogManager.d(MessageSocketService.TAG, "heart package发送失败")
            false
        }
    }

    private fun sendMessagePackageToSocket(
        msg: SocketPackageBean,
        checkSocketIsActive: Boolean = true,
        checkHeart: Boolean = true
    ): Boolean {
        try {
            if (checkSocketIsActive && !ReceiveMessageManager.socketIsLogin) {
                RLogManager.e(
                    MessageSocketService.TAG,
                    "ReceiveMessageManager.socketIsLogin is false"
                )
            } else {
                val future = MessageSocketService.getChannel()?.writeAndFlush(msg)?.sync()
                val result = future != null && future.isSuccess
                if (!result) {
                    RLogManager.e(
                        MessageSocketService.TAG,
                        "SendMessageManager--->future writeAndFlush is false"
                    )
                } else {
                    // 写成功
                    RLogManager.d(MessageSocketService.TAG, "SendMessageManager--->消息写入socket通道成功")
                    if (checkHeart && (System.currentTimeMillis() - ReceiveMessageManager.lastReceiveHeartTime) > 10000) {
                        MessageSocketService.checkConnectStatus()
                    }
                }

                return result
            }
        } catch (e: Exception) {
            RLogManager.e(
                MessageSocketService.TAG,
                "SendMessageManager--->sendMessagePackageToSocket",
                e
            )
        }
        return false
    }

    fun sendGroupNoticeMessageToGroup(
        noticeId: Long,
        noticeContent: String,
        showNotify: Boolean,
        myUid: Long,
        targetGid: Long
    ) {
        val msgModel = MessageModel.createNoticeMessage(
            noticeId,
            noticeContent,
            showNotify,
            ArouterServiceManager.messageService.getCurrentTime(),
            myUid,
            targetGid,
            ChatModel.CHAT_TYPE_GROUP
        )
        MessageController.saveMessage(myUid, targetGid, ChatModel.CHAT_TYPE_GROUP, msgModel) {
            sendMessagePackage(ChatModel.CHAT_TYPE_GROUP, myUid, it)
        }
    }

    /** ------------------------------------------------------群发相关------------------------------------------------------ **/
    /** 群发 图片 **/
    fun groupSendImageMessage(
        chatType: Int,
        attachmentKey: String,
        imageUrl: String,
        thumbUrl: String,
        resizeImageSize: IntArray?,
        myUid: Long,
        contactList: List<ContactDataModel>,
        complete: ((process: Int, complete: Boolean) -> Unit)?,
        error: ((String) -> Unit)
    ) {
        ThreadUtils.runOnIOThread {
            val newList = splitList(contactList, 20)
            var i = 0
            newList.forEach { contactDataModels ->
                contactDataModels.forEach { contactDataModel ->
                    val targetUid = contactDataModel.uid
                    val msgModel = MessageModel.createImageMessage(
                        imageUrl,
                        thumbUrl,
                        resizeImageSize?.first() ?: 0,
                        resizeImageSize?.last() ?: 0,
                        attachmentKey,
                        ArouterServiceManager.messageService.getCurrentTime(),
                        null,
                        if (contactDataModel.isBfReadCancel) contactDataModel.msgCancelTime else 0,
                        myUid,
                        targetUid,
                        chatType
                    )
                    msgModel.status = MessageModel.STATUS_SENDING

                    MessageController.saveMessage(
                        myUid,
                        targetUid,
                        chatType,
                        msgModel,
                        true,
                        contactDataModel
                    ) { savedMsgModel ->
                        //开始发送socket
                        sendMessagePackage(
                            chatType,
                            myUid,
                            savedMsgModel,
                            null,
                            contactDataModel,
                            isEncrypt = false
                        )
                    }

                    ThreadUtils.sleep(200)
                    ThreadUtils.runOnUIThread {
                        complete?.invoke(i++, i == (contactList.size - 1))
                    }
                }

//                ThreadUtils.sleep(3000)
            }
        }
    }

    /** 群发 动图 **/
    fun groupSendDynamicImageMessage(
        chatType: Int,
        attachmentKey: String,
        emoticonId: Long,
        imageFileUrl: String,
        width: Int,
        height: Int,
        myUid: Long,
        contactList: List<ContactDataModel>,
        complete: ((process: Int, complete: Boolean) -> Unit)?,
        error: ((String) -> Unit)
    ) {
        ThreadUtils.runOnIOThread {
            val newList = splitList(contactList, 20)
            var i = 0
            newList.forEach { contactDataModels ->
                contactDataModels.forEach { contactDataModel ->
                    val targetUid = contactDataModel.uid
                    val msgModel = MessageModel.createDynamicImageMessage(
                        emoticonId,
                        imageFileUrl,
                        "",
                        width,
                        height,
                        attachmentKey,
                        ArouterServiceManager.messageService.getCurrentTime(),
                        null,
                        if (contactDataModel.isBfReadCancel) contactDataModel.msgCancelTime else 0,
                        myUid,
                        targetUid,
                        chatType
                    )
                    msgModel.status = MessageModel.STATUS_SENDING
                    MessageController.saveMessage(
                        myUid,
                        targetUid,
                        chatType,
                        msgModel,
                        true,
                        contactDataModel
                    ) { savedMsgModel ->
                        //开始发送socket
                        sendMessagePackage(
                            chatType,
                            myUid,
                            savedMsgModel,
                            null,
                            contactDataModel,
                            isEncrypt = false
                        )
                    }

                    ThreadUtils.sleep(200)
                    ThreadUtils.runOnUIThread {
                        complete?.invoke(i++, i == (contactList.size - 1))
                    }
                }

//                ThreadUtils.sleep(3000)
            }
        }
    }

    /** 群发 语音 **/
    fun groupSendVoiceMessage(
        chatType: Int,
        attachmentKey: String,
        recordTime: Int,
        mp3FileUrl: String,
        highDArr: IntArray,
        myUid: Long,
        contactList: List<ContactDataModel>,
        complete: ((process: Int, complete: Boolean) -> Unit)?,
        error: ((String) -> Unit)
    ) {
        ThreadUtils.runOnIOThread {
            val newList = splitList(contactList, 20)
            var i = 0
            newList.forEach { contactDataModels ->
                contactDataModels.forEach { contactDataModel ->
                    val targetUid = contactDataModel.uid
                    val msgModel = MessageModel.createVoiceMessage(
                        recordTime,
                        mp3FileUrl,
                        highDArr,
                        attachmentKey,
                        ArouterServiceManager.messageService.getCurrentTime(),
                        null,
                        if (contactDataModel.isBfReadCancel) contactDataModel.msgCancelTime else 0,
                        myUid,
                        targetUid,
                        chatType
                    )
                    msgModel.status = MessageModel.STATUS_SENDING

                    MessageController.saveMessage(
                        myUid,
                        targetUid,
                        chatType,
                        msgModel,
                        true,
                        contactDataModel
                    ) { savedMsgModel ->
                        //开始发送socket
                        sendMessagePackage(
                            chatType,
                            myUid,
                            savedMsgModel,
                            null,
                            contactDataModel,
                            isEncrypt = false
                        )
                    }

                    ThreadUtils.sleep(200)
                    ThreadUtils.runOnUIThread {
                        complete?.invoke(i++, i == (contactList.size - 1))
                    }
                }

//                ThreadUtils.sleep(3000)
            }
        }
    }

    /** 群发 视频 **/
    fun groupSendVideoMessage(
        chatType: Int,
        videoUrl: String,
        thumbImageUrl: String,
        attachmentKey: String,
        width: Int,
        height: Int,
        duration: Int,
        myUid: Long,
        contactList: List<ContactDataModel>,
        complete: ((process: Int, complete: Boolean) -> Unit)?,
        error: ((String) -> Unit)
    ) {
        ThreadUtils.runOnIOThread {
            val newList = splitList(contactList, 20)
            var i = 0
            newList.forEach { contactDataModels ->
                contactDataModels.forEach { contactDataModel ->
                    val targetUid = contactDataModel.uid
                    val msgModel = MessageModel.createVideoMessage(
                        videoUrl,
                        thumbImageUrl,
                        width,
                        height,
                        duration,
                        attachmentKey,
                        ArouterServiceManager.messageService.getCurrentTime(),
                        null,
                        if (contactDataModel.isBfReadCancel) contactDataModel.msgCancelTime else 0,
                        myUid,
                        targetUid,
                        chatType
                    )
                    msgModel.status = MessageModel.STATUS_SENDING

                    MessageController.saveMessage(
                        myUid,
                        targetUid,
                        chatType,
                        msgModel,
                        true,
                        contactDataModel
                    ) { savedMsgModel ->
                        //开始发送socket
                        sendMessagePackage(
                            chatType,
                            myUid,
                            savedMsgModel,
                            null,
                            contactDataModel,
                            isEncrypt = false
                        )
                    }

                    ThreadUtils.sleep(200)
                    ThreadUtils.runOnUIThread {
                        complete?.invoke(i++, i == (contactList.size - 1))
                    }
                }

//                ThreadUtils.sleep(3000)
            }
        }
    }

    /** 群发 文件 **/
    fun groupSendFileMessage(
        chatType: Int,
        fileName: String,
        fileLength: Long,
        attachmentKey: String,
        fileUri: String,
        mimeType: String,
        myUid: Long,
        contactList: List<ContactDataModel>,
        complete: ((process: Int, complete: Boolean) -> Unit)?,
        error: ((String) -> Unit)
    ) {
        ThreadUtils.runOnIOThread {
            val newList = splitList(contactList, 20)
            var i = 0
            newList.forEach { contactDataModels ->
                contactDataModels.forEach { contactDataModel ->
                    val targetUid = contactDataModel.uid
                    val msgModel = MessageModel.createFileMessage(
                        fileName,
                        fileLength,
                        mimeType,
                        fileUri,
                        attachmentKey,
                        ArouterServiceManager.messageService.getCurrentTime(),
                        null,
                        if (contactDataModel.isBfReadCancel) contactDataModel.msgCancelTime else 0,
                        myUid,
                        targetUid,
                        chatType
                    )
                    msgModel.status = MessageModel.STATUS_SENDING

                    MessageController.saveMessage(
                        myUid,
                        targetUid,
                        chatType,
                        msgModel,
                        true,
                        contactDataModel
                    ) { savedMsgModel ->
                        //开始发送socket
                        sendMessagePackage(
                            chatType,
                            myUid,
                            savedMsgModel,
                            null,
                            contactDataModel,
                            isEncrypt = false
                        )
                    }

                    ThreadUtils.sleep(200)
                    ThreadUtils.runOnUIThread {
                        complete?.invoke(i++, i == (contactList.size - 1))
                    }
                }

//                ThreadUtils.sleep(3000)
            }
        }
    }

    private fun <T> splitList(list: List<T>, len: Int): List<List<T>> {
        if (list.isEmpty() || len < 1) {
            return arrayListOf(list)
        }

        val result = arrayListOf<List<T>>()
        val size = list.size
        val count = (size + len - 1) / len
        for (i in 0 until count) {
            val subList = list.subList(i * len, if ((i + 1) * len > size) size else len * (i + 1))
            result.add(subList)
        }
        return result
    }
}