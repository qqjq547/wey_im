package framework.telegram.support.tools.screenshot

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import framework.telegram.support.BuildConfig
import java.io.File
import java.lang.ref.WeakReference
import java.util.*


/**
 * @author ounk
 * 监控截图
 * 具体看Readme
 */
object ScreenShotsUtils {

    enum class RESULT {
        REQUEST_PERMISSION,
        SUCCESS,
        FAILED
    }

    //  读取媒体数据库时需要读取的列
    private val MEDIA_PROJECTIONS_API_16 = listOf<String>(
            MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns.DATE_TAKEN)

    //  截屏依据中的路径判断关键字
    private val KEYWORDS = listOf<String>(
            "screenshot", "screen_shot", "screen-shot", "screen shot",
            "screencapture", "screen_capture", "screen-capture", "screen capture",
            "screencap", "screen_cap", "screen-cap", "screen cap"
            //vivo截图保存为中文名
            , "截屏_")

    //  请求usageStats权限
    const val REQUEST_USAGE_STATS = 0x9919

    //  默认截图存放路径
    private val SCREEN_SHOT_PATH by lazy {
        StringBuilder()
                .append(Environment.getExternalStorageDirectory())
                .append(File.separator)
                .append(Environment.DIRECTORY_DCIM)
                .append(File.separator)
                .append("Screenshots")
                .append(File.separator)
                .toString()
    }

    //  已经提醒截图后保留的文件名，避免二次提醒
    private var mHasAlertPath: String = ""

    //  回调
    private var mCallBack: (() -> Unit)? = null

    //  --------------------------------  FileObserver监听相关 -------------------------------- //
    private val mRecentComp = RecentUseComparator()
    private var isFileObserver: Boolean = false
    private val _fileObserver by lazy {
        ScreenShotsFileObserver(SCREEN_SHOT_PATH, FileObserver.CREATE) { result, targetName ->
            if (result) {
                if (BuildConfig.DEBUG)
                    Log.e("ScreenShotsUtils", "F - $targetName")
                if (mHasAlertPath != targetName.toLowerCase()) {
                    mCallBack?.invoke()
                    mHasAlertPath = targetName.toLowerCase()
                }
            }
        }
    }
    //  ------------------------------------------------------------------------------------------//


    //  --------------------------------  ContentObserver监听相关 -------------------------------- //
    private val _startListTime = System.currentTimeMillis()
    private var _weakReference: WeakReference<Context>? = null
    private val _handlerThread: HandlerThread = HandlerThread("ScreenShotsUtils").also { it.start() }
    private val _handler: Handler = Handler(_handlerThread.looper)
    private val mContentObserverCallback: (selfChange: Boolean, uri: Uri?) -> Unit = { _: Boolean, uri: Uri? ->
        if (handleContentChange(uri)) {
            if (BuildConfig.DEBUG)
                Log.e("ScreenShotsUtils", "C - $uri")
            mCallBack?.invoke()
        }
    }
    private var isContentObserver: Boolean = false

