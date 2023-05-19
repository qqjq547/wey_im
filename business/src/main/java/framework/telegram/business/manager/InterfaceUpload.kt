package framework.telegram.business.manager

import androidx.lifecycle.LifecycleOwner
import com.im.domain.pb.CommonProto
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by lzh on 20-3-5.
 * INFO:
 */
interface InterfaceUpload{

    fun uploadFile(owner: LifecycleOwner, filePathUri: String, type: CommonProto.AttachType, spaceType: CommonProto.AttachWorkSpaceType, complete: (String) -> Unit, error: () -> Unit)

}