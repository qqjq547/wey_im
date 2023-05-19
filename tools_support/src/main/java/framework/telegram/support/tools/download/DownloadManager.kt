package framework.telegram.support.tools.download

import android.util.Log
import com.liulishuo.okdownload.*
import com.liulishuo.okdownload.core.listener.DownloadListener3
import java.io.File
import framework.telegram.support.BaseApp
import framework.telegram.support.tools.MD5
import framework.telegram.support.tools.ResourceUtils
import framework.telegram.support.tools.file.DirManager
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import java.util.concurrent.ConcurrentHashMap


/**
 * 附件下载加解密服务
 */
object DownloadManager {

    private val downloadTasks: ConcurrentHashMap<String, DownloadTask> by lazy { ConcurrentHashMap<String, DownloadTask>() }

    fun download(downloadKey: String, downloadUrl: String, targetFile: File, downloadListener: DownloadListener3) {
        var task = downloadTasks[downloadKey]
        if (task == null) {
            task = DownloadTask.Builder(downloadUrl, targetFile.parentFile)
                    .setFilename(targetFile.name)
                    .setMinIntervalMillisCallbackProcess(500)//进度回调间隔ms
                    .setPassIfAlreadyCompleted(false)//是否重复下载
                    .build()
            downloadTasks[downloadKey] = task
        }

        task?.let {
            val status = StatusUtil.getStatus(task)
            if (status == StatusUtil.Status.UNKNOWN) {
                // 任务未在下载序列中,加入listener并启动
                task.enqueue(downloadListener)
            } else {
                //任务已在下载序列中
                when (status) {
                    StatusUtil.Status.COMPLETED,
                    StatusUtil.Status.IDLE -> {
                        // 任务已完成或暂停中
                        task.enqueue(downloadListener)
                    }
                    else -> {
                        // 任务进行中或者任务已完成
                        task.replaceListener(downloadListener)
                    }
                }
            }
        }
    }

    fun isDownloading(downloadKey: String?): Boolean {
        downloadKey?.let {
            val task = downloadTasks[downloadKey]
            if (task != null) {
                val status = StatusUtil.getStatus(task)
                Log.d("demo","$downloadKey status---> $status")
                return status == StatusUtil.Status.PENDING || status == StatusUtil.Status.RUNNING
            }
        }

        return false
    }

    fun isIdeling(downloadKey: String?): Boolean {
        downloadKey?.let {
            val task = downloadTasks[downloadKey]
            if (task != null) {
                val status = StatusUtil.getStatus(task)
                return status == StatusUtil.Status.IDLE
            }
        }

        return false
    }

    fun stopDownload(downloadKey: String?) {
        downloadKey?.let {
            downloadTasks[downloadKey]?.cancel()
        }
    }

    fun stopAllDownload() {
        downloadTasks.forEach {
            it.value.cancel()
        }

        downloadTasks.clear()
    }

    /**
     * 关键代码，根据远程URL生成本地文件对象，既用于本地文件的下载，也用于判断远程文件是否在本地有缓存
     */
    fun generateDownloadApKSaveFile(uriString: String): File? {
        val uri = UriUtils.parseUri(uriString)
        return when {
            ResourceUtils.isHttpScheme(uri) -> File(DirManager.getApkCacheDir(BaseApp.app), MD5.md5(uriString) + ".apk")
            ResourceUtils.isFileScheme(uri) -> File(uri.path!!)
            else -> null
        }
    }


    fun getDownloadSize(downloadKey: String?):Float{
        downloadKey?.let {
            val task = downloadTasks[downloadKey]
            if (task != null) {
                task.info?.let {
                    return (((it.totalOffset.toDouble()/it.totalLength.toDouble())*0.8)).toFloat()
                }

            }
        }

        return 0f
    }
}
