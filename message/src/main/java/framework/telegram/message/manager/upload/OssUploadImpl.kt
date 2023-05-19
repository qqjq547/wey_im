package framework.telegram.message.manager.upload

import android.util.Log
import com.alibaba.sdk.android.oss.ClientConfiguration
import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback
import com.alibaba.sdk.android.oss.common.OSSLog
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.alibaba.sdk.android.oss.model.PutObjectResult
import com.alibaba.sdk.android.oss.model.ResumableUploadRequest
import com.alibaba.sdk.android.oss.model.ResumableUploadResult
import com.im.domain.pb.CommonProto
import com.im.domain.pb.SysProto
import framework.telegram.message.controller.UploadAttachmentController
import framework.telegram.message.http.HttpManager
import framework.telegram.message.http.creator.SysHttpReqCreator
import framework.telegram.message.http.getResult
import framework.telegram.message.http.protocol.LoginHttpProtocol
import framework.telegram.support.BaseApp
import framework.telegram.support.BuildConfig.IS_JENKINS
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.FileUtils
import java.io.File
import java.lang.Exception
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by lzh on 20-1-13.
 * INFO:
 */
object OssUploadImpl : InterfaceUpload {

    private val conf = ClientConfiguration().also {
        // 连接超时
        it.connectionTimeout = 120 * 1000
        // socket超时
        it.socketTimeout = 120 * 1000
        // 最大并发请求树
        it.maxConcurrentRequest = 5
        // 失败后最大重试次数
        it.maxErrorRetry = 2
    }


    // 阈值，大于阈值将启动分片上传
    private const val MAX_SINGLE_THRESHHOLD = 512 * 1024L

    // 过期时间
    private var expiration = 0L

    // bucketName
    private var bucketName = ""

    // endpoint
    private var endpoint = ""

