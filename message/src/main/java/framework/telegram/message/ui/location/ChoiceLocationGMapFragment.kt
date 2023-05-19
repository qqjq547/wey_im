package framework.telegram.message.ui.location

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import framework.telegram.message.R
import framework.telegram.message.ui.location.ChoiceLocationActivity.Companion.REQUEST_CODE_SEARCH_LOCATION
import framework.telegram.message.ui.location.adapter.PlaceAdapter
import framework.telegram.message.ui.location.bean.ClientLatLng
import framework.telegram.message.ui.location.bean.ClientLocation
import framework.telegram.message.ui.location.bean.ClientLocationStore
import framework.telegram.message.ui.location.bean.POIBean
import framework.telegram.message.ui.location.presenter.*
import framework.telegram.support.BaseApp
import framework.telegram.support.BaseFragment
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.IntentUtils
import framework.telegram.support.tools.LocationUtils
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.exception.ClientException
import framework.telegram.support.tools.exception.LocationAccessException
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.status.QMUIStatusView
import kotlinx.android.synthetic.main.msg_location_choice_gmap_fragment.*
import java.util.*

/**
 * 发送位置-谷歌地图呈现页面
 * by zst
 * Created on 2016/4/11.
 */
class ChoiceLocationGMapFragment : BaseFragment(), LocationContract.View, DataListContract.View {

    override val fragmentName: String
        get() = "ChoiceLocationGMapFragment"

    /**
     * Other
     */
    private var mTarget: LatLng? = null
    private var mGMap: GoogleMap? = null
    private var mLocationMarker: Marker? = null
    private var mChoicePosition: Int = 0
    private var isCurrentLocation: Boolean = false
    private var mChoiceLocation: POIBean? = null
    private var moveToCurrentLocationing = true
    private var moveToLocationing = true
    private var mRequestAddressRetryMaxCount = MAX_RETRY_COUNT_REQUEST_ADDRESS

    private val mQMUIStatusView by lazy { QMUIStatusView(this) }
    private val mLocationImpl by lazy { LocationPresenterImpl(this, this@ChoiceLocationGMapFragment.context, lifecycle()) }
    private val mDataImpl by lazy { DataListPresenterImpl(this, this@ChoiceLocationGMapFragment.context, lifecycle()) }

    private val mAdapter by lazy { PlaceAdapter(true) }

    private val mOnCameraChangeListener = object : GoogleMap.OnCameraChangeListener {

        override fun onCameraChange(cameraPosition: CameraPosition) {
            val lastMoveToCurrentLocationing = moveToCurrentLocationing
            val lastMoveToLocationing = moveToLocationing

            moveToCurrentLocationing = false
            moveToLocationing = false

            if (!lastMoveToLocationing) {
                mRequestAddressRetryMaxCount = MAX_RETRY_COUNT_REQUEST_ADDRESS
                requestAddress()
            }

            if (lastMoveToCurrentLocationing) {
                request_location?.setImageResource(R.drawable.location_gps_requested)
            } else {
                request_location?.setImageResource(R.drawable.location_gps_request)
            }

            if (lastMoveToCurrentLocationing) {
                isCurrentLocation = true
            } else if (!lastMoveToLocationing) {
                isCurrentLocation = false
            }
        }
    }

