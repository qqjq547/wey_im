package framework.telegram.business.ui.me

import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.PhoneChangeFirstContract
import framework.telegram.business.ui.me.presenter.PhoneChangeFirstPresenterImpl
import framework.telegram.business.utils.UserInfoCheckUtil
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.toast
import kotlinx.android.synthetic.main.bus_me_activity_change_phone_first.*
import kotlinx.android.synthetic.main.bus_me_activity_item_edit.custom_toolbar
import android.text.InputFilter
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.KeyboardktUtils


/**
 * Created by lzh on 19-6-7.
 * INFO: 改变手机号绑定，第一步
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_CHANGE_PHONE_FIRST)
class PhoneChangeFirstActivity : BaseBusinessActivity<PhoneChangeFirstContract.Presenter>(), PhoneChangeFirstContract.View {

    private var mType: Int = 1 //0:密码登录   1：验证码登录
    val accountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    override fun getLayoutId() = R.layout.bus_me_activity_change_phone_first

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.change_to_mobile_phone))
        val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
        text_view_phone.text = "${accountInfo.getCountryCode()} ${accountInfo.getPhone()}"
        changeLoginMode()
    }

    override fun initListen() {
        text_view_change.setOnClickListener {
            mType = if (mType == 0) 1 else 0
            changeLoginMode()
        }

        text_view_login.setOnClickListener {
            val text = eet.et.text.toString().trim()
            if (mType == 0) {
                if (!UserInfoCheckUtil.checkPassword(this, text))
                    return@setOnClickListener
                mPresenter?.checkPassword(text)
            } else {
                if (!UserInfoCheckUtil.checkSmsCode(this, text))
                    return@setOnClickListener
                mPresenter?.checkCode(accountInfo.getPhone(), accountInfo.getCountryCode(), text)
            }
        }

        //点击任意地方隐藏键盘
        linear_layout_all.setOnClickListener {
            KeyboardktUtils.hideKeyboard(linear_layout_all)
        }
    }

    override fun initData() {
        PhoneChangeFirstPresenterImpl(this, this, lifecycle())
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@PhoneChangeFirstActivity,this@PhoneChangeFirstActivity)
    }

    override fun showErrMsg(str: String?) {
        dialog?.dismiss()
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as PhoneChangeFirstPresenterImpl
    }

    override fun isActive() = true

    private fun changeLoginMode() {
        if (mType == 0) {
            eet.initEasyEditText(true,true,false,null,null)
            eet.et.hint = getString(R.string.bus_login_password)
            text_view_change.text = getString(R.string.verification_of_mobile_phone)
            text_view_phone.visibility = View.GONE
            eet.et.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(24))
            eet.et.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        } else {
            eet.initEasyEditText(true,false,true,{
                mPresenter?.sendCode(accountInfo.getPhone(), accountInfo.getCountryCode())
                return@initEasyEditText true
            },null)
            eet.et.hint = getString(R.string.bus_login_code)
            eet.et.inputType = InputType.TYPE_CLASS_NUMBER
            text_view_change.text = getString(R.string.password_authentification)
            text_view_phone.visibility = View.VISIBLE
            eet.et.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(6))
        }
    }

    override fun sendCodeSuccess(str: String) {
        dialog?.dismiss()
        toast(str.toString())
        if (mType == 1) {
            eet.countDown.sendVerifyCode()
        }
    }

    override fun checkCodeSuccess(str: String) {
        dialog?.dismiss()
        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_ME_CHANGE_PHONE_SECOND).navigation()
    }

    override fun checkPasswordSuccess(password: String) {
        dialog?.dismiss()
        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_ME_CHANGE_PHONE_SECOND).navigation()
    }

}