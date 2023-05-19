package framework.telegram.message.ui.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.controller.DownloadAttachmentController
import framework.telegram.message.controller.UploadAttachmentController
import framework.telegram.message.ui.media.adapter.MediaAdapter
import framework.telegram.support.BaseFragment
import framework.telegram.support.mvp.BasePresenter
import kotlinx.android.synthetic.main.msg_fragment_file.*

@Route(path = Constant.ARouter.ROUNTE_MSG_MEDIA_MANAGER_FILE)
class FileFragment: BaseFragment(), MediaPresenter.View{

    override val fragmentName: String
        get() = "StreamCallHistoryFragment"

    private var mPresenterImpl: MediaPresenter.Presenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.msg_fragment_file,container, false)
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
            it.onItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, _, position ->
                val item = adapter.getItem(position) as MessageModel
                val targetId = if (item.isSend == 0) item.senderId else item.targetId
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_PREVIEW_FILE)
                        .withLong("messageLocalId", item.id)
                        .withLong("targetId", targetId)
                        .withInt("chatType", mChatType)
                        .withString("mimetype", item.fileMessageContentBean.mimeType)
                        .withString("downloadPath", item.fileMessageContentBean.fileUri)
                        .withString("fileName", item.fileMessageContentBean.name)
                        .withLong("fileSize", item.fileMessageContentBean.size)
                        .navigation()
            }
        }
    }

    private fun initRecyclerView(){
        recycler_view_media.initMultiTypeRecycleView(LinearLayoutManager(activity), mAdapter,false)
        recycler_view_media.refreshController().setEnableLoadMore(false)
        recycler_view_media.refreshController().setEnablePullToRefresh(false)
        recycler_view_media.emptyController().setEmpty()

        if (mChatType == ChatModel.CHAT_TYPE_GROUP)
            mPresenterImpl?.getFileFromGroup(mChaterId)
        else
            mPresenterImpl?.getFileFromPv(mChaterId)

        DownloadAttachmentController.attachDownloadListener(mAdapter)
        UploadAttachmentController.attachUploadListener(mAdapter)
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

    override fun onDestroy() {
        super.onDestroy()

        DownloadAttachmentController.detachDownloadListener(mAdapter)
        UploadAttachmentController.detachUploadListener(mAdapter)
    }

}