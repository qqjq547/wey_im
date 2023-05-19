package framework.telegram.support.tools

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import framework.telegram.support.BaseApp
import framework.telegram.support.R

/**
 * Created by lzh on 19-8-19.
 * INFO:
 */
object NotificationChannelUtil {
    private var mChannelId = ""
    private var mSilentChannelId = ""

    fun initChannelId(channelId: String,silentChannelId: String) {
        mChannelId = channelId
        mSilentChannelId = silentChannelId
    }

    fun getChannelId(): String {
        return mChannelId
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getChannelVibration(): Boolean {
        return getChannel()?.shouldVibrate() ?: false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getChannelSound(): Boolean {
        if (getChannel()?.sound == null)
            return false
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun gotoChannelSetting(context: Context, channelId: String) {
        val intent = Intent()
        intent.setAction(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
        intent.putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        context.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getChannel(): NotificationChannel? {
        val notificationManager = BaseApp.app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notificationChannels.forEach {
            if (it.id.equals(mChannelId)) {
                return it
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getChannelList(): List<String> {
        val list = mutableListOf<String>()
        val notificationManager = BaseApp.app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notificationChannels.forEach {
            if (mSilentChannelId != it.id)//静默ID 不加入设置行列
                list.add(it.id)
        }
        return list
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getChannelNameList(): List<String> {
        val list = mutableListOf<String>()
        val notificationManager = BaseApp.app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notificationChannels.forEach {
            if (it.id.equals(mChannelId)) {
                list.add(BaseApp.app.getString(R.string.notification_of_new_information))
            } else if(mSilentChannelId == it.id){
                //静默ID 不加入设置行列
            } else {
                list.add(BaseApp.app.getString(R.string.push_message_notification))
            }
        }
        return list
    }
}
