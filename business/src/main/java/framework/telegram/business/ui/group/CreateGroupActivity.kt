package framework.telegram.business.ui.group

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputFilter
import android.text.TextUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.CommonProto
import com.im.domain.pb.GroupProto
import com.yalantis.ucrop.UCrop
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.event.CreateGroupSuccessEvent
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.GroupHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.GroupHttpProtocol
import framework.telegram.business.ui.group.adapter.SelectedUsersAdapter
import framework.telegram.business.bridge.bean.SelectedUsersModel
import framework.telegram.business.utils.CustomCoinNameFilter
import framework.telegram.business.manager.UploadManager
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.file.DirManager
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.imagepicker.ImagePicker
import framework.telegram.ui.imagepicker.MimeType
import framework.telegram.ui.imagepicker.engine.impl.GlideEngine
import framework.telegram.ui.utils.FileUtils
import framework.telegram.ui.utils.ScreenUtils
import kotlinx.android.synthetic.main.bus_group_activity_create.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_CREATE)
class CreateGroupActivity : BaseActivity() {
    companion object {
        private val GET_PERMISSIONS_REQUEST_CODE = 1
        private val GET_PERMISSIONS_REQUEST_CAMERA_CODE = 0
        private val TOOL_IMAGEPICKER_REQUESTCODE = 10
        private val TAKE_PICTURE = 11
        private val EDIT_NAME_REQUEST_CODE = 21
    }

    private var mCropImageTempFile: File? = null
    private var mCropImageTempUri: Uri? = null
    private var mImageTempFile: File? = null
    private var mImageTempUri: Uri? = null

    private var isProgressing = false
    private var dialog: AppDialog? = null
    private val mSelectUserList by lazy {
        val bundle = intent.extras
        if (bundle != null)
            bundle.getParcelableArrayList<SelectedUsersModel>("selectList") ?: arrayListOf()
        else
            arrayListOf()
    }

    private val mSelectUids = arrayListOf<Long>()

