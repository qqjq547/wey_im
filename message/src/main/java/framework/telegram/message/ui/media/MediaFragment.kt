package framework.telegram.message.ui.media

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.ui.media.adapter.MediaAdapter
import framework.telegram.support.BaseFragment
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.ui.recyclerview.GridItemDecoration
import kotlinx.android.synthetic.main.msg_fragment_media.*

@Route(path = Constant.ARouter.ROUNTE_MSG_MEDIA_MANAGER_MEDIA)
class MediaFragment: BaseFragment(), MediaPresenter.View{

    override val fragmentName: String
        get() = "StreamCallHistoryFragment"

    private var mPresenterImpl: MediaPresenter.Presenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.msg_fragment_media,container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MediaPresentImpl(lifecycle(),this)
        initRecyclerView()
    }

    private val mChaterId: Long by lazy {
        arguments?.getLong("chaterId",0L) as Long
    }
    private val mChatType: Int by lazy {
        arguments?.getInt("chatType",-1)  as Int
    }

    private val mAdapter by lazy {
        MediaAdapter(mChaterId, mChatType).also {
            it.onItemChildClickListener =  BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
                if (view.id == R.id.image_view
                        || view.id == R.id.image_view_video
                        || view.id == R.id.image_view_play){
                    val item = adapter.getItem(position) as MessageModel
                    if (item.imageMessageContent != null) {
                        val targetId = if (item.isSend == 0) item.senderId else item.targetId
                        ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_PREVIEW_BRIDGE_ACTIVITY)
                                .withLong("messageLocalId", item.id)
                                .withLong("targetId", targetId)
                                .withInt("chatType", mChatType)
                                .withBoolean("shouldReverse", true)
                                .navigation()
                    }
                }
            }
        }
    }

    private fun initRecyclerView(){
        val spanCount = 4
        val layoutManager = GridLayoutManager(activity,spanCount)
        recycler_view_media.initMultiTypeRecycleView(layoutManager, mAdapter,false)
        recycler_view_media.refreshController().setEnableLoadMore(false)
        recycler_view_media.refreshController().setEnablePullToRefresh(false)
        recycler_view_media.emptyController().setEmpty()
        recycler_view_media.recyclerViewController().recyclerView.addItemDecoration(GridItemDecoration(spanCount,1,1,1,1))
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                // 返回的是为要占几个格,每行格总数由spanCount决定
                if (mAdapter.getItem(position)?.itemType == MessageModel.LOCAL_TYPE_OTHER_UNKNOW
                        || mAdapter.getItem(position)?.itemType == MessageModel.LOCAL_TYPE_MYSELF_UNKNOW
                        || mAdapter.getItem(position)?.itemType == MessageModel.LOCAL_TYPE_OTHER_FILE
                        || mAdapter.getItem(position)?.itemType == MessageModel.LOCAL_TYPE_MYSELF_FILE
                        || mAdapter.data.size == 0)
                    return spanCount
                else if (position == mAdapter.data.size - 1){
                    Log.e("??", (spanCount - (position - 1)%4).toString())
                    return spanCount - (position - 1)%4
                }
                else
                    return 1
            }
        }

        if (mChatType == ChatModel.CHAT_TYPE_GROUP)
            mPresenterImpl?.getMediaFromGroup(mChaterId)
        else
            mPresenterImpl?.getMediaFromPv(mChaterId)
    }

    override fun showData(list: MutableList<MessageModel>) {
        mAdapter.setNewData(list)
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenterImpl = presenter as MediaPresenter.Presenter
    }

    override fun isActive(): Boolean {
        return true
    }

}