    private val mOnGeocodeSearchListener = object : GeocodeSearch.OnGeocodeSearchListener {

        override fun onRegeocodeSearched(regeocodeResult: RegeocodeResult, i: Int) {
            if (i == 0) {
                val regeocodeAddress = regeocodeResult.regeocodeAddress
                if (regeocodeAddress != null) {
                    val centerLocation = POIBean()
                    centerLocation.name = regeocodeAddress.formatAddress
                    centerLocation.lat = (regeocodeResult.regeocodeQuery.point.latitude * 1000000.0f).toLong()
                    centerLocation.lng = (regeocodeResult.regeocodeQuery.point.longitude * 1000000.0f).toLong()
                    mChoiceLocation = centerLocation
                    mChoicePosition = 0

                    centerLocation.let {
                        mDataImpl.setHeadData(it)
                        mDataImpl.getFirstDataList(it.lat,it.lng,"")
                    }
                }
            } else {
                if (mRequestAddressRetryMaxCount == 0) {
                    val centerLocation = POIBean()
                    centerLocation.name = getString(R.string.pet_text_632)
                    centerLocation.lat = (regeocodeResult.regeocodeQuery.point.latitude * 1000000.0f).toLong()
                    centerLocation.lng = (regeocodeResult.regeocodeQuery.point.longitude * 1000000.0f).toLong()
                    mChoiceLocation = centerLocation
                    mChoicePosition = 0

                    centerLocation.let {
                        mDataImpl.setHeadData(it)
                        mDataImpl.getFirstDataList(it.lat,it.lng,"")
                    }
                } else {
                    mRequestAddressRetryMaxCount--
                    requestAddress()
                }
            }
        }

        override fun onGeocodeSearched(geocodeResult: GeocodeResult, i: Int) {}
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(activity).inflate(R.layout.msg_location_choice_gmap_fragment, null, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initMapView(savedInstanceState)
        initData()
    }

    private fun initView() {
        common_recycler?.initSingleTypeRecycleView(LinearLayoutManager(this@ChoiceLocationGMapFragment.context), mAdapter, true)
        common_recycler.refreshController().setEnablePullToRefresh(false)
        common_recycler.emptyController().setEmpty()
        common_recycler.loadMoreController().setOnLoadMoreListener {
            mChoiceLocation?.let {
                mDataImpl.getDataList(it.lat, it.lng, "")
            }
        }

        mAdapter.setOnItemChildClickListener { _, _, position ->
            mChoiceLocation = mAdapter.getItem(position)
            mChoicePosition = position
            mDataImpl.setCheckList(position)
            moveToLocationing = true
            if (position == 0 && isCurrentLocation) {
                moveToCurrentLocationing = true
            }

            val mCameraPosition = CameraPosition.Builder().target(LatLng(((mChoiceLocation?.lat
                    ?: 0L).toDouble() / 1000000.0f), ((mChoiceLocation?.lng
                    ?: 0L).toDouble() / 1000000.0f))).zoom(16f).build()
            mGMap?.animateCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition), 300, null)
            common_recycler.recyclerViewController().adapter.notifyDataSetChanged()
        }

