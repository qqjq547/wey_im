package framework.telegram.business.ui.login

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.webview.WebUrlConfig
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.event.SelectCountryEvent
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.login.GetSmsCodeActivity.Companion.GET_SMSCODE_DATA_TIME
import framework.telegram.business.ui.login.GetSmsCodeActivity.Companion.GET_SMSCODE_RESULT
import framework.telegram.business.ui.login.presenter.RegisterContract
import framework.telegram.business.ui.login.presenter.RegisterPresenterImpl

import framework.telegram.business.utils.UserInfoCheckUtil
import framework.telegram.business.utils.ValidationUtils
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.JumpPermissionManagement
import framework.telegram.support.tools.language.LocalManageUtil
import framework.telegram.support.tools.permission.MPermission
import framework.telegram.support.tools.permission.annotation.OnMPermissionDenied
import framework.telegram.support.tools.permission.annotation.OnMPermissionGranted
import framework.telegram.support.tools.permission.annotation.OnMPermissionNeverAskAgain
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.KeyboardktUtils
import framework.telegram.ui.utils.NavBarUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_login_activity_first.*
import kotlinx.android.synthetic.main.bus_login_activity_first.view.*
import kotlinx.android.synthetic.main.bus_login_activity_register.*
import kotlinx.android.synthetic.main.bus_login_activity_register.custom_toolbar
import kotlinx.android.synthetic.main.bus_login_activity_register.edit_text_phone
import kotlinx.android.synthetic.main.bus_login_activity_register.linear_layout_all
import kotlinx.android.synthetic.main.bus_login_activity_register.text_view_country_code
import kotlinx.android.synthetic.main.bus_me_activity_password_change.*


