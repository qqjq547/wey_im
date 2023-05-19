package framework.telegram.message.ui.forward


import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.common.TitleModel.TITLE_HEAD
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT
import framework.telegram.business.bridge.Constant.Search.FORWARD_IDS
import framework.telegram.business.bridge.Constant.Search.SEARCH_FORWARD_CONTACTS
import framework.telegram.business.bridge.Constant.Search.SEARCH_TYPE
import framework.telegram.business.bridge.event.ForwardFinishEvent
import framework.telegram.business.bridge.event.ForwardMessageEvent
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.ui.forward.adapter.ForwardContactsAdapter
import framework.telegram.support.BaseActivity
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.ui.recyclerview.sticky.StickyItemDecoration
import framework.telegram.ui.utils.ScreenUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import kotlinx.android.synthetic.main.msg_activity_forward_contacts.*
import kotlinx.android.synthetic.main.msg_activity_forward_contacts.custom_toolbar
import kotlinx.android.synthetic.main.msg_contacts_item_head2.*
import kotlinx.android.synthetic.main.msg_search.*

@Route(path = Constant.ARouter.ROUNTE_MSG_FORWARD_SELECT_CONTACTS)
class ForwardToContactsActivity : BaseActivity() {

    private val mRealm by lazy { ArouterServiceManager.groupService.getContactsRealm() }

    private val mModelsList by lazy { ArrayList<MultiItemEntity>() }

    private val mAdapter by lazy { ForwardContactsAdapter() }

    private val mForwardIdSet by lazy { intent.getStringArrayListExtra(FORWARD_IDS) ?: arrayListOf() }

    private var mSureTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.msg_activity_forward_contacts)

        initTitleBar()
        initList()
        initData()
    }

    private fun initTitleBar() {
        custom_toolbar.showCenterTitle(getString(R.string.select_contact))
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        sticky_head_container.setDataCallback {
            val item = mModelsList.get(it)
            if (item is TitleModel) {
                val title = item.title
                if (getString(R.string.string_star).equals(title)) {
                    text_view_head_name2.text = getString(R.string.string_star_sign)
                } else {
                    text_view_head_name2.text = title
                }
            }
        }

        text_view_search_icon.setOnClickListener {
            ARouter.getInstance().build(ROUNTE_BUS_SEARCH_CONTACT)
                    .withStringArrayList(FORWARD_IDS,getSendedId())
                    .withInt(SEARCH_TYPE, SEARCH_FORWARD_CONTACTS).navigation()
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
        val dataList = ArrayList<ContactDataModel>()
        mRealm.executeTransactionAsync(Realm.Transaction { realm ->
            val models = realm.where(ContactDataModel::class.java)?.equalTo("bfMyContacts", true)?.sort("letter")?.findAll()
            models?.forEach {
                dataList.add(it.copyContactDataModel())
            }
        }, Realm.Transaction.OnSuccess {
            val tmpList = mutableListOf<ContactDataModel>()
            val tmpStarList = mutableListOf<ContactDataModel>()
            dataList.forEach { info ->
                if (info.isBfStar) {
                    tmpStarList.add(info.copyContactDataModel())
                }
                tmpList.add(info)
            }
            mModelsList.clear()
            mModelsList.addAll(getSortList(tmpList, tmpStarList))
            mAdapter.setNewData(mModelsList)
        })
    }

    @SuppressLint("CheckResult")
    private fun initList() {
        index_fast_scroll_recycler_view.initIndexFastScrollRecyclerView(
                LinearLayoutManager(this),
                mAdapter,
                false)
        index_fast_scroll_recycler_view.indexFastScrollController()
                .setIndexBarVisibility(false)
                .setIndexBarColor(this.resources.getColor(R.color.white))
                .setIndexBarTextColor(this.resources.getColor(R.color.a2a4a7))
                .setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
                .setIndexbarMargin(0f)
                .setIndexTextSize(11)
                .addItemDecoration(StickyItemDecoration(sticky_head_container, TITLE_HEAD))

        EventBus.getFlowable(ForwardFinishEvent::class.java)
                .bindToLifecycle(this@ForwardToContactsActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    finish()
                }

        EventBus.getFlowable(ForwardMessageEvent::class.java)
                .bindToLifecycle(this@ForwardToContactsActivity)
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

    /**
     * 获取排序的列表
     */
    private fun getSortList(allContacts: MutableList<ContactDataModel>, startContacts: List<ContactDataModel>): MutableList<MultiItemEntity> {
        val tmpList1 = mutableListOf<ContactDataModel>()
        val tmpList2 = mutableListOf<ContactDataModel>()
        allContacts.forEach {
            if (it.letter.equals("#")) {
                tmpList2.add(it)
            } else {
                tmpList1.add(it)
            }
        }
        allContacts.clear()
        allContacts.addAll(tmpList1)
        allContacts.addAll(tmpList2)

        val dataList = mutableListOf<MultiItemEntity>()
        if (startContacts.isNotEmpty()) {
            dataList.add(TitleModel(getString(R.string.string_star), TITLE_HEAD, 0))
            dataList.addAll(startContacts)
        }
        var prevSection = ""

        for (i in allContacts.indices) {
            val currentItem = allContacts[i]
            val currentSection = currentItem.letter
            if (prevSection != currentSection) {
                dataList.add(TitleModel(currentSection, TITLE_HEAD, 0))
                prevSection = currentSection ?: ""
            }
            dataList.add(currentItem)
        }
        return dataList
    }

    private fun getSendedId():ArrayList<String> {
        val list = ArrayList<String>()
        mAdapter.getSendedModleId().forEach {
            list.add(it.toString())
        }
        return list
    }
}

