package framework.telegram.app.push

import android.annotation.SuppressLint
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.utils.MuteStatusUtil
import framework.telegram.message.bridge.event.NotificationEvent
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ActivitiesHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class PushManagerEventHandler {
    
    @SuppressLint("CheckResult")
    fun initEvent() {
        EventBus.getFlowable(NotificationEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    val privacy = AccountManager.getLoginAccount(AccountInfo::class.java).getPrivacy()
                    event.let {
                        when {
                            it.pushType == framework.telegram.message.bridge.Constant.Push.PUSH_TYPE.CLEAR_ALL_NOTIFICATION -> PushManager.cancelAllNotification(BaseApp.app)
                            it.pushType == framework.telegram.message.bridge.Constant.Push.PUSH_TYPE.CLEAR_AUDIO_NOTIFICATION -> PushManager.cancelAudioNotification(BaseApp.app)
                            it.pushType == framework.telegram.message.bridge.Constant.Push.PUSH_TYPE.CLEAR_VIDEO_NOTIFICATION -> PushManager.cancelVideoNotification(BaseApp.app)
                            ActivitiesHelper.getInstance().toBackgroud() -> {
                                val isStream = when (it.pushType) {
                                    framework.telegram.message.bridge.Constant.Push.PUSH_TYPE.AUDIO_STREAM,
                                    framework.telegram.message.bridge.Constant.Push.PUSH_TYPE.VIDEO_STREAM -> {
                                        true
                                    }
                                    else -> {
                                        false
                                    }
                                }
                                PushManager.showNotification(BaseApp.app, it.title, it.text, it.targetId, it.pushType, !MuteStatusUtil.isNotification(privacy, isStream), isStream)
                            }

                        }
                    }
                }
    }
}