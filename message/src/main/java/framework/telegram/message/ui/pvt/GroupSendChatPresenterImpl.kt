package framework.telegram.message.ui.pvt

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.text.TextUtils
import com.trello.rxlifecycle3.RxLifecycle
import com.trello.rxlifecycle3.android.ActivityEvent
import com.umeng.analytics.MobclickAgent
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.manager.SendMessageManager
import framework.telegram.message.manager.upload.UploadManager
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.ui.utils.BitmapUtils
import framework.telegram.ui.utils.FileUtils
import framework.telegram.ui.utils.ScreenUtils
import framework.telegram.ui.utils.UriUtils
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.io.File

class GroupSendChatPresenterImpl : GroupSendChatContract.Presenter {

    private val mContext: Context
    private var mView: GroupSendChatContract.View? = null
    private val mViewObservalbe: Observable<ActivityEvent>
    private val mListId = mutableListOf<Long>()
    private val mMineUid by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
    }

    private val mRealm by lazy { ArouterServiceManager.groupService.getContactsRealm() }

    constructor(
        view: GroupSendChatContract.View,
        context: Context,
        observable: Observable<ActivityEvent>,
        idList: List<Long>
    ) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mContext = context
        this.mListId.addAll(idList)
        view.setPresenter(this)
    }

    @SuppressLint("CheckResult")
    override fun sendTextMessage(msg: String) {
        getContactModels { list ->
            val newList = splitList(list, 20)
            var i = 0
            ThreadUtils.runOnIOThread {
                newList.forEach { contactDataModels ->
                    contactDataModels.forEach { contactDataModel ->
                        SendMessageManager.sendTextMessageToUser(
                            msg,
                            null,
                            mMineUid,
                            contactDataModel.uid,
                            contactDataModel,
                            isGroupSend = true
                        )

                        ThreadUtils.sleep(200)
                        ThreadUtils.runOnUIThread {
                            mView?.showProcess(i++, list.size)
                        }
                    }

//                    ThreadUtils.sleep(3000)
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    override fun sendVoiceMessage(recordTime: Int, recordFilePath: String, highDArr: Array<Int>) {
        mView?.showProcess(0, 100)
        SendMessageManager.compressVoice(File(recordFilePath), { mp3File ->
            val chatType = ChatModel.CHAT_TYPE_PVT
            val attachmentKey = SendMessageManager.generateAttachmentKey(chatType, 0, true)
            UploadManager.uploadGroupSendMsg(
                chatType,
                MessageModel.MESSAGE_TYPE_VOICE,
                mMineUid,
                attachmentKey,
                "",
                Uri.fromFile(mp3File).toString(),
                { _, voiceUrl ->
                    getContactModels { list ->
                        SendMessageManager.groupSendVoiceMessage(
                            chatType,
                            attachmentKey,
                            recordTime,
                            voiceUrl,
                            highDArr.toIntArray(),
                            mMineUid,
                            list,
                            { process, _ ->
                                mView?.showProcess(
                                    (list.size - 1).coerceAtMost((list.size * 0.2 + (process + 1) * 0.8).toInt()),
                                    list.size
                                )
                            }) {
                            mView?.showError(
                                it,
                                GroupSendMsgVoice(recordTime, recordFilePath, highDArr)
                            )
                        }
                    }
                }) {
                mView?.showError(
                    BaseApp.app.getString(R.string.string_group_send_upload_fail),
                    GroupSendMsgVoice(recordTime, recordFilePath, highDArr)
                )
            }
        }) { error ->
            mView?.showError(
                BaseApp.app.getString(R.string.string_group_send_upload_fail),
                GroupSendMsgVoice(recordTime, recordFilePath, highDArr)
            )
        }
    }

    override fun sendImageMessage(imageFilePath: String) {
        mView?.showProcess(0, 100)
        SendMessageManager.compressImage(imageFilePath, { resizeImageFile ->
            val chatType = ChatModel.CHAT_TYPE_PVT
            val attachmentKey = SendMessageManager.generateAttachmentKey(chatType, 0, true)
            val maxThumbSize = ScreenUtils.dp2px(BaseApp.app, 240.0f)
            val thumbFileName = FileUtils.getAPPInternalStorageFilePath(resizeImageFile) + "___thumb"
            val resizeImageThumbPath = BitmapUtils.revitionImageSize(
                resizeImageFile.absolutePath,
                thumbFileName,
                maxThumbSize,
                maxThumbSize
            )
            val resizeImageThumbUri =
                if (TextUtils.isEmpty(resizeImageThumbPath)) "" else Uri.fromFile(
                    File(resizeImageThumbPath)
                ).toString()
            val resizeImageFileUri = Uri.fromFile(resizeImageFile).toString()
            val resizeImageSize = BitmapUtils.getImageSize(resizeImageFile.absolutePath)
            UploadManager.uploadGroupSendMsg(
                chatType,
                MessageModel.MESSAGE_TYPE_IMAGE,
                mMineUid,
                attachmentKey,
                resizeImageThumbUri,
                resizeImageFileUri,
                { thumbUrl, imageUrl ->
                    getContactModels { list ->
                        SendMessageManager.groupSendImageMessage(
                            chatType,
                            attachmentKey,
                            imageUrl,
                            thumbUrl,
                            resizeImageSize,
                            mMineUid,
                            list,
                            { process, _ ->
                                mView?.showProcess(
                                    Math.min(
                                        list.size - 1,
                                        (list.size * 0.2 + (process + 1) * 0.8).toInt()
                                    ), list.size
                                )
                            }) {
                            mView?.showError(it, GroupSendMsgImage(imageFilePath))
                        }
                    }
                }) {
                mView?.showError(
                    BaseApp.app.getString(R.string.string_group_send_upload_fail),
                    GroupSendMsgImage(imageFilePath)
                )
            }
        }) {
            mView?.showError(
                BaseApp.app.getString(R.string.string_group_send_upload_fail),
                GroupSendMsgImage(imageFilePath)
            )
        }
    }

    override fun sendDynamicImageMessage(emoticonId: Long, imageFilePath: String) {
        mView?.showProcess(0, 100)

        val chatType = ChatModel.CHAT_TYPE_PVT
        val attachmentKey = SendMessageManager.generateAttachmentKey(chatType, 0, true)
        val imageFile = File(imageFilePath)
        val imageSize = BitmapUtils.getImageSize(imageFile.absolutePath)

        UploadManager.uploadGroupSendMsg(
            chatType,
            MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE,
            mMineUid,
            attachmentKey,
            "",
            Uri.fromFile(File(imageFilePath)).toString(),
            { _, imageUrl ->
                getContactModels { list ->
                    SendMessageManager.groupSendDynamicImageMessage(chatType,
                        attachmentKey,
                        emoticonId,
                        imageUrl,
                        imageSize?.first() ?: 0,
                        imageSize?.last() ?: 0,
                        mMineUid,
                        list,
                        { process, _ ->
                            mView?.showProcess(
                                Math.min(
                                    list.size - 1,
                                    (list.size * 0.2 + (process + 1) * 0.8).toInt()
                                ), list.size
                            )
                        }) {
                        mView?.showError(
                            BaseApp.app.getString(R.string.string_group_send_upload_fail),
                            GroupSendMsgDynamicImage(emoticonId, imageFilePath)
                        )
                    }
                }
            }) {
            mView?.showError(
                BaseApp.app.getString(R.string.string_group_send_upload_fail),
                GroupSendMsgDynamicImage(emoticonId, imageFilePath)
            )
        }
    }

    override fun sendDynamicImageUrlMessage(
        emoticonId: Long,
        imageUrl: String,
        width: Int,
        height: Int
    ) {
        val chatType = ChatModel.CHAT_TYPE_PVT
        val attachmentKey = ""

        getContactModels { list ->
            SendMessageManager.groupSendDynamicImageMessage(
                chatType,
                attachmentKey,
                emoticonId,
                imageUrl,
                width,
                height,
                mMineUid,
                list,
                { process, _ ->
                    mView?.showProcess(
                        Math.min(
                            list.size - 1,
                            (list.size * 0.2 + (process + 1) * 0.8).toInt()
                        ), list.size
                    )
                }) {
                mView?.showError(
                    BaseApp.app.getString(R.string.string_group_send_upload_fail),
                    GroupSendMsgDynamicImage(emoticonId, imageUrl)
                )
            }
        }
    }

    @SuppressLint("CheckResult")
    override fun sendVideoMessage(videoFilePath: String) {
        val mmr = MediaMetadataRetriever()
        try {
            mView?.showProcess(0, 100)

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

            val chatType = ChatModel.CHAT_TYPE_PVT
            val videoFile = File(videoFilePath)
            val attachmentKey = SendMessageManager.generateAttachmentKey(chatType, 0, true)
            UploadManager.uploadGroupSendMsg(chatType,
                MessageModel.MESSAGE_TYPE_VIDEO,
                mMineUid,
                attachmentKey,
                Uri.fromFile(videoThumbFile).toString(),
                Uri.fromFile(videoFile).toString(),
                { thumbImageUrl, videoUrl ->
                    getContactModels { list ->
                        SendMessageManager.groupSendVideoMessage(
                            chatType,
                            videoUrl,
                            thumbImageUrl,
                            attachmentKey,
                            width,
                            height,
                            (duration / 1000).toInt(),
                            mMineUid,
                            list,
                            { process, isFinish ->
                                mView?.showProcess(
                                    Math.min(
                                        list.size - 1,
                                        (list.size * 0.2 + (process + 1) * 0.8).toInt()
                                    ), list.size
                                )
                            }) {
                            mView?.showError(it, GroupSendMsgVideo(videoFilePath))
                        }
                    }
                }) {
                mView?.showError(
                    BaseApp.app.getString(R.string.string_group_send_upload_fail),
                    GroupSendMsgVideo(videoFilePath)
                )
            }
        } catch (ex: Exception) {
            BaseApp.app.toast(mContext.getString(R.string.video_conversion_has_an_exception))
            AppLogcat.logger.e(ex)
            MobclickAgent.reportError(BaseApp.app, ex)
        } finally {
            mmr.release()
        }
    }

    override fun sendFileMessage(filePath: String, mimeType: String) {
        mView?.showProcess(0, 100)
        ThreadUtils.runOnIOThread {
            val chatType = ChatModel.CHAT_TYPE_PVT
            val attachmentKey =
                SendMessageManager.generateAttachmentKey(ChatModel.CHAT_TYPE_PVT, 0, true)
            val fileName = File(filePath).name
            val fileLength = File(filePath).length()
            UploadManager.uploadGroupSendMsg(chatType,
                MessageModel.MESSAGE_TYPE_FILE,
                mMineUid,
                attachmentKey,
                "",
                Uri.fromFile(File(filePath)).toString(),
                { _, fileUri ->
                    getContactModels { list ->
                        SendMessageManager.groupSendFileMessage(
                            chatType,
                            fileName,
                            fileLength,
                            attachmentKey,
                            fileUri,
                            mimeType,
                            mMineUid,
                            list,
                            { process, isFinish ->
                                mView?.showProcess(
                                    Math.min(
                                        list.size - 1,
                                        (list.size * 0.2 + (process + 1) * 0.8).toInt()
                                    ), list.size
                                )
                            }) {
                            mView?.showError(it, GroupSendMsgFile(filePath, mimeType))
                        }
                    }
                }) {
                mView?.showError(
                    BaseApp.app.getString(R.string.string_group_send_upload_fail),
                    GroupSendMsgFile(filePath, mimeType)
                )
            }
        }
    }

    override fun sendNameCardMessage(uid: Long) {
        getContactModels { list ->
            ThreadUtils.runOnIOThread {
                val newList = splitList(list, 20)
                var i = 0
                newList.forEach { contactDataModels ->
                    contactDataModels.forEach { contactDataModel ->
                        SendMessageManager.sendNameCardMessageToUser(
                            uid,
                            null,
                            mMineUid,
                            contactDataModel.uid,
                            contactDataModel,
                            isGroupSend = true
                        )

                        ThreadUtils.sleep(200)
                        ThreadUtils.runOnUIThread {
                            mView?.showProcess(i++, list.size)
                        }
                    }

//                    ThreadUtils.sleep(3000)
                }
            }
        }
    }

    override fun sendLocationMessage(lat: Long, lng: Long, address: String) {
        getContactModels { list ->
            ThreadUtils.runOnIOThread {
                val newList = splitList(list, 20)
                var i = 0
                newList.forEach { contactDataModels ->
                    contactDataModels.forEach {  contactDataModel ->
                        SendMessageManager.sendLocationMessageToUser(
                            lat,
                            lng,
                            address,
                            null,
                            mMineUid,
                            contactDataModel.uid,
                            contactDataModel,
                            isGroupSend = true
                        )

                        ThreadUtils.sleep(200)
                        ThreadUtils.runOnUIThread {
                            mView?.showProcess(i++, list.size)
                        }
                    }

//                    ThreadUtils.sleep(3000)
                }
            }
        }
    }

    override fun start() {
    }

    @SuppressLint("CheckResult")
    private fun getContactModels(callback: ((List<ContactDataModel>) -> Unit)) {
        ThreadUtils.runOnUIThread {
            Flowable.just(mRealm)
                .compose(RxLifecycle.bindUntilEvent(mViewObservalbe, ActivityEvent.DESTROY))
                .subscribeOn(AndroidSchedulers.mainThread())
                .map {
                    val query =
                        it.where(ContactDataModel::class.java)?.equalTo("bfMyContacts", true)
                            ?.beginGroup()
                    mListId.forEachIndexed { index, id ->
                        if (index == mListId.size - 1)
                            query?.equalTo("uid", id)
                        else {
                            query?.equalTo("uid", id)?.or()
                        }
                    }
                    query?.endGroup()?.findAllAsync()
                }
                .observeOn(AndroidSchedulers.mainThread()).subscribe { result ->
                    val list = mutableListOf<ContactDataModel>()
                    result?.forEach {
                        list.add(it.copyContactDataModel())
                    }
                    callback.invoke(list)
                }
        }
    }

    override fun destroy() {
        try {
            mRealm.close()
            mView = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun <T> splitList(list: List<T>, len: Int): List<List<T>> {
        if (list.isEmpty() || len < 1) {
            return arrayListOf(list)
        }

        val result = arrayListOf<List<T>>()
        val size = list.size
        val count = (size + len - 1) / len
        for (i in 0 until count) {
            val subList = list.subList(i * len, if ((i + 1) * len > size) size else len * (i + 1))
            result.add(subList)
        }
        return result
    }
}