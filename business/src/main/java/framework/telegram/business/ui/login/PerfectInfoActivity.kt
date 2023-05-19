package framework.telegram.business.ui.login

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.text.InputFilter
import android.text.TextUtils
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.CommonProto
import com.im.domain.pb.UserProto
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.login.presenter.PerfectInfoContract
import framework.telegram.business.ui.login.presenter.PerfectInfoPresenterImpl
import framework.telegram.business.utils.CustomCoinNameFilter
import framework.telegram.business.manager.UploadManager
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.NavBarUtils
import kotlinx.android.synthetic.main.bus_login_activity_perfect_info.*

/**
 * Created by lzh on 19-5-16.
 * INFO:完善注册信息
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_LOGIN_PERFECT_INFO)
class PerfectInfoActivity : BaseBusinessActivity<PerfectInfoContract.Presenter>(), PerfectInfoContract.View {

    private var mCropImageTempUri: Uri? = null

    override fun getLayoutId() = R.layout.bus_login_activity_perfect_info

    //获取屏幕的高度
    private val mScreenHeight by lazy {this@PerfectInfoActivity.window.decorView.rootView.height}

    private var mUserName: String? = ""
    private var mPicture: String? = ""

    // 最后一次点击退出的时间
    private var mLastExitTime: Long = 0

    override fun initView() {
        image_view_user.setImageURI(UriUtils.newWithResourceId(R.drawable.bus_login_icon_upload_icon))
        setSaveBtn(false)

        eet.initEasyEditText(true,false,false,null,{
            mUserName = it.toString()
            if (!TextUtils.isEmpty(mUserName)) {
                setSaveBtn(true)
            } else {
                setSaveBtn(false)
            }
        })
        eet.et.filters = arrayOf<InputFilter>(CustomCoinNameFilter(Constant.Bus.MAX_TEXT_NAME))
        eet.et.hint = getString(R.string.bus_login_upload_username)

        custom_toolbar.setToolbarColor(R.color.white)
    }

    override fun initListen() {
        image_view_user.setOnClickListener {
            AppDialog.showBottomListView(this, this, mutableListOf(getString(R.string.photograph), getString(R.string.select_from_camera))) { _, index, _ ->
                when (index) {
                    0 -> {
                        mPresenter?.clickTakePhoto()
                    }
                    1 -> {
                        mPresenter?.clickPickPhoto()
                    }
                }
            }
        }

        text_view_login.setOnClickListener {
            if (mCropImageTempUri != null) {
                UploadManager.uploadFile(this@PerfectInfoActivity,mCropImageTempUri.toString(), CommonProto.AttachType.PIC, CommonProto.AttachWorkSpaceType.COMMON, {
                    mPicture = it
                    val userParam = UserProto.UserParam.newBuilder().setIcon(mPicture).setNickName(mUserName).build()
                    val list = listOf(UserProto.UserOperator.NICK_NAME, UserProto.UserOperator.ICON)
                    mPresenter?.savePerfectInfo(list, userParam)
                }, {
                    toast(getString(R.string.upload_picture_failed))
                })
            } else {
                val userParam = UserProto.UserParam.newBuilder().setNickName(mUserName).build()
                val list = listOf(UserProto.UserOperator.NICK_NAME)
                mPresenter?.savePerfectInfo(list, userParam)
            }
        }

        linear_layout_all.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            //获取当前界面可视部分
            this@PerfectInfoActivity.window.decorView.getWindowVisibleDisplayFrame(r)
            //此处就是用来获取键盘的高度的， 在键盘没有弹出的时候 此高度为0 键盘弹出的时候为一个正数
            val heightDifference = mScreenHeight - r.bottom
            val textY = mScreenHeight - view1.bottom
            val navHeight = NavBarUtils.getNavigationBarHeight(this@PerfectInfoActivity)
            if (heightDifference -navHeight > 0 ){
                linear_layout_all.translationY = 0 - (heightDifference - textY).toFloat()
            }else{
                linear_layout_all.translationY = 0f
            }
        }
    }

    override fun initData() {
        PerfectInfoPresenterImpl(this, this, lifecycle()).start()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mPresenter?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mPresenter?.onActivityResult(requestCode, resultCode, data)
    }

    override fun setUserHeadUri(uri: Uri?) {
        mCropImageTempUri = uri
        image_view_user.setImageURI(uri)
    }

    private fun setSaveBtn(contentOK: Boolean) {
        if (contentOK) {
            text_view_login.isEnabled = true
            text_view_login.background = getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
        } else {
            text_view_login.isEnabled = false
            text_view_login.background = getSimpleDrawable(R.drawable.common_corners_trans_d4d6d9_6_0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter?.onDestroy()
    }

    override fun showLoading() {
        try {
            dialog?.dismiss()
            dialog = AppDialog.showLoadingView(this@PerfectInfoActivity, this@PerfectInfoActivity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun savePerfectInfoSuccess(str: String?) {
        dialog?.dismiss()
        val accountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }
        accountInfo.putNickName(mUserName ?: "")
        accountInfo.putAvatar(mPicture ?: "")
        BaseApp.app.onUserInfoChange()
        ARouter.getInstance().build("/app/act/main").navigation()
        finish()
    }

    override fun showErrMsg(str: String?) {
        dialog?.dismiss()
        toast(str ?: "")
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as PerfectInfoContract.Presenter
    }

    override fun isActive() = true

    override fun onBackPressed() {
        if (System.currentTimeMillis() - mLastExitTime > 1000) {
            toast(getString(R.string.common_quit_app_tips))
            mLastExitTime = System.currentTimeMillis()
        } else {
            ActivitiesHelper.getInstance().closeAll()
            run {
                Runtime.getRuntime().gc()
            }
        }
    }
}