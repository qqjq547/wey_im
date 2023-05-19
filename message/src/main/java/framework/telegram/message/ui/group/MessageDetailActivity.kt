package framework.telegram.message.ui.group

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.common.TitleModel.TITLE_HEAD
import framework.ideas.common.model.group.GroupChatDetailModel
import framework.ideas.common.model.group.GroupChatDetailModel.GROUP_CHAT_DETAIL_TYPE
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.ui.adapter.MessageDetailAdapter
import framework.telegram.support.BaseActivity
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.recyclerview.sticky.StickyItemDecoration
import framework.telegram.ui.status.QMUIStatusView
import kotlinx.android.synthetic.main.msg_activity_group_chat_detail.*
import kotlinx.android.synthetic.main.msg_title1.*
import java.util.*

/**
 * Created by lzh on 19-7-17.
 * INFO:
 */
@Route(path = Constant.ARouter.ROUNTE_MSG_DETAIL_ACTIVITY)
class MessageDetailActivity : BaseActivity(), MessageDetailContract.View {

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private val mChatType by lazy { intent.getIntExtra("chatType", 0) }

    private val mTargetId by lazy { intent.getLongExtra("targetId", 0L) }

    private val mLocalMessageId by lazy { intent.getLongExtra("messageLocalId", 0L) }

    private var mPresenter: MessageDetailPresenterImpl? = null

    private val mMessageAdapter by lazy { MessageDetailAdapter(mChatType) }

    private val mQMUIStatusView by lazy { QMUIStatusView(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (mMineUid <= 0 || mTargetId <= 0) {
            Toast.makeText(applicationContext, getString(R.string.targetId_error), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContentView(R.layout.msg_activity_group_chat_detail)
        initView()
        initData()
    }

    private fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.message_for_details))

        recycler_view_messages.initMultiTypeRecycleView(LinearLayoutManager(this@MessageDetailActivity), mMessageAdapter, false)
        recycler_view_messages.refreshController().setEnablePullToRefresh(true)
        recycler_view_messages.recyclerViewController().recyclerView.addItemDecoration(StickyItemDecoration(sticky_head_container, TITLE_HEAD))
        recycler_view_messages.refreshController().setOnRefreshListener {
            mPresenter?.loadModelData(mLocalMessageId)
        }

        mMessageAdapter.setOnItemClickListener { _, _, position ->
            if (mMessageAdapter.getItemViewType(position) == GROUP_CHAT_DETAIL_TYPE) {
                val info = mMessageAdapter.data[position]
                if (info is GroupChatDetailModel) {
                    mMessageAdapter.data.forEach {
                        if (it is GroupChatDetailModel) {
                            if (it.canSelect() && it.bean.senderUid == info.bean.senderUid) {
                                it.isSelect = !it.isSelect
                            } else {
                                it.isSelect = false
                            }
                        }
                    }
                }
                mMessageAdapter.notifyDataSetChanged()
            }
        }

        sticky_head_container.setDataCallback {
            val item = mMessageAdapter.getItem(it)
            if (item is TitleModel) {
                if (TextUtils.isEmpty(item.title)) {
                    frame_layout1.visibility = View.GONE
                } else {
                    frame_layout1.visibility = View.VISIBLE
                }
                text_view_title1.text = item.title
            }
        }
    }

    private fun initData() {
        mPresenter = MessageDetailPresenterImpl(this@MessageDetailActivity, this@MessageDetailActivity, lifecycle(), mChatType, mTargetId)
        mPresenter?.loadModelData(mLocalMessageId)

        if (mChatType == ChatModel.CHAT_TYPE_PVT) {
            getContactInfo()
        } else if (mChatType == ChatModel.CHAT_TYPE_GROUP) {
            getMemberInfo()
        }
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as MessageDetailPresenterImpl
    }

    override fun isActive(): Boolean = true

    override fun loadPageSuccess(list: MutableList<MultiItemEntity>) {
        mMessageAdapter.setNewData(list)
        recycler_view_messages.refreshController().refreshComplete()
    }

    override fun loadPageFail(str: String, hasData: Boolean) {
        recycler_view_messages.refreshController().refreshComplete()

        if (!hasData) {
            mQMUIStatusView.showErrorView()
        }

        toast(str)
    }

    private fun getContactInfo() {
        // 获取个人信息缓存
        ArouterServiceManager.contactService.getContactInfo(lifecycle(), mTargetId, { contactInfoModel, _ ->
            val map = HashMap<Long, Any>()
            map[contactInfoModel.uid] = contactInfoModel
            mMessageAdapter.setMessageOwnerList(map)
        })
    }

    private fun getMemberInfo() {
        ArouterServiceManager.groupService.getAllGroupMembersInfoByCache(mTargetId, Long.MAX_VALUE, { groupInfoModels, _ ->
            val map = HashMap<Long, Any>()
            groupInfoModels.forEach {
                map[it.uid] = it
            }

            mMessageAdapter.setMessageOwnerList(map)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter?.destory()
    }
}