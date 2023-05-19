package framework.telegram.business.manager

/**
 * Created by lzh on 19-6-6.
 * INFO:
 */

import android.net.Uri
import androidx.lifecycle.LifecycleOwner
import com.im.domain.pb.CommonProto
import framework.telegram.support.system.upload.Constant
import framework.telegram.support.system.upload.ResultListener
import framework.telegram.support.system.upload.UploadManager
import java.io.File


object ImUploadImpl :InterfaceUpload{

    override fun uploadFile(owner: LifecycleOwner, filePathUri: String, type: CommonProto.AttachType, spaceType: CommonProto.AttachWorkSpaceType, complete: (String) -> Unit, error: () -> Unit) {
//        if (encryptFile.length() < MAX_SINGLE_THRESHHOLD) {
//            //TODO 分片上传 ，暂时没有
//        } else {
        val file = File(Uri.parse(filePathUri).path)
        val uploadType = getUploadType(type, spaceType)
        if (uploadType == null) {
            error.invoke()
        } else {
            UploadManager.uploadFile(file, uploadType, object : ResultListener {
                override fun onProgress(currentSize: Long, totalSize: Long) {
                }

                override fun onSuccess(url: String) {
                    complete.invoke( url)
                    // 删除加密的临时文件
                }

                override fun onFailure(throwable: Throwable) {
                    error.invoke()
                    // 删除加密的临时文件
                }
            })
        }
//        }
    }

    private fun getUploadType(type: CommonProto.AttachType, spaceType: CommonProto.AttachWorkSpaceType): UploadManager.UPLOAD_TYPE? {
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