        request_location?.setOnClickListener {
            val clientLatLng = ClientLocationStore.getLastClientLatLng()
            if (clientLatLng != null) {
                moveToCurrentLocationing = true
                moveToLocationing = false
                mChoiceLocation = null
                val mCameraPosition = CameraPosition.Builder().target(LatLng(clientLatLng.latitude, clientLatLng.longitude)).zoom(16f).build()
                mGMap?.animateCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition), 300, null)
            } else {
                BaseApp.app.toast(getString(R.string.not_location_info))
            }
        }
        mQMUIStatusView.showLoadingView()
    }

    /**
     * 初始化地图
     */
    private fun initMapView(savedInstanceState: Bundle?) {
        mapview
        mapview?.onCreate(savedInstanceState)
        mapview?.getMapAsync { googleMap ->

            val uiSettings = googleMap.uiSettings
            uiSettings.isMyLocationButtonEnabled = false//设置默认定位按钮是否显示
            uiSettings.isZoomControlsEnabled = false//设置缩放按钮是否显示
            uiSettings.isCompassEnabled = false//设置指南针是否显示
            uiSettings.setAllGesturesEnabled(true)//设置是否允许所有手势操作
            uiSettings.isMapToolbarEnabled = false//设置是否显示工具栏
            uiSettings.isScrollGesturesEnabled = true//设置是否允许拖拽手势
            uiSettings.isTiltGesturesEnabled = true//设置是否允许手势改变视角
            uiSettings.isZoomGesturesEnabled = true//设置是否允许缩放手势
            mGMap = googleMap
            mGMap?.setOnCameraChangeListener(mOnCameraChangeListener)
        }
    }

    private fun initData() {
        mLocationImpl.onActivityCreate()
        mLocationImpl.getClientLocation(true, false, true)
    }

    override fun onGetClientLocation(clientLatLng: ClientLatLng?, isCache: Boolean) {
        ThreadUtils.runOnUIThread {
            refreshLocationMarker(clientLatLng)
        }
    }

    override fun onGetClientLocationError(e: ClientException) {
        ThreadUtils.runOnUIThread {
            if (e is LocationAccessException) {
                BaseApp.app.toast(getString(R.string.please_open_location_permissions))
            } else {
                BaseApp.app.toast(getString(R.string.pet_text_324))
            }
            mQMUIStatusView.showEmptyView(R.drawable.common_icon_empty_data)
        }
    }

    override fun onCheckGpsDisEnable() {
        ThreadUtils.runOnUIThread {
            this@ChoiceLocationGMapFragment.context?.let {
                AppDialog.show(it) {
                    positiveButton(text = getString(R.string.pet_text_195), click = {
                        val GPSIntent = IntentUtils.openGPS()
                        if (IntentUtils.isIntentAvailable(context, GPSIntent)) {
                            startActivity(GPSIntent)
                        } else {
                            BaseApp.app.toast(getString(R.string.pet_text_1254))
                        }
                    })
                    negativeButton(text = getString(R.string.pet_text_802))
                    title(text = getString(R.string.pet_text_995))

                }
            }
        }
    }

    override fun onGPSChangeEnable() {
        mLocationImpl.getClientLocation(false, false, true)
    }

    /**
     * 刷新位置标记
     */
    private fun refreshLocationMarker(clientLatLng: ClientLatLng?) {
        if (mGMap == null) {
            return
        }

        if (mLocationMarker == null) {
            val locationMarkerOptions = MarkerOptions()
            locationMarkerOptions.position(LatLng(clientLatLng?.latitude
                    ?: 0.0, clientLatLng?.longitude ?: 0.0))
            locationMarkerOptions.anchor(0.5f, 0.5f)
            locationMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_marker))
            mLocationMarker = mGMap?.addMarker(locationMarkerOptions)
        } else {
            mLocationMarker?.setPosition(LatLng(clientLatLng?.latitude
                    ?: 0.0, clientLatLng?.longitude ?: 0.0))
        }

        moveToCurrentLocationing = true
        moveToLocationing = false
        val mCameraPosition = CameraPosition.Builder().target(LatLng(clientLatLng?.latitude
                ?: 0.0, clientLatLng?.longitude ?: 0.0)).zoom(16f).build()
        mGMap?.animateCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition), 300, object : GoogleMap.CancelableCallback {
            override fun onFinish() {
                mGMap?.animateCamera(CameraUpdateFactory.zoomTo(16f))
            }

            override fun onCancel() {

            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SEARCH_LOCATION && resultCode == Activity.RESULT_OK && data != null) {
            //返回搜索的位置
            val poiBean = data.getSerializableExtra("data") as POIBean?
            if (poiBean != null) {
                mChoiceLocation = poiBean
                mChoicePosition = 0
                poiBean.let {
                    mDataImpl.setHeadData(it)
                    mDataImpl.getFirstDataList(it.lat,it.lng,"")
                }

                //移动到对应位置
                moveToCurrentLocationing = false
                moveToLocationing = false
                val mCameraPosition = CameraPosition.Builder().target(LatLng((poiBean.lat / 1000000.0f).toDouble(), (poiBean.lng / 1000000.0f).toDouble())).zoom(16f).build()
                mGMap?.animateCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition), 300, null)
            }
        }
    }

    /**
     * 请求位置
     */
    private fun requestAddress() {
        if (mGMap == null) {
            return
        }

        ThreadUtils.runOnUIThread {
            val cameraPosition = mGMap?.cameraPosition
            mTarget = cameraPosition?.target
            ThreadUtils.runOnIOThread {
                if (mTarget != null && (mChoiceLocation == null || LocationUtils.calculateDistance(
                                ((mChoiceLocation?.lat ?: 0L).toDouble() / 1000000.0),
                                ((mChoiceLocation?.lng ?: 0L).toDouble() / 1000000.0),
                                mTarget?.latitude ?: 0.0,
                                mTarget?.longitude ?: 0.0) > 200)) {
                    val latLonPoint = LatLonPoint(mTarget?.latitude ?: 0.0, mTarget?.longitude
                            ?: 0.0)
                    val geocoderSearch = GeocodeSearch(context)
                    geocoderSearch.setOnGeocodeSearchListener(mOnGeocodeSearchListener)
                    val query = RegeocodeQuery(latLonPoint, 1f, GeocodeSearch.AMAP)
                    try {
                        //先用高德逆编码，如果没用就用谷歌的逆编码
                        //经纬度都会转成高德的上传给后台查询附近地标
                        val regeocodeAddress = geocoderSearch.getFromLocation(query)
                        if (regeocodeAddress != null && !TextUtils.isEmpty(regeocodeAddress.formatAddress)) {
                            requestAddressed(regeocodeAddress.formatAddress, (query.point.latitude * 1000000.0f).toLong(), (query.point.longitude * 1000000.0f).toLong())
                        } else {
                            var googleName = ""
                            val geoCoder = Geocoder(this@ChoiceLocationGMapFragment.context, Locale.getDefault())
                            val address = geoCoder.getFromLocation(mTarget?.latitude
                                    ?: 0.0, mTarget?.longitude ?: 0.0, 1)
                            if (address.size >= 1) {
                                googleName = address[0].countryName + " " + address[0].adminArea
//                                Log.i("lzh","address  ${address[0].countryName }  ${address[0].adminArea}  ${address[0].thoroughfare}  ${address[0].subAdminArea }  ${address[0].subLocality}   ${address[0].subThoroughfare}")
                            }
                            if (!TextUtils.isEmpty(googleName)) {
                                requestAddressed(googleName, (query.point.latitude * 1000000.0f).toLong(), (query.point.longitude * 1000000.0f).toLong())
                            } else {
                                requestAddressed(getString(R.string.pet_text_632), (query.point.latitude * 1000000.0f).toLong(), (query.point.longitude * 1000000.0f).toLong())
                            }
                        }
                    } catch (e: Exception) {
                        if (mRequestAddressRetryMaxCount == 0) {
                            requestAddressed(getString(R.string.pet_text_632), (query.point.latitude * 1000000.0f).toLong(), (query.point.longitude * 1000000.0f).toLong())
                        } else {
                            mRequestAddressRetryMaxCount--
                            requestAddress()
                        }
                    }
                }
            }
        }
    }

    override fun onGetClientLocation(clientLocation: ClientLocation?, isCache: Boolean) {
    }

    private fun requestAddressed(address: String, lat: Long, lng: Long) {
        ThreadUtils.runOnUIThread {
            val centerLocation = POIBean()
            centerLocation.name = address
            centerLocation.lat = lat
            centerLocation.lng = lng
            mChoiceLocation = centerLocation
            mChoicePosition = 0

            centerLocation.let {
                mDataImpl.setHeadData(it)
                mDataImpl.getFirstDataList(it.lat,it.lng,"")
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapview?.onSaveInstanceState(outState)
    }


    override fun onResume() {
        super.onResume()
        mapview?.onResume()
        mLocationImpl.onActivityResume()
    }

    override fun onPause() {
        super.onPause()
        mapview?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mLocationImpl.onActivityRelease()
        mapview?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapview?.onLowMemory()
    }

    override fun showLoading() {
    }

    override fun showData(list: MutableList<POIBean>, hasMore: Boolean) {
        mAdapter.setNewData(list)
        if (!hasMore) {
            common_recycler.loadMoreController().loadMoreEnd()
        } else {
            common_recycler.loadMoreController().loadMoreComplete()
        }


    }

    override fun showErrMsg(str: String?) {
        BaseApp.app.toast(str.toString())
        mQMUIStatusView.showErrorView(str?:"")
    }

    override fun showEmpty() {

    }


    companion object {

        private val MAX_RETRY_COUNT_REQUEST_ADDRESS = 3

        fun newInstance(): ChoiceLocationGMapFragment {
            return ChoiceLocationGMapFragment()
        }
    }

    fun getChoiceLocation(): POIBean? {
        return mChoiceLocation
    }

    fun requestAddressedAgain(){
        mLocationImpl.getClientLocation(false, false, true)
    }

}
