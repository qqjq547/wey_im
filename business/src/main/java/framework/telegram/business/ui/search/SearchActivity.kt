package framework.telegram.business.ui.search

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.Search.FORWARD_IDS
import framework.telegram.business.bridge.Constant.Search.SEARCH_BLACK
import framework.telegram.business.bridge.Constant.Search.SEARCH_CALL
import framework.telegram.business.bridge.Constant.Search.SEARCH_CARD_CONTACTS
import framework.telegram.business.bridge.Constant.Search.SEARCH_COUNTRY
import framework.telegram.business.bridge.Constant.Search.SEARCH_FORWARD_CONTACTS
import framework.telegram.business.bridge.Constant.Search.SEARCH_FORWARD_CONTACTS_GROUP
import framework.telegram.business.bridge.Constant.Search.SEARCH_FORWARD_GROUP
import framework.telegram.business.bridge.Constant.Search.SEARCH_GROUP_ADD_ADMIN
import framework.telegram.business.bridge.Constant.Search.SEARCH_GROUP_ALL_MEMBER
import framework.telegram.business.bridge.Constant.Search.SEARCH_GROUP_AT_MEMBER
import framework.telegram.business.bridge.Constant.Search.SEARCH_GROUP_TAN_OWNER
import framework.telegram.business.bridge.Constant.Search.SEARCH_MY_GROUP
import framework.telegram.business.bridge.Constant.Search.SEARCH_NEW_CALL
import framework.telegram.business.bridge.Constant.Search.SEARCH_SHARE_CARD_CONTACTS
import framework.telegram.business.bridge.Constant.Search.SEARCH_SHARE_CARD_CONTACTS_GROUP
import framework.telegram.business.bridge.Constant.Search.SEARCH_SHARE_CONTACTS
import framework.telegram.business.bridge.Constant.Search.SEARCH_SHARE_CONTACTS_GROUP
import framework.telegram.business.bridge.Constant.Search.SEARCH_SHARE_GROUP
import framework.telegram.business.bridge.Constant.Search.SEARCH_TARGET_ID
import framework.telegram.business.bridge.Constant.Search.SEARCH_TYPE
import framework.telegram.business.bridge.Constant.Search.SEARCH_UN_VIEW_ONLINE
import framework.telegram.business.bridge.Constant.Search.TARGET_NAME
import framework.telegram.business.bridge.Constant.Search.TARGET_PIC
import framework.telegram.business.bridge.event.*
import framework.telegram.business.bridge.service.ISearchAdapterService
import framework.telegram.business.event.SelectCountryEvent
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.search.adapter.SearchAdapter
import framework.telegram.business.ui.search.contract.SearchContract
import framework.telegram.business.ui.search.presenter.*
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_SERVICE_ADAPTER_CALL
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_SERVICE_ADAPTER_NEW_CALL
import framework.telegram.message.bridge.event.SearchChatEvent
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.screenshot.ScreenShotsUtils
import framework.telegram.ui.utils.KeyboardktUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_search_activity.*

