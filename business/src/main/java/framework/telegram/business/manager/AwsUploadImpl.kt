package framework.telegram.business.manager

/**
 * Created by lzh on 19-6-6.
 * INFO:
 */

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.S3ClientOptions
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.im.domain.pb.CommonProto
import com.im.domain.pb.UploadFileProto
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.SysHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.LoginHttpProtocol
import framework.telegram.support.BaseApp
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.system.upload.Constant.Common.UPLOAD_WAY_TYPE
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future


object AwsUploadImpl : InterfaceUpload {

    fun uploadFile(
        file: File,
        type: CommonProto.AttachType,
        spaceType: CommonProto.AttachWorkSpaceType,
        processListener: OnProgressChangedListener? = null,
        complete: (String) -> Unit,
        error: () -> Unit
    ) {
        HttpManager.getStore(LoginHttpProtocol::class.java)
            .getUploadUrl(object : HttpReq<UploadFileProto.GetUploadUrlReq>() {
                override fun getData(): UploadFileProto.GetUploadUrlReq {
                    return SysHttpReqCreator.getUploadUrl(UPLOAD_WAY_TYPE.toLong(), spaceType, type)
                }
            })

            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                upload(file, fileKey = it.fileId, url = it.url, processListener, complete, error)
            }, {
                // 上传失败
                error.invoke()
            })
    }


    @SuppressLint("CheckResult")
    override fun uploadFile(
        owner: LifecycleOwner,
        filePathUri: String,
        type: CommonProto.AttachType,
        spaceType: CommonProto.AttachWorkSpaceType,
        complete: (String) -> Unit,
        error: () -> Unit
    ) {

        HttpManager.getStore(LoginHttpProtocol::class.java)
            .getUploadUrl(object : HttpReq<UploadFileProto.GetUploadUrlReq>() {
                override fun getData(): UploadFileProto.GetUploadUrlReq {
                    return SysHttpReqCreator.getUploadUrl(UPLOAD_WAY_TYPE.toLong(), spaceType, type)
                }
            })
            .bindToLifecycle(owner)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                val file = File(Uri.parse(filePathUri).path)
                upload(file, fileKey = it.fileId, url = it.url, null, complete, error)
            }, {
                // 上传失败
                error.invoke()
            })

    }


    /**
     * 简单上传
     */
    private fun upload(
        encryptFile: File,
        fileKey: String,
        url: String,
        processListener: OnProgressChangedListener? = null,
        complete: (String) -> Unit,
        error: () -> Unit
    ) {
        HttpManager.getStore(LoginHttpProtocol::class.java)
            .getAwsUpload(object : HttpReq<UploadFileProto.GetAwsUploadReq>() {
                override fun getData(): UploadFileProto.GetAwsUploadReq {
                    return SysHttpReqCreator.getAwsUpload()
                }
            })
            .getResult(null, {


                val awsCredentials: AWSCredentials =
                    BasicAWSCredentials(it.accessKey, it.accessKeySecret)

                val clientOptions: S3ClientOptions =
                    S3ClientOptions.builder().setAccelerateModeEnabled(false).build()

                val uploadClient = AmazonS3Client(awsCredentials)
                uploadClient.setRegion(Region.getRegion(it.region))

                uploadClient.setS3ClientOptions(clientOptions)

                val transferUtility =
                    TransferUtility.builder().s3Client(uploadClient).context(BaseApp.app)
                        .build()

                val transferObserver: TransferObserver =
                    transferUtility.upload(
                        it.bucketName,
                        fileKey,
                        encryptFile,
                        CannedAccessControlList.PublicRead
                    )


                transferObserver.setTransferListener(object : TransferListener {

                    override fun onStateChanged(id: Int, state: TransferState) {

                        Log.e("AwsUploadImpl", "onStateChanged----" + state.name)

                        if (state == TransferState.COMPLETED) {
                            complete?.invoke(url)
                            encryptFile.delete()
                            Log.e("AwsUploadImpl", "上传成功")
                        }
                    }

                    override fun onProgressChanged(
                        id: Int,
                        bytesCurrent: Long,
                        bytesTotal: Long
                    ) {


                        Log.e("AwsUploadImpl", "onProgressChanged----")

                        processListener?.onProgressChanged(bytesCurrent, bytesTotal) {

                           // transferUtility.cancel(transferObserver.id)
                        }
                    }

                    override fun onError(id: Int, ex: Exception) {

                        error?.invoke()

                        Log.e("AwsUploadImpl", "上传失败")
                    }
                })


            }, {
                Log.e("AwsUploadImpl", "error---" + it.message)
                error.invoke()
            })
    }


    interface OnProgressChangedListener {

        fun onProgressChanged(
            bytesCurrent: Long,
            bytesTotal: Long,
            cancel: () -> Unit
        )
    }

}

