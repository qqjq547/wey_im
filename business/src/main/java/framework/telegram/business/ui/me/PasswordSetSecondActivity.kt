package framework.telegram.business.ui.me

import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.BusinessApplication
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.event.MeRedPointChangeEvent
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.PasswordSetSecondContract
import framework.telegram.business.ui.me.presenter.PasswordSetSecondPresenterImpl
import framework.telegram.business.utils.UserInfoCheckUtil
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.KeyboardktUtils
import kotlinx.android.synthetic.main.bus_me_activity_item_edit.custom_toolbar
import kotlinx.android.synthetic.main.bus_me_activity_password_set_second.*

/**
 * Created by lzh on 19-6-7.
 * INFO: 改变手机号绑定，第二步
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_PASSWORD_SET_SECOND)
class PasswordSetSecondActivity : BaseBusinessActivity<PasswordSetSecondContract.Presenter>(), PasswordSetSecondContract.View {

    private var mPassword1OK = false
    private var mPassword2OK = false

    override fun getLayoutId() = R.layout.bus_me_activity_password_set_second

    override fun initView() {
        custom_toolbar.showCenterTitle(getString(R.string.set_a_password))
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        eet_1.initEasyEditText(true,true,false,null,{
            mPassword1OK = it.isNotEmpty()
            setLoginBtn()
        })
        eet_1.et.hint = getString(R.string.enter_password)

        eet_2.initEasyEditText(true,true,false,null,{
            mPassword2OK = it.isNotEmpty()
            setLoginBtn()
        })
        eet_2.et.hint = getString(R.string.confirm_password)
    }

    override fun initListen() {

        text_view_login.setOnClickListener {
            val text = eet_1.et.text.trim().toString()
            val text2 = eet_1.et.text.trim().toString()
            if (!UserInfoCheckUtil.doubleCheckPassword(this, text,text2))
                return@setOnClickListener
            if (!UserInfoCheckUtil.checkPassword(this, text))
                return@setOnClickListener
            mPresenter?.setPassword(text)
        }

        linear_layout_all.setOnClickListener {
            KeyboardktUtils.hideKeyboard(linear_layout_all)
        }
    }

    override fun initData() {
        PasswordSetSecondPresenterImpl(this, this, lifecycle())
    }

    private fun setLoginBtn() {
        if (mPassword1OK && mPassword2OK) {
            text_view_login.isEnabled = true
            text_view_login.background = getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
        } else {
            text_view_login.isEnabled = false
            text_view_login.background = getSimpleDrawable(R.drawable.common_corners_trans_d4d6d9_6_0)
        }
    }

    override fun isActive(): Boolean  = true

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@PasswordSetSecondActivity,this@PasswordSetSecondActivity)
    }

    override fun setPasswordSuccess(password: String) {
        dialog?.dismiss()
        toast(getString(R.string.successfully_set))
        BusinessApplication.setPassword(password)
        EventBus.publishEvent(MeRedPointChangeEvent())
        ActivitiesHelper.getInstance().closeTarget(PasswordSetFirstActivity::class.java)
        finish()
    }

    override fun showErrMsg(str: String?) {
        dialog?.dismiss()
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as PasswordSetSecondPresenterImpl
    }

}