package framework.telegram.support.tools

import android.app.Notification.EXTRA_CHANNEL_ID
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import framework.telegram.support.tools.AndroidUtils.getApplicationInfo
import android.provider.Settings.EXTRA_APP_PACKAGE
import android.widget.Toast
import framework.telegram.support.R


/**
 * Created by lzh on 19-6-22.
 * INFO: 这里没有测过兼容
 */
object NotificationUtils {

    fun showNotificationWindow(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent()
                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                //这种方案适用于 API 26, 即8.0（含8.0）以上可以用
                intent.putExtra(EXTRA_APP_PACKAGE, context.packageName)
                intent.putExtra(EXTRA_CHANNEL_ID, context.applicationInfo.uid)
                context.startActivity(intent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                val intent = Intent()
                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                //这种方案适用于 API21——25，即 5.0——7.1 之间的版本可以使用
                intent.putExtra("app_package", context.packageName)
                intent.putExtra("app_uid", context.applicationInfo.uid)
                context.startActivity(intent);
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.data = Uri.parse("package:" + context.packageName)
                context.startActivity(intent);
            } else {
                val localIntent = Intent()
                localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                localIntent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                localIntent.data = Uri.fromParts("package", context.packageName, null)
                context.startActivity(localIntent);
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, R.string.start_activity_error, Toast.LENGTH_SHORT).show()
        }
    }

    fun notificationSwitchOn(context: Context): Boolean {//19及以上可用
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}