    // ossToken
    private var credentialProvider: OSSCredentialProvider? = null

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
        if (!IS_JENKINS) OSSLog.enableLog()
        var task: OSSAsyncTask<*>? = null
        fileLog.invoke("开始上传...")
        if (expiration == 0L || credentialProvider == null || bucketName == "" || endpoint == "" || (expiration < System.currentTimeMillis())) {
            fileLog.invoke("获取上传参数...")
            HttpManager.getStore(LoginHttpProtocol::class.java)
                .getUploadToken(object : HttpReq<SysProto.GetUploadTokenReq>() {
                    override fun getData(): SysProto.GetUploadTokenReq {
                        return SysHttpReqCreator.getUploadToken()
                    }
                })
                .getResult(null, {
                    if (cancelSignal.get()) {
                        error.invoke()
                    } else {
                        fileLog.invoke("获取上传参数成功...")
                        bucketName = it.ossBucket
                        endpoint = it.ossEndpoint
                        expiration = it.expiration

                        credentialProvider = getCredentialProvider(
                            accessKeyId = it.accessKeyId,
                            accessKeySecret = it.accessKeySecret,
                            securityToken = it.securityToken
                        )

                        if (encryptFile.length() < MAX_SINGLE_THRESHHOLD) {
                            fileLog.invoke("获取上传url...")
                            runPutobjectUploadTask(type, spaceType, { fileId, url ->
                                fileLog.invoke("获取上传url成功，开始上传文件...")
                                task = createPutobjectUploadTask(encryptFile,
                                    fileId,
                                    OSSProgressCallback { _, currentSize, totalSize ->
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
                                            task?.cancel()
                                            if (!IS_JENKINS) Log.i(
                                                "task",
                                                "$currentSize * 0.8 + 0.2 = ${currentSize * 0.8 + 0.2} - pause"
                                            )
                                        }
                                    }, object :
                                        OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
                                        override fun onSuccess(
                                            request: PutObjectRequest?,
                                            result: PutObjectResult?
                                        ) {
                                            fileLog.invoke("上传文件完成...")
                                            complete.invoke(url)
                                            // 删除加密的临时文件
                                            FileUtils.deleteQuietly(encryptFile)
                                        }

                                        override fun onFailure(
                                            request: PutObjectRequest?,
                                            clientException: ClientException?,
                                            serviceException: ServiceException?
                                        ) {
                                            fileLog.invoke("上传文件失败...")
                                            clientException?.let {
                                                throwableLog.invoke(it)
                                            }
                                            serviceException?.let {
                                                throwableLog.invoke(it)
                                            }
                                            error.invoke()
                                            // 删除加密的临时文件
                                            FileUtils.deleteQuietly(encryptFile)
                                        }
                                    })
                            }, {
                                fileLog.invoke("获取上传url失败...")
                                error.invoke()
                            })
                        } else {
                            fileLog.invoke("获取上传url...")
                            runResumableUploadTask(type, spaceType, { fileId, url ->
                                fileLog.invoke("获取上传url成功，开始上传文件...")
                                task = createResumableUploadTask(
                                    encryptFile,
                                    fileId,
                                    OSSProgressCallback { _, currentSize, totalSize ->
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
                                            task?.cancel()
                                            if (!IS_JENKINS) Log.i(
                                                "task",
                                                "$currentSize * 0.8 + 0.2 = ${currentSize * 0.8 + 0.2} - pause"
                                            )
                                        }
                                    }, object :
                                        OSSCompletedCallback<ResumableUploadRequest, ResumableUploadResult> {
                                        override fun onSuccess(
                                            request: ResumableUploadRequest?,
                                            result: ResumableUploadResult?
                                        ) {
                                            fileLog.invoke("上传文件完成...")
                                            complete.invoke(url)
                                            // 删除加密的临时文件
                                            FileUtils.deleteQuietly(encryptFile)
                                        }

                                        override fun onFailure(
                                            request: ResumableUploadRequest?,
                                            clientException: ClientException?,
                                            serviceException: ServiceException?
                                        ) {
                                            fileLog.invoke("上传文件失败...")
                                            clientException?.let {
                                                throwableLog.invoke(it)
                                            }
                                            serviceException?.let {
                                                throwableLog.invoke(it)
                                            }
                                            error.invoke()
                                            // 删除加密的临时文件
                                            FileUtils.deleteQuietly(encryptFile)
                                        }
                                    })
                            }, error)
                        }
                    }
                }, {
                    // 上传失败
                    error.invoke()
                })
        } else {
            if (encryptFile.length() < MAX_SINGLE_THRESHHOLD) {
                fileLog.invoke("获取上传url...")
                runPutobjectUploadTask(type, spaceType, { fileId, url ->
                    fileLog.invoke("获取上传url成功，开始上传文件...")
                    task = createPutobjectUploadTask(encryptFile, fileId,
                        OSSProgressCallback { _, currentSize, totalSize ->
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
                                task?.cancel()
                                if (!IS_JENKINS) Log.i(
                                    "task",
                                    "$currentSize * 0.8 + 0.2 = ${currentSize * 0.8 + 0.2} - pause"
                                )
                            }
                        }, object : OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
                            override fun onSuccess(
                                request: PutObjectRequest?,
                                result: PutObjectResult?
                            ) {
                                fileLog.invoke("上传文件完成...")
                                complete.invoke(url)
                                // 删除加密的临时文件
                                FileUtils.deleteQuietly(encryptFile)
                            }

                            override fun onFailure(
                                request: PutObjectRequest?,
                                clientException: ClientException?,
                                serviceException: ServiceException?
                            ) {
                                fileLog.invoke("上传文件失败...")
                                clientException?.let {
                                    throwableLog.invoke(it)
                                }
                                serviceException?.let {
                                    throwableLog.invoke(it)
                                }
                                error.invoke()
                                // 删除加密的临时文件
                                FileUtils.deleteQuietly(encryptFile)
                            }
                        })
                }, error)
            } else {
                fileLog.invoke("获取上传url...")
                runResumableUploadTask(type, spaceType, { fileId, url ->
                    fileLog.invoke("获取上传url成功，开始上传文件...")
                    task = createResumableUploadTask(
                        encryptFile,
                        fileId,
                        OSSProgressCallback { _, currentSize, totalSize ->
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
                                task?.cancel()
                                if (!IS_JENKINS) Log.i(
                                    "task",
                                    "$currentSize * 0.8 + 0.2 = ${currentSize * 0.8 + 0.2} - pause"
                                )
                            }
                        },
                        object :
                            OSSCompletedCallback<ResumableUploadRequest, ResumableUploadResult> {
                            override fun onSuccess(
                                request: ResumableUploadRequest?,
                                result: ResumableUploadResult?
                            ) {
                                fileLog.invoke("上传文件完成...")
                                complete.invoke(url)
                                // 删除加密的临时文件
                                FileUtils.deleteQuietly(encryptFile)
                            }

                            override fun onFailure(
                                request: ResumableUploadRequest?,
                                clientException: ClientException?,
                                serviceException: ServiceException?
                            ) {
                                fileLog.invoke("上传文件失败...")
                                clientException?.let {
                                    throwableLog.invoke(it)
                                }
                                serviceException?.let {
                                    throwableLog.invoke(it)
                                }
                                error.invoke()
                                // 删除加密的临时文件
                                FileUtils.deleteQuietly(encryptFile)
                            }
                        })
                }, error)
            }
        }
    }

    override fun createFileUploadTask(
        file: File,
        type: CommonProto.AttachType,
        spaceType: CommonProto.AttachWorkSpaceType,
        complete: (url: String) -> Unit,
        error: () -> Unit
    ) {
        if (expiration == 0L || credentialProvider == null || bucketName == "" || endpoint == "" || (expiration < System.currentTimeMillis())) {
            HttpManager.getStore(LoginHttpProtocol::class.java)
                .getUploadToken(object : HttpReq<SysProto.GetUploadTokenReq>() {
                    override fun getData(): SysProto.GetUploadTokenReq {
                        return SysHttpReqCreator.getUploadToken()
                    }
                })
                .getResult(null, {
                    bucketName = it.ossBucket
                    endpoint = it.ossEndpoint
                    expiration = it.expiration
                    credentialProvider = getCredentialProvider(
                        accessKeyId = it.accessKeyId,
                        accessKeySecret = it.accessKeySecret,
                        securityToken = it.securityToken
                    )

                    runPutobjectUploadTask(type, spaceType, { fileId, url ->
                        createPutobjectUploadTask(
                            file,
                            fileId,
                            null,
                            object : OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
                                override fun onSuccess(
                                    request: PutObjectRequest?,
                                    result: PutObjectResult?
                                ) {
                                    complete.invoke(url)
                                }

                                override fun onFailure(
                                    request: PutObjectRequest?,
                                    clientException: ClientException?,
                                    serviceException: ServiceException?
                                ) {
                                    error.invoke()
                                }
                            })
                    }, error)
                }, {
                    error.invoke()
                })
        } else {
            runPutobjectUploadTask(type, spaceType, { fileId, url ->
                createPutobjectUploadTask(
                    file,
                    fileId,
                    null,
                    object : OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
                        override fun onSuccess(
                            request: PutObjectRequest?,
                            result: PutObjectResult?
                        ) {
                            complete.invoke(url)
                        }

                        override fun onFailure(
                            request: PutObjectRequest?,
                            clientException: ClientException?,
                            serviceException: ServiceException?
                        ) {
                            error.invoke()
                        }
                    })
            }, error)
        }
    }

    /**
     * 简单上传
     */
    private fun runPutobjectUploadTask(
        type: CommonProto.AttachType,
        spaceType: CommonProto.AttachWorkSpaceType,
        getTask: (String, String) -> Unit,
        error: () -> Unit
    ) {
        HttpManager.getStore(LoginHttpProtocol::class.java)
            .getUploadUrl(object : HttpReq<SysProto.GetUploadUrlReq>() {
                override fun getData(): SysProto.GetUploadUrlReq {
                    return SysHttpReqCreator.getUploadUrl(spaceType, type)
                }
            })
            .getResult(null, {
                getTask.invoke(it.fileId, it.url)
            }, {
                error.invoke()
            })
    }

    /**
     * 分片+断点 上传
     */
    private fun runResumableUploadTask(
        type: CommonProto.AttachType,
        spaceType: CommonProto.AttachWorkSpaceType,
        getTask: (String, String) -> Unit,
        error: () -> Unit
    ) {
        HttpManager.getStore(LoginHttpProtocol::class.java)
            .getUploadUrl(object : HttpReq<SysProto.GetUploadUrlReq>() {
                override fun getData(): SysProto.GetUploadUrlReq {
                    return SysHttpReqCreator.getUploadUrl(spaceType, type)
                }
            })
            .getResult(null, {
                getTask.invoke(it.fileId, it.url)
            }, {
                error.invoke()
            })
    }


    private fun getCredentialProvider(
        accessKeyId: String,
        accessKeySecret: String,
        securityToken: String
    ): OSSCredentialProvider {
        return OSSStsTokenCredentialProvider(accessKeyId, accessKeySecret, securityToken)
    }

    private fun createPutobjectUploadTask(
        file: File,
        objectKey: String,
        progressCallBack: OSSProgressCallback<PutObjectRequest>?,
        callback: OSSCompletedCallback<PutObjectRequest, PutObjectResult>
    ): OSSAsyncTask<PutObjectResult> {
        val request = PutObjectRequest(bucketName, objectKey, file.absolutePath)
        request.progressCallback = progressCallBack
        return OSSClient(BaseApp.app, endpoint, credentialProvider, conf).asyncPutObject(
            request,
            callback
        )
    }

    private fun createResumableUploadTask(
        file: File,
        objectKey: String,
        progressCallBack: OSSProgressCallback<ResumableUploadRequest>,
        callback: OSSCompletedCallback<ResumableUploadRequest, ResumableUploadResult>
    ): OSSAsyncTask<ResumableUploadResult>? {
        // 调用OSSAsyncTask cancel()方法时是否需要删除断点记录文件的设置
//        val recordDirectory = Environment.getExternalStorageDirectory().absolutePath + "/oss_record/"
//        val recordDir = File(recordDirectory)
        // 要保证目录存在，如果不存在则主动创建
//        if (!recordDir.exists()) {
//            recordDir.mkdirs()
//        }
//        val request = ResumableUploadRequest(bucketName, objectKey, file.absolutePath, recordDirectory)
        val request = ResumableUploadRequest(bucketName, objectKey, file.absolutePath)
//        request.setDeleteUploadOnCancelling(true)
        request.progressCallback = progressCallBack
        return OSSClient(BaseApp.app, endpoint, credentialProvider, conf).asyncResumableUpload(
            request,
            callback
        )
    }
}