package framework.telegram.business.ui.me

import android.annotation.SuppressLint
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseViewHolder
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.BlackContract
import framework.telegram.business.ui.me.presenter.BlackPresenterImpl
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.doubleclick.recycler.AppBaseQuickAdapter
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView
import kotlinx.android.synthetic.main.bus_me_activity_black_list.*
import kotlinx.android.synthetic.main.bus_search.*

/**
 * Created by lzh on 19-6-7.
 * INFO: 黑名单
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_BLACK_LIST)
class BlackListActivity : BaseBusinessActivity<BlackContract.Presenter>(), BlackContract.View {

    private var mAppDialog: AppDialog? = null

    private val mAdapter by lazy {
        BlackAdapter {targetId->
            AppDialog.showBottomListView(this, this,
                    mutableListOf(getString(R.string.remove_from_blacklist))) { dialog, index, _ ->
                when (index) {
                    0 -> {
                        mPresenter?.setBlack(false,targetId)
                    }
                }
            }
        }
    }

    override fun getLayoutId() = R.layout.bus_me_activity_black_list

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.blacklist))
        common_recycler?.initSingleTypeRecycleView(LinearLayoutManager(this), mAdapter, false)
        common_recycler.refreshController().setEnablePullToRefresh(false)
        common_recycler.emptyController().setEmpty()
    }

    override fun initListen() {
        text_view_search_icon.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT)
                    .withInt(Constant.Search.SEARCH_TYPE, Constant.Search.SEARCH_BLACK).navigation()
        }
    }

    @SuppressLint("CheckResult")
    override fun initData() {
        BlackPresenterImpl(this, this, lifecycle()).start()
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as BlackPresenterImpl
    }

    override fun isActive() = true

    class BlackAdapter(val listen: ((Long) -> Unit)) : AppBaseQuickAdapter<ContactDataModel, BaseViewHolder>(R.layout.bus_contacts_item) {

        override fun convert(helper: BaseViewHolder, item: ContactDataModel) {
            helper.getView<AppTextView>(R.id.app_text_view_name)?.text = item.displayName
            helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item.icon)
            helper.itemView.setOnLongClickListener {
                listen.invoke(item.uid)
                false
            }
        }
    }

    override fun showLoading() {
        mAppDialog = AppDialog.showLoadingView(this@BlackListActivity, this@BlackListActivity)
    }

    override fun showData(list: MutableList<ContactDataModel>) {
        mAppDialog?.dismiss()
        mAdapter.setNewData(list)
    }

    override fun showErrMsg(str: String?) {
        mAppDialog?.dismiss()
        toast(str.toString())
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter?.destory()
    }

    override fun showBlackInfo(bfMyBlack: Boolean) {
    }
}