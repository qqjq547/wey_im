package framework.telegram.message.controller

import android.text.TextUtils
import com.im.pb.IMPB
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.ChatModel.CHAT_TYPE_PVT
import framework.ideas.common.model.im.MessageModel
import framework.ideas.common.model.im.StreamCallModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.db.RealmCreator
import framework.telegram.message.manager.*
import framework.telegram.message.ui.telephone.core.RtcEngineHolder
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.AESHelper
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.HexString
import framework.telegram.support.tools.ThreadUtils

object StreamCallController {

    fun endCall(tipMsg: String, delayEndCall: Boolean) {
        ThreadUtils.runOnUIThread {
            BaseApp.app.toast(tipMsg)
        }

        ThreadUtils.runOnIOThread(if (delayEndCall) 3000 else 0) {
            RtcEngineHolder.endCall()
        }
    }

    /**
     * 请求语音通话
     */
    fun requestAudioCall(secretKey: String) {
        //开始发送socket
        if (RtcEngineHolder.isActive()) {
            ArouterServiceManager.systemService.getUserSecretKey(RtcEngineHolder.targetUid, appVer = 0, complete = { sk, _, _, _ ->
                RtcEngineHolder.reqTime = System.currentTimeMillis()
                if (TextUtils.isEmpty(sk)){
                    SendMessageManager.sendStreamRequestPackage(RtcEngineHolder.targetUid, IMPB.StreamType.streamAudio, "")
                }else{
                    SendMessageManager.sendStreamRequestPackage(RtcEngineHolder.targetUid, IMPB.StreamType.streamAudio, AESHelper.encrypt(secretKey.toByteArray(), sk))
                }
            }, error = {
                RtcEngineHolder.endCall()
            })
        }
    }

    /**
     * 请求视频通话
     */
    fun requestVideoCall(secretKey: String) {
        //开始发送socket
        if (RtcEngineHolder.isActive()) {
            ArouterServiceManager.systemService.getUserSecretKey(RtcEngineHolder.targetUid, appVer = 0, complete = { sk, _, _, _ ->
                RtcEngineHolder.reqTime = System.currentTimeMillis()
                if (TextUtils.isEmpty(sk)){
                    SendMessageManager.sendStreamRequestPackage(RtcEngineHolder.targetUid, IMPB.StreamType.streamVideo, "")
                }else{
                    SendMessageManager.sendStreamRequestPackage(RtcEngineHolder.targetUid, IMPB.StreamType.streamVideo, AESHelper.encrypt(secretKey.toByteArray(), sk))
                }
            }, error = {
                RtcEngineHolder.endCall()
            })
        }
    }

    /**
     * 取消通话请求
     */
    fun cancelStreamCallReq(streamType: IMPB.StreamType) {
        //取消通话请求
        if (RtcEngineHolder.isActive()) {
            updateCallStreamStatus(RtcEngineHolder.mineUid, RtcEngineHolder.targetUid, RtcEngineHolder.currentChannelName, 3)
            SendMessageManager.sendCancelStreamRequestPackage(RtcEngineHolder.currentChannelName, RtcEngineHolder.openType, RtcEngineHolder.mineUid, RtcEngineHolder.targetUid, streamType)
        }
    }

    /**
     * 拒绝通话请求
     */
    fun refuseStreamCallReq(streamType: IMPB.StreamType) {
        if (RtcEngineHolder.isActive()) {
            updateCallStreamStatus(RtcEngineHolder.mineUid, RtcEngineHolder.targetUid, RtcEngineHolder.currentChannelName, 2)
            SendMessageManager.sendRefuseStreamRequestPackage(RtcEngineHolder.currentChannelName, RtcEngineHolder.openType, RtcEngineHolder.mineUid, RtcEngineHolder.targetUid, streamType)
        }
    }

    /**
     * 同意通话请求
     */
    fun agreeStreamCallReq(streamType: IMPB.StreamType) {
        if (RtcEngineHolder.isActive()) {
            SendMessageManager.sendAgreeStreamRequestPackage(RtcEngineHolder.currentChannelName, RtcEngineHolder.openType, RtcEngineHolder.mineUid, RtcEngineHolder.targetUid, streamType)
        }
    }

    /**
     * 结束通话请求
     */
    fun overStreamCallReq(openType: Int) {
        if (RtcEngineHolder.isActive()) {
            if (openType == 0) {
                SendMessageManager.sendOverStreamRequestPackage(RtcEngineHolder.currentChannelName, RtcEngineHolder.mineUid, RtcEngineHolder.targetUid)
            } else {
                SendMessageManager.sendOverStreamRequestPackage(RtcEngineHolder.currentChannelName, RtcEngineHolder.targetUid, RtcEngineHolder.mineUid)
            }
        }
    }

