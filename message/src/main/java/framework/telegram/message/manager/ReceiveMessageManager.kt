package framework.telegram.message.manager

import android.annotation.SuppressLint
import android.text.TextUtils
import android.util.Log
import com.alibaba.android.arouter.launcher.ARouter
import com.google.protobuf.ByteString
import com.im.domain.pb.CommonProto
import com.im.domain.pb.FriendMessageProto
import com.im.domain.pb.GroupMessageProto
import com.im.pb.IMPB
import com.umeng.analytics.MobclickAgent
import framework.ideas.common.model.common.SearchChatModel
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.ChatModel.CHAT_TYPE_GROUP
import framework.ideas.common.model.im.ChatModel.CHAT_TYPE_PVT
import framework.ideas.common.model.im.MessageModel
import framework.ideas.common.model.im.MessageReceiptModel
import framework.ideas.common.rlog.RLogManager
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.*
import framework.telegram.message.connect.MessageSocketService
import framework.telegram.message.connect.bean.SocketPackageBean
import framework.telegram.message.controller.MessageController
import framework.telegram.message.controller.StreamCallController
import framework.telegram.message.db.RealmCreator
import framework.telegram.message.event.InputtingStatusEvent
import framework.telegram.message.event.ScreenShotDetectionEvent
import framework.telegram.message.event.ScreenShotStateEvent
import framework.telegram.message.sp.CommonPref
import framework.telegram.message.ui.pvt.PrivateChatActivity
import framework.telegram.message.ui.telephone.core.RtcEngineHolder
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.*
import io.realm.Sort
import java.util.concurrent.CopyOnWriteArrayList

object ReceiveMessageManager {

    // 与socket服务器的时差(北京时间)
    var serverDifferenceTime: Long = 0

    var lastReceiveHeartTime: Long = System.currentTimeMillis()

    // socket是否活跃
    var socketIsLogin: Boolean = false

    // 是否正在更新key
    var updateAccountPublicKey: Boolean = false

    private val oneToOneMsgsCacheList by lazy { CopyOnWriteArrayList<IMPB.OneToOneMessage>() }
    private val groupMsgsCacheList by lazy { CopyOnWriteArrayList<IMPB.GroupMessage>() }
    private val oneToOneMsgSendSuccessCacheList by lazy { CopyOnWriteArrayList<IMPB.PushSendMessageSuccess>() }
    private val groupMsgSendSuccessCacheList by lazy { CopyOnWriteArrayList<IMPB.PushGroupMessageSendSuccess>() }
    private val oneToOneMsgReceiptsCacheList by lazy { CopyOnWriteArrayList<IMPB.PushReceiptMessage>() }
    private val groupMsgReceiptsCacheList by lazy { CopyOnWriteArrayList<IMPB.PushGroupMsgReceiptMessage>() }
    private val msgReceiptsSendSuccessCacheList by lazy { CopyOnWriteArrayList<IMPB.PushSendReceiptMessageSuccess>() }
    private var lastBatchHandleReceiveMsgsTime = 0L

