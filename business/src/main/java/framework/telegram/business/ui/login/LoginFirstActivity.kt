package framework.telegram.business.ui.login

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity.CENTER
import android.view.Gravity.RIGHT
import android.view.View
import android.widget.FrameLayout
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.telegram.business.R
import framework.telegram.business.UpdatePresenterImpl
import framework.telegram.business.bridge.Constant
import framework.telegram.business.event.SelectCountryEvent
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.login.GetSmsCodeActivity.Companion.GET_SMSCODE_DATA_TIME
import framework.telegram.business.ui.login.GetSmsCodeActivity.Companion.GET_SMSCODE_RESULT
import framework.telegram.business.ui.login.presenter.LoginContract
import framework.telegram.business.ui.login.presenter.LoginPresenterImpl
import framework.telegram.business.utils.UserInfoCheckUtil
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.event.EventBus
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
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_login_activity_first.*

/**
 * Created by lzh on 19-5-16.
 * INFO:登录
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_LOGIN_FIRST)
class LoginFirstActivity : BaseBusinessActivity<LoginContract.Presenter>(), LoginContract.View {

    override fun getLayoutId() = R.layout.bus_login_activity_first

    private val type by lazy { intent.getIntExtra("type", 1) }
    private val mIsCanCancel by lazy { intent.getBooleanExtra("isCancel", false) }
    private val mDefaultAreaCode by lazy { intent.getStringExtra("area_code") ?: "" }

    //0:密码登录   1：验证码登录
    private var mType = 1
    private var mCountyStr: String = "+55"
    private var mPasswordOK = false
    private var mContentOK = false
    private var mPasswordType = 0

    // 最后一次点击退出的时间
    private var mLastExitTime: Long = 0

    private var mUpdatePresenterImpl: UpdatePresenterImpl? = null

    private var mFirstLoad = true

    //获取屏幕的高度
    private val mScreenHeight by lazy { this@LoginFirstActivity.window.decorView.rootView.height }

    override fun isActive() = isFinishing

    override fun initView() {
        mType = type

        custom_toolbar.showRightTextView(
            getString(R.string.bus_login_register),
            object : () -> Unit {
                override fun invoke() {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_LOGIN_REGISTER)
                        .withString("phone", edit_text_phone.text.toString())
                        .withString("area_code", text_view_country_code.text.toString())
                        .navigation()
                }
            })

        custom_toolbar.setToolbarColor(R.color.white)

        text_view_login.isEnabled = false

        if (mIsCanCancel) {
            custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
                finish()
            }
        }

        if (!TextUtils.isEmpty(mDefaultAreaCode)) {
            mCountyStr = mDefaultAreaCode
        }

        changeLoginMode()
        setTextContent()

        if (!mIsCanCancel) {
            mUpdatePresenterImpl = UpdatePresenterImpl(this, this, lifecycle())
            mUpdatePresenterImpl?.start(showCanUpDialog = true, showNotUpdateDialog = false)
        }

        requestBasicPermission()
    }

    /******************权限*****************/
    private val BASIC_PERMISSIONS = arrayOf<String>(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private fun requestBasicPermission() {
        MPermission.with(this@LoginFirstActivity)
            .setRequestCode(Constant.Permission.BASIC_PERMISSION_REQUEST_CODE)
            .permissions(*BASIC_PERMISSIONS)
            .request()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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
        AppDialog.show(this@LoginFirstActivity, this@LoginFirstActivity) {
            message(text = context.getString(R.string.authorization_failed_fail))
            cancelOnTouchOutside(false)
            cancelable(false)
            negativeButton(text = context.getString(R.string.pet_text_508), click = {
                requestBasicPermission()
            })
            positiveButton(text = getString(R.string.pet_text_195), click = {
                ActivitiesHelper.getInstance().closeAll()
                JumpPermissionManagement.GoToSetting(this@LoginFirstActivity)
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
                AppDialog.show(this@LoginFirstActivity, this@LoginFirstActivity) {
                    positiveButton(text = getString(R.string.confirm))
                    message(text = tipMsg)
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    override fun initListen() {
        linear_layout_all.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            //获取当前界面可视部分
            this@LoginFirstActivity.window.decorView.getWindowVisibleDisplayFrame(r)
            //此处就是用来获取键盘的高度的， 在键盘没有弹出的时候 此高度为0 键盘弹出的时候为一个正数
            val heightDifference = mScreenHeight - r.bottom
            val textY = mScreenHeight - frame_layout.bottom
            val navHeight = NavBarUtils.getNavigationBarHeight(this@LoginFirstActivity)
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

        linear_layout_all.setOnClickListener {
            KeyboardktUtils.hideKeyboard(linear_layout_all)
        }

        text_view_login.setOnClickListener {
            val phone = edit_text_phone.text.trim().toString()
            val text = eet.et.text.toString().trim()
            if (!UserInfoCheckUtil.checkMobile(this, phone, mCountyStr))
                return@setOnClickListener
            if (mType == 0) {
                if (!UserInfoCheckUtil.checkPassword(this, text)) {
                    return@setOnClickListener
                }

                mPresenter?.loginByPassword(phone, text, mCountyStr)
            } else {
                mPresenter?.sendCode(phone, mCountyStr)
            }
        }

        text_view_country_code.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_LOGIN_SELECT_COUNTRY)
                .navigation()
        }

        edit_text_phone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val str = s.toString()
                mContentOK = !TextUtils.isEmpty(str)
                setLoginBtn()
            }
        })

        text_view_forget_password.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_ME_FIND_PASSWORD_FIRST)
                .navigation()
        }

        EventBus.getFlowable(SelectCountryEvent::class.java)
            .bindToLifecycle(this@LoginFirstActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                mCountyStr = event.countryCode
                setTextContent()
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
        dialog = AppDialog.showLoadingView(this@LoginFirstActivity, this@LoginFirstActivity)
    }

    override fun loginSuccess(str: String?) {
        dialog?.dismiss()

        ActivitiesHelper.getInstance().closeExcept(LoginFirstActivity::class.java)
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
            .withString("countryCode", mCountyStr)
            .withString("phone", edit_text_phone.text.trim().toString())
            .withInt("smsType", 0)
            .withInt(GET_SMSCODE_DATA_TIME, time)
            .navigation(this@LoginFirstActivity, GET_SMSCODE_RESULT)
    }

    override fun showErrMsg(str: String?, showDialog: Boolean) {
        dialog?.dismiss()
        if (showDialog) {
            AppDialog.show(this@LoginFirstActivity, this@LoginFirstActivity) {
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
                mPasswordOK = it.length > (if (mType == 0) 5 else 3)
                setLoginBtn()
            })
            text_view_forget_password.visibility = View.VISIBLE
            (text_view_change.layoutParams as FrameLayout.LayoutParams).gravity = RIGHT
            eet.et.hint = getString(R.string.bus_login_password)
            text_view_change.text = getString(R.string.bus_login_code_login)
            text_view_login.text = getString(R.string.bus_login_login)
        } else {
            eet.visibility = View.GONE
            text_view_change.text = getString(R.string.bus_login_password_login)
            text_view_login.text = getString(R.string.bus_get_sms_code)
            (text_view_change.layoutParams as FrameLayout.LayoutParams).gravity = CENTER
            text_view_forget_password.visibility = View.GONE
            setLoginBtn()
        }
    }


    override fun onBackPressed() {
        if (mIsCanCancel) {
            super.onBackPressed()
        } else {
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

    private fun setTextContent() {
        text_view_country_code.text = mCountyStr
        //如果是中国区号，就限制11位，不然就限制
        if ("+86" == mCountyStr) {
            edit_text_phone.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(11))
        } else if ("+84" == mCountyStr) {
            edit_text_phone.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(9))
        } else {
            edit_text_phone.filters = arrayOf<InputFilter>()
        }
    }

    private fun setLoginBtn() {
        if (mPasswordOK && mContentOK && mType == 0) {
            text_view_login.isEnabled = true
            text_view_login.background =
                getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
        } else if (mContentOK && mType != 0) {// 验证码


            if(isDuringCountDown == false){ // 非倒计时期间
                text_view_login.isEnabled = true
                text_view_login.background =
                    getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
            }

        } else {
            text_view_login.isEnabled = false
            text_view_login.background =
                getSimpleDrawable(R.drawable.common_corners_trans_d4d6d9_6_0)
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

    private fun startGetSmsCodeCountDown(totalTimeSecond: Int){

        isDuringCountDown = true

        text_view_login.isEnabled = false
        text_view_login.background = getSimpleDrawable(R.drawable.common_corners_trans_d4d6d9_6_0)

        text_view_login.text = "contagem regressiva:" + totalTimeSecond + "S"


        object : CountDownTimer(totalTimeSecond * 1000L,1000){

            override fun onTick(millisUntilFinished: Long) {
                text_view_login.text = "contagem regressiva:" + millisUntilFinished/1000%60 + "S"
            }

            override fun onFinish() {

                text_view_login.text = getString(R.string.bus_get_sms_code)

                isDuringCountDown = false

                setLoginBtn()
            }
        }.start()

    }
}