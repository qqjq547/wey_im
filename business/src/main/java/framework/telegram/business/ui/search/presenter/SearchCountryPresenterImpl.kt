package framework.telegram.business.ui.search.presenter

import android.annotation.SuppressLint
import android.content.Context
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.R
import framework.telegram.business.bean.CountryCodeInfoBean
import framework.telegram.business.ui.search.contract.SearchContract
import framework.telegram.business.utils.CountryUtil
import io.reactivex.Observable

class SearchCountryPresenterImpl : SearchContract.Presenter {
    private var mCountryBeans: List<CountryCodeInfoBean>? = null
    private var mContext: Context?
    private var mView: SearchContract.View
    private var mObservalbe: Observable<ActivityEvent>

    constructor(view: SearchContract.View, context: Context?, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {

    }

    @SuppressLint("CheckResult")
    override fun getDataSearchList(keyword: String, dataList: MutableList<MultiItemEntity>,pageNum:Int) {
        if (mCountryBeans == null) {
            mCountryBeans = CountryUtil.getCountryListSync()
        }

        if (mCountryBeans != null) {
            CountryUtil.populateAsync(mCountryBeans, keyword, true, {
                mView.showError(mContext?.getString(R.string.the_locale_information_failed_to_load))
            }) {
                mView.getDataListSuccess(it?.toMutableList() as MutableList<MultiItemEntity>,false)
            }
        } else {
            mView.showError(mContext?.getString(R.string.the_locale_information_failed_to_load))
        }
    }

    override fun destroy() {
    }
}

