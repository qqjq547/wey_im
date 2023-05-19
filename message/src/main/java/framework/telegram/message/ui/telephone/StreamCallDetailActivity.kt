package framework.telegram.message.ui.telephone

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.bean.StreamCallItem
import framework.telegram.message.bridge.event.FriendInfoChangeEvent
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.manager.ReceiveMessageManager
import framework.telegram.message.ui.telephone.adapter.StreamCallDetailAdapter
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.menu.FloatMenu
import framework.telegram.ui.tools.Helper
import framework.telegram.ui.videoplayer.utils.NetworkUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.msg_stream_call_activity_detail.*


@Route(path = Constant.ARouter.ROUNTE_MSG_STREAM_CALL_DETAIL)
class StreamCallDetailActivity: BaseActivity(), StreamCallContract.View {

    private val mStreamType by lazy { intent.getIntExtra("streamType", 0) }
    private val mChaterName by lazy { intent.getStringExtra("chaterName") }
    private val mIconUrl by lazy { intent.getStringExtra("iconUrl") }
    private val mIsMissCall by lazy { intent.getBooleanExtra("isMissCall", false) }
    private val mChaterId by lazy { intent.getLongExtra("targetUid", 0L) }
    private val mDate by lazy { intent.getLongExtra("date",0L) }
    private var mPresenter: StreamCallContract.Presenter? = null
    private val mAdapter = StreamCallDetailAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.msg_stream_call_activity_detail)

        initUI()

        StreamCallPresenterImpl(this, false).setAObservable(lifecycle())
        mPresenter?.getStreamCallHistoryData(mChaterId, mIsMissCall, mStreamType,mDate)

    }

    override fun update(list: List<StreamCallItem>) {
        mAdapter.setNewData(list)
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as StreamCallContract.Presenter
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun onDestroy() {
        super.onDestroy()

        mPresenter?.stop()
    }

    @SuppressLint("CheckResult")
    private fun initUI() {
        initTitleBar()
        initRecyclerView()

        ArouterServiceManager.contactService.updateContactInfo(lifecycle(), mChaterId, { cacheDataModel ->
            showStatus(cacheDataModel)
        }, { contactDataModel ->
            showStatus(contactDataModel)
        })

        EventBus.getFlowable(FriendInfoChangeEvent::class.java)
                .bindToLifecycle(this@StreamCallDetailActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.user.uid == mChaterId) {
                        showStatus(it.user)
                    }
                }

        image_view_icon.setImageURI(UriUtils.parseUri(mIconUrl))

        image_view_chat.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY).withLong("targetUid", mChaterId).navigation()
        }

        image_view_audio.setOnClickListener {
            callStream(0)
        }

        image_view_video.setOnClickListener {
            callStream(1)
        }

        text_view_phone.setOnLongClickListener {
            showCopyDialog(it,text_view_phone.text.toString())
            false
        }
    }

    private fun initTitleBar() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.call_details))
    }

    private fun initRecyclerView() {
        recycler_view_detail.initSingleTypeRecycleView(LinearLayoutManager(this), mAdapter, false)
        recycler_view_detail.refreshController().setEnablePullToRefresh(false)
    }

    private fun showStatus(contactDataModel: ContactDataModel) {
        if (contactDataModel.isShowLastOnlineTime && contactDataModel.isOnlineStatus) {
            // 显示在线状态
            view_online_status_point.visibility = View.VISIBLE
        } else {
            // 隐藏在线状态
            view_online_status_point.visibility = View.GONE
        }
        text_view_nickname.text = contactDataModel.nickName
        text_view_phone.text = contactDataModel.phone
    }

    private fun showCopyDialog(v: View, str:String?){
        val userPhone=str?.replace("+","")?.replace(" ","")
        val floatMenu = FloatMenu(this)

        val items = mutableListOf<String>()
        items.add(getString(R.string.copy))

        floatMenu.items(*items.toTypedArray())

        floatMenu.showDropDown(v, v.width - 5, 0)
        floatMenu.setOnItemClickListener { _, text ->
            when (text) {
                getString(R.string.copy) ->{
                    Helper.setPrimaryClip(BaseApp.app,userPhone)
                    toast(getString(R.string.copy_success))
                }
            }
        }
    }

    private fun callStream(streamType: Int) {
        if (NetworkUtils.isAvailable(BaseApp.app) && ReceiveMessageManager.socketIsLogin) {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO).withLong("targetUid", mChaterId).withInt("streamType", streamType).navigation()
        } else {
            toast(getString(R.string.socket_is_error))
        }
    }
}