package framework.telegram.message.ui.group

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
import android.util.Log
import com.trello.rxlifecycle3.RxLifecycle
import com.trello.rxlifecycle3.android.FragmentEvent
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import com.umeng.analytics.MobclickAgent
import framework.ideas.common.bean.RefMessageBean
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.UnreadMessageEvent
import framework.telegram.message.db.RealmCreator
import framework.telegram.message.manager.*
import framework.telegram.message.sp.CommonPref
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.ui.utils.FileUtils
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.ThreadUtils
import io.realm.*
import java.io.File

class GroupChatPresenterImpl : GroupChatContract.Presenter,
    RealmChangeListener<RealmResults<MessageModel>> {

    private val mContext: Context
    private val mView: GroupChatContract.View
    private val mViewObservalbe: Observable<ActivityEvent>

    private val mRealm by lazy { RealmCreator.getGroupChatMessagesRealm(mMineUid, mTargetGid) }

    private val mMineUid by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
    }

    private var mTargetGid: Long = 0

    private var mMessages: RealmResults<MessageModel>? = null

    private val mAtMeMessageLocalIds = ArrayList<Long>()

    private val mCommonPref by lazy {
        SharePreferencesStorage.createStorageInstance(
            CommonPref::class.java,
            "group_notice_${mMineUid}_$mTargetGid"
        )
    }

    private var mLastMessageTime = 0L

    private var isLoadAll = false

    constructor(
        view: GroupChatContract.View,
        context: Context,
        observable: Observable<ActivityEvent>,
        targetGid: Long
    ) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mContext = context
        this.mTargetGid = targetGid
        view.setPresenter(this)
    }

    private fun checkShutup(): Boolean {
        if (mView.isShutup()) {
            mView.showError(mContext.getString(R.string.you_cannot_send_messages))
            return true
        }

        return false
    }

    @SuppressLint("CheckResult")
    override fun sendTextMessage(
        msg: String,
        atUids: List<Long>?,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        groupInfoModel: GroupInfoModel?
    ) {
        if (checkShutup()) {
            return
        }

        SendMessageManager.sendTextMessageToGroup(
            msg,
            atUids,
            refMessageBean,
            myUid,
            targetUid,
            groupInfoModel
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
        groupInfoModel: GroupInfoModel?
    ) {
        if (checkShutup()) {
            return
        }

        SendMessageManager.sendVoiceMessageToGroup(
            recordTime,
            File(recordFilePath),
            highDArr.toIntArray(),
            refMessageBean,
            myUid,
            targetUid,
            groupInfoModel
        )
    }

    override fun sendImageMessage(
        imageFilePath: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        groupInfoModel: GroupInfoModel?
    ) {
        if (checkShutup()) {
            return
        }

        SendMessageManager.sendImageMessageToGroup(
            imageFilePath,
            refMessageBean,
            myUid,
            targetUid,
            groupInfoModel
        )
    }

    override fun sendDynamicImageMessage(
        emoticonId: Long,
        imageFilePath: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        groupInfoModel: GroupInfoModel?
    ) {
        if (checkShutup()) {
            return
        }

        SendMessageManager.sendDynamicImageMessageToGroup(
            emoticonId,
            imageFilePath,
            refMessageBean,
            myUid,
            targetUid,
            groupInfoModel
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
        groupInfoModel: GroupInfoModel?
    ) {
        if (checkShutup()) {
            return
        }

        SendMessageManager.sendDynamicImageMessageToGroup(
            emoticonId,
            imageFileUrl,
            width,
            height,
            refMessageBean,
            myUid,
            targetUid,
            groupInfoModel
        )
    }

    @SuppressLint("CheckResult")
    override fun sendVideoMessage(
        videoFilePath: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        groupInfoModel: GroupInfoModel?
    ) {
        if (checkShutup()) {
            return
        }

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
            //val videoThumbFile = File(videoFilePath + "___thumb")
            val videoThumbFile = File(FileUtils.getAPPInternalStorageFilePath(File(videoFilePath)) + "___thumb")
            FileUtils.saveBitmap(firstFrame, videoThumbFile)

            SendMessageManager.sendVideoMessageToGroup(
                File(videoFilePath),
                videoThumbFile,
                width,
                height,
                (duration / 1000).toInt(),
                refMessageBean,
                myUid,
                targetUid,
                groupInfoModel
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
        groupInfoModel: GroupInfoModel?
    ) {
        if (checkShutup()) {
            return
        }

        SendMessageManager.sendNameCardMessageToGroup(
            uid,
            refMessageBean,
            myUid,
            targetUid,
            groupInfoModel
        )
    }

    override fun sendLocationMessage(
        lat: Long,
        lng: Long,
        address: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        groupInfoModel: GroupInfoModel?
    ) {
        if (checkShutup()) {
            return
        }

        SendMessageManager.sendLocationMessageToGroup(
            lat,
            lng,
            address,
            refMessageBean,
            myUid,
            targetUid,
            groupInfoModel
        )
    }

    override fun sendFileMessage(
        filePath: String,
        mimeType: String,
        refMessageBean: MessageModel?,
        myUid: Long,
        targetUid: Long,
        groupInfoModel: GroupInfoModel?
    ) {
        if (checkShutup()) {
            return
        }

        SendMessageManager.sendFileMessageToGroup(
            filePath,
            mimeType,
            refMessageBean,
            myUid,
            targetUid,
            groupInfoModel
        )
    }

    override fun start(isLoadAll: Boolean) {
        ChatsHistoryManager.clearChatAtMeCount(mMineUid, ChatModel.CHAT_TYPE_GROUP, mTargetGid)

        // 查询@me的所有消息，缓存到内存中（查询完成后，这些消息的isAtMe自动置为0）
        MessagesManager.findAtMeMessages(mMineUid, mTargetGid, {
            mAtMeMessageLocalIds.addAll(it)
            MessagesManager.deleteToGroupFireMessage(mMineUid, mTargetGid, {
                if (isLoadAll) {
                    loadAllMessageHistory()
                } else {
                    loadMessageHistory()
                }
            })
        })
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
                    ?.notEqualTo("type", MessageModel.MESSAGE_TYPE_RECALL)
                    ?.notEqualTo("type", MessageModel.MESSAGE_TYPE_RECALL_SUCCESS)
                    ?.sort("time", Sort.DESCENDING)
                    ?.limit(Constant.Common.FIRST_LOAD_MESSAGE_HISTORY_COUNT)?.findAllAsync()

            }
            .observeOn(AndroidSchedulers.mainThread()).subscribe {
                mMessages = it
                mMessages?.addChangeListener(this@GroupChatPresenterImpl)
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
                    ?.notEqualTo("type", MessageModel.MESSAGE_TYPE_RECALL)
                    ?.notEqualTo("type", MessageModel.MESSAGE_TYPE_RECALL_SUCCESS)
                    ?.sort("time")?.findAllAsync()

            }
            .observeOn(AndroidSchedulers.mainThread()).subscribe {
                mMessages = it
                mMessages?.addChangeListener(this@GroupChatPresenterImpl)
                mView.onMessagesLoadComplete(mMessages)
            }
    }

    @SuppressLint("CheckResult")
    private fun setGroupMessageTime() {
        //找出所有阅后即焚的消息，且过期时间还没有时间的
        var realmResult: RealmResults<MessageModel>? = null
        RealmCreator.executeGroupChatTransactionAsync(mMineUid, mTargetGid, { realm ->
            realmResult = realm.where(MessageModel::class.java)
                ?.notEqualTo("type", MessageModel.MESSAGE_TYPE_RECALL)
                ?.notEqualTo("type", MessageModel.MESSAGE_TYPE_RECALL_SUCCESS)
                ?.notEqualTo("snapchatTime", 0L)
                ?.equalTo("expireTime", 0L)
                ?.beginGroup()
                ?.equalTo("isSend", 0L)
                ?.endGroup()
                ?.sort("time")?.findAll()

            realmResult?.let { result ->
                result.forEach { msgModel ->
                    msgModel.expireTime = System.currentTimeMillis() + msgModel.snapchatTime * 1000
                }
            }
            realm.copyToRealm(realmResult)
        }, {
        }, {
        })
    }

    override fun clickAtMeButton() {
        val msgLocalId = mAtMeMessageLocalIds.removeAt(0)
        var msgIndex = -1

        run outside@{
            val size = mMessages?.size ?: 1
            for (index in size - 1 downTo 0) {
                if ((mMessages?.get(index)?.id ?: 0) == msgLocalId) {
                    msgIndex = index
                }
            }
        }

        if (msgIndex >= 0) {
            mView.refreshAtMeButton(mAtMeMessageLocalIds.size)
            mView.scrollToPosition(msgIndex)
        }
    }

    private fun removeFirstScreenAtMe(
        result: RealmResults<MessageModel>,
        msgs: List<Long>,
        complete: (List<Long>) -> Unit
    ) {
        val filterMsgs = ArrayList<Long>()
        val visibleItemCount = mView.getVisibleItemCount() + 1
        val count = result.size
        msgs.forEach {
            val msgLocalId = it
            run outside@{
                val size = count - 1
                for (index in size downTo 0) {
                    if (size > 1000 && index < size - 1000) {
                        // 超出1000不遍历了
                        return@outside
                    }

                    if ((result[index]?.id ?: 0) == msgLocalId) {
                        if (index < size - visibleItemCount) {
                            filterMsgs.add(msgLocalId)
                        }

                        return@outside
                    }
                }
            }
        }

        complete.invoke(filterMsgs)
    }

    override fun setAllMessageReaded() {
        if (mView.isActivityPause()) {
            return
        }

        //所有本地未阅读消息改成已阅，并发送回执
        MessagesManager.setAllGroupMessageReaded(mMineUid, mTargetGid, { c, id ->
            mView.showNewMsgTip(c, id)

            //会话改成已阅
            ChatsHistoryManager.setChatMessagesAllReaded(
                mMineUid,
                ChatModel.CHAT_TYPE_GROUP,
                mTargetGid,
                {
                    EventBus.publishEvent(UnreadMessageEvent())//修改红点数字
                })
        })

        //将群公告设置为已读
        if (mCommonPref.getHasNewGroupNotice()) {
            mCommonPref.putHasNewGroupNotice(false)
        }
    }

    override fun getAddToken(identify: String, userId: Long, findSign: String) {
        ArouterServiceManager.contactService.getAddToken(
            identify, userId, findSign, { userId, addToken ->
                mView.getAddToken(userId, addToken)
            }) { _, msg ->
            mView.showError(msg)
        }
    }

    @SuppressLint("CheckResult")
    override fun onChange(result: RealmResults<MessageModel>) {
        if (!result.isValid)
            return

//        if (!mIsFilterFirstScreenAtMeMessages) {
//            mIsFilterFirstScreenAtMeMessages = true
//            val atMessageLocalIds = ArrayList<Long>(mAtMeMessageLocalIds)
//            removeFirstScreenAtMe(result, atMessageLocalIds) { filterMsgs ->
//                mAtMeMessageLocalIds.clear()
//                mAtMeMessageLocalIds.addAll(filterMsgs)
//                mView.refreshAtMeButton(filterMsgs.size)
//            }
//        }

        try {
            val data = if (isLoadAll) result else result.sort("time", Sort.ASCENDING)

            if (data.isNotEmpty()) {
                val lastMessageTime = data.last()?.time ?: 0
                if (mLastMessageTime < lastMessageTime) {
                    setAllMessageReaded()
                }

                mLastMessageTime = lastMessageTime

                setGroupMessageTime()//当前页面 给数据设置上过期时间
            }

            if (mView.isActive()) {
                mView.onMessagesChanged(data)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun isLoadAll(): Boolean {
        return isLoadAll
    }
}