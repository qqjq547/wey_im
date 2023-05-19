package framework.telegram.business.ui.search.expand.expand

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.Search.INDEX_ID
import framework.telegram.business.bridge.Constant.Search.KEYWORD
import framework.telegram.business.bridge.Constant.Search.SEARCH_CHAT_CONTENT
import framework.telegram.business.bridge.Constant.Search.SEARCH_CHAT_CONTENT_PRIVATE
import framework.telegram.business.bridge.Constant.Search.SEARCH_GROUP_CHAT_CONTENT
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.event.SearchFinishEvent
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.search.contract.SearchContract
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.screenshot.ScreenShotsUtils
import framework.telegram.ui.utils.KeyboardktUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_search_activity.*
import kotlin.math.abs

/**
 * Created by lzh on 19-6-17.
 * INFO:
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_SEARCH_EXPAND)
class SearchExpandActivity : BaseBusinessActivity<SearchContract.Presenter>(), SearchContract.View {

    private val mAdapter by lazy { SearchExpandAdapter() }
    private val mSearchType by lazy { intent.getIntExtra(Constant.Search.SEARCH_TYPE, 0) }


    private var mKeyword = ""
    private var mPageNum = 1

    override fun isActive() = true

    override fun getLayoutId() = R.layout.bus_search_activity

    override fun initView() {

        mKeyword = intent.getStringExtra(KEYWORD)?:""

        SearchExpandPresenterImpl(this, this, lifecycle(),mSearchType)

        back_image.visibility = View.VISIBLE

        custom_toolbar.setToolbarSize(0f)
        custom_search_bar.setSearBarListen({
            EventBus.publishEvent(SearchFinishEvent())
            finish()
        }) {
            mKeyword = it.replace("*", "").trim()//过滤掉×
            layout_video_file.visibility = View.GONE
            getContactData(it)
        }

        common_recycler.initSingleTypeRecycleView(LinearLayoutManager(this), mAdapter, false)

        common_recycler.refreshController().setEnablePullToRefresh(false)
        common_recycler.emptyController().setTopEmpty(getString(R.string.no_data))

        common_recycler.visibility = View.INVISIBLE
        if (!TextUtils.isEmpty(mKeyword))
            custom_search_bar.getEditView().setText(mKeyword)

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

        back_image.setOnClickListener {
            finish()
        }

        EventBus.getFlowable(SearchFinishEvent::class.java)
                .bindToLifecycle(this@SearchExpandActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { finish() }
    }

    override fun initData() {
    }

    @SuppressLint("CheckResult")
    fun getContactData(keyword: String) {
        mPageNum = 1
        mPresenter?.getDataSearchList(keyword, mutableListOf(), mPageNum)
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
        common_recycler.visibility = View.VISIBLE
        mAdapter.setKeyword(mKeyword)
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
    }
}


