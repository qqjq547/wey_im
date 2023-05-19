package framework.telegram.message.ui.location

import android.content.Context
import android.text.TextUtils

import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import framework.telegram.message.R
import framework.telegram.message.ui.location.bean.ClientLatLng
import framework.telegram.message.ui.location.bean.ClientLocation
import framework.telegram.message.ui.location.bean.ClientLocationStore
import framework.telegram.support.BaseApp
import framework.telegram.support.BuildConfig
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.tools.AndroidUtils
import framework.telegram.support.tools.exception.ClientException
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.exception.LocationAccessException
import framework.telegram.support.tools.exception.LocationException


/**
 * Created by hyf on 15/12/28.
 */
class ClientLocationManager(context: Context, private var mClientLocationListener: ClientLocationListener?) {

    private var mContext: Context? = null
    private var mLocationClient: AMapLocationClient? = null

    private val mLocationListener = AMapLocationListener { aMapLocation ->
        ThreadUtils.runOnIOThread {
            if (aMapLocation != null) {
                if (aMapLocation.errorCode == AMapLocation.LOCATION_SUCCESS) {
                    //定位成功回调信息，设置相关消息
                    val locationType = aMapLocation.locationType//获取当前定位结果来源，如网络定位结果，详见定位类型表
                    if (locationType == AMapLocation.LOCATION_TYPE_GPS) {
                        AppLogcat.logger.d(TAG, "GPS定位结果")
                    } else if (locationType == AMapLocation.LOCATION_TYPE_AMAP) {
                        AppLogcat.logger.d(TAG, "补偿定位结果")
                    } else if (locationType == AMapLocation.LOCATION_TYPE_CELL) {
                        AppLogcat.logger.d(TAG, "CELL定位结果")
                    } else if (locationType == AMapLocation.LOCATION_TYPE_OFFLINE) {
                        AppLogcat.logger.d(TAG, "离线定位结果")
                    } else if (locationType == AMapLocation.LOCATION_TYPE_FIX_CACHE) {
                        AppLogcat.logger.d(TAG, "缓存定位结果")
                    } else if (locationType == AMapLocation.LOCATION_TYPE_WIFI) {
                        AppLogcat.logger.d(TAG, "WIFI定位结果")
                    } else if (locationType == AMapLocation.LOCATION_TYPE_SAME_REQ) {
                        AppLogcat.logger.d(TAG, "与上次定位结果高度相似")
                    } else {
                        AppLogcat.logger.d(TAG, "未知来源的定位结果")
                    }

                    val latitude = aMapLocation.latitude
                    val longitude = aMapLocation.longitude
                    //                            CoordinateConverter converter = new CoordinateConverter(getApplicationContext());
                    //                            boolean isAMapDataAvailable = converter.isAMapDataAvailable(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                    //                            if (!isAMapDataAvailable) {
                    //                                try {
                    //                                    converter.from(CoordinateConverter.CoordType.GPS);
                    //                                    converter.coord(new DPoint(aMapLocation.getLatitude(), aMapLocation.getLongitude()));
                    //                                    DPoint desLatLng = converter.convert();
                    //                                    latitude = desLatLng.getLatitude();
                    //                                    longitude = desLatLng.getLongitude();
                    //                                } catch (Exception e) {
                    //                                    latitude = aMapLocation.getLatitude();
                    //                                    longitude = aMapLocation.getLongitude();
                    //                                }
                    //                            } else {
                    //                                latitude = aMapLocation.getLatitude();
                    //                                longitude = aMapLocation.getLongitude();
                    //                            }

                    AppLogcat.logger.d(TAG, "定位结果---> latitude=$latitude   longitude+$longitude")

                    //上传经纬度,目前不需要
//                    val oldClientLatLng = ClientLocationStore.getInstance().getLastClientLatLng()
//                    if (oldClientLatLng == null || Math.abs(System.currentTimeMillis() - oldClientLatLng?.time) >= 30 * 60 * 1000) {
//                        //没上传过或者超过半小时上传
//                        val account = AccountManager.getInstance().getLoginAccount()
//                        if (!account.isVistor()) {
//                            //上传经纬度
//                            UserController.getInstance().updateLocation(account, latitude, longitude, aMapLocation.address, null)
//                        }
//                    }

                    val clientLatLng = ClientLatLng(latitude, longitude, System.currentTimeMillis())
                    ClientLocationStore.saveLastClientLatLng(clientLatLng)

                    callOnSuccess(clientLatLng)

                    val countryName = aMapLocation.country
                    var provinceName = aMapLocation.province
                    var cityName: String = aMapLocation.city
                    var countyName: String? = aMapLocation.district

                    if (provinceName.endsWith(context.getString(R.string.province)) || provinceName.endsWith(context.getString(R.string.city))) {
                        provinceName = provinceName.substring(0, provinceName.length - 1)
                    }

                    if (cityName?.endsWith(context.getString(R.string.city))) {
                        cityName = cityName?.substring(0, cityName.length - 1)
                    }

                    if (TextUtils.isEmpty(provinceName) || TextUtils.isEmpty(cityName)) {
                        try {
                            val geocoderSearch = GeocodeSearch(BaseApp.app)
                            val query = RegeocodeQuery(LatLonPoint(latitude, longitude), 1000f, GeocodeSearch.AMAP)
                            val regeocodeAddress = geocoderSearch.getFromLocation(query)
                            provinceName = regeocodeAddress.province
                            cityName = regeocodeAddress.city
                            countyName = regeocodeAddress.district
                        } catch (e: Exception) {
                        }

                    }

                    if (!TextUtils.isEmpty(provinceName) && !TextUtils.isEmpty(cityName)) {
                        if (provinceName == cityName) {
                            cityName = countyName?:""
                            countyName = null
                        }

                        AppLogcat.logger.d(TAG, "定位结果---> countryName=$countryName   provinceName=$provinceName   cityName=$cityName   countyName=$countyName")
//                        val loginAccount = AccountManager.getInstance().getLoginAccount()
//                        val country = ProvinceController.getInstance().getCountryByNameSync(loginAccount, countryName)
//                        val province = ProvinceController.getInstance().getProvinceByNameSync(loginAccount, provinceName)
//                        val city = ProvinceController.getInstance().getCityByNameSync(loginAccount, cityName)
//                        if (province != null && city != null) {
                            // 只支持国内大陆的位置信息
                            val location = ClientLocation()

//                            if (country != null) {
//                                location.setCountryId(country?.getCountryID() as Int)
//                            }

//                            location.setProvinceId(province?.getProvinceID() as Int)
//                            location.setCityId(city?.getCityID() as Int)
                            location.setLatitude(latitude)
                            location.setLongitude(longitude)
                            location.setAddr(aMapLocation.address)
                            location.setTime(System.currentTimeMillis())
                            ClientLocationStore.saveLastClientLocation(location)

                            callOnSuccess(location)
                    } else {
//                        callOnError(NoSupportLocationException())
                    }
                } else if (aMapLocation.errorCode == AMapLocation.ERROR_CODE_FAILURE_LOCATION_PERMISSION) {
                    AppLogcat.logger.d(TAG, "定位权限有问题")
                    callOnError(LocationAccessException())
                } else if (aMapLocation.errorCode == AMapLocation.ERROR_CODE_FAILURE_NOWIFIANDAP) {
                    if (!AndroidUtils.hasM()) {
                        AppLogcat.logger.d(TAG, "定位权限有问题")
                        callOnError(LocationAccessException())
                    } else {
                        AppLogcat.logger.d(TAG, "定位错误: 1 " + aMapLocation.errorCode)
                        callOnError(LocationException())
                    }
                } else {
                    AppLogcat.logger.d(TAG, "定位错误: 2 " + aMapLocation.errorCode)
                    callOnError(LocationException())
                }
            } else {
                AppLogcat.logger.d(TAG, "定位错误")
                callOnError(LocationException())
            }
        }
    }

