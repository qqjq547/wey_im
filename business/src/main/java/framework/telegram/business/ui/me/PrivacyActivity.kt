package framework.telegram.business.ui.me

import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.sp.CommonPref
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.PrivacyContract
import framework.telegram.business.ui.me.presenter.PrivacyPresenterImpl
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.BitUtils
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.Helper
import kotlinx.android.synthetic.main.bus_me_activity_item_edit.custom_toolbar
import kotlinx.android.synthetic.main.bus_me_activity_privacy.*

/**
 * Created by lzh on 19-6-7.
 * INFO:隐私
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_INFO_PRIVACY)
class PrivacyActivity : BaseBusinessActivity<PrivacyContract.Presenter>(), PrivacyContract.View {
    private val mAccountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    private val commonPref by lazy { SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()) }

    override fun getLayoutId() = R.layout.bus_me_activity_privacy

    override fun initView() {
        val privacy = mAccountInfo.getPrivacy()

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.privacy))

        me_item_view_add.setData(getString(R.string.add_my_way), "") {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_ME_INFO_PRIVACY_WAY).navigation()
        }

        //使用一个int储存这两个设置
        switch_button_verification_for_add_me.setData(getString(R.string.when_you_add_me_as_a_friend_you_need_to_verify), BitUtils.checkBitValue(Helper.int2Bytes(privacy)[3], 5)) {
            mPresenter?.savePerfectInfo(3, 5, it)
        }
        switch_button_verification_for_invitation_me.setData(getString(R.string.inviting_me_to_join_a_group_chat_requires_verification), BitUtils.checkBitValue(Helper.int2Bytes(privacy)[3], 6)) {
            mPresenter?.savePerfectInfo(3, 6, it)
        }
        switch_button_verification_show_phone.setData(getString(R.string.show_your_phone_number_to_a_friend), !BitUtils.checkBitValue(Helper.int2Bytes(privacy)[2], 4)) {
            mPresenter?.savePerfectInfo(2, 4, !it)
        }

        me_item_view_last_online_time.setData(getString(R.string.lastactive), "") {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_ME_INFO_PRIVACY_DIS_SHOW_ONLINE).navigation()
        }

        me_item_view_close_screen.setData(getString(R.string.blur_screen), commonPref.getBlurScreen()) {
            commonPref.putBlurScreen(it)
        }

        switch_button_input_state.setData(getString(R.string.show_the_input_state), !BitUtils.checkBitValue(Helper.int2Bytes(privacy)[2], 6)) {
            mPresenter?.savePerfectInfo(2, 6, !it)
        }
        switch_button_read_receipt.setData(getString(R.string.read_receipt), !BitUtils.checkBitValue(Helper.int2Bytes(privacy)[2], 7)) {
            mPresenter?.savePerfectInfo(2, 7, !it)
        }

        me_item_view_black.setData(getString(R.string.blacklist), "") {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_ME_BLACK_LIST).navigation()
        }
    }

    override fun initListen() {
    }

    override fun initData() {
        PrivacyPresenterImpl(this, this, lifecycle())
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

}