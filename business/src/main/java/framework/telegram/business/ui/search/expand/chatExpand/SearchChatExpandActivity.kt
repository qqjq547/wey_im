package framework.telegram.business.ui.search.expand.chatExpand

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.entity.MultiItemEntity
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
import kotlinx.android.synthetic.main.bus_search_activity.*
import kotlin.math.abs

/**
 * Created by lzh on 19-6-17.
 * INFO:
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_SEARCH_CHAT_EXPAND)
class SearchChatExpandActivity : BaseBusinessActivity<SearchContract.Presenter>(), SearchContract.View {

    private val mAdapter by lazy { SearchChatAdapter() }
    private val mIndexId by lazy { intent.getLongExtra(INDEX_ID, 0) }
    private val mSearchType by lazy { intent.getIntExtra(Constant.Search.SEARCH_TYPE, 0) }

    private val mIsShowBack by lazy { intent.getBooleanExtra(Constant.Search.SEARCH_BACK, false) }

    private val mTargetName by lazy { intent.getStringExtra(Constant.Search.TARGET_NAME) }

    private val mMatchCount by lazy { intent.getIntExtra(Constant.Search.SEARCH_MATCH_COUNT, 0) }

    private var mKeyword = ""
    private var mPageNum = 1

    override fun isActive() = true

    override fun getLayoutId() = R.layout.bus_search_activity

    override fun initView() {

        mKeyword = intent.getStringExtra(KEYWORD)?:""

        SearchChatExpandPresenterImpl(this, this, lifecycle(),mIndexId,mTargetName,mMatchCount)

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

        if (mSearchType == SEARCH_CHAT_CONTENT || mSearchType == SEARCH_GROUP_CHAT_CONTENT
                || mSearchType ==SEARCH_CHAT_CONTENT_PRIVATE){
            common_recycler.emptyController().setEmpty(resources.getString(R.string.no_chat_record_was_found),R.drawable.common_icon_empty_data)
            layout_video_file.visibility = View.VISIBLE
        }else{
            common_recycler.emptyController().setEmpty(R.drawable.common_icon_empty_data)
        }

        common_recycler.visibility = View.INVISIBLE
        if (mIsShowBack){
            back_image.visibility = View.VISIBLE
        }
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

        layout_video.setOnClickListener {
            if (mSearchType == SEARCH_CHAT_CONTENT || mSearchType == SEARCH_CHAT_CONTENT_PRIVATE) {
                ARouter.getInstance().build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_MEDIA_MANAGER)
                        .withInt("chatType", ChatModel.CHAT_TYPE_PVT)
                        .withLong("chaterId", mIndexId)
                        .withInt("curPager",0)
                        .navigation()
            }else if( mSearchType == SEARCH_GROUP_CHAT_CONTENT){
                ARouter.getInstance().build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_MEDIA_MANAGER)
                        .withInt("chatType", ChatModel.CHAT_TYPE_GROUP)
                        .withLong("chaterId", abs(mIndexId))
                        .withInt("curPager",0)
                        .navigation()
            }
        }

        layout_file.setOnClickListener {
            if (mSearchType == SEARCH_CHAT_CONTENT || mSearchType == SEARCH_CHAT_CONTENT_PRIVATE) {
                ARouter.getInstance().build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_MEDIA_MANAGER)
                        .withInt("chatType", ChatModel.CHAT_TYPE_PVT)
                        .withLong("chaterId", mIndexId)
                        .withInt("curPager",1)
                        .navigation()
            }else if( mSearchType == SEARCH_GROUP_CHAT_CONTENT){
                ARouter.getInstance().build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_MEDIA_MANAGER)
                        .withInt("chatType", ChatModel.CHAT_TYPE_GROUP)
                        .withLong("chaterId",abs( mIndexId))
                        .withInt("curPager",1)
                        .navigation()
            }
        }

        back_image.setOnClickListener {
            finish()
        }
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
        if (TextUtils.isEmpty(mKeyword)){
            if (mSearchType == SEARCH_CHAT_CONTENT || mSearchType == SEARCH_GROUP_CHAT_CONTENT
                    || mSearchType ==SEARCH_CHAT_CONTENT_PRIVATE) {
                layout_video_file.visibility = View.VISIBLE
            }
            common_recycler.visibility = View.INVISIBLE
        }else{
            common_recycler.visibility = View.VISIBLE
        }
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

    override fun onResume() {
        super.onResume()
        if (mSearchType == SEARCH_CHAT_CONTENT_PRIVATE)
            ArouterServiceManager.contactService.getContactInfo(lifecycle(), mIndexId, { contactInfoModel, _ ->
                if (contactInfoModel.isBfScreenshot) {
                    ScreenShotsUtils.startScreenShotsListen(this@SearchChatExpandActivity) {
                        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                        ArouterServiceManager.messageService.sendScreenShotsPackage(myUid, mIndexId)
                    }
                }
            })
    }
}


