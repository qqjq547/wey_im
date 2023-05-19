package framework.telegram.business.ui.login

import android.annotation.SuppressLint
import android.text.*
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.event.SelectCountryEvent
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.login.presenter.FindPasswordFirstContract
import framework.telegram.business.ui.login.presenter.FindPasswordFirstPresenterImpl
import framework.telegram.business.utils.UserInfoCheckUtil
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.KeyboardktUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_me_activity_change_phone_second.*
import kotlinx.android.synthetic.main.bus_me_activity_item_edit.custom_toolbar

/**
 * Created by lzh on 19-6-7.
 * INFO: 忘记密码 第一步
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_FIND_PASSWORD_FIRST)
class FindPasswordFirstActivity : BaseBusinessActivity<FindPasswordFirstContract.Presenter>(),
    FindPasswordFirstContract.View {

    override fun isActive() = true

    private var mCountyStr: String = "+55"
    private var mPasswordOK = false
    private var mContentOK = false

    override fun getLayoutId() = R.layout.bus_me_activity_change_phone_second

    override fun initView() {
        custom_toolbar.showCenterTitle(getString(R.string.string_forget_password))
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        eet.initEasyEditText(true, false, true, {
            val phone = edit_text_phone.text.trim().toString()
            if (UserInfoCheckUtil.checkMobile(this, phone, mCountyStr)) {
                mPresenter?.sendCode(phone, mCountyStr)
                return@initEasyEditText true
            }
            return@initEasyEditText false
        }, {
            mContentOK = it.length >= 4
            setLoginBtn()
        })
        eet.et.hint = getString(R.string.bus_login_code)
        eet.et.inputType = InputType.TYPE_CLASS_NUMBER
        text_view_login.text = getString(R.string.next_step)
        text_view_tip.text = getString(R.string.string_check_and_find_password)
        setTextContent()
    }

    @SuppressLint("CheckResult")
    override fun initListen() {
        text_view_country_code.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_LOGIN_SELECT_COUNTRY)
                .navigation()
        }

        //按钮启用逻辑
        edit_text_phone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val str = s.toString()
                mPasswordOK = !TextUtils.isEmpty(str)
                setLoginBtn()
            }
        })

        text_view_login.setOnClickListener {
            val phone = edit_text_phone.text.trim().toString()
            if (!UserInfoCheckUtil.checkMobile(this, phone, mCountyStr))
                return@setOnClickListener

            val code = eet.et.text.toString().trim()
            if (!UserInfoCheckUtil.checkSmsCode(this, code))
                return@setOnClickListener
            mPresenter?.checkCode(phone, mCountyStr, code)
        }

        EventBus.getFlowable(SelectCountryEvent::class.java)
            .bindToLifecycle(this@FindPasswordFirstActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                mCountyStr = event.countryCode
                setTextContent()
            }

        //点击任意地方隐藏键盘
        linear_layout_all.setOnClickListener {
            KeyboardktUtils.hideKeyboard(linear_layout_all)
        }
    }

    override fun initData() {
        FindPasswordFirstPresenterImpl(this, this, lifecycle())
    }

    private fun setLoginBtn() {
        if (mPasswordOK && mContentOK) {
            text_view_login.isEnabled = true
            text_view_login.background =
                getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
        } else {
            text_view_login.isEnabled = false
            text_view_login.background =
                getSimpleDrawable(R.drawable.common_corners_trans_d4d6d9_6_0)
        }
    }

    private fun setTextContent() {
        text_view_country_code.text = mCountyStr
        //如果是中国区号，就限制11位，不然就限制
        when (mCountyStr) {
            "+86" -> {
                edit_text_phone.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(11))
            }
            "+84" -> {
                edit_text_phone.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(9))
            }
            else -> {
                edit_text_phone.filters = arrayOf<InputFilter>()
            }
        }
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(
            this@FindPasswordFirstActivity,
            this@FindPasswordFirstActivity
        )
    }

    override fun sendCodeSuccess(str: String) {
        dialog?.dismiss()
        eet.countDown.sendVerifyCode()
        toast(str)
    }

    override fun checkCodeSuccess(phone: String, countryCode: String, smsCod: String) {
        dialog?.dismiss()
        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_ME_FIND_PASSWORD_SECOND)
            .withString("phone", phone).withString("countryCode", countryCode)
            .withString("smsCode", smsCod).navigation()
        finish()
    }

    override fun showErrMsg(str: String?) {
        dialog?.dismiss()
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        dialog?.dismiss()
        mPresenter = presenter as FindPasswordFirstPresenterImpl
    }
}