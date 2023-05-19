package framework.telegram.business.ui.me

import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.CommonProto
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.PrivacyDisShowOnlineContract
import framework.telegram.business.ui.me.presenter.PrivacyDisShowOnlinePresenterImpl
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.toast
import kotlinx.android.synthetic.main.bus_me_activity_dis_show_online_privacy.*
import kotlinx.android.synthetic.main.bus_me_activity_item_edit.custom_toolbar

/**
 * Created by lzh on 19-6-7.
 * INFO:最后上线时间
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_INFO_PRIVACY_DIS_SHOW_ONLINE)
class PrivacyDisShowOnlineActivity : BaseBusinessActivity<PrivacyDisShowOnlineContract.Presenter>(), PrivacyDisShowOnlineContract.View {

    override fun getLayoutId() = R.layout.bus_me_activity_dis_show_online_privacy

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.lastactive))

        setting_item_view_0.setData(getString(R.string.invisible_list), "") {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_ME_UNVIEW_ONLINE_LIST).navigation()
        }
    }

    override fun initListen() {
        //显示目前已选的项
        val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
        when (accountInfo.getOnlineViewType()) {
            CommonProto.LastOnlineTimeViewType.ALL_SHOW.number -> upDateSelect(CommonProto.LastOnlineTimeViewType.ALL_SHOW)
            CommonProto.LastOnlineTimeViewType.ONLY_FRIEND.number -> upDateSelect(CommonProto.LastOnlineTimeViewType.ONLY_FRIEND)
            else -> upDateSelect(CommonProto.LastOnlineTimeViewType.NOT_SHOW)
        }

        //用户选择了某项，更新ui并通知后台
        ll_all.setOnClickListener {
            if(accountInfo.getOnlineViewType()==CommonProto.LastOnlineTimeViewType.ALL_SHOW.number)return@setOnClickListener
            upDateSelect(CommonProto.LastOnlineTimeViewType.ALL_SHOW)
            mPresenter?.savePerfectInfo(CommonProto.LastOnlineTimeViewType.ALL_SHOW)
        }
        ll_contact.setOnClickListener {
            if(accountInfo.getOnlineViewType()==CommonProto.LastOnlineTimeViewType.ONLY_FRIEND.number)return@setOnClickListener
            upDateSelect(CommonProto.LastOnlineTimeViewType.ONLY_FRIEND)
            mPresenter?.savePerfectInfo(CommonProto.LastOnlineTimeViewType.ONLY_FRIEND)
        }
        ll_dis_show.setOnClickListener {
            if(accountInfo.getOnlineViewType()==CommonProto.LastOnlineTimeViewType.NOT_SHOW.number)return@setOnClickListener
            upDateSelect(CommonProto.LastOnlineTimeViewType.NOT_SHOW)
            mPresenter?.savePerfectInfo(CommonProto.LastOnlineTimeViewType.NOT_SHOW)
        }
    }

    private fun upDateSelect(type: CommonProto.LastOnlineTimeViewType){
        iv_all_user_show_online.setImageResource(R.drawable.common_oval_ffffff_10)
        iv_contact_show_online.setImageResource(R.drawable.common_oval_ffffff_10)
        iv_dis_show.setImageResource(R.drawable.common_oval_ffffff_10)

        when (type) {
            CommonProto.LastOnlineTimeViewType.ALL_SHOW -> iv_all_user_show_online.setImageResource(R.drawable.common_contacts_icon_check_selected)
            CommonProto.LastOnlineTimeViewType.ONLY_FRIEND -> iv_contact_show_online.setImageResource(R.drawable.common_contacts_icon_check_selected)
            CommonProto.LastOnlineTimeViewType.NOT_SHOW -> iv_dis_show.setImageResource(R.drawable.common_contacts_icon_check_selected)
        }
    }

    override fun initData() {
        PrivacyDisShowOnlinePresenterImpl(this, this, lifecycle())
    }

    override fun showLoading() {
    }

    override fun savePerfectInfoSuccess() {

    }

    override fun showErrMsg(str: String?) {
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as PrivacyDisShowOnlinePresenterImpl
    }

    override fun isActive() = true

}