    interface ClientLocationListener {
        fun onSuccess(clientLocationManager: ClientLocationManager, clientLatLng: ClientLatLng?)

        fun onSuccess(clientLocationManager: ClientLocationManager, clientLocation: ClientLocation?)

        fun onError(clientLocationManager: ClientLocationManager, e: ClientException)
    }

    init {
        mContext = context.applicationContext
    }

    fun stopRequest() {
        if (mLocationClient != null) {
            mLocationClient?.unRegisterLocationListener(mLocationListener)
            mLocationClient?.stopLocation()
            mLocationClient?.onDestroy()
            mLocationClient = null
        }
    }

    fun release() {
        stopRequest()
        mContext = null
        mClientLocationListener = null
    }

    /**
     * 获取经纬度,包含地址信息
     */
    fun requestLocationAndAddress(quick: Boolean) {
        stopRequest()

        mLocationClient = AMapLocationClient(mContext)
        mLocationClient?.setLocationListener(mLocationListener)

        val option = AMapLocationClientOption()
        option.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy//设置定位模式
        //设置是否返回地址信息（默认返回地址信息）
        option.isNeedAddress = true
        //设置是否只定位一次,默认为false
        option.isOnceLocation = true
        //设置是否强制刷新WIFI，默认为强制刷新
        option.isWifiActiveScan = !quick
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        option.isMockEnable = false
        //设置是否优先返回GPS定位信息
        option.isGpsFirst = !quick
        //给定位客户端对象设置定位参数
        mLocationClient?.setLocationOption(option)

        mLocationClient?.startLocation()
    }