    private val mAccountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)

    private val mAddedUserId by lazy {
        intent.getLongExtra("addedUserId", 0L)
    }

    private val mAdapter by lazy {
        SelectedUsersAdapter()
    }

    private var mSureTextView: TextView? = null

    private var mGroupIcon: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bus_group_activity_create)

        initUI()
    }

    /**
     * 监听跳转回来的Intent，然后分发跳转结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                TOOL_IMAGEPICKER_REQUESTCODE -> {
                    val paths = ImagePicker.obtainInfoResult(data)
                    if (paths != null && paths.isNotEmpty() && paths.size >= 1) {
                        val uri = Uri.fromFile(File(paths[0].path))
                        cropImage(uri)
                    }
                }
                UCrop.REQUEST_CROP -> {
                    dialog?.dismiss()
                    dialog = AppDialog.showLoadingView(this@CreateGroupActivity, this@CreateGroupActivity)
                    upLoadPicture(mCropImageTempUri)
                }
                TAKE_PICTURE -> {
                    mImageTempUri?.let {
                        cropImage(it)
                    }
                }
            }
            setSureButton()
        }
    }

    /**
     * 请求权限
     * 需判断当前 android版本 >= Build.VERSION_CODES.M ?
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                toast(getString(R.string.bus_me_permission_req_error))
            } else {
                when (requestCode) {
                    GET_PERMISSIONS_REQUEST_CODE -> {
                        pickPhoto()
                    }
                    GET_PERMISSIONS_REQUEST_CAMERA_CODE -> {
                        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                            toast(getString(R.string.bus_me_permission_req_error))
                        else
                            takePhoto()
                    }
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        FileUtils.deleteQuietly(mCropImageTempFile)
        FileUtils.deleteQuietly(mImageTempFile)
    }

    private fun initUI() {
        custom_toolbar.showCenterTitle(getString(R.string.bus_create_ground_chat))
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showRightTextView("", {
            operate()
        }) {
            val size = ScreenUtils.dp2px(this, 10f)
            mSureTextView = it
            val lp = mSureTextView?.layoutParams as LinearLayout.LayoutParams
            lp.rightMargin = ScreenUtils.dp2px(this, 10f)
            mSureTextView?.setPadding(size, 0, size, 0)
            setSureButton()
        }

        recycler_view_group.initSingleTypeRecycleView(GridLayoutManager(this@CreateGroupActivity, 5), mAdapter, false)
        recycler_view_group.refreshController().setEnablePullToRefresh(false)
        recycler_view_group.refreshController().setEnableLoadMore(false)


        mSelectUserList.add(0, SelectedUsersModel(mAccountInfo.getUserId(), mAccountInfo.getAvatar(), mAccountInfo.getNickName()))
        mAdapter.setNewData(mSelectUserList)

        mSelectUserList.forEach {
            mSelectUids.add(it.uid)
        }
        text_view_group_count.text = String.format(getString(R.string.group_member_count), mSelectUids.size)


        image_view_group_icon.setOnClickListener {
            showPhotoDialog()
        }

        layout_group_icon.setOnClickListener {
            showPhotoDialog()
        }

        et_name.initEasyEditText(true, false, false, null, {
            setSureButton()
        })
        et_name.et.hint = forecastGroupName()
        et_name.et.filters = arrayOf<InputFilter>(CustomCoinNameFilter(Constant.Bus.MAX_TEXT_NAME))
        et_name.requestFocus()

//        layout_group_name.setOnClickListener {
//            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_EDIT_NAME)
//                    .withString("defaultValue", "")
//                    .withString("title", getString(R.string.edit_group_name))
//                    .withInt("max_count", 15)
//                    .navigation(this@CreateGroupActivity, EDIT_NAME_REQUEST_CODE)
//        }
    }

    private fun forecastGroupName(): String {
        return if (mSelectUserList.size >= 2) {
            mSelectUserList[0].name + getString(R.string.separate_sign) + mSelectUserList[1].name
        } else {
            ""
        }
    }

    private fun showPhotoDialog() {
        AppDialog.showBottomListView(this,
                this,
                mutableListOf(getString(R.string.bus_me_camera), getString(R.string.bus_me_galley), getString(R.string.cancel))) { dialog, index, _ ->
            when (index) {
                0 -> {
                    if (checkCamera()) {
                        takePhoto()
                    }
                }
                1 -> {
                    if (checkPhoto()) {
                        pickPhoto()
                    }
                }
                2 -> {
                    dialog.dismiss()
                }
            }
        }
    }

    private fun operate() {
        isProgressing = true

        val groupName = if (!TextUtils.isEmpty(et_name.et.text.toString().trim())) {
            et_name.et.text.toString().trim()
        } else {
            forecastGroupName()
        }

        // 创建群
        if (mSelectUids.size > 1) {
            createGroupMember(mSelectUids, groupName, mGroupIcon)
        } else {
            EventBus.publishEvent(CreateGroupSuccessEvent())
            //跳转私聊界面
            ARouter.getInstance().build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY)
                    .withLong("targetUid", mSelectUids[0]).navigation()
            finish()
        }
        setSureButton()
    }


    private fun createGroupMember(uids: MutableList<Long>, groupName: String, groupIcon: String?) {
        val appDialog = AppDialog.showLoadingView(this@CreateGroupActivity, this@CreateGroupActivity)
        uids.remove(mAccountInfo.getUserId())
        ArouterServiceManager.contactService.getContactsInfoCache(lifecycle(), uids, { contactModels ->
            HttpManager.getStore(GroupHttpProtocol::class.java)
                    .groupCreate(object : HttpReq<GroupProto.GroupCreateReq>() {
                        override fun getData(): GroupProto.GroupCreateReq {
                            return GroupHttpReqCreator.createGroupCreateReq(uids, groupName, groupIcon)
                        }
                    })
                    .getResult(lifecycle(), {
                        isProgressing = false
                        appDialog.dismiss()

                        val groupInfo = it.groupBase
                        val groupId = groupInfo.groupId
                        val notFriendUidsList = it.notFriendUidsList
                        val needCheckUidsList = it.needCheckUidsList
                        val notFriendList = ArrayList<ContactDataModel>()
                        val needCheckList = ArrayList<ContactDataModel>()
                        if (!notFriendUidsList.isNullOrEmpty() || !needCheckUidsList.isNullOrEmpty()) {
                            contactModels.forEach { contactModel ->
                                if (notFriendUidsList.contains(contactModel.uid)) {
                                    notFriendList.add(contactModel)
                                } else if (needCheckUidsList.contains(contactModel.uid)) {
                                    needCheckList.add(contactModel)
                                }
                            }
                        }

                        if (it.bfSuccess) {
                            if (notFriendList.isNotEmpty() || needCheckList.isNotEmpty()) {
                                // 提示未成功邀请的联系人
                                val tipMsg = getTipMessage(getContactsNames(needCheckList), getContactsNames(notFriendList))
                                showCreateGroupTipDialog(getString(R.string.adding_password_for_user_admin), tipMsg) {
                                    createGroupSuccess(groupId)
                                }
                            } else {
                                createGroupSuccess(groupId)
                            }
                        } else {
                            if (notFriendList.isNotEmpty() || needCheckList.isNotEmpty()) {
                                // 提示未成功邀请的联系人
                                val tipMsg = getTipMessage(getContactsNames(needCheckList), getContactsNames(notFriendList))
                                showCreateGroupTipDialog(getString(R.string.failed_to_create_group_chat), tipMsg) {}
                            } else {
                                toast(getString(R.string.group_creation_failed))
                            }
                        }
                        setSureButton()
                    }, {
                        isProgressing = false
                        setSureButton()
                        appDialog.dismiss()
                        toast(getString(R.string.group_creation_failed))
                    })
        }, {
            appDialog.dismiss()
            toast(getString(R.string.group_creation_failed))
        })
    }

    private fun getTipMessage(needCheckUserNames: String, notFriendUserNames: String): String {
        return if (!TextUtils.isEmpty(needCheckUserNames) && !TextUtils.isEmpty(notFriendUserNames)) {
            String.format(getString(R.string.tip_message_one), notFriendUserNames, needCheckUserNames)
        } else if (!TextUtils.isEmpty(needCheckUserNames)) {
            String.format(getString(R.string.tip_message_three), needCheckUserNames)
        } else {
            String.format(getString(R.string.tip_message_four), notFriendUserNames)
        }
    }

    private fun showCreateGroupTipDialog(title: String, tipMsg: String, clickButton: () -> Unit) {
        AppDialog.show(this@CreateGroupActivity, this@CreateGroupActivity) {
            positiveButton(text = getString(R.string.confirm), click = {
                clickButton.invoke()
            })
            title(text = title)
            cancelOnTouchOutside(false)
            cancelable(false)
            message(text = tipMsg)
        }
    }

    private fun createGroupSuccess(groupId: Long) {
        EventBus.publishEvent(CreateGroupSuccessEvent())
        ArouterServiceManager.messageService.createChatHistory(ChatModel.CHAT_TYPE_GROUP, mAccountInfo.getUserId(), groupId, true)
        ARouter.getInstance().build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_GROUP_CHAT_ACTIVITY).withLong("targetGid", groupId).navigation()
        finish()
    }

    private fun getContactsNames(list: List<ContactDataModel>): String {
        val sb = StringBuilder()
        list.forEachIndexed { index, contactModel ->
            val displayName = contactModel.displayName
            when {
                index == 0 -> sb.append(displayName)
                index == 5 -> sb.append(getString(R.string.rank))
                index < 5 -> sb.append("、").append(displayName)
            }
        }
        return sb.toString()
    }

    private fun setSureButton() {
        if (mSelectUserList.size == 0 || isProgressing) {
            mSureTextView?.setTextColor(getSimpleColor(R.color.d4d6d9))
            mSureTextView?.isEnabled = false
        } else {
            mSureTextView?.setTextColor(getSimpleColor(R.color.c178aff))
            mSureTextView?.isEnabled = true
        }
        mSureTextView?.text = getString(R.string.create)
    }


    private fun pickPhoto() {
        ImagePicker.from(this)
                .choose(EnumSet.of(MimeType.JPEG, MimeType.PNG))
                .countable(true)
                .maxSelectable(1)
                .thumbnailScale(0.85f)
                .originalEnable(false)
                .showSingleMediaType(true)
                .imageEngine(GlideEngine())
                .forResult(TOOL_IMAGEPICKER_REQUESTCODE)
    }

    private fun takePhoto() {
        FileUtils.deleteQuietly(mImageTempFile)
        mImageTempFile = File(DirManager.getImageCacheDir(BaseApp.app, AccountManager.getLoginAccountUUid()), "temp_image" + System.currentTimeMillis() + ".jpg")
        //启动图片选择器
        val openCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        mImageTempUri = FileProvider.getUriForFile(this, packageName, mImageTempFile!!)
        openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageTempUri)
        startActivityForResult(openCameraIntent, TAKE_PICTURE)
    }

    private fun checkCamera(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), GET_PERMISSIONS_REQUEST_CAMERA_CODE)
                false
            } else {
                true
            }
        }
        return true
    }

    private fun checkPhoto(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), GET_PERMISSIONS_REQUEST_CODE)
                false
            } else {
                true
            }
        }
        return true
    }

    private fun cropImage(uriSource: Uri) {
        FileUtils.deleteQuietly(mCropImageTempFile)
        mCropImageTempFile = File(DirManager.getImageCacheDir(BaseApp.app, AccountManager.getLoginAccountUUid()), "temp_image_crop" + System.currentTimeMillis() + ".jpg")
        mCropImageTempUri = Uri.fromFile(mCropImageTempFile)
        val size = ScreenUtils.dp2px(this, 120f)
        UCrop.of(uriSource, mCropImageTempUri!!)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(size, size)
                .start(this)
    }

    private fun upLoadPicture(uri: Uri?) {
        UploadManager.uploadFile(this, uri.toString(), CommonProto.AttachType.PIC, CommonProto.AttachWorkSpaceType.COMMON, {
            runOnUiThread {
                mGroupIcon = it
                toast(getString(R.string.save_success))
                image_view_group_icon.setImageURI(mGroupIcon)
                dialog?.dismiss()
            }
        }, {
            runOnUiThread {
                dialog?.dismiss()
                toast(getString(R.string.common_fail))
            }
        })
    }
}