/**
 * Created by lzh on 19-5-16.
 * INFO: 注册
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_LOGIN_REGISTER)
class RegisterActivity : BaseBusinessActivity<RegisterContract.Presenter>(), RegisterContract.View {


    //注册模式, 0:短信（默认）， 1:密码
    private val REGISTER_TYPE_SMS_CODE = 0
    private val REGISTER_TYPE_PWD = 1



    private val registerType = 1

    private var mCountyStr: String = "+44"
    private var mPasswordOK = false

    private val mDefaultPhone by lazy { intent.getStringExtra("phone") ?: "" }
    private val mDefaultAreaCode by lazy { intent.getStringExtra("area_code") ?: "" }

    override fun isActive() = isFinishing

    //获取屏幕的高度
    private val mScreenHeight by lazy { this@RegisterActivity.window.decorView.rootView.height }

    override fun getLayoutId() = R.layout.bus_login_activity_register

    override fun initView() {

        if(registerType == REGISTER_TYPE_PWD){
            tv_send_sms_code.visibility = View.GONE
            set_pwd_layout.visibility = View.VISIBLE
        }

        if(registerType == REGISTER_TYPE_SMS_CODE){
            tv_send_sms_code.visibility = View.VISIBLE
            set_pwd_layout.visibility = View.GONE
        }

        tv_send_sms_code.isEnabled = false

        edit_text_phone.setSelection(edit_text_phone.text.toString().length)

        custom_toolbar?.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        mCountyStr = if (mDefaultAreaCode == null) LocalManageUtil.getCurrentCountryCode() else mDefaultAreaCode
        setTextContent()

        custom_toolbar.setToolbarColor(R.color.white)

        requestBasicPermission()


        //密码
        edit_pwd1.initEasyEditText(true, true, false,null) {

            setRegisterByPwdBtn()
        }
        edit_pwd1.et.hint = getString(R.string.bus_login_password_input_error_2)

        //再次输入新密码
        edit_pwd2.initEasyEditText(true, true, false,null) {
            setRegisterByPwdBtn()
        }
        edit_pwd2.et.hint = getString(R.string.confirm_password)

    }

    /******************权限*****************/
    private val BASIC_PERMISSIONS = arrayOf<String>(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private fun requestBasicPermission() {
        MPermission.with(this@RegisterActivity)
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
        AppDialog.show(this@RegisterActivity, this@RegisterActivity) {
            message(text = context.getString(R.string.authorization_failed_fail))
            cancelOnTouchOutside(false)
            cancelable(false)
            negativeButton(text = context.getString(R.string.pet_text_508), click = {
                requestBasicPermission()
            })
            positiveButton(text = getString(R.string.pet_text_195), click = {
                ActivitiesHelper.getInstance().closeAll()
                JumpPermissionManagement.GoToSetting(this@RegisterActivity)
            })
            title(text = context.getString(R.string.hint))
        }
    }

    /******************权限  end*****************/

    @SuppressLint("CheckResult")
    override fun initListen() {

        text_view_country_code.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_LOGIN_SELECT_COUNTRY)
                .navigation()
        }

        text_view_more.setOnClickListener {
            val i = Intent(Intent.ACTION_VIEW)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            i.data = Uri.parse(getUrl())
            startActivity(i)
        }

        linear_layout_all.setOnClickListener {
            KeyboardktUtils.hideKeyboard(linear_layout_all)
        }

        tv_send_sms_code.setOnClickListener {
            val phone = edit_text_phone.text.trim().toString()
            if (!UserInfoCheckUtil.checkMobile(this, phone, mCountyStr))
                return@setOnClickListener
            mPresenter?.sendCode(phone, mCountyStr)
        }

        edit_text_phone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(str: Editable?) {
                mPasswordOK = !TextUtils.isEmpty(str)
                setRegisterBtn()

                setRegisterByPwdBtn()
            }
        })

        EventBus.getFlowable(SelectCountryEvent::class.java)
            .bindToLifecycle(this@RegisterActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                mCountyStr = event.countryCode
                setTextContent()
            }

        linear_layout_all.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            //获取当前界面可视部分
            this@RegisterActivity.window.decorView.getWindowVisibleDisplayFrame(r)
            //此处就是用来获取键盘的高度的， 在键盘没有弹出的时候 此高度为0 键盘弹出的时候为一个正数
            val heightDifference = mScreenHeight - r.bottom
            val textY = mScreenHeight - view1.bottom
            val navHeight = NavBarUtils.getNavigationBarHeight(this@RegisterActivity)
            if (heightDifference - navHeight > 0) {
                linear_layout_all.translationY = 0 - (heightDifference - textY).toFloat()
                custom_toolbar.androidMTransparency(true)
            } else {
                linear_layout_all.translationY = 0f
                custom_toolbar.androidMTransparency(false)
            }
        }
        edit_text_phone.setText(mDefaultPhone)




        //--------------------

        tv_register_by_pwd.setOnClickListener {

            mPresenter?.registerByPwd(mCountyStr, edit_text_phone.text.trim().toString(), edit_pwd1.et.text.toString().trim())

            /*if (!UserInfoCheckUtil.checkMobile(this, edit_text_phone.text.toString().trim(), mCountyStr)){

                return@setOnClickListener

            }

            val pwd1 = edit_pwd1.et.text.toString().trim()
            val pwd2 = edit_pwd2.et.text.toString().trim()

            if(checkPassword(pwd1) && checkPassword(pwd2) && doubleCheckPassword(pwd1,pwd2)){
                mPresenter?.registerByPwd(mCountyStr, edit_text_phone.text.trim().toString(), pwd1)
            }*/
        }
    }

    private fun setRegisterByPwdBtn(){

        val pwd1 = edit_pwd1.et.text.toString().trim()
        val pwd2 = edit_pwd2.et.text.toString().trim()

        if (UserInfoCheckUtil.checkMobile2(this, edit_text_phone.text.toString().trim(), mCountyStr) && checkPassword(pwd1) && checkPassword(pwd2) && doubleCheckPassword(pwd1,pwd2)){

            tv_register_by_pwd.isEnabled = true
            tv_register_by_pwd.background = getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)

        }else{
            tv_register_by_pwd.isEnabled = false
            tv_register_by_pwd.background = getSimpleDrawable(R.drawable.common_corners_trans_d4d6d9_6_0)
        }
    }


    fun checkPassword(password: String): Boolean {
        if (ValidationUtils.isEmpty(password)) {
            //toast(getString(R.string.bus_login_password_input_error_1))
            return false
        }
        if (password.length < 6 || password.length > 24) {
            //toast(getString(R.string.bus_login_password_input_error_2))
            return false
        }
        return true
    }


    /**
     * 检查密码
     * @param context
     * @return
     */
    fun doubleCheckPassword(pwd1: String, pwd2: String): Boolean {
        if (pwd1 != pwd2) {
            // toast(getString(R.string.bus_login_password_input_error_3))
            return false
        }
        return true
    }


    private fun getUrl(): String {
        val url = Uri.parse(
            if (!TextUtils.isEmpty(Constant.Common.DOWNLOAD_HTTP_HOST)) {
                Constant.Common.DOWNLOAD_HTTP_HOST
            } else {
                "https://www.bufa.chat"
            } + WebUrlConfig.userProtocolUrl_en
        )
        return url.toString()
    }

    override fun initData() {
        setRegisterByPwdBtn()
        RegisterPresenterImpl(this, this, lifecycle()).start()
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as RegisterContract.Presenter
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@RegisterActivity, this@RegisterActivity)
    }

    override fun registerSuccess(str: String?) {
        dialog?.dismiss()
        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_LOGIN_PERFECT_INFO).navigation()
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
            .withInt(GET_SMSCODE_DATA_TIME, time)
            .withInt("smsType", 1)
            .navigation(this@RegisterActivity, GET_SMSCODE_RESULT)
    }

    override fun showErrMsg(str: String?) {
        dialog?.dismiss()
        toast(str ?: "")
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

    private fun setRegisterBtn() {

        if(isDuringCountDown.not()){
            if (mPasswordOK) {
                tv_send_sms_code.isEnabled = true
                tv_send_sms_code.background =
                    getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
            } else {
                tv_send_sms_code.isEnabled = false
                tv_send_sms_code.background =
                    getSimpleDrawable(R.drawable.common_corners_trans_d4d6d9_6_0)
            }
        }
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


    private var countDownTimer: CountDownTimer? = null

    private var isDuringCountDown = false

    private fun startGetSmsCodeCountDown(totalTimeSecond: Int) {

        isDuringCountDown = true

        tv_send_sms_code.isEnabled = false
        tv_send_sms_code.background =
            getSimpleDrawable(R.drawable.common_corners_trans_d4d6d9_6_0)
        tv_send_sms_code.text = "${totalTimeSecond}s"


        countDownTimer = object : CountDownTimer(totalTimeSecond * 1000L, 1000) {

            override fun onTick(millisUntilFinished: Long) {

                tv_send_sms_code.text ="${millisUntilFinished / 1000 % 60}s"
            }

            override fun onFinish() {

                tv_send_sms_code.text = getString(R.string.bus_get_sms_code)

                setRegisterBtn()

                countDownTimer = null

                isDuringCountDown = false
            }
        }.start()

    }


    override fun onDestroy() {
        super.onDestroy()

        countDownTimer?.let {

            it.cancel()

            countDownTimer= null
        }
    }

}