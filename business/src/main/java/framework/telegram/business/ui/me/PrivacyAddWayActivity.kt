package framework.telegram.business.ui.me

import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.PrivacyContract
import framework.telegram.business.ui.me.presenter.PrivacyPresenterImpl
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.BitUtils
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.Helper
import kotlinx.android.synthetic.main.bus_me_activity_item_edit.custom_toolbar
import kotlinx.android.synthetic.main.bus_me_activity_privacy_add_way.*

/**
 * Created by lzh on 19-6-7.
 * INFO:添加我的方式
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_INFO_PRIVACY_WAY)
class PrivacyAddWayActivity : BaseBusinessActivity<PrivacyContract.Presenter>(), PrivacyContract.View {

    private val mAccountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    override fun getLayoutId() = R.layout.bus_me_activity_privacy_add_way

    override fun initView() {
        val privacy = mAccountInfo.getPrivacy()
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.add_my_way))

        //使用一个int储存以下设置
        switch_button_phone.setData(getString(R.string.phone_number),!BitUtils.checkBitValue(Helper.int2Bytes(privacy)[3], 7)) {
            mPresenter?.savePerfectInfo(3, 7, !it)
        }

        switch_button_number.setData(getString(R.string.six_eight_number),!BitUtils.checkBitValue(Helper.int2Bytes(privacy)[1], 1)) {
            mPresenter?.savePerfectInfo(1, 1, !it)
        }

        switch_button_group_chat.setData(getString(R.string.group_chat),!BitUtils.checkBitValue(Helper.int2Bytes(privacy)[2], 0)) {
            mPresenter?.savePerfectInfo(2, 0, !it)
        }
        switch_button_qr.setData(getString(R.string.qr_code),!BitUtils.checkBitValue(Helper.int2Bytes(privacy)[2], 1)) {
            mPresenter?.savePerfectInfo(2, 1, !it)
        }
        switch_button_card.setData(getString(R.string.business_card),!BitUtils.checkBitValue(Helper.int2Bytes(privacy)[2], 2)) {
            mPresenter?.savePerfectInfo(2, 2, !it)
        }

        switch_button_link.setData(getString(R.string.string_link),!BitUtils.checkBitValue(Helper.int2Bytes(privacy)[1], 2)) {
            mPresenter?.savePerfectInfo(1, 2, !it)
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