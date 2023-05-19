package framework.telegram.message.manager.upload

import android.util.Log
import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.alibaba.sdk.android.oss.common.OSSLog
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.alibaba.sdk.android.oss.model.PutObjectResult
import com.alibaba.sdk.android.oss.model.ResumableUploadRequest
import com.alibaba.sdk.android.oss.model.ResumableUploadResult
import com.im.domain.pb.CommonProto
import com.im.domain.pb.SysProto
import framework.telegram.business.manager.AwsUploadImpl
import framework.telegram.message.controller.UploadAttachmentController
import framework.telegram.message.http.HttpManager
import framework.telegram.message.http.creator.SysHttpReqCreator
import framework.telegram.message.http.getResult
import framework.telegram.message.http.protocol.LoginHttpProtocol
import framework.telegram.support.BuildConfig
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

object AwsUploadImpl {


     fun createUploadTask(
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
        if (!BuildConfig.IS_JENKINS) OSSLog.enableLog()
        fileLog.invoke("开始上传...")

        val progressChangedListener = object : AwsUploadImpl.OnProgressChangedListener{

            override fun onProgressChanged(
                currentSize: Long,
                totalSize: Long,
                cancel: () -> Unit
            ) {


                if (showProgress && !cancelSignal.get()) {
                    val newPercent =
                        (currentSize.toDouble() / totalSize.toDouble()) * 0.8 + 0.2
                    UploadAttachmentController.notifyItemChangedWithProgress(
                        chatType,
                        targetId,
                        msgLocalId,
                        newPercent,
                        (100 * newPercent).toLong(),
                        totalSize
                    )
                } else if (cancelSignal.get()) {
                    cancel.invoke()
                    if (!BuildConfig.IS_JENKINS) Log.i(
                        "task",
                        "$currentSize * 0.8 + 0.2 = ${currentSize * 0.8 + 0.2} - pause"
                    )
                }

            }

        }

         AwsUploadImpl.uploadFile(encryptFile,type,spaceType, progressChangedListener, complete, error)

     }

}