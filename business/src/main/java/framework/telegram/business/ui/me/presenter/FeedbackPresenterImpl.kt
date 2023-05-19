package framework.telegram.business.ui.me.presenter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.im.domain.pb.CommonProto
import com.im.domain.pb.SysProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.R
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.SysHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.SystemHttpProtocol
import framework.telegram.business.ui.contacts.presenter.ComplainEditPresenterImpl
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.ui.imagepicker.ImagePicker
import framework.telegram.ui.imagepicker.MimeType
import framework.telegram.ui.imagepicker.engine.impl.GlideEngine
import framework.telegram.support.tools.ExpandClass.toast
import io.reactivex.Observable
import java.io.File
import java.util.*

class FeedbackPresenterImpl : FeedbackContract.Presenter {

    companion object {
        internal const val GET_PERMISSIONS_REQUEST_CODE = 100
        internal const val TOOL_IMAGEPICKER_REQUESTCODE = 0x1000
    }

    private val mActivity: AppCompatActivity
    private val mView: FeedbackContract.View
    private val mViewObservalbe: Observable<ActivityEvent>

    constructor(view: FeedbackContract.View,  activity: AppCompatActivity, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mActivity = activity
        view.setPresenter(this)
    }

    private var mTaskIndex = 0

    override fun clickPickPhoto(taskIndex: Int) {
        mTaskIndex = taskIndex
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == AppCompatActivity.RESULT_OK) {
            when (requestCode) {
                TOOL_IMAGEPICKER_REQUESTCODE -> {
                    val paths = ImagePicker.obtainInfoResult(data)
                    if (paths != null && paths.isNotEmpty() && paths.size >= 1) {
                        val uri = Uri.fromFile(File(paths[0].path))
                        mView.setUri(mTaskIndex,uri)
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
        }
    }

    private fun checkPhoto(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return if (mActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || mActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                mActivity.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), ComplainEditPresenterImpl.GET_PERMISSIONS_REQUEST_CODE)
                false
            } else {
                true
            }
        }
        return true
    }

    override fun start() {
    }

    override fun setFeedback(type: Int, msg: String,picUrls:String) {
        var realType = CommonProto.FeedbackType.SUGGEST
        when (type) {
            0 -> {
                realType = CommonProto.FeedbackType.SUGGEST
            }
            1 -> {
                realType = CommonProto.FeedbackType.MISTAKE
            }
            2 -> {
                realType = CommonProto.FeedbackType.OTHER
            }
        }
        HttpManager.getStore(SystemHttpProtocol::class.java)
                .setFeedback(object : HttpReq<SysProto.FeedbackReq>() {
                    override fun getData(): SysProto.FeedbackReq {
                        return SysHttpReqCreator.createFeedBackReq(realType, msg,picUrls)
                    }
                })
                .getResult(mViewObservalbe, {
                    //请求成功
                    mView.setFeedbackSuccess(msg)
                }, {
                    //请求失败
                    mView.showErrMsg(mActivity.getString(R.string.common_fail))
                })
    }
}