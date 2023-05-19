package framework.telegram.support.tools

import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * Created by lzh on 19-6-29.
 * INFO:
 */
object AppInfo{
    /**
     * 判断apk是不是debug
     */
    fun isDebug(context: Context): Boolean {
        try {
            val info = context.applicationInfo
            return info.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        } catch (e: Exception) {

        }

        return false
    }
}