    private val _internalObserver: ScreenShotsContentObserver by lazy {
        ScreenShotsContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, _handler, mContentObserverCallback)
    }
    private val _externalObserver: ScreenShotsContentObserver by lazy {
        ScreenShotsContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, _handler, mContentObserverCallback)
    }
    //  ------------------------------------------------------------------------------------------//


    fun startScreenShotsListen(@NonNull activity: AppCompatActivity, @NonNull context: Context, @NonNull callBack: () -> Unit): RESULT {
        mCallBack = callBack
        _weakReference = WeakReference(context)
        if (BuildConfig.DEBUG) Log.e("SCREEN_SHOT_PATH", SCREEN_SHOT_PATH)
        //  校验权限
        if (!checkUsageStatsPermission(context)) {
            //  申请权限
            intentToAllowPermission(activity)
            return ScreenShotsUtils.RESULT.REQUEST_PERMISSION
        }
        //  校验权限
        if (checkUsageStatsPermission(context)) {
            startFileObserver()
            startContentObserver(context)
            return ScreenShotsUtils.RESULT.SUCCESS
        }
        return ScreenShotsUtils.RESULT.FAILED
    }

    fun startScreenShotsListen(@NonNull context: Context, @NonNull callBack: () -> Unit) {

        mCallBack = callBack
        _weakReference = WeakReference(context)
        if (BuildConfig.DEBUG) Log.e("SCREEN_SHOT_PATH", SCREEN_SHOT_PATH)
        startFileObserver()
        startContentObserver(context)
    }

    fun stopScreenShotsListen(context: Context) {
        mCallBack = null
        if (isFileObserver)
            stopFileObserver()
        if (isContentObserver)
            stopContentObserver(context)
    }

    fun checkUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        var mode = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun intentToAllowPermission(activity: AppCompatActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.startActivityForResult(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), REQUEST_USAGE_STATS)
        }
    }

    @SuppressLint("CheckResult")
    fun checkIsTopPackageName(context: Context): Boolean {
        return context.packageName == getTopPackageName(context)
    }

    private fun handleContentChange(uri: Uri?): Boolean {
        if (uri != null && _weakReference!!.get() != null) {
            val context = _weakReference!!.get()
            context?.let {
                var cursor: Cursor? = null
                try {
                    //  数据改变时查询数据库中最后加入的一条数据
                    cursor = context.contentResolver.query(
                            uri,
                            MEDIA_PROJECTIONS_API_16.toTypedArray(), null, null,
                            MediaStore.Images.ImageColumns.DATE_ADDED + " desc limit 1"
                    )

                    if (cursor == null) {
                        return false
                    }
                    if (!cursor.moveToFirst()) {
                        return false
                    }

                    //  获取各列的索引
                    val dataIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                    val dateTakenIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN)

                    //  获取行数据
                    var data = cursor.getString(dataIndex)
                    val dateTaken = cursor.getLong(dateTakenIndex)

                    //  处理获取到的第一行数据
                    /*
                     * 判断依据一: 时间判断
                     */
                    //  如果加入数据库的时间在开始监听之前, 或者与当前时间相差大于10秒, 则认为当前没有截屏
                    if (dateTaken < _startListTime || System.currentTimeMillis() - dateTaken > 60 * 1000) {
                        return false
                    }

                    /*
                     * 判断依据三: 路径判断
                     */
                    if (TextUtils.isEmpty(data)) {
                        return false
                    }
                    data = data.toLowerCase()
                    //  判断图片路径是否含有指定的关键字之一, 如果有, 则认为当前截屏了
                    for (keyWork in KEYWORDS) {
                        if (data.contains(keyWork) &&
                                !mHasAlertPath.toLowerCase().replace(".", "").contains(data.replace(".", ""))
                                && !data.contains("__thumb") && !data.contains("__encrypt")) {
                            mHasAlertPath = data
                            return true
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()

                } finally {
                    if (cursor != null && !cursor.isClosed) {
                        cursor.close()
                    }
                }
            }
        }
        return false
    }

    private fun getTopPackageName(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val ts = System.currentTimeMillis()
            val mUsageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            //  在1000ms内第一位的app是本app的话才会提醒
            val usageStats: List<UsageStats> = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, ts - 1000, ts)
            if (usageStats.isEmpty()) {
                return ""
            }
            Collections.sort(usageStats, mRecentComp)
            return usageStats[0].packageName
        } else {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val taskList = am.getRunningTasks(1)
            return taskList[0].topActivity?.className ?: ""
        }
    }

    private fun startFileObserver() {
        if (!isFileObserver)
            _fileObserver.startWatching()
        isFileObserver = true
    }

    private fun stopFileObserver() {
        if (isFileObserver)
            _fileObserver.stopWatching()
        isFileObserver = false
    }

    private fun startContentObserver(context: Context) {
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, false, _internalObserver)
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, _externalObserver)
        isContentObserver = true
    }

    private fun stopContentObserver(context: Context) {
        context.contentResolver.unregisterContentObserver(_internalObserver)
        context.contentResolver.unregisterContentObserver(_externalObserver)
        isContentObserver = false
    }

    private class RecentUseComparator : Comparator<UsageStats> {
        override fun compare(lhs: UsageStats, rhs: UsageStats): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (lhs.lastTimeUsed > rhs.lastTimeUsed) -1 else if (lhs.lastTimeUsed === rhs.lastTimeUsed) 0 else 1
            } else
                -1
        }
    }
}