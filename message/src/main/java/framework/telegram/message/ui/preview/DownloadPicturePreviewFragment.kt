package framework.telegram.message.ui.preview

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.bumptech.glide.disklrucache.DiskLruCache
import com.bumptech.glide.load.engine.cache.DiskCache
import com.bumptech.glide.load.engine.cache.SafeKeyGenerator
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.EmptySignature
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_QR_ANALYZE
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_FORWARD_CHATS
import framework.telegram.message.bridge.event.ReadAttachmentEvent
import framework.telegram.message.controller.MessageController
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.FileHelper
import framework.telegram.support.tools.SafeBitmapUtils
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.file.DirManager
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.qr.activity.CodeUtils
import framework.telegram.ui.utils.BitmapUtils
import framework.telegram.ui.utils.NavBarUtils
import framework.telegram.ui.utils.UriUtils
import framework.telegram.ui.utils.glide.DataCacheKey
import framework.telegram.ui.widget.scale.ImageSource
import framework.telegram.ui.widget.scale.SubsamplingScaleImageView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.msg_activity_download_picture_preview_new.*
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.concurrent.schedule
import framework.telegram.ui.widget.CommonLoadindView

@Route(path = Constant.ARouter.ROUNTE_MSG_PREVIEW_IMAGE_FRAGMENT)
class DownloadPicturePreviewFragment : LazyFragment() {

    override val fragmentName: String
        get() {
            return "DownloadPicturePreviewFragment"
        }

    private val mChatType by lazy { arguments?.getInt("chatType", -1) ?: -1 }
    private val mTargetId by lazy { arguments?.getLong("targetId", -1) ?: -1 }
    private val mImageFileBackupUri by lazy { arguments?.getString("imageFileBackupUri") }
    private val mImageThumbFileUri by lazy { arguments?.getString("imageThumbFileUri") }
    private val mImageFileUri by lazy { arguments?.getString("imageFileUri") }
    private val mAttachmentKey by lazy { arguments?.getString("attachmentKey") }
    private val mMsgId by lazy { arguments?.getLong("msgId", -1) ?: -1 }
    private val mMessageLocalId by lazy { arguments?.getLong("messageLocalId", -1) ?: -1 }
    private var mExpireTime = 0L
    private val mSnapchatTime by lazy { arguments?.getInt("snapchatTime", 0) ?: 0 }

    private val mPrivate by lazy { arguments?.getBoolean("private", false) }

    private var mImageFile: File? = null

    private var mScaleImageView: SubsamplingScaleImageView? = null

    private var mProgressBar: CommonLoadindView? = null

    private var mTimer: Timer? = null

    private val mOutAnimation = AnimationUtils.loadAnimation(BaseApp.app, R.anim.anim_alpha_out)

    private var isLoadReady = true

