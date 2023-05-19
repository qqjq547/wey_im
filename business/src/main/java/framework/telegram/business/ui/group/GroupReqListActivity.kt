package framework.telegram.business.ui.group

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.CommonProto
import com.im.domain.pb.GroupProto
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.ChatModel.CHAT_TYPE_GROUP_NOTIFY
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.GroupHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.GroupHttpProtocol
import framework.telegram.business.ui.group.adapter.GroupReqListAdapter
import framework.telegram.message.bridge.event.UnreadMessageEvent
import framework.telegram.support.BaseActivity
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import kotlinx.android.synthetic.main.bus_recycler_inside_linear_with_toolbar.*

@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_JOIN_REQ_LIST)
class GroupReqListActivity : BaseActivity() {

    companion object {
        const val REQUEST_CODE_OPERATE = 0X1000
    }

    private var mPageNum = 1

    private val mPageSize = 20

    private val mAdapter by lazy { GroupReqListAdapter() }

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private val mGroupReqList by lazy { ArrayList<GroupReqListAdapter.GroupReqInfoItem>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bus_recycler_inside_linear_with_toolbar)

        //会话改成已阅
        ArouterServiceManager.messageService.setChatIsReaded(ChatModel.CHAT_TYPE_GROUP_NOTIFY, 0, {
            EventBus.publishEvent(UnreadMessageEvent())//修改红点数字
        })

        /**
         * 初始化
         */
        custom_toolbar.showCenterTitle(getString(R.string.group_of_notice))
        custom_toolbar.showRightImageView(R.drawable.common_icon_more, {
            AppDialog.showBottomListView(this, this, mutableListOf(getString(R.string.string_clear_group_notification))
            ) { _, index, _ ->
                when (index) {
                    0 -> {
                        ArouterServiceManager.groupService.clearGroupReq(null, {
                            ArouterServiceManager.messageService.deleteChat(CHAT_TYPE_GROUP_NOTIFY, 0)//群通知id=0
                            finish()
                        }, {
                            toast(it.message.toString())
                        })
                    }
                }
            }
        })

        initListen()
        initAdapter()

