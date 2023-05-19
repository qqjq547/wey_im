package framework.telegram.message.manager.upload

import android.util.Log
import com.im.domain.pb.CommonProto
import framework.telegram.message.controller.UploadAttachmentController
import framework.telegram.support.BuildConfig.IS_JENKINS
import framework.telegram.support.system.upload.ResultListener
import framework.telegram.support.system.upload.UploadManager
import framework.telegram.support.tools.FileUtils
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by lzh on 20-1-13.
 * INFO:
 */
object ImUploadImpl : InterfaceUpload {

    // 阈值，大于阈值将启动分片上传
    private const val MAX_SINGLE_THRESHHOLD = 512 * 1024L

    override fun createUploadTask(
        chatType: Int,
        targetId: Long,
        msgLocalId: Long,
        encryptFile: File,
        type: CommonProto.AttachType,
        spaceType: CommonProto.AttachWorkSpaceType,
        showProgress: Boolean,
        cancelSignal: AtomicBoolean,
        complete: (String) -> Unit,
        error: () -> Unit,
        fileLog: (String) -> Unit,
        throwableLog: (Throwable) -> Unit
    ) {
//        if (encryptFile.length() < MAX_SINGLE_THRESHHOLD) {
//            //TODO 分片上传 ，暂时没有
//        } else {
        val uploadType = getUploadType(type, spaceType)
        if (uploadType == null) {
            error.invoke()
        } else {
            UploadManager.uploadFile(encryptFile, uploadType, object : ResultListener {
                override fun onProgress(currentSize: Long, totalSize: Long) {
                    if (showProgress && !cancelSignal.get()) {
                        val newPercent = (currentSize.toDouble() / totalSize.toDouble()) * 0.8 + 0.2
                        Log.i(
                            "lzh",
                            " onProgress   new $newPercent  currentOffset  ${(100 * newPercent).toLong()}  totalSize $totalSize"
                        )
                        UploadAttachmentController.notifyItemChangedWithProgress(
                            chatType,
                            targetId,
                            msgLocalId,
                            newPercent,
                            (100 * newPercent).toLong(),
                            totalSize
                        )
                    } else if (cancelSignal.get()) {
                        if (!IS_JENKINS) Log.i(
                            "task",
                            "$currentSize * 0.8 + 0.2 = ${currentSize * 0.8 + 0.2} - pause"
                        )
                    }
                }

                override fun onSuccess(url: String) {
                    complete.invoke(url)
                    // 删除加密的临时文件
                    FileUtils.deleteQuietly(encryptFile)
                }

                override fun onFailure(throwable: Throwable) {
                    error.invoke()
                    // 删除加密的临时文件
                    FileUtils.deleteQuietly(encryptFile)
                }
            })
        }
//        }
    }

    override fun createFileUploadTask(
        file: File,
        type: CommonProto.AttachType,
        spaceType: CommonProto.AttachWorkSpaceType,
        complete: (url: String) -> Unit,
        error: () -> Unit
    ) {
        val uploadType = getUploadType(type, spaceType)
        if (uploadType == null) {
            error.invoke()
        } else {
            UploadManager.uploadFile(file, uploadType, object : ResultListener {
                override fun onProgress(currentSize: Long, totalSize: Long) {

                }

                override fun onSuccess(url: String) {
                    complete.invoke(url)
                }

                override fun onFailure(throwable: Throwable) {
                    error.invoke()
                }
            })
        }
    }

    private fun getUploadType(
        type: CommonProto.AttachType,
        spaceType: CommonProto.AttachWorkSpaceType
    ): UploadManager.UPLOAD_TYPE? {
        if (spaceType.ordinal == CommonProto.AttachWorkSpaceType.COMMON_VALUE) {
            return when (type.ordinal) {
                CommonProto.AttachType.PIC_VALUE -> UploadManager.UPLOAD_TYPE.COMMON_PIC
                CommonProto.AttachType.AUDIO_VALUE -> UploadManager.UPLOAD_TYPE.COMMON_AUDIO
                CommonProto.AttachType.VIDEO_VALUE -> UploadManager.UPLOAD_TYPE.COMMON_VIDEO
                CommonProto.AttachType.FILE_VALUE -> UploadManager.UPLOAD_TYPE.COMMON_FILE
                CommonProto.AttachType.LOG_VALUE -> UploadManager.UPLOAD_TYPE.COMMON_LOG
                else -> null
            }
        } else {
            return when (type.ordinal) {
                CommonProto.AttachType.PIC_VALUE -> UploadManager.UPLOAD_TYPE.CHAT_PIC
                CommonProto.AttachType.AUDIO_VALUE -> UploadManager.UPLOAD_TYPE.CHAT_AUDIO
                CommonProto.AttachType.VIDEO_VALUE -> UploadManager.UPLOAD_TYPE.CHAT_VIDEO
                CommonProto.AttachType.FILE_VALUE -> UploadManager.UPLOAD_TYPE.CHAT_FILE
                CommonProto.AttachType.LOG_VALUE -> UploadManager.UPLOAD_TYPE.CHAT_LOG
                CommonProto.AttachType.EMOTICON_VALUE -> UploadManager.UPLOAD_TYPE.CHAT_EMOTICON
                else -> null
            }
        }
    }
}