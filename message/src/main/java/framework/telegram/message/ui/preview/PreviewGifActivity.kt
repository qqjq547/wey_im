package framework.telegram.message.ui.preview

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.widget.ImageView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.bumptech.glide.disklrucache.DiskLruCache
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.engine.cache.DiskCache
import com.bumptech.glide.load.engine.cache.SafeKeyGenerator
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.EmptySignature
import com.im.domain.pb.UserProto
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener3
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_FORWARD_CHATS
import framework.telegram.message.bridge.event.RecallMessageEvent
import framework.telegram.message.bridge.event.SnapMessageEvent
import framework.telegram.message.controller.MessageController
import framework.telegram.message.event.DynamicFaceUpdateEvent
import framework.telegram.message.http.HttpManager
import framework.telegram.message.http.creator.UserHttpReqCreator
import framework.telegram.message.http.getResult
import framework.telegram.message.http.getResultWithCache
import framework.telegram.message.http.protocol.UserHttpProtocol
import framework.telegram.message.manager.MessagesManager
import framework.telegram.message.manager.upload.UploadManager
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.cache.kotlin.applyCache
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.AESHelper
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.FileHelper
import framework.telegram.support.tools.MD5
import framework.telegram.support.tools.download.DownloadManager
import framework.telegram.support.tools.file.DirManager
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.BitmapUtils
import framework.telegram.ui.utils.FileUtils
import framework.telegram.ui.utils.UriUtils
import framework.telegram.ui.utils.glide.DataCacheKey
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.msg_activity_download_gif_preview.*
import kotlinx.android.synthetic.main.msg_activity_download_gif_preview.custom_toolbar
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.util.*


/**
 * Created by lzh on 19-8-26.
 * INFO:gif的详情页
 */
@Route(path = Constant.ARouter.ROUNTE_MSG_PREVIEW_GIF_ACTIVITY)
class PreviewGifActivity : BaseActivity() {

    /**
     * 必填项
     */
    private val mImageFileUri by lazy { intent?.getStringExtra("imageFileUri") }

    private val mImageFileBackupUri by lazy { intent?.getStringExtra("imageFileBackupUri") }

    /**
     * 选填（当预览的是一条消息时）
     */
    private val mMessageLocalId by lazy { intent.getLongExtra("messageLocalId", -1) }

    private val mChatType by lazy { intent.getIntExtra("chatType", -1) }

    private val mTargetId by lazy { intent.getLongExtra("targetId", -1) }

    private val mAttachmentKey by lazy { intent?.getStringExtra("attachmentKey") }

    private val mMsgId by lazy { intent?.getLongExtra("msgId", -1) ?: -1 }

    private var mImageFile: File? = null

    // 是否阅后即焚消息
    private val mPrivate by lazy { intent?.getBooleanExtra("private", false) }

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.msg_activity_download_gif_preview)
        initView()

        if (mMessageLocalId > 0) {
            if (mPrivate != true) {
                custom_toolbar.showRightImageView(R.drawable.common_icon_black_more, {
                    showDialog()
                })

                scale_image_view2?.setOnLongClickListener {
                    showDialog()
                    return@setOnLongClickListener false
                }
            }

            bindEvents()
        } else {
            if (TextUtils.isEmpty(mImageFileUri) && !TextUtils.isEmpty(mImageFileBackupUri)) {
                custom_toolbar.showRightTextView(getString(R.string.accomplish), {
                    addToEmotion()
                })
            }
        }
    }

    private fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        scale_image_view2.minimumScale = 0.5f
        scale_image_view2.scaleType = ImageView.ScaleType.CENTER_INSIDE

        if (!TextUtils.isEmpty(mImageFileBackupUri)) {
            Glide.with(this@PreviewGifActivity).load(mImageFileBackupUri).addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: Target<Drawable>?, p3: Boolean): Boolean {
                    return false
                }

                override fun onResourceReady(p0: Drawable?, p1: Any?, p2: Target<Drawable>?, p3: DataSource?, p4: Boolean): Boolean {
                    UriUtils.parseUri(mImageFileBackupUri)?.path?.let {
                        mImageFile = File(it)
                    }

                    return false
                }

            }).into(scale_image_view2)
        } else {
            Glide.with(scale_image_view2).load(GlideUrl(mImageFileUri, mAttachmentKey)).addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: Target<Drawable>?, p3: Boolean): Boolean {
                    return false
                }

                override fun onResourceReady(p0: Drawable?, p1: Any?, p2: Target<Drawable>?, p3: DataSource?, p4: Boolean): Boolean {
                    getCacheFile(mImageFileUri)?.absoluteFile?.let {
                        mImageFile = it
                    }

                    return false
                }

            }).into(scale_image_view2)
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
            val cacheDir = File(DirManager.getImageCacheDir(this, uuid), DiskCache.Factory.DEFAULT_DISK_CACHE_DIR)
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

    private fun addToEmotion() {
        // 本地图
        val filePath = UriUtils.parseUri(mImageFileBackupUri).path
        if (filePath != null) {
            val imageSize = BitmapUtils.getImageSize(filePath)
            showLoading()
            UploadManager.uploadFile(filePath, { url ->
                addEmoticon(0, url, imageSize?.first() ?: 0, imageSize?.last()
                        ?: 0, true)
            }, {
                // 上传失败
                dismissLoading()
            })
        } else {
            // 上传失败
            dismissLoading()
        }
    }

    private var dialog: AppDialog? = null
    private fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@PreviewGifActivity, this@PreviewGifActivity)
    }

    private fun dismissLoading() {
        dialog?.dismiss()
    }

    private fun showDialog() {
        MessageController.executeChatTransactionAsyncWithResult(mChatType, mMineUid, mTargetId, {
            it.where(MessageModel::class.java)?.equalTo("id", mMessageLocalId)?.findFirst()?.copyMessage()
        }, { model ->
            if (model != null) {
                val emoticonId = model.dynamicImageMessageBean?.emoticonId ?: 0
                val emoticonUrl = model.dynamicImageMessageBean?.imageFileUri ?: ""
                val width = model.dynamicImageMessageBean?.width ?: 0
                val height = model.dynamicImageMessageBean?.height ?: 0
                val attachmentKey = model.attachmentKey

                HttpManager.getStore(UserHttpProtocol::class.java)
                        .getEmoticon(object : HttpReq<UserProto.GetEmoticonReq>() {
                            override fun getData(): UserProto.GetEmoticonReq {
                                return UserHttpReqCreator.getEmoticon()
                            }
                        })
                        .applyCache("${mMineUid}_dynamic_face_cache", framework.telegram.support.system.cache.stategy.CacheStrategy.firstCache())
                        .getResultWithCache(lifecycle(), {
                            val emoticons = it.data.emoticonsList
                            var isFinded = false
                            emoticons.forEach { emoticon ->
                                if (emoticon.emoticonId == emoticonId) {
                                    isFinded = true
                                }
                            }

                            if (!isFinded && !TextUtils.isEmpty(emoticonUrl)) {
                                showDialog2(true, emoticonId, emoticonUrl, width, height, attachmentKey)
                            } else {
                                showDialog2(false)
                            }
                        }, {
                            showDialog2(false)
                        })
            } else {
                showDialog2(false)
            }
        })
    }

    private fun showDialog2(showSave: Boolean, emoticonId: Long = 0, emoticonUrl: String = "", width: Int = 0, height: Int = 0, attachmentKey: String = "") {
        if (hasWindowFocus()) {
            val list = mutableListOf<String>()
            if (showSave) {
                list.add(getString(R.string.string_dynamic_face_manager_add))
            }
            list.add(getString(R.string.forward))
            list.add(getString(R.string.string_save_to_album))

            AppDialog.showBottomListView(this@PreviewGifActivity, this@PreviewGifActivity, list) { _, index, text ->
                when (text) {
                    getString(R.string.string_dynamic_face_manager_add) -> {
                        showLoading()
                        addEmoticon(emoticonId, emoticonUrl, width, height, attachmentKey = attachmentKey)
                    }
                    getString(R.string.forward) -> {
                        forwardMsg()
                    }
                    getString(R.string.string_save_to_album) -> {
                        savePicture()
                    }
                }
            }
        }
    }

    private fun savePicture() {
        if (mImageFile != null) {
            if (FileHelper.insertImageToGallery(BaseApp.app, mImageFile!!, "image/gif", "gif")) {
                BaseApp.app.toast(getString(R.string.save_success))
            } else {
                BaseApp.app.toast(getString(R.string.save_fail))
            }
        } else {
            BaseApp.app.toast(getString(R.string.save_fail))
        }
    }

    private fun addEmoticon(emoticonId: Long = 0, emoticonUrl: String = "", width: Int = 0, height: Int = 0, autoFinish: Boolean = false, attachmentKey: String = "") {
        val saveFile = File(DirManager.getImageCacheDir(BaseApp.app, AccountManager.getLoginAccountUUid()), "${MD5.md5(emoticonUrl)}.gif")
        val tmpCacheFile = File(saveFile.absolutePath + "___download")
        downloadFile(attachmentKey, UriUtils.parseUri(emoticonUrl), tmpCacheFile) { filePath ->
            UploadManager.uploadFile(filePath, { url ->
                // 上传成功
                FileUtils.deleteQuietly(saveFile)
                addEmoticon(emoticonId, url, width, height, autoFinish)
            }, {
                // 上传失败
                FileUtils.deleteQuietly(saveFile)
                dismissLoading()
            })
        }
    }

    private fun addEmoticon(emoticonId: Long = 0, emoticonUrl: String = "", width: Int = 0, height: Int = 0, autoFinish: Boolean = false) {
        HttpManager.getStore(UserHttpProtocol::class.java)
                .addEmoticon(object : HttpReq<UserProto.AddEmoticonReq>() {
                    override fun getData(): UserProto.AddEmoticonReq {
                        return UserHttpReqCreator.addEmoticon(emoticonId, emoticonUrl, width, height)
                    }
                })
                .getResult(lifecycle(), { resp ->
                    dismissLoading()

                    toast(getString(R.string.successfully_added))

                    // 成功上传
                    MessagesManager.executeChatTransactionAsync(mChatType, mMineUid, mTargetId, { realm ->
                        val model = realm.where(MessageModel::class.java)?.equalTo("id", mMessageLocalId)?.findFirst()
                        model?.let {
                            val dynamicFace = model.dynamicImageMessageBean
                            dynamicFace.emoticonId = resp.emoticonId
                            model.dynamicImageMessageBean = dynamicFace
                            realm.copyToRealmOrUpdate(model)
                        }
                    })

                    HttpManager.getStore(UserHttpProtocol::class.java)
                            .getEmoticon(object : HttpReq<UserProto.GetEmoticonReq>() {
                                override fun getData(): UserProto.GetEmoticonReq {
                                    return UserHttpReqCreator.getEmoticon()
                                }
                            })
                            .applyCache("${mMineUid}_dynamic_face_cache", framework.telegram.support.system.cache.stategy.CacheStrategy.firstRemote())
                            .getResultWithCache(null, {
                                EventBus.publishEvent(DynamicFaceUpdateEvent())
                            }, {
                            })

                    if (autoFinish) {
                        finish()
                    }
                }, {
                    dismissLoading()
                    toast(getString(R.string.string_dynamic_face_upload_fail))
                })
    }

    private fun forwardMsg() {
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

    @SuppressLint("CheckResult")
    private fun bindEvents() {
        EventBus.getFlowable(RecallMessageEvent::class.java)
                .bindToLifecycle(this@PreviewGifActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { recallMsg ->
                    if (mTargetId == recallMsg.targetId && recallMsg.msgId == mMsgId) {
                        AppDialog.show(this@PreviewGifActivity, this@PreviewGifActivity) {
                            title(text = getString(R.string.hint))
                            message(text = getString(R.string.the_message_you_are_viewing_has_been_deleted))
                            cancelOnTouchOutside(false)
                            cancelable(false)

                            positiveButton(text = getString(R.string.confirm), click = {
                                finish()
                            })
                        }
                    }
                }


        EventBus.getFlowable(RecallMessageEvent::class.java)
                .bindToLifecycle(this@PreviewGifActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { recallMsg ->
                    if (mTargetId == recallMsg.targetId && recallMsg.msgId == mMsgId) {
                        AppDialog.show(this@PreviewGifActivity, this@PreviewGifActivity) {
                            title(text = getString(R.string.hint))
                            message(text = getString(R.string.the_message_you_are_viewing_has_been_deleted))
                            cancelOnTouchOutside(false)
                            cancelable(false)

                            positiveButton(text = getString(R.string.confirm), click = {
                                finish()
                            })
                        }
                    }
                }

        EventBus.getFlowable(SnapMessageEvent::class.java)
                .bindToLifecycle(this@PreviewGifActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.msgId == mMsgId) {
                        finish()
                    }
                }
    }

    private fun downloadFile(secretKey: String?, downloadUri: Uri, saveFile: File, complete: (String) -> Unit) {
        val downloadUrl = downloadUri.toString()
        DownloadManager.download(downloadKey = "$downloadUri", downloadUrl = downloadUrl, targetFile = saveFile, downloadListener = object : DownloadListener3() {
            override fun warn(task: DownloadTask) {

            }

            override fun connected(task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long) {

            }

            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {

            }

            override fun started(task: DownloadTask) {

            }

            override fun error(task: DownloadTask, e: Exception) {
                dismissLoading()
            }

            override fun canceled(task: DownloadTask) {

            }

            override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {

            }

            override fun completed(task: DownloadTask) {
                val file = task.file
                file?.let {
                    val realFile = File(file.absolutePath.replace("___download", ""))
                    file.renameTo(realFile)

                    if (TextUtils.isEmpty(secretKey)) {
                        // 下载并解密完成
                        decryptComplete(realFile)
                    } else {
                        // 重命名下载的文件
                        val encryptFile = File("${realFile.absolutePath}___encrypt")
                        realFile.renameTo(encryptFile)

                        try {
                            // 解密下载的文件，并使用原下载路径作为解密文件的路径
                            val result = AESHelper.decryptFile(secretKey, encryptFile.absolutePath, realFile.absolutePath, null)
                            if (result) {
                                // 删除加密文件
                                FileUtils.deleteQuietly(encryptFile)

                                // 下载并解密完成
                                decryptComplete(realFile)
                            } else {
                                // 删除加密文件
                                FileUtils.deleteQuietly(encryptFile)
                            }
                        } catch (e: Exception) {
                            // 删除加密文件
                            FileUtils.deleteQuietly(encryptFile)
                        }
                    }
                }
            }

            private fun decryptComplete(file: File?) {
                complete.invoke(file?.absolutePath ?: "")
            }
        })
    }

}