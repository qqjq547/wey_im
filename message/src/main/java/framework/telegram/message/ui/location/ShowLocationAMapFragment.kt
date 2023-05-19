package framework.telegram.message.ui.location

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import framework.telegram.message.R
import framework.telegram.message.ui.location.bean.ClientLatLng
import framework.telegram.message.ui.location.bean.ClientLocation
import framework.telegram.message.ui.location.bean.ClientLocationStore
import framework.telegram.message.ui.location.bean.LocationBean
import framework.telegram.message.ui.location.presenter.LocationContract
import framework.telegram.message.ui.location.presenter.LocationPresenterImpl
import framework.telegram.support.BaseFragment
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.IntentUtils
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.exception.ClientException
import framework.telegram.support.tools.exception.LocationAccessException
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.widget.LocationMarkerView
import kotlinx.android.synthetic.main.msg_location_show_amap_fragment.*


/**
 * 聊天发送的地图展示页（高德地图）
 * Created by hu on 15/8/28.
 */
class ShowLocationAMapFragment  : BaseFragment(), LocationContract.View  {


    /**
     * View
     */
    private var mAMap: AMap? = null
    private var mLocationMarker: Marker? = null

    /**
     * Other
     */
    private var moveToLocationing: Boolean = false
    private var mLocationMarkerBitmap: Bitmap? = null

    private val mLocationBean by lazy {arguments?.getSerializable("location") as LocationBean }
    private val mLocationImpl: LocationPresenterImpl by lazy { LocationPresenterImpl(this, this@ShowLocationAMapFragment.context, lifecycle())}



    private val mOnCameraChangeListener = object : AMap.OnCameraChangeListener {

        override fun onCameraChange(cameraPosition: CameraPosition) {}

        override fun onCameraChangeFinish(cameraPosition: CameraPosition?) {
            if (cameraPosition == null) {
                return
            }

            if (moveToLocationing) {
                moveToLocationing = false
                request_location?.setImageResource(R.drawable.location_gps_requested)
            } else {
                request_location?.setImageResource(R.drawable.location_gps_request)
            }
        }
    }

    override val fragmentName: String
        get() = "ShowLocationAMapFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(activity).inflate(R.layout.msg_location_show_amap_fragment, null, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mLocationBean == null) {
            activity?.finish()
            return
        }

        initView()
        initMapView(savedInstanceState)
        initData()
    }

    private fun initView() {
        request_location.setOnClickListener {
            val clientLatLng = ClientLocationStore.getLastClientLatLng()
            if (clientLatLng != null) {
                moveToLocationing = true
                mAMap?.animateCamera(CameraUpdateFactory.changeLatLng(LatLng(clientLatLng.latitude, clientLatLng.longitude)), 300, null)
            }
        }
    }

    /**
     * 初始化地图
     */
    private fun initMapView(savedInstanceState: Bundle?) {
        mapview.onCreate(savedInstanceState)
        mAMap = mapview?.map
        if (mAMap != null) {
            mAMap?.mapTextZIndex = 2
            mAMap?.isMyLocationEnabled = false// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
            mAMap?.uiSettings?.isMyLocationButtonEnabled = false//设置默认定位按钮是否显示
            mAMap?.uiSettings?.isZoomControlsEnabled = false//设置缩放按钮是否显示
            mAMap?.uiSettings?.isCompassEnabled = false//设置指南针是否显示
            mAMap?.setMyLocationType(AMap.LOCATION_TYPE_LOCATE)// 设置定位的类型为根据地图面向方向旋转
            mAMap?.mapType = AMap.MAP_TYPE_NORMAL// 矢量地图模式

            mAMap?.setOnCameraChangeListener(mOnCameraChangeListener)
        }
    }

    private fun initData (){
        val locationMarkerView = LocationMarkerView(context)
        locationMarkerView.setAddress(mLocationBean.address)
        mLocationMarkerBitmap = locationMarkerView.bitmap

        val markerOptions = MarkerOptions()
        markerOptions.position(LatLng(mLocationBean?.lat, mLocationBean?.lng))
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(mLocationMarkerBitmap))
        mAMap?.addMarker(markerOptions)

        mAMap?.animateCamera(CameraUpdateFactory.changeLatLng(LatLng(mLocationBean?.lat, mLocationBean?.lng)), 300, object : AMap.CancelableCallback {
            override fun onFinish() {
                mAMap?.animateCamera(CameraUpdateFactory.zoomTo(14f))
            }

            override fun onCancel() {

            }
        })

        mLocationImpl.getClientLocation(false, false, true)
    }

    private fun refreshLocationMarker(clientLatLng: ClientLatLng?) {
        if (mLocationMarker == null) {
            val locationMarkerOptions = MarkerOptions()
            val latitude = (clientLatLng?.latitude)?:0.0
            val longitude = clientLatLng?.longitude?:0.0
            locationMarkerOptions.position(LatLng(latitude, longitude))
            locationMarkerOptions.anchor(0.5f, 0.5f)
            locationMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_marker))
            mLocationMarker = mAMap?.addMarker(locationMarkerOptions)
        } else {
            mLocationMarker?.position = LatLng(clientLatLng?.latitude?:0.0,clientLatLng?.longitude?:0.0)
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


    override fun onGetClientLocation(clientLatLng: ClientLatLng?, isCache: Boolean) {
        ThreadUtils.runOnUIThread {
            refreshLocationMarker(clientLatLng)
        }
    }

    override fun onGetClientLocationError(e: ClientException) {
        ThreadUtils.runOnUIThread {
            if (e is LocationAccessException) {
                toast(getString(R.string.please_open_location_permissions))
            } else {
                toast(getString(R.string.pet_text_324))
            }
        }
    }

    override fun onCheckGpsDisEnable() {
        ThreadUtils.runOnUIThread {
            this@ShowLocationAMapFragment.context?.let {
                AppDialog.show( it) {
                    positiveButton(text = getString(R.string.pet_text_195), click = {
                        val GPSIntent = IntentUtils.openGPS()
                        if (IntentUtils.isIntentAvailable(context, GPSIntent)) {
                            startActivity(GPSIntent)
                        } else {
                            toast(getString(R.string.pet_text_1254))
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

    override fun onGetClientLocation(clientLocation: ClientLocation?, isCache: Boolean) {
    }

    fun requestAddressedAgain(){
        mLocationImpl.getClientLocation(false, false, true)
    }

    companion object {

        fun newInstance(): ShowLocationAMapFragment {
            return ShowLocationAMapFragment()
        }
    }
}
