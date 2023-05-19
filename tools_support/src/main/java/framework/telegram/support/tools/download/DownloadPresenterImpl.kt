package framework.telegram.support.tools.download

/**
 * Created by lzh on 19-7-23.
 * INFO:
 */

import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener3
import framework.telegram.support.BaseApp
import framework.telegram.support.R
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.FileUtils
import java.io.File
import java.lang.Exception

class DownloadPresenterImpl(private val mUrl: String, val mView: DownloadContract.View) : DownloadContract.Presenter {

    override fun download() {
        val downloadFile = DownloadManager.generateDownloadApKSaveFile(mUrl)
        if (downloadFile != null) {
            if (!FileUtils.checkFile(downloadFile)) {
                //文件不存在,则说明文件从未下载或在下载中
                val tmpCacheFile = File(downloadFile.absolutePath + "___download")
                DownloadManager.download(mUrl, mUrl, tmpCacheFile, mListener)
            } else {
                //文件已存在,则说明文件下载完成
                mView.downloadSucess(BaseApp.app.getString(R.string.the_task_has_been_downloaded))
                EventBus.publishEvent(ApkDownloadEvent(mUrl))
            }
        }
    }

    override fun onDestroy() {
        DownloadManager.stopDownload(mUrl)
    }

    private val mListener = object : DownloadListener3() {

        override fun connected(task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long) {

        }

        override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
            if (totalLength > 0L) {
                val process = (currentOffset * 100 / totalLength).toFloat()
                mView.setDownloadProgress(process)
            }
        }

        override fun completed(task: DownloadTask) {
            mView.downloadSucess(BaseApp.app.getString(R.string.the_task_has_been_downloaded))
            val file = task.file
            file?.let {
                val realFile = File(file.absolutePath.replace("___download", ""))
                file.renameTo(realFile)

                EventBus.publishEvent(ApkDownloadEvent(mUrl))
            }
        }

        override fun error(task: DownloadTask, e: Exception) {
            mView.downloadError(e.message.toString())
        }

        override fun retry(task: DownloadTask, cause: ResumeFailedCause) {

        }

        override fun warn(task: DownloadTask) {

        }

        override fun started(task: DownloadTask) {

        }

        override fun canceled(task: DownloadTask) {

        }
    }
}

