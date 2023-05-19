package framework.telegram.business.ui.me

import android.app.Activity
import android.text.InputType
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.utils.UserInfoCheckUtil
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.toast
import kotlinx.android.synthetic.main.bus_me_activity_change_phone_first.*
import kotlinx.android.synthetic.main.bus_me_activity_item_edit.custom_toolbar
import android.text.InputFilter
import framework.telegram.business.ui.me.presenter.ForgetAppLockContract
import framework.telegram.business.ui.me.presenter.ForgetAppLockPresenterImpl
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.KeyboardktUtils
import kotlinx.android.synthetic.main.bus_me_activity_change_phone_first.eet
import kotlinx.android.synthetic.main.bus_me_activity_change_phone_first.linear_layout_all
import kotlinx.android.synthetic.main.bus_me_activity_change_phone_first.text_view_phone
import kotlinx.android.synthetic.main.bus_me_activity_forget_applock.*

@Route(path = Constant.ARouter.ROUNTE_BUS_ME_APPLOCK_FORGET)
class ForgetAppLockActivity : BaseBusinessActivity<ForgetAppLockContract.Presenter>(), ForgetAppLockContract.View {

    private val accountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    override fun getLayoutId() = R.layout.bus_me_activity_forget_applock

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.forget_applock_password))
        val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
        text_view_phone.text = "${accountInfo.getCountryCode()} ${accountInfo.getPhone()}"
        changeLoginMode()
    }

    override fun initListen() {
        text_view_next.setOnClickListener {
            val text = eet.et.text.toString().trim()
            if (!UserInfoCheckUtil.checkSmsCode(this, text))
                return@setOnClickListener

            mPresenter?.checkCode(accountInfo.getPhone(), accountInfo.getCountryCode(), text)
        }

        //点击任意地方隐藏键盘
        linear_layout_all.setOnClickListener {
            KeyboardktUtils.hideKeyboard(linear_layout_all)
        }
    }

    override fun initData() {
        ForgetAppLockPresenterImpl(this, this, lifecycle())
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@ForgetAppLockActivity, this@ForgetAppLockActivity)
    }

    override fun showErrMsg(str: String?) {
        dialog?.dismiss()
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as ForgetAppLockPresenterImpl
    }

    override fun isActive() = true

    private fun changeLoginMode() {
        eet.initEasyEditText(true, false, true, {
            mPresenter?.sendCode(accountInfo.getPhone(), accountInfo.getCountryCode())
            return@initEasyEditText true
        }, null)
        eet.et.hint = getString(R.string.bus_login_code)
        eet.et.inputType = InputType.TYPE_CLASS_NUMBER
        text_view_phone.visibility = View.VISIBLE
        eet.et.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(6))
    }

    override fun sendCodeSuccess(str: String) {
        dialog?.dismiss()
        toast(str)
        eet.countDown.sendVerifyCode()
    }

    override fun checkCodeSuccess(str: String) {
        dialog?.dismiss()
        setResult(Activity.RESULT_OK)
        finish()
    }
}