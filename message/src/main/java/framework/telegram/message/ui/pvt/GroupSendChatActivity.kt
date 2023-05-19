package framework.telegram.message.ui.pvt

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.text.format.Formatter
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.alibaba.android.arouter.core.LogisticsCenter
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.RoundingParams
import com.im.domain.pb.UserProto
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.business.bridge.bean.SelectedUsersModel
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_PRIVATE_SEND_CHAT_ACTIVITY
import framework.telegram.message.bridge.Constant.TargetId.GROUP_SEND_ID
import framework.telegram.message.bridge.event.MainToChatMessageEvent
import framework.telegram.message.controller.UploadAttachmentController
import framework.telegram.message.event.DynamicFaceUpdateEvent
import framework.telegram.message.http.HttpManager
import framework.telegram.message.http.creator.UserHttpReqCreator
import framework.telegram.message.http.getResultWithCache
import framework.telegram.message.http.protocol.UserHttpProtocol
import framework.telegram.message.ui.AndroidBug5497Workaround
import framework.telegram.message.ui.location.ChoiceLocationActivity
import framework.telegram.message.ui.location.bean.POIBean
import framework.telegram.message.ui.telephone.core.RtcEngineHolder
import framework.telegram.message.ui.widget.MessageInputView
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.cache.kotlin.applyCache
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.BitUtils
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.Helper
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.file.DirManager
import framework.telegram.ui.cameraview.activity.CameraActivity
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.emoji.IconFacePopup
import framework.telegram.ui.emoji.ToolsPopup
import framework.telegram.ui.face.IconFaceItem
import framework.telegram.ui.face.IconFacePageView
import framework.telegram.ui.face.dynamic.DynamicFaceBean
import framework.telegram.ui.filepicker.config.FilePickerManager
import framework.telegram.ui.imagepicker.ImagePicker
import framework.telegram.ui.imagepicker.MimeType
import framework.telegram.ui.imagepicker.engine.impl.GlideEngine
import framework.telegram.ui.imagepicker.filter.Mp4SizeFilter
import framework.telegram.ui.utils.KeyboardktUtils
import framework.telegram.ui.utils.ScreenUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.msg_activity_group_send_pvt_chat.*
import kotlinx.android.synthetic.main.msg_activity_group_send_pvt_chat.custom_toolbar
import kotlinx.android.synthetic.main.msg_activity_group_send_pvt_chat.message_input_view
import kotlinx.android.synthetic.main.msg_activity_group_send_pvt_chat.root_view_private_chat
import java.io.File
import java.util.*

