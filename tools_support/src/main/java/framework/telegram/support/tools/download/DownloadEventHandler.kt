package framework.telegram.support.tools.download


import android.annotation.SuppressLint
import android.text.TextUtils
import framework.telegram.support.BaseApp
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.APKUtils
import framework.telegram.support.tools.ThreadUtils
import io.reactivex.android.schedulers.AndroidSchedulers

/**
 * Created by hyf on 16/7/5.
 */
class DownloadEventHandler {

    @SuppressLint("CheckResult")
    fun initEvent() {
        EventBus.getFlowable(ApkDownloadEvent::class.java).observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    if (event == null || TextUtils.isEmpty(event.getDownloadUrl())) {
                        return@subscribe
                    }

                    val downloadFile = DownloadManager.generateDownloadApKSaveFile(event.downloadUrl)
                    ThreadUtils.runOnIOThread {
                        APKUtils.installApk(downloadFile, BaseApp.app)
                    }
                }
    }
}