        /**
         * 刷新数据
         */
        common_recycler.refreshController().autoRefresh()
    }

    private fun initAdapter() {
        mAdapter.setOnItemChildClickListener { _, view, position ->
            if (view.id == R.id.image_view_button_agree) {
                val data = mAdapter.getItem(position)
                data?.let {
                    //是邀请
                    if (data.type == CommonProto.GroupReqType.GROUP_INVITE && mMineUid == data.data.targetUser.uid) {
                        HttpManager.getStore(GroupHttpProtocol::class.java)
                                .groupUserCheckJoin(object : HttpReq<GroupProto.GroupUserCheckJoinReq>() {
                                    override fun getData(): GroupProto.GroupUserCheckJoinReq {
                                        return GroupHttpReqCreator.createGroupUserCheckJoinReq(it.data.groupReqId, true)
                                    }
                                })
                                .getResult(lifecycle(), { groupCheck ->
                                    //                                    val account = AccountManager.getLoginAccount(AccountInfo::class.java)
//                                    it.checkUser = CommonProto.UserBase.newBuilder().setNickName(account.getNickName()).setIcon(account.getAvatar()).build()
                                    it.status = CommonProto.GroupReqStatus.AGREE
                                    mAdapter.notifyDataSetChanged()
                                }, { throwable ->
                                    //出错了
                                    toast(throwable.message.toString())
                                })
                    } else {
                        HttpManager.getStore(GroupHttpProtocol::class.java)
                                .groupCheckJoin(object : HttpReq<GroupProto.GroupCheckJoinReq>() {
                                    override fun getData(): GroupProto.GroupCheckJoinReq {
                                        return GroupHttpReqCreator.createGroupCheckJoinReq(it.data.groupReqId, true)
                                    }
                                })
                                .getResult(lifecycle(), { _ ->
                                    //                                    val account = AccountManager.getLoginAccount(AccountInfo::class.java)
//                                    it.checkUser = CommonProto.UserBase.newBuilder().setNickName(account.getNickName()).setIcon(account.getAvatar()).build()
                                    it.status = CommonProto.GroupReqStatus.AGREE
                                    mAdapter.notifyDataSetChanged()
                                }, { throwable ->
                                    //出错了
                                    toast(throwable.message.toString())
                                })
                    }
                }
            } else if (view.id == R.id.relative_layout) {
                val data = mAdapter.getItem(position)
                data?.let {
                    if (data.status == CommonProto.GroupReqStatus.CHECKING) {
                        if (data.type == CommonProto.GroupReqType.GROUP_INVITE && mMineUid == data.data.targetUser.uid) {
                            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_UN_AUDIT_INVITE_MEMBER_DETAIL)
                                    .withInt(Constant.ARouter_Key.KEY_POSITION, position)
                                    .withLong(Constant.ARouter_Key.KEY_RECORD_ID, data.data.groupReqId)
                                    .withString(Constant.ARouter_Key.KEY_USER_ICON, data.data.fromUser.icon)
                                    .withString(Constant.ARouter_Key.KEY_USER_NAME, data.data.fromUser.nickName)
                                    .withString(Constant.ARouter_Key.KEY_GROUP_ICON, data.data.pic)
                                    .withString(Constant.ARouter_Key.KEY_GROUP_NAME, data.data.groupName)
                                    .navigation(this@GroupReqListActivity, REQUEST_CODE_OPERATE)
//                        } else if(data.type == CommonProto.GroupReqType.GROUP_IS_BANNED.number
//                                || data.type == CommonProto.GroupReqType.GROUP_MEMBER_SHUTUP.number ){//封禁的群，禁言消息不可点击
//                            //不处理
                        } else {
                            //群主或管理员
                            val fromSource = when (data.data.groupReqType.number) {
                                CommonProto.GroupReqType.GROUP_QR_CODE.number -> {
                                    getString(R.string.think_change)
                                }
                                CommonProto.GroupReqType.GROUP_LINK.number -> {
                                    getString(R.string.string_group_link_in)
                                }
                                else -> {
                                    String.format(getString(R.string.invite_to_group_chats), data.data.fromUser.nickName)
                                }
                            }
                            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_UN_AUDIT_MEMBER_DETAIL)
                                    .withInt(Constant.ARouter_Key.KEY_POSITION, position)
                                    .withLong(Constant.ARouter_Key.KEY_TARGET_UID, data.data.targetUser.uid)
                                    .withLong(Constant.ARouter_Key.KEY_RECORD_ID, data.data.groupReqId)
                                    .withString(Constant.ARouter_Key.KEY_REQ_MSG, data.data.msg)
                                    .withString(Constant.ARouter_Key.KEY_GROUP_NAME, data.data.groupName)
                                    .withString(Constant.ARouter_Key.KEY_FROM_SOURCE, fromSource)
                                    .navigation(this@GroupReqListActivity, REQUEST_CODE_OPERATE)
                        }
                    }
                }
            }
        }
        mAdapter.setOnItemChildLongClickListener { _, view, position ->
            if (view.id == R.id.relative_layout) {
                AppDialog.showList(this@GroupReqListActivity, this@GroupReqListActivity,
                        listOf(getString(R.string.delete))) { _, _, _ ->

                    val data = mAdapter.getItem(position)
                    mAdapter.remove(position)
                    data?.let {
                        HttpManager.getStore(GroupHttpProtocol::class.java)
                                .delGroupReqRecord(object : HttpReq<GroupProto.DelGroupReqRecordReq>() {
                                    override fun getData(): GroupProto.DelGroupReqRecordReq {
                                        return GroupHttpReqCreator.createDelGroupReqRecord(data.data.groupReqId)
                                    }
                                })
                                .getResult(null, {
                                    AppLogcat.logger.e("demo", "删除群请求成功！！！")
                                }, {
                                    toast(getString(R.string.the_delete_group_request_failed))
                                    AppLogcat.logger.e("demo", "删除群请求失败！！！")
                                })
                    }
                }
            }

            true
        }

        mAdapter.setNewData(mGroupReqList)
        common_recycler.initSingleTypeRecycleView(LinearLayoutManager(this), mAdapter, true)


        common_recycler.refreshController().setOnRefreshListener {
            mPageNum = 1
            getData(mPageNum, mPageSize)
        }

        common_recycler.loadMoreController().setOnLoadMoreListener {
            getData(mPageNum + 1, mPageSize)
        }

        common_recycler.refreshController().setEnablePullToRefresh(true)
    }

    fun initListen() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPERATE && resultCode == Activity.RESULT_OK) {
            val operate = data?.getBooleanExtra(Constant.ARouter_Key.KEY_OPERATE, false) ?: false
            val position = data?.getIntExtra(Constant.ARouter_Key.KEY_POSITION, -1) ?: -1
            if (position >= 0) {
                mAdapter.getItem(position)?.let {
                    val account = AccountManager.getLoginAccount(AccountInfo::class.java)
//                    it.checkUser = CommonProto.UserBase.newBuilder().setNickName(account.getNickName()).setIcon(account.getAvatar()).build()
//                    it.status = if (operate) CommonProto.GroupReqStatus.AGREE.number else CommonProto.GroupReqStatus.REFUSE.number
                    val dd = mGroupReqList[position]
                    dd.status = if (operate) CommonProto.GroupReqStatus.AGREE else CommonProto.GroupReqStatus.REFUSE
                    mGroupReqList[position] = dd
                    mAdapter.notifyItemChanged(position)
                }
            }
        }
    }

    private fun getData(pageNum: Int, pageSize: Int) {
        val loadFirst = pageNum == 1
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .groupReqList(object : HttpReq<GroupProto.GroupReqListReq>() {
                    override fun getData(): GroupProto.GroupReqListReq {
                        return GroupHttpReqCreator.createGroupReqListReq(pageNum, pageSize)
                    }
                })
                .getResult(lifecycle(), { data ->
                    common_recycler.refreshController().refreshComplete()

                    if (pageNum == 1) {
                        mGroupReqList.clear()
                    }

                    if (!data.groupReqsList.isNullOrEmpty()) {
                        //下发数据
                        mPageNum = pageNum
                        data.groupReqsList.forEach { groupReqInfo ->
                            mGroupReqList.add(GroupReqListAdapter.GroupReqInfoItem(groupReqInfo))
                        }
                        mAdapter.notifyDataSetChanged()
                        common_recycler.loadMoreController().loadMoreComplete()
                    } else {
                        //无下发数据，说明已经取完
                        common_recycler.loadMoreController().loadMoreEnd()
                    }
                }, {
                    toast(it.message.toString())
                    common_recycler.refreshController().refreshComplete()
                    if (!loadFirst) {
                        common_recycler.loadMoreController().loadMoreFail()
                    }
                })
    }
}
