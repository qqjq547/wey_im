package framework.telegram.support.mvp

interface BaseView<T> {
    fun setPresenter(presenter: BasePresenter)

    fun isActive(): Boolean
}