    fun receiveMsg(msg: SocketPackageBean) {
        if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_HEART_RESP) {
            RLogManager.d(MessageSocketService.TAG, "收到心跳包答复--->")
            lastReceiveHeartTime = System.currentTimeMillis()
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_LOGIN_RESP) {
            val time = IMPB.PushLoginSuccessMessage.parseFrom(msg.data)

            // 设置时差
            serverDifferenceTime = time.loginTime - TimeUtils.currentTimeMillis()

            // 设置socket为活动的
            socketIsLogin = true

            // 还原连接次数
            MessageSocketService.connectRetryCount = 0

            // 保存web端状态
            val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
            accountInfo.putWebPublicKey(time.webKeyPair.publicKey)
            accountInfo.putWebPublicKeyVersion(time.webKeyPair.keyVersion)
            accountInfo.putWebOnline(if (time.bfWebOnline) 1 else 0)

            // 清除缓存的加密key
            ArouterServiceManager.systemService.clearMyselfWebSecretKeysCache()
            ArouterServiceManager.settingService.setMuteStatus(time.bfWebOnline)
            // 发送web端在线事件
            EventBus.publishEvent(
                WebOnlineStatusChangeEvent(
                    accountInfo.getUserId(),
                    time.bfWebOnline
                )
            )

            // 校验本地保存的publickey
            ArouterServiceManager.systemService.updateAccountKey(time.userKeyPair.publicKey, {
                // 查询所有消息状态，并进行状态重置或自动重发工作
                updateAccountPublicKey = true
            }, {
                updateAccountPublicKey = false
            })

            // 用户登录成功
            ThreadUtils.runOnUIThread {
                BaseApp.app.onUserLogin()

                // 发送socket连接状态变更事件
                EventBus.publishEvent(SocketStatusChangeEvent())

                RLogManager.d(MessageSocketService.TAG, "登录成功,与服务器时差--->$serverDifferenceTime")
            }
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_WEB_ONLINE_STATUS) {
            val body = IMPB.PushWebOnlineMessage.parseFrom(msg.data)
            RLogManager.d(MessageSocketService.TAG, "收到web端现在状态变更的消息--->${body.online}")

            val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
            accountInfo.putWebPublicKey(body.webKeyPair.publicKey)
            accountInfo.putWebPublicKeyVersion(body.webKeyPair.keyVersion)
            accountInfo.putWebOnline(if (body.online) 1 else 0)

            // 清除缓存的加密key
            ArouterServiceManager.systemService.clearMyselfWebSecretKeysCache()
            ArouterServiceManager.settingService.setMuteStatus(body.online)
            EventBus.publishEvent(WebOnlineStatusChangeEvent(accountInfo.getUserId(), body.online))
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_SEND_ONE_TO_ONE_MSG_RESP) {
            val body = IMPB.PushSendMessageSuccess.parseFrom(msg.data)
            RLogManager.d(MessageSocketService.TAG, "收到私聊消息发送成功的消息--->${body.msgId}")

            oneToOneMsgSendSuccessCacheList.add(body)
            checkBatchHandleReceiveMsgs()
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_SEND_GROUP_MSG_RESP) {
            val body = IMPB.PushGroupMessageSendSuccess.parseFrom(msg.data)
            RLogManager.d(MessageSocketService.TAG, "收到群聊消息发送成功的消息--->${body.msgId}")

            groupMsgSendSuccessCacheList.add(body)
            checkBatchHandleReceiveMsgs()
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_GET_ONE_TO_ONE_MSG) {
            RLogManager.d(MessageSocketService.TAG, "收到私聊消息--->")
            val body = IMPB.PushOneToOneMessage.parseFrom(msg.data)

            oneToOneMsgsCacheList.addAll(body.oneToOneMsgList)
            checkBatchHandleReceiveMsgs()
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_GET_GROUP_MSG) {
            RLogManager.d(MessageSocketService.TAG, "收到群聊消息--->")
            val body = IMPB.PushGroupMessage.parseFrom(msg.data)

            groupMsgsCacheList.addAll(body.groupMsgList)
            checkBatchHandleReceiveMsgs()
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_RECALL_PVT_MSG) {
            RLogManager.d(MessageSocketService.TAG, "收到撤回私聊消息--->")
            val body = IMPB.PushRecallOneToOneMessage.parseFrom(msg.data)
            recallPvtMessage(body)
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_RECALL_GROUP_MSG) {
            RLogManager.d(MessageSocketService.TAG, "收到撤回群聊消息--->")
            val body = IMPB.PushRecallGroupMessage.parseFrom(msg.data)
            recallGroupMessage(body)
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_GET_ONE_TO_ONE_STREAM_RESP) {
            RLogManager.d(MessageSocketService.TAG, "收到一对一的流媒体消息响应--->")
            val body = IMPB.PushSendOneToOneStreamReqSuccess.parseFrom(msg.data)
            receiveStreamCallResp(body)
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_GET_ONE_TO_ONE_STREAM_OPERATE) {
            RLogManager.d(MessageSocketService.TAG, "收到一对一的流媒体操作消息--->")
            val body = IMPB.PushOneToOneStreamOperateMessage.parseFrom(msg.data)
            receiveStreamCallReq(body)
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_GET_ONE_TO_ONE_STREAM_BUILD) {
            RLogManager.d(MessageSocketService.TAG, "收到双方同意建立一对一的流媒体消息--->")
            val body = IMPB.PushOneToOneStreamSuccess.parseFrom(msg.data)
            receiveStreamCallBuildReq(body)
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_GET_ONE_TO_ONE_STREAM_NEW_TOKEN) {
            RLogManager.d(MessageSocketService.TAG, "收到一对一的流媒体新token--->")
            val body = IMPB.PushStreamNewToken.parseFrom(msg.data)
            receiveStreamCallNewToken(body)
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_ADD_CONTACT_OPERATE) {
            RLogManager.d(MessageSocketService.TAG, "收到好友处理消息--->")
            val body = IMPB.PushFriendRecordMessage.parseFrom(msg.data)
            receiveJoinContactOperate(body)
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_ADD_CONTACT) {
            RLogManager.d(MessageSocketService.TAG, "收到好友请求数量--->")
            val body = IMPB.PushFriendReqNum.parseFrom(msg.data)
            receiveJoinContactReqNum(body)
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_JOIN_GROUP) {
            RLogManager.d(MessageSocketService.TAG, "收到加群请求数量--->")
            val body = IMPB.PushGroupReqNum.parseFrom(msg.data)
            receiveGroupOperateReqNum(body)
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_JOIN_GROUP_OPERATE) {
            RLogManager.d(MessageSocketService.TAG, "收到加群处理消息--->")
            val body = IMPB.PushGroupReqMessage.parseFrom(msg.data)
            receiveGroupOperateReq(body)
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_KICK_OFF) {
            RLogManager.d(MessageSocketService.TAG, "服务器发现有多条连接--->")
            MessageSocketService.disConnect("服务器发现有多条连接，主动断开连接--->")
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_USER_ON_OFF_LINE) {
            RLogManager.d(MessageSocketService.TAG, "用户上线下线信息--->")
            val body = IMPB.PushUserOnOrOffLineMessage.parseFrom(msg.data)
            receiveUserOnOffline(body)
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_KICK_USER) {
            RLogManager.d(MessageSocketService.TAG, "收到服务器踢人消息--->")
            //重新登录
            val kickMsg = IMPB.PushKickUserMessage.parseFrom(msg.data)
            if (kickMsg.kickUserTip.kickType == 4) {
                // 强制更新，不踢出来，而是禁止连接socket
                MessageSocketService.exitSocket = true
                MessageSocketService.disConnect("APP强制更新，主动断开连接--->")
            } else {
                BaseApp.app.onUserLogout(kickMsg.kickUserTip.tip)
            }
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_RECEIPT_MSG) {
            RLogManager.d(MessageSocketService.TAG, "收到私聊回执消息--->")
            val receiptMsgs = IMPB.PushReceiptMessage.parseFrom(msg.data)

            oneToOneMsgReceiptsCacheList.add(receiptMsgs)
            checkBatchHandleReceiveMsgs()
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_PUSH_GROUP_MSG_RECEIPT_RESP) {
            RLogManager.d(MessageSocketService.TAG, "收到群聊回执消息--->")
            val receiptMsgs = IMPB.PushGroupMsgReceiptMessage.parseFrom(msg.data)

            groupMsgReceiptsCacheList.add(receiptMsgs)
            checkBatchHandleReceiveMsgs()
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_RECEIPT_MSG_RESP) {
            RLogManager.d(MessageSocketService.TAG, "收到回执发送成功消息--->")
            val receiptRespMsgs = IMPB.PushSendReceiptMessageSuccess.parseFrom(msg.data)

            msgReceiptsSendSuccessCacheList.add(receiptRespMsgs)
            checkBatchHandleReceiveMsgs()
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_RECALL_GROUP_MSG_RESP) {
            RLogManager.d(MessageSocketService.TAG, "收到撤回群消息成功消息--->")
            val recallGroupMessage = IMPB.PushGroupRecallMessageSuccess.parseFrom(msg.data)
            val recallMessage = recallGroupMessage.recallMessage
            val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
            RealmCreator.executeGroupChatTransactionAsync(
                myUid,
                recallMessage.msgTargetId,
                { realm ->
                    if (recallMessage.msgId > 0) {
                        val model = realm.where(MessageModel::class.java)
                            .equalTo("msgId", recallMessage.msgId).findFirst()
                        if (model != null) {
                            model.type = MessageModel.MESSAGE_TYPE_RECALL_SUCCESS
                            realm.copyToRealmOrUpdate(model)
                        }
                    } else if (recallMessage.clearTime > 0) {
                        val models = realm.where(MessageModel::class.java)
                            .lessThanOrEqualTo("time", recallMessage.clearTime).findAll()
                        models?.forEach { model ->
                            model.type = MessageModel.MESSAGE_TYPE_RECALL_SUCCESS
                        }
                        realm.copyToRealmOrUpdate(models)
                    }
                })

            RealmCreator.executeDeleteChatsHistoryTransactionAsync(myUid, { realm ->
                realm.where(ChatModel::class.java).equalTo("chaterType", ChatModel.CHAT_TYPE_GROUP)
                    .and()
                    .equalTo("chaterId", recallMessage.msgTargetId).findAll()?.deleteAllFromRealm()
            })
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_RECALL_PVT_MSG_RESP) {
            RLogManager.d(MessageSocketService.TAG, "收到撤回私聊消息成功消息--->")
            val recallPvtMessage = IMPB.PushOneToOneRecallMessageSuccess.parseFrom(msg.data)
            val recallMessage = recallPvtMessage.recallMessage
            val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
            Log.d("demo", "收到撤回${recallMessage.msgTargetId}私聊消息成功消息--->")
            RealmCreator.executePvtChatTransactionAsync(
                myUid,
                recallMessage.msgTargetId,
                { realm ->
                    if (recallMessage.msgId > 0) {
                        val model = realm.where(MessageModel::class.java)
                            .equalTo("msgId", recallMessage.msgId).findFirst()
                        if (model != null) {
                            model.type = MessageModel.MESSAGE_TYPE_RECALL_SUCCESS
                            realm.copyToRealmOrUpdate(model)
                        }
                    } else if (!TextUtils.isEmpty(recallMessage.channelName)) {
                        val model = realm.where(MessageModel::class.java)
                            .equalTo("flag", recallMessage.channelName).findFirst()
                        if (model != null) {
                            model.type = MessageModel.MESSAGE_TYPE_RECALL_SUCCESS
                            realm.copyToRealmOrUpdate(model)
                        }
                    } else if (recallMessage.clearTime > 0) {
                        Log.d("demo", "将 ${recallMessage.msgTargetId} 会话的所有消息标记为已撤回")
                        val models = realm.where(MessageModel::class.java)
                            .lessThanOrEqualTo("time", recallMessage.clearTime).findAll()
                        models?.forEach { model ->
                            model.type = MessageModel.MESSAGE_TYPE_RECALL_SUCCESS
                        }
                        realm.copyToRealmOrUpdate(models)
                    }
                })

            RealmCreator.executeDeleteChatsHistoryTransactionAsync(myUid, { realm ->
                Log.d("demo", "将 ${recallMessage.msgTargetId} 从需远程删除的会话记录中移除")
                realm.where(ChatModel::class.java).equalTo("chaterType", ChatModel.CHAT_TYPE_PVT)
                    .and()
                    .equalTo("chaterId", recallMessage.msgTargetId).findAll()?.deleteAllFromRealm()
            })
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_KEYPAIR_CHANGE_RESP) {
            RLogManager.d(MessageSocketService.TAG, "收到用户公钥发生变更消息--->")
            val keyPairChangeMsg = IMPB.PushKeyPairChangeMessage.parseFrom(msg.data)

            val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
            if (keyPairChangeMsg.uid == myUid) {
                // 自己的web端秘钥变化
                val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
                if (accountInfo.getWebPublicKeyVersion() > keyPairChangeMsg.webKeyPair.keyVersion) {
                    accountInfo.putWebPublicKey(keyPairChangeMsg.webKeyPair.publicKey)
                    accountInfo.putWebPublicKeyVersion(keyPairChangeMsg.webKeyPair.keyVersion)
                    ArouterServiceManager.systemService.clearMyselfWebSecretKeysCache()
                }
            } else {
                updateKeyPair(CHAT_TYPE_PVT, keyPairChangeMsg.uid) {
                    SendMessageManager.sendRecvKeypairMessagesPackage(
                        keyPairChangeMsg.keyPair.keyVersion,
                        keyPairChangeMsg.uid
                    )
                }
            }
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_INPUTTING_STATUS) {
            RLogManager.d(MessageSocketService.TAG, "收到用户正在输入消息--->")
            val content = IMPB.PushOneToOneInputMessage.parseFrom(msg.data)
            EventBus.publishEvent(InputtingStatusEvent(content.sendUid))
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_SEREEN_SHOTS) {
            RLogManager.d(MessageSocketService.TAG, "收到对方截取屏幕消息--->")
            val screenShotMsg = IMPB.PushScreenHotsMessage.parseFrom(msg.data)
            screenShotMsg.freindMsgsList?.forEach { friendMsg ->
                var hasActivity = false
                val uid = friendMsg.contactsDetail.userInfo.uid
                val activities = ActivitiesHelper.getInstance()
                    .getTargetActivity(PrivateChatActivity::class.java)
                activities?.forEach { act ->
                    if (act is PrivateChatActivity) {
                        if (act.getTargetUid() == uid) {
                            hasActivity = true
                        }
                    }
                }

                if (hasActivity) {
                    val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                    EventBus.publishEvent(ScreenShotDetectionEvent(uid, myUid))
                } else {
                    // 插入一条开启阅后即焚的消息到消息列表中，并生成会话记录
                    ArouterServiceManager.messageService.insertSystemTipMsg(
                        CHAT_TYPE_PVT,
                        uid,
                        friendMsg.createTime,
                        String.format(
                            BaseApp.app.getString(R.string.a_screenshot_of_the_chat_was_taken),
                            getDisplayName(friendMsg.contactsDetail.userInfo)
                        )
                    )
                }
            }
        } else if (msg.messageType == SocketPackageBean.MESSAGE_TYPE_ERROR) {
            val errorMsg = IMPB.ErrrMessage.parseFrom(msg.data)
            if (errorMsg != null) {
                RLogManager.e(MessageSocketService.TAG, "发生错误--->" + errorMsg.errorMsgCode)

                when (errorMsg.errorMsgCode) {
                    100L -> {
                        //重新登录
                        val oldToken =
                            AccountManager.getLoginAccount(AccountInfo::class.java).getSessionId()
                        ArouterServiceManager.systemService.intercept(100, oldToken)
                    }
                    8000L -> {
                        //未登录
                        MessageSocketService.loginAccount()
                    }
                    else -> {
                        when (errorMsg.messageProtocolId.toShort()) {
                            SocketPackageBean.MESSAGE_TYPE_SEND_ONE_TO_ONE_MSG_REQ -> {
                                receivePvtErrorTipMsg(
                                    errorMsg.targetId,
                                    errorMsg.flag.toString(),
                                    errorMsg.errorMsgCode,
                                    errorMsg.errorMsg
                                )
                            }
                            SocketPackageBean.MESSAGE_TYPE_SEND_GROUP_MSG_REQ -> {
                                receiveGroupErrorTipMsg(
                                    errorMsg.targetId,
                                    errorMsg.flag.toString(),
                                    errorMsg.errorMsgCode,
                                    errorMsg.errorMsg
                                )
                            }
                            SocketPackageBean.MESSAGE_TYPE_SEND_ONE_TO_ONE_STREAM_REQ -> {
                                when (errorMsg.errorMsgCode) {
                                    6302L -> {
                                        StreamCallController.endCall(
                                            BaseApp.app.getString(R.string.other_account_unregistration),
                                            true
                                        )
                                    }
                                    6500L -> {
                                        StreamCallController.endCall(
                                            BaseApp.app.getString(R.string.they_refused_to_talk),
                                            true
                                        )
                                    }
                                    6501L -> {
                                        StreamCallController.endCall(
                                            BaseApp.app.getString(R.string.you_not_target_linkman),
                                            true
                                        )
                                    }
                                    else -> {
                                        if (!TextUtils.isEmpty(errorMsg.errorMsg)) {
                                            StreamCallController.endCall(errorMsg.errorMsg, true)
                                        } else {
                                            StreamCallController.endCall(
                                                BaseApp.app.getString(R.string.error_close_call),
                                                true
                                            )
                                        }

                                    }
                                }
                            }
                            else -> {

                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkBatchHandleReceiveMsgs() {
        val leftTime = System.currentTimeMillis() - lastBatchHandleReceiveMsgsTime
        if (leftTime < 1500) {
            RLogManager.d(MessageSocketService.TAG, "${1500 - leftTime}毫秒后执行批处理--->")
            ThreadUtils.runOnIOThread(1500 - leftTime) {
                RLogManager.d(MessageSocketService.TAG, "时间到，执行批处理--->")
                batchHandleReceiveMsgs()
            }
        } else {
            RLogManager.d(MessageSocketService.TAG, "直接执行批处理--->")
            batchHandleReceiveMsgs()
        }
    }

    @Synchronized
    private fun batchHandleReceiveMsgs() {
        when {
            oneToOneMsgsCacheList.isNotEmpty() -> {
                RLogManager.d(MessageSocketService.TAG, "批处理收到的私聊消息--->")
                val cache = mutableListOf<IMPB.OneToOneMessage>()
                cache.addAll(oneToOneMsgsCacheList)
                oneToOneMsgsCacheList.clear()
                receiveMsgFromUser(cache)
            }
            groupMsgsCacheList.isNotEmpty() -> {
                RLogManager.d(MessageSocketService.TAG, "批处理收到的群消息--->")
                val cache = mutableListOf<IMPB.GroupMessage>()
                cache.addAll(groupMsgsCacheList)
                groupMsgsCacheList.clear()
                receiveMsgFromGroup(cache)
            }
            oneToOneMsgSendSuccessCacheList.isNotEmpty() -> {
                RLogManager.d(MessageSocketService.TAG, "批处理私聊发送成功消息--->")
                val cache = mutableListOf<IMPB.PushSendMessageSuccess>()
                cache.addAll(oneToOneMsgSendSuccessCacheList)
                oneToOneMsgSendSuccessCacheList.clear()
                receiveSendedPvtMsgResp(cache)
            }
            groupMsgSendSuccessCacheList.isNotEmpty() -> {
                RLogManager.d(MessageSocketService.TAG, "批处理群聊发送成功消息--->")
                val cache = mutableListOf<IMPB.PushGroupMessageSendSuccess>()
                cache.addAll(groupMsgSendSuccessCacheList)
                groupMsgSendSuccessCacheList.clear()
                receiveSendedGroupMsgResp(cache)
            }
            oneToOneMsgReceiptsCacheList.isNotEmpty() -> {
                RLogManager.d(MessageSocketService.TAG, "批处理私聊回执消息--->")
                val cache = mutableListOf<IMPB.PushReceiptMessage>()
                cache.addAll(oneToOneMsgReceiptsCacheList)
                oneToOneMsgReceiptsCacheList.clear()
                receivePrivateMsgReceipts(cache)
            }
            groupMsgReceiptsCacheList.isNotEmpty() -> {
                RLogManager.d(MessageSocketService.TAG, "批处理群聊回执消息--->")
                val cache = mutableListOf<IMPB.PushGroupMsgReceiptMessage>()
                cache.addAll(groupMsgReceiptsCacheList)
                groupMsgReceiptsCacheList.clear()
                receiveGroupMsgReceipts(cache)
            }
            msgReceiptsSendSuccessCacheList.isNotEmpty() -> {
                RLogManager.d(MessageSocketService.TAG, "批处理回执发送成功消息--->")
                val cache = mutableListOf<IMPB.PushSendReceiptMessageSuccess>()
                cache.addAll(msgReceiptsSendSuccessCacheList)
                msgReceiptsSendSuccessCacheList.clear()
                receiveMsgReceiptsResp(cache)
            }
        }

        lastBatchHandleReceiveMsgsTime = System.currentTimeMillis()
    }

    private fun updateKeyPair(chaterType: Int, chaterId: Long, complete: (() -> Unit)? = null) {
        if (chaterType == CHAT_TYPE_PVT) {
            ArouterServiceManager.systemService.updateUserPublicKey(chaterId, 0, 0, { _, _, _, _ ->
                complete?.invoke()
                RLogManager.d(MessageSocketService.TAG, "用户公钥更新成功--->")
            }, {
                RLogManager.d(MessageSocketService.TAG, "用户公钥更新失败--->")
            })
        } else if (chaterType == ChatModel.CHAT_TYPE_GROUP) {
            ArouterServiceManager.systemService.updateGroupPublicKey(chaterId, { _, _, _ ->
                complete?.invoke()
                RLogManager.d(MessageSocketService.TAG, "群公钥更新成功--->")
            }, {
                RLogManager.d(MessageSocketService.TAG, "群公钥更新失败--->")
            })
        }
    }

    private fun receiveMsgReceiptsResp(receiptMsgs: List<IMPB.PushSendReceiptMessageSuccess>) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val msgReceiptsMap = HashMap<String, ArrayList<IMPB.ReceiptMessage>>()
        receiptMsgs.forEach {
            it.receiptsList.forEach { receiptMsg ->
                val chatType =
                    if (receiptMsg.type.number == IMPB.ChatMessageType.ONE_TO_ONE.number) CHAT_TYPE_PVT else ChatModel.CHAT_TYPE_GROUP
                val targetId =
                    if (receiptMsg.type.number == IMPB.ChatMessageType.ONE_TO_ONE.number) receiptMsg.sendUid else receiptMsg.groupId
                if (msgReceiptsMap["${chatType}_$targetId"] == null) {
                    msgReceiptsMap["${chatType}_$targetId"] = ArrayList()
                }
                msgReceiptsMap["${chatType}_$targetId"]?.add(receiptMsg)
            }
        }

        msgReceiptsMap.iterator().forEach {
            val keys = it.key.split("_")
            val chatType = keys[0].toInt()
            val targetId = keys[1].toLong()
            val notFindedReceiptMsg = mutableListOf<IMPB.ReceiptMessage>()
            MessagesManager.executeChatTransactionAsync(chatType, myUid, targetId, { realm ->
                it.value.forEach { receiptMsg ->
                    val msgModel =
                        realm.where(MessageModel::class.java)?.equalTo("isSend", 0.toInt())?.and()
                            ?.equalTo("msgId", receiptMsg.msgId)?.findFirst()
                    if (msgModel != null) {
                        when (receiptMsg.receiptStatus.status.number) {
                            CommonProto.MsgReceiptStatus.DELIVERED.number -> {
                                RLogManager.d(
                                    MessageSocketService.TAG,
                                    "确认发出消息回执--->isDeliver myUid:$myUid,chatType:$chatType,targetId:$targetId,msgId:${receiptMsg.msgId}"
                                )
                                msgModel.isDeliver = 1
                            }
                            CommonProto.MsgReceiptStatus.VIEWED.number -> {
                                RLogManager.d(
                                    MessageSocketService.TAG,
                                    "确认发出消息回执--->isRead myUid:$myUid,chatType:$chatType,targetId:$targetId,msgId:${receiptMsg.msgId}"
                                )
                                msgModel.isRead = 1
                            }
                            CommonProto.MsgReceiptStatus.PLAYED.number -> {
                                RLogManager.d(
                                    MessageSocketService.TAG,
                                    "确认发出消息回执--->isReadedAttachment myUid:$myUid,chatType:$chatType,targetId:$targetId,msgId:${receiptMsg.msgId}"
                                )
                                msgModel.isReadedAttachment = 1
                            }
                        }
                        realm.copyToRealmOrUpdate(msgModel)
                    } else {
                        notFindedReceiptMsg.add(receiptMsg)
                    }
                }
            }, {
                msgReceiptsSendSuccessCacheList.add(
                    IMPB.PushSendReceiptMessageSuccess.newBuilder()
                        .addAllReceipts(notFindedReceiptMsg).build()
                )
            })
        }
    }

    /**
     * 收到消息回执
     */
    private fun receivePrivateMsgReceipts(receiptMsgs: List<IMPB.PushReceiptMessage>) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val otherMsgReceiptsMap = HashMap<String, ArrayList<IMPB.ReceiptMessage>>()
        val otherWebMsgReceiptsMap = HashMap<String, ArrayList<IMPB.ReceiptMessage>>()
        val myselfWebMsgReceiptsMap = HashMap<String, ArrayList<IMPB.ReceiptMessage>>()
        receiptMsgs.forEach {
            it.receiptsList.forEach { receiptMsg ->
                val targetId = if (receiptMsg.sendUid == myUid) {
                    receiptMsg.targetId
                } else {
                    receiptMsg.sendUid
                }

                if (receiptMsg.source == IMPB.MessageSource.WEB) {
                    if (receiptMsg.sendUid == myUid) {
                        if (myselfWebMsgReceiptsMap["$targetId"] == null) {
                            myselfWebMsgReceiptsMap["$targetId"] = ArrayList()
                        }
                        myselfWebMsgReceiptsMap["$targetId"]?.add(receiptMsg)
                    } else {
                        if (otherWebMsgReceiptsMap["$targetId"] == null) {
                            otherWebMsgReceiptsMap["$targetId"] = ArrayList()
                        }
                        otherWebMsgReceiptsMap["$targetId"]?.add(receiptMsg)
                    }
                } else {
                    if (otherMsgReceiptsMap["$targetId"] == null) {
                        otherMsgReceiptsMap["$targetId"] = ArrayList()
                    }
                    otherMsgReceiptsMap["$targetId"]?.add(receiptMsg)
                }
            }
        }

        otherMsgReceiptsMap.forEach {
            val targetId = it.key.toLong()
            receivePrivateMsgReceipts(myUid, targetId, it.value, IMPB.MessageSource.APP)
        }

        otherWebMsgReceiptsMap.forEach {
            val targetId = it.key.toLong()
            receivePrivateMsgReceipts(myUid, targetId, it.value, IMPB.MessageSource.WEB)
        }

        myselfWebMsgReceiptsMap.forEach {
            val targetId = it.key.toLong()
            receiveMyselfWebSyncPrivateMsgReceipts(myUid, targetId, it.value)
        }
    }

    private fun receiveGroupMsgReceipts(receiptMsgs: List<IMPB.PushGroupMsgReceiptMessage>) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val msgReceiptsMap = HashMap<String, ArrayList<IMPB.ReceiptMessage>>()
        val myselfWebMsgReceiptsMap = HashMap<String, ArrayList<IMPB.ReceiptMessage>>()
        receiptMsgs.forEach {
            it.receiptMessageList.forEach { receiptMsg ->
                val targetId = receiptMsg.groupId
                if (receiptMsg.sendUid == myUid) {
                    if (myselfWebMsgReceiptsMap["$targetId"] == null) {
                        myselfWebMsgReceiptsMap["$targetId"] = ArrayList()
                    }
                    myselfWebMsgReceiptsMap["$targetId"]?.add(receiptMsg)
                } else {
                    if (msgReceiptsMap["$targetId"] == null) {
                        msgReceiptsMap["$targetId"] = ArrayList()
                    }
                    msgReceiptsMap["$targetId"]?.add(receiptMsg)
                }
            }
        }

        msgReceiptsMap.forEach {
            val targetId = it.key.toLong()
            receiveGroupMsgReceipts(myUid, targetId, it.value)
        }

        myselfWebMsgReceiptsMap.forEach {
            val targetId = it.key.toLong()
            receiveMyselfWebSyncGroupMsgReceipts(myUid, targetId, it.value)
        }
    }

    private fun receivePrivateMsgReceipts(
        myUid: Long,
        targetId: Long,
        receiptMsgs: List<IMPB.ReceiptMessage>,
        source: IMPB.MessageSource
    ) {
        val msgs = mutableListOf<MessageModel>()
        val findedReceiptMsg = mutableListOf<IMPB.ReceiptMessage>()
        val notFindedReceiptMsg = mutableListOf<IMPB.ReceiptMessage>()
        MessagesManager.executeChatTransactionAsync(CHAT_TYPE_PVT, myUid, targetId, { realm ->
            receiptMsgs.forEach { receiptMsg ->
                val time =
                    if (receiptMsg.receiptStatus.time == 0L) ArouterServiceManager.messageService.getCurrentTime() else receiptMsg.receiptStatus.time
                val msgModel =
                    realm.where(MessageModel::class.java)?.equalTo("isSend", 1.toInt())?.and()
                        ?.equalTo("msgId", receiptMsg.msgId)?.findFirst()
                if (msgModel != null) {
                    when (receiptMsg.receiptStatus.status.number) {
                        CommonProto.MsgReceiptStatus.DELIVERED.number -> {
                            RLogManager.d(
                                MessageSocketService.TAG,
                                "消息回执--->isDeliver myUid:$myUid,chatType:$CHAT_TYPE_PVT,targetId:$targetId,msgId:${receiptMsg.msgId}"
                            )
                            msgModel.isDeliver = 1
                            msgModel.deliverTime = time
                        }
                        CommonProto.MsgReceiptStatus.VIEWED.number -> {
                            RLogManager.d(
                                MessageSocketService.TAG,
                                "消息回执--->isRead myUid:$myUid,chatType:$CHAT_TYPE_PVT,targetId:$targetId,msgId:${receiptMsg.msgId}"
                            )
                            msgModel.isRead = 1
                            msgModel.readTime = time

                            if (msgModel.snapchatTime > 0 && msgModel.expireTime <= 0L) {
                                msgModel.expireTime =
                                    System.currentTimeMillis() + msgModel.snapchatTime * 1000
                            }

                            var expireTime = msgModel.expireTime
                            EventBus.publishEvent(
                                ReadAttachmentEvent(
                                    targetId,
                                    receiptMsg.msgId,
                                    expireTime
                                )
                            )

                            // 之前收到的消息全部置为已读
                            val msgModels = realm.where(MessageModel::class.java)
                                ?.lessThanOrEqualTo("msgId", msgModel.msgId)
                                ?.and()?.equalTo("isSend", 1.toInt())
                                ?.and()?.equalTo("isRead", 0L)?.findAll()
                            msgModels?.forEach { msg ->
                                if (msg.isRead != 1) {
                                    msg.isRead = 1
                                    msg.readTime = time

                                    if (msg.snapchatTime > 0 && msg.expireTime <= 0L) {
                                        msg.expireTime =
                                            System.currentTimeMillis() + msg.snapchatTime * 1000
                                    }

                                    expireTime = msgModel.expireTime
                                    EventBus.publishEvent(
                                        ReadAttachmentEvent(
                                            targetId,
                                            receiptMsg.msgId,
                                            expireTime
                                        )
                                    )
                                }
                            }
                            msgModels?.let {
                                realm.copyToRealmOrUpdate(it)
                            }
                        }
                        CommonProto.MsgReceiptStatus.PLAYED.number -> {
                            RLogManager.d(
                                MessageSocketService.TAG,
                                "消息回执--->isReadedAttachment myUid:$myUid,chatType:$CHAT_TYPE_PVT,targetId:$targetId,msgId:${receiptMsg.msgId}"
                            )
                            msgModel.isReadedAttachment = 1
                            msgModel.readedAttachmentTime = time
                        }
                    }

                    msgs.add(msgModel.copyMessage())
                    realm.copyToRealmOrUpdate(msgModel)
                    findedReceiptMsg.add(receiptMsg)
                } else {
                    if (System.currentTimeMillis() - receiptMsg.receiptStatus.time > Constant.Common.AUTO_CLEAR_MESSAGE_MAX_TIME) {
                        findedReceiptMsg.add(receiptMsg)
                    } else {
                        notFindedReceiptMsg.add(receiptMsg)
                    }
                }
            }
        }, {
            EventBus.publishEvent(MessageStateChangeEvent(msgs))
            SendMessageManager.sendRecvPvtReceiptMessagePackage(findedReceiptMsg)
            oneToOneMsgReceiptsCacheList.add(
                IMPB.PushReceiptMessage.newBuilder().addAllReceipts(notFindedReceiptMsg).build()
            )
        })
    }

    private fun receiveMyselfWebSyncPrivateMsgReceipts(
        myUid: Long,
        targetId: Long,
        receiptMsgs: List<IMPB.ReceiptMessage>
    ) {
        var isChange = false
        val msgs = mutableListOf<MessageModel>()
        val findedReceiptMsg = mutableListOf<IMPB.ReceiptMessage>()
        val notFindedReceiptMsg = mutableListOf<IMPB.ReceiptMessage>()
        MessagesManager.executeChatTransactionAsync(CHAT_TYPE_PVT, myUid, targetId, { realm ->
            receiptMsgs.forEach { receiptMsg ->
                if (receiptMsg.receiptStatus.status.number != CommonProto.MsgReceiptStatus.DELIVERED.number) {
                    val time =
                        if (receiptMsg.receiptStatus.time == 0L) ArouterServiceManager.messageService.getCurrentTime() else receiptMsg.receiptStatus.time
                    val msgModel =
                        realm.where(MessageModel::class.java)?.equalTo("isSend", 0.toInt())?.and()
                            ?.equalTo("msgId", receiptMsg.msgId)?.findFirst()
                    if (msgModel != null) {
                        when (receiptMsg.receiptStatus.status.number) {
                            CommonProto.MsgReceiptStatus.VIEWED.number -> {
                                RLogManager.d(
                                    MessageSocketService.TAG,
                                    "消息回执--->isRead myUid:$myUid,chatType:$CHAT_TYPE_PVT,targetId:$targetId,msgId:${receiptMsg.msgId}"
                                )
                                if (msgModel.isRead != 1) {
                                    msgModel.isRead = 1
                                    msgModel.readTime = time

                                    if (msgModel.snapchatTime > 0 && msgModel.expireTime <= 0L) {
                                        val t = System.currentTimeMillis()
                                        msgModel.expireTime = t + msgModel.snapchatTime * 1000
                                    }

                                    var expireTime = msgModel.expireTime
                                    EventBus.publishEvent(
                                        ReadAttachmentEvent(
                                            targetId,
                                            receiptMsg.msgId,
                                            expireTime
                                        )
                                    )

                                    realm.copyToRealmOrUpdate(msgModel)

                                    // 之前收到的消息全部置为已读
                                    val msgModels = realm.where(MessageModel::class.java)
                                        ?.lessThanOrEqualTo("msgId", msgModel.msgId)
                                        ?.and()?.equalTo("isSend", 1.toInt())
                                        ?.and()?.equalTo("isRead", 0L)?.findAll()
                                    msgModels?.forEach { msg ->
                                        if (msg.isRead != 1) {
                                            msg.isRead = 1
                                            msg.readTime = time

                                            if (msgModel.snapchatTime > 0 && msgModel.expireTime <= 0L) {
                                                msgModel.expireTime =
                                                    System.currentTimeMillis() + msgModel.snapchatTime * 1000
                                            }

                                            expireTime = msgModel.expireTime
                                            EventBus.publishEvent(
                                                ReadAttachmentEvent(
                                                    targetId,
                                                    receiptMsg.msgId,
                                                    expireTime
                                                )
                                            )
                                        }
                                    }
                                    realm.copyToRealmOrUpdate(msgModels)

                                    isChange = true
                                }
                            }
                            CommonProto.MsgReceiptStatus.PLAYED.number -> {
                                RLogManager.d(
                                    MessageSocketService.TAG,
                                    "消息回执--->isReadedAttachment myUid:$myUid,chatType:$CHAT_TYPE_PVT,targetId:$targetId,msgId:${receiptMsg.msgId}"
                                )
                                if (msgModel.isReadedAttachment != 1) {
                                    msgModel.isReadedAttachment = 1
                                    msgModel.readedAttachmentTime = time

                                    realm.copyToRealmOrUpdate(msgModel)

                                    isChange = true
                                }
                            }
                        }
                        msgs.add(msgModel.copyMessage())

                        findedReceiptMsg.add(receiptMsg)
                    } else {
                        if (System.currentTimeMillis() - receiptMsg.receiptStatus.time > Constant.Common.AUTO_CLEAR_MESSAGE_MAX_TIME) {
                            findedReceiptMsg.add(receiptMsg)
                        } else {
                            notFindedReceiptMsg.add(receiptMsg)
                        }
                    }
                } else {
                    findedReceiptMsg.add(receiptMsg)
                }
            }
        }, {
            SendMessageManager.sendRecvPvtReceiptMessagePackage(findedReceiptMsg)
            oneToOneMsgReceiptsCacheList.add(
                IMPB.PushReceiptMessage.newBuilder().addAllReceipts(notFindedReceiptMsg).build()
            )

            // 收到web端发给app端的回执确认
            if (isChange) {
                EventBus.publishEvent(MessageStateChangeEvent(msgs))

                var unReadCount = 0L
                RealmCreator.executePvtChatTransactionAsync(myUid, targetId, { realm ->
                    unReadCount =
                        realm.where(MessageModel::class.java).equalTo("isSend", 0.toInt()).and()
                            .equalTo("isRead", 0.toInt()).count()
                }, {
                    //  //  屏蔽，这里没有msgmodel 会导致 chat的时间错乱
//                    ChatsHistoryManager.checkChatHistoryIsCreated(CHAT_TYPE_PVT, myUid, targetId, unReadCount = unReadCount.toInt(), forcedCreate = false) {
//                        EventBus.publishEvent(UnreadMessageEvent(CHAT_TYPE_PVT, targetId))
//                    }
                })
            }
        })
    }

    private fun receiveGroupMsgReceipts(
        myUid: Long,
        targetId: Long,
        receiptMsgs: List<IMPB.ReceiptMessage>
    ) {
        val findedReceiptMsg = mutableListOf<IMPB.ReceiptMessage>()
        val notFindedReceiptMsg = mutableListOf<IMPB.ReceiptMessage>()
        MessagesManager.executeChatTransactionAsync(
            ChatModel.CHAT_TYPE_GROUP,
            myUid,
            targetId,
            { realm ->
                receiptMsgs.forEach { receiptMsg ->
                    val msgModel =
                        realm.where(MessageModel::class.java)?.equalTo("msgId", receiptMsg.msgId)
                            ?.findFirst()
                    var isChanged = false
                    if (msgModel != null) {
                        val time =
                            if (receiptMsg.receiptStatus.time == 0L) ArouterServiceManager.messageService.getCurrentTime() else receiptMsg.receiptStatus.time

                        // 修改回执记录表
                        var msgReceiptModel = realm.where(MessageReceiptModel::class.java)
                            ?.equalTo("msgId", receiptMsg.msgId)
                            ?.and()
                            ?.equalTo("senderUid", receiptMsg.sendUid)?.findFirst()
                        if (msgReceiptModel == null) {
                            msgReceiptModel = MessageReceiptModel()
                            msgReceiptModel.id = System.nanoTime();
                            msgReceiptModel.msgId = receiptMsg.msgId
                            msgReceiptModel.messageType = receiptMsg.messageType.number
                            msgReceiptModel.senderUid = receiptMsg.sendUid
                        }

                        if (receiptMsg.receiptStatus.status.number == CommonProto.MsgReceiptStatus.DELIVERED.number) {
                            msgReceiptModel.deliverTime = time
                        } else if (receiptMsg.receiptStatus.status.number == CommonProto.MsgReceiptStatus.VIEWED.number) {
                            msgReceiptModel.readTime = time
                            if (msgReceiptModel.deliverTime == 0L) {
                                msgReceiptModel.deliverTime = time
                            }
                        } else if (receiptMsg.receiptStatus.status.number == CommonProto.MsgReceiptStatus.PLAYED.number) {
                            msgReceiptModel.readedAttachmentTime = time
                            if (msgReceiptModel.deliverTime == 0L) {
                                msgReceiptModel.deliverTime = time
                            }
                            if (msgReceiptModel.readTime == 0L) {
                                msgReceiptModel.readTime = time
                            }
                        }

                        realm.copyToRealmOrUpdate(msgReceiptModel)

                        // 修改消息表
                        val targetReceiptCount = msgModel.receiptCount - 1
                        val readedAttachmentCount = realm.where(MessageReceiptModel::class.java)
                            ?.equalTo("msgId", receiptMsg.msgId)
                            ?.and()
                            ?.greaterThan("readedAttachmentTime", 0)
                            ?.count() ?: 0
                        if (readedAttachmentCount >= targetReceiptCount) {
                            val lastMsgReceiptModelTime =
                                realm.where(MessageReceiptModel::class.java)
                                    ?.equalTo("msgId", receiptMsg.msgId)
                                    ?.sort("readedAttachmentTime", Sort.DESCENDING)
                                    ?.findFirst()?.readedAttachmentTime ?: 0

                            if (msgModel.isDeliver != 1) {
                                msgModel.isDeliver = 1
                                msgModel.deliverTime = lastMsgReceiptModelTime
                                isChanged = true
                            }

                            if (msgModel.isRead != 1) {
                                msgModel.isRead = 1
                                msgModel.readTime = lastMsgReceiptModelTime
                                isChanged = true
                            }

                            if (msgModel.isReadedAttachment != 1) {
                                msgModel.isReadedAttachment = 1
                                msgModel.readedAttachmentTime = lastMsgReceiptModelTime
                                isChanged = true
                            }
                        } else {
                            val readedCount = realm.where(MessageReceiptModel::class.java)
                                ?.equalTo("msgId", receiptMsg.msgId)
                                ?.and()
                                ?.greaterThan("readTime", 0)
                                ?.count() ?: 0
                            if (readedCount >= targetReceiptCount) {
                                val lastMsgReceiptModelTime =
                                    realm.where(MessageReceiptModel::class.java)
                                        ?.equalTo("msgId", receiptMsg.msgId)
                                        ?.sort("readTime", Sort.DESCENDING)
                                        ?.findFirst()?.readTime ?: 0

                                if (msgModel.isDeliver != 1) {
                                    msgModel.isDeliver = 1
                                    msgModel.deliverTime = lastMsgReceiptModelTime
                                    isChanged = true
                                }

                                if (msgModel.isRead != 1) {
                                    msgModel.isRead = 1
                                    msgModel.readTime = lastMsgReceiptModelTime
                                    isChanged = true
                                }

                            } else {
                                val deliverCount = realm.where(MessageReceiptModel::class.java)
                                    ?.equalTo("msgId", receiptMsg.msgId)
                                    ?.and()
                                    ?.greaterThan("deliverTime", 0)
                                    ?.count() ?: 0
                                if (deliverCount >= targetReceiptCount) {
                                    val lastMsgReceiptModelTime =
                                        realm.where(MessageReceiptModel::class.java)
                                            ?.equalTo("msgId", receiptMsg.msgId)
                                            ?.sort("deliverTime", Sort.DESCENDING)
                                            ?.findFirst()?.deliverTime ?: 0

                                    if (msgModel.isDeliver != 1) {
                                        msgModel.isDeliver = 1
                                        msgModel.deliverTime = lastMsgReceiptModelTime
                                        isChanged = true
                                    }
                                }
                            }
                        }

                        if (isChanged) {
                            realm.copyToRealmOrUpdate(msgModel)
                        }

                        findedReceiptMsg.add(receiptMsg)
                    } else {
                        if (System.currentTimeMillis() - receiptMsg.receiptStatus.time > Constant.Common.AUTO_CLEAR_MESSAGE_MAX_TIME) {
                            findedReceiptMsg.add(receiptMsg)
                        } else {
                            notFindedReceiptMsg.add(receiptMsg)
                        }
                    }
                }
            },
            {
                SendMessageManager.sendRecvGroupReceiptMessagePackage(findedReceiptMsg)
                groupMsgReceiptsCacheList.add(
                    IMPB.PushGroupMsgReceiptMessage.newBuilder()
                        .addAllReceiptMessage(notFindedReceiptMsg).build()
                )
            })
    }

    /**
     * 只有别人发送给自己的消息产生的回执，自己的web端才会生成并同步过来
     */
    private fun receiveMyselfWebSyncGroupMsgReceipts(
        myUid: Long,
        targetId: Long,
        receiptMsgs: List<IMPB.ReceiptMessage>
    ) {
        var changeReaded = false
        val findedReceiptMsg = mutableListOf<IMPB.ReceiptMessage>()
        val notFindedReceiptMsg = mutableListOf<IMPB.ReceiptMessage>()
        MessagesManager.executeChatTransactionAsync(
            ChatModel.CHAT_TYPE_GROUP,
            myUid,
            targetId,
            { realm ->
                receiptMsgs.forEach { receiptMsg ->
                    if (receiptMsg.receiptStatus.status.number != CommonProto.MsgReceiptStatus.DELIVERED.number) {
                        val time =
                            if (receiptMsg.receiptStatus.time == 0L) ArouterServiceManager.messageService.getCurrentTime() else receiptMsg.receiptStatus.time
                        val msgModel = realm.where(MessageModel::class.java)
                            ?.equalTo("msgId", receiptMsg.msgId)?.findFirst()
                        if (msgModel != null) {
                            // 修改消息表
                            var isChanged = false
                            if (receiptMsg.receiptStatus.status.number == CommonProto.MsgReceiptStatus.VIEWED.number) {
                                if (msgModel.isRead != 1) {
                                    msgModel.isRead = 1
                                    msgModel.readTime = time

                                    isChanged = true
                                    changeReaded = true
                                }
                            } else if (receiptMsg.receiptStatus.status.number == CommonProto.MsgReceiptStatus.PLAYED.number) {
                                if (msgModel.isRead != 1) {
                                    msgModel.isRead = 1
                                    msgModel.readTime = time

                                    isChanged = true
                                    changeReaded = true
                                }

                                if (msgModel.isReadedAttachment != 1) {
                                    msgModel.isReadedAttachment = 1
                                    msgModel.readedAttachmentTime = time

                                    val expireTime = msgModel.expireTime
                                    EventBus.publishEvent(
                                        ReadAttachmentEvent(
                                            targetId,
                                            receiptMsg.msgId,
                                            expireTime
                                        )
                                    )

                                    isChanged = true
                                }
                            }

                            if (isChanged) {
                                realm.copyToRealmOrUpdate(msgModel)
                            }

                            findedReceiptMsg.add(receiptMsg)
                        } else {
                            if (System.currentTimeMillis() - receiptMsg.receiptStatus.time > Constant.Common.AUTO_CLEAR_MESSAGE_MAX_TIME) {
                                findedReceiptMsg.add(receiptMsg)
                            } else {
                                notFindedReceiptMsg.add(receiptMsg)
                            }
                        }
                    } else {
                        findedReceiptMsg.add(receiptMsg)
                    }
                }
            },
            {
                SendMessageManager.sendRecvGroupReceiptMessagePackage(findedReceiptMsg)
                groupMsgReceiptsCacheList.add(
                    IMPB.PushGroupMsgReceiptMessage.newBuilder()
                        .addAllReceiptMessage(notFindedReceiptMsg).build()
                )

                // 收到web端同步给app端的回执确认
                if (changeReaded) {
                    var unReadCount = 0L
                    RealmCreator.executePvtChatTransactionAsync(myUid, targetId, { realm ->
                        unReadCount =
                            realm.where(MessageModel::class.java).equalTo("isSend", 0.toInt()).and()
                                .equalTo("isRead", 0.toInt()).count()
                    }, {
                        if (unReadCount == 0L) {
                            MessagesManager.updateMessagesAtMeStatus(myUid, targetId, {
                                ChatsHistoryManager.clearChatAtMeCount(
                                    myUid,
                                    ChatModel.CHAT_TYPE_GROUP,
                                    targetId,
                                    {
                                        EventBus.publishEvent(ChatHistoryChangeEvent())
                                    })
                            })
                        }

                        //  屏蔽，这里没有msgmodel 会导致 chat的时间错乱
//                    ChatsHistoryManager.checkChatHistoryIsCreated(ChatModel.CHAT_TYPE_GROUP, myUid, targetId, unReadCount = unReadCount.toInt(), forcedCreate = false) {
//                        EventBus.publishEvent(UnreadMessageEvent(ChatModel.CHAT_TYPE_GROUP, targetId))
//                    }
                    })
                }
            })
    }

    /**
     * 收到用户上下线消息
     */
    private fun receiveUserOnOffline(msg: IMPB.PushUserOnOrOffLineMessage) {
        if (msg.usersList.isNotEmpty()) {
            ArouterServiceManager.contactService.setContactLastOnline(null, msg.usersList)

            msg.usersList.forEach {
                EventBus.publishEvent(OnlineStatusChangeEvent(it.uid, it.online))
            }
        }
    }

    /**
     * 收到错误消息
     */
    private fun receivePvtErrorTipMsg(
        targetId: Long,
        flag: String,
        errorCode: Long,
        errorMsg: String
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        MessagesManager.executeChatTransactionAsync(CHAT_TYPE_PVT, myUid, targetId, { realm ->
            val findMsg = realm.where(MessageModel::class.java).equalTo("flag", flag).findFirst()
            if (findMsg != null) {
                findMsg.status = MessageModel.STATUS_SEND_FAIL
                realm.copyToRealmOrUpdate(findMsg)
            }

            val msgModel = if (findMsg?.isRetry == 0) {
                when (errorCode) {
                    5113L -> {
                        MessageModel.parseErrorTipMessage(
                            BaseApp.app.getString(R.string.please_add_each_other_as_friends_first),
                            5113L,
                            ArouterServiceManager.messageService.getCurrentTime(),
                            targetId,
                            targetId,
                            myUid,
                            CHAT_TYPE_PVT
                        )
                    }
                    5114L -> {
                        MessageModel.parseErrorTipMessage(
                            BaseApp.app.getString(R.string.message_has_send_but_they_refused),
                            5114L,
                            ArouterServiceManager.messageService.getCurrentTime(),
                            targetId,
                            targetId,
                            myUid,
                            CHAT_TYPE_PVT
                        )
                    }
                    else -> {
                        if (!TextUtils.isEmpty(errorMsg)) {
                            MessageModel.parseErrorTipMessage(
                                errorMsg,
                                errorCode,
                                ArouterServiceManager.messageService.getCurrentTime(),
                                targetId,
                                targetId,
                                myUid,
                                CHAT_TYPE_PVT
                            )
                        } else {
                            null
                        }
                    }
                }
            } else {
                null
            }

            if (errorCode == 5113L) {
                // 无法发送消息给对方，说明已经不是好友关系，发送好友关系变更的事件
                ArouterServiceManager.contactService.updataFriendShip(targetId, true, {
                    EventBus.publishEvent(FriendRelationChangeEvent(targetId, true))
                }, {})
            }

            msgModel?.let {
                realm.copyToRealm(msgModel)
            }
        })
    }

    /**
     * 收到错误消息
     */
    private fun receiveGroupErrorTipMsg(
        targetId: Long,
        flag: String,
        errorCode: Long,
        errorMsg: String
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        MessagesManager.executeChatTransactionAsync(
            ChatModel.CHAT_TYPE_GROUP,
            myUid,
            targetId,
            { realm ->
                val findMsg =
                    realm.where(MessageModel::class.java).equalTo("flag", flag).findFirst()
                if (findMsg != null) {
                    findMsg.status = MessageModel.STATUS_SEND_FAIL
                    realm.copyToRealmOrUpdate(findMsg)
                }

                val msgModel = if (findMsg?.isRetry == 0) {
                    when (errorCode) {
                        1001L -> {
                            MessageModel.parseErrorTipMessage(
                                BaseApp.app.getString(R.string.you_are_no_group_member_cant_send_message),
                                1001L,
                                ArouterServiceManager.messageService.getCurrentTime(),
                                targetId,
                                targetId,
                                myUid,
                                CHAT_TYPE_GROUP
                            )
                        }
                        else -> {
                            if (!TextUtils.isEmpty(errorMsg)) {
                                MessageModel.parseErrorTipMessage(
                                    errorMsg,
                                    errorCode,
                                    ArouterServiceManager.messageService.getCurrentTime(),
                                    targetId,
                                    targetId,
                                    myUid,
                                    CHAT_TYPE_GROUP
                                )
                            } else {
                                null
                            }
                        }
                    }
                } else {
                    null
                }

                msgModel?.let {
                    realm.copyToRealm(msgModel)
                }
            })
    }

    /**
     * 收到添加好友数量的消息
     */
    private fun receiveJoinContactReqNum(resp: IMPB.PushFriendReqNum) {
        EventBus.publishEvent(JoinContactReqEvent(resp.friendReqNum.toInt()))
        EventBus.publishEvent(NotificationEvent(0, "", Constant.Push.PUSH_TYPE.FRIEND))
    }

    /**
     * 收到添加到联系人的操作消息
     */
    private fun receiveJoinContactOperate(resp: IMPB.PushFriendRecordMessage) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        if (!resp.friendRecordmsgList.isNullOrEmpty()) {
            resp.friendRecordmsgList.forEach {
                when (it.doType) {
                    FriendMessageProto.FriendDoType.AGREE_JOIN_FRIEND -> {
                        // 添加此用户的信息到通讯录中
                        ArouterServiceManager.contactService.updateContactInfo(null, it.sendUid)

                        // 插入一条添加好友的消息到消息列表中，并生成会话记录
                        ArouterServiceManager.messageService.insertSystemTipMsg(
                            CHAT_TYPE_PVT,
                            it.sendUid,
                            it.createTime,
                            BaseApp.app.getString(R.string.say_hello)
                        )
                        EventBus.publishEvent(FriendRelationChangeEvent(it.sendUid, false))
                    }
                    FriendMessageProto.FriendDoType.READ_RECEIPT -> {
                        ArouterServiceManager.contactService.updateContactInfo(
                            null,
                            it.sendUid,
                            it.bfReadReceipt
                        )
                    }
                    FriendMessageProto.FriendDoType.SCREENSHOT -> {
                        ArouterServiceManager.contactService.updateContactInfo(
                            null,
                            it.contactsDetail
                        )

                        // 插入一条开启阅后即焚的消息到消息列表中，并生成会话记录
                        val content = if (it.contactsDetail.bfScreenshot) {
                            // 插入一条开启阅后即焚的消息到消息列表中，并生成会话记录
                            if (it.sendUid == myUid) {
                                BaseApp.app.getString(R.string.you_turn_on_screenshot_notifications)
                            } else {
                                String.format(
                                    BaseApp.app.getString(R.string.screenshot_notification_enabled),
                                    getDisplayName(it.contactsDetail.userInfo)
                                )
                            }
                        } else {
                            if (it.sendUid == myUid) {
                                BaseApp.app.getString(R.string.you_turned_off_screenshot_notifications)
                            } else {
                                String.format(
                                    BaseApp.app.getString(R.string.turn_off_screenshot_notifications),
                                    getDisplayName(it.contactsDetail.userInfo)
                                )
                            }
                        }
                        EventBus.publishEvent(
                            ScreenShotStateEvent(
                                it.contactsDetail.bfScreenshot,
                                if (it.sendUid == myUid) it.receiveUid else it.sendUid
                            )
                        )
                        ArouterServiceManager.messageService.insertSystemTipMsg(
                            CHAT_TYPE_PVT,
                            it.contactsDetail.userInfo.uid,
                            it.createTime,
                            content
                        )
                    }
                    FriendMessageProto.FriendDoType.READ_CANCEL -> {
                        ArouterServiceManager.contactService.updateContactInfo(
                            null,
                            it.contactsDetail
                        )

                        // 插入一条开启阅后即焚的消息到消息列表中，并生成会话记录
                        val content = if (it.contactsDetail.bfReadCancel) {
                            // 插入一条开启阅后即焚的消息到消息列表中，并生成会话记录
                            if (it.sendUid == myUid) {
                                String.format(
                                    BaseApp.app.getString(R.string.you_set_the_message_to_read_mat),
                                    TimeUtils.timeFormatForDeadline(it.contactsDetail.msgCancelTime)
                                )
                            } else {
                                String.format(
                                    BaseApp.app.getString(R.string.message_read_is_set),
                                    getDisplayName(it.contactsDetail.userInfo),
                                    TimeUtils.timeFormatForDeadline(it.contactsDetail.msgCancelTime)
                                )
                            }
                        } else {
                            EventBus.publishEvent(
                                ScreenShotStateEvent(
                                    false,
                                    if (it.sendUid == myUid) it.receiveUid else it.sendUid
                                )
                            )
                            if (it.sendUid == myUid) {
                                BaseApp.app.getString(R.string.you_shut_it_down)
                            } else {
                                String.format(
                                    BaseApp.app.getString(R.string.closed_after_reading_burn),
                                    getDisplayName(it.contactsDetail.userInfo)
                                )
                            }
                        }
                        ArouterServiceManager.messageService.insertSystemTipMsg(
                            CHAT_TYPE_PVT,
                            it.contactsDetail.userInfo.uid,
                            it.createTime,
                            content,
                            {
                                EventBus.publishEvent(
                                    FireStatusChangeEvent(
                                        mutableListOf(
                                            FireStatus(
                                                it.contactsDetail.userInfo.uid,
                                                it.contactsDetail.bfReadCancel
                                            )
                                        )
                                    )
                                )
                            })
                    }
                }

                SendMessageManager.sendRecvFriendRecordMessagesPackage(
                    it.sendUid,
                    it.receiveUid,
                    it.doTypeValue
                )
            }
        }
    }

    /**
     * 收到群通知请求数量
     */
    private fun receiveGroupOperateReqNum(resp: IMPB.PushGroupReqNum) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val msg = resp.groupReqMsg.msg
        if (!TextUtils.isEmpty(msg)) {
            val sp = SharePreferencesStorage.createStorageInstance(
                CommonPref::class.java,
                "group_new_number_${myUid}"
            )
            val time = sp.getHasNewGroupNumberTime()
            if (time < resp.groupReqMsg.updateTime) {
                sp.putHasNewGroupNumberTime(resp.groupReqMsg.updateTime)
                //插入数据库
                ChatsHistoryManager.selectChatHistoryHasGroupNotify(
                    myUid,
                    msg,
                    resp.groupReqMsg.updateTime,
                    resp.groupReqMsg.unReadNum.toInt(),
                    {
                        EventBus.publishEvent(UnreadMessageEvent())
                        EventBus.publishEvent(
                            NotificationEvent(
                                0,
                                msg,
                                Constant.Push.PUSH_TYPE.INVITE_GROUP
                            )
                        )
                    })
            }
        }
    }

    @Deprecated("v2.0.0 开始 文字拼接将会交给后台控制，这里将配遗弃，后期再把代码删除")
    private fun converGroupOperateReqNumMsg(
        myUid: Long,
        groupReqMsg: GroupMessageProto.GroupReqMsgDto
    ): String {
        return when (groupReqMsg.groupReqType.number) {
            CommonProto.GroupReqType.GROUP_TRANSFER.number -> {
                if (groupReqMsg.groupReqStatus.number == CommonProto.GroupReqStatus.AGREE.number) {
                    // 转让信息
                    if (groupReqMsg.fromUser?.uid == myUid) {
                        //我把群转让给了别人
                        String.format(
                            BaseApp.app.getString(R.string.group_manager_changed_to_mat),
                            getDisplayName(groupReqMsg.targetUser)
                        )
                    } else {
                        if (groupReqMsg.targetUser?.uid == myUid) {
                            //别人转让给了我
                            BaseApp.app.getString(R.string.you_ve_become_group_manager)
                        } else {
                            //别人转让给了别人
                            String.format(
                                BaseApp.app.getString(R.string.group_manager_changed_to_mat),
                                getDisplayName(groupReqMsg.targetUser)
                            )
                        }
                    }
                } else {
                    ""
                }
            }
            CommonProto.GroupReqType.GROUP_SET_ADMIN.number -> {
                // 设置管理员信息
                if (groupReqMsg.targetUser?.uid == myUid) {
                    String.format(
                        BaseApp.app.getString(R.string.set_you_as_administrator),
                        getDisplayName(groupReqMsg.fromUser)
                    )
                } else {
                    ""
                }
            }
            CommonProto.GroupReqType.GROUP_CANCLE_ADMIN.number -> {
                // 取消管理员信息
                if (groupReqMsg.targetUser?.uid == myUid) {
                    BaseApp.app.getString(R.string.your_administrator_status_has_been_removed)
                } else {
                    ""
                }
            }
            CommonProto.GroupReqType.GROUP_QR_CODE.number -> {
                when (groupReqMsg.groupReqStatus.number) {
                    CommonProto.GroupReqStatus.CHECKING.number -> {
                        String.format(
                            BaseApp.app.getString(R.string.apply_to_join_mat2),
                            getDisplayName(groupReqMsg.targetUser),
                            groupReqMsg.groupName
                        )
                    }
                    else -> {
                        ""
                    }
                }
            }
            CommonProto.GroupReqType.GROUP_OWNER_CHECK_QR_CODE.number -> {
                when (groupReqMsg.groupReqStatus.number) {
                    CommonProto.GroupReqStatus.REFUSE.number -> {
                        String.format(
                            BaseApp.app.getString(R.string.rejected_your_application_to_join_the_group),
                            getDisplayName(groupReqMsg.checkUser)
                        )
                    }
                    else -> {
                        ""
                    }
                }
            }
            CommonProto.GroupReqType.GROUP_INVITE.number -> {
                when (groupReqMsg.groupReqStatus.number) {
                    CommonProto.GroupReqStatus.CHECKING.number -> {
                        if (groupReqMsg.targetUser.uid == myUid) {
                            String.format(
                                BaseApp.app.getString(R.string.invite_you_to_join_a_group_chat),
                                getDisplayName(groupReqMsg.fromUser)
                            )
                        } else {
                            String.format(
                                BaseApp.app.getString(R.string.join_the_group_chat_mat),
                                getDisplayName(groupReqMsg.fromUser),
                                getDisplayName(groupReqMsg.targetUser)
                            )
                        }
                    }
                    else -> {
                        ""
                    }
                }
            }
            CommonProto.GroupReqType.GROUP_MEMBER_CHECK.number -> {
                when (groupReqMsg.groupReqStatus.number) {
                    CommonProto.GroupReqStatus.REFUSE.number -> {
                        String.format(
                            BaseApp.app.getString(R.string.refused_to_join_mat2),
                            getDisplayName(groupReqMsg.targetUser),
                            groupReqMsg.groupName
                        )
                    }
                    else -> {
                        ""
                    }
                }
            }
            CommonProto.GroupReqType.GROUP_OWNER_CHECK_INVITE.number -> {
                when (groupReqMsg.groupReqStatus.number) {
                    CommonProto.GroupReqStatus.REFUSE.number -> {
                        String.format(
                            BaseApp.app.getString(R.string.rejected_your_application_to_join_the_group),
                            getDisplayName(groupReqMsg.checkUser)
                        )
                    }
                    else -> {
                        ""
                    }
                }
            }
            CommonProto.GroupReqType.GROUP_IS_BANNED.number,
            CommonProto.GroupReqType.GROUP_IS_DISABLE.number,
            CommonProto.GroupReqType.GROUP_LINK.number,
            CommonProto.GroupReqType.GROUP_MEMBER_SHUTUP.number -> {
                groupReqMsg.msg
            }
            else -> {
                ""
            }
        }
    }

    private fun receiveGroupOperateReq(resp: IMPB.PushGroupReqMessage) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        if (!resp.groupReqMsgList.isNullOrEmpty()) {
            val reqIds = ArrayList<String>()
            resp.groupReqMsgList.forEach { groupReqMsg ->
                reqIds.add(groupReqMsg.rId.toString())

                val msg = groupReqMsg.msg
                if (!TextUtils.isEmpty(msg)) {
                    //插入数据库
                    val updateTime =
                        if (groupReqMsg.updateTime > 0) groupReqMsg.updateTime else ArouterServiceManager.messageService.getCurrentTime()
                    MessageController.executeChatTransactionAsyncWithResult(
                        ChatModel.CHAT_TYPE_GROUP,
                        myUid,
                        groupReqMsg.groupId,
                        { realm ->
                            val msgModel = MessageModel.parseGroupTipMessage(
                                msg,
                                updateTime,
                                groupReqMsg.groupId,
                                groupReqMsg.groupId,
                                myUid,
                                CHAT_TYPE_GROUP
                            )
                            realm.copyToRealm(msgModel)
                            msgModel.copyMessage()
                        },
                        { copyMsgModel ->
                            // 更新会话
                            if (copyMsgModel != null) {
                                ChatsHistoryManager.checkChatHistoryIsCreated(
                                    ChatModel.CHAT_TYPE_GROUP,
                                    myUid,
                                    groupReqMsg.groupId,
                                    copyMsgModel
                                )
                            }
                        })
                }

                val targetUid = groupReqMsg.targetUser?.uid ?: 0
                val fromUid = groupReqMsg.fromUser?.uid ?: 0L
                when (groupReqMsg.handleType.number) {
                    CommonProto.GroupHandleType.GROUP_REQ.number -> {
                        when (groupReqMsg.groupReqType.number) {
                            CommonProto.GroupReqType.GROUP_TRANSFER.number -> {
                                ArouterServiceManager.groupService.transferGroupOwner(
                                    groupReqMsg.groupId,
                                    fromUid,
                                    targetUid,
                                    {
                                        EventBus.publishEvent(GroupMemberChangeEvent(groupReqMsg.groupId))
                                        EventBus.publishEvent(
                                            GroupShutupEvent(
                                                groupReqMsg.groupId,
                                                groupReqMsg.groupShutup
                                            )
                                        )
                                    })
                            }
                            CommonProto.GroupReqType.GROUP_ADMIN_UPDATE.number -> {
                                ArouterServiceManager.groupService.updateGroupMemberInfoFromSocket(
                                    groupReqMsg.groupId,
                                    groupReqMsg.groupMember,
                                    {
                                        EventBus.publishEvent(GroupMemberChangeEvent(groupReqMsg.groupId))
                                    })
                            }
                            CommonProto.GroupReqType.GROUP_SET_ADMIN.number -> {
                                ArouterServiceManager.groupService.updateGroupMemberInfoFromSocket(
                                    groupReqMsg.groupId,
                                    groupReqMsg.groupMember,
                                    {
                                        EventBus.publishEvent(GroupMemberChangeEvent(groupReqMsg.groupId))
                                        EventBus.publishEvent(
                                            GroupShutupEvent(
                                                groupReqMsg.groupId,
                                                it
                                            )
                                        )
                                    })
                            }
                            CommonProto.GroupReqType.GROUP_CANCLE_ADMIN.number -> {
                                ArouterServiceManager.groupService.updateGroupMemberPermission(
                                    groupReqMsg.groupId,
                                    groupReqMsg.targetUser.uid,
                                    2,
                                    {
                                        EventBus.publishEvent(
                                            GroupShutupEvent(
                                                groupReqMsg.groupId,
                                                true
                                            )
                                        )
                                    })
                            }
                            CommonProto.GroupReqType.GROUP_QR_CODE.number,
                            CommonProto.GroupReqType.GROUP_OWNER_CHECK_QR_CODE.number,
                            CommonProto.GroupReqType.GROUP_INVITE.number,
                            CommonProto.GroupReqType.GROUP_MEMBER_CHECK.number,
                            CommonProto.GroupReqType.GROUP_LINK.number,
                            CommonProto.GroupReqType.GROUP_OWNER_CHECK_INVITE.number -> {
                                if (groupReqMsg.groupReqStatus.number == CommonProto.GroupReqStatus.AGREE.number) {
                                    //有成员入群
                                    if (targetUid == myUid) {
                                        ArouterServiceManager.groupService.syncGroupAllMemberInfoNew(
                                            null,
                                            1,
                                            200,
                                            groupReqMsg.groupId,
                                            1,
                                            { _, _, _ ->
                                                EventBus.publishEvent(
                                                    GroupMemberChangeEvent(
                                                        groupReqMsg.groupId
                                                    )
                                                )
                                            })
                                    } else {
                                        ArouterServiceManager.groupService.updateGroupMemberInfoFromSocket(
                                            groupReqMsg.groupId,
                                            groupReqMsg.groupMember,
                                            {
                                                EventBus.publishEvent(
                                                    GroupMemberChangeEvent(
                                                        groupReqMsg.groupId
                                                    )
                                                )
                                            })
                                    }
                                }
                            }
                            CommonProto.GroupReqType.GROUP_OWNER_REMOVE_MEMBER.number -> {
                                ArouterServiceManager.groupService.deleteGroupMemberInfo(
                                    groupReqMsg.groupId,
                                    targetUid,
                                    {
                                        EventBus.publishEvent(GroupMemberChangeEvent(groupReqMsg.groupId))
                                    })
                            }
                            CommonProto.GroupReqType.GROUP_MEMBER_EXIT.number -> {
                                ArouterServiceManager.groupService.deleteGroupMemberInfo(
                                    groupReqMsg.groupId,
                                    targetUid,
                                    {
                                        EventBus.publishEvent(GroupMemberChangeEvent(groupReqMsg.groupId))
                                    })
                            }
                            CommonProto.GroupReqType.GROUP_IS_BANNED.number -> {
                                EventBus.publishEvent(BanGroupMessageEvent(groupReqMsg.groupId))
                            }
                            CommonProto.GroupReqType.GROUP_MEMBER_SHUTUP.number -> {
                            }
                            CommonProto.GroupReqType.GROUP_IS_DISABLE.number -> {
                                ArouterServiceManager.groupService.deleteGroup(
                                    null,
                                    groupReqMsg.groupId,
                                    {
                                        EventBus.publishEvent(DisableGroupMessageEvent(groupReqMsg.groupId))
                                    }) {}
                            }
                        }
                    }
                    CommonProto.GroupHandleType.GROUP_NAME.number,
                    CommonProto.GroupHandleType.GROUP_PIC.number -> {
                        ArouterServiceManager.groupService.updateGroupInfo(
                            null,
                            groupReqMsg.groupId,
                            null,
                            {
                                EventBus.publishEvent(GroupInfoChangeEvent(groupReqMsg.groupId))
                            })
                    }
                    CommonProto.GroupHandleType.GROUP_MEMBER_NICKNAME.number -> {
                        ArouterServiceManager.groupService.updateGroupMemberInfoFromSocket(
                            groupReqMsg.groupId,
                            groupReqMsg.groupMember,
                            {
                                EventBus.publishEvent(GroupMemberChangeEvent(groupReqMsg.groupId))
                            })
                    }
                    CommonProto.GroupHandleType.GROUP_SHUTUP.number -> {
                        ArouterServiceManager.groupService.updateGroupInfo(
                            null,
                            groupReqMsg.groupId,
                            null,
                            {
                                EventBus.publishEvent(
                                    GroupShutupEvent(
                                        groupReqMsg.groupId,
                                        groupReqMsg.groupShutup
                                    )
                                )
                            })
                    }
                    CommonProto.GroupHandleType.GROUP_READ_CANCEL.number -> {
                        ArouterServiceManager.groupService.updateGroupInfo(
                            null,
                            groupReqMsg.groupId,
                            null,
                            null
                        )
                        // 插入一条开启阅后即焚的消息到消息列表中，并生成会话记录
                        val content = if (groupReqMsg.bfGroupReadCancel) {
                            // 插入一条开启阅后即焚的消息到消息列表中，并生成会话记录
                            if (groupReqMsg.fromUser.uid == myUid) {
                                String.format(
                                    BaseApp.app.getString(R.string.you_set_the_message_to_read_mat),
                                    TimeUtils.timeFormatForDeadline(groupReqMsg.groupMsgCancelTime)
                                )
                            } else {
                                String.format(
                                    BaseApp.app.getString(R.string.message_read_is_set),
                                    getDisplayName(groupReqMsg.fromUser),
                                    TimeUtils.timeFormatForDeadline(groupReqMsg.groupMsgCancelTime)
                                )
                            }
                        } else {
                            if (groupReqMsg.fromUser.uid == myUid) {
                                BaseApp.app.getString(R.string.you_shut_it_down)
                            } else {
                                String.format(
                                    BaseApp.app.getString(R.string.closed_after_reading_burn),
                                    getDisplayName(groupReqMsg.fromUser)
                                )
                            }
                        }
                        EventBus.publishEvent(GroupInfoChangeEvent(groupReqMsg.groupId))
                        EventBus.publishEvent(
                            FireStatusChangeEvent(
                                mutableListOf(
                                    FireStatus(
                                        groupReqMsg.groupId,
                                        groupReqMsg.bfGroupReadCancel
                                    )
                                )
                            )
                        )
                    }
                    else -> {

                    }
                }
            }

            SendMessageManager.sendRecvGroupOperateMessagesPackage(reqIds)
        }
    }

