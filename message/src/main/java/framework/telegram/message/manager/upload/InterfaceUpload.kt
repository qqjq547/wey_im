package framework.telegram.message.manager.upload

import com.im.domain.pb.CommonProto
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by lzh on 20-3-5.
 * INFO:
 */
interface InterfaceUpload {

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
    )


    fun createFileUploadTask(
        file: File,
        type: CommonProto.AttachType,
        spaceType: CommonProto.AttachWorkSpaceType,
        complete: (url: String) -> Unit,
        error: () -> Unit
    )
}