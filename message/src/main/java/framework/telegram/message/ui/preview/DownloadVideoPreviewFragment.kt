package framework.telegram.message.ui.preview

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_FORWARD_CHATS
import framework.telegram.message.bridge.event.ReadAttachmentEvent
import framework.telegram.message.controller.DownloadAttachmentController
import framework.telegram.message.controller.MessageController
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.FileHelper
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.utils.NavBarUtils
import framework.telegram.ui.videoplayer.GSYVideoManager
import framework.telegram.ui.videoplayer.video.StandardGSYVideoPlayer
import framework.telegram.ui.widget.CommonLoadindView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.msg_activity_download_video_preview_new.*
import java.io.File
import java.util.*
import kotlin.concurrent.schedule

@Route(path = Constant.ARouter.ROUNTE_MSG_PREVIEW_VIDEO_FRAGMENT)
class DownloadVideoPreviewFragment : LazyFragment(),
    DownloadAttachmentController.DownloadAttachmentListener {


    override val fragmentName: String
        get() {
            return "DownloadVideoPreviewFragment"
        }

    private val mMineUid by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
    }

    private val mMessageLocalId by lazy { arguments?.getLong("messageLocalId", -1) ?: -1L }

    private val mMsgId by lazy { arguments?.getLong("msgId", -1) ?: -1L }

    private val mChatType by lazy { arguments?.getInt("chatType", -1) ?: -1 }

    private val mTargetId by lazy { arguments?.getLong("targetId", -1) ?: -1L }

    private val mAttachmentKey by lazy { arguments?.getString("attachmentKey") }

    private val mVideoFileBackupUri by lazy { arguments?.getString("videoFileBackupUri") }

    private val mVideoFileUri by lazy { arguments?.getString("videoFileUri") }

    private val mVideoThumbFileBackupUri by lazy { arguments?.getString("videoThumbFileBackupUri") }

    private val mThumbFileUri by lazy { arguments?.getString("videoThumbFileUri") }

    private var mExpireTime = 0L
    private val mSnapchatTime by lazy { arguments?.getInt("snapchatTime", 0) }

    private val mIsSilentPlay by lazy { arguments?.getBoolean("isSilentPlay", false) }

    private var mTimer: Timer? = null

    private var mDownLoadFile: File? = null

    private var mPlayer: StandardGSYVideoPlayer? = null

    private var mProgressBar: CommonLoadindView? = null

    private val mPrivate by lazy { arguments?.getBoolean("private", false) }

    private var mLastVolume = 0//设置静音前的音量，页面退出的时候，把音量设置回去

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            rootView =
                inflater.inflate(R.layout.msg_activity_download_video_preview_new, container, false)

        }
        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        DownloadAttachmentController.attachDownloadListener(this@DownloadVideoPreviewFragment)
    }

    @SuppressLint("CheckResult")
    override fun lazyLoad() {
        Log.i("lzh", "lazyLoad")
        mPlayer = download_video_view
        mProgressBar = normal_background_progress

        mExpireTime = arguments?.getLong("expireTime", -1L) ?: 0L
        initCountDownTimer()
        showThumbImage()
        findMessageModel()

        mPlayer?.container?.setOnLongClickListener(longClickListener)
        mPlayer?.setVisiableMoreView(mPrivate != true)
        mPlayer?.setVideoClick(object : StandardGSYVideoPlayer.IVideoClick {
            override fun onGoClick() {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_MEDIA_MANAGER)
                    .withInt("chatType", mChatType)
                    .withLong("chaterId", mTargetId)
                    .withInt("curPager", 0)
                    .navigation()
            }

            override fun onCloseClick() {
                activity?.finish()
            }

            override fun onMoreClick() {
                showDialog()
            }
        })
        //是否静音播放
        if (mIsSilentPlay == true) {
            mLastVolume = mPlayer?.setVolumeState(0) ?: 0
        }

        EventBus.getFlowable(ReadAttachmentEvent::class.java)
            .bindToLifecycle(this@DownloadVideoPreviewFragment)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (activity != null) {
                    if (mTargetId == it.userId && mMsgId == it.msgId) {
                        mExpireTime = it.expireTime
                        startCountDownTimer()
                    }
                }
            }

        this@DownloadVideoPreviewFragment.activity?.let {
            if (NavBarUtils.isNavigationBarShow(it)) {
                val size = NavBarUtils.getNavigationBarHeight(BaseApp.app)
                mPlayer?.barLayout?.layoutParams?.height = size
            }
        }
    }

    private fun initCountDownTimer() {
        if (mPrivate == true && mSnapchatTime ?: 0 > 0) {
            pointer_count_down.visibility = View.VISIBLE
            pointer_count_down.initCountDownText((mSnapchatTime ?: 0))
        }

        pointer_count_down.setCallback {
            this@DownloadVideoPreviewFragment.activity?.finish()
        }
    }

    private fun startCountDownTimer() {
        if (mPrivate == true && mExpireTime != 0L) {
            mTimer?.cancel()
            mTimer?.purge()
            mTimer = Timer("DownloadVideoPreviewFragment", false)
            mTimer?.schedule(0, 1000) {
                ThreadUtils.runOnUIThread {
                    pointer_count_down.setCurProgress((mExpireTime - System.currentTimeMillis()).toInt())
                }
            }
        }
    }

    private val longClickListener: View.OnLongClickListener = View.OnLongClickListener {
        if (mPrivate == true)
            return@OnLongClickListener true
        showDialog()
        return@OnLongClickListener true
    }

    private fun findMessageModel() {
        MessageController.executeChatTransactionAsyncWithResult(
            mChatType,
            mMineUid,
            mTargetId,
            { realm ->
                val msg =
                    realm.where(MessageModel::class.java).equalTo("id", mMessageLocalId).findFirst()
                msg?.copyMessage()
            },
            {
                if (it != null) {
                    val cacheFileUri = mVideoFileBackupUri
                    val downloadUrl = mVideoFileUri
                    val cacheFile =
                        DownloadAttachmentController.hasCacheFile(cacheFileUri, downloadUrl, it)
                    if (cacheFile != null) {
                        mDownLoadFile = cacheFile
                        play(cacheFile)
                    } else {
                        if (!DownloadAttachmentController.isDownloading(mChatType, it)) {
                            DownloadAttachmentController.downloadAttachment(mChatType, it)
                        }
                    }
                }
            })
    }

    private fun showThumbImage() {
        val imageView = AppImageView(this@DownloadVideoPreviewFragment.context)
        imageView.let {
            val drawable = if (!TextUtils.isEmpty(mVideoThumbFileBackupUri)) {
                val file = File(Uri.parse(mVideoThumbFileBackupUri).path)
                if (file.exists()) {
                    Drawable.createFromStream(file.inputStream(), mVideoThumbFileBackupUri)
                } else {
                    null
                }
            } else null
            if (!TextUtils.isEmpty(mThumbFileUri)) {
                Glide.with(it)
                    .load(GlideUrl(mThumbFileUri, mAttachmentKey))
                    .placeholder(drawable).into(imageView)
            }
        }

        mPlayer?.thumbImageView = imageView
    }

    private fun play(cacheFile: File) {
        mPlayer?.visibility = View.VISIBLE

        //是否可以滑动调整
        mPlayer?.setIsTouchWiget(true)
        //隐藏全屏键
        mPlayer?.fullscreenButton?.visibility = View.GONE
        //隐藏返回按键
        mPlayer?.backButton?.visibility = View.GONE

        mPlayer?.setUp(Uri.fromFile(cacheFile).toString(), false, cacheFile, "")
        if (isFragmentVisible) {
            mPlayer?.startPlayLogic()

            // 已阅
            MessageController.sendMsgPlayedReceipt(mChatType, mTargetId, mMessageLocalId) {
                mExpireTime = it
                startCountDownTimer()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mPlayer?.onVideoPause()
    }

    override fun onResume() {
        super.onResume()
        mPlayer?.onVideoResume()
    }

    override fun downloadStart(
        downloadUrl: String,
        chatType: Int,
        targetId: Long,
        msgLocalId: Long
    ) {
    }

    override fun downloadProgress(
        downloadUrl: String,
        chatType: Int,
        targetId: Long,
        msgLocalId: Long,
        percent: Double,
        currentOffset: Long,
        totalLength: Long
    ) {
        if (activity != null && mChatType == chatType && mTargetId == targetId && mMessageLocalId == msgLocalId) {
            mProgressBar?.visibility = View.VISIBLE
        }
    }

    override fun downloadCancel(
        downloadUrl: String,
        chatType: Int,
        targetId: Long,
        msgLocalId: Long
    ) {
    }

    override fun downloadComplete(
        downloadUrl: String,
        chatType: Int,
        targetId: Long,
        msgLocalId: Long,
        file: File
    ) {
        if (activity != null && mChatType == chatType && mTargetId == targetId && mMessageLocalId == msgLocalId) {
            mProgressBar?.visibility = View.GONE
            mProgressBar?.clearAnimation()
            mDownLoadFile = file
            image_view_fail.visibility = View.GONE
            play(file)
        }
    }

    override fun downloadFail(
        downloadUrl: String,
        chatType: Int,
        targetId: Long,
        msgLocalId: Long
    ) {
        if (activity != null && mChatType == chatType && mTargetId == targetId && mMessageLocalId == msgLocalId) {
            BaseApp.app.toast(getString(R.string.video_failed_to_load))
            image_view_fail.visibility = View.VISIBLE
        }
    }

    override fun onFragmentVisibleChange(isVisible: Boolean) {
        super.onFragmentVisibleChange(isVisible)

        if (!isVisible) {
            mPlayer?.onVideoPause()
        }
    }

    override fun pauseFragment() {
        mPlayer?.onVideoPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mIsSilentPlay == true && mLastVolume != 0) {//如果是静音模式进来
            mPlayer?.setVolumeState(mLastVolume)
        }

        GSYVideoManager.releaseAllVideos()

        mPlayer?.setVideoAllCallBack(null)

        mTimer?.cancel()
        mTimer?.purge()
        mTimer = null

        DownloadAttachmentController.detachDownloadListener(this@DownloadVideoPreviewFragment)
        mProgressBar?.clearAnimation()
        mProgressBar = null
    }

    private var isShow = false

    fun setButtonLayout(isVisible: Boolean) {
        if (isShow == isVisible)
            return
        isShow = isVisible
        mPlayer?.showCompleteUi(isVisible)
    }

    private fun showDialog() {
        val list = mutableListOf<String>()
        list.add(getString(R.string.forward))
        list.add(getString(R.string.save_the_video))
        this@DownloadVideoPreviewFragment.context?.let {
            AppDialog.showBottomListView(
                it as AppCompatActivity,
                this@DownloadVideoPreviewFragment,
                list
            ) { _, index, _ ->
                when (index) {
                    0 -> {
                        if (mChatType == ChatModel.CHAT_TYPE_PVT) {
                            ARouter.getInstance().build(ROUNTE_MSG_FORWARD_CHATS)
                                .withLong("messageLocalId", mMessageLocalId)
                                .withInt("chatType", ChatModel.CHAT_TYPE_PVT)
                                .withLong("targetId", mTargetId)
                                .navigation()
                        } else {
                            ARouter.getInstance().build(ROUNTE_MSG_FORWARD_CHATS)
                                .withLong("messageLocalId", mMessageLocalId)
                                .withInt("chatType", ChatModel.CHAT_TYPE_GROUP)
                                .withLong("targetId", mTargetId)
                                .navigation()
                        }
                    }
                    1 -> {
                        if (mDownLoadFile != null) {
                            FileHelper.insertVideoToGallery(it, mDownLoadFile!!)
                        } else {
                            BaseApp.app.toast(getString(R.string.download_to_save))
                        }
                    }
                }
            }
        }
    }
}

