package framework.telegram.business.ui.widget

import android.widget.TextView

import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.group.GroupMemberModel
import framework.telegram.business.BusinessApplication
import framework.telegram.business.R
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.bridge.service.IMessageService
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.TimeUtils

object ViewUtils {

    fun showOnlineStatus(messageService: IMessageService, uid: Long, isShowLastOnlineTime: Boolean, isOnlineStatus: Boolean, lastOnlineTime: Long, statusView: TextView?) {
        showOnlineStatus(messageService, statusView, uid, isShowLastOnlineTime, isOnlineStatus, lastOnlineTime)
    }

    fun showOnlineStatus(messageService: IMessageService, data: GroupMemberModel?, statusView: TextView?) {
        data?.let {
            showOnlineStatus(messageService, statusView, data.uid, data.isShowLastOnlineTime, data.isOnlineStatus, data.lastOnlineTime)
        }
    }

    fun showOnlineStatus(messageService: IMessageService, statusView: TextView?, uid: Long, isShowLastOnlineTime: Boolean, isOnlineStatus: Boolean, lastOnlineTime: Long) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        if (uid == myUid) {
            if (isShowLastOnlineTime) {
                statusView?.text = BaseApp.app.getString(R.string.on_line)
            } else {
                statusView?.text = BaseApp.app.getString(R.string.recently_crossed_the_line)
            }
        } else {
            if (isShowLastOnlineTime) {
                if (isOnlineStatus) {
                    statusView?.text = BaseApp.app.getString(R.string.on_line)
                } else {
                    statusView?.text = TimeUtils.timeFormatForDynamic(lastOnlineTime, messageService.getCurrentTime())
                }
            } else {
                if (isOnlineStatus) {
                    statusView?.text = BaseApp.app.getString(R.string.recently_crossed_the_line)
                } else {
                    if (System.currentTimeMillis() - lastOnlineTime < 7 * 24 * 60 * 60 * 1000) {
                        statusView?.text = BaseApp.app.getString(R.string.recently_crossed_the_line)
                    } else {
                        statusView?.text = BaseApp.app.getString(R.string.recently_crossed_the_line)
                    }
                }
            }
        }
    }
}
