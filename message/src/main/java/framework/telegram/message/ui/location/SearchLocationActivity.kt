package framework.telegram.message.ui.location

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.ui.location.adapter.PlaceAdapter
import framework.telegram.message.ui.location.bean.ClientLocationStore
import framework.telegram.message.ui.location.bean.POIBean
import framework.telegram.message.ui.location.presenter.DataListContract
import framework.telegram.message.ui.location.presenter.DataListPresenterImpl
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.tools.ExpandClass.toast
import kotlinx.android.synthetic.main.msg_location_search_activity.*


/**
 * Created by kuang on 2015/4/10.
 */
@Route(path = Constant.ARouter.ROUNTE_LOCATION_SEARCH_ACTIVITY)
class SearchLocationActivity : BaseActivity(), DataListContract.View {

    private val mAdapter by lazy { PlaceAdapter(false) }
    private val mLat by lazy { ClientLocationStore.getLastClientLatLng()?.longLat ?: 0L }
    private val mlng by lazy { ClientLocationStore.getLastClientLatLng()?.longLng ?: 0L }

    private val mDataImpl by lazy { DataListPresenterImpl(this, this@SearchLocationActivity, activityObservalbe = lifecycle()) }

    private var mKeyword = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.msg_location_search_activity)

        initTitleBar()
        initView()
        setListener()
        initData()
    }

    private fun initView() {
        common_recycler?.initSingleTypeRecycleView(LinearLayoutManager(this@SearchLocationActivity), mAdapter, true)
        common_recycler.refreshController().setEnablePullToRefresh(false)
        common_recycler.emptyController().setEmpty()
        common_recycler.loadMoreController().setOnLoadMoreListener {
            mDataImpl.getDataList(mLat, mlng, mKeyword)
        }
    }

    private fun initData() {
    }

    private fun setListener() {
        custom_search_bar.setSearBarListen({
            finish()
        }) {
            mKeyword = it
            mAdapter.setKeyword(it)
            mDataImpl.getFirstDataList(mLat, mlng, it)
        }

        mAdapter.setOnItemChildClickListener { _, _, position ->
            val data = Intent()
            data.putExtra("data", mAdapter.getItem(position))
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    private fun initTitleBar() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            onBackPressed()
        }
        custom_toolbar.showCenterTitle(getString(R.string.pet_text_281))

    }

    override fun showLoading() {

    }

    override fun showData(list: MutableList<POIBean>, hasMore: Boolean) {
        mAdapter.setNewData(list)

        if (!hasMore){
            common_recycler.loadMoreController().loadMoreEnd()
        } else {
            common_recycler.loadMoreController().loadMoreComplete()
        }
    }

    override fun showErrMsg(str: String?) {
        BaseApp.app.toast(str.toString())
    }

    override fun showEmpty() {

    }

}
