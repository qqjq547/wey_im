package framework.telegram.business.ui.base

import android.os.Bundle
import framework.telegram.support.BaseActivity
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.ui.dialog.AppDialog
import io.reactivex.disposables.CompositeDisposable

/**
 * Created by lzh on 19-5-14.
 * INFO: public abstract class
 */
abstract class BaseBusinessActivity <T: BasePresenter> : BaseActivity() {
    protected var dialog: AppDialog? = null

    protected var mPresenter: T? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (getLayoutId() <= 0 || !checkFields()) {
            finish()
            return
        }

        setContentView(getLayoutId())
        initView()
        initListen()
        initData()
    }

    open fun checkFields(): Boolean {
        return true
    }

    abstract fun getLayoutId(): Int

    abstract fun initView()

    abstract fun initListen()

    abstract fun initData()

}