    @Deprecated("v2.0.0 开始 文字拼接将会交给后台控制，这里将配遗弃，后期再把代码删除")
    private fun converGroupOperateReqMsg(
        myUid: Long,
        groupReqMsg: GroupMessageProto.GroupReqMsgDto
    ): String {
        val targetUid = groupReqMsg.targetUser?.uid ?: 0
        val fromUid = groupReqMsg.fromUser?.uid ?: 0L
        return when (groupReqMsg.handleType.number) {
            CommonProto.GroupHandleType.GROUP_REQ.number -> {
                when (groupReqMsg.groupReqType.number) {
                    CommonProto.GroupReqType.GROUP_TRANSFER.number -> {
                        // 转让信息
                        if (fromUid == myUid) {
                            //我把群转让给了别人
                            String.format(
                                BaseApp.app.getString(R.string.group_manager_changed_to_mat),
                                getDisplayName(groupReqMsg.targetUser)
                            )
                        } else {
                            if (targetUid == myUid) {
                                //别人转让给了我
                                BaseApp.app.getString(R.string.you_ve_become_group_manager)
                            } else {
                                //别人转让给了别人
                                String.format(
                                    BaseApp.app.getString(R.string.group_manager_changed_to_mat),
                                    getDisplayName(groupReqMsg.targetUser)
                                )
                            }
                        }
                    }
                    CommonProto.GroupReqType.GROUP_SET_ADMIN.number -> {
                        // 设置管理员信息
                        if (targetUid == myUid) {
                            BaseApp.app.getString(R.string.you_have_become_the_administrator_of_this_group)
                        } else {
                            ""
                        }
                    }
                    CommonProto.GroupReqType.GROUP_CANCLE_ADMIN.number -> {
                        // 取消管理员信息
                        if (targetUid == myUid) {
                            BaseApp.app.getString(R.string.your_administrator_status_has_been_removed)
                        } else {
                            ""
                        }
                    }
                    CommonProto.GroupReqType.GROUP_QR_CODE.number,
                    CommonProto.GroupReqType.GROUP_OWNER_CHECK_QR_CODE.number -> {
                        if (targetUid == myUid) {
                            BaseApp.app.getString(R.string.you_join_a_group_chat_by_scanning_the_qr_code)
                        } else {
                            String.format(
                                BaseApp.app.getString(R.string.join_a_group_chat_by_scanning_the_qr_code),
                                getDisplayName(groupReqMsg.targetUser)
                            )
                        }
                    }
                    CommonProto.GroupReqType.GROUP_INVITE.number,
                    CommonProto.GroupReqType.GROUP_MEMBER_CHECK.number,
                    CommonProto.GroupReqType.GROUP_OWNER_CHECK_INVITE.number -> {
                        when (myUid) {
                            targetUid -> String.format(
                                BaseApp.app.getString(R.string.invite_you_to_join_a_group_chat),
                                getDisplayName(groupReqMsg.fromUser)
                            )
                            fromUid -> String.format(
                                BaseApp.app.getString(R.string.you_are_invited),
                                getDisplayName(groupReqMsg.targetUser)
                            )
                            else -> String.format(
                                BaseApp.app.getString(R.string.join_the_group_chat_mat),
                                getDisplayName(groupReqMsg.fromUser),
                                getDisplayName(groupReqMsg.targetUser)
                            )
                        }
                    }
                    CommonProto.GroupReqType.GROUP_OWNER_REMOVE_MEMBER.number -> {
                        when (myUid) {
                            targetUid -> String.format(
                                BaseApp.app.getString(R.string.removed_you_from_the_group_chat),
                                getDisplayName(groupReqMsg.fromUser)
                            )
                            fromUid -> String.format(
                                BaseApp.app.getString(R.string.you_move_out_of_group_chat),
                                getDisplayName(groupReqMsg.targetUser)
                            )
                            else -> ""//其他成员被踢，不需要提示
                        }
                    }
                    CommonProto.GroupReqType.GROUP_MEMBER_EXIT.number -> {
                        if (targetUid == myUid) {
                            ""//自己退出了群聊，不响应
                        } else {
                            String.format(
                                BaseApp.app.getString(R.string.quit_the_group_chat),
                                getDisplayName(groupReqMsg.targetUser)
                            )
                        }
                    }
                    CommonProto.GroupReqType.GROUP_MEMBER_SHUTUP.number -> {
                        groupReqMsg.msg
                    }
                    CommonProto.GroupReqType.GROUP_IS_DISABLE.number -> {
                        groupReqMsg.msg
                    }
                    CommonProto.GroupReqType.GROUP_LINK.number -> {
                        if (groupReqMsg.groupReqStatus == CommonProto.GroupReqStatus.AGREE) {
                            groupReqMsg.msg
                        } else {
                            ""
                        }
                    }
                    else -> {
                        ""
                    }
                }
            }
            CommonProto.GroupHandleType.GROUP_NAME.number -> {
                if (groupReqMsg.fromUser?.uid == myUid) {
                    String.format(
                        BaseApp.app.getString(R.string.you_change_the_group_name),
                        groupReqMsg.groupName
                    )
                } else {
                    String.format(
                        BaseApp.app.getString(R.string.modify_group_name),
                        getDisplayName(groupReqMsg.fromUser),
                        groupReqMsg.groupName
                    )
                }
            }
            CommonProto.GroupHandleType.GROUP_PIC.number -> ""
            CommonProto.GroupHandleType.GROUP_MEMBER_NICKNAME.number -> ""
            CommonProto.GroupHandleType.GROUP_SHUTUP.number -> {
                if (groupReqMsg.fromUser?.uid == myUid) {
                    String.format(
                        BaseApp.app.getString(R.string.you_the_entire_staff_was_silenced),
                        (if (groupReqMsg.groupShutup) BaseApp.app.getString(R.string.open) else BaseApp.app.getString(
                            R.string.close
                        ))
                    )
                } else {
                    String.format(
                        BaseApp.app.getString(R.string.admin_the_entire_staff_was_silenced),
                        (if (groupReqMsg.groupShutup) BaseApp.app.getString(R.string.open) else BaseApp.app.getString(
                            R.string.close
                        ))
                    )
                }
            }
            else -> ""
        }
    }

