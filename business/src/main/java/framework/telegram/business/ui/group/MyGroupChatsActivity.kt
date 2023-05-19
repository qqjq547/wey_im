package framework.telegram.business.ui.group

import android.annotation.SuppressLint
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.Search.SEARCH_MY_GROUP
import framework.telegram.business.bridge.Constant.Search.SEARCH_TYPE
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.group.adapter.MyGroupChatsAdapter
import framework.telegram.business.ui.group.presenter.MyGroupChatsContract
import framework.telegram.business.ui.group.presenter.MyGroupChatsPresenterImpl
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_GROUP_CHAT_ACTIVITY
import framework.telegram.message.bridge.event.BanGroupMessageEvent
import framework.telegram.message.bridge.event.DisableGroupMessageEvent
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import kotlinx.android.synthetic.main.bus_groud_activity_chat.*
import kotlinx.android.synthetic.main.bus_search.*

/**
 * Created by lzh on 19-5-27.
 * INFO:群聊
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_CONTACTS_GROUP_CHAT)
class MyGroupChatsActivity : BaseBusinessActivity<MyGroupChatsContract.Presenter>(), MyGroupChatsContract.View {

    override fun getLayoutId() = R.layout.bus_groud_activity_chat

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        custom_toolbar.showCenterTitle(getString(R.string.group_chat))
    }

    @SuppressLint("CheckResult")
    override fun initListen() {
        text_view_search_icon.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT)
                    .withInt(SEARCH_TYPE, SEARCH_MY_GROUP).navigation()
        }
        common_recycler.initSingleTypeRecycleView(LinearLayoutManager(this), MyGroupChatsAdapter { targetGid ->
            ARouter.getInstance().build(ROUNTE_MSG_GROUP_CHAT_ACTIVITY)
                    .withLong("targetGid", targetGid).navigation()
        }, false)

        common_recycler.refreshController().setOnRefreshListener {
            mPresenter?.start()
        }
        common_recycler.emptyController().setEmpty()

        common_recycler.refreshController().setEnablePullToRefresh(false)

        EventBus.getFlowable(BanGroupMessageEvent::class.java)
                .bindToLifecycle(this@MyGroupChatsActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val list = common_recycler.recyclerViewController().adapter.data as MutableList<GroupInfoModel>
                    var matchIndex = -1
                    run outside@{
                        list.forEachIndexed { index, group ->
                            if (group.groupId == it.groupId) {
                                matchIndex = index
                                return@outside
                            }
                        }
                    }
                    if (matchIndex != -1){
                        list.removeAt(matchIndex)
                        common_recycler.itemController().setNewData(list)
                    }
                }

        EventBus.getFlowable(DisableGroupMessageEvent::class.java)
                .bindToLifecycle(this@MyGroupChatsActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val list = common_recycler.recyclerViewController().adapter.data as MutableList<GroupInfoModel>
                    var matchIndex = -1
                    run outside@{
                        list.forEachIndexed { index, group ->
                            if (group.groupId == it.groupId) {
                                matchIndex = index
                                return@outside
                            }
                        }
                    }
                    if (matchIndex != -1){
                        list.removeAt(matchIndex)
                        common_recycler.itemController().setNewData(list)
                    }
                }
    }

    @SuppressLint("CheckResult")
    override fun initData() {
        MyGroupChatsPresenterImpl(this, this, lifecycle()).start()
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@MyGroupChatsActivity, this@MyGroupChatsActivity)
    }

    override fun showEmpty() {
        dialog?.dismiss()
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as MyGroupChatsContract.Presenter
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun refreshListUI(list: List<GroupInfoModel>) {
        dialog?.dismiss()
        common_recycler.itemController().setNewData(list)
        common_recycler.refreshController().refreshComplete()
    }

    override fun showError(errStr: String?) {
        dialog?.dismiss()
        toast(errStr.toString())
    }
}