/**
 * Created by lzh on 19-6-17.
 * INFO:
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT)
class SearchActivity : BaseBusinessActivity<SearchContract.Presenter>(), SearchContract.View {

    private val mAdapter by lazy { SearchAdapter(mAdapterList) }
    private val mSearchType by lazy { intent.getIntExtra(SEARCH_TYPE, 0) }
    private val mSearchTargetId by lazy { intent.getLongExtra(SEARCH_TARGET_ID, 0) }
    private val mTargetPic by lazy { intent.getStringExtra(TARGET_PIC) ?: "" }
    private val mTargetName by lazy { intent.getStringExtra(TARGET_NAME) ?: "" }
    private val mMapList by lazy { mutableListOf<MutableMap<String, String>>() }

    private val mAdapterList by lazy { mutableListOf<ISearchAdapterService>() }
    private val mForwardIdSet by lazy {
        intent.getStringArrayListExtra(FORWARD_IDS) ?: arrayListOf()
    }
    private var mKeyword = ""
    private var mPageNum = 1

    override fun isActive() = true

    override fun getLayoutId() = R.layout.bus_search_activity

    override fun initView() {
        if (mSearchType == 0)
            return

        when (mSearchType) {
            SEARCH_COUNTRY -> {//搜索城市
                mAdapterList.add(
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_COUNTRY)
                        .navigation() as ISearchAdapterService
                )
                SearchCountryPresenterImpl(this, this, lifecycle())
            }
            SEARCH_MY_GROUP -> {//我保存的群组
                mAdapterList.add(
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_GROUP)
                        .navigation() as ISearchAdapterService
                )
                SearchMyGroupPresenterImpl(this, this, lifecycle())
            }
            SEARCH_CALL -> {//通话
                mAdapterList.add(
                    ARouter.getInstance().build(ROUNTE_SERVICE_ADAPTER_CALL)
                        .navigation() as ISearchAdapterService
                )
                SearchCallPresenterImpl(this, this, lifecycle())
            }
            SEARCH_NEW_CALL -> {//新通话
                mAdapterList.add(
                    ARouter.getInstance().build(ROUNTE_SERVICE_ADAPTER_NEW_CALL)
                        .navigation() as ISearchAdapterService
                )
                SearchNewCallPresenterImpl(this, this, lifecycle())
            }
            SEARCH_BLACK -> {//黑名单
                mAdapterList.add(
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_CONTACT)
                        .navigation() as ISearchAdapterService
                )
                SearchBlackContactsPresenterImpl(this, this, lifecycle())
            }
            SEARCH_GROUP_ALL_MEMBER -> {//群所有人员
                mAdapterList.add(
                    ARouter.getInstance()
                        .build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_GROUP_MEMBER)
                        .navigation() as ISearchAdapterService
                )
                SearchGroupMemberByNetPresenterImpl(
                    this,
                    this,
                    mSearchTargetId,
                    true,
                    lifecycle(),
                    SEARCH_GROUP_ALL_MEMBER
                )
            }
            SEARCH_GROUP_AT_MEMBER -> {//@群成员
                mAdapterList.add(
                    ARouter.getInstance()
                        .build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_GROUP_MEMBER)
                        .navigation() as ISearchAdapterService
                )
                SearchGroupMemberByNetPresenterImpl(
                    this,
                    this,
                    mSearchTargetId,
                    false,
                    lifecycle(),
                    SEARCH_GROUP_AT_MEMBER
                )
            }
            SEARCH_GROUP_TAN_OWNER -> {//  转让群主
                mAdapterList.add(
                    ARouter.getInstance()
                        .build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_GROUP_MEMBER)
                        .navigation() as ISearchAdapterService
                )
                SearchGroupMemberByNetPresenterImpl(
                    this,
                    this,
                    mSearchTargetId,
                    false,
                    lifecycle(),
                    SEARCH_GROUP_TAN_OWNER
                )
            }
            SEARCH_GROUP_ADD_ADMIN -> {//增加管理员
                mAdapterList.add(
                    ARouter.getInstance()
                        .build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_GROUP_MEMBER)
                        .navigation() as ISearchAdapterService
                )
                SearchGroupMemberByNetPresenterImpl(
                    this,
                    this,
                    mSearchTargetId,
                    true,
                    lifecycle(),
                    SEARCH_GROUP_ADD_ADMIN
                )
            }
            SEARCH_FORWARD_CONTACTS -> {//转发给联系人
                mAdapterList.add(
                    ARouter.getInstance()
                        .build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_FORWARD_CONTACT)
                        .navigation() as ISearchAdapterService
                )
                SearchContactsPresenterImpl(this, this, lifecycle())
            }
            SEARCH_FORWARD_CONTACTS_GROUP -> {//转发到群 或者联系人
                mAdapterList.add(
                    ARouter.getInstance()
                        .build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_FORWARD_CONTACT)
                        .navigation() as ISearchAdapterService
                )
                mAdapterList.add(
                    ARouter.getInstance()
                        .build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_FORWARD_GROUP)
                        .navigation() as ISearchAdapterService
                )
                SearchContactsAndGroupPresenterImpl2(this, this, lifecycle())
            }
            SEARCH_FORWARD_GROUP -> {//转发到群
                mAdapterList.add(
                    ARouter.getInstance()
                        .build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_FORWARD_GROUP)
                        .navigation() as ISearchAdapterService
                )
                SearchMyGroupPresenterImpl(this, this, lifecycle())
            }
            SEARCH_CARD_CONTACTS -> {//把名片发送到群或人
                mAdapterList.add(
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_CONTACT)
                        .navigation() as ISearchAdapterService
                )
                SearchContactsPresenterImpl(
                    this,
                    this,
                    lifecycle(),
                    mTargetPic,
                    mTargetName
                ).start()
            }

            SEARCH_SHARE_CARD_CONTACTS -> {//
                mAdapterList.add(
                    ARouter.getInstance()
                        .build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_FORWARD_CONTACT)
                        .navigation() as ISearchAdapterService
                )
                SearchContactsPresenterImpl(
                    this,
                    this,
                    lifecycle(),
                    mTargetPic,
                    mTargetName
                ).start()
            }
            SEARCH_SHARE_CARD_CONTACTS_GROUP -> {//
                mAdapterList.add(
                    ARouter.getInstance()
                        .build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_FORWARD_CONTACT)
                        .navigation() as ISearchAdapterService
                )
                mAdapterList.add(
                    ARouter.getInstance()
                        .build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_FORWARD_GROUP)
                        .navigation() as ISearchAdapterService
                )
                SearchContactsAndGroupPresenterImpl2(this, this, lifecycle()).start()
            }
            SEARCH_UN_VIEW_ONLINE -> {//
                mAdapterList.add(
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_CONTACT)
                        .navigation() as ISearchAdapterService
                )
                SearchUnViewContactsPresenterImpl(this, this, lifecycle())
            }

            //分享联系人
            SEARCH_SHARE_CONTACTS -> {
                mAdapterList.add(
                    ARouter.getInstance()
                        .build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_FORWARD_CONTACT)
                        .navigation() as ISearchAdapterService
                )
                SearchContactsPresenterImpl(this, this, lifecycle())
            }

            //分享联系人和群
            SEARCH_SHARE_CONTACTS_GROUP -> {
                mAdapterList.add(
                    ARouter.getInstance()
                        .build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_FORWARD_CONTACT)
                        .navigation() as ISearchAdapterService
                )
                mAdapterList.add(
                    ARouter.getInstance()
                        .build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_FORWARD_GROUP)
                        .navigation() as ISearchAdapterService
                )
                SearchContactsAndGroupPresenterImpl2(this, this, lifecycle())
            }

            //分享群
            SEARCH_SHARE_GROUP -> {
                mAdapterList.add(
                    ARouter.getInstance()
                        .build(Constant.ARouter.ROUNTE_SERVICE_ADAPTER_FORWARD_GROUP)
                        .navigation() as ISearchAdapterService
                )
                SearchMyGroupPresenterImpl(this, this, lifecycle())
            }
        }

        custom_toolbar.setToolbarSize(0f)
        custom_search_bar.setSearBarListen({
            finish()
        }) {
            mKeyword = it.replace("*", "").trim()//过滤掉×
            layout_video_file.visibility = View.GONE
            getContactData(it)
        }

        if (SEARCH_GROUP_AT_MEMBER == mSearchType || SEARCH_GROUP_ALL_MEMBER == mSearchType) {
            common_recycler.initSingleTypeRecycleView(LinearLayoutManager(this), mAdapter, true)
            common_recycler.loadMoreController().setOnLoadMoreListener {
                mPresenter?.getDataSearchList(mKeyword, mAdapter.data, ++mPageNum)
            }
        } else {
            common_recycler.initSingleTypeRecycleView(LinearLayoutManager(this), mAdapter, false)
        }

        common_recycler.refreshController().setEnablePullToRefresh(false)
        common_recycler.emptyController().setTopEmpty(getString(R.string.no_data))
        common_recycler.visibility = View.INVISIBLE

        custom_search_bar.getEditView().requestFocus()
        common_recycler.post {
            ThreadUtils.runOnUIThread(300) {
                KeyboardktUtils.showKeyboard(custom_search_bar.getEditView())
            }
        }
    }

    @SuppressLint("CheckResult")
    override fun initListen() {
        common_recycler.recyclerViewController().recyclerView.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_UP -> {
                    KeyboardktUtils.hideKeyboard(all_layout)
                }
            }
            false
        }

        all_layout.setOnClickListener {
            KeyboardktUtils.hideKeyboard(all_layout)
        }

        EventBus.getFlowable(SelectCountryEvent::class.java)
            .bindToLifecycle(this@SearchActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { finish() }
        EventBus.getFlowable(SearchGroupOperateEvent::class.java)
            .bindToLifecycle(this@SearchActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { finish() }
        EventBus.getFlowable(ForwardFinishEvent::class.java)
            .bindToLifecycle(this@SearchActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { finish() }
        EventBus.getFlowable(ShareFinishEvent::class.java)
            .bindToLifecycle(this@SearchActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { finish() }

        EventBus.getFlowable(SearchCardFinishEvent::class.java)
            .bindToLifecycle(this@SearchActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { finish() }

        EventBus.getFlowable(SearchCardEvent::class.java)
            .bindToLifecycle(this@SearchActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { finish() }

        EventBus.getFlowable(SearchChatEvent::class.java)
            .bindToLifecycle(this@SearchActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { finish() }

        EventBus.getFlowable(SearchFinishEvent::class.java)
            .bindToLifecycle(this@SearchActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { finish() }

        EventBus.getFlowable(SearchSetBlackEvent::class.java)
            .bindToLifecycle(this@SearchActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (!TextUtils.isEmpty(mKeyword)) {
                    getContactData(mKeyword)
                }
            }
    }

    override fun initData() {
    }

    @SuppressLint("CheckResult")
    fun getContactData(keyword: String) {
        mPageNum = 1
        mPresenter?.getDataSearchList(keyword, mutableListOf(), mPageNum)
    }

    override fun onPause() {
        super.onPause()
        ScreenShotsUtils.stopScreenShotsListen(this@SearchActivity)
    }


    override fun finish() {
        super.finish()
        KeyboardktUtils.hideKeyboard(common_recycler)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter?.destroy()
    }

    override fun getDataListSuccess(list: MutableList<MultiItemEntity>, hasMore: Boolean) {
        mAdapter.setSearchTargerId(mSearchTargetId)
        mAdapter.setExtra(mMapList)
        mAdapter.setKeyword(mKeyword)
        mAdapter.setSearchType(mSearchType)
        mForwardIdSet?.let {
            val set = mutableSetOf<Long>()
            mForwardIdSet.forEach {
                set.add(it.toLong())
            }
            mAdapter.setDataSet(set)
        }
        common_recycler.visibility = View.VISIBLE
        mAdapter.setNewData(list)

        if (!hasMore) {
            common_recycler.loadMoreController().loadMoreEnd()
        } else {
            common_recycler.loadMoreController().loadMoreComplete()
        }
    }

    override fun showLoading() {
    }

    override fun showError(errStr: String?) {
        toast(errStr ?: "")
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as SearchContract.Presenter
    }

    override fun setMapListData(mapList: List<MutableMap<String, String>>) {
        mMapList.clear()
        mMapList.addAll(mapList)
    }

}


