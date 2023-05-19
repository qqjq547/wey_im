package framework.telegram.support.system.upload.progress

/**
 * Created by lzh on 20-1-13.
 * INFO:
 */
interface ProgressListener{
    fun onProgress(totalBytes:Long,remainingBytes:Long, done:Boolean)
}