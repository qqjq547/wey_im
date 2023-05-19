package framework.telegram.business.ui.search.contract

import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface SearchContract {

    interface Presenter : BasePresenter {
        fun getDataSearchList(keyword :String, dataList:MutableList<MultiItemEntity>,pageNum:Int)

        fun destroy()
    }

    interface View : BaseView<Presenter> {
        //显示加载中
        fun showLoading()

        fun getDataListSuccess(list: MutableList<MultiItemEntity>,hasMore:Boolean)

        //显示错误界面
        fun showError(errStr: String?)

        fun setMapListData(mapList: List<MutableMap<String, String>>)
    }
}