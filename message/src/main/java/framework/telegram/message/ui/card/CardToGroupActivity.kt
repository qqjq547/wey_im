package framework.telegram.message.ui.card

/**
 * Created by lzh on 19-7-3.
 * INFO:
 */

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.group.GroupInfoModel
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.ShareCardEvent
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.ui.card.adapter.CardGroupAdapter
import framework.telegram.support.BaseActivity
import framework.telegram.support.system.event.EventBus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import kotlinx.android.synthetic.main.msg_activity_forward_chats.*
import kotlinx.android.synthetic.main.msg_search.*

@Route(path = Constant.ARouter.ROUNTE_MSG_CARD_SELECT_GROUP)
class CardToGroupActivity : BaseActivity() {

    private val mRealm by lazy { ArouterServiceManager.groupService.getGroupRealm() }

    private val mModelsList by lazy { ArrayList<MultiItemEntity>() }

    private val mTargetPic by lazy { intent.getStringExtra("targetPic")?:"" }

    private val mTargetName by lazy { intent.getStringExtra("targetName")?:"" }

    private val mAdapter by lazy { CardGroupAdapter(mTargetPic, mTargetName) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.msg_activity_forward_chats)

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
        }
    }

    @SuppressLint("CheckResult")
    private fun initData() {
        val dataList = ArrayList<MultiItemEntity>()
        mRealm.executeTransactionAsync(Realm.Transaction { realm ->
            val models = realm.where(GroupInfoModel::class.java)?.findAll()
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
        recycler_view_history.initMultiTypeRecycleView(LinearLayoutManager(this@CardToGroupActivity), mAdapter, false)
        recycler_view_history.refreshController().setEnablePullToRefresh(false)
        recycler_view_history.emptyController().setEmpty()

        EventBus.getFlowable(ShareCardEvent::class.java)
                .bindToLifecycle(this@CardToGroupActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    finish()
                }
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.removeAllChangeListeners()
        mRealm.close()
    }
}

