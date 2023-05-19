package framework.telegram.business.manager

/**
 * Created by lzh on 19-6-6.
 * INFO:
 */

import androidx.lifecycle.LifecycleOwner
import com.im.domain.pb.CommonProto
import framework.telegram.support.system.upload.Constant.Common.UPLOAD_WAY_TYPE


object UploadManager : InterfaceUpload {

    override fun uploadFile(owner: LifecycleOwner, filePathUri: String, type: CommonProto.AttachType, spaceType: CommonProto.AttachWorkSpaceType, complete: (String) -> Unit, error: () -> Unit) {
        if (UPLOAD_WAY_TYPE == 0){
            OssUploadImpl.uploadFile(owner, filePathUri, type, spaceType, complete, error)
        }else if(UPLOAD_WAY_TYPE == 1){
            AwsUploadImpl.uploadFile(owner, filePathUri, type, spaceType, complete, error)
        }else {
            ImUploadImpl.uploadFile(owner, filePathUri, type, spaceType, complete, error)
        }
    }
}

