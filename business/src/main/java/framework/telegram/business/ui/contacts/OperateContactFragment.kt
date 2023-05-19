package framework.telegram.business.ui.contacts

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.RxLifecycle
import com.trello.rxlifecycle3.android.FragmentEvent
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.event.CreateGroupAllSelectMemberEvent
import framework.telegram.business.event.CreateGroupSelectMemberEvent
import framework.telegram.business.ui.contacts.adapter.OperateContactAdapter
import framework.telegram.support.BaseFragment
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.ui.recyclerview.sticky.StickyItemDecoration
import framework.telegram.ui.status.QMUIViewBuilder
import framework.telegram.ui.utils.KeyboardktUtils
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import kotlinx.android.synthetic.main.bus_contacts_item_head.*
import kotlinx.android.synthetic.main.bus_group_fragment_create_and_add_member.*

/**
 * 显示来源为联系人
 *
 * 如果携带groupId(long)参数打开页面，则为一个群添加群成员
 */
//TODO QMUIStatusView
@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE_FRAGMENT)
class OperateContactFragment : BaseFragment(), RealmChangeListener<RealmResults<ContactDataModel>> {
    override val fragmentName: String
        get() = "OperateContactFragment"

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private val mRealm by lazy { RealmCreator.getContactsRealm(mMineUid) }

    private val mOperate by lazy { arguments?.getInt("operate", 0) }

    private val mGroupId by lazy { arguments?.getLong("groupId", 0) }

    private val mAddedUserId by lazy { arguments?.getLong("addedUserId", 0) }

    private val mAdapter by lazy { OperateContactAdapter() }

    private var mContactModels: RealmResults<ContactDataModel>? = null

    private val mListDatas by lazy { ArrayList<MultiItemEntity>() }

