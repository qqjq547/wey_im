package framework.telegram.message.ui.telephone

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.event.JumpStreamTopEvent
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.bean.StreamCallItem
import framework.telegram.message.controller.MessageController
import framework.telegram.message.manager.ReceiveMessageManager
import framework.telegram.message.ui.IMultiCheckCallCallback
import framework.telegram.message.ui.IMultiCheckChatCallback
import framework.telegram.message.ui.IMultiCheckable
import framework.telegram.message.ui.telephone.adapter.StreamCallHistoryAdapter
import framework.telegram.support.BaseApp
import framework.telegram.support.BaseFragment
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.menu.FloatMenu
import framework.telegram.ui.videoplayer.utils.NetworkUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.msg_fragment_stream_call_history_all.*

@Route(path = Constant.ARouter.ROUNTE_MSG_STREAM_CALL_HISTORY_FRAGMENT_ALL)
class StreamCallHistoryAllFragment : BaseFragment(), StreamCallContract.View, IMultiCheckCallCallback {

    override val fragmentName: String
        get() = "StreamCallHistoryFragment"

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private var mPresenter: StreamCallContract.Presenter? = null
    private val mAdapter = StreamCallHistoryAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.msg_fragment_stream_call_history_all, container, false)
    }

    @SuppressLint("CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()

        EventBus.getFlowable(JumpStreamTopEvent::class.java)//跳转到顶部
                .bindToLifecycle(this@StreamCallHistoryAllFragment)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (activity != null) {
                        recycler_view_history.recyclerViewController().scrollToPosition(0)
                    }
                }

        StreamCallPresenterImpl(this, true).setFObservable(lifecycle())
        mPresenter?.getStreamCallHistoryData()
    }

    override fun update(list: List<StreamCallItem>) {
        if (activity != null) {
            mAdapter.setNewData(list)
            recycler_view_history.recyclerViewController().notifyDataSetChanged()

            if (mAdapter.isMultiCheckMode) {
                if (mAdapter.data.size != mAdapter.getCheckableMessages().size) {
                    (activity as IMultiCheckable).setAllChecked(false)
                }
            }
        }
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as StreamCallContract.Presenter
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        recycler_view_history?.destory()
        mPresenter?.stop()
    }

    private fun initRecyclerView() {
        mAdapter.setOnItemClickListener { _, _, position ->
            if (!mAdapter.isMultiCheckMode) {
                if (NetworkUtils.isAvailable(BaseApp.app) && ReceiveMessageManager.socketIsLogin) {
                    val data = mAdapter.getItem(position)
                    data?.let {
                        activity?.let { act ->
                            AppDialog.showList(act, this@StreamCallHistoryAllFragment,
                                    listOf(getString(R.string.voice_communication), getString(R.string.video_call))) { _, index, _ ->
                                when (index) {
                                    0 -> {
                                        ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO)
                                                .withLong("targetUid", data.data.chaterId)
                                                .withInt("streamType", 0)
                                                .navigation()
                                    }
                                    1 -> {
                                        ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO)
                                                .withLong("targetUid", data.data.chaterId)
                                                .withInt("streamType", 1)
                                                .navigation()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    toast(getString(R.string.socket_is_error))
                }
            }
        }

        mAdapter.setOnItemLongClickListener { _, _, position ->
            if (!mAdapter.isMultiCheckMode) {
                val data = mAdapter.getItem(position)
                data?.let { item ->
                    activity?.let {
                        val floatMenu = FloatMenu(this.activity)
                        val items = mutableListOf<String>()
                        items.add(getString(R.string.delete))
                        items.add(getString(R.string.multi_checkable))
                        floatMenu.items(*items.toTypedArray())
                        floatMenu.show(recycler_view_history.popPoint)
                        floatMenu.setOnItemClickListener { _, text ->
                            when (text) {
                                getString(R.string.delete) -> {
                                    if (item.nearCount > 0) {
                                        item.nearSessionIdList.forEach { sessionId ->
                                            MessageController.deleteStreamCallMessage(mMineUid, item.data.chaterId, sessionId)
                                        }
                                    }
                                    MessageController.deleteStreamCallMessage(mMineUid, item.data.chaterId, item.data.sessionId)
                                }
                                getString(R.string.multi_checkable) -> {
                                    setCheckableMessage(data)
                                }
                            }
                        }
                    }
                }
            }

            true
        }

        mAdapter.setOnItemChildClickListener { _, _, position ->
            if (!mAdapter.isMultiCheckMode) {
                val data = mAdapter.getItem(position)
                data?.let {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_DETAIL)
                            .withLong("targetUid", data.data.chaterId)
                            .withString("iconUrl", data.data.chaterIcon)
                            .withString("chaterName", data.data.chaterName)
                            .withInt("streamType", data.data.streamType)
                            .withLong("date", data.data.reqTime)
                            //  对方发起的未接听
                            .withBoolean("isMissCall", (data.data.isSend == 0 && (data.data.status == 0 || data.data.status == 3)))
                            .navigation()
                }
            }
        }

        recycler_view_history.initSingleTypeRecycleView(LinearLayoutManager(activity), mAdapter, false)
        recycler_view_history.refreshController().setEnablePullToRefresh(false)
        recycler_view_history.emptyController().setEmpty(getString(R.string.no_call_recently), R.drawable.common_icon_empty_stream)
    }

    private fun setCheckableMessage(msg: StreamCallItem) {
        if (activity is IMultiCheckable) {
            mAdapter.setCheckable(msg) { msgCount ->
                (activity as IMultiCheckable).setCheckableMessage(showSetReaded = false, countTitle = getString(R.string.sign_call), msgCount = msgCount)

                if (mAdapter.data.size != mAdapter.getCheckableMessages().size) {
                    (activity as IMultiCheckable).setAllChecked(false)
                }
            }
        }
    }

    override fun showCheckMessages() {

    }

    override fun clickAllChecked(isChecked: Boolean): Int {
        return mAdapter.setAllChecked(isChecked)
    }

    override fun clickBatchDelete() {
        val list = mAdapter.getCheckableMessages()

        AppDialog.show(activity!!, this@StreamCallHistoryAllFragment) {
            positiveButton(text = getString(R.string.confirm), click = {
                // 删除
                list.forEach {
                    if (it.nearCount > 0) {
                        it.nearSessionIdList.forEach { sessionId ->
                            MessageController.deleteStreamCallMessage(mMineUid, it.data.chaterId, sessionId)
                        }
                    }
                    MessageController.deleteStreamCallMessage(mMineUid, it.data.chaterId, it.data.sessionId)
                }

                (activity as IMultiCheckable).dismissCheckMessages()
            })
            negativeButton(text = getString(R.string.cancel))
            title(text = getString(R.string.clear_and_delete))
            message(text = String.format(getString(R.string.multi_delete_call_tip), list.size))
        }
    }

    override fun dismissCheckMessages() {
        mAdapter.setUnCheckable()
    }
}
