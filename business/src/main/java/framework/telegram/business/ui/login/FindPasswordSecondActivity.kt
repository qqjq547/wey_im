package framework.telegram.business.ui.login

import android.text.TextUtils
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.login.presenter.FindPasswordSecondContract
import framework.telegram.business.ui.login.presenter.FirstPasswordSecondPresenterImpl
import framework.telegram.business.utils.UserInfoCheckUtil
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.KeyboardktUtils
import kotlinx.android.synthetic.main.bus_me_activity_find_password.*

/**
 * Created by lzh on 19-6-7.
 * INFO: 忘记密码 第二步
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_FIND_PASSWORD_SECOND)
class FindPasswordSecondActivity : BaseBusinessActivity<FindPasswordSecondContract.Presenter>(),
    FindPasswordSecondContract.View {

    val accountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    private var mContextOk = false
    private var mContextOk1 = false

    private val mPhone by lazy { intent.getStringExtra("phone") ?: "" }
    private val mCountryCode by lazy { intent.getStringExtra("countryCode") ?: "" }
    private val mSmsCode by lazy { intent.getStringExtra("smsCode") ?: "" }

    override fun getLayoutId() = R.layout.bus_me_activity_find_password

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.reset_password))
        val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
        text_view_phone.text = mCountryCode + " " + mPhone

        //验证码
        eet_0.initEasyEditText(true, true, false, null, {
            mContextOk = !TextUtils.isEmpty(it)
            setLoginBtn()
        })
        eet_0.et.hint = getString(R.string.enter_new_password)

        //旧密码
        eet_1.initEasyEditText(true, true, false, null, {
            mContextOk1 = !TextUtils.isEmpty(it)
            setLoginBtn()
        })
        eet_1.et.hint = getString(R.string.retype_new_password)

    }

    override fun initListen() {
        text_view_login.setOnClickListener {
            val text1 = eet_0.et.text.toString().trim()
            val text2 = eet_1.et.text.toString().trim()
            if (!UserInfoCheckUtil.checkPassword(this, text1))
                return@setOnClickListener
            if (!UserInfoCheckUtil.checkPassword(this, text2))
                return@setOnClickListener
            if (!UserInfoCheckUtil.doubleCheckPassword(this, text1, text2))
                return@setOnClickListener
            mPresenter?.sure(mPhone, mCountryCode, mSmsCode, text1)
        }

        linear_layout_all.setOnClickListener {
            KeyboardktUtils.hideKeyboard(linear_layout_all)
        }
    }

    override fun initData() {
        FirstPasswordSecondPresenterImpl(this, this, lifecycle())
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(
            this@FindPasswordSecondActivity,
            this@FindPasswordSecondActivity
        )
    }

    override fun showErrMsg(str: String?) {
        dialog?.dismiss()
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as FirstPasswordSecondPresenterImpl
    }

    override fun isActive() = true

    private fun setLoginBtn() {
        if (mContextOk && mContextOk1) {
            text_view_login.isEnabled = true
            text_view_login.background =
                getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
        } else {
            text_view_login.isEnabled = false
            text_view_login.background =
                getSimpleDrawable(R.drawable.common_corners_trans_d4d6d9_6_0)
        }
    }


    override fun success(str: String) {
        toast(str)
        finish()
    }

}