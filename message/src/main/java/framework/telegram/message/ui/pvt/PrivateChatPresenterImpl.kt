package framework.telegram.message.ui.pvt

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.im.MessageModel
import framework.telegram.support.BaseApp
import framework.telegram.support.system.log.AppLogcat
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.Flowable
import android.net.Uri
import com.im.domain.pb.ContactsProto
import com.trello.rxlifecycle3.RxLifecycle
import com.umeng.analytics.MobclickAgent
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.UnreadMessageEvent
import framework.telegram.message.db.RealmCreator
import framework.telegram.message.manager.*
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.utils.FileUtils
import io.realm.*
import java.io.File

class PrivateChatPresenterImpl : PrivateChatContract.Presenter,
    RealmChangeListener<RealmResults<MessageModel>> {

    private val mContext: Context
    private val mView: PrivateChatContract.View
    private val mViewObservalbe: Observable<ActivityEvent>

    private val mRealm by lazy { RealmCreator.getPvtChatMessagesRealm(mMineUid, mTargetUid) }

    private val mMineUid by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
    }

    private var mTargetUid: Long = 0

    private var mMessages: RealmResults<MessageModel>? = null

    private var mLastMessageTime = 0L

    private var isLoadAll = false

    constructor(
        view: PrivateChatContract.View,
        context: Context,
        observable: Observable<ActivityEvent>,
        targetUid: Long
    ) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mContext = context
        this.mTargetUid = targetUid
        view.setPresenter(this)
    }

    @SuppressLint("CheckResult")
    override fun sendTextMessage(
        msg: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel?
    ) {
        SendMessageManager.sendTextMessageToUser(
            msg,
            refMessageBean,
            myUid,
            targetUid,
            contactDataModel
        )
    }

    @SuppressLint("CheckResult")
    override fun sendVoiceMessage(
        recordTime: Int,
        recordFilePath: String,
        highDArr: Array<Int>,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel?
    ) {
        SendMessageManager.sendVoiceMessageToUser(
            recordTime,
            File(recordFilePath),
            highDArr.toIntArray(),
            refMessageBean,
            myUid,
            targetUid,
            contactDataModel
        )
    }

    override fun sendImageMessage(
        imageFilePath: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel?
    ) {
        SendMessageManager.sendImageMessageToUser(
            imageFilePath,
            refMessageBean,
            myUid,
            targetUid,
            contactDataModel
        )
    }

    override fun sendDynamicImageMessage(
        emoticonId: Long,
        imageFilePath: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel?
    ) {
        SendMessageManager.sendDynamicImageMessageToUser(
            emoticonId,
            imageFilePath,
            refMessageBean,
            myUid,
            targetUid,
            contactDataModel
        )
    }

    override fun sendDynamicImageUrlMessage(
        emoticonId: Long,
        imageFileUrl: String,
        width: Int,
        height: Int,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel?
    ) {
        SendMessageManager.sendDynamicImageMessageToUser(
            emoticonId,
            imageFileUrl,
            width,
            height,
            refMessageBean,
            myUid,
            targetUid,
            contactDataModel
        )
    }

    @SuppressLint("CheckResult")
    override fun sendVideoMessage(
        videoFilePath: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel?
    ) {
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(BaseApp.app, Uri.fromFile(File(videoFilePath)))

            // 获取视频尺寸和时长
            val duration =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    ?: 0 //时长(毫秒)
            val rotation = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull()
                ?: 0//方向
            var width =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                    ?: 0//宽
            var height =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                    ?: 0//高
            if (rotation == 90 || rotation == 270) {
                val tmp = width
                width = height
                height = tmp
            }
            // 生成视频缩略图
            val firstFrame = mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
           // val videoThumbFile = File(videoFilePath + "___thumb")
            val videoThumbFile = File(FileUtils.getAPPInternalStorageFilePath(File(videoFilePath)) + "___thumb")
            FileUtils.saveBitmap(firstFrame, videoThumbFile)

            SendMessageManager.sendVideoMessageToUser(
                File(videoFilePath),
                videoThumbFile,
                width,
                height,
                (duration / 1000).toInt(),
                refMessageBean,
                myUid,
                targetUid,
                contactDataModel
            )
        } catch (ex: Exception) {
            BaseApp.app.toast(mContext.getString(R.string.video_conversion_has_an_exception))
            AppLogcat.logger.e(ex)
            MobclickAgent.reportError(BaseApp.app, ex)
        } finally {
            mmr.release()
        }
    }

    override fun sendNameCardMessage(
        uid: Long,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel?
    ) {
        SendMessageManager.sendNameCardMessageToUser(
            uid,
            refMessageBean,
            myUid,
            targetUid,
            contactDataModel
        )
    }

    override fun sendLocationMessage(
        lat: Long,
        lng: Long,
        address: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel?
    ) {
        SendMessageManager.sendLocationMessageToUser(
            lat,
            lng,
            address,
            refMessageBean,
            myUid,
            targetUid,
            contactDataModel
        )
    }

    override fun sendFileMessage(
        filePath: String,
        mimeType: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        contactDataModel: ContactDataModel?
    ) {
        SendMessageManager.sendFileMessageToUser(
            filePath,
            mimeType,
            refMessageBean,
            myUid,
            targetUid,
            contactDataModel
        )
    }

    override fun start(isLoadAll: Boolean) {
        if (isLoadAll) {
            loadAllMessageHistory()
        } else {
            loadMessageHistory()
        }
    }

    override fun start() {
        start(true)
    }

    @SuppressLint("CheckResult")
    fun destory() {
        mRealm.removeAllChangeListeners()
        mRealm.close()
    }

    @SuppressLint("CheckResult")
    override fun loadMessageHistory() {
        isLoadAll = false
        Flowable.just<Realm>(mRealm)
            .compose(RxLifecycle.bindUntilEvent(mViewObservalbe, ActivityEvent.DESTROY))
            .subscribeOn(AndroidSchedulers.mainThread())
            .map {
                it.where(MessageModel::class.java)
                    ?.beginGroup()
                    ?.notEqualTo("type", MessageModel.MESSAGE_TYPE_RECALL)
                    ?.and()
                    ?.notEqualTo("type", MessageModel.MESSAGE_TYPE_RECALL_SUCCESS)
                    ?.endGroup()
                    ?.beginGroup()
                    ?.equalTo("expireTime", 0L)
                    ?.or()
                    ?.greaterThan("expireTime", System.currentTimeMillis())
                    ?.endGroup()
                    ?.sort("time", Sort.DESCENDING)
                    ?.limit(Constant.Common.FIRST_LOAD_MESSAGE_HISTORY_COUNT)?.findAllAsync()
            }
            .observeOn(AndroidSchedulers.mainThread()).subscribe {
                mMessages = it
                mMessages?.addChangeListener(this@PrivateChatPresenterImpl)
                mView.onMessagesLoadComplete(mMessages)
            }
    }

    @SuppressLint("CheckResult")
    override fun loadAllMessageHistory() {
        isLoadAll = true
        Flowable.just<Realm>(mRealm)
            .compose(RxLifecycle.bindUntilEvent(mViewObservalbe, ActivityEvent.DESTROY))
            .subscribeOn(AndroidSchedulers.mainThread())
            .map {
                it.where(MessageModel::class.java)
                    ?.beginGroup()
                    ?.notEqualTo("type", MessageModel.MESSAGE_TYPE_RECALL)
                    ?.and()
                    ?.notEqualTo("type", MessageModel.MESSAGE_TYPE_RECALL_SUCCESS)
                    ?.endGroup()
                    ?.beginGroup()
                    ?.equalTo("expireTime", 0L)
                    ?.or()
                    ?.greaterThan("expireTime", System.currentTimeMillis())
                    ?.endGroup()
                    ?.sort("time")?.findAllAsync()
            }
            .observeOn(AndroidSchedulers.mainThread()).subscribe {
                mMessages = it
                mMessages?.addChangeListener(this@PrivateChatPresenterImpl)
                mView.onMessagesLoadComplete(mMessages)
            }
    }

    override fun setAllMessageReaded() {
        if (mView.isActivityPause()) {
            return
        }

        //所有消息改成已阅
        MessagesManager.setAllUserMessageReaded(mMineUid, mTargetUid, { c, id ->
            mView.showNewMsgTip(c, id)

            // 会话改成已阅
            ChatsHistoryManager.setChatMessagesAllReaded(
                mMineUid,
                ChatModel.CHAT_TYPE_PVT,
                mTargetUid,
                {
                    EventBus.publishEvent(UnreadMessageEvent())//修改红点数字
                })
        })
    }

    override fun addFriend(
        identify: String
    ) {
        ArouterServiceManager.contactService.addFriend(
            identify, 0, "", {
                mView.showAddFriendMsg()
            }) { error, msg ->
            mView.showErrorMsg(error, msg)
        }
    }

    override fun getAddToken(identify: String, userId: Long, findSign: String) {
        ArouterServiceManager.contactService.getAddToken(
            identify, userId, findSign, { userId, addToken ->
                mView.getAddToken(userId, addToken)
            }) { error, msg ->
            mView.showErrorMsg(error, msg)
        }
    }

    override fun checkFriendShip(
        myUid: Long,
        targetUid: Long,
        complete: (Boolean) -> Unit,
        error: ((Throwable) -> Unit)?
    ) {
        ArouterServiceManager.contactService.checkFriendShip(myUid, targetUid, {
            complete.invoke(it)
        }) {
            error?.invoke(it)
        }
    }

    override fun updataFriendShip(
        myUid: Long,
        targetUid: Long,
        isDeleteMe: Boolean,
        complete: () -> Unit,
        error: ((Throwable) -> Unit)?
    ) {
        ArouterServiceManager.contactService.updataFriendShip(targetUid, isDeleteMe, {
            complete.invoke()
        }) {
            error?.invoke(it)
        }
    }

    @SuppressLint("CheckResult")
    override fun onChange(result: RealmResults<MessageModel>) {
        if (!result.isValid)
            return

        try {
            val data = if (isLoadAll) result else result.sort("time", Sort.ASCENDING)

            if (data.isNotEmpty()) {
                val lastMessageTime = data.last()?.time ?: 0
                if (mLastMessageTime < lastMessageTime) {
                    setAllMessageReaded()
                    mView.resetInputingStatus()
                }

                mLastMessageTime = lastMessageTime
            }

            if (mView.isActive()) {
                mView.onMessagesChanged(data)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun isLoadAll(): Boolean {
        return isLoadAll;
    }
}