    private fun getDisplayName(user: CommonProto.UserBase): String {
        return if (TextUtils.isEmpty(user.friendRelation.remarkName)) user.nickName else user.friendRelation.remarkName
    }

    /**
     * 收到撤回私聊的消息
     */
    private fun recallPvtMessage(resp: IMPB.PushRecallOneToOneMessage) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val recallMaps = HashMap<Long, ArrayList<IMPB.RecallMessage>>()

        resp.recallOneToOneMessagesList?.forEach { recallMessage ->
            if (recallMaps[recallMessage.msgTargetId] == null) {
                recallMaps[recallMessage.msgTargetId] = ArrayList()
            }
            recallMaps[recallMessage.msgTargetId]?.add(recallMessage)
        }

        recallMaps.forEach {
            MessageController.receiveRecallMessage(ChatModel.CHAT_TYPE_PVT, myUid, it.key, it.value)
        }
    }

    /**
     * 收到撤回群聊的消息
     */
    private fun recallGroupMessage(resp: IMPB.PushRecallGroupMessage) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val recallMaps = HashMap<Long, ArrayList<IMPB.RecallMessage>>()

        resp.recallGroupMessagesList?.forEach { recallMessage ->
            if (recallMaps[recallMessage.msgTargetId] == null) {
                recallMaps[recallMessage.msgTargetId] = ArrayList()
            }
            recallMaps[recallMessage.msgTargetId]?.add(recallMessage)
        }

        recallMaps.forEach {
            MessageController.receiveRecallMessage(
                ChatModel.CHAT_TYPE_GROUP,
                myUid,
                it.key,
                it.value
            )
        }
    }

    /**
     * 收到流媒体通话的新token
     */
    @SuppressLint("CheckResult")
    private fun receiveStreamCallNewToken(req: IMPB.PushStreamNewToken) {
        if (!RtcEngineHolder.isActive()) {
            //通话已关闭，不响应
            return
        }

        ThreadUtils.runOnUIThread {
            if (req.channelName == RtcEngineHolder.currentChannelName) {
                RtcEngineHolder.renewToken(req.token)
            }
        }
    }

    /**
     * 收到建立流媒体通信信道的请求(双方同意建立对话)
     */
    @SuppressLint("CheckResult")
    private fun receiveStreamCallBuildReq(req: IMPB.PushOneToOneStreamSuccess) {
        val streamType = when (req.streamType) {
            IMPB.StreamType.streamAudio -> {
                0
            }
            IMPB.StreamType.streamVideo -> {
                1
            }
            else -> {
                -1
            }
        }

        if (streamType < 0) {
            return
        }

        //更新流媒体通话记录的状态
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val targetUid = if (myUid == req.sendUid) {
            req.receiveUid
        } else {
            req.sendUid
        }

        if (RtcEngineHolder.isActive() && RtcEngineHolder.mineUid == myUid && RtcEngineHolder.targetUid == targetUid) {
            ThreadUtils.runOnUIThread {
                if (req.streamType == IMPB.StreamType.streamAudio) {
                    RtcEngineHolder.joinAudioStream(req.token, req.channelName, req.secretKey)
                } else if (req.streamType == IMPB.StreamType.streamVideo) {
                    RtcEngineHolder.joinVideoStream(req.token, req.channelName, req.secretKey)
                }
            }
        }
    }

    /**
     * 服务器响应流媒体通话请求（下发ChannelName）
     */
    @SuppressLint("CheckResult")
    private fun receiveStreamCallResp(resp: IMPB.PushSendOneToOneStreamReqSuccess) {
        val streamType = when (resp.streamType) {
            IMPB.StreamType.streamAudio -> {
                0
            }
            IMPB.StreamType.streamVideo -> {
                1
            }
            else -> {
                -1
            }
        }

        if (streamType < 0) {
            return
        }

        ThreadUtils.runOnUIThread {
            if (RtcEngineHolder.isActive()) {
                RtcEngineHolder.currentChannelName = resp.channelName
                StreamCallController.createStreamCallRecord(
                    resp.receiveUid,
                    resp.channelName,
                    streamType,
                    1
                )
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun receiveStreamCallReq(req: IMPB.PushOneToOneStreamOperateMessage) {
        val streamType = when (req.streamType) {
            IMPB.StreamType.streamAudio -> {
                0
            }
            IMPB.StreamType.streamVideo -> {
                1
            }
            else -> {
                -1
            }
        }

        if (streamType < 0) {
            return
        }

        ThreadUtils.runOnUIThread {
            val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
            val targetUid = if (myUid == req.sendUid) {
                req.revceiveUid
            } else {
                req.sendUid
            }

            //能正常通话，说明已经是好友关系，发送好友关系变更的事件
            EventBus.publishEvent(FriendRelationChangeEvent(targetUid, false))
            when (req.operate) {
                IMPB.StreamOperateType.confirm -> {
                    // 接收到通话请求
                    if (RtcEngineHolder.isActive()) {
                        // 正忙...
                        RLogManager.d(MessageSocketService.TAG, "收到流媒体通话请求,正忙...--->")
                        SendMessageManager.sendBusyNowStreamRequestPackage(
                            req.channelName,
                            RtcEngineHolder.openType,
                            myUid,
                            targetUid,
                            req.streamType
                        )
                    } else {
                        // 未在通话中...
                        RtcEngineHolder.currentChannelName = req.channelName
                        RtcEngineHolder.pendingCall = RtcEngineHolder.PendingCall(
                            req.sendUid,
                            streamType,
                            1,
                            System.currentTimeMillis()
                        )

                        RLogManager.d(MessageSocketService.TAG, "收到流媒体通话请求,弹出通话界面...--->")

                        var hasAppGestureUnlockActivity = false
                        var hasAppConfirmPinUnlockActivity = false
                        var hasAppFingerprintIdentifyActivity = false
                        when {
                            ActivitiesHelper.getInstance()
                                .hasActivity("framework.telegram.business.ui.other.AppGestureUnlockActivity") -> {
                                hasAppGestureUnlockActivity = true
                            }
                            ActivitiesHelper.getInstance()
                                .hasActivity("framework.telegram.business.ui.other.AppConfirmPinUnlockActivity") -> {
                                hasAppConfirmPinUnlockActivity = true
                            }
                            ActivitiesHelper.getInstance()
                                .hasActivity("framework.telegram.business.ui.other.AppFingerprintIdentifyActivity") -> {
                                hasAppFingerprintIdentifyActivity = true
                            }
                        }

                        if (hasAppGestureUnlockActivity || hasAppConfirmPinUnlockActivity || hasAppFingerprintIdentifyActivity) {
                            // 如果在锁定界面，则只播放声音
                            val privacy =
                                AccountManager.getLoginAccount(AccountInfo::class.java).getPrivacy()
                            if (ArouterServiceManager.settingService.getVoiceStatus(
                                    privacy,
                                    true
                                )
                            ) // 声音
                                SoundPoolManager.playStreamCalling()
                            if (ArouterServiceManager.settingService.getVibrationStatus(
                                    privacy,
                                    true
                                )
                            ) // 震动
                                SoundPoolManager.vibratorRepeat()
                        } else {
                            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO)
                                .withLong("targetUid", req.sendUid)
                                .withInt("streamType", streamType)
                                .withInt("openType", 1)
                                .navigation()
                        }

                        // oppo等机器无法在后台打开页面(或者停留在锁屏界面时)，弹出一个通知栏消息（如果打开了界面，则此通知会被清除或者无法出现）
                        ArouterServiceManager.contactService.getContactInfo(
                            null,
                            targetUid,
                            { contactInfoModel, _ ->
                                if (!contactInfoModel.isBfDisturb) {
                                    val name = contactInfoModel.copyContactDataModel().displayName
                                    EventBus.publishEvent(
                                        NotificationEvent(
                                            req.sendUid,
                                            name,
                                            if (streamType == IMPB.StreamType.streamAudio.number)
                                                Constant.Push.PUSH_TYPE.AUDIO_STREAM else Constant.Push.PUSH_TYPE.VIDEO_STREAM
                                        )
                                    )
                                }
                            })

                        // 查询是否建立过流媒体通话记录
                        var hasStreamCall = false
                        RealmCreator.executePvtChatTransactionAsync(myUid, targetUid, {
                            val msgModel =
                                it.where(MessageModel::class.java).equalTo("flag", req.channelName)
                                    .findFirst()
                            if (msgModel != null) {
                                hasStreamCall = true
                            }
                        }, {
                            if (!hasStreamCall) {
                                // 建立流媒体通话记录
                                StreamCallController.createStreamCallRecord(
                                    targetUid,
                                    req.channelName,
                                    streamType,
                                    0
                                )
                            } else {
                                // 已建立过不需要重复建立
                            }
                        })
                    }
                }
                IMPB.StreamOperateType.cancel -> {
                    // 接收到取消通话答复
                    RLogManager.d(MessageSocketService.TAG, "收到对方取消流媒体通话的答复--->")

                    RtcEngineHolder.pendingCall = null

                    // 如果在锁定界面，则只播放声音，所以对方取消通话后这里要关闭这个声音
                    SoundPoolManager.stopPlayStreamCalling()
                    SoundPoolManager.stopVibrator()

                    ArouterServiceManager.contactService.getContactInfo(
                        null,
                        targetUid,
                        { contactInfoModel, _ ->
                            val name = contactInfoModel.copyContactDataModel().displayName
                            if (streamType == IMPB.StreamType.streamAudio.number) {
                                EventBus.publishEvent(
                                    NotificationEvent(
                                        0,
                                        name,
                                        Constant.Push.PUSH_TYPE.CLEAR_AUDIO_NOTIFICATION
                                    )
                                )
                            } else {
                                EventBus.publishEvent(
                                    NotificationEvent(
                                        0,
                                        name,
                                        Constant.Push.PUSH_TYPE.CLEAR_VIDEO_NOTIFICATION
                                    )
                                )
                            }
                        })

                    if (RtcEngineHolder.isActive() && RtcEngineHolder.mineUid == myUid && RtcEngineHolder.targetUid == targetUid) {
                        StreamCallController.endCall(
                            BaseApp.app.getString(R.string.the_call_has_been_cancelled),
                            false
                        )
                    }

                    //查询是否建立过流媒体通话记录
                    var hasStreamCall = false
                    RealmCreator.executePvtChatTransactionAsync(myUid, targetUid, {
                        val msgModel =
                            it.where(MessageModel::class.java).equalTo("flag", req.channelName)
                                .findFirst()
                        if (msgModel != null) {
                            hasStreamCall = true
                        }
                    }, {
                        if (!hasStreamCall) {
                            // 建立流媒体通话记录
                            StreamCallController.createStreamCallRecord(
                                targetUid,
                                req.channelName,
                                streamType,
                                0,
                                req.sendTime,
                                3
                            )
                        } else {
                            // 更新流媒体通话记录状态
                            StreamCallController.updateCallStreamStatus(
                                myUid,
                                targetUid,
                                req.channelName,
                                3
                            )
                        }
                    })
                }
                IMPB.StreamOperateType.agree -> {
                    // 接收到对方同意通话请求
                    RLogManager.d(MessageSocketService.TAG, "收到对方同意流媒体通话的答复--->")
                }
                IMPB.StreamOperateType.refuse -> {
                    // 接收到对方拒绝通话答复
                    RLogManager.d(MessageSocketService.TAG, "收到流对方拒绝媒体通话的答复--->")
                    if (RtcEngineHolder.isActive() && RtcEngineHolder.mineUid == myUid && RtcEngineHolder.targetUid == targetUid) {
                        StreamCallController.endCall(
                            BaseApp.app.getString(R.string.your_call_has_been_denied),
                            false
                        )
                    }

                    //更新流媒体通话记录状态
                    StreamCallController.updateCallStreamStatus(
                        myUid,
                        targetUid,
                        req.channelName,
                        2
                    )
                }
                IMPB.StreamOperateType.calling -> {
                    // 接收到对方通话正忙答复
                    RLogManager.d(MessageSocketService.TAG, "收到对方流媒体通话正忙答复--->")
                    if (RtcEngineHolder.isActive() && RtcEngineHolder.mineUid == myUid && RtcEngineHolder.targetUid == targetUid) {
                        StreamCallController.endCall(
                            BaseApp.app.getString(R.string.the_line_is_engaged),
                            false
                        )
                    }

                    //更新流媒体通话记录状态
                    StreamCallController.updateCallStreamStatus(
                        myUid,
                        targetUid,
                        req.channelName,
                        4
                    )
                }
                else -> {
                }
            }
        }
    }

    private fun receiveSendedPvtMsgResp(list: List<IMPB.PushSendMessageSuccess>) {
        val msgsMap = HashMap<Long, ArrayList<IMPB.PushSendMessageSuccess>>()
        list.forEach {
            if (msgsMap[it.receiveUid] == null) {
                msgsMap[it.receiveUid] = ArrayList()
            }
            msgsMap[it.receiveUid]?.add(it)
        }

        msgsMap.forEach { (t, u) ->
            receiveSendedPvtMsgResp(t, u)
        }
    }

    @SuppressLint("CheckResult")
    private fun receiveSendedPvtMsgResp(targetUid: Long, list: List<IMPB.PushSendMessageSuccess>) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val msgs = mutableListOf<MessageModel>()
        MessagesManager.executeChatTransactionAsync(CHAT_TYPE_PVT, myUid, targetUid, { realm ->
            list.forEach { m ->
                val msgModel =
                    realm.where(MessageModel::class.java).equalTo("flag", m.flag.toString())
                        .findFirst()
                if (msgModel != null) {
                    msgModel.msgId = m.msgId
                    msgModel.status = MessageModel.STATUS_SENDED_HAS_RESP
                    msgs.add(msgModel.copyMessage())
                    realm.copyToRealmOrUpdate(msgModel)
                } else {
                    oneToOneMsgSendSuccessCacheList.add(m)
                }
            }
        }, {
            // 能发送消息给对方，说明已经是好友关系，发送好友关系变更的事件
            EventBus.publishEvent(FriendRelationChangeEvent(targetUid, false))
            EventBus.publishEvent(MessageStateChangeEvent(msgs))

            //保存数据到全文搜索数据库
            saveMessageToSearchDB(msgs)
        })
    }

    private fun receiveSendedGroupMsgResp(list: List<IMPB.PushGroupMessageSendSuccess>) {
        val msgsMap = HashMap<Long, ArrayList<IMPB.PushGroupMessageSendSuccess>>()
        list.forEach {
            if (msgsMap[it.groupId] == null) {
                msgsMap[it.groupId] = ArrayList()
            }
            msgsMap[it.groupId]?.add(it)
        }

        msgsMap.forEach { (t, u) ->
            receiveSendedGroupMsgResp(t, u)
        }
    }

    @SuppressLint("CheckResult")
    private fun receiveSendedGroupMsgResp(
        targetGid: Long,
        list: List<IMPB.PushGroupMessageSendSuccess>
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val msgs = mutableListOf<MessageModel>()
        MessagesManager.executeChatTransactionAsync(
            ChatModel.CHAT_TYPE_GROUP,
            myUid,
            targetGid,
            { realm ->
                list.forEach { m ->
                    val msgModel =
                        realm.where(MessageModel::class.java).equalTo("flag", m.flag.toString())
                            .findFirst()
                    if (msgModel != null) {
                        msgModel.msgId = m.msgId
                        msgModel.receiptCount = m.memberCount
                        if (msgModel.snapchatTime > 0 && msgModel.expireTime <= 0) {
                            msgModel.expireTime =
                                System.currentTimeMillis() + msgModel.snapchatTime * 1000
                        }
                        msgModel.status = MessageModel.STATUS_SENDED_HAS_RESP
                        msgs.add(msgModel.copyMessage())
                        realm.copyToRealmOrUpdate(msgModel)
                    } else {
                        groupMsgSendSuccessCacheList.add(m)
                    }
                }
            },
            {
                //保存数据到全文搜索数据库
                saveMessageToSearchDB(msgs)
            })
    }

    /**
     * 按照不同的用户，做一次分拣
     */
    private fun receiveMsgFromUser(msg: List<IMPB.OneToOneMessage>?) {
        //区分发送者
        val msgsMap = HashMap<Long, ArrayList<IMPB.OneToOneMessage>>()
        val myselfWebMsgsMap = HashMap<Long, ArrayList<IMPB.OneToOneMessage>>()
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        msg?.forEach {
            if (it.sendUid == myUid) {
                if (myselfWebMsgsMap[it.receiveUid] == null) {
                    myselfWebMsgsMap[it.receiveUid] = ArrayList()
                }
                myselfWebMsgsMap[it.receiveUid]?.add(it)
            } else {
                if (msgsMap[it.sendUid] == null) {
                    msgsMap[it.sendUid] = ArrayList()
                }
                msgsMap[it.sendUid]?.add(it)
            }
        }

        myselfWebMsgsMap.forEach {
            receiveMyselfWebMsgFromUser(myUid, it.key, it.value)
        }

        msgsMap.forEach {
            receiveMsgFromUser(myUid, it.key, it.value)
        }
    }

    private fun receiveMyselfWebMsgFromUser(
        myUid: Long,
        targetUid: Long,
        msgs: List<IMPB.OneToOneMessage>
    ) {
        val msgsMap = HashMap<Int, ArrayList<IMPB.OneToOneMessage>>()
        msgs.forEach { msg ->
            // 先判断是不是自己的web端发送的（自己发送的消息，需要录入成自己发送的标识）
            if (msgsMap[msg.version] == null) {
                msgsMap[msg.version] = ArrayList()
            }
            msgsMap[msg.version]?.add(msg)
        }

        msgsMap.forEach {
            if (it.key == 0) {
                receiveMyselfWebMsgFromUser(
                    myUid,
                    targetUid,
                    "",
                    0,
                    it.value
                )
            } else {
                ArouterServiceManager.systemService.getUserSecretKey(
                    myUid,
                    webVer = it.key,
                    complete = { _, _, webSecretKey, webKeyVersion ->
                        receiveMyselfWebMsgFromUser(
                            myUid,
                            targetUid,
                            webSecretKey,
                            webKeyVersion,
                            it.value
                        )
                    },
                    error = {
                        MobclickAgent.reportError(
                            BaseApp.app,
                            "ReceiveMessageManager--->receiveMsgFromUser.WEB.getUserSecretKey失败 ${it.localizedMessage}"
                        )
                    })
            }
        }
    }

    /**
     * 按照加密key的来源和版本，做一次分拣
     */
    private fun receiveMsgFromUser(myUid: Long, targetUid: Long, msgs: List<IMPB.OneToOneMessage>) {
        val msgsMap = HashMap<String, ArrayList<IMPB.OneToOneMessage>>()
        msgs.forEach { msg ->
            // 先判断是不是对方的web端发送的
            val msgSource = msg.source
            val msgKeyVer = msg.version

            val key = "${msgKeyVer}_${if (msgSource == IMPB.MessageSource.WEB) 1 else 0}"
            if (msgsMap[key] == null) {
                msgsMap[key] = ArrayList()
            }
            msgsMap[key]?.add(msg)
        }

        msgsMap.forEach {
            val keys = it.key.split("_")
            val msgSource =
                if (1 == keys[1].toInt()) IMPB.MessageSource.WEB else IMPB.MessageSource.APP
            val msgKeyVer = keys[0].toInt()
            if (msgKeyVer == 0) {
                receiveMsgFromUser(myUid, targetUid, "", 0, msgs)
            } else {
                if (msgSource.number == IMPB.MessageSource.WEB.number) {
                    ArouterServiceManager.systemService.getUserSecretKey(
                        targetUid,
                        webVer = msgKeyVer,
                        complete = { _, _, webSecretKey, webKeyVersion ->
                            receiveMsgFromUser(myUid, targetUid, webSecretKey, webKeyVersion, msgs)
                        },
                        error = {
                            MobclickAgent.reportError(
                                BaseApp.app,
                                "ReceiveMessageManager--->receiveMsgFromUser.WEB.getUserSecretKey失败 ${it.localizedMessage}"
                            )
                        })
                } else {
                    ArouterServiceManager.systemService.getUserSecretKey(
                        targetUid,
                        appVer = msgKeyVer,
                        complete = { secretKey, keyVersion, _, _ ->
                            receiveMsgFromUser(myUid, targetUid, secretKey, keyVersion, msgs)
                        },
                        error = {
                            MobclickAgent.reportError(
                                BaseApp.app,
                                "ReceiveMessageManager--->receiveMsgFromUser.APP.getUserSecretKey失败 ${it.localizedMessage}"
                            )
                        })
                }
            }
        }
    }

    private fun receiveMsgFromUser(
        myUid: Long,
        targetUid: Long,
        secretKey: String,
        localKeyVersion: Int,
        msgs: List<IMPB.OneToOneMessage>
    ) {
        val msgModels = ArrayList<MessageModel>()
        val msgReceipts = ArrayList<IMPB.ReceiptMessage>()
        val receiptStatus = CommonProto.MsgReceiptStatusBase.newBuilder()
            .setStatus(CommonProto.MsgReceiptStatus.DELIVERED)
            .setTime(ArouterServiceManager.messageService.getCurrentTime())
        var isUpdateKey = false
        val commonPref = SharePreferencesStorage.createStorageInstance(
            CommonPref::class.java,
            "user_${AccountManager.getLoginAccountUUid()}_${targetUid}"
        )
        val lastAutoDeleteFireMsgTime = commonPref.getLastAutoDeleteFireMsgTime()
        msgs.forEach { msg ->
            val receiptMsgBuilder = IMPB.ReceiptMessage.newBuilder().setMsgId(msg.msgId)
                .setType(IMPB.ChatMessageType.ONE_TO_ONE).setSendUid(msg.sendUid)
                .setReceiptStatus(receiptStatus)

            val msgSource = msg.source
            val msgKeyVersion: Int = msg.version
            val msgContentMd5: String = msg.contentMd5
            val msgAttachmentKey: String
            val msgContent: ByteString

            if (msgSource == IMPB.MessageSource.APP && (msg.appContent == null || msg.appContent.content == null || msg.appContent.content.isEmpty)) {
                // 兼容老版本的离线消息
                msgContent = msg.content
                msgAttachmentKey = msg.attachmentKey
            } else {
                msgContent = msg.appContent.content
                msgAttachmentKey = msg.appContent.attachmentKey
            }

            val msgModel = if (msgKeyVersion == 0 || msgKeyVersion == localKeyVersion) {
                // 版本号一致或未加密
                var data: ByteArray? = null
                var attachmentKey: String? = ""
                try {
                    data = if (msgKeyVersion == 0) {
                        msgContent.toByteArray()
                    } else {
                        AESHelper.decryptToBytes(
                            msgContent.toByteArray(),
                            secretKey
                        )
                    }
                    attachmentKey = if (TextUtils.isEmpty(msgAttachmentKey)) {
                        ""
                    } else {
                        if (msgKeyVersion == 0 || TextUtils.isEmpty(secretKey)) {
                            msgAttachmentKey
                        } else {
                            AESHelper.decrypt(
                                HexString.hexToBuffer(msgAttachmentKey), secretKey
                            )
                        }
                    }
                } catch (e: Exception) {
                    // 解密失败
                    e.printStackTrace()
                    MobclickAgent.reportError(BaseApp.app, e)
                    // 有消息解密失败，尝试更新他的key
                    isUpdateKey = true
                }

                if (data != null && (((TextUtils.isEmpty(msgContentMd5) || MD5.md5(data) == msgContentMd5) && !TextUtils.isEmpty(
                        secretKey
                    )) || msgKeyVersion == 0)
                ) {
                    try {
                        when (msg.msgType) {
                            IMPB.MessageType.text -> {
                                val content = IMPB.TextObj.parseFrom(data)
                                MessageModel.parseTextMessage(
                                    msg.msgId,
                                    content.content,
                                    msg.contentMd5,
                                    msg.sendTime,
                                    null,
                                    content.ref,
                                    msg.snapchatTime,
                                    targetUid,
                                    "",
                                    "",
                                    targetUid,
                                    myUid,
                                    CHAT_TYPE_PVT
                                )
                            }
                            IMPB.MessageType.image -> {
                                val content = IMPB.ImageObj.parseFrom(data)
                                MessageModel.parseImageMessage(
                                    msg.msgId,
                                    content.url,
                                    content.thumbUrl,
                                    content.width,
                                    content.height,
                                    msg.contentMd5,
                                    attachmentKey,
                                    msg.sendTime,
                                    content.ref,
                                    msg.snapchatTime,
                                    targetUid,
                                    "",
                                    "",
                                    targetUid,
                                    myUid,
                                    CHAT_TYPE_PVT
                                )
                            }
                            IMPB.MessageType.dynamicImage -> {
                                val content = IMPB.DynamicImageObj.parseFrom(data)
                                MessageModel.parseDynamicImageMessage(
                                    msg.msgId,
                                    content.emoticonId,
                                    content.url,
                                    content.thumbUrl,
                                    content.width,
                                    content.height,
                                    msg.contentMd5,
                                    attachmentKey,
                                    msg.sendTime,
                                    content.ref,
                                    msg.snapchatTime,
                                    targetUid,
                                    "",
                                    "",
                                    targetUid,
                                    myUid,
                                    content.fileSize,
                                    CHAT_TYPE_PVT
                                )
                            }
                            IMPB.MessageType.video -> {
                                val content = IMPB.VideoObj.parseFrom(data)
                                MessageModel.parseVideoMessage(
                                    msg.msgId,
                                    content.url,
                                    content.thumbUrl,
                                    content.width,
                                    content.height,
                                    content.duration,
                                    msg.contentMd5,
                                    attachmentKey,
                                    msg.sendTime,
                                    content.ref,
                                    msg.snapchatTime,
                                    targetUid,
                                    "",
                                    "",
                                    targetUid,
                                    myUid,
                                    CHAT_TYPE_PVT
                                )
                            }
                            IMPB.MessageType.audio -> {
                                val content = IMPB.AudioObj.parseFrom(data)
                                val aHighByteArr = content.waveData.toByteArray()
                                val aHighIntArr = IntArray(aHighByteArr.size)
                                aHighByteArr.forEachIndexed { index, i ->
                                    aHighIntArr[index] = i.toInt()
                                }
                                MessageModel.parseVoiceMessage(
                                    msg.msgId,
                                    content.duration,
                                    content.url,
                                    aHighIntArr,
                                    msg.contentMd5,
                                    attachmentKey,
                                    msg.sendTime,
                                    content.ref,
                                    msg.snapchatTime,
                                    targetUid,
                                    "",
                                    "",
                                    targetUid,
                                    myUid,
                                    CHAT_TYPE_PVT
                                )
                            }
                            IMPB.MessageType.nameCard -> {
                                val content = IMPB.NameCardObj.parseFrom(data)
                                MessageModel.parseNameCardMessage(
                                    msg.msgId,
                                    content.uid,
                                    content.nickName,
                                    content.icon,
                                    content.identify,
                                    msg.contentMd5,
                                    msg.sendTime,
                                    content.ref,
                                    msg.snapchatTime,
                                    targetUid,
                                    "",
                                    "",
                                    targetUid,
                                    myUid,
                                    CHAT_TYPE_PVT
                                )
                            }
                            IMPB.MessageType.file -> {
                                val content = IMPB.FileObj.parseFrom(data)
                                MessageModel.parseFileMessage(
                                    msg.msgId,
                                    content.fileUrl,
                                    content.name,
                                    content.mimeType,
                                    content.size,
                                    msg.contentMd5,
                                    attachmentKey,
                                    msg.sendTime,
                                    content.ref,
                                    msg.snapchatTime,
                                    targetUid,
                                    "",
                                    "",
                                    msg.sendUid,
                                    myUid,
                                    CHAT_TYPE_PVT
                                )
                            }
                            IMPB.MessageType.location -> {
                                val content = IMPB.LocationObj.parseFrom(data)
                                MessageModel.parseLocationMessage(
                                    msg.msgId,
                                    content.address,
                                    content.lat,
                                    content.lng,
                                    msg.contentMd5,
                                    msg.sendTime,
                                    content.ref,
                                    msg.snapchatTime,
                                    targetUid,
                                    "",
                                    "",
                                    msg.sendUid,
                                    myUid,
                                    CHAT_TYPE_PVT
                                )
                            }
                            IMPB.MessageType.system -> {
                                val content = IMPB.SystemObj.parseFrom(data)
                                MessageModel.parseSystemTipMessage(
                                    msg.msgId,
                                    content.content,
                                    msg.sendTime,
                                    targetUid,
                                    "",
                                    "",
                                    targetUid,
                                    myUid,
                                    CHAT_TYPE_PVT
                                )
                            }
                            else -> {
                                MessageModel.parseUnKnowMessage(
                                    msg.msgId,
                                    msg.msgType.ordinal,
                                    data,
                                    msg.contentMd5,
                                    attachmentKey,
                                    msg.sendTime,
                                    msg.snapchatTime,
                                    targetUid,
                                    "",
                                    "",
                                    targetUid,
                                    myUid,
                                    CHAT_TYPE_PVT
                                )
                            }
                        }
                    } catch (e: Exception) {
                        MessageModel.parseUnKnowMessage(
                            msg.msgId,
                            msg.msgType.ordinal,
                            data,
                            msg.contentMd5,
                            attachmentKey,
                            msg.sendTime,
                            msg.snapchatTime,
                            targetUid,
                            "",
                            "",
                            targetUid,
                            myUid,
                            CHAT_TYPE_PVT
                        )
                    }
                } else {
                    MobclickAgent.reportError(
                        BaseApp.app,
                        "ReceiveMessageManager--->receiveMsgFromUser 消息无法解密，secretKey->>>${secretKey}   msg.contentMd5->>>${msg.contentMd5}  localMsg.contentMd5->>>${
                            MD5.md5(data)
                        }  msg.version->>>${msg.version}"
                    )
                    MessageModel.parseUnDecryptMessage(
                        msg.msgId,
                        msg.msgType.ordinal,
                        msgContent.toByteArray(),
                        msgKeyVersion,
                        msgContentMd5,
                        msgAttachmentKey,
                        msg.sendTime,
                        msg.snapchatTime,
                        targetUid,
                        "",
                        "",
                        targetUid,
                        myUid,
                        CHAT_TYPE_PVT
                    )
                }
            } else {
                // 版本号不一致，更新用户的key
                if (msg.version > localKeyVersion) {
                    // 消息key版本号大于本地保存的用户key版本号，更新用户的key
                    isUpdateKey = true
                }

                MobclickAgent.reportError(
                    BaseApp.app,
                    "ReceiveMessageManager--->receiveMsgFromUser 消息无法解密，版本号不一致，更新用户的key  localKeyVersion--->${localKeyVersion}  msg.version--->${msg.version}"
                )
                MessageModel.parseUnDecryptMessage(
                    msg.msgId,
                    msg.msgType.ordinal,
                    msgContent.toByteArray(),
                    msgKeyVersion,
                    msgContentMd5,
                    msgAttachmentKey,
                    msg.sendTime,
                    msg.snapchatTime,
                    targetUid,
                    "",
                    "",
                    targetUid,
                    myUid,
                    CHAT_TYPE_PVT
                )
            }

            if (msgModel.snapchatTime > 0) {
                if (msgModel.time > lastAutoDeleteFireMsgTime) {
                    msgModels.add(msgModel)
                }
            } else {
                msgModels.add(msgModel)
            }

            receiptMsgBuilder.messageTypeValue = msg.msgType.ordinal
            receiptMsgBuilder.snapchatTime = msgModel.snapchatTime
            receiptMsgBuilder.duration = msgModel.durationTime
            msgReceipts.add(receiptMsgBuilder.build())
        }

        //发送接收回执
        SendMessageManager.sendReceiptMessagePackage(msgReceipts)

        //加入数据库
        MessageController.receiveUserMessage(myUid, targetUid, msgModels) {
            // 能接受到对方消息，说明已经是好友关系，发送好友关系变更的事件
            EventBus.publishEvent(FriendRelationChangeEvent(targetUid, false))
        }

        EventBus.publishEvent(MessageStateChangeEvent(msgModels))

        if (isUpdateKey) {
            // 其他用户的秘钥才需要更新
            updateKeyPair(CHAT_TYPE_PVT, targetUid)
        }
    }

    private fun receiveMyselfWebMsgFromUser(
        myUid: Long,
        targetUid: Long,
        secretKey: String,
        localKeyVersion: Int,
        msgs: List<IMPB.OneToOneMessage>
    ) {
        val msgModels = ArrayList<MessageModel>()
        val msgReceipts = ArrayList<IMPB.ReceiptMessage>()
        val receiptStatus = CommonProto.MsgReceiptStatusBase.newBuilder()
            .setStatus(CommonProto.MsgReceiptStatus.DELIVERED)
            .setTime(ArouterServiceManager.messageService.getCurrentTime())
        val commonPref = SharePreferencesStorage.createStorageInstance(
            CommonPref::class.java,
            "user_${AccountManager.getLoginAccountUUid()}_${targetUid}"
        )
        val lastAutoDeleteFireMsgTime = commonPref.getLastAutoDeleteFireMsgTime()
        msgs.forEach { msg ->
            val receiptMsgBuilder = IMPB.ReceiptMessage.newBuilder().setMsgId(msg.msgId)
                .setType(IMPB.ChatMessageType.ONE_TO_ONE).setSendUid(msg.sendUid)
                .setReceiptStatus(receiptStatus)

            val msgKeyVersion: Int = msg.version
            val msgContentMd5: String = msg.contentMd5
            val msgAttachmentKey: String = msg.myselfAppContent.attachmentKey
            val msgContent: ByteString = msg.myselfAppContent.content

            val msgModel = if (msgKeyVersion == 0 || msgKeyVersion == localKeyVersion) {
                // 版本号一致或未加密
                var data: ByteArray? = null
                var attachmentKey: String? = ""
                try {
                    data = if (msgKeyVersion == 0) {
                        msgContent.toByteArray()
                    } else {
                        AESHelper.decryptToBytes(
                            msgContent.toByteArray(),
                            secretKey
                        )
                    }
                    attachmentKey = if (TextUtils.isEmpty(msgAttachmentKey)) {
                        ""
                    } else {
                        if (msgKeyVersion == 0 || TextUtils.isEmpty(secretKey)) {
                            msgAttachmentKey
                        } else {
                            AESHelper.decrypt(
                                HexString.hexToBuffer(msgAttachmentKey), secretKey
                            )
                        }
                    }

                } catch (e: Exception) {
                    // 解密失败
                    e.printStackTrace()
                    MobclickAgent.reportError(BaseApp.app, e)
                }

                if (data != null && (((TextUtils.isEmpty(msgContentMd5) || MD5.md5(data) == msgContentMd5) && !TextUtils.isEmpty(
                        secretKey
                    )) || msgKeyVersion == 0)
                ) {
                    when (msg.msgType) {
                        IMPB.MessageType.text -> {
                            val content = IMPB.TextObj.parseFrom(data)
                            val refMessage = if (content.ref != null && content.ref.msgId > 0) {
                                val ref = MessageModel()
                                ref.msgId = content.ref.msgId
                                ref.content = content.ref.content
                                ref.type = content.ref.typeValue
                                ref.ownerUid = content.ref.uid
                                ref.ownerName = content.ref.nickname
                                ref
                            } else {
                                null
                            }

                            MessageModel.createTextMessage(
                                content.content,
                                msg.sendTime,
                                null,
                                refMessage,
                                msg.snapchatTime,
                                myUid,
                                targetUid,
                                CHAT_TYPE_PVT
                            )
                        }
                        IMPB.MessageType.image -> {
                            val content = IMPB.ImageObj.parseFrom(data)
                            val refMessage = if (content.ref != null && content.ref.msgId > 0) {
                                val ref = MessageModel()
                                ref.msgId = content.ref.msgId
                                ref.content = content.ref.content
                                ref.type = content.ref.typeValue
                                ref.ownerUid = content.ref.uid
                                ref.ownerName = content.ref.nickname
                                ref
                            } else {
                                null
                            }

                            MessageModel.createImageMessage(
                                content.url,
                                content.thumbUrl,
                                content.width,
                                content.height,
                                attachmentKey,
                                msg.sendTime,
                                refMessage,
                                msg.snapchatTime,
                                myUid,
                                targetUid,
                                CHAT_TYPE_PVT
                            )
                        }
                        IMPB.MessageType.dynamicImage -> {
                            val content = IMPB.DynamicImageObj.parseFrom(data)
                            val refMessage = if (content.ref != null && content.ref.msgId > 0) {
                                val ref = MessageModel()
                                ref.msgId = content.ref.msgId
                                ref.content = content.ref.content
                                ref.type = content.ref.typeValue
                                ref.ownerUid = content.ref.uid
                                ref.ownerName = content.ref.nickname
                                ref
                            } else {
                                null
                            }

                            MessageModel.createDynamicImageMessage(
                                content.emoticonId,
                                content.url,
                                content.thumbUrl,
                                content.width,
                                content.height,
                                attachmentKey,
                                msg.sendTime,
                                refMessage,
                                msg.snapchatTime,
                                myUid,
                                targetUid,
                                CHAT_TYPE_PVT
                            )
                        }
                        IMPB.MessageType.video -> {
                            val content = IMPB.VideoObj.parseFrom(data)
                            val refMessage = if (content.ref != null && content.ref.msgId > 0) {
                                val ref = MessageModel()
                                ref.msgId = content.ref.msgId
                                ref.content = content.ref.content
                                ref.type = content.ref.typeValue
                                ref.ownerUid = content.ref.uid
                                ref.ownerName = content.ref.nickname
                                ref
                            } else {
                                null
                            }

                            MessageModel.createVideoMessage(
                                content.url,
                                content.thumbUrl,
                                content.width,
                                content.height,
                                content.duration,
                                attachmentKey,
                                msg.sendTime,
                                refMessage,
                                msg.snapchatTime,
                                myUid,
                                targetUid,
                                CHAT_TYPE_PVT
                            )
                        }
                        IMPB.MessageType.audio -> {
                            val content = IMPB.AudioObj.parseFrom(data)
                            val refMessage = if (content.ref != null && content.ref.msgId > 0) {
                                val ref = MessageModel()
                                ref.msgId = content.ref.msgId
                                ref.content = content.ref.content
                                ref.type = content.ref.typeValue
                                ref.ownerUid = content.ref.uid
                                ref.ownerName = content.ref.nickname
                                ref
                            } else {
                                null
                            }

                            val aHighByteArr = content.waveData.toByteArray()
                            val aHighIntArr = IntArray(aHighByteArr.size)
                            aHighByteArr.forEachIndexed { index, i ->
                                aHighIntArr[index] = i.toInt()
                            }
                            MessageModel.createVoiceMessage(
                                content.duration,
                                content.url,
                                aHighIntArr,
                                attachmentKey,
                                msg.sendTime,
                                refMessage,
                                msg.snapchatTime,
                                myUid,
                                targetUid,
                                CHAT_TYPE_PVT
                            )
                        }
                        IMPB.MessageType.nameCard -> {
                            val content = IMPB.NameCardObj.parseFrom(data)
                            val refMessage = if (content.ref != null && content.ref.msgId > 0) {
                                val ref = MessageModel()
                                ref.msgId = content.ref.msgId
                                ref.content = content.ref.content
                                ref.type = content.ref.typeValue
                                ref.ownerUid = content.ref.uid
                                ref.ownerName = content.ref.nickname
                                ref
                            } else {
                                null
                            }

                            MessageModel.createNameCardMessage(
                                content.uid,
                                content.nickName,
                                content.icon,
                                content.identify,
                                msg.sendTime,
                                refMessage,
                                msg.snapchatTime,
                                myUid,
                                targetUid,
                                CHAT_TYPE_PVT
                            )
                        }
                        IMPB.MessageType.file -> {
                            val content = IMPB.FileObj.parseFrom(data)
                            val refMessage = if (content.ref != null && content.ref.msgId > 0) {
                                val ref = MessageModel()
                                ref.msgId = content.ref.msgId
                                ref.content = content.ref.content
                                ref.type = content.ref.typeValue
                                ref.ownerUid = content.ref.uid
                                ref.ownerName = content.ref.nickname
                                ref
                            } else {
                                null
                            }

                            MessageModel.createFileMessage(
                                content.name,
                                content.size,
                                content.mimeType,
                                content.fileUrl,
                                attachmentKey,
                                msg.sendTime,
                                refMessage,
                                msg.snapchatTime,
                                myUid,
                                targetUid,
                                CHAT_TYPE_PVT
                            )
                        }
                        IMPB.MessageType.location -> {
                            val content = IMPB.LocationObj.parseFrom(data)
                            val refMessage = if (content.ref != null && content.ref.msgId > 0) {
                                val ref = MessageModel()
                                ref.msgId = content.ref.msgId
                                ref.content = content.ref.content
                                ref.type = content.ref.typeValue
                                ref.ownerUid = content.ref.uid
                                ref.ownerName = content.ref.nickname
                                ref
                            } else {
                                null
                            }

                            MessageModel.createLocationMessage(
                                content.lat,
                                content.lng,
                                content.address,
                                msg.sendTime,
                                refMessage,
                                msg.snapchatTime,
                                myUid,
                                targetUid,
                                CHAT_TYPE_PVT
                            )
                        }
                        else -> {
                            MessageModel.createUnKnowMessage(
                                msg.msgType.ordinal,
                                data,
                                msgContentMd5,
                                attachmentKey,
                                msg.sendTime,
                                msg.snapchatTime,
                                myUid,
                                targetUid,
                                CHAT_TYPE_PVT
                            )
                        }
                    }
                } else {
                    MobclickAgent.reportError(
                        BaseApp.app,
                        "ReceiveMessageManager--->receiveMyselfWebMsgFromUser 消息无法解密，secretKey->>>${secretKey}   msg.contentMd5->>>${msg.contentMd5}  localMsg.contentMd5->>>${
                            MD5.md5(data)
                        }  msg.version->>>${msg.version}"
                    )
                    MessageModel.createUnDecryptMessage(
                        msg.msgType.ordinal,
                        msgContent.toByteArray(),
                        msgKeyVersion,
                        msgContentMd5,
                        msgAttachmentKey,
                        msg.sendTime,
                        msg.snapchatTime,
                        myUid,
                        targetUid,
                        CHAT_TYPE_PVT
                    )
                }
            } else {
                MobclickAgent.reportError(
                    BaseApp.app,
                    "ReceiveMessageManager--->receiveMyselfWebMsgFromUser 消息无法解密，版本号不一致，更新用户的key  localKeyVersion--->${localKeyVersion}  msg.version--->${msg.version}"
                )
                MessageModel.createUnDecryptMessage(
                    msg.msgType.ordinal,
                    msgContent.toByteArray(),
                    msgKeyVersion,
                    msgContentMd5,
                    msgAttachmentKey,
                    msg.sendTime,
                    msg.snapchatTime,
                    myUid,
                    targetUid,
                    CHAT_TYPE_PVT
                )
            }

            msgModel.msgId = msg.msgId
            msgModel.status = MessageModel.STATUS_SENDED_HAS_RESP

            if (msgModel.snapchatTime > 0) {
                if (msgModel.time > lastAutoDeleteFireMsgTime) {
                    msgModels.add(msgModel)
                }
            } else {
                msgModels.add(msgModel)
            }

            receiptMsgBuilder.messageTypeValue = msg.msgType.ordinal
            receiptMsgBuilder.snapchatTime = msgModel.snapchatTime
            receiptMsgBuilder.duration = msgModel.durationTime
            msgReceipts.add(receiptMsgBuilder.build())
        }

        //发送接收回执
        SendMessageManager.sendReceiptMessagePackage(msgReceipts)

        //加入数据库
        MessageController.receiveMyselfWebUserMessage(myUid, targetUid, msgModels) {

        }
        EventBus.publishEvent(MessageStateChangeEvent(msgModels))
    }

    private fun receiveMsgFromGroup(msg: List<IMPB.GroupMessage>?) {
        //区分发送群和发送者
        val msgsMap = HashMap<Long, ArrayList<IMPB.GroupMessage>>()
        val myselfWebMsgsMap = HashMap<Long, ArrayList<IMPB.GroupMessage>>()
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        msg?.forEach {
            if (it.sendUid == myUid) {
                if (myselfWebMsgsMap[it.groupId] == null) {
                    myselfWebMsgsMap[it.groupId] = ArrayList()
                }
                myselfWebMsgsMap[it.groupId]?.add(it)
            } else {
                if (msgsMap[it.groupId] == null) {
                    msgsMap[it.groupId] = ArrayList()
                }
                msgsMap[it.groupId]?.add(it)
            }
        }

        myselfWebMsgsMap.forEach { myselfWebMsgs ->
            receiveMyselfMsgFromGroup(myUid, myselfWebMsgs.key, myselfWebMsgs.value)
        }

        msgsMap.forEach { groupMsgs ->
            receiveMsgFromGroup(myUid, groupMsgs.key, groupMsgs.value)
        }
    }

    private fun receiveMyselfMsgFromGroup(
        myUid: Long,
        groupId: Long,
        myselfWebMsgs: ArrayList<IMPB.GroupMessage>
    ) {
        ArouterServiceManager.groupService.getGroupInfo(
            null,
            groupId,
            { groupModel, _ ->
                val versionMsgsMap = HashMap<Int, ArrayList<IMPB.GroupMessage>>()
                myselfWebMsgs.forEach { msg ->
                    // 先判断是不是自己的web端发送的（自己发送的消息，需要录入成自己发送的标识）
                    if (versionMsgsMap[msg.version] == null) {
                        versionMsgsMap[msg.version] = ArrayList()
                    }
                    versionMsgsMap[msg.version]?.add(msg)
                }

                versionMsgsMap.forEach { versionMsgs ->
                    if (versionMsgs.key == 0) {
                        receiveMyselfWebMsgFromGroup(
                            myUid,
                            groupId,
                            "",
                            versionMsgs.value,
                            groupModel.memberCount
                        )
                    } else {
                        ArouterServiceManager.systemService.getGroupSecretKey(
                            groupId,
                            { secretKey, _ ->
                                receiveMyselfWebMsgFromGroup(
                                    myUid,
                                    groupId,
                                    secretKey,
                                    versionMsgs.value,
                                    groupModel.memberCount
                                )
                            },
                            {
                                MobclickAgent.reportError(
                                    BaseApp.app,
                                    "ReceiveMessageManager--->receiveMsgFromGroup.getGroupSecretKey  ${it.localizedMessage}"
                                )
                            })
                    }
                }
            })
    }

    private fun receiveMsgFromGroup(
        myUid: Long,
        groupId: Long,
        groupMsgs: ArrayList<IMPB.GroupMessage>
    ) {
        ArouterServiceManager.groupService.getGroupInfo(null, groupId, { groupModel, _ ->
            val versionMsgsMap = HashMap<Int, ArrayList<IMPB.GroupMessage>>()
            groupMsgs.forEach { msg ->
                // 先判断是不是自己的web端发送的（自己发送的消息，需要录入成自己发送的标识）
                if (versionMsgsMap[msg.version] == null) {
                    versionMsgsMap[msg.version] = ArrayList()
                }
                versionMsgsMap[msg.version]?.add(msg)
            }

            versionMsgsMap.forEach { versionMsgs ->
                if (versionMsgs.key == 0) {
                    receiveMsgFromGroup(
                        myUid,
                        groupId,
                        "",
                        versionMsgs.value,
                        groupModel.memberCount
                    )
                } else {
                    ArouterServiceManager.systemService.getGroupSecretKey(
                        groupId,
                        { secretKey, _ ->
                            receiveMsgFromGroup(
                                myUid,
                                groupId,
                                secretKey,
                                versionMsgs.value,
                                groupModel.memberCount
                            )
                        },
                        {
                            MobclickAgent.reportError(
                                BaseApp.app,
                                "ReceiveMessageManager--->receiveMsgFromGroup.getGroupSecretKey  ${it.localizedMessage}"
                            )
                        })
                }
            }
        })
    }

    private fun receiveMsgFromGroup(
        myUid: Long,
        targetGid: Long,
        secretKey: String,
        msgs: List<IMPB.GroupMessage>,
        memberCount: Int
    ) {
        val msgModels = ArrayList<MessageModel>()
        val msgReceipts = ArrayList<IMPB.ReceiptMessage>()
        val receiptStatus = CommonProto.MsgReceiptStatusBase.newBuilder()
            .setStatus(CommonProto.MsgReceiptStatus.DELIVERED)
            .setTime(ArouterServiceManager.messageService.getCurrentTime())
        var hasNewGroupNotice = false
        val updatedGroupMembers = HashMap<Long, CommonProto.GroupMemberBase>()
        val commonPref = SharePreferencesStorage.createStorageInstance(
            CommonPref::class.java,
            "group_${AccountManager.getLoginAccountUUid()}_${targetGid}"
        )
        val lastAutoDeleteFireMsgTime = commonPref.getLastAutoDeleteFireMsgTime()
        msgs.forEach { msg ->
            val receiptMsgBuilder = IMPB.ReceiptMessage.newBuilder().setMsgId(msg.msgId)
                .setType(IMPB.ChatMessageType.GROUP).setGroupId(msg.groupId).setSendUid(msg.sendUid)
                .setReceiptStatus(receiptStatus)
            var data: ByteArray? = null
            var attachmentKey: String? = ""
            try {
                data = if (msg.version == 0) {
                    msg.content.toByteArray()
                } else {
                    AESHelper.decryptToBytes(
                        msg.content.toByteArray(),
                        secretKey
                    )
                }
                attachmentKey = if (TextUtils.isEmpty(msg.attachmentKey)) {
                    ""
                } else {
                    if (msg.version == 0 || TextUtils.isEmpty(secretKey)) {
                        msg.attachmentKey
                    } else {
                        AESHelper.decrypt(
                            HexString.hexToBuffer(msg.attachmentKey), secretKey
                        )
                    }
                }
            } catch (e: Exception) {
                // 解密失败
                e.printStackTrace()
                MobclickAgent.reportError(BaseApp.app, e)
            }

            val msgModel =
                if ((data?.isNotEmpty() == true) && ((TextUtils.isEmpty(msg.contentMd5) || MD5.md5(
                        data
                    ) == msg.contentMd5) || msg.version == 0)
                ) {
                    try {
                        when {
                            msg.msgType == IMPB.MessageType.text -> {
                                val content = IMPB.TextObj.parseFrom(data)
                                val atUids = msg.atUidsList?.toLongArray()
                                    ?: ArrayList<Long>().toLongArray()
                                MessageModel.parseTextMessage(
                                    msg.msgId,
                                    content.content,
                                    msg.contentMd5,
                                    msg.sendTime,
                                    atUids,
                                    content.ref,
                                    msg.snapchatTime,
                                    targetGid,
                                    getSendMemberDisplayName(msg.sendMember),
                                    getSendMemberIcon(msg.sendMember),
                                    msg.sendUid,
                                    myUid,
                                    CHAT_TYPE_GROUP
                                )
                            }
                            msg.msgType == IMPB.MessageType.image -> {
                                val content = IMPB.ImageObj.parseFrom(data)
                                MessageModel.parseImageMessage(
                                    msg.msgId,
                                    content.url,
                                    content.thumbUrl,
                                    content.width,
                                    content.height,
                                    msg.contentMd5,
                                    attachmentKey,
                                    msg.sendTime,
                                    content.ref,
                                    msg.snapchatTime,
                                    targetGid,
                                    getSendMemberDisplayName(msg.sendMember),
                                    getSendMemberIcon(msg.sendMember),
                                    msg.sendUid,
                                    myUid,
                                    CHAT_TYPE_GROUP
                                )
                            }
                            msg.msgType == IMPB.MessageType.dynamicImage -> {
                                val content = IMPB.DynamicImageObj.parseFrom(data)
                                MessageModel.parseDynamicImageMessage(
                                    msg.msgId,
                                    content.emoticonId,
                                    content.url,
                                    content.thumbUrl,
                                    content.width,
                                    content.height,
                                    msg.contentMd5,
                                    attachmentKey,
                                    msg.sendTime,
                                    content.ref,
                                    msg.snapchatTime,
                                    targetGid,
                                    getSendMemberDisplayName(msg.sendMember),
                                    getSendMemberIcon(msg.sendMember),
                                    msg.sendUid,
                                    myUid,
                                    content.fileSize,
                                    CHAT_TYPE_GROUP
                                )
                            }
                            msg.msgType == IMPB.MessageType.video -> {
                                val content = IMPB.VideoObj.parseFrom(data)
                                MessageModel.parseVideoMessage(
                                    msg.msgId,
                                    content.url,
                                    content.thumbUrl,
                                    content.width,
                                    content.height,
                                    content.duration,
                                    msg.contentMd5,
                                    attachmentKey,
                                    msg.sendTime,
                                    content.ref,
                                    msg.snapchatTime,
                                    targetGid,
                                    getSendMemberDisplayName(msg.sendMember),
                                    getSendMemberIcon(msg.sendMember),
                                    msg.sendUid,
                                    myUid,
                                    CHAT_TYPE_GROUP
                                )
                            }
                            msg.msgType == IMPB.MessageType.audio -> {
                                val content = IMPB.AudioObj.parseFrom(data)
                                val aHighByteArr = content.waveData.toByteArray()
                                val aHighIntArr = IntArray(aHighByteArr.size)
                                aHighByteArr.forEachIndexed { index, i ->
                                    aHighIntArr[index] = i.toInt()
                                }
                                MessageModel.parseVoiceMessage(
                                    msg.msgId,
                                    content.duration,
                                    content.url,
                                    aHighIntArr,
                                    msg.contentMd5,
                                    attachmentKey,
                                    msg.sendTime,
                                    content.ref,
                                    msg.snapchatTime,
                                    targetGid,
                                    getSendMemberDisplayName(msg.sendMember),
                                    getSendMemberIcon(msg.sendMember),
                                    msg.sendUid,
                                    myUid,
                                    CHAT_TYPE_GROUP
                                )
                            }
                            msg.msgType == IMPB.MessageType.nameCard -> {
                                val content = IMPB.NameCardObj.parseFrom(data)
                                MessageModel.parseNameCardMessage(
                                    msg.msgId,
                                    content.uid,
                                    content.nickName,
                                    content.icon,
                                    content.identify,
                                    msg.contentMd5,
                                    msg.sendTime,
                                    content.ref,
                                    msg.snapchatTime,
                                    targetGid,
                                    getSendMemberDisplayName(msg.sendMember),
                                    getSendMemberIcon(msg.sendMember),
                                    msg.sendUid,
                                    myUid,
                                    CHAT_TYPE_GROUP
                                )
                            }
                            msg.msgType == IMPB.MessageType.file -> {
                                val content = IMPB.FileObj.parseFrom(data)
                                MessageModel.parseFileMessage(
                                    msg.msgId,
                                    content.fileUrl,
                                    content.name,
                                    content.mimeType,
                                    content.size,
                                    msg.contentMd5,
                                    attachmentKey,
                                    msg.sendTime,
                                    content.ref,
                                    msg.snapchatTime,
                                    targetGid,
                                    getSendMemberDisplayName(msg.sendMember),
                                    getSendMemberIcon(msg.sendMember),
                                    msg.sendUid,
                                    myUid,
                                    CHAT_TYPE_GROUP
                                )
                            }
                            msg.msgType == IMPB.MessageType.location -> {
                                val content = IMPB.LocationObj.parseFrom(data)
                                MessageModel.parseLocationMessage(
                                    msg.msgId,
                                    content.address,
                                    content.lat,
                                    content.lng,
                                    msg.contentMd5,
                                    msg.sendTime,
                                    content.ref,
                                    msg.snapchatTime,
                                    targetGid,
                                    getSendMemberDisplayName(msg.sendMember),
                                    getSendMemberIcon(msg.sendMember),
                                    msg.sendUid,
                                    myUid,
                                    CHAT_TYPE_GROUP
                                )
                            }
                            msg.msgType == IMPB.MessageType.notice -> {
                                hasNewGroupNotice = true
                                val content = IMPB.GroupNoticeObj.parseFrom(data)
                                MessageModel.parseNoticeMessage(
                                    msg.msgId,
                                    content.noticeId,
                                    content.content,
                                    content.showNotify,
                                    msg.contentMd5,
                                    msg.sendTime,
                                    targetGid,
                                    getSendMemberDisplayName(msg.sendMember),
                                    getSendMemberIcon(msg.sendMember),
                                    msg.sendUid,
                                    myUid,
                                    CHAT_TYPE_GROUP
                                )
                            }
                            msg.msgType == IMPB.MessageType.system -> {
                                val content = IMPB.SystemObj.parseFrom(data)
                                MessageModel.parseSystemTipMessage(
                                    msg.msgId,
                                    content.content,
                                    msg.sendTime,
                                    targetGid,
                                    getSendMemberDisplayName(msg.sendMember),
                                    getSendMemberIcon(msg.sendMember),
                                    msg.sendUid,
                                    myUid,
                                    CHAT_TYPE_GROUP
                                )
                            }
                            else -> {
                                MessageModel.parseUnKnowMessage(
                                    msg.msgId,
                                    msg.msgType.ordinal,
                                    data,
                                    msg.contentMd5,
                                    attachmentKey,
                                    msg.sendTime,
                                    msg.snapchatTime,
                                    targetGid,
                                    getSendMemberDisplayName(msg.sendMember),
                                    getSendMemberIcon(msg.sendMember),
                                    msg.sendUid,
                                    myUid,
                                    CHAT_TYPE_GROUP
                                )
                            }
                        }
                    } catch (e: Exception) {
                        MessageModel.parseUnKnowMessage(
                            msg.msgId,
                            msg.msgType.ordinal,
                            data,
                            msg.contentMd5,
                            attachmentKey,
                            msg.sendTime,
                            msg.snapchatTime,
                            targetGid,
                            getSendMemberDisplayName(msg.sendMember),
                            getSendMemberIcon(msg.sendMember),
                            msg.sendUid,
                            myUid,
                            CHAT_TYPE_GROUP
                        )
                    }
                } else {
                    MobclickAgent.reportError(
                        BaseApp.app,
                        "ReceiveMessageManager--->receiveMsgFromGroup 消息无法解密，secretKey->>>${secretKey}   msg.contentMd5->>>${msg.contentMd5}  localMsg.contentMd5->>>${
                            MD5.md5(data)
                        }  msg.version->>>${msg.version}"
                    )
                    MessageModel.parseUnDecryptMessage(
                        msg.msgId,
                        msg.msgType.ordinal,
                        msg.content.toByteArray(),
                        msg.version,
                        msg.contentMd5,
                        msg.attachmentKey,
                        msg.sendTime,
                        msg.snapchatTime,
                        targetGid,
                        getSendMemberDisplayName(msg.sendMember),
                        getSendMemberIcon(msg.sendMember),
                        msg.sendUid,
                        myUid,
                        CHAT_TYPE_GROUP
                    )
                }

            receiptMsgBuilder.messageTypeValue = msg.msgType.ordinal
            receiptMsgBuilder.snapchatTime = msgModel.snapchatTime
            receiptMsgBuilder.duration = msgModel.durationTime

            msgModel.receiptCount = memberCount

            if (msgModel.snapchatTime > 0) {
                if (msgModel.time > lastAutoDeleteFireMsgTime) {
                    msgModels.add(msgModel)
                }
            } else {
                msgModels.add(msgModel)
            }

            msgReceipts.add(receiptMsgBuilder.build())

            // 更新发送者的资料
            updatedGroupMembers[msg.sendMember.user.uid] = msg.sendMember
        }

        //发送接收回执
        SendMessageManager.sendReceiptMessagePackage(msgReceipts)

        //加入数据库
        MessageController.receiveGroupMessage(myUid, targetGid, msgModels) {
            if (hasNewGroupNotice) {
                // 保存新公告提醒
                val storageName = "group_notice_${myUid}_$targetGid"
                val commonPref = SharePreferencesStorage.createStorageInstance(
                    CommonPref::class.java,
                    storageName
                )
                commonPref.putHasNewGroupNotice(true)

                // 通知前台更新
                EventBus.publishEvent(GroupInfoChangeEvent(targetGid))
            }

            if (updatedGroupMembers.isNotEmpty()) {
                updatedGroupMembers.forEach {
                    ArouterServiceManager.groupService.updateGroupMemberInfoFromSocket(
                        targetGid,
                        it.value,
                        { isChange ->
                            if (isChange) {
                                EventBus.publishEvent(GroupMemberChangeEvent(targetGid))
                            }
                        })
                }
            }
        }
    }

    private fun receiveMyselfWebMsgFromGroup(
        myUid: Long,
        targetGid: Long,
        secretKey: String,
        msgs: List<IMPB.GroupMessage>,
        memberCount: Int
    ) {
        val msgModels = ArrayList<MessageModel>()
        val msgReceipts = ArrayList<IMPB.ReceiptMessage>()
        var hasNewGroupNotice = false
        val receiptStatus = CommonProto.MsgReceiptStatusBase.newBuilder()
            .setStatus(CommonProto.MsgReceiptStatus.DELIVERED)
            .setTime(ArouterServiceManager.messageService.getCurrentTime())
        val commonPref = SharePreferencesStorage.createStorageInstance(
            CommonPref::class.java,
            "group_${AccountManager.getLoginAccountUUid()}_${targetGid}"
        )
        val lastAutoDeleteFireMsgTime = commonPref.getLastAutoDeleteFireMsgTime()
        msgs.forEach { msg ->
            val receiptMsgBuilder = IMPB.ReceiptMessage.newBuilder().setMsgId(msg.msgId)
                .setType(IMPB.ChatMessageType.GROUP).setGroupId(msg.groupId).setSendUid(msg.sendUid)
                .setReceiptStatus(receiptStatus)
            var data: ByteArray? = null
            var attachmentKey: String? = ""
            try {
                data = if (msg.version == 0) {
                    msg.content.toByteArray()
                } else {
                    AESHelper.decryptToBytes(
                        msg.content.toByteArray(),
                        secretKey
                    )
                }
                attachmentKey = if (TextUtils.isEmpty(msg.attachmentKey)) {
                    ""
                } else {
                    if (msg.version == 0 || TextUtils.isEmpty(secretKey)) {
                        msg.attachmentKey
                    } else {
                        AESHelper.decrypt(
                            HexString.hexToBuffer(msg.attachmentKey), secretKey
                        )
                    }
                }
            } catch (e: Exception) {
                // 解密失败
                e.printStackTrace()
                MobclickAgent.reportError(BaseApp.app, e)
            }

            val msgModel =
                if ((data?.isNotEmpty() == true) && ((TextUtils.isEmpty(msg.contentMd5) || MD5.md5(
                        data
                    ) == msg.contentMd5) || msg.version == 0)
                ) {
                    when (msg.msgType) {
                        IMPB.MessageType.text -> {
                            val content = IMPB.TextObj.parseFrom(data)
                            val atUids = msg.atUidsList?.toLongArray()
                                ?: ArrayList<Long>().toLongArray()
                            val refMessage = if (content.ref != null && content.ref.msgId > 0) {
                                val ref = MessageModel()
                                ref.msgId = content.ref.msgId
                                ref.content = content.ref.content
                                ref.type = content.ref.typeValue
                                ref.ownerUid = content.ref.uid
                                ref.ownerName = content.ref.nickname
                                ref
                            } else {
                                null
                            }

                            MessageModel.createTextMessage(
                                content.content,
                                msg.sendTime,
                                atUids,
                                refMessage,
                                msg.snapchatTime,
                                myUid,
                                targetGid,
                                CHAT_TYPE_GROUP
                            )
                        }
                        IMPB.MessageType.image -> {
                            val content = IMPB.ImageObj.parseFrom(data)
                            val refMessage = if (content.ref != null && content.ref.msgId > 0) {
                                val ref = MessageModel()
                                ref.msgId = content.ref.msgId
                                ref.content = content.ref.content
                                ref.type = content.ref.typeValue
                                ref.ownerUid = content.ref.uid
                                ref.ownerName = content.ref.nickname
                                ref
                            } else {
                                null
                            }

                            MessageModel.createImageMessage(
                                content.url,
                                content.thumbUrl,
                                content.width,
                                content.height,
                                attachmentKey,
                                msg.sendTime,
                                refMessage,
                                msg.snapchatTime,
                                myUid,
                                targetGid,
                                CHAT_TYPE_GROUP
                            )
                        }
                        IMPB.MessageType.dynamicImage -> {
                            val content = IMPB.DynamicImageObj.parseFrom(data)
                            val refMessage = if (content.ref != null && content.ref.msgId > 0) {
                                val ref = MessageModel()
                                ref.msgId = content.ref.msgId
                                ref.content = content.ref.content
                                ref.type = content.ref.typeValue
                                ref.ownerUid = content.ref.uid
                                ref.ownerName = content.ref.nickname
                                ref
                            } else {
                                null
                            }

                            MessageModel.createDynamicImageMessage(
                                content.emoticonId,
                                content.url,
                                content.thumbUrl,
                                content.width,
                                content.height,
                                attachmentKey,
                                msg.sendTime,
                                refMessage,
                                msg.snapchatTime,
                                myUid,
                                targetGid,
                                CHAT_TYPE_GROUP
                            )
                        }
                        IMPB.MessageType.video -> {
                            val content = IMPB.VideoObj.parseFrom(data)
                            val refMessage = if (content.ref != null && content.ref.msgId > 0) {
                                val ref = MessageModel()
                                ref.msgId = content.ref.msgId
                                ref.content = content.ref.content
                                ref.type = content.ref.typeValue
                                ref.ownerUid = content.ref.uid
                                ref.ownerName = content.ref.nickname
                                ref
                            } else {
                                null
                            }

                            MessageModel.createVideoMessage(
                                content.url,
                                content.thumbUrl,
                                content.width,
                                content.height,
                                content.duration,
                                attachmentKey,
                                msg.sendTime,
                                refMessage,
                                msg.snapchatTime,
                                myUid,
                                targetGid,
                                CHAT_TYPE_GROUP
                            )
                        }
                        IMPB.MessageType.audio -> {
                            val content = IMPB.AudioObj.parseFrom(data)
                            val refMessage = if (content.ref != null && content.ref.msgId > 0) {
                                val ref = MessageModel()
                                ref.msgId = content.ref.msgId
                                ref.content = content.ref.content
                                ref.type = content.ref.typeValue
                                ref.ownerUid = content.ref.uid
                                ref.ownerName = content.ref.nickname
                                ref
                            } else {
                                null
                            }

                            val aHighByteArr = content.waveData.toByteArray()
                            val aHighIntArr = IntArray(aHighByteArr.size)
                            aHighByteArr.forEachIndexed { index, i ->
                                aHighIntArr[index] = i.toInt()
                            }
                            MessageModel.createVoiceMessage(
                                content.duration,
                                content.url,
                                aHighIntArr,
                                attachmentKey,
                                msg.sendTime,
                                refMessage,
                                msg.snapchatTime,
                                myUid,
                                targetGid,
                                CHAT_TYPE_GROUP
                            )
                        }
                        IMPB.MessageType.nameCard -> {
                            val content = IMPB.NameCardObj.parseFrom(data)
                            val refMessage = if (content.ref != null && content.ref.msgId > 0) {
                                val ref = MessageModel()
                                ref.msgId = content.ref.msgId
                                ref.content = content.ref.content
                                ref.type = content.ref.typeValue
                                ref.ownerUid = content.ref.uid
                                ref.ownerName = content.ref.nickname
                                ref
                            } else {
                                null
                            }

                            MessageModel.createNameCardMessage(
                                content.uid,
                                content.nickName,
                                content.icon,
                                content.identify,
                                msg.sendTime,
                                refMessage,
                                msg.snapchatTime,
                                myUid,
                                targetGid,
                                CHAT_TYPE_GROUP
                            )
                        }
                        IMPB.MessageType.file -> {
                            val content = IMPB.FileObj.parseFrom(data)
                            val refMessage = if (content.ref != null && content.ref.msgId > 0) {
                                val ref = MessageModel()
                                ref.msgId = content.ref.msgId
                                ref.content = content.ref.content
                                ref.type = content.ref.typeValue
                                ref.ownerUid = content.ref.uid
                                ref.ownerName = content.ref.nickname
                                ref
                            } else {
                                null
                            }

                            MessageModel.createFileMessage(
                                content.name,
                                content.size,
                                content.mimeType,
                                content.fileUrl,
                                attachmentKey,
                                msg.sendTime,
                                refMessage,
                                msg.snapchatTime,
                                myUid,
                                targetGid,
                                CHAT_TYPE_GROUP
                            )
                        }
                        IMPB.MessageType.location -> {
                            val content = IMPB.LocationObj.parseFrom(data)
                            val refMessage = if (content.ref != null && content.ref.msgId > 0) {
                                val ref = MessageModel()
                                ref.msgId = content.ref.msgId
                                ref.content = content.ref.content
                                ref.type = content.ref.typeValue
                                ref.ownerUid = content.ref.uid
                                ref.ownerName = content.ref.nickname
                                ref
                            } else {
                                null
                            }

                            MessageModel.createLocationMessage(
                                content.lat,
                                content.lng,
                                content.address,
                                msg.sendTime,
                                refMessage,
                                msg.snapchatTime,
                                myUid,
                                targetGid,
                                CHAT_TYPE_GROUP
                            )
                        }
                        IMPB.MessageType.notice -> {
                            hasNewGroupNotice = true
                            val content = IMPB.GroupNoticeObj.parseFrom(data)
                            MessageModel.createNoticeMessage(
                                content.noticeId,
                                content.content,
                                content.showNotify,
                                msg.sendTime,
                                myUid,
                                targetGid,
                                CHAT_TYPE_GROUP
                            )
                        }
                        else -> {
                            MessageModel.createUnKnowMessage(
                                msg.msgType.ordinal,
                                data,
                                msg.contentMd5,
                                attachmentKey,
                                msg.sendTime,
                                msg.snapchatTime,
                                myUid,
                                targetGid,
                                CHAT_TYPE_GROUP
                            )
                        }
                    }
                } else {
                    MobclickAgent.reportError(
                        BaseApp.app,
                        "ReceiveMessageManager--->receiveMyselfWebMsgFromGroup 消息无法解密，secretKey->>>${secretKey}   msg.contentMd5->>>${msg.contentMd5}  localMsg.contentMd5->>>${
                            MD5.md5(data)
                        }  msg.version->>>${msg.version}"
                    )
                    MessageModel.createUnDecryptMessage(
                        msg.msgType.ordinal,
                        data,
                        msg.version,
                        msg.contentMd5,
                        attachmentKey,
                        msg.sendTime,
                        msg.snapchatTime,
                        myUid,
                        targetGid,
                        CHAT_TYPE_GROUP
                    )
                }

            receiptMsgBuilder.messageTypeValue = msg.msgType.ordinal
            receiptMsgBuilder.snapchatTime = msgModel.snapchatTime
            receiptMsgBuilder.duration = msgModel.durationTime

            if (msgModel.snapchatTime > 0) {
                msgModel.expireTime = msgModel.time + msgModel.snapchatTime * 1000
            }

            msgModel.msgId = msg.msgId
            msgModel.receiptCount = memberCount
            msgModel.status = MessageModel.STATUS_SENDED_HAS_RESP

            if (msgModel.snapchatTime > 0) {
                if (msgModel.time > lastAutoDeleteFireMsgTime) {
                    msgModels.add(msgModel)
                }
            } else {
                msgModels.add(msgModel)
            }

            msgReceipts.add(receiptMsgBuilder.build())
        }

        //发送接收回执
        SendMessageManager.sendReceiptMessagePackage(msgReceipts)

        //加入数据库
        MessageController.receiveMyselfWebGroupMessage(myUid, targetGid, msgModels) {
            if (hasNewGroupNotice) {
                // 保存新公告提醒
                val storageName = "group_notice_${myUid}_$targetGid"
                val commonPref = SharePreferencesStorage.createStorageInstance(
                    CommonPref::class.java,
                    storageName
                )
                commonPref.putHasNewGroupNotice(true)

                // 通知前台更新
                EventBus.publishEvent(GroupInfoChangeEvent(targetGid))
            }
        }
    }

    private fun getSendMemberDisplayName(groupMember: CommonProto.GroupMemberBase?): String {
        return if (groupMember != null) {
            if (!TextUtils.isEmpty(groupMember.user?.friendRelation?.remarkName)) {
                groupMember.user.friendRelation.remarkName
            } else if (!TextUtils.isEmpty(groupMember.groupNickName)) {
                groupMember.groupNickName
            } else {
                groupMember.user.nickName
            }
        } else {
            ""
        }
    }

    private fun getSendMemberIcon(groupMember: CommonProto.GroupMemberBase): String {
        return groupMember.user?.icon ?: ""
    }

    private fun saveMessageToSearchDB(msgModels: List<MessageModel>) {
        val searchList = mutableListOf<SearchChatModel>()
        msgModels.forEach {
            if (it.snapchatTime == 0) {
                val content = when (it.type) {
                    MessageModel.MESSAGE_TYPE_NOTICE -> {
                        it.noticeMessageBean.content
                    }
                    MessageModel.MESSAGE_TYPE_TEXT -> {
                        it.content
                    }
                    MessageModel.MESSAGE_TYPE_FILE -> {
                        it.fileMessageContentBean.name
                    }
                    MessageModel.MESSAGE_TYPE_LOCATION -> {
                        it.locationMessageContentBean.address
                    }
                    else -> {
                        ""
                    }
                }
                if (!TextUtils.isEmpty(content)) {
                    val id = if (it.isSend == 1) {
                        it.targetId
                    } else {
                        it.senderId
                    }
                    searchList.add(
                        SearchChatModel(
                            id,
                            it.chatType,
                            content,
                            it.msgId,
                            it.id,
                            it.time,
                            it.type,
                            if (it.chatType == CHAT_TYPE_PVT) id else -id,
                            it.ownerUid
                        )
                    )
                }
            }
        }
        if (searchList.size > 0)
            ArouterServiceManager.searchService.insertMessage(searchList)
    }
}