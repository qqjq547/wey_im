package framework.telegram.business.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import framework.telegram.support.BaseFragment
import framework.telegram.support.mvp.BasePresenter
import io.reactivex.disposables.CompositeDisposable


/**
 * Created by lzh on 19-5-14.
 * INFO:
 */
abstract class BaseBusinessFragment<T: BasePresenter> : BaseFragment() {

    protected var mPresenter: T? = null
    protected var mRoot: View? = null

    private var mIsLoadData = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mRoot = inflater.inflate(getLayoutId(), container, false)
        return mRoot
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListen()

        if (isLazyLoad() && userVisibleHint) {
            initData()
            mIsLoadData = true
        } else if (!isLazyLoad()) {
            initData()
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isLazyLoad() && !mIsLoadData && isVisibleToUser) {
            mIsLoadData = true
            initData()
        }
    }

    protected open fun isLazyLoad(): Boolean = false

    abstract fun getLayoutId(): Int

    abstract fun initView()

    abstract fun initListen()

    abstract fun initData()

}