package framework.telegram.message.manager.upload

import android.annotation.SuppressLint
import android.net.Uri
import android.text.TextUtils
import com.im.domain.pb.CommonProto
import framework.ideas.common.model.im.MessageModel
import framework.telegram.message.bridge.Constant
import framework.telegram.message.controller.UploadAttachmentController
import framework.telegram.message.manager.MessagesManager
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.system.upload.Constant.Common.UPLOAD_WAY_TYPE
import framework.telegram.support.tools.AESHelper
import framework.telegram.support.tools.FileUtils
import framework.telegram.support.tools.ThreadUtils
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean


object UploadManager {


    /**
     * 上传消息中的附件,上传成功回调complete，上传失败则会将消息改为发送失败无须后续操作
     */
    @SuppressLint("CheckResult")
    fun uploadMsgAttachment(
        chatType: Int,
        msgModel: MessageModel,
        complete: (MessageModel?) -> Unit
    ) {
        var lastMsg: MessageModel? = null
        when (msgModel.type) {
            MessageModel.MESSAGE_TYPE_IMAGE -> {
                var contentBean = msgModel.imageMessageContent
                if (TextUtils.isEmpty(contentBean.imageThumbFileBackupUri)) {
                    //上传失败
                    setError(chatType, msgModel)
                } else {
                    uploadAttachment(chatType, msgModel, contentBean.imageThumbFileBackupUri,
                        CommonProto.AttachType.PIC, CommonProto.AttachWorkSpaceType.CHAT, false,
                        { thumbUrl ->
                            //上传完毕
                            MessagesManager.executeChatTransactionAsync(
                                chatType,
                                msgModel.senderId,
                                msgModel.targetId,
                                { realm ->
                                    realm.where(MessageModel::class.java)
                                        .equalTo("flag", msgModel.flag).findFirst()?.let {
                                            contentBean = it.imageMessageContent
                                            contentBean.imageThumbFileUri = thumbUrl
                                            it.imageMessageContent = contentBean
                                            realm.copyToRealmOrUpdate(it)
                                            lastMsg = it.copyMessage()
                                        }
                                },
                                {
                                    if (TextUtils.isEmpty(contentBean.imageFileBackupUri)) {
                                        //上传失败
                                        setError(chatType, msgModel)
                                    } else {
                                        uploadAttachment(chatType,
                                            msgModel,
                                            contentBean.imageFileBackupUri,
                                            CommonProto.AttachType.PIC,
                                            CommonProto.AttachWorkSpaceType.CHAT,
                                            true,
                                            { imageUrl ->
                                                //上传完毕
                                                MessagesManager.executeChatTransactionAsync(
                                                    chatType,
                                                    msgModel.senderId,
                                                    msgModel.targetId,
                                                    { realm ->
                                                        realm.where(MessageModel::class.java)
                                                            .equalTo("flag", msgModel.flag)
                                                            .findFirst()?.let {
                                                                contentBean = it.imageMessageContent
                                                                contentBean.imageFileUri = imageUrl
                                                                it.imageMessageContent = contentBean
//                                                            it.time = ArouterServiceManager.messageService.getCurrentTime()
                                                                it.status =
                                                                    MessageModel.STATUS_SENDING
                                                                realm.copyToRealmOrUpdate(it)
                                                                lastMsg = it.copyMessage()
                                                            }
                                                    },
                                                    {
                                                        UploadAttachmentController.notifyItemChangedWithComplete(
                                                            chatType,
                                                            msgModel.targetId,
                                                            msgModel.id
                                                        )
                                                        complete.invoke(lastMsg)
                                                    },
                                                    {
                                                        //上传失败
                                                        setError(chatType, msgModel)
                                                    })
                                            },
                                            {
                                                //上传失败
                                                setError(chatType, msgModel)
                                            })
                                    }
                                },
                                {
                                    //上传失败
                                    setError(chatType, msgModel)
                                })
                        }, {
                            //上传失败
                            setError(chatType, msgModel)
                        })
                }
            }
            MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE -> {
                var contentBean = msgModel.dynamicImageMessageBean
                if (TextUtils.isEmpty(contentBean.imageFileBackupUri)) {
                    //上传失败
                    setError(chatType, msgModel)
                } else {
                    uploadAttachment(
                        chatType,
                        msgModel,
                        contentBean.imageFileBackupUri,
                        CommonProto.AttachType.PIC,
                        CommonProto.AttachWorkSpaceType.CHAT,
                        true,
                        { imageUrl ->
                            //上传完毕
                            MessagesManager.executeChatTransactionAsync(
                                chatType,
                                msgModel.senderId,
                                msgModel.targetId,
                                { realm ->
                                    realm.where(MessageModel::class.java)
                                        .equalTo("flag", msgModel.flag).findFirst()?.let {
                                            contentBean = it.dynamicImageMessageBean
                                            contentBean.imageThumbFileUri = ""
                                            contentBean.imageFileUri = imageUrl
                                            it.dynamicImageMessageBean = contentBean
//                                it.time = ArouterServiceManager.messageService.getCurrentTime()
                                            it.status = MessageModel.STATUS_SENDING
                                            realm.copyToRealmOrUpdate(it)
                                            lastMsg = it.copyMessage()
                                        }
                                },
                                {
                                    UploadAttachmentController.notifyItemChangedWithComplete(
                                        chatType,
                                        msgModel.targetId,
                                        msgModel.id
                                    )
                                    complete.invoke(lastMsg)
                                },
                                {
                                    //上传失败
                                    setError(chatType, msgModel)
                                })
                        },
                        {
                            //上传失败
                            setError(chatType, msgModel)
                        })
                }
            }
            MessageModel.MESSAGE_TYPE_VOICE -> {
                var contentBean = msgModel.voiceMessageContent
                if (TextUtils.isEmpty(contentBean.recordFileBackupUri)) {
                    //上传失败
                    setError(chatType, msgModel)
                } else {
                    uploadAttachment(chatType, msgModel, contentBean.recordFileBackupUri,
                        CommonProto.AttachType.AUDIO, CommonProto.AttachWorkSpaceType.CHAT, true,
                        { voiceUrl ->
                            //上传完毕
                            MessagesManager.executeChatTransactionAsync(
                                chatType,
                                msgModel.senderId,
                                msgModel.targetId,
                                { realm ->
                                    realm.where(MessageModel::class.java)
                                        .equalTo("flag", msgModel.flag).findFirst()?.let {
                                            contentBean = it.voiceMessageContent
                                            contentBean.recordFileUri = voiceUrl
                                            it.voiceMessageContent = contentBean
//                                        it.time = ArouterServiceManager.messageService.getCurrentTime()
                                            it.status = MessageModel.STATUS_SENDING
                                            realm.copyToRealmOrUpdate(it)
                                            lastMsg = it.copyMessage()
                                        }
                                },
                                {
                                    UploadAttachmentController.notifyItemChangedWithComplete(
                                        chatType,
                                        msgModel.targetId,
                                        msgModel.id
                                    )
                                    complete.invoke(lastMsg)
                                },
                                {
                                    //上传失败
                                    setError(chatType, msgModel)
                                })
                        }, {
                            //上传失败
                            setError(chatType, msgModel)
                        })
                }
            }
            MessageModel.MESSAGE_TYPE_VIDEO -> {
                var contentBean = msgModel.videoMessageContent
                if (TextUtils.isEmpty(contentBean.videoThumbFileBackupUri)) {
                    //上传失败
                    setError(chatType, msgModel)
                } else {
                    uploadAttachment(chatType, msgModel, contentBean.videoThumbFileBackupUri,
                        CommonProto.AttachType.PIC, CommonProto.AttachWorkSpaceType.CHAT, false,
                        { thumbUrl ->
                            //上传完毕
                            MessagesManager.executeChatTransactionAsync(
                                chatType,
                                msgModel.senderId,
                                msgModel.targetId,
                                { realm ->
                                    realm.where(MessageModel::class.java)
                                        .equalTo("flag", msgModel.flag).findFirst()?.let {
                                            contentBean = it.videoMessageContent
                                            contentBean.videoThumbFileUri = thumbUrl
                                            it.videoMessageContent = contentBean
                                            realm.copyToRealmOrUpdate(it)
                                            lastMsg = it.copyMessage()
                                        }
                                },
                                {
                                    if (TextUtils.isEmpty(contentBean.videoFileBackupUri)) {
                                        //上传失败
                                        setError(chatType, msgModel)
                                    } else {
                                        uploadAttachment(chatType,
                                            msgModel,
                                            contentBean.videoFileBackupUri,
                                            CommonProto.AttachType.VIDEO,
                                            CommonProto.AttachWorkSpaceType.CHAT,
                                            true,
                                            { videoUrl ->
                                                //上传完毕
                                                MessagesManager.executeChatTransactionAsync(
                                                    chatType,
                                                    msgModel.senderId,
                                                    msgModel.targetId,
                                                    { realm ->
                                                        realm.where(MessageModel::class.java)
                                                            .equalTo("flag", msgModel.flag)
                                                            .findFirst()?.let {
                                                                contentBean = it.videoMessageContent
                                                                contentBean.videoFileUri = videoUrl
                                                                it.videoMessageContent = contentBean
//                                                            it.time = ArouterServiceManager.messageService.getCurrentTime()
                                                                it.status =
                                                                    MessageModel.STATUS_SENDING
                                                                realm.copyToRealmOrUpdate(it)
                                                                lastMsg = it.copyMessage()
                                                            }
                                                    },
                                                    {
                                                        UploadAttachmentController.notifyItemChangedWithComplete(
                                                            chatType,
                                                            msgModel.targetId,
                                                            msgModel.id
                                                        )
                                                        complete.invoke(lastMsg)
                                                    },
                                                    {
                                                        //上传失败
                                                        setError(chatType, msgModel)
                                                    })
                                            },
                                            {
                                                //上传失败
                                                setError(chatType, msgModel)
                                            })
                                    }
                                },
                                {
                                    //上传失败
                                    setError(chatType, msgModel)
                                })
                        }, {
                            //上传失败
                            setError(chatType, msgModel)
                        })
                }
            }
            MessageModel.MESSAGE_TYPE_FILE -> {
                var contentBean = msgModel.fileMessageContentBean
                if (TextUtils.isEmpty(contentBean.fileBackupUri)) {
                    //上传失败
                    setError(chatType, msgModel)
                } else {
                    uploadAttachment(chatType, msgModel, contentBean.fileBackupUri,
                        CommonProto.AttachType.FILE, CommonProto.AttachWorkSpaceType.CHAT, true,
                        { fileUrl ->
                            //上传完毕
                            MessagesManager.executeChatTransactionAsync(
                                chatType,
                                msgModel.senderId,
                                msgModel.targetId,
                                { realm ->
                                    realm.where(MessageModel::class.java)
                                        .equalTo("flag", msgModel.flag).findFirst()?.let {
                                            contentBean = it.fileMessageContentBean
                                            contentBean.fileUri = fileUrl
                                            it.fileMessageContentBean = contentBean
//                                        it.time = ArouterServiceManager.messageService.getCurrentTime()
                                            it.status = MessageModel.STATUS_SENDING
                                            realm.copyToRealmOrUpdate(it)
                                            lastMsg = it.copyMessage()
                                        }
                                },
                                {
                                    UploadAttachmentController.notifyItemChangedWithComplete(
                                        chatType,
                                        msgModel.targetId,
                                        msgModel.id
                                    )
                                    complete.invoke(lastMsg)
                                },
                                {
                                    //上传失败
                                    setError(chatType, msgModel)
                                })
                        }, {
                            //上传失败
                            setError(chatType, msgModel)
                        })
                }
            }
            else -> {
                //上传失败
                setError(chatType, msgModel)
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun uploadAttachment(
        chatType: Int, msgModel: MessageModel, filePathUri: String,
        type: CommonProto.AttachType, spaceType: CommonProto.AttachWorkSpaceType,
        showProgress: Boolean, complete: (String) -> Unit, error: () -> Unit
    ) {
        ThreadUtils.runOnIOThread {
            val file = File(Uri.parse(filePathUri).path)
           // val encryptFile = File("${Uri.parse(filePathUri).path}___encrypt}")
            val encryptFile = File("${framework.telegram.ui.utils.FileUtils.getAPPInternalStorageFilePath(file)}___encrypt}")
            val encryptTmpFile =
                //File("${Uri.parse(filePathUri).path}___encrypt___${System.currentTimeMillis()}")
                File("${framework.telegram.ui.utils.FileUtils.getAPPInternalStorageFilePath(file)}___encrypt___${System.currentTimeMillis()}")

            try {
                // 加密附件
                encryptAndUploadAttachment(
                    chatType,
                    msgModel.senderId,
                    msgModel.targetId,
                    msgModel.id,
                    msgModel.attachmentKey,
                    file,
                    encryptFile,
                    encryptTmpFile,
                    type,
                    spaceType,
                    showProgress,
                    complete,
                    error
                )
            } catch (e: Exception) {
                // 加密失败
                error.invoke()
                FileUtils.deleteQuietly(encryptFile)
                FileUtils.deleteQuietly(encryptTmpFile)
            }
        }
    }

    /**
     * 群发用的
     */
    @SuppressLint("CheckResult")
    private fun uploadAttachment2(
        chatType: Int, senderId: Long, attachmentKey: String, filePathUri: String,
        type: CommonProto.AttachType, spaceType: CommonProto.AttachWorkSpaceType,
        showProgress: Boolean, complete: (String) -> Unit, error: () -> Unit
    ) {
        ThreadUtils.runOnIOThread {
            val file = File(Uri.parse(filePathUri).path)
            //val encryptFile = File("${Uri.parse(filePathUri).path}___encrypt}")
            val encryptFile = File("${framework.telegram.ui.utils.FileUtils.getAPPInternalStorageFilePath(file)}___encrypt}")
            val encryptTmpFile =
                //File("${Uri.parse(filePathUri).path}___encrypt___${System.currentTimeMillis()}")
                File("${framework.telegram.ui.utils.FileUtils.getAPPInternalStorageFilePath(file)}___encrypt___${System.currentTimeMillis()}")
            try {
                // 加密附件
                encryptAndUploadAttachment(
                    chatType, senderId, Constant.TargetId.GROUP_SEND_ID, 0, attachmentKey,
                    file, encryptFile, encryptTmpFile,
                    type, spaceType, showProgress, complete, error
                )
            } catch (e: Exception) {
                // 加密失败
                error.invoke()
                FileUtils.deleteQuietly(encryptFile)
                FileUtils.deleteQuietly(encryptTmpFile)
            }
        }
    }

    //targetId = -1000 就是群发消息过来的，不需要进度等
    private fun encryptAndUploadAttachment(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        msgLocalId: Long,
        attachmentKey: String,
        file: File,
        encryptFile: File,
        encryptTmpFile: File,
        type: CommonProto.AttachType,
        spaceType: CommonProto.AttachWorkSpaceType,
        showProgress: Boolean,
        complete: (String) -> Unit,
        error: () -> Unit
    ) {
        if (showProgress) {
            // 开始上传附件
            UploadAttachmentController.notifyItemChangedWithStart(chatType, targetId, msgLocalId)
        }

        val cancelSignal = AtomicBoolean(false)
        // 保存取消下载标识符
        UploadAttachmentController.saveCancelSignel(
            chatType,
            myUid,
            targetId,
            msgLocalId,
            cancelSignal
        )

        if (AESHelper.encryptFile(
                attachmentKey,
                file.absolutePath,
                encryptTmpFile.absolutePath
            ) { current, total ->
                if (showProgress) {
                    val percent = current.toDouble() / total.toDouble() * 0.2
                    UploadAttachmentController.notifyItemChangedWithProgress(
                        chatType,
                        targetId,
                        msgLocalId,
                        percent,
                        (100 * percent).toLong(),
                        total
                    )
                }

                //返回是否继续上传
                !cancelSignal.get()
            }
        ) {
            // 重命名为正式加密名称
            encryptTmpFile.renameTo(encryptFile)



            if (UPLOAD_WAY_TYPE == 0) {
                OssUploadImpl.createUploadTask(
                    chatType,
                    targetId,
                    msgLocalId,
                    encryptFile,
                    type,
                    spaceType,
                    showProgress,
                    cancelSignal,
                    complete,
                    error, { msg ->
                        AppLogcat.logger.e(msg)
                    }, { t ->
                        AppLogcat.logger.e(t)
                    })
            } else if(UPLOAD_WAY_TYPE == 1) {

                AwsUploadImpl.createUploadTask(
                    chatType,
                    targetId,
                    msgLocalId,
                    encryptFile,
                    type,
                    spaceType,
                    showProgress,
                    cancelSignal,
                    complete,
                    error, { msg ->
                        AppLogcat.logger.e(msg)
                    }, { t ->
                        AppLogcat.logger.e(t)
                    })
            }else{
                ImUploadImpl.createUploadTask(
                    chatType,
                    targetId,
                    msgLocalId,
                    encryptFile,
                    type,
                    spaceType,
                    showProgress,
                    cancelSignal,
                    complete,
                    error, {}, {}
                )
            }
        } else {
            // 加密失败
            error.invoke()
        }
    }

    private fun setError(chatType: Int, msgModel: MessageModel) {
        MessagesManager.executeChatTransactionAsync(
            chatType,
            msgModel.senderId,
            msgModel.targetId,
            { realm ->
                realm.where(MessageModel::class.java).equalTo("flag", msgModel.flag).findFirst()
                    ?.let {
                        it.status = MessageModel.STATUS_SEND_FAIL
                        realm.copyToRealmOrUpdate(it)
                    }
            },
            {
                UploadAttachmentController.notifyItemChangedWithFail(
                    chatType,
                    msgModel.targetId,
                    msgModel.id
                )
            })
    }


    fun uploadFile(filePath: String, complete: (url: String) -> Unit, error: () -> Unit) {
        val file = File(filePath)
        if (TextUtils.isEmpty(filePath) || !file.exists()) {
            //上传失败
            error.invoke()
        } else {
            uploadFile(
                file,
                CommonProto.AttachType.EMOTICON,
                CommonProto.AttachWorkSpaceType.CHAT,
                { imageUrl ->
                    complete.invoke(imageUrl)
                },
                {
                    error.invoke()
                })
        }
    }

    private fun uploadFile(
        file: File,
        type: CommonProto.AttachType,
        spaceType: CommonProto.AttachWorkSpaceType,
        complete: (url: String) -> Unit,
        error: () -> Unit
    ) {
        if (UPLOAD_WAY_TYPE == 0) {
            OssUploadImpl.createFileUploadTask(file, type, spaceType, complete, error)
        } else if(UPLOAD_WAY_TYPE == 1){
           // AwsUploadImpl.createFileUploadTask(file, type, spaceType, complete, error)
            framework.telegram.business.manager.AwsUploadImpl.uploadFile(file, type,spaceType,null,complete,error)
        }else{
            ImUploadImpl.createFileUploadTask(file, type, spaceType, complete, error)
        }
    }

    /*
    * 群发 发送消息
    */
    @SuppressLint("CheckResult")
    fun uploadGroupSendMsg(
        chatType: Int,
        type: Int,
        senderId: Long,
        attachmentKey: String,
        thumbFileBackupUri: String,
        fileBackupUri: String,
        complete: (thumbUrl: String, url: String) -> Unit,
        error: () -> Unit
    ) {
        when (type) {
            MessageModel.MESSAGE_TYPE_IMAGE -> {
                if (TextUtils.isEmpty(thumbFileBackupUri) || TextUtils.isEmpty(fileBackupUri)) {
                    //上传失败
                    error.invoke()
                } else {
                    uploadAttachment2(chatType, senderId, attachmentKey, thumbFileBackupUri,
                        CommonProto.AttachType.PIC, CommonProto.AttachWorkSpaceType.CHAT, false,
                        { thumbUrl ->
                            uploadAttachment2(chatType,
                                senderId,
                                attachmentKey,
                                fileBackupUri,
                                CommonProto.AttachType.PIC,
                                CommonProto.AttachWorkSpaceType.CHAT,
                                true,
                                { imageUrl ->
                                    complete.invoke(thumbUrl, imageUrl)
                                },
                                {
                                    //上传失败
                                    error.invoke()
                                })
                        }, {
                            error.invoke()
                        })
                }
            }
            MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE -> {
                if (TextUtils.isEmpty(fileBackupUri)) {
                    //上传失败
                    error.invoke()
                } else {
                    uploadAttachment2(chatType,
                        senderId,
                        attachmentKey,
                        fileBackupUri,
                        CommonProto.AttachType.PIC,
                        CommonProto.AttachWorkSpaceType.CHAT,
                        true,
                        { imageUrl ->
                            complete.invoke("", imageUrl)
                        },
                        {
                            error.invoke()
                        })
                }
            }
            MessageModel.MESSAGE_TYPE_VOICE -> {
                if (TextUtils.isEmpty(fileBackupUri)) {
                    //上传失败
                    error.invoke()
                } else {
                    uploadAttachment2(chatType,
                        senderId,
                        attachmentKey,
                        fileBackupUri,
                        CommonProto.AttachType.AUDIO,
                        CommonProto.AttachWorkSpaceType.CHAT,
                        true,
                        { voiceUrl ->
                            //上传完毕
                            complete.invoke("", voiceUrl)
                        },
                        {
                            //上传失败
                            error.invoke()
                        })
                }
            }
            MessageModel.MESSAGE_TYPE_VIDEO -> {
                if (TextUtils.isEmpty(thumbFileBackupUri) || TextUtils.isEmpty(fileBackupUri)) {
                    //上传失败
                    error.invoke()
                } else {
                    uploadAttachment2(chatType, senderId, attachmentKey, thumbFileBackupUri,
                        CommonProto.AttachType.PIC, CommonProto.AttachWorkSpaceType.CHAT, false,
                        { thumbUrl ->
                            //上传完毕
                            uploadAttachment2(chatType,
                                senderId,
                                attachmentKey,
                                fileBackupUri,
                                CommonProto.AttachType.VIDEO,
                                CommonProto.AttachWorkSpaceType.CHAT,
                                true,
                                { videoUrl ->
                                    //上传完毕
                                    complete.invoke(thumbUrl, videoUrl)
                                },
                                {
                                    //上传失败
                                    error.invoke()
                                })
                        }, {
                            //上传失败
                            error.invoke()
                        })
                }
            }
            MessageModel.MESSAGE_TYPE_FILE -> {
                if (TextUtils.isEmpty(fileBackupUri)) {
                    //上传失败
                    error.invoke()
                } else {
                    uploadAttachment2(chatType, senderId, attachmentKey, fileBackupUri,
                        CommonProto.AttachType.FILE, CommonProto.AttachWorkSpaceType.CHAT, true,
                        { fileUrl ->
                            //上传完毕
                            complete.invoke("", fileUrl)
                        }, {
                            //上传失败
                            error.invoke()
                        })
                }
            }
            else -> {
                //上传失败
                error.invoke()
            }
        }
    }
}

