package framework.telegram.business.ui.group

import android.annotation.SuppressLint
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.GroupProto
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.group.adapter.SameGroupChatsAdapter
import framework.telegram.business.ui.group.presenter.SameGroupChatsContract
import framework.telegram.business.ui.group.presenter.SameGroupChatsPresenterImpl
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_GROUP_CHAT_ACTIVITY
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import kotlinx.android.synthetic.main.bus_groud_activity_same_chat.*

/**
 * Created by lzh on 19-9-27.
 * INFO:相同的群聊
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_SAME_GROUP_CHAT)
class SameGroupChatsActivity : BaseBusinessActivity<SameGroupChatsContract.Presenter>(), SameGroupChatsContract.View {

    companion object {
        const val TARGET_ID = "TARETID"
    }

    private val mTargetId by lazy { intent.getLongExtra(TARGET_ID,0L) }

    private val mAdapter by lazy {SameGroupChatsAdapter { targetGid ->
        ARouter.getInstance().build(ROUNTE_MSG_GROUP_CHAT_ACTIVITY)
                .withLong("targetGid", targetGid).navigation()
    }}
    override fun getLayoutId() = R.layout.bus_groud_activity_same_chat


    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.bus_group_same_chat))
    }

    override fun initListen() {
        common_recycler.initSingleTypeRecycleView(LinearLayoutManager(this),mAdapter , true)

        common_recycler.refreshController().setOnRefreshListener {
            mPresenter?.getFirstDataList()
        }
        common_recycler.emptyController().setEmpty()
        common_recycler.loadMoreController().setOnLoadMoreListener {
            mPresenter?.getDataList()
        }
        common_recycler.refreshController().setEnablePullToRefresh(true)
    }

    @SuppressLint("CheckResult")
    override fun initData() {
        SameGroupChatsPresenterImpl(this, mTargetId,this, lifecycle()).start()
        mPresenter?.getFirstDataList()
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@SameGroupChatsActivity, this@SameGroupChatsActivity)
    }

    override fun showEmpty() {
        dialog?.dismiss()
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as SameGroupChatsContract.Presenter
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun refreshListUI(list: List<GroupProto.GroupBase>, hasMore: Boolean) {
        dialog?.dismiss()
        common_recycler.refreshController().refreshComplete()
        mAdapter.setNewData(list)
        if (!hasMore){
            common_recycler.loadMoreController().loadMoreEnd()
        }else{
            common_recycler.loadMoreController().loadMoreComplete()
        }
    }

    override fun showError(errStr: String?) {
        dialog?.dismiss()
        toast(errStr.toString())
    }
}