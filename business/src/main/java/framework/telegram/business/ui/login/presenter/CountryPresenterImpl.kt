package framework.telegram.business.ui.login.presenter

import android.content.Context
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.R
import framework.telegram.business.bean.CountryCodeInfoBean
import framework.telegram.business.utils.CountryUtil
import io.reactivex.Observable

class CountryPresenterImpl : CountryContract.Presenter {

    private var mCountryBeans: List<CountryCodeInfoBean>? = null
    private val mContext: Context
    private val mView: CountryContract.View
    private val mViewObservalbe: Observable<ActivityEvent>

    constructor(view: CountryContract.View, context: Context, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {
        getCountryList()
    }

    override fun getCountryList() {
        if (mCountryBeans == null) {
            mCountryBeans = CountryUtil.getCountryListSync()
        }
        if (mCountryBeans != null) {
            CountryUtil.populateAsync(mCountryBeans,null,error = {
                mView.showErrMsg(mContext?.getString(R.string.the_locale_information_failed_to_load))
            }){
                mView.getListSuccess(it)
            }
        } else {
            mView.showErrMsg(mContext?.getString(R.string.the_locale_information_failed_to_load))
        }
    }
}