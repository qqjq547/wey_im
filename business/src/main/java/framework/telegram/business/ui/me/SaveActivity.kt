package framework.telegram.business.ui.me

import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.getCustomView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.UserProto
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_ME_APPLOCK_SETTING
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_ME_CHANGE_PHONE_FIRST
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_ME_CLEAR_ACCOUNT
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_ME_PASSWORD_CHANGE
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_ME_PASSWORD_SET_FIRST
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.UserHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.UserHttpProtocol
import framework.telegram.business.sp.CommonPref
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.PrivacyContract
import framework.telegram.business.ui.me.presenter.PrivacyPresenterImpl
import framework.telegram.business.ui.widget.SwitchButtonView
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.BitUtils
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.Helper
import framework.telegram.support.tools.MD5
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.ScreenUtils
import kotlinx.android.synthetic.main.bus_me_activity_item_edit.custom_toolbar
import kotlinx.android.synthetic.main.bus_me_activity_save.*
import yourpet.client.android.sign.NativeLibUtil

/**
 * Created by lzh on 19-6-7.
 * INFO:安全
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_INFO_SAVE)
class SaveActivity : BaseBusinessActivity<PrivacyContract.Presenter>(), PrivacyContract.View {

    companion object {
        const val REQUEST_CODE = 100
    }

    private val mCommonPref by lazy { SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()) }

    private val mAccountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    override fun getLayoutId() = R.layout.bus_me_activity_save

    private var mHasPassword = true

    override fun initView() {
        val privacy = mAccountInfo.getPrivacy()

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.safety))

        mHasPassword = AccountManager.getLoginAccount(AccountInfo::class.java).getBfPassword()

        switch_button_verification_login.setData(getString(R.string.disable_captcha_login), BitUtils.checkBitValue(Helper.int2Bytes(privacy)[2], 3)) { check ->
            if (mHasPassword) {
                AppDialog.showCustomView(this@SaveActivity, R.layout.bus_dialog_edit, this@SaveActivity) {
                    title(text = getString(R.string.safety_verification))
                    getCustomView().findViewById<TextView>(R.id.app_text_message).text = getString(R.string.verify_your_password_first)
                    val editView = getCustomView().findViewById<AppCompatEditText>(R.id.app_edit_text)
                    editView.hint = getString(R.string.enter_password)

                    var isCheck = false
                    positiveButton(text = getString(R.string.confirm), click = {
                        isCheck = true
                        checkPassword(editView.text.toString(), check)
                    })
                    onDismiss {
                        if (!isCheck) {//如果没有点击确定就关闭了dialog就恢复原来的样子
                            this@SaveActivity.findViewById<SwitchButtonView>(R.id.switch_button_verification_login).setData(!check)
                        }
                    }
                }

            } else {
                toast(getString(R.string.please_set_the_password_first))
                switch_button_verification_login.setData(!check)
            }
        }

        me_item_view_clear_user.setData(getString(R.string.string_clear_account), getClearAccountMouth(mAccountInfo.getClearTime()), listen = {
            ARouter.getInstance().build(ROUNTE_BUS_ME_CLEAR_ACCOUNT).navigation(this@SaveActivity, REQUEST_CODE)
        })
    }

    private fun checkPassword(password: String, originalState: Boolean) {
        val realPassword = if (TextUtils.isEmpty(password)) "" else getPassword(password)
        HttpManager.getStore(UserHttpProtocol::class.java)
                .checkPassword(object : HttpReq<UserProto.ValidatePasswordReq>() {
                    override fun getData(): UserProto.ValidatePasswordReq {
                        return UserHttpReqCreator.createCheckPasswordReq(realPassword)
                    }
                })
                .getResult(lifecycle(), {
                    //请求成功
                    mPresenter?.savePerfectInfo(2, 3, originalState)
                    toast(getString(R.string.string_password_success))
                }, {
                    //请求失败
                    switch_button_verification_login.setData(!originalState)
                    toast(getString(R.string.string_password_fail))
                })
    }

    override fun initListen() {

    }

    override fun initData() {
        PrivacyPresenterImpl(this, this, lifecycle())

        me_item_view_phone.setData(getString(R.string.phone_number), "${mAccountInfo.getCountryCode()} ${mAccountInfo.getPhone()}") {
            ARouter.getInstance().build(ROUNTE_BUS_ME_CHANGE_PHONE_FIRST).navigation()
        }
        me_item_view_password.setData(getString(R.string.password), if (mHasPassword) getString(R.string.change_password) else getString(R.string.not_set)) {
            if (mHasPassword) {
                ARouter.getInstance().build(ROUNTE_BUS_ME_PASSWORD_CHANGE).navigation()
            } else {
                ARouter.getInstance().build(ROUNTE_BUS_ME_PASSWORD_SET_FIRST).navigation()
            }
        }
        me_item_view_applock.setData(getString(R.string.app_lock)) {
            ARouter.getInstance().build(ROUNTE_BUS_ME_APPLOCK_SETTING).navigation()
        }
        me_item_view_password.setExtraIcon(if (mHasPassword) 0 else R.drawable.common_oval_f50d2e_8, ScreenUtils.dp2px(me_item_view_password.context, 8f))
    }

    override fun showLoading() {
    }

    override fun savePerfectInfoSuccess(index: Int, pos: Int, value: Boolean) {

    }

    override fun showErrMsg(str: String?) {
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as PrivacyPresenterImpl
    }

    override fun isActive() = true

    override fun onResume() {
        super.onResume()
        mHasPassword = mAccountInfo.getBfPassword()
        me_item_view_phone.setDataValue("${mAccountInfo.getCountryCode()} ${mAccountInfo.getPhone()}")
        me_item_view_password.setExtraIcon(if (mHasPassword) 0 else R.drawable.common_oval_f50d2e_8, ScreenUtils.dp2px(me_item_view_password.context, 8f))
        me_item_view_password.setDataValue(if (mHasPassword) getString(R.string.change_password) else getString(R.string.not_set))
    }

    private fun getPassword(data: String): String {
        return try {
            NativeLibUtil.getInstance().sign2(BaseApp.app, false, MD5.md5(data))
        } catch (e: Exception) {
            ""
        }
    }

    private fun getClearAccountMouth(index: Int): String {
        when (index) {
            1 -> {
                return getString(R.string.string_1_month)
            }
            2 -> {
                return getString(R.string.string_3_month)
            }
            0 -> {
                return getString(R.string.string_6_month)
            }
            3 -> {
                return getString(R.string.string_12_month)
            }
        }
        return getString(R.string.string_6_month)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val target = data?.getIntExtra("target", 0) ?: 0
            me_item_view_clear_user.setDataValue(getClearAccountMouth(target))
            mPresenter?.saveClearAccountTime(target) {
                mAccountInfo.putClearTime(target)
            }
        }
    }

}