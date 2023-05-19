package framework.telegram.business.ui.login

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.login.presenter.SmsCodeContract
import framework.telegram.business.ui.login.presenter.SmsCodePresenterImpl
import framework.telegram.business.utils.UserInfoCheckUtil
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.KeyboardktUtils
import kotlinx.android.synthetic.main.bus_login_activity_sms_code.*


@Route(path = Constant.ARouter.ROUNTE_BUS_LOGIN_GET_SMS_CODE)
class GetSmsCodeActivity : BaseBusinessActivity<SmsCodeContract.Presenter>(), SmsCodeContract.View {

    companion object {
        internal const val GET_SMSCODE_DATA_TIME = "time"
        internal const val GET_SMSCODE_RESULT = 999
    }

    private var preLength_1 = 0
    private var preLength_2 = 0
    private var preLength_3 = 0
    private var preLength_4 = 0
    private var mSendCodeTime = 0L

    private val onKeyListener by lazy {
        View.OnKeyListener { v, keyCode, _ ->
            if (v is EditText) {
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    when (v.id) {
                        R.id.edit_text_phone_1 -> {
                            v.text = null
                        }
                        R.id.edit_text_phone_2 -> {
                            if (TextUtils.isEmpty(v.text.toString())) {
                                edit_text_phone_1.requestFocus()
                                edit_text_phone_1.text = null
                            } else
                                v.text = null
                        }
                        R.id.edit_text_phone_3 -> {
                            if (TextUtils.isEmpty(v.text.toString())) {
                                edit_text_phone_2.requestFocus()
                                edit_text_phone_2.text = null
                            } else
                                v.text = null
                        }
                        R.id.edit_text_phone_4 -> {
                            if (TextUtils.isEmpty(v.text.toString())) {
                                edit_text_phone_3.requestFocus()
                                edit_text_phone_3.text = null
                            } else
                                v.text = null
                        }
                    }
                }
            }
            false
        }
    }

    // 0为登陆, 1为注册
    private val mType: Int by lazy { intent.getIntExtra("smsType", 0) }

    private val mCountryCode: String by lazy { intent.getStringExtra("countryCode") ?: "" }

    private val mPhone: String by lazy { intent.getStringExtra("phone") ?: "" }

    private val mCountDown by lazy { intent.getIntExtra(GET_SMSCODE_DATA_TIME, 60) }

    override fun getLayoutId(): Int {
        return R.layout.bus_login_activity_sms_code
    }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        if (!TextUtils.isEmpty(mCountryCode) && !TextUtils.isEmpty(mPhone)) {
            text_view_phone.text = "$mCountryCode  $mPhone"
            btn_count_down.setCountDownText(
                getString(framework.telegram.ui.R.string.get_code),
                getString(R.string.bus_retry_get_sms_code),
                getString(R.string.bus_counting_down)
            )

            if (mCountDown != 60) {
                mSendCodeTime = System.currentTimeMillis() - (60 - mCountDown) * 1000
            } else {
                mSendCodeTime = System.currentTimeMillis()
            }
            btn_count_down.setCountTime(mCountDown)
            custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
                val intent = Intent()
                intent.putExtra(GET_SMSCODE_DATA_TIME, mSendCodeTime)
                setResult(Activity.RESULT_OK, intent)
                onBackPressed()
            }
            custom_toolbar.setToolbarColor(R.color.white)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            KeyboardktUtils.showKeyboard(edit_text_phone_1)
        } else
            finish()
    }

    override fun initListen() {
        initSMSEditText()

        btn_count_down.setOnClickListener {
            if (btn_count_down.getClickStatus()) {
                mSendCodeTime = System.currentTimeMillis()
                mPresenter?.sendCode(mPhone, mCountryCode, mType)
            }
        }
    }

    override fun initData() {
        SmsCodePresenterImpl(this, this, lifecycle())
        if (UserInfoCheckUtil.checkMobile(this@GetSmsCodeActivity, mPhone, mCountryCode)) {
            btn_count_down.sendVerifyCode()
        }
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@GetSmsCodeActivity, this@GetSmsCodeActivity)
    }

    override fun sendCodeSuccess(str: String?) {
        dialog?.dismiss()
        toast(str.toString())
        btn_count_down.sendVerifyCode()
    }

    override fun loginSuccess(str: String?) {
        dialog?.dismiss()

        ActivitiesHelper.getInstance().closeExcept(GetSmsCodeActivity::class.java)
        ActivitiesHelper.getInstance().lastBackgroundAppTime = 0
        ARouter.getInstance().build("/app/act/main").withBoolean("fromLogin", true).navigation()
        finish()
    }

    override fun registerSuccess(str: String?) {
        dialog?.dismiss()
        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_LOGIN_PERFECT_INFO).navigation()
        finish()
    }

    override fun showErrMsg(str: String?) {
        dialog?.dismiss()
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as SmsCodeContract.Presenter
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        btn_count_down.removeRunnable()
    }

    private fun initSMSEditText() {

        //TODO 可以封装为一个ViewGroup
        edit_text_phone_1.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 1) {
                    edit_text_phone_2.requestFocus()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })

        edit_text_phone_2.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 1) {
                    edit_text_phone_3.requestFocus()
                    preLength_2 = s.length
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })

        edit_text_phone_3.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 1) {
                    edit_text_phone_4.requestFocus()
                    preLength_3 = s.length
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })

        edit_text_phone_4.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 1) {
                    val smsCode =
                        edit_text_phone_1.text.toString() + edit_text_phone_2.text.toString() + edit_text_phone_3.text.toString() + edit_text_phone_4.text.toString()
                    if (smsCode.length == 4) {
                        if (mType == 0) {
                            mPresenter?.loginBySmsCode(mPhone, smsCode, mCountryCode)
                        } else
                            mPresenter?.register(mPhone, smsCode, mCountryCode)
                    }
                    preLength_4 = s.length
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })
        edit_text_phone_1.setOnKeyListener(onKeyListener)
        edit_text_phone_2.setOnKeyListener(onKeyListener)
        edit_text_phone_3.setOnKeyListener(onKeyListener)
        edit_text_phone_4.setOnKeyListener(onKeyListener)
    }
}