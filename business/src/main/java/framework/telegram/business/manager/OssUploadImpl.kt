package framework.telegram.business.manager

/**
 * Created by lzh on 19-6-6.
 * INFO:
 */

import android.annotation.SuppressLint
import android.net.Uri
import androidx.lifecycle.LifecycleOwner
import com.alibaba.sdk.android.oss.ClientConfiguration
import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.alibaba.sdk.android.oss.model.PutObjectResult
import com.im.domain.pb.CommonProto
import com.im.domain.pb.SysProto
import com.im.domain.pb.UploadFileProto
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.SysHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.LoginHttpProtocol
import framework.telegram.support.BaseApp
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.system.upload.Constant
import framework.telegram.support.system.upload.UploadManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File


object OssUploadImpl :InterfaceUpload{

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
    // 过期时间
    private var expiration = 0L
    // bucketName
    private var bucketName = ""
    // endpoint
    private var endpoint = ""
    // ossToken
    private var credentialProvider: OSSCredentialProvider? = null
    // ossAsyncTask
    private var task: OSSAsyncTask<*>? = null

    @SuppressLint("CheckResult")
    override fun uploadFile(owner: LifecycleOwner, filePathUri: String, type: CommonProto.AttachType, spaceType: CommonProto.AttachWorkSpaceType, complete: (String) -> Unit, error: () -> Unit) {
        HttpManager.getStore(LoginHttpProtocol::class.java)
                .getUploadToken(object : HttpReq<SysProto.GetUploadTokenReq>() {
                    override fun getData(): SysProto.GetUploadTokenReq {
                        return SysHttpReqCreator.getUploadToken()
                    }
                })
                .bindToLifecycle(owner)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    bucketName = it.ossBucket
                    endpoint = it.ossEndpoint
                    expiration = it.expiration
                    credentialProvider = getCredentialProvider(
                            accessKeyId = it.accessKeyId,
                            accessKeySecret = it.accessKeySecret,
                            securityToken = it.securityToken)
                    val file = File(Uri.parse(filePathUri).path)
                    runPutobjectUploadTask(file, type, spaceType, null, complete, error)
                }, {
                    // 上传失败
                    error.invoke()
                })
    }

    private fun getCredentialProvider(accessKeyId: String, accessKeySecret: String, securityToken: String): OSSCredentialProvider {
        return OSSStsTokenCredentialProvider(accessKeyId, accessKeySecret, securityToken)
    }

    /**
     * 简单上传
     */
    private fun runPutobjectUploadTask(encryptFile: File,
                                       type: CommonProto.AttachType,
                                       spaceType: CommonProto.AttachWorkSpaceType,
                                       ossProgressCallback: OSSProgressCallback<PutObjectRequest>?,
                                       complete: (String) -> Unit,
                                       error: () -> Unit) {
        HttpManager.getStore(LoginHttpProtocol::class.java)
                .getUploadUrl(object : HttpReq<UploadFileProto.GetUploadUrlReq>() {
                    override fun getData(): UploadFileProto.GetUploadUrlReq {
                        return SysHttpReqCreator.getUploadUrl(Constant.Common.UPLOAD_WAY_TYPE.toLong() ,spaceType,type)
                    }
                })
                .getResult(null, {
                    task = createPutobjectUploadTask(encryptFile, it.fileId, ossProgressCallback, object : OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
                        override fun onSuccess(request: PutObjectRequest?, result: PutObjectResult?) {
                            complete.invoke(it.url)
                        }

                        override fun onFailure(request: PutObjectRequest?, clientException: ClientException?, serviceException: ServiceException?) {
                            error.invoke()
                        }
                    })

                },{
                    error.invoke()
                })
    }

    private fun createPutobjectUploadTask(file: File, objectKey: String, progressCallBack: OSSProgressCallback<PutObjectRequest>?, callback: OSSCompletedCallback<PutObjectRequest, PutObjectResult>): OSSAsyncTask<PutObjectResult>? {
        val request = PutObjectRequest(bucketName, objectKey, file.absolutePath)
        request.progressCallback = progressCallBack
        return OSSClient(BaseApp.app, endpoint, credentialProvider, conf).asyncPutObject(request,callback)
    }
}

