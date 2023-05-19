package framework.telegram.support.tools

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import com.umeng.analytics.MobclickAgent
import framework.telegram.support.BaseApp
import framework.telegram.support.system.log.AppLogcat
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object ThreadUtils {

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private val singleThreadExecutor by lazy { Executors.newSingleThreadExecutor() }

    private val cachedThreadExecutor by lazy { Executors.newCachedThreadPool() }

    @SuppressLint("CheckResult")
    fun runOnUIThread(cmd: () -> Unit) {
        mainHandler.post {
            cmd.invoke()
        }
    }

    @SuppressLint("CheckResult")
    fun runOnUIThread(delay: Long, cmd: () -> Unit) {
        mainHandler.postDelayed({
            cmd.invoke()
        }, delay)
    }

    @SuppressLint("CheckResult")
    fun runOnAppointThread(looper: Looper?, cmd: () -> Unit) {
        if (looper == null) mainHandler else Handler(looper).post {
            cmd.invoke()
        }
    }

    @SuppressLint("CheckResult")
    fun runOnAppointThread(looper: Looper?, delay: Long, cmd: () -> Unit) {
        mainHandler.postDelayed({
            runOnAppointThread(looper, cmd)
        }, delay)
    }

    @SuppressLint("CheckResult")
    fun runOnIOThread(cmd: () -> Unit) {
        cachedThreadExecutor.execute {
            cmd.invoke()
        }
    }

    @SuppressLint("CheckResult")
    fun runOnIOThread(delay: Long, cmd: () -> Unit) {
        mainHandler.postDelayed({
            runOnIOThread(cmd)
        }, delay)
    }

    @SuppressLint("CheckResult")
    fun runOnBackgroundThread(cmd: () -> Unit) {
        singleThreadExecutor.execute {
            cmd.invoke()
        }
    }

    fun sleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
