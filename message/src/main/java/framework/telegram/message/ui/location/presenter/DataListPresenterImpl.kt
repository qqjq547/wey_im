package framework.telegram.message.ui.location.presenter

import android.content.Context
import android.util.Log
import com.im.domain.pb.CommonProto
import com.im.domain.pb.SysProto
import com.trello.rxlifecycle3.android.ActivityEvent
import com.trello.rxlifecycle3.android.FragmentEvent
import framework.telegram.message.http.HttpManager
import framework.telegram.message.http.creator.SysHttpReqCreator
import framework.telegram.message.http.getResult
import framework.telegram.message.http.getResultForFragment
import framework.telegram.message.http.protocol.SystemHttpProtocol
import framework.telegram.message.ui.location.bean.POIBean
import framework.telegram.message.ui.location.utils.MapUtils
import framework.telegram.support.system.network.http.HttpReq
import io.reactivex.Observable

class DataListPresenterImpl : DataListContract.Presenter {


    private val mPageSize = 15
    private val mContext: Context?
    private val mView: DataListContract.View
    private var mActivityObservalbe: Observable<ActivityEvent>? = null
    private var mFragmentObservalbe: Observable<FragmentEvent>? = null


    private val mList = mutableListOf<POIBean>()
    private var mPOIBean: POIBean? = null

    private var mPageNum = 1

    constructor(view: DataListContract.View, context: Context?, fragmentObservalbe: Observable<FragmentEvent>? = null, activityObservalbe: Observable<ActivityEvent>? = null) {
        this.mView = view
        this.mFragmentObservalbe = fragmentObservalbe
        this.mActivityObservalbe = activityObservalbe
        this.mContext = context
    }


    override fun setHeadData(bean: POIBean) {
        mPOIBean = bean
        mPOIBean?.isCheck = true
    }

    override fun setCheckList(position:Int){
        val data = mList[position]
        mList.forEach {
            it.isCheck = it.id == data.id
        }
    }

    override fun getFirstDataList(lat: Long, lng: Long, keyword: String) {
        getList(lat,lng,keyword,1)
    }

    override fun getDataList(lat: Long, lng: Long, keyword: String) {
        getList(lat,lng,keyword,mPageNum +1)
    }

    private fun getList(lat: Long, lng: Long, keyword: String,pageNum:Int){
        var type = CommonProto.SearchMapType.GAODE
        if (MapUtils.isGoogleMapData(mContext, lat.toDouble() / 1000000, lng.toDouble()/ 1000000)) {
            type = CommonProto.SearchMapType.GOOGLE
        }
        val ob = HttpManager.getStore(SystemHttpProtocol::class.java)
                .getPlaceList(object : HttpReq<SysProto.PlaceListReq>() {
                    override fun getData(): SysProto.PlaceListReq {
                        return SysHttpReqCreator.getPlaceList((lat ).toInt(), (lng ).toInt(), type,keyword, pageNum,mPageSize)
                    }
                })
        if (mActivityObservalbe!=null){
            ob .getResult(mActivityObservalbe, {
                        getReuslt(pageNum,it)
                    }, {
                        mView.showErrMsg(it.message)
                    })
        }else if(mFragmentObservalbe !=null){
            ob.getResultForFragment(mFragmentObservalbe, {
                        getReuslt(pageNum,it)
                    }, {
                        mView.showErrMsg(it.message)
                    })
        }

    }

    private fun getReuslt(pageNum:Int,it :SysProto.PlaceListResp){
        if (pageNum == 1){
            mList.clear()
            if (mPOIBean!=null){
                mList.add(mPOIBean!!)
            }
        }
        val list = mutableListOf<POIBean>()
        it.placesList.forEach {bean->
            val poiBean = POIBean(bean.id,bean.name,bean.address,bean.lat,bean.lng)
            list.add(poiBean)
        }
        mList.addAll(list)
        if (mList.size>0){
            mPageNum = pageNum
            mView.showData(mList,list.size >= mPageSize)
        }else{
            mView.showEmpty()
        }
    }

    override fun getGoogleUrl(lat: Int, lng: Int) {
    }

}