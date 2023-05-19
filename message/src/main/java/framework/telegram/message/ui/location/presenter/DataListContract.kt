package framework.telegram.message.ui.location.presenter

import framework.telegram.message.ui.location.bean.POIBean


/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface DataListContract {

    interface Presenter {

        fun getFirstDataList(lat:Long,lng:Long,keyword:String)

        fun getDataList(lat:Long,lng:Long,keyword:String)

        fun setHeadData(bean:POIBean)

        fun setCheckList(position:Int)

        fun getGoogleUrl(lat :Int ,lng :Int)
    }

    interface View {
        fun showLoading()

        fun showData(list: MutableList<POIBean>,hasMore:Boolean)

        fun showEmpty()

        fun showErrMsg(str: String?)
    }
}