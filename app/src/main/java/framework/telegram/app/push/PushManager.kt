package framework.telegram.app.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import framework.telegram.app.App
import framework.telegram.app.R
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.bridge.Constant
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.cache.utils.LogUtils
import framework.telegram.support.tools.BitUtils
import framework.telegram.support.tools.Helper
import framework.telegram.support.tools.NotificationChannelUtil
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.ui.badge.BadgeNumberManagerSystem
import framework.telegram.ui.badge.BadgeNumberManagerXiaoMi
import framework.telegram.ui.tools.RomUtils


/**
 * Created by lzh on 19-7-18.
 * INFO:
 */
object PushManager {
    private const val NOTIFICATION_MSG_PVT_ID = 33331  //私聊消息
    private const val NOTIFICATION_MSG_GROUP_ID = 33332  //群聊消息

    private const val NOTIFICATION_AUDIO_ID = 33333  //音频
    private const val NOTIFICATION_VIDEO_ID = 33334  //视频
    private const val NOTIFICATION_FRIEND_ID = 33335  //好友通知
    private const val NOTIFICATION_INVITE_JOIN_GROUP_ID = 33336  //邀请加入群聊
    private const val NOTIFICATION_APPLY_JOIN_GROUP_ID = 33337  //申请加入群聊
    private const val NOTIFICATION_MSG_GROUP_AT_ID = 33338  //群聊AT消息
    private const val NOTIFICATION_MSG_GROUP_NOTICE_ID = 33339  //群聊群公告消息

    // 通知渠道的id
//    var mChannelId = "liuba.client.android.telephone"
    var mChannelId = "1"
    // 用户可以看到的通知渠道的名字.
    private var mChannelName = BaseApp.app.getString(R.string.notification_of_new_information)
    // 用户可以看到的通知渠道的描述
    private var mChannelDescription = BaseApp.app.getString(R.string.notification_of_new_information)

    //静默通道相关
    var mSilentChannelId = "silent_channel"
    private var mSilentChannelName = "Silent"
    private var mSilentChannelDescription = "Silent"

    fun initChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannelUtil.initChannelId(mChannelId, mSilentChannelId)

            val notificationManager = BaseApp.app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(mChannelId, mChannelName, NotificationManager.IMPORTANCE_HIGH)
            // 配置通知渠道的属性
            channel.description = mChannelDescription

