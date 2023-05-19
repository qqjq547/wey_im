package framework.telegram.business.ui.group

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.CommonProto
import com.yalantis.ucrop.UCrop
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.http.HttpException
import framework.telegram.business.manager.UploadManager
import framework.telegram.message.bridge.event.GroupInfoChangeEvent
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.file.DirManager
import framework.telegram.ui.imagepicker.ImagePicker
import framework.telegram.ui.imagepicker.MimeType
import framework.telegram.ui.imagepicker.engine.impl.GlideEngine
import framework.telegram.ui.utils.FileUtils
import framework.telegram.ui.utils.ScreenUtils
import kotlinx.android.synthetic.main.bus_group_activity_group_info.*
import java.io.File
import java.util.*

@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_INFO)
class GroupInfoActivity : BaseActivity() {

    companion object {
        internal const val GET_PERMISSIONS_REQUEST_CODE = 100
        internal const val EDIT_NAME_REQUEST_CODE = 0x1005
        internal const val TOOL_IMAGEPICKER_REQUESTCODE = 0x1001
    }

    private var mCropImageTempFile: File? = null
    private var mCropImageTempUri: Uri? = null

    private val mGroupId by lazy { intent.getLongExtra("groupId", 0) }
    private val mGroupIcon by lazy { intent.getStringExtra("groupIcon") }
    private var mGroupName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (mGroupId <= 0) {
            finish()
            return
        }
        mGroupName = intent.getStringExtra("groupName")?:""
        setContentView(R.layout.bus_group_activity_group_info)

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        layout_group_icon.setOnClickListener {
            ImagePicker.from(this).choose(EnumSet.of(MimeType.JPEG, MimeType.PNG))
                    .countable(true)
                    .maxSelectable(1)
                    .thumbnailScale(0.85f)
                    .originalEnable(false)
                    .showSingleMediaType(true)
                    .imageEngine(GlideEngine())
                    .forResult(TOOL_IMAGEPICKER_REQUESTCODE)
        }

        layout_group_name.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_EDIT_NAME)
                    .withString("defaultValue", mGroupName)
                    .withString("title", getString(R.string.edit_group_name))
                    .withInt("max_count", Constant.Bus.MAX_TEXT_NAME)
                    .navigation(this@GroupInfoActivity, EDIT_NAME_REQUEST_CODE)
        }

        app_image_view_group_icon.setImageURI(mGroupIcon)
        app_text_view_group_name.text = mGroupName
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requestCode == GET_PERMISSIONS_REQUEST_CODE) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                toast(getString(R.string.no_permissions_were_obtained))
                finish()
            }
        }

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                TOOL_IMAGEPICKER_REQUESTCODE -> {
                    val paths = ImagePicker.obtainInfoResult(data)
                    if (paths != null && paths.isNotEmpty() && paths.size >= 1) {
                        FileUtils.deleteQuietly(mCropImageTempFile)
                        mCropImageTempFile = File(DirManager.getImageCacheDir(BaseApp.app, AccountManager.getLoginAccountUUid()), "temp_image_crop_${System.currentTimeMillis()}.jpg")
                        mCropImageTempUri = Uri.fromFile(mCropImageTempFile)

                        val uri = Uri.fromFile(File(paths[0].path))
                        val size = ScreenUtils.dp2px(this, 120f)
                        UCrop.of(uri, mCropImageTempUri!!)
                                .withAspectRatio(1f, 1f)
                                .withMaxResultSize(size, size)
                                .start(this)
                    }
                }
                UCrop.REQUEST_CROP -> {
                    if (mCropImageTempUri != null) {
                        UploadManager.uploadFile(this@GroupInfoActivity, mCropImageTempUri.toString(), CommonProto.AttachType.PIC, CommonProto.AttachWorkSpaceType.COMMON, {
                            runOnUiThread {
                                app_image_view_group_icon.setImageURI(it)
                                ArouterServiceManager.groupService.saveGroupPic(lifecycle(), mGroupId, it, {
                                    toast(getString(R.string.the_group_profile_was_modified_successfully))
                                    //修改群头像，没有socket消息通知，因此需要本地事件通知更新
                                    EventBus.publishEvent(GroupInfoChangeEvent(mGroupId))
                                    setResult(Activity.RESULT_OK)
                                }, {
                                    if (it is HttpException) {
                                        toast(it.errMsg)
                                    } else {
                                        toast(getString(R.string.failed_to_modify_group_profile))
                                    }
                                })
                            }
                        }, {
                            runOnUiThread {
                                toast(getString(R.string.failed_to_modify_group_profile))
                            }
                        })
                    }
                }
                EDIT_NAME_REQUEST_CODE -> {
                    val text = data?.getStringExtra("text") ?: ""
                    if (!TextUtils.isEmpty(text)) {
                        app_text_view_group_name.text = text
                        mGroupName = text
                        ArouterServiceManager.groupService.saveGroupName(lifecycle(), mGroupId, text, {
                            toast(getString(R.string.group_name_changed_successfully))
                            //修改群名称，有socket消息通知，因此不需要本地事件通知更新
                            setResult(Activity.RESULT_OK)
                        }, {
                            if (it is HttpException) {
                                toast(it.errMsg)
                            } else {
                                toast(getString(R.string.failed_to_change_group_name))
                            }
                        })
                    } else {
                        toast(getString(R.string.failed_to_change_group_name))
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FileUtils.deleteQuietly(mCropImageTempFile)
    }
}