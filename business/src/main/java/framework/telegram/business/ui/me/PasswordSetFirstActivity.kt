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
import framework.telegram.business.ui.me.presenter.PasswordSetFirstContract
import framework.telegram.business.ui.me.presenter.PasswordSetFirstPresenterImpl
import framework.telegram.business.ui.me.presenter.PhoneChangeFirstPresenterImpl
import framework.telegram.business.utils.UserInfoCheckUtil
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.KeyboardktUtils
import kotlinx.android.synthetic.main.bus_me_activity_item_edit.custom_toolbar
import kotlinx.android.synthetic.main.bus_me_activity_password_set_first.*

/**
 * Created by lzh on 19-6-7.
 * INFO: 改变手机号绑定，第一步
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_PASSWORD_SET_FIRST)
class PasswordSetFirstActivity : BaseBusinessActivity<PasswordSetFirstContract.Presenter>(), PasswordSetFirstContract.View {
    val accountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    override fun getLayoutId() = R.layout.bus_me_activity_password_set_first

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.set_a_password))
        val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
        text_view_phone.text = "${accountInfo.getCountryCode()} ${accountInfo.getPhone()}"

        eet.initEasyEditText(true,false,true,{
            mPresenter?.sendCode(accountInfo.getPhone(), accountInfo.getCountryCode())
            return@initEasyEditText true
        },{

        })
        eet.et.hint = getString(R.string.bus_login_code)
        eet.et.inputType = InputType.TYPE_CLASS_NUMBER
    }

    override fun initListen() {

        text_view_login.setOnClickListener {
            val text = eet.et.text.toString().trim()
            if (!UserInfoCheckUtil.checkSmsCode(this, text))
                return@setOnClickListener
            mPresenter?.checkPassword(accountInfo.getPhone(), accountInfo.getCountryCode(), text)

        }

        linear_layout_all.setOnClickListener {
            KeyboardktUtils.hideKeyboard(linear_layout_all)
        }
    }

    override fun initData() {
        PasswordSetFirstPresenterImpl(this, this, lifecycle())
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@PasswordSetFirstActivity,this@PasswordSetFirstActivity)
    }

    override fun showErrMsg(str: String?) {
        dialog?.dismiss()
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as PasswordSetFirstPresenterImpl
    }

    override fun isActive() = true

    override fun sendCodeSuccess(str: String) {
        dialog?.dismiss()
        eet.countDown.sendVerifyCode()
        toast(str.toString())
    }

    override fun checkPasswordSuccess(password: String) {
        dialog?.dismiss()
        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_ME_PASSWORD_SET_SECOND).navigation()
    }

}