            // 设置通知出现时的闪灯（如果 android 设备支持的话）
            channel.enableLights(true)
            channel.lightColor = Color.RED
            // 设置通知出现时的震动（如果 android 设备支持的话）
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            // 设置通知出现时声音，默认通知是有声音的
            channel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, Notification.AUDIO_ATTRIBUTES_DEFAULT);
            channel.setShowBadge(true)//打开脚标
            notificationManager.createNotificationChannel(channel)

            //静默通道相关
            val channel1 = NotificationChannel(mSilentChannelId, mSilentChannelName, NotificationManager.IMPORTANCE_MIN)
            channel1.description = mSilentChannelName
            channel1.enableLights(false)
            channel1.enableVibration(false)
            // 设置通知出现时声音，默认通知是有声音的
            channel.setSound(null, null)
            channel1.setShowBadge(true)//打开脚标
            notificationManager.createNotificationChannel(channel1)
        }
    }

    fun showNotification(context: BaseApp, title: String, text: String, targetId: Long, pushType: Constant.Push.PUSH_TYPE, silent: Boolean, isStream: Boolean) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = if (!silent) getNotificationConfig(context, isStream) else getSilentNotificationConfig(context)
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setOngoing(false).setAutoCancel(true)
        builder.setContentIntent(getNotificationPending(context, targetId, pushType))

        val privacy = AccountManager.getLoginAccount(AccountInfo::class.java).getPrivacy()
        val hideContentTitle = BitUtils.checkBitValue(Helper.int2Bytes(privacy)[2], 5)

        when (pushType) {
            Constant.Push.PUSH_TYPE.MSG_PVT -> {
                val contentTitle = if (hideContentTitle) {
                    context.getString(R.string.received_a_new_message)
                } else {
                    String.format(context.getString(R.string.send_a_massage_for_you), title)
                }
                builder.setContentTitle(contentTitle)
                showIconBadge(builder) {
                    manager.notify(NOTIFICATION_MSG_PVT_ID, it)
                }
            }
            Constant.Push.PUSH_TYPE.MSG_GROUP -> {
                val contentTitle = if (hideContentTitle) {
                    context.getString(R.string.received_a_new_message)
                } else {
                    String.format(context.getString(R.string.send_a_massage_for_you), title)
                }
                builder.setContentTitle(contentTitle)
                showIconBadge(builder) {
                    manager.notify(NOTIFICATION_MSG_GROUP_ID, it)
                }
            }
            Constant.Push.PUSH_TYPE.MSG_GROUP_AT -> {
                if (hideContentTitle) {
                    builder.setContentTitle(context.getString(R.string.received_a_new_message))
                } else {
                    builder.setContentTitle(title)
                    builder.setContentText(text)
                }
                showIconBadge(builder) {
                    manager.notify(NOTIFICATION_MSG_GROUP_AT_ID, it)
                }
            }
            Constant.Push.PUSH_TYPE.MSG_GROUP_AT_NOTICE -> {
                if (hideContentTitle) {
                    builder.setContentTitle(context.getString(R.string.received_a_new_message))
                } else {
                    builder.setContentTitle(title)
                    builder.setContentText(String.format(context.getString(R.string.group_of_announcement_sign2), text))
                }
                showIconBadge(builder) {
                    manager.notify(NOTIFICATION_MSG_GROUP_NOTICE_ID, it)
                }
            }

            Constant.Push.PUSH_TYPE.AUDIO_STREAM -> {
                val contentTitle = if (hideContentTitle) {
                    context.getString(R.string.received_a_new_message)
                } else {
                    String.format(context.getString(R.string.invite_you_for_a_voice_call), title)
                }

                builder.setContentTitle(contentTitle)
                showIconBadge(builder) {
                    manager.notify(NOTIFICATION_AUDIO_ID, it)
                }
            }
            Constant.Push.PUSH_TYPE.VIDEO_STREAM -> {
                val contentTitle = if (hideContentTitle) {
                    context.getString(R.string.received_a_new_message)
                } else {
                    String.format(context.getString(R.string.invite_you_for_a_video_call), title)
                }

                builder.setContentTitle(contentTitle)
                showIconBadge(builder) {
                    manager.notify(NOTIFICATION_VIDEO_ID, it)
                }
            }
            Constant.Push.PUSH_TYPE.FRIEND -> {
                val contentTitle = if (hideContentTitle) {
                    context.getString(R.string.received_a_new_message)
                } else {
                    String.format(context.getString(R.string.received_a_new_add_request), title)
                }
                builder.setContentTitle(contentTitle)
                showIconBadge(builder) {
                    manager.notify(NOTIFICATION_FRIEND_ID, it)
                }
            }
            Constant.Push.PUSH_TYPE.INVITE_GROUP -> {
                val contentTitle = if (hideContentTitle) {
                    context.getString(R.string.received_a_new_message)
                } else {
                    title
                }
                builder.setContentTitle(contentTitle)
                showIconBadge(builder) {
                    manager.notify(NOTIFICATION_INVITE_JOIN_GROUP_ID, it)
                }
            }
            Constant.Push.PUSH_TYPE.APPLY_GROUP -> {
                val contentTitle = if (hideContentTitle) {
                    context.getString(R.string.received_a_new_message)
                } else {
                    title
                }
                builder.setContentTitle(contentTitle)
                showIconBadge(builder) {
                    manager.notify(NOTIFICATION_APPLY_JOIN_GROUP_ID, it)
                }
            }
        }
    }


    private fun getSilentNotificationConfig(context: BaseApp): NotificationCompat.Builder {
        var builder: NotificationCompat.Builder?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = NotificationCompat.Builder(context, mSilentChannelId)
        } else {
            builder = NotificationCompat.Builder(context, mSilentChannelId)
//            var default = Notification.DEFAULT_LIGHTS
//            if (ArouterServiceManager.settingService.getVibrationStatus(privacy)) {//震动
//                default = default or Notification.DEFAULT_VIBRATE
//            }
//            if (ArouterServiceManager.settingService.getVoiceStatus(privacy)) {//声音
//                default = default or Notification.DEFAULT_SOUND
//            }
//            builder.setDefaults(default)
        }
        return builder
    }

    private fun getNotificationConfig(context: BaseApp, isStream: Boolean): NotificationCompat.Builder {
        val privacy = AccountManager.getLoginAccount(AccountInfo::class.java).getPrivacy()
        var builder: NotificationCompat.Builder?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = NotificationCompat.Builder(context, mChannelId)
        } else {
            builder = NotificationCompat.Builder(context, mChannelId)
            var default = Notification.DEFAULT_LIGHTS
            if (ArouterServiceManager.settingService.getVibrationStatus(privacy, isStream)) {//震动
                default = default or Notification.DEFAULT_VIBRATE
            }
            if (ArouterServiceManager.settingService.getVoiceStatus(privacy, isStream)) {//声音
                default = default or Notification.DEFAULT_SOUND
            }
            builder.setDefaults(default)
        }
        return builder
    }

    private fun getNotificationPending(context: BaseApp, targetId: Long, pushType: Constant.Push.PUSH_TYPE): PendingIntent {
        val bundle = Bundle()
        var link = ""
        when (pushType) {
            Constant.Push.PUSH_TYPE.MSG_PVT -> {
                link = "im://page/oneToOneMessage?uid=$targetId"
            }
            Constant.Push.PUSH_TYPE.MSG_GROUP,
            Constant.Push.PUSH_TYPE.MSG_GROUP_AT,
            Constant.Push.PUSH_TYPE.MSG_GROUP_AT_NOTICE -> {
                link = "im://page/groupMessage?groupId=$targetId"
            }
            Constant.Push.PUSH_TYPE.AUDIO_STREAM -> {
                link = "im://page/streamMessage?uid=$targetId&streamType=0&openType=1"
            }
            Constant.Push.PUSH_TYPE.VIDEO_STREAM -> {
                link = "im://page/streamMessage?uid=$targetId&streamType=1&openType=1"
            }
            Constant.Push.PUSH_TYPE.FRIEND -> {
                link = "im://page/inviteFriend"
            }
            Constant.Push.PUSH_TYPE.INVITE_GROUP -> {
                link = "im://page/inviteJoinGroup"
            }
            Constant.Push.PUSH_TYPE.APPLY_GROUP -> {
                link = "im://page/applyJoinGroup"
            }
        }
        bundle.putString("link", link)
        val intent = Intent(context, PushActivity::class.java)
        intent.putExtras(bundle)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun cancelAllNotification(context: BaseApp) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancelAll()
    }

    fun cancelVideoNotification(context: BaseApp) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_VIDEO_ID)
    }

    fun cancelAudioNotification(context: BaseApp) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_AUDIO_ID)
    }

    private fun showIconBadge(notificationBuilder: NotificationCompat.Builder, complete: (Notification) -> Unit) {
        ThreadUtils.runOnIOThread {
            val count = ArouterServiceManager.messageService.getAllUnreadMessageCount()
            ThreadUtils.runOnUIThread {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    BadgeNumberManagerSystem.setBadgeNumber(notificationBuilder, count)
                }

                val notification = notificationBuilder.build()
                if (RomUtils.isMiui) {
                    BadgeNumberManagerXiaoMi.setBadgeNumber(notification, count)
                }

                complete.invoke(notification)
            }
        }
    }
}