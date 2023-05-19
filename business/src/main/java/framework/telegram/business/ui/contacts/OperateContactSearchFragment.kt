package framework.telegram.business.ui.contacts

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
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
import framework.telegram.business.event.CreateGroupSearchMemberEvent
import framework.telegram.business.event.CreateGroupSelectMemberEvent
import framework.telegram.business.ui.contacts.adapter.OperateContactSearchAdapter
import framework.telegram.support.BaseFragment
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.ui.utils.KeyboardktUtils
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Case
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import kotlinx.android.synthetic.main.bus_group_fragment_search_member.*

/**
 * 显示来源为联系人
 *
 * 如果携带groupId(long)参数打开页面，则为一个群添加群成员
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE_SEARCH_FRAGMENT)
class OperateContactSearchFragment : BaseFragment(), RealmChangeListener<RealmResults<ContactDataModel>> {
    override val fragmentName: String
        get() = "OperateContactSearchFragment"

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private val mRealm by lazy { RealmCreator.getContactsRealm(AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()) }

    private val mGroupId by lazy { arguments?.getLong("groupId", 0) }

    private val mAddedUserId by lazy { arguments?.getLong("addedUserId", 0) }

    private val mSelectList by lazy { arguments?.getParcelableArrayList<Parcelable>("selectList") }

    private val mAdapter by lazy { OperateContactSearchAdapter(mKeyword) }

    private var mContactModels: RealmResults<ContactDataModel>? = null

    private val mListDatas by lazy { ArrayList<MultiItemEntity>() }

    private var mKeyword = ""

    private val mOperate by lazy { arguments?.getInt("operate", 0) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bus_group_fragment_search_member, container, false)
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        initData()
        registerEvent()
    }

    private fun initRecyclerView() {
        common_recycler?.initSingleTypeRecycleView(LinearLayoutManager(this.context), mAdapter, false)
        common_recycler.refreshController().setEnablePullToRefresh(false)
        common_recycler.emptyController().setEmpty()

        common_recycler.recyclerViewController().recyclerView.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_UP -> {
                    KeyboardktUtils.hideKeyboard(common_recycler)
                }
            }
            false
        }
    }

    private fun initData() {
        mKeyword = arguments?.getString("keyword") ?: ""

        mAdapter.setSelectedUid(mSelectList as java.util.ArrayList<Long>)

        when {
            mGroupId ?: 0 > 0 -> {
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
            mAddedUserId ?: 0 > 0 -> {
                //新建群，但有预选用户
                mAdapter.addUnSelectUid(mAddedUserId ?: 0)
                loadContacts()
            }
            mOperate == 4 -> {
                //群发
                loadContacts()
            }
            mOperate == 3 -> {
                ArouterServiceManager.contactService.getAllDisShowOnlineCacheContacts({ contacts ->
                    val uids = mutableListOf<Long>()
                    contacts.forEach {
                        uids.add(it.uid)
                    }
                    mAdapter.setUnSelectUids(uids)
                    loadContacts()
                }, { activity?.finish() })
            }
            else -> {// 新建群
                loadContacts()
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun registerEvent() {
        EventBus.getFlowable(CreateGroupSearchMemberEvent::class.java)
                .bindToLifecycle(this@OperateContactSearchFragment)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (activity != null) {
                        if (it.from == 1) {
                            mKeyword = it.keyword
                            mAdapter.setKeyword(mKeyword)
                            loadContacts()
                        }
                    }
                }

        EventBus.getFlowable(CreateGroupSelectMemberEvent::class.java)
                .bindToLifecycle(this@OperateContactSearchFragment)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.from == 1) {
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

    override fun onDestroy() {
        super.onDestroy()
        mRealm.removeAllChangeListeners()
        mRealm.close()
    }

    @SuppressLint("CheckResult")
    private fun loadContacts() {
        Flowable.just<Realm>(mRealm)
                .compose(RxLifecycle.bindUntilEvent(lifecycle(), FragmentEvent.DESTROY))
                .subscribeOn(AndroidSchedulers.mainThread())
                .map {
                    it.where(ContactDataModel::class.java).equalTo("bfMyContacts", true)?.equalTo("bfMyBlack", false)?.beginGroup()?.like("nickName", "*$mKeyword*", Case.INSENSITIVE)?.or()?.like("noteName", "*$mKeyword*", Case.INSENSITIVE)?.endGroup()?.findAllAsync()
                }
                .observeOn(AndroidSchedulers.mainThread()).subscribe {
                    mContactModels = it
                    mContactModels?.addChangeListener(this@OperateContactSearchFragment)
                }
    }

    override fun onChange(t: RealmResults<ContactDataModel>) {
        if (!t.isValid)
            return

        if (activity == null || context == null) {
            return
        }

        mListDatas.clear()

        for (i in t.indices) {
            val currentItem = t[i]
            mListDatas.add(OperateContactSearchAdapter.ItemData(currentItem))
        }

        mAdapter.setNewData(mListDatas)
    }
}
