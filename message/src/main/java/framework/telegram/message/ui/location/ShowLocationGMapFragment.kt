package framework.telegram.message.ui.location

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
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
import kotlinx.android.synthetic.main.msg_location_show_gmap_fragment.*


/**
 * 聊天发送的地图展示页（谷歌地图）
 * Created by hu on 15/8/28.
 */
class ShowLocationGMapFragment : BaseFragment() , LocationContract.View  {
    override val fragmentName: String
        get() = "ShowLocationGMapFragment"

    /**
     * View
     */
    private var mGMap: GoogleMap? = null
    private var mLocationMarker: Marker? = null

    /**
     * Other
     */
    private var moveToLocationing: Boolean = false
    private var mLocationMarkerBitmap: Bitmap? = null

    private val mLocationBean by lazy {arguments?.getSerializable("location") as LocationBean }
    private val mLocationImpl: LocationPresenterImpl by lazy { LocationPresenterImpl(this, this@ShowLocationGMapFragment.context, lifecycle())}


    private val mOnCameraChangeListener = object : GoogleMap.OnCameraChangeListener {

        override fun onCameraChange(cameraPosition: CameraPosition) {
            if (moveToLocationing) {
                moveToLocationing = false
                request_location?.setImageResource(R.drawable.location_gps_requested)
            } else {
                request_location?.setImageResource(R.drawable.location_gps_request)
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(activity).inflate(R.layout.msg_location_show_gmap_fragment, null, false)
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
                val mCameraPosition = CameraPosition.Builder().target(LatLng(clientLatLng.latitude, clientLatLng.longitude)).zoom(14f).build()
                mGMap?.animateCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition), 300, null)
            }
        }
    }


    /**
     * 初始化地图
     */
    private fun initMapView(savedInstanceState: Bundle?) {
        mapview.onCreate(savedInstanceState)
        mapview.getMapAsync(object : OnMapReadyCallback {
            override fun onMapReady(googleMap: GoogleMap) {
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

                val locationMarkerView = LocationMarkerView(getContext())
                locationMarkerView.setAddress(mLocationBean?.address)
                mLocationMarkerBitmap = locationMarkerView.getBitmap()

                val markerOptions = MarkerOptions()
                markerOptions.position(LatLng(mLocationBean?.lat?:0.0, mLocationBean?.lng?:0.0))
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(mLocationMarkerBitmap))
                mGMap?.addMarker(markerOptions)

                val mCameraPosition = CameraPosition.Builder().target(LatLng(mLocationBean?.lat?:0.0, mLocationBean?.lng?:0.0)).zoom(14f).build()
                mGMap?.animateCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition), 300, object : GoogleMap.CancelableCallback {
                    override fun onFinish() {
                        mGMap?.animateCamera(CameraUpdateFactory.zoomTo(14f))
                    }

                    override  fun onCancel() {

                    }
                })
            }
        })
    }

    private fun initData (){
        mLocationImpl.getClientLocation(false, false, true)
    }

    private fun refreshLocationMarker(clientLatLng: ClientLatLng?) {
        if (mGMap == null) {
            return
        }

        if (mLocationMarker == null) {
            val locationMarkerOptions = MarkerOptions()
            locationMarkerOptions.position(LatLng(clientLatLng?.latitude?:0.0, clientLatLng?.longitude?:0.0))
            locationMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_marker))
            mLocationMarker = mGMap?.addMarker(locationMarkerOptions)
        } else {
            mLocationMarker?.setPosition(LatLng(clientLatLng?.latitude?:0.0, clientLatLng?.longitude?:0.0))
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
            this@ShowLocationGMapFragment.context?.let {
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

        fun newInstance(): ShowLocationGMapFragment {
            return ShowLocationGMapFragment()
        }
    }
}
