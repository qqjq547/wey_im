package framework.telegram.app.push

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import framework.telegram.business.commandhandler.ImCommandHandlers
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.NotificationEvent
import framework.telegram.support.BaseActivity
import framework.telegram.support.system.event.EventBus

/**
 * Created by lzh on 19-7-18.
 * INFO:
 */
class PushActivity :BaseActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val link = intent.getStringExtra("link")
        if (!TextUtils.isEmpty(link))  {
            ImCommandHandlers().executeCommand(link)
            //点击了通知后，清掉所有通知
            EventBus.publishEvent(NotificationEvent(0, "",Constant.Push.PUSH_TYPE.CLEAR_ALL_NOTIFICATION))
        }
        finish()
    }
}