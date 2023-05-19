package framework.telegram.business.ui.me

import android.annotation.SuppressLint
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseViewHolder
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.BuildConfig
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.UnViewOnlineContract
import framework.telegram.business.ui.me.presenter.UnViewOnlinePresenterImpl
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.doubleclick.recycler.AppBaseQuickAdapter
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView
import kotlinx.android.synthetic.main.bus_me_activity_unview_online_list.*
import kotlinx.android.synthetic.main.bus_search.*

/**
 * Created by lzh on 19-6-7.
 * INFO: 黑名单
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_UNVIEW_ONLINE_LIST)
class UnViewOnlineListActivity : BaseBusinessActivity<UnViewOnlineContract.Presenter>(), UnViewOnlineContract.View {

    private var mAppDialog: AppDialog? = null

    private val mAdapter by lazy {
        UnViewOnlineAdapter {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACT_DETAIL)
                    .withLong(Constant.ARouter_Key.KEY_TARGET_UID, it).navigation()
        }
    }

    override fun getLayoutId() = R.layout.bus_me_activity_unview_online_list

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.Invisible_list))

        custom_toolbar.showRightImageView(R.drawable.common_icon_circle_add, onClickCallback = {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE).withInt("operate", 3).navigation()
        })

        mAdapter.setOnItemLongClickListener { _, _, position ->
            val data = mAdapter.getItem(position)
            data?.let {
                AppDialog.showList(this@UnViewOnlineListActivity, this@UnViewOnlineListActivity,
                        listOf(getString(R.string.shift_out))) { _, _, _ ->

                    val appDialog = AppDialog.showLoadingView(this@UnViewOnlineListActivity, this@UnViewOnlineListActivity)
                    ArouterServiceManager.contactService.deleteDisShowOnlineContact(lifecycle(), data.uid, {
                        appDialog.dismiss()
                    }, {
                        appDialog.dismiss()
                        if (BuildConfig.DEBUG) {
                            toast(getString(R.string.deletion_not_visible_failed_sign) + it.message)
                        } else {
                            toast(getString(R.string.deletion_not_visible_failed))
                        }
                    })
                }
            }

            false
        }

        common_recycler?.initSingleTypeRecycleView(LinearLayoutManager(this), mAdapter, false)
        common_recycler.refreshController().setEnablePullToRefresh(false)
        common_recycler.emptyController().setEmpty()
    }

    override fun initListen() {
        text_view_search_icon.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT)
                    .withInt(Constant.Search.SEARCH_TYPE, Constant.Search.SEARCH_UN_VIEW_ONLINE).navigation()
        }
    }

    @SuppressLint("CheckResult")
    override fun initData() {
        UnViewOnlinePresenterImpl(this, this, lifecycle()).start()
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as UnViewOnlinePresenterImpl
    }

    override fun isActive() = true

    class UnViewOnlineAdapter(val listen: ((Long) -> Unit)) : AppBaseQuickAdapter<ContactDataModel, BaseViewHolder>(R.layout.bus_contacts_item) {

        override fun convert(helper: BaseViewHolder, item: ContactDataModel) {
            helper.getView<AppTextView>(R.id.app_text_view_name)?.text = item.displayName
            helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item.icon)
            helper.itemView.setOnClickListener {
                listen.invoke(item.uid)
            }
        }
    }

    override fun showLoading() {
        mAppDialog = AppDialog.showLoadingView(this@UnViewOnlineListActivity, this@UnViewOnlineListActivity)
    }

    override fun showData(list: MutableList<ContactDataModel>) {
        mAppDialog?.dismiss()
        mAdapter.setNewData(list)
    }

    override fun showEmpty() {
        mAppDialog?.dismiss()
    }

    override fun showErrMsg(str: String?) {
        mAppDialog?.dismiss()
        toast(str.toString())
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter?.destory()
    }
}