    /**
     * 创建一个流媒体通话记录
     */
    fun createStreamCallRecord(targetUid: Long, channelName: String, streamType: Int, isSend: Int, sendTime: Long = 0L,streamStatus:Int = 0 ,complete: (() -> Unit)? = null) {
        //建立流媒体通话记录
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        var msgModelCopy: MessageModel? = null
        ArouterServiceManager.contactService.getContactInfo(null, targetUid, { contactInfoModel, _ ->
            if (!contactInfoModel.isBfReadCancel){
                RealmCreator.executePvtChatTransactionAsync(myUid, targetUid, {
                    val msgModel = MessageModel.createStreamMessage(channelName, streamType, isSend,if (sendTime != 0L) sendTime else  ArouterServiceManager.messageService.getCurrentTime(),
                            myUid, targetUid,CHAT_TYPE_PVT,streamStatus)
                    it.copyToRealm(msgModel)
                    msgModelCopy = msgModel.copyMessage()
                }, {
                    //修改会话记录 流媒体只有私聊才能用
                    ChatsHistoryManager.checkChatHistoryIsCreated(ChatModel.CHAT_TYPE_PVT, myUid, targetUid, msgModelCopy)
                })
            }
            RealmCreator.executeStreamCallsHistoryTransactionAsync(myUid, {
                val msgModel = StreamCallModel.createStream(channelName, streamType, isSend, targetUid, contactInfoModel.displayName, contactInfoModel.nickName, contactInfoModel.icon, ArouterServiceManager.messageService.getCurrentTime())
                it.copyToRealm(msgModel)
            }, complete)
        })


    }

    /**
     * 更新本次通话开始时间，并更新状态为已同意
     */
    fun updateCallStartTime(myUid: Long, targetUid: Long, channelName: String, streamType: Int, time: Long) {
        RealmCreator.executePvtChatTransactionAsync(myUid, targetUid, { realm ->
            realm.where(MessageModel::class.java).equalTo("flag", channelName).findFirst()?.let {
                val contentBean = it.streamMessageContent
                contentBean.status = 1//同意
                contentBean.streamType = streamType
                contentBean.startTime = time
                it.streamMessageContent = contentBean
                realm.copyToRealmOrUpdate(it)
            }
        })

        RealmCreator.executeStreamCallsHistoryTransactionAsync(myUid, { realm ->
            realm.where(StreamCallModel::class.java).equalTo("sessionId", channelName).findFirst()?.let {
                it.status = 1//同意
                it.streamType = streamType
                it.startTime = time
                realm.copyToRealmOrUpdate(it)
            }
        })
    }

    /**
     * 更新通话结束时间，并更新状态为已完成
     */
    fun updateCallEndTime(myUid: Long, targetUid: Long, channelName: String, resetStatus: Boolean, time: Long) {
        //更新流媒体通话记录的状态
        RealmCreator.executePvtChatTransactionAsync(myUid, targetUid, { realm ->
            realm.where(MessageModel::class.java).equalTo("flag", channelName).findFirst()?.let {
                val contentBean = it.streamMessageContent
                if (resetStatus) {
                    contentBean.status = 5
                }

                contentBean.endTime = time
                it.streamMessageContent = contentBean
                realm.copyToRealmOrUpdate(it)
            }
        })

        RealmCreator.executeStreamCallsHistoryTransactionAsync(myUid, { realm ->
            realm.where(StreamCallModel::class.java).equalTo("sessionId", channelName).findFirst()?.let {
                if (resetStatus) {
                    it.status = 5
                }

                it.endTime = time
                realm.copyToRealmOrUpdate(it)
            }
        })
    }

    /**
     * 更新通话状态
     */
    fun updateCallStreamStatus(myUid: Long, targetUid: Long, channelName: String, status: Int) {
        RealmCreator.executePvtChatTransactionAsync(myUid, targetUid, { realm ->
            realm.where(MessageModel::class.java).equalTo("flag", channelName).findFirst()?.let {
                val contentBean = it.streamMessageContent
                contentBean.status = status
                it.streamMessageContent = contentBean
                realm.copyToRealmOrUpdate(it)
            }
        })

        RealmCreator.executeStreamCallsHistoryTransactionAsync(myUid, { realm ->
            realm.where(StreamCallModel::class.java).equalTo("sessionId", channelName).findFirst()?.let {
                it.status = status
                realm.copyToRealmOrUpdate(it)
            }
        })
    }

    /**
     * 删除指定流媒体通话记录
     */
    fun deleteStreamCall(myUid: Long, targetUid: Long, channelName: String, complete: ((preMessage: MessageModel?, deleteIsUnread: Boolean) -> Unit)? = null, error: ((Throwable) -> Unit)? = null) {
        RealmCreator.executeStreamCallsHistoryTransactionAsync(myUid, { realm ->
            realm.where(StreamCallModel::class.java).equalTo("sessionId", channelName).findFirst()?.let {
                it.deleteFromRealm()
            }
        }, {
            MessagesManager.deleteToUserMessage(myUid, targetUid, channelName, complete, error)
        })
    }

    /**
     * 删除指定用户的流媒体通话记录
     */
    fun deleteStreamCallHistory(myUid: Long, targetUid: Long, complete: (() -> Unit)? = null, error: ((Throwable) -> Unit)? = null) {
        RealmCreator.executeStreamCallsHistoryTransactionAsync(myUid, { realm ->
            realm.where(StreamCallModel::class.java).equalTo("chaterId", targetUid).findAll()?.let {
                it.deleteAllFromRealm()
            }
        }, complete, error)
    }
}