package framework.telegram.message

import android.annotation.SuppressLint
import android.content.Context
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.RecallMessageEvent
import framework.telegram.message.bridge.event.ReciveMessageEvent
import framework.telegram.message.bridge.event.UnreadMessageEvent
import framework.telegram.message.connect.MessageSocketService
import framework.telegram.message.controller.DownloadAttachmentController
import framework.telegram.message.controller.UploadAttachmentController
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.manager.MessagesManager
import framework.telegram.message.manager.SendMessageManager
import framework.telegram.message.manager.SoundPoolManager
import framework.telegram.message.ui.group.GroupChatActivity
import framework.telegram.message.ui.pvt.PrivateChatActivity
import framework.telegram.message.ui.telephone.core.RtcEngineHolder
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.BitUtils
import framework.telegram.support.tools.Helper
import framework.telegram.support.tools.download.DownloadManager
import framework.telegram.ui.badge.BadgeNumberManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


class MessageApplication {

    companion object {

        @SuppressLint("StaticFieldLeak")
        private var context: Context? = null

        private var lastResetMessagesByUserLoginTime = System.currentTimeMillis()

        fun init(context: Context) {
            MessageApplication.context = context.applicationContext

            if (framework.telegram.support.BuildConfig.IS_LAB_MODEL) {
                Constant.Common.FIRST_LOAD_MESSAGE_HISTORY_COUNT = 50
                Constant.Common.SHOW_RECEIPT_MAX_GROUP_MEMBER_COUNT = 5
                Constant.Common.SHOW_RECEIPT_MAX_GROUP_MESSAGE_COUNT = 5
                Constant.Common.AUTO_CLEAR_MESSAGE_MAX_TIME = 60 * 60 * 1000
            }

            registerBus()

            // 查询所有消息状态，并进行状态重置
            MessagesManager.resetMessagesByReboot()
        }

        @SuppressLint("CheckResult")
        private fun registerBus() {
            EventBus.getFlowable(RecallMessageEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                    var msgModel: MessageModel? = null
                    MessagesManager.executeChatTransactionAsync(
                        it.chatType,
                        myUid,
                        it.targetId,
                        { realm ->
                            msgModel =
                                realm.where(MessageModel::class.java).equalTo("msgId", it.msgId)
                                    .findFirst()?.copyMessage()
                        },
                        {
                            when (msgModel?.type) {
                                MessageModel.MESSAGE_TYPE_IMAGE -> {
                                    DownloadManager.stopDownload(msgModel?.imageMessageContent?.imageThumbFileUri)
                                    DownloadManager.stopDownload(msgModel?.imageMessageContent?.imageFileUri)
                                }
                                MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE -> {
                                    DownloadManager.stopDownload(msgModel?.dynamicImageMessageBean?.imageThumbFileUri)
                                    DownloadManager.stopDownload(msgModel?.dynamicImageMessageBean?.imageFileUri)
                                }
                                MessageModel.MESSAGE_TYPE_VIDEO -> {
                                    DownloadManager.stopDownload(msgModel?.videoMessageContent?.videoThumbFileUri)
                                    DownloadManager.stopDownload(msgModel?.videoMessageContent?.videoFileUri)
                                }
                                MessageModel.MESSAGE_TYPE_VOICE -> DownloadManager.stopDownload(
                                    msgModel?.voiceMessageContent?.recordFileUri
                                )
                                MessageModel.MESSAGE_TYPE_FILE -> DownloadManager.stopDownload(
                                    msgModel?.fileMessageContentBean?.fileUri
                                )
                            }
                        })
                }

            EventBus.getFlowable(ReciveMessageEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (!ActivitiesHelper.getInstance().toBackgroud()) {
                        when (it.chaterType) {
                            ChatModel.CHAT_TYPE_PVT -> {
                                val topActivity = ActivitiesHelper.getInstance().topActivity
                                if (topActivity != null && topActivity.javaClass != PrivateChatActivity::class.java) {
                                    ArouterServiceManager.messageService.getChatIsDisturb(
                                        it.chaterType,
                                        it.chaterId,
                                        { result ->
                                            if (!result) {
                                                val privacy =
                                                    AccountManager.getLoginAccount(AccountInfo::class.java)
                                                        .getPrivacy()
                                                if (!BitUtils.checkBitValue(
                                                        Helper.int2Bytes(privacy)[3],
                                                        0
                                                    )
                                                ) {
                                                    if (ArouterServiceManager.settingService.getVibrationStatus(
                                                            privacy,
                                                            false
                                                        )
                                                    ) {//震动
                                                        SoundPoolManager.vibrator()
                                                    }
                                                    if (ArouterServiceManager.settingService.getVoiceStatus(
                                                            privacy,
                                                            false
                                                        )
                                                    ) {//声音
                                                        SoundPoolManager.playMsgRecv()
                                                    }
                                                }

                                            }
                                        })
                                }
                            }
                            ChatModel.CHAT_TYPE_GROUP -> {
                                val topActivity = ActivitiesHelper.getInstance().topActivity
                                if (topActivity != null && topActivity.javaClass != GroupChatActivity::class.java) {
                                    ArouterServiceManager.messageService.getChatIsDisturb(
                                        it.chaterType,
                                        it.chaterId,
                                        { result ->
                                            if (!result) {
                                                val privacy =
                                                    AccountManager.getLoginAccount(AccountInfo::class.java)
                                                        .getPrivacy()
                                                if (!BitUtils.checkBitValue(
                                                        Helper.int2Bytes(privacy)[3],
                                                        0
                                                    )
                                                ) {
                                                    if (ArouterServiceManager.settingService.getVibrationStatus(
                                                            privacy,
                                                            false
                                                        )
                                                    ) {//震动
                                                        SoundPoolManager.vibrator()
                                                    }
                                                    if (ArouterServiceManager.settingService.getVoiceStatus(
                                                            privacy,
                                                            false
                                                        )
                                                    ) {//声音
                                                        SoundPoolManager.playMsgRecv()
                                                    }
                                                }
                                            }
                                        })
                                }
                            }
                        }
                    }
                }

            EventBus.getFlowable(UnreadMessageEvent::class.java)
                .subscribeOn(Schedulers.io())
                .map {
                    ArouterServiceManager.messageService.getAllUnreadMessageCount()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    showIconBadge(it)
                }
        }

        private fun showIconBadge(count: Int) {
            // 适配角标（华为/荣耀/vivo/oppo/三星）
            BadgeNumberManager.from(context).setBadgeNumber(count)
        }

        // 启动消息状态线程
        fun startMessageSocket() {
            MessageSocketService.newMessageSocketService()
        }

        fun onUserLogin() {
            if (System.currentTimeMillis() - lastResetMessagesByUserLoginTime > 60000) {
                lastResetMessagesByUserLoginTime = System.currentTimeMillis()
                MessagesManager.resetMessagesByUserLogin()
            }
        }

        fun onUserChangeToken() {
            MessageSocketService.disConnect("onUserChangeToken，主动断开连接--->")
            RtcEngineHolder.endCall()
        }

        fun onUserLogout() {
            // 取消当前进行中的上传和下载操作
            DownloadAttachmentController.cancelAllDownload()
            UploadAttachmentController.cancelAllUpload()

            // 发送离线
            SendMessageManager.sendLogoutMessagePackage()
            MessageSocketService.disConnect("onUserLogout，主动断开连接--->")

            // 关闭所有进行中的通话
            RtcEngineHolder.pendingCall = null
            RtcEngineHolder.endCall()
        }
    }
}