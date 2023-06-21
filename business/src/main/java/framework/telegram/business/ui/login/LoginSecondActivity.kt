package framework.telegram.business.ui.login

import android.Manifest
import android.content.Intent
import android.graphics.Rect
import android.os.CountDownTimer
import android.text.TextUtils
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.telegram.business.R
import framework.telegram.business.UpdatePresenterImpl
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.login.GetSmsCodeActivity.Companion.GET_SMSCODE_DATA_TIME
import framework.telegram.business.ui.login.GetSmsCodeActivity.Companion.GET_SMSCODE_RESULT
import framework.telegram.business.ui.login.presenter.LoginContract
import framework.telegram.business.ui.login.presenter.LoginPresenterImpl
import framework.telegram.business.utils.UserInfoCheckUtil
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.JumpPermissionManagement
import framework.telegram.support.tools.permission.MPermission
import framework.telegram.support.tools.permission.annotation.OnMPermissionDenied
import framework.telegram.support.tools.permission.annotation.OnMPermissionGranted
import framework.telegram.support.tools.permission.annotation.OnMPermissionNeverAskAgain
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.KeyboardktUtils
import framework.telegram.ui.utils.NavBarUtils
import kotlinx.android.synthetic.main.bus_login_activity_first.*
import kotlinx.android.synthetic.main.bus_login_activity_second.*
import kotlinx.android.synthetic.main.bus_login_activity_second.custom_toolbar
import kotlinx.android.synthetic.main.bus_login_activity_second.eet
import kotlinx.android.synthetic.main.bus_login_activity_second.image_view_user
import kotlinx.android.synthetic.main.bus_login_activity_second.linear_layout_all
import kotlinx.android.synthetic.main.bus_login_activity_second.text_view_change
import kotlinx.android.synthetic.main.bus_login_activity_second.text_view_login

