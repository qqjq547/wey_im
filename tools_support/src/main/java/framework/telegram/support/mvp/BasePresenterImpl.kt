package framework.telegram.support.mvp

import java.lang.ref.WeakReference

abstract class BasePresenterImpl<V : BaseView<Any?>>(view: V) : BasePresenter {

    protected var mView: WeakReference<V>? = null

    protected val isViewActive: Boolean
        get() = mView != null && mView!!.get()?.isActive() ?: false

    init {
        mView = WeakReference(view)
//        view.setPresenter(this@BasePresenterImpl)
    }

    fun detachView() {
        mView?.clear()
        mView = null
    }
}