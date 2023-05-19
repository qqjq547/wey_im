package framework.telegram.support.system.upload

/**
 * Created by lzh on 20-1-13.
 * INFO:
 */
interface ResultListener{
    fun onProgress(currentSize:Long,totalSize:Long)

    fun onSuccess(url:String)

    fun onFailure(throwable:Throwable)
}