    private var mHasSetScaleType = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.msg_activity_download_picture_preview_new, container, false)

        }
        return rootView
    }

    override fun lazyLoad() {
        mExpireTime = arguments?.getLong("expireTime", 0L) ?: 0L

        initView()
        initData()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mScaleImageView?.requestLayout()
        mScaleImageView?.post {
            findMessageModel()
        }
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        mScaleImageView = scale_image_view
        mProgressBar = normal_background_progress
        mScaleImageView?.minScale = 1F//最小显示比例
        mScaleImageView?.maxScale = 10.0F//最大显示比例

        initCountDownTimer()

        mScaleImageView?.setOnClickListener {
            this@DownloadPicturePreviewFragment.activity?.finish()
        }

        mScaleImageView?.setOnLongClickListener {
            if (mPrivate == true || activity == null || context == null)
                return@setOnLongClickListener false
            showDialog()
            return@setOnLongClickListener false
        }

        image_download.setOnClickListener {
            if (mCurVisable) {
                savePicture()
            }
        }

        image_more.setOnClickListener {
            if (mCurVisable) {
                showDialog()
            }
        }

        image_go.setOnClickListener {
            if (mCurVisable) {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_MEDIA_MANAGER)
                        .withInt("chatType", mChatType)
                        .withLong("chaterId", mTargetId)
                        .withInt("curPager", 0)
                        .navigation()
            }
        }

        EventBus.getFlowable(ReadAttachmentEvent::class.java)
                .bindToLifecycle(this@DownloadPicturePreviewFragment)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (activity != null) {
                        if (mTargetId == it.userId && mMsgId == it.msgId) {
                            mExpireTime = it.expireTime
                            startCountDownTimer()
                        }
                    }
                }

        mOutAnimation.startOffset = 3000
        mOutAnimation.fillAfter = true

        if (mPrivate == true) {
            mCurVisable = false
            button_layout.visibility = View.GONE
        } else {
            mCurVisable = true
            this@DownloadPicturePreviewFragment.activity?.let {
                if (NavBarUtils.isNavigationBarShow(it)) {
                    val size = NavBarUtils.getNavigationBarHeight(BaseApp.app)
                    bar_layout.layoutParams.height = size
                }
            }
        }
    }

    private fun initCountDownTimer() {
        if (mPrivate == true && mSnapchatTime > 0) {
            pointer_count_down.visibility = View.VISIBLE
            pointer_count_down.initCountDownText(mSnapchatTime)
        }

        pointer_count_down.setCallback {
            this@DownloadPicturePreviewFragment.activity?.finish()
        }
    }

    private fun startCountDownTimer() {
        if (mPrivate == true && mExpireTime != 0L) {
            mTimer?.cancel()
            mTimer?.purge()
            mTimer = Timer("DownloadPicturePreviewFragment", false)
            mTimer?.schedule(0, 1000) {
                ThreadUtils.runOnUIThread {
                    pointer_count_down.setCurProgress((mExpireTime - System.currentTimeMillis()).toInt())
                }
            }
        }
    }

    private fun initData() {
        findMessageModel()
    }

    private fun findMessageModel() {
        //本地的图
        val localUri = mImageFileBackupUri
        //网络的图
        val netUrl = mImageFileUri
        //解密的key
        val key = if (TextUtils.isEmpty(mAttachmentKey)) null else mAttachmentKey

        mProgressBar?.visibility = View.VISIBLE

        if (!TextUtils.isEmpty(localUri)) {
            Glide.with(this@DownloadPicturePreviewFragment).load(localUri).into(object : SimpleTarget<Drawable>() {
                override fun onResourceReady(p0: Drawable, p1: Transition<in Drawable>?) {
                    val bitmap = BitmapUtils.drawable2Bitmap(p0)
                    setImageViewScaleType(bitmap.height, bitmap.width)
                    mScaleImageView?.setImage(ImageSource.bitmap(bitmap))
                    UriUtils.parseUri(localUri)?.path?.let {
                        mImageFile = File(it)
                    }
                    loadUrlImage(netUrl, key)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    mProgressBar?.clearAnimation()
                    mProgressBar?.visibility = View.GONE
                    isLoadReady = false
                    loadUrlImage(netUrl, key)
                }
            })
        } else {
            isLoadReady = false
            loadUrlImage(netUrl, key)
        }

        // 已阅
        MessageController.sendMsgPlayedReceipt(mChatType, mTargetId, mMessageLocalId) {
            mExpireTime = it
            startCountDownTimer()
        }
    }

    private fun loadUrlImage(netUrl: String?, key: String?) {
        if (!TextUtils.isEmpty(netUrl)) {
            Glide.with(this@DownloadPicturePreviewFragment).load(GlideUrl(netUrl, key)).into(object : SimpleTarget<Drawable>() {
                override fun onResourceReady(p0: Drawable, p1: Transition<in Drawable>?) {
                    val bitmap = BitmapUtils.drawable2Bitmap(p0)
                    setImageViewScaleType(bitmap.height, bitmap.width)
                    mScaleImageView?.setImage(ImageSource.bitmap(bitmap))

                    getCacheFile(mImageFileUri)?.absoluteFile?.let {
                        mImageFile = it
                    }

                    mProgressBar?.clearAnimation()
                    mProgressBar?.visibility = View.GONE
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    mProgressBar?.clearAnimation()
                    mProgressBar?.visibility = View.GONE
                    if (!isLoadReady) {
                        image_view_fail.visibility = View.VISIBLE
                    }
                }
            })
        } else {
            if (!isLoadReady) {
                image_view_fail.visibility = View.VISIBLE
            }
            mProgressBar?.clearAnimation()
            mProgressBar?.visibility = View.GONE

        }
    }


    private fun getCacheFile(url: String?): File? {
        if (TextUtils.isEmpty(url)) {
            return null
        }

        val dataCacheKey = DataCacheKey(GlideUrl(url), EmptySignature.obtain())
        val safeKeyGenerator = SafeKeyGenerator()
        val safeKey = safeKeyGenerator.getSafeKey(dataCacheKey)
        try {
            val cacheSize = 100 * 1000 * 1000L
//            val uuid = if (!AccountManager.hasLoginAccount()) UUID.nameUUIDFromBytes("default".toByteArray()).toString() else AccountManager.getLoginAccountUUid()
            val uuid = UUID.nameUUIDFromBytes("default".toByteArray()).toString()
            val cacheDir = File(DirManager.getImageCacheDir(context!!, uuid), DiskCache.Factory.DEFAULT_DISK_CACHE_DIR)
            val diskLruCache = DiskLruCache.open(cacheDir, 1, 1, cacheSize)
            val value = diskLruCache.get(safeKey)
            if (value != null) {
                return value.getFile(0)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun pauseFragment() {
    }

    override fun onDestroy() {
        super.onDestroy()

        mTimer?.cancel()
        mTimer?.purge()
        mTimer = null

        mProgressBar?.clearAnimation()
        mProgressBar = null
        mScaleImageView = null
    }

    override fun onFragmentVisibleChange(isVisible: Boolean) {
        super.onFragmentVisibleChange(isVisible)
    }

    private fun savePicture() {
        if (mImageFile != null) {
            if (FileHelper.insertImageToGallery(BaseApp.app, mImageFile!!)) {
                BaseApp.app.toast(getString(R.string.save_success))
            } else {
                BaseApp.app.toast(getString(R.string.save_fail))
            }
        } else {
            BaseApp.app.toast(getString(R.string.save_fail))
        }
    }

    private fun showDialog() {
        val list = mutableListOf<String>()
        list.add(getString(R.string.forward))
        list.add(getString(R.string.save_the_image))
        list.add(getString(R.string.recognition_qr_code))
        this@DownloadPicturePreviewFragment.context?.let {
            AppDialog.showBottomListView(it as AppCompatActivity, this@DownloadPicturePreviewFragment, list) { _, index, _ ->
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
                        savePicture()
                    }
                    2 -> {
                        if (mImageFile != null) {
//                            ARouter.getInstance().build(ROUNTE_BUS_QR_ANALYZE)
//                                    .withString("image_path", mImageFile!!.path)
//                                    .navigation(this@DownloadPicturePreviewFragment.context)
                            CodeUtils.analyzeBitmap(mImageFile!!.path, object : CodeUtils.AnalyzeCallback {
                                override fun onAnalyzeSuccess( result: String?) {
                                    ArouterServiceManager.qrService.resultHandler(result
                                            ?: "", this@DownloadPicturePreviewFragment.context, { isShow ->
                                        showLoading(isShow)
                                    }, {
                                        activity?.finish()
                                    }) {
                                        BaseApp.app.toast(it)
                                    }
                                }

                                override fun onAnalyzeFailed() {
                                    BaseApp.app.toast(getString(R.string.identification_of_failure))
                                }
                            })
                        } else {
                            BaseApp.app.toast(getString(R.string.identification_of_failure))
                        }
                    }
                }
            }
        }
    }

    private var mCurVisable = false

    fun setButtonLayout(isVisible: Boolean) {
        if (mPrivate == true) {
            return
        }

        if (mCurVisable == isVisible)
            return
        if (isVisible) {
            button_layout.clearAnimation()
            showAnimation(mOutAnimation)
            button_layout.visibility = View.VISIBLE

        } else {
            button_layout.clearAnimation()
            button_layout.visibility = View.GONE
        }
        mCurVisable = isVisible
    }


    private fun showAnimation(animation: Animation) {
        animation.cancel()
        button_layout.animation = animation
        animation.start()
    }

    private fun setImageViewScaleType(height: Int, width: Int) {
//        Log.i("lzh","setImageViewScaleType  $height  $width")
        if (!mHasSetScaleType) {
            mHasSetScaleType = true
            if (height > 2 * width) {
                mScaleImageView?.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_START)
            } else {
                mScaleImageView?.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
            }
        }
    }

    private var dialog: AppDialog? = null
    private fun showLoading(isShow: Boolean) {
        dialog?.dismiss()
        if (isShow) {
            this@DownloadPicturePreviewFragment.activity?.let {
                dialog = AppDialog.showLoadingView(it as AppCompatActivity, it)
            }

        }
    }
}