package framework.telegram.business.ui.login.presenter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.im.domain.pb.UserProto
import com.trello.rxlifecycle3.android.ActivityEvent
import com.yalantis.ucrop.UCrop
import framework.telegram.business.R
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.UserHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.UserHttpProtocol
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.file.DirManager
import framework.telegram.ui.imagepicker.ImagePicker
import framework.telegram.ui.imagepicker.MimeType
import framework.telegram.ui.imagepicker.engine.impl.GlideEngine
import framework.telegram.ui.utils.FileUtils
import framework.telegram.ui.utils.ScreenUtils
import io.reactivex.Observable
import java.io.File
import java.util.*

class PerfectInfoPresenterImpl : PerfectInfoContract.Presenter {
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
    private val mView: PerfectInfoContract.View
    private val mViewObservalbe: Observable<ActivityEvent>

    constructor(view: PerfectInfoContract.View, activity: AppCompatActivity, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mActivity = activity
        view.setPresenter(this)
    }

    override fun start() {

    }

    override fun savePerfectInfo(opList: List<UserProto.UserOperator>, userParam: UserProto.UserParam) {
        mActivity.runOnUiThread {
            mView.showLoading()
        }
        HttpManager.getStore(UserHttpProtocol::class.java)
                .updateUserInfo(object : HttpReq<UserProto.UpdateReq>() {
                    override fun getData(): UserProto.UpdateReq {
                        return UserHttpReqCreator.createUserUpdateReq(opList, userParam)
                    }
                })
                .getResult(mViewObservalbe, {
                    //请求成功
                    mView.savePerfectInfoSuccess(mActivity.getString(R.string.set_personal_attributes_successfully))
                }, {
                    //请求失败
                    mView.showErrMsg(mActivity.getString(R.string.request_unsuccessful))
                })
    }

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
                    mView.setUserHeadUri(mCropImageTempUri)
                }
                TAKE_PICTURE -> {
                    mImageTempUri?.let {
                        cropImage(it)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            GET_PERMISSIONS_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (mActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                            || mActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        mActivity.toast(mActivity.getString(R.string.no_permissions_were_obtained))
                    } else {
                        pickPhoto()
                    }
                }
            }
            GET_PERMISSIONS_REQUEST_CAMERA_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (mActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                            || mActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                            || mActivity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        mActivity.toast(mActivity.getString(R.string.no_permissions_were_obtained))
                    } else {
                        takePhoto()
                    }
                }
            }
        }
    }

    private fun cropImage(uriSource: Uri) {
        FileUtils.deleteQuietly(mCropImageTempFile)
        mCropImageTempFile = File(DirManager.getImageCacheDir(BaseApp.app, AccountManager.getLoginAccountUUid()), "temp_image_crop_${System.currentTimeMillis()}.jpg")
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
        ImagePicker.from(mActivity).choose(EnumSet.of(MimeType.JPEG, MimeType.PNG))
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