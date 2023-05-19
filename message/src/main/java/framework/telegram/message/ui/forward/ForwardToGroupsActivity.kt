package framework.telegram.message.ui.forward

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.group.GroupInfoModel
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT
import framework.telegram.business.bridge.Constant.Search.FORWARD_IDS
import framework.telegram.business.bridge.Constant.Search.SEARCH_FORWARD_GROUP
import framework.telegram.business.bridge.Constant.Search.SEARCH_TYPE
import framework.telegram.business.bridge.event.ForwardFinishEvent
import framework.telegram.business.bridge.event.ForwardMessageEvent
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.ui.forward.adapter.ForwardGroupAdapter
import framework.telegram.support.BaseActivity
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.ui.utils.ScreenUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import kotlinx.android.synthetic.main.msg_activity_forward_chats.*
import kotlinx.android.synthetic.main.msg_activity_forward_chats.custom_toolbar
import kotlinx.android.synthetic.main.msg_search.*

@Route(path = Constant.ARouter.ROUNTE_MSG_FORWARD_SELECT_GROUP)
class ForwardToGroupsActivity : BaseActivity() {

    private val mRealm by lazy { ArouterServiceManager.groupService.getGroupRealm() }

    private val mModelsList by lazy { ArrayList<MultiItemEntity>() }

    private val mAdapter by lazy { ForwardGroupAdapter() }

    private val mForwardIdSet by lazy { intent.getStringArrayListExtra(FORWARD_IDS) ?: arrayListOf() }

    private var mSureTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.msg_activity_forward_group)


        initTitleBar()
        initList()
        initData()
    }

    private fun initTitleBar() {
        custom_toolbar.showCenterTitle(getString(R.string.select_group_chat))

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        text_view_search_icon.setOnClickListener {
            ARouter.getInstance().build(ROUNTE_BUS_SEARCH_CONTACT)
                    .withStringArrayList(FORWARD_IDS,getSendedId())
                    .withInt(SEARCH_TYPE, SEARCH_FORWARD_GROUP).navigation()
        }

        custom_toolbar.showRightTextView("", {
            EventBus.publishEvent(ForwardFinishEvent())
            finish()
        }) {
            val size = ScreenUtils.dp2px(this, 10f)
            mSureTextView = it
            val lp = mSureTextView?.layoutParams as LinearLayout.LayoutParams
            lp.rightMargin = ScreenUtils.dp2px(this, 10f)
            mSureTextView?.setPadding(size, 0, size, 0)
            mSureTextView?.text = getString(R.string.accomplish)
        }
    }

    @SuppressLint("CheckResult")
    private fun initData() {
        val set = mutableSetOf<Long>()
        mForwardIdSet.forEach {
            set.add(it.toLong())
        }
        mAdapter.setSendedId(set)
        val dataList = ArrayList<MultiItemEntity>()
        mRealm.executeTransactionAsync(Realm.Transaction { realm ->
            val models = realm.where(GroupInfoModel::class.java)?.equalTo("bfAddress", true)?.findAll()
            models?.forEach {
                dataList.add(it.copyGroupInfoModel())
            }
        }, Realm.Transaction.OnSuccess {
            mModelsList.clear()
            mModelsList.addAll(dataList)

            recycler_view_history.recyclerViewController().notifyDataSetChanged()
        })
    }

    @SuppressLint("CheckResult")
    private fun initList() {

        mAdapter.setNewData(mModelsList)
        recycler_view_history.initMultiTypeRecycleView(LinearLayoutManager(this@ForwardToGroupsActivity), mAdapter, false)
        recycler_view_history.refreshController().setEnablePullToRefresh(false)
        recycler_view_history.emptyController().setEmpty()

        EventBus.getFlowable(ForwardFinishEvent::class.java)
                .bindToLifecycle(this@ForwardToGroupsActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    finish()
                }

        EventBus.getFlowable(ForwardMessageEvent::class.java)
                .bindToLifecycle(this@ForwardToGroupsActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    mAdapter.addSendedId(event.chaterId)
                    mAdapter.notifyDataSetChanged()
                }
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.removeAllChangeListeners()
        mRealm.close()
    }

    private fun getSendedId():ArrayList<String> {
        val list = ArrayList<String>()
        mAdapter.getSendedModleId().forEach {
            list.add(it.toString())
        }
        return list
    }
}