    private var mContactSize = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bus_group_fragment_create_and_add_member, container, false)
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
        registerEvent()
    }

    private fun initView() {
        mAdapter.emptyView = QMUIViewBuilder(QMUIViewBuilder.TYPE.EMPTY_VIEW).build(this)
        index_fast_scroll_recycler_view.initIndexFastScrollRecyclerView(
                LinearLayoutManager(this.context!!),
                mAdapter,
                false)
        index_fast_scroll_recycler_view.indexFastScrollController()
                .setIndexBarColor(this.context!!.resources.getColor(R.color.white))
                .setIndexBarTextColor(this.context!!.resources.getColor(R.color.a2a4a7))
                .setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
                .setIndexbarMargin(0f)
                .setIndexTextSize(11)
                .addItemDecoration(StickyItemDecoration(sticky_head_container, 1))
        index_fast_scroll_recycler_view.refreshController().setEnablePullToRefresh(false)
        text_view_head_name.text = ""

        sticky_head_container.setDataCallback { index ->
            mListDatas.let {
                val data = it[index]
                if (data.itemType == 1) {
                    val title = (data as OperateContactAdapter.ItemData).data as String
                    if (getString(R.string.string_star).equals(title)) {
                        text_view_head_name.text = getString(R.string.string_star_sign)
                    } else {
                        text_view_head_name.text = title
                    }
                }
            }
        }

        index_fast_scroll_recycler_view.recyclerViewController().recyclerView.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_UP -> {
                    KeyboardktUtils.hideKeyboard(index_fast_scroll_recycler_view)
                }
            }
            false
        }
    }

    private fun initData() {
        when (mOperate) {
            1 -> {
                if (mAddedUserId ?: 0 > 0) {
                    //新建群，但有预选用户
                    mAdapter.addUnSelectUid(mAddedUserId ?: 0)
                    loadContacts()
                } else {
                    // 新建群
                    loadContacts()
                }
            }
            2 -> {
                // 添加联系人到群中
                ArouterServiceManager.groupService.getGroupMemberUids(mGroupId ?: 0, {
                    if (it.isNullOrEmpty()) {
                        activity?.finish()
                    } else {
                        mAdapter.setUnSelectUids(it)
                        loadContacts()
                    }
                }, { activity?.finish() })
            }
            4 -> { //群发
                loadContacts()
            }
            else -> {
                ArouterServiceManager.contactService.getAllDisShowOnlineCacheContacts({ contacts ->
                    val uids = mutableListOf<Long>()
                    contacts.forEach {
                        uids.add(it.uid)
                    }
                    mAdapter.setUnSelectUids(uids)
                    loadContacts()
                }, { activity?.finish() })
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun registerEvent() {
        EventBus.getFlowable(CreateGroupSelectMemberEvent::class.java)
                .bindToLifecycle(this@OperateContactFragment)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (activity != null) {
                        if (it.from == 2) {
                            if (it.op == 1) {
                                mAdapter.addSelectedUid(it.uid)
                            } else {
                                mAdapter.removeSelectedUid(it.uid)
                            }
                        } else if (it.from == 3) {
                            mAdapter.removeSelectedUid(it.uid)
                        }
                        mAdapter.notifyDataSetChanged()
                    }
                }
    }

    @SuppressLint("CheckResult")
    private fun loadContacts() {
        ThreadUtils.runOnUIThread {
            Flowable.just<Realm>(mRealm)
                    .compose(RxLifecycle.bindUntilEvent(lifecycle(), FragmentEvent.DESTROY))
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .map { it.where(ContactDataModel::class.java)?.equalTo("bfMyContacts", true)?.and()?.equalTo("bfMyBlack", false)?.sort("letter")?.findAllAsync() }
                    .observeOn(AndroidSchedulers.mainThread()).subscribe {
                        mContactModels = it
                        mContactModels?.addChangeListener(this@OperateContactFragment)
                    }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.removeAllChangeListeners()
        mRealm.close()
    }

    override fun onChange(t: RealmResults<ContactDataModel>) {
        if (!t.isValid) {
            return
        }

        if (activity == null || context == null) {
            return
        }

        val tmpList = mutableListOf<ContactDataModel>()
        val tmpStarList = mutableListOf<ContactDataModel>()
        t.forEach {
            val info = it.copyContactDataModel()
            if (info.isBfStar) {
                tmpStarList.add(info.copyContactDataModel())
            }
            tmpList.add(info)
        }
        mContactSize = tmpList.size
        mListDatas.clear()
        mListDatas.addAll(getSortList(tmpList, tmpStarList))
        mAdapter.setNewData(mListDatas)
    }

    /**
     * 获取排序的列表
     */
    private fun getSortList(allContacts: MutableList<ContactDataModel>, startContacts: List<ContactDataModel>): MutableList<MultiItemEntity> {
        val tmpList1 = mutableListOf<ContactDataModel>()
        val tmpList2 = mutableListOf<ContactDataModel>()
        allContacts.forEach {
            if (it?.letter.equals("#")) {
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
            dataList.add(OperateContactAdapter.ItemData(getString(R.string.string_star)))
            for (i in startContacts.indices) {
                dataList.add(OperateContactAdapter.ItemData(startContacts[i]))
            }
        }
        var prevSection = ""
        for (i in allContacts.indices) {
            val currentItem = allContacts[i]
            val currentSection = currentItem.letter
            if (prevSection != currentSection) {
                dataList.add(OperateContactAdapter.ItemData(currentSection))
                prevSection = currentSection ?: ""
            }
            dataList.add(OperateContactAdapter.ItemData(currentItem))
        }
        return dataList
    }

    fun selectAllContact(all: Boolean) {
        val idList = mutableListOf<Long>()
        val iconList = mutableListOf<String>()
        val displayList = mutableListOf<String>()
        mAdapter.data.forEach {
            if (it is OperateContactAdapter.ItemData) {
                if (it.data is ContactDataModel) {
                    idList.add(it.data.uid)
                    iconList.add(it.data.icon)
                    displayList.add(it.data.displayName)
                }
            }
        }

        if (all) {
            mAdapter.addSelectedUid(idList)
            EventBus.publishEvent(CreateGroupAllSelectMemberEvent(idList, iconList,displayList, 1, 1))
        } else {
            mAdapter.removeSelectedUid(idList)
            EventBus.publishEvent(CreateGroupAllSelectMemberEvent(idList,iconList, displayList, 2, 1))
        }
        mAdapter.notifyDataSetChanged()
    }

    //判断是不是全选
    fun getIsAllSelect(): Boolean {
        return mContactSize == mAdapter.getSelectSize()
    }
}
