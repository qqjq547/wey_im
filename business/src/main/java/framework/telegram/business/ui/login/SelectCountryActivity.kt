package framework.telegram.business.ui.login

import android.annotation.SuppressLint
import android.graphics.Typeface
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.telegram.business.R
import framework.telegram.business.bean.CountryCodeInfoBean
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.Search.SEARCH_COUNTRY
import framework.telegram.business.bridge.Constant.Search.SEARCH_TYPE
import framework.telegram.business.event.SelectCountryEvent
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.login.adapter.CountryAdapter
import framework.telegram.business.ui.login.presenter.CountryContract
import framework.telegram.business.ui.login.presenter.CountryPresenterImpl
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.recyclerview.sticky.StickyItemDecoration
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_login_activity_select_country.*
import kotlinx.android.synthetic.main.bus_login_item_select_country_head.*
import kotlinx.android.synthetic.main.bus_search.*


/**
 * Created by lzh on 19-5-16.
 * INFO:选择城市
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_LOGIN_SELECT_COUNTRY)
class SelectCountryActivity : BaseBusinessActivity<CountryContract.Presenter>(), CountryContract.View {

    private var mListData: List<CountryCodeInfoBean> = arrayListOf()

    private val mAdapter by lazy { CountryAdapter() }

    override fun getLayoutId() = R.layout.bus_login_activity_select_country

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        text_view_head_name.text = ""

        index_fast_scroll_recycler_view.initIndexFastScrollRecyclerView(
                LinearLayoutManager(this),
                mAdapter,
                false)
        index_fast_scroll_recycler_view.indexFastScrollController()
                .setIndexBarColor(this.resources.getColor(R.color.white))
                .setIndexBarTextColor(this.resources.getColor(R.color.a2a4a7))
                .setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
                .setIndexbarMargin(0f)
                .setIndexTextSize(11)
                .addItemDecoration(StickyItemDecoration(sticky_head_container, 0))
    }

    @SuppressLint("CheckResult")
    override fun initListen() {
        text_view_search_icon.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT)
                    .withInt(SEARCH_TYPE, SEARCH_COUNTRY).navigation()
        }

        sticky_head_container.setDataCallback {
            if (mListData[it].itemType == Constant.Search.SEARCH_HEAD) {
                text_view_head_name.text = mListData[it].getLetterByLanguage()
            }
        }

        mAdapter.setOnItemClickListener { adapter, view, position ->
            EventBus.publishEvent(SelectCountryEvent("+" + mListData[position].getCountryCode()))
        }

        EventBus.getFlowable(SelectCountryEvent::class.java)
                .bindToLifecycle(this@SelectCountryActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    finish()
                }
    }

    override fun initData() {
        CountryPresenterImpl(this, this, lifecycle()).start()
    }

    override fun showLoading() {
    }

    override fun getListSuccess(dataList: List<CountryCodeInfoBean>?) {
        runOnUiThread {
            mListData = dataList!!
            mAdapter.setNewData(mListData)
        }
    }

    override fun showErrMsg(str: String?) {
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as CountryContract.Presenter
    }

    override fun isActive(): Boolean = isFinishing
}