/**
 * Created by lzh on 19-5-16.
 * INFO:登录
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_LOGIN_SECOND)
class LoginSecondActivity : BaseBusinessActivity<LoginContract.Presenter>(), LoginContract.View {

    private var mType: Int = 1 //0:密码登录   1：验证码登录

    private val accountInfo by lazy { AccountManager.getLoginAccountByUuid(AccountManager.getLastLoginAccountUuid(), AccountInfo::class.java) }

    private var mUpdatePresenterImpl: UpdatePresenterImpl? = null

    private var mFirstLoad = true

    //获取屏幕的高度
    private val mScreenHeight by lazy { this@LoginSecondActivity.window.decorView.rootView.height }

    override fun isActive() = isFinishing

    override fun getLayoutId() = R.layout.bus_login_activity_second

    override fun initView() {
        changeLoginMode()
        image_view_user.setImageURI(accountInfo.getAvatar())
        text_view_phone.text = accountInfo.getCountryCode() + " " + accountInfo.getPhone()

        custom_toolbar.setToolbarColor(R.color.white)

//        mUpdatePresenterImpl = UpdatePresenterImpl(this, this, lifecycle())
//        mUpdatePresenterImpl?.start(showCanUpDialog = true, showNotUpdateDialog = false)

        requestBasicPermission()
    }

    /******************权限*****************/
    private val BASIC_PERMISSIONS = arrayOf<String>(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun requestBasicPermission() {
        MPermission.with(this@LoginSecondActivity)
            .setRequestCode(Constant.Permission.BASIC_PERMISSION_REQUEST_CODE)
            .permissions(*BASIC_PERMISSIONS)
            .request()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        MPermission.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    @OnMPermissionGranted(Constant.Permission.BASIC_PERMISSION_REQUEST_CODE)
    fun onBasicPermissionSuccess() {
        // 授权成功
    }

    @OnMPermissionDenied(Constant.Permission.BASIC_PERMISSION_REQUEST_CODE)
    @OnMPermissionNeverAskAgain(Constant.Permission.BASIC_PERMISSION_REQUEST_CODE)
    fun onBasicPermissionFailed() {
        //授权失败
        AppDialog.show(this@LoginSecondActivity, this@LoginSecondActivity) {
            message(text = context.getString(R.string.authorization_failed_fail))
            cancelOnTouchOutside(false)
            cancelable(false)
            negativeButton(text = context.getString(R.string.pet_text_508), click = {
                requestBasicPermission()
            })
            positiveButton(text = getString(R.string.pet_text_195), click = {
                ActivitiesHelper.getInstance().closeAll()
                JumpPermissionManagement.GoToSetting(this@LoginSecondActivity)
            })
            title(text = context.getString(R.string.hint))
        }
    }

    /******************权限  end*****************/

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && mFirstLoad) {
            mFirstLoad = false
            val tipMsg = intent.getStringExtra("tipMsg") ?: ""
            if (!TextUtils.isEmpty(tipMsg)) {
                AppDialog.show(this@LoginSecondActivity, this@LoginSecondActivity) {
                    positiveButton(text = getString(R.string.confirm))
                    message(text = tipMsg)
                }
            }
        }
    }

    override fun initListen() {
        linear_layout_all.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            //获取当前界面可视部分
            this@LoginSecondActivity.window.decorView.getWindowVisibleDisplayFrame(r)
            //此处就是用来获取键盘的高度的， 在键盘没有弹出的时候 此高度为0 键盘弹出的时候为一个正数
            val heightDifference = mScreenHeight - r.bottom
            val textY = mScreenHeight - text_view_change.bottom
            val navHeight = NavBarUtils.getNavigationBarHeight(this@LoginSecondActivity)
            if (heightDifference - navHeight > 0) {
                linear_layout_all.translationY = 0 - (heightDifference - textY).toFloat()
                custom_toolbar.androidMTransparency(true)
            } else {
                linear_layout_all.translationY = 0f
                custom_toolbar.androidMTransparency(false)
            }
        }

        text_view_change.setOnClickListener {
            mType = if (mType == 0) 1 else 0
            changeLoginMode()
        }

        text_view_more.setOnClickListener {
            val data = mutableListOf<String>()
            data.add(getString(R.string.bus_login_change_account))
            data.add(getString(R.string.bus_login_register))
            if (mType == 0) {
                data.add(getString(R.string.string_forget_password))
            }

            AppDialog.showBottomListView(this@LoginSecondActivity, this@LoginSecondActivity, data) { _, index, _ ->
                if (index == 0) {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_LOGIN_FIRST)
                        .withBoolean("isCancel", true)
                        .withString("area_code", accountInfo.getCountryCode())
                        .navigation()
                } else if (index == 1) {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_LOGIN_REGISTER)
                        .withString("area_code", accountInfo.getCountryCode())
                        .navigation()
                } else if (index == 2) {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_ME_FIND_PASSWORD_FIRST)
                        .navigation()
                }
            }
        }

        linear_layout_all.setOnClickListener {
            KeyboardktUtils.hideKeyboard(linear_layout_all)
        }

        text_view_login.setOnClickListener {
            val text = eet.et.text.toString().trim()
            if (mType == 0) {
                if (!UserInfoCheckUtil.checkPassword(this, text))
                    return@setOnClickListener
                mPresenter?.loginByPassword(accountInfo.getPhone(), text, accountInfo.getCountryCode())
            } else {
                mPresenter?.sendCode(accountInfo.getPhone(), accountInfo.getCountryCode())
            }
        }
    }

    override fun initData() {
        LoginPresenterImpl(this, this, lifecycle()).start()
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as LoginContract.Presenter
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@LoginSecondActivity, this@LoginSecondActivity)
    }

    override fun loginSuccess(str: String?) {
        dialog?.dismiss()

        ActivitiesHelper.getInstance().closeExcept(LoginSecondActivity::class.java)
        ActivitiesHelper.getInstance().lastBackgroundAppTime = 0
        ARouter.getInstance().build("/app/act/main").withBoolean("fromLogin", true).navigation()
        finish()
    }

    override fun sendCodeSuccess(str: String?, time: Int) {
        dialog?.dismiss()
        if (!TextUtils.isEmpty(str)) {
            toast(str.toString())
        }

        startGetSmsCodeCountDown(time)

        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_LOGIN_GET_SMS_CODE)
            .withString("countryCode", accountInfo.getCountryCode())
            .withString("phone", accountInfo.getPhone())
            .withInt(GET_SMSCODE_DATA_TIME, time)
            .withInt("smsType", 0)
            .navigation(this@LoginSecondActivity, GET_SMSCODE_RESULT)
    }

    override fun showErrMsg(str: String?, showDialog: Boolean) {
        dialog?.dismiss()
        if (showDialog) {
            AppDialog.show(this@LoginSecondActivity, this@LoginSecondActivity) {
                positiveButton(text = getString(R.string.confirm))
                message(text = str)
            }
        } else {
            toast(str.toString())
        }
    }

    private fun changeLoginMode() {
        if (mType == 0) {
            eet.visibility = View.VISIBLE
            eet.initEasyEditText(true, true, false, null, {
                setLoginBtn(it.length > (if (mType == 0) 5 else 3))
            })
            eet.et.hint = getString(R.string.bus_login_password)
            text_view_change.text = getString(R.string.bus_login_code_login)
            text_view_login.text = getString(R.string.bus_login_login)
        } else {
            eet.visibility = View.GONE
            text_view_change.text = getString(R.string.bus_login_password_login)
            if(isDuringCountDown){
                text_view_login.text = countDownText
            }else{
                text_view_login.text = getString(R.string.bus_get_sms_code)
            }
            setLoginBtn(isDuringCountDown.not())
        }
    }

    private fun setLoginBtn(contentOK: Boolean) {
        if (contentOK) {
            text_view_login.isEnabled = true
            text_view_login.background = getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
        } else {
            text_view_login.isEnabled = false
            text_view_login.background = getSimpleDrawable(R.drawable.common_corners_trans_d4d6d9_6_0)
        }
    }

    // 最后一次点击退出的时间
    private var mLastExitTime: Long = 0

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

    override fun loginSuccessToPerfect(str: String?) {
        toast(str.toString())
        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_LOGIN_PERFECT_INFO).navigation()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mUpdatePresenterImpl?.cancel()

        countDownTimer?.let {

            it.cancel()

            countDownTimer= null
        }
    }

    override fun registerSuccess(str: String?) {
        dialog?.dismiss()
        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_LOGIN_PERFECT_INFO).navigation()
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                GET_SMSCODE_RESULT -> {
                    mPresenter?.setCountDown(data?.getLongExtra(GET_SMSCODE_DATA_TIME, 0) ?: 0L)
                }
            }
        }
    }


    private var isDuringCountDown = false
    private var countDownText = ""
    private var countDownTimer: CountDownTimer? = null

    private fun startGetSmsCodeCountDown(totalTimeSecond: Int){

        isDuringCountDown = true

        text_view_login.isEnabled = false
        text_view_login.background = getSimpleDrawable(R.drawable.common_corners_trans_d4d6d9_6_0)

        countDownText = "${totalTimeSecond}S"

        text_view_login.text = countDownText


        countDownTimer = object : CountDownTimer(totalTimeSecond * 1000L,1000){

            override fun onTick(millisUntilFinished: Long) {

                countDownText ="${millisUntilFinished/1000%60}S"

                if(mType == 1){
                    text_view_login.text = countDownText
                }

            }

            override fun onFinish() {



                if(mType == 1){
                    text_view_login.text = getString(R.string.bus_get_sms_code)
                }

                setLoginBtn(isDuringCountDown.not())

                isDuringCountDown = false
                countDownTimer = null

            }
        }.start()

    }


}