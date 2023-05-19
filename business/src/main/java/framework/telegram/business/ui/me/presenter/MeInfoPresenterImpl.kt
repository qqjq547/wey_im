package framework.telegram.business.ui.me.presenter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.im.domain.pb.CommonProto
import com.im.domain.pb.UserProto
import com.trello.rxlifecycle3.android.ActivityEvent
import com.yalantis.ucrop.UCrop
import framework.telegram.support.tools.file.DirManager
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.UserHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.UserHttpProtocol
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.imagepicker.ImagePicker
import framework.telegram.ui.imagepicker.MimeType
import framework.telegram.ui.imagepicker.engine.impl.GlideEngine
import framework.telegram.ui.utils.ScreenUtils
import io.reactivex.Observable
import java.io.File
import java.util.*
import androidx.core.content.FileProvider
import framework.telegram.business.R
import framework.telegram.business.manager.UploadManager
import framework.telegram.ui.utils.FileUtils


class MeInfoPresenterImpl : MeInfoContract.Presenter {


    companion object {
        internal const val GET_PERMISSIONS_REQUEST_CODE = 100
        internal const val GET_PERMISSIONS_REQUEST_CAMERA_CODE = 101
        internal const val TOOL_IMAGEPICKER_REQUESTCODE = 0x1000
        internal const val TAKE_PICTURE = 0x1001
    }

    private var mCropImageTempFile: File? = null
    private var mCropImageTempUri: Uri? = null
    private var mImageTempFile: File? = null
    private var mImageTempUri: Uri? = null

    private val mActivity: AppCompatActivity
    private val mView: MeInfoContract.View
    private val mViewObservable: Observable<ActivityEvent>
    private val mLifeCycle: LifecycleOwner

    constructor(view: MeInfoContract.View, lifeCycle: LifecycleOwner, activity: AppCompatActivity, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mViewObservable = observable
        this.mActivity = activity
        this.mLifeCycle = lifeCycle
        view.setPresenter(this)
    }

    override fun start() {

    }


    /**
     * 更新性别
     * sex 跟 gender 存在序号差别
     */
    override fun saveSexInfo(sex: Int, name: String, gender: CommonProto.Gender) {
        val userParam = UserProto.UserParam.newBuilder().setGender(gender).build()
        val opList = listOf(UserProto.UserOperator.GENDER)
        HttpManager.getStore(UserHttpProtocol::class.java)
                .updateUserInfo(object : HttpReq<UserProto.UpdateReq>() {
                    override fun getData(): UserProto.UpdateReq {
                        return UserHttpReqCreator.createUserUpdateReq(opList, userParam)
                    }
                })
                .getResult(mViewObservable, {
                    //请求成功
                    mView.saveSexSuccess(sex, name)
                }, {
                    //请求失败
                    mView.showErrMsg(mActivity?.getString(R.string.common_fail))
                })
    }


    private fun savePicInfo(info: String) {
        val userParam = UserProto.UserParam.newBuilder().setIcon(info).build()
        val opList = listOf(UserProto.UserOperator.ICON)
        HttpManager.getStore(UserHttpProtocol::class.java)
                .updateUserInfo(object : HttpReq<UserProto.UpdateReq>() {
                    override fun getData(): UserProto.UpdateReq {
                        return UserHttpReqCreator.createUserUpdateReq(opList, userParam)
                    }
                })
                .getResult(mViewObservable, {
                    //请求成功
                    mView.savePicInfoSuccess(info)
                }, {
                    //请求失败
                    mView.showErrMsg(mActivity?.getString(R.string.common_fail))
                })
    }

    private fun upLoadPicture(uri: Uri?) {
        UploadManager.uploadFile(mActivity, uri.toString(), CommonProto.AttachType.PIC, CommonProto.AttachWorkSpaceType.COMMON, {
            savePicInfo(it)
        }, {
            mView.showErrMsg("Falha ao carregar a imagem")
        })
    }

    /**
     * 监听跳转回来的Intent，然后分发跳转结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == AppCompatActivity.RESULT_OK) {
            when (requestCode) {
                TOOL_IMAGEPICKER_REQUESTCODE -> {
                    val paths = ImagePicker.obtainInfoResult(data)
                    if (paths != null && paths.isNotEmpty() && paths.size >= 1) {
                        val uri = Uri.fromFile(File(paths[0].path))
                        cropImage(uri)
                    }
                }
                UCrop.REQUEST_CROP -> {
                    mView.showLoading()
                    upLoadPicture(mCropImageTempUri)
                }
                TAKE_PICTURE -> {
                    mImageTempUri?.let {
                        cropImage(it)
                    }
                }
            }
        }
    }

    /**
     * 请求权限
     * 需判断当前 android版本 >= Build.VERSION_CODES.M ?
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || mActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                mActivity.toast(mActivity.getString(R.string.bus_me_permission_req_error))
            } else {
                when (requestCode) {
                    GET_PERMISSIONS_REQUEST_CODE -> {
                        pickPhoto()
                    }
                    GET_PERMISSIONS_REQUEST_CAMERA_CODE -> {
                        if (mActivity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                            mActivity.toast(mActivity.getString(R.string.bus_me_permission_req_error))
                        else
                            takePhoto()
                    }
                }
            }
        }
    }

    private fun cropImage(uriSource: Uri) {
        FileUtils.deleteQuietly(mCropImageTempFile)
        mCropImageTempFile = File(DirManager.getImageCacheDir(BaseApp.app, AccountManager.getLoginAccountUUid()), "temp_image_crop" + System.currentTimeMillis() + ".jpg")
        mCropImageTempUri = Uri.fromFile(mCropImageTempFile)
        val size = ScreenUtils.dp2px(mActivity, 120f)
        UCrop.of(uriSource, mCropImageTempUri!!)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(size, size)
                .start(mActivity)
    }

    override fun onDestroy() {
        FileUtils.deleteQuietly(mCropImageTempFile)
        FileUtils.deleteQuietly(mImageTempFile)
    }

    override fun clickTakePhoto() {
        if (checkCamera()) {
            takePhoto()
        }
    }

    override fun clickPickPhoto() {
        if (checkPhoto()) {
            pickPhoto()
        }
    }

    private fun pickPhoto() {
        ImagePicker.from(mActivity)
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
        mImageTempUri = FileProvider.getUriForFile(mActivity, mActivity.packageName, mImageTempFile!!)
        openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageTempUri)
        mActivity.startActivityForResult(openCameraIntent, TAKE_PICTURE)
    }

    private fun checkCamera(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return if (mActivity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || mActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || mActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                mActivity.requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), GET_PERMISSIONS_REQUEST_CAMERA_CODE)
                false
            } else {
                true
            }
        }
        return true
    }

    private fun checkPhoto(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return if (mActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || mActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                mActivity.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), GET_PERMISSIONS_REQUEST_CODE)
                false
            } else {
                true
            }
        }
        return true
    }
}