@Route(path = ROUNTE_MSG_PRIVATE_SEND_CHAT_ACTIVITY)
class GroupSendChatActivity : BaseActivity(), GroupSendChatContract.View,
    UploadAttachmentController.UploadAttachmentListener {

    companion object {
        internal const val TAG = "GroupSendChatActivity"
        internal const val GET_PERMISSIONS_REQUEST_CODE = 0x100
        internal const val TOOL_IMAGEPICKER_REQUESTCODE = 0x1000
        internal const val TOOL_CAMERAVIEW_REQUESTCODE = 0x2000
        internal const val TOOL_NAMECARD_REQUESTCODE = 0x3000
    }

    private var mPresenter: GroupSendChatPresenterImpl? = null
    private var mEmojiPopup: IconFacePopup? = null
    private var mToolsPopup: ToolsPopup? = null

    private val mMineUid by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
    }

    private var mDialog: AppDialog? = null
    private var mProgressBar: ProgressBar? = null
    private var mProgressName: TextView? = null
    private var mProgressSize: TextView? = null
    private var mSureText: TextView? = null
    private var mCancelText: TextView? = null

    private var mReSendData: GroupSendMsg? = null

    private val mAccountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.msg_activity_group_send_pvt_chat)
        AndroidBug5497Workaround.assistActivity(this@GroupSendChatActivity)

        initView()
        initData()

        EventBus.getFlowable(DynamicFaceUpdateEvent::class.java)
            .bindToLifecycle(this@GroupSendChatActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                updateDynamicFace()
            }

        UploadAttachmentController.attachUploadListener(this@GroupSendChatActivity)
    }

    private fun initData() {
        val idList = mutableListOf<Long>()
        framework.telegram.business.bridge.Constant.TmpData.groupSendContactList.forEach {
            idList.add(it.uid)
        }
        GroupSendChatPresenterImpl(
            this@GroupSendChatActivity,
            this@GroupSendChatActivity,
            lifecycle(),
            idList
        ).start()

        var names = ""
        val size = framework.telegram.business.bridge.Constant.TmpData.groupSendContactList.size
        framework.telegram.business.bridge.Constant.TmpData.groupSendContactList.forEachIndexed { index, result ->
            names += if ((size - 1) == index) {
                result.name
            } else {
                result.name + ","
            }
        }
        text_name.text = names

        text_number.text = String.format(getString(R.string.string_group_send_user_size), size)
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as GroupSendChatPresenterImpl
    }

    private fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back, { imageView ->
            val size = ScreenUtils.dp2px(this@GroupSendChatActivity, 2f)
            imageView.setPadding(0, 0, size, 0)
        }) {
            finish()
        }


        custom_toolbar.showLeftTextView(getString(R.string.string_group_send_title)) {
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        }

        custom_toolbar.showLeftImageView(rid = R.drawable.msg_icon_group_send, listen = {
            val roundingParams = RoundingParams.fromCornersRadius(5f)
            roundingParams.roundAsCircle = true
            it.hierarchy.roundingParams = roundingParams
            it.hierarchy.actualImageScaleType = ScalingUtils.ScaleType.FOCUS_CROP
            it.hierarchy.setPlaceholderImage(R.drawable.common_holder_one_user)
            it.hierarchy.setBackgroundImage(
                ColorDrawable(
                    ContextCompat.getColor(
                        this@GroupSendChatActivity,
                        R.color.edeff2
                    )
                )
            )
            it.hierarchy.fadeDuration = 300
            (it.layoutParams as LinearLayout.LayoutParams).setMargins(
                ScreenUtils.dp2px(this@GroupSendChatActivity, 16f),
                0,
                ScreenUtils.dp2px(this@GroupSendChatActivity, 8f),
                0
            )
        })

        // set buttons
        message_input_view.inputView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                mEmojiPopup?.dismiss()
                mToolsPopup?.dismiss()
            }
            false
        }

        message_input_view.buttonsListener = object : MessageInputView.ButtonsListener {

            override fun onClickSend(uids: List<Long>, msg: String?) {
                if (!TextUtils.isEmpty(msg)) {
                    mPresenter?.sendTextMessage(msg ?: "")
                }
            }

            override fun onClickFace() {
                mEmojiPopup?.toggle()
                message_input_view.postDelayed({
                    mToolsPopup?.dismiss()
                }, 200)
            }

            override fun onClickTools() {
                mToolsPopup?.toggle()
                message_input_view.postDelayed({
                    mEmojiPopup?.dismiss()
                }, 200)
            }

            override fun onClickFire() {
                if (message_input_view.isRecording) {
                    return
                }

                mToolsPopup?.let {
                    if (it.isShowing) {
                        it.dismiss()
                    }
                }
                KeyboardktUtils.hideKeyboard(layout)
            }
        }

        message_input_view.recordingListener = object : MessageInputView.RecordingListener {
            override fun checkRecordable(): Boolean {
                return if (RtcEngineHolder.isActive()) {
                    if (RtcEngineHolder.streamType == 0) {
                        toast(getString(R.string.calling_audio_busy_tip))
                    } else {
                        toast(getString(R.string.calling_video_busy_tip))
                    }
                    false
                } else {
                    true
                }
            }

            override fun onRecordingStarted() {
            }

            override fun onRecordingLocked() {

            }

            override fun onRecordingCompleted(
                recordTime: Long,
                recordFilePath: String?,
                highDArr: Array<out Int>
            ) {
                if (!TextUtils.isEmpty(recordFilePath)) {
                    mPresenter?.sendVoiceMessage(
                        (recordTime / 1000L).toInt(),
                        recordFilePath!!,
                        highDArr.toIntArray().toTypedArray()
                    )
                }
            }

            override fun onRecordingCanceled() {

            }

            override fun onRecordSoShort() {

            }

            override fun onPlayAudioDraft() {
            }

            override fun getDraftStorageName(): String {
                return ""
            }

            override fun onInputHeightChange(height: Int) {

            }

            override fun onInputing() {
            }
        }

        // setup emoji and tools
        layout.post {
            setUpToolsPopup()
            setUpEmojiPopup()
        }

        message_input_view.setKeyboardSend(
            !BitUtils.checkBitValue(
                Helper.int2Bytes(mAccountInfo.getPrivacy())[1],
                3
            )
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requestCode == GET_PERMISSIONS_REQUEST_CODE) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.no_permissions_were_obtained),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }

        if (requestCode == TOOL_IMAGEPICKER_REQUESTCODE && resultCode == Activity.RESULT_OK) {
            val paths = ImagePicker.obtainInfoResult(data)
            if (paths != null && paths.isNotEmpty()) {
                paths.forEach { mediaInfo ->
                    when (mediaInfo.type) {
                        3 -> {
                            // 以视频传输
                            mPresenter?.sendVideoMessage(mediaInfo.path)
                        }
                        2 -> {
                            // 以动图传输
                            mPresenter?.sendDynamicImageMessage(0, mediaInfo.path)
                        }
                        else -> {
                            // 以静态图片传输
                            mPresenter?.sendImageMessage(mediaInfo.path)
                        }
                    }
                }
            }
        }

        if (requestCode == TOOL_CAMERAVIEW_REQUESTCODE && resultCode == Activity.RESULT_OK) {
            val mimeType = data?.getStringExtra(CameraActivity.RESULT_FLAG_MIME_TYPE)
            val filePath = data?.getStringExtra(CameraActivity.RESULT_FLAG_PATH)

            filePath?.let {
                when (mimeType) {
                    CameraActivity.JPEG -> {// 以图片传输
                        mPresenter?.sendImageMessage(filePath)
                    }
                    CameraActivity.MP4 -> {// 以视频传输
                        mPresenter?.sendVideoMessage(filePath)
                    }
                    else -> {
                        // 报错
                    }
                }
            }
        }

        if (requestCode == TOOL_NAMECARD_REQUESTCODE && resultCode == Activity.RESULT_OK) {
            val uid = data?.getLongExtra("uid", 0L) ?: 0L
            if (uid > 0) {
                mPresenter?.sendNameCardMessage(uid)
            }
        }

        //文件
        if (requestCode == FilePickerManager.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            //返回的是文件和文件类型
            val arrayList = FilePickerManager.obtainData()
            for (hashMap in arrayList) {
                val mimeType = hashMap["mimetype"] as String
                val path = hashMap["path"] as String
                val file = File(path)
                if (file.exists() && file.isFile && file.length() > 0) {
                    mPresenter?.sendFileMessage(file.absolutePath, mimeType)
                }
            }
        }

        //位置
        if (requestCode == ChoiceLocationActivity.REQUEST_CODE_SEND_LOCATION && resultCode == Activity.RESULT_OK && data != null) {
            val poiBean = data.getSerializableExtra("data") as POIBean?
            poiBean?.let {
                mPresenter?.sendLocationMessage(poiBean.lat, poiBean.lng, poiBean.name)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ), GET_PERMISSIONS_REQUEST_CODE
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mEmojiPopup?.dismiss()
        mToolsPopup?.dismiss()

        UploadAttachmentController.detachUploadListener(this@GroupSendChatActivity)

        message_input_view.recordingListener = null
        mPresenter?.destroy()

        (BaseApp.app.getSystemService(Context.AUDIO_SERVICE) as AudioManager).isSpeakerphoneOn =
            true
        (BaseApp.app.getSystemService(Context.AUDIO_SERVICE) as AudioManager).mode =
            AudioManager.MODE_NORMAL
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(object : ContextWrapper(newBase) {
            override fun getSystemService(name: String): Any {
                // 解决 VideoView 中 AudioManager 造成的内存泄漏
                return if (Context.AUDIO_SERVICE == name && applicationContext != null) {
                    applicationContext.getSystemService(name)
                } else super.getSystemService(name)
            }
        })
    }

    override fun isActive(): Boolean {
        return !ActivitiesHelper.isDestroyedActivity(this@GroupSendChatActivity)
    }

    private fun setUpEmojiPopup() {
        mEmojiPopup = IconFacePopup.Builder.fromRootView(root_view_private_chat)
            .setOnPopupShownListener { message_input_view.faceView.setImageResource(R.drawable.msg_chat_ic_keyboard) }
            .setOnSoftKeyboardOpenListener {
            }
            .setOnPopupDismissListener { message_input_view.faceView.setImageResource(R.drawable.msg_chat_ic_emoji) }
            .setOnSoftKeyboardCloseListener { }
            .setOnIconFaceListener(object : IconFacePageView.OnIconFaceListener {
                override fun onIconFaceBackspaceClicked() {

                }

                override fun onIconFaceClicked(iconFaceItem: IconFaceItem?, type: Int) {
                    if (type == 3 && iconFaceItem != null) {
                        if (this@GroupSendChatActivity.getString(R.string.append) == iconFaceItem.name) {
                            // 管理表情
                            ARouter.getInstance()
                                .build(Constant.ARouter.ROUNTE_MSG_DYNAMIC_FACE_MANAGER)
                                .navigation()
                        } else {
                            // 发送表情
                            mPresenter?.sendDynamicImageUrlMessage(
                                iconFaceItem.id,
                                iconFaceItem.path,
                                iconFaceItem.width,
                                iconFaceItem.height
                            )
                        }
                    }
                }
            })
            .build(message_input_view.inputView)

        EventBus.publishEvent(DynamicFaceUpdateEvent())
    }

    private fun updateDynamicFace() {
        HttpManager.getStore(UserHttpProtocol::class.java)
            .getEmoticon(object : HttpReq<UserProto.GetEmoticonReq>() {
                override fun getData(): UserProto.GetEmoticonReq {
                    return UserHttpReqCreator.getEmoticon()
                }
            })
            .applyCache(
                "${mMineUid}_dynamic_face_cache",
                framework.telegram.support.system.cache.stategy.CacheStrategy.firstCache()
            )
            .getResultWithCache(lifecycle(), {
                val faces = mutableListOf<DynamicFaceBean>()
                it.data.emoticonsList.forEach { emoticon ->
                    faces.add(
                        DynamicFaceBean(
                            emoticon.emoticonId,
                            emoticon.emoticonUrl,
                            emoticon.width,
                            emoticon.height
                        )
                    )
                }

                mEmojiPopup?.addDynamicFaceIcons(faces)
            }, {
            })
    }

    private fun setUpToolsPopup() {
        mToolsPopup = ToolsPopup.Builder.fromRootView(root_view_private_chat)
            .setOnToolClickListener { index ->
                KeyboardktUtils.hideKeyboard(root_view_private_chat)
                when (index) {
                    0 -> ImagePicker.from(this@GroupSendChatActivity)
                        .choose(
                            EnumSet.of(MimeType.JPEG, MimeType.PNG, MimeType.GIF, MimeType.MP4),
                            false
                        )
                        .addFilter(
                            Mp4SizeFilter(
                                150 * 1024 * 1024,
                                getString(R.string.hint),
                                getString(R.string.maximum_file_limit_50m)
                            )
                        )
                        .countable(true)
                        .maxSelectable(1)
                        .thumbnailScale(0.85f)
                        .originalEnable(false)
                        .showSingleMediaType(false)
                        .imageEngine(GlideEngine())
                        .forResult(TOOL_IMAGEPICKER_REQUESTCODE)
                    1 -> {
                        val saveDir = DirManager.getCameraFileDir(
                            BaseApp.app,
                            AccountManager.getLoginAccountUUid()
                        )
                        val intent =
                            CameraActivity.getLaunchIntent(this@GroupSendChatActivity, saveDir)
                        startActivityForResult(intent, TOOL_CAMERAVIEW_REQUESTCODE)
                    }
                    2 -> {
                        toast(this@GroupSendChatActivity.getString(R.string.string_group_send_name))
                    }
                    //位置
                    3 -> {
                        toast(this@GroupSendChatActivity.getString(R.string.string_group_send_location))
                    }
                    //文件
                    4 -> {
                        FilePickerManager.from(this@GroupSendChatActivity)
                            .forResult(FilePickerManager.REQUEST_CODE)
                    }
                }
            }
            .setOnPopupShownListener { message_input_view.faceView.setImageResource(R.drawable.msg_chat_ic_emoji) }
            .setOnSoftKeyboardOpenListener {

            }
            .setOnPopupDismissListener { message_input_view.faceView.setImageResource(R.drawable.msg_chat_ic_emoji) }
            .setOnSoftKeyboardCloseListener { }
            .build(message_input_view.inputView)
    }

    override fun onBackPressed() {
        // 屏蔽返回键
        toast(getString(R.string.group_send_back_press_disable))
    }

    override fun showError(str: String, data: GroupSendMsg) {
        toast(str)
        mReSendData = data
        errorDialog()
    }

    override fun showProcess(process: Int, size: Int) {
        Log.i("lzh", "process $process size $size")


        if (process == 0) {
            Log.i("lzh", "size $size")
        }
        if (size == 1 && process == 0) {
            showProgressDialog()

            mProgressBar?.progress = 100
            mProgressBar?.max = 100
            mProgressSize?.text = "100%"

            finishProgressDialog()
        } else {
            when (process) {
                0 -> {
                    showProgressDialog()
                }
                (size - 1) -> {
                    mProgressBar?.progress = size
                    mProgressBar?.max = size
                    mProgressSize?.text = "100%"

                    finishProgressDialog()
                }
                else -> {
                    mProgressBar?.progress = process
                    mProgressBar?.max = size
                    val percent = (process.toFloat() / size)
                    val data = (percent * 100).toInt().toString() + "%"
                    mProgressSize?.text = data
                }
            }
        }
    }

    override fun uploadStart(chatType: Int, targetId: Long, msgLocalId: Long) {

    }

    @SuppressLint("SetTextI18n")
    override fun uploadProgress(
        chatType: Int,
        targetId: Long,
        msgLocalId: Long,
        percent: Double,
        currentOffset: Long,
        totalLength: Long
    ) {
        if (targetId == GROUP_SEND_ID) {
            mProgressBar?.progress = (percent * 100 * 0.2).toInt()
            val data = (percent * 100 * 0.2).toInt().toString() + "%"
            mProgressSize?.text = data
        }
    }

    override fun uploadCancel(chatType: Int, targetId: Long, msgLocalId: Long) {
    }

    override fun uploadComplete(chatType: Int, targetId: Long, msgLocalId: Long) {
    }

    override fun uploadFail(chatType: Int, targetId: Long, msgLocalId: Long) {
        mDialog?.dismiss()
        toast(getString(R.string.string_group_send_upload_fail))
    }

    private fun showProgressDialog() {
        if (mDialog == null)
            mDialog = AppDialog.showCustomView(
                this@GroupSendChatActivity,
                R.layout.msg_group_send_dialog,
                null
            ) {
                cancelOnTouchOutside(false)
                view.setBackgroundColor(Color.TRANSPARENT)
                mProgressName = view.findViewById<TextView>(R.id.progress_name)
                mProgressName?.text = getString(R.string.string_group_send_uploading)
                mSureText = view.findViewById<TextView>(R.id.sure)
                mCancelText = view.findViewById<TextView>(R.id.cancel)
                mSureText?.setOnClickListener {
                    onClickSureBtn()
                }
                mCancelText?.setOnClickListener {
                    UploadAttachmentController.cancelUpload(
                        ChatModel.CHAT_TYPE_PVT,
                        mMineUid,
                        GROUP_SEND_ID,
                        0
                    )
                    mDialog?.dismiss()
                    mPresenter?.destroy()
                    finish()
                }
                mProgressSize = view.findViewById<TextView>(R.id.progress_size)
                mProgressBar = view.findViewById<ProgressBar>(R.id.normal_background_progress)
            }
    }

    private fun finishProgressDialog() {
        mReSendData = null
        mSureText?.text = getString(R.string.fine)
        mProgressName?.text = getString(R.string.string_group_send_complete)
        mProgressName?.setTextColor(getSimpleColor(R.color.c4a4a4a))

        mSureText?.visibility = View.VISIBLE
        mCancelText?.visibility = View.GONE
    }

    private fun errorDialog() {
        mSureText?.text = getString(R.string.string_group_send_reload)
        mProgressName?.text = getString(R.string.string_group_send_fail)
        mProgressName?.setTextColor(getSimpleColor(R.color.f50d2e))
        mSureText?.visibility = View.VISIBLE
        mCancelText?.visibility = View.VISIBLE
    }

    private fun onClickSureBtn() {
        if (mReSendData == null) {
            EventBus.publishEvent(MainToChatMessageEvent())
            val postcard = ARouter.getInstance().build("/app/act/main")
            LogisticsCenter.completion(postcard)
            val tClass = postcard.destination
            ActivitiesHelper.getInstance().closeToTarget(tClass)
        } else {
            mReSendData?.let {
                when (it) {
                    is GroupSendMsgImage -> mPresenter?.sendImageMessage(it.imageFilePath)
                    is GroupSendMsgVoice -> mPresenter?.sendVoiceMessage(
                        it.recordTime,
                        it.recordFilePath,
                        it.highDArr
                    )
                    is GroupSendMsgDynamicImage -> mPresenter?.sendDynamicImageMessage(
                        it.emoticonId,
                        it.imageFilePath
                    )
                    is GroupSendMsgDynamicUrlImage -> mPresenter?.sendDynamicImageUrlMessage(
                        it.emoticonId,
                        it.imageFilePath,
                        it.width,
                        it.height
                    )
                    is GroupSendMsgVideo -> mPresenter?.sendVideoMessage(it.videoFilePath)
                    is GroupSendMsgFile -> mPresenter?.sendFileMessage(it.filePath, it.mimeType)
                    else -> {

                    }
                }
            }
        }
    }
}
