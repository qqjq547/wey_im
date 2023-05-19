package framework.telegram.business.ui.me

import android.text.InputType
import android.text.TextUtils
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.BusinessApplication
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.PasswordChangeContract
import framework.telegram.business.ui.me.presenter.PasswordChangePresenterImpl
import framework.telegram.business.utils.UserInfoCheckUtil
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.KeyboardktUtils
import kotlinx.android.synthetic.main.bus_me_activity_item_edit.custom_toolbar
import kotlinx.android.synthetic.main.bus_me_activity_password_change.*

/**
 * Created by lzh on 19-6-7.
 * INFO: 修改密码
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_PASSWORD_CHANGE)
class PasswordChangeActivity : BaseBusinessActivity<PasswordChangeContract.Presenter>(), PasswordChangeContract.View {
    val accountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    private var mType: Int = 1 //0:密码登录   1：验证码登录

    private var mContextOk = false
    private var mContextOk1 = false
    private var mContextOk2 = false
    private var mContextOk3 = false

    override fun getLayoutId() = R.layout.bus_me_activity_password_change

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.change_password))
        val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
        text_view_phone.text = "${accountInfo.getCountryCode()} ${accountInfo.getPhone()}"

        //验证码
        eet_0.initEasyEditText(true,false,true,{
            mPresenter?.sendCode(accountInfo.getPhone(), accountInfo.getCountryCode())
            true
        },{
            mContextOk = !TextUtils.isEmpty(it)
            setLoginBtn()
        })
        eet_0.et.hint = getString(R.string.bus_login_code)
        eet_0.et.inputType = InputType.TYPE_CLASS_NUMBER

        //旧密码
        eet_1.initEasyEditText(true, true, false,null,{
            mContextOk1 = !TextUtils.isEmpty(it)
            setLoginBtn()
        })
        eet_1.et.hint = getString(R.string.input_old_password)

        //新密码
        eet_2.initEasyEditText(true, true, false,null,{
            mContextOk2 = !TextUtils.isEmpty(it)
            setLoginBtn()
        })
        eet_2.et.hint = getString(R.string.enter_new_password)

        //再次输入新密码
        eet_3.initEasyEditText(true, true, false,null,{
            mContextOk3 = !TextUtils.isEmpty(it)
            setLoginBtn()
        })
        eet_3.et.hint = getString(R.string.retype_new_password)

        changeLoginMode()
    }

    override fun initListen() {
        text_view_change.setOnClickListener {
            mType = if (mType == 0) 1 else 0
            changeLoginMode()
        }

        text_view_login.setOnClickListener {
            if (mType == 0) {
                val text1 = eet_1.et.text.toString().trim()
                val text2 = eet_2.et.text.toString().trim()
                val text3 = eet_3.et.text.toString().trim()
                if (!UserInfoCheckUtil.checkPassword(this, text1))
                    return@setOnClickListener
                if (!UserInfoCheckUtil.checkPassword(this, text2))
                    return@setOnClickListener
                if (!UserInfoCheckUtil.checkPassword(this, text3))
                    return@setOnClickListener
                if (!UserInfoCheckUtil.doubleCheckPassword(this, text2, text3))
                    return@setOnClickListener
                mPresenter?.updataPasswordByPassword(text1, text2)
            } else {
                val text0 = eet_0.et.text.toString().trim()
                val text2 = eet_2.et.text.toString().trim()
                if (!UserInfoCheckUtil.checkSmsCode(this, text0))
                    return@setOnClickListener
                if (!UserInfoCheckUtil.checkPassword(this, text2))
                    return@setOnClickListener
                mPresenter?.updataPasswordBySmsCode(accountInfo.getCountryCode(), accountInfo.getPhone(), text0, text2)
            }
        }

        linear_layout_all.setOnClickListener {
            KeyboardktUtils.hideKeyboard(linear_layout_all)
        }
    }

    override fun initData() {
        PasswordChangePresenterImpl(this, this, lifecycle())
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@PasswordChangeActivity, this@PasswordChangeActivity)
    }

    override fun showErrMsg(str: String?) {
        dialog?.dismiss()
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as PasswordChangePresenterImpl
    }

    override fun isActive() = true

    /**
     * 切换手机验证，密码验证
     */
    private fun changeLoginMode() {
        if (mType == 0) {
            text_view_change.text = getString(R.string.verification_of_mobile_phone)
            text_view_phone.visibility = View.GONE
            eet_0.visibility = View.GONE
            eet_1.visibility = View.VISIBLE
            eet_3.visibility = View.VISIBLE
        } else {
            text_view_change.text = getString(R.string.bus_login_password_login)
            text_view_change.text = getString(R.string.password_authentification)
            text_view_phone.visibility = View.VISIBLE
            eet_0.visibility = View.VISIBLE
            eet_1.visibility = View.GONE
            eet_3.visibility = View.GONE
        }
        eet_0.clear()
        eet_1.clear()
        eet_2.clear()
        eet_3.clear()
    }

    override fun sendCodeSuccess(str: String) {
        dialog?.dismiss()
        eet_0.countDown.sendVerifyCode()
        toast(str.toString())
    }

    private fun setLoginBtn() {
        if (mType == 0 && mContextOk1 && mContextOk2 && mContextOk3) {
            text_view_login.isEnabled = true
            text_view_login.background = getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
        } else if (mType == 1 && mContextOk && mContextOk2) {
            text_view_login.isEnabled = true
            text_view_login.background = getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
        } else {
            text_view_login.isEnabled = false
            text_view_login.background = getSimpleDrawable(R.drawable.common_corners_trans_d4d6d9_6_0)
        }
    }

    override fun updataPasswordByPasswordSuccess(password: String) {
        dialog?.dismiss()
        BusinessApplication.setPassword(password)
        finish()
    }

    override fun updataPasswordBySmsCode(countryCode: String, phone: String, smsCode: String, password: String) {
        dialog?.dismiss()
        BusinessApplication.setPassword(password)
        finish()
    }
}