    /**
     * 获取经纬度,不包含地址信息
     */
    fun requestLocation(quick: Boolean) {
        stopRequest()

        mLocationClient = AMapLocationClient(mContext)
        mLocationClient?.setLocationListener(mLocationListener)

        val option = AMapLocationClientOption()
        option.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy//设置定位模式
        //设置是否返回地址信息（默认返回地址信息）
        option.isNeedAddress = false
        //设置是否只定位一次,默认为false
        option.isOnceLocation = true
        //设置是否强制刷新WIFI，默认为强制刷新
        option.isWifiActiveScan = !quick
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        option.isMockEnable = false
        //设置是否优先返回GPS定位信息
        option.isGpsFirst = !quick
        //给定位客户端对象设置定位参数
        mLocationClient?.setLocationOption(option)

        mLocationClient?.startLocation()
    }

    /**
     * 设置间隔不断获取定位
     *
     * @param needAddress  是否需要地址
     * @param intervalTime 间隔时间，毫秒
     */
    fun requestLocationIntervalTime(needAddress: Boolean, onlyGps: Boolean, intervalTime: Long) {
        stopRequest()

        mLocationClient = AMapLocationClient(mContext)
        mLocationClient?.setLocationListener(mLocationListener)

        val option = AMapLocationClientOption()
        option.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy//设置定位模式
        option.isLocationCacheEnable = !onlyGps
        //设置是否返回地址信息（默认返回地址信息）
        option.isNeedAddress = needAddress
        //设置是否只定位一次,默认为false
        option.isOnceLocation = false
        //设置是否强制刷新WIFI，默认为强制刷新
        option.isWifiActiveScan = true
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        option.isMockEnable = false
        //设置是否优先返回GPS定位信息
        option.isGpsFirst = true
        //设置定位间隔,单位毫秒,默认为2000ms
        option.interval = intervalTime

        //给定位客户端对象设置定位参数
        mLocationClient?.setLocationOption(option)
        mLocationClient?.startLocation()
    }

    private fun callOnSuccess(clientLatLng: ClientLatLng?) {
        if (clientLatLng == null) {
            return
        }

        ThreadUtils.runOnIOThread {
            if (mClientLocationListener != null) {
                mClientLocationListener?.onSuccess(this@ClientLocationManager, clientLatLng)
            }
        }
    }

    private fun callOnSuccess(clientLocation: ClientLocation?) {
        if (clientLocation == null) {
            return
        }

        ThreadUtils.runOnIOThread {
            if (mClientLocationListener != null) {
                mClientLocationListener?.onSuccess(this@ClientLocationManager, clientLocation)
            }
        }
    }

    private fun callOnError(e: ClientException) {
        ThreadUtils.runOnIOThread {
            if (mClientLocationListener != null) {
                mClientLocationListener?.onError(this@ClientLocationManager, e)
            }
        }
    }

    companion object {

        private val TAG = "ClientLocationManager"
    }
}
