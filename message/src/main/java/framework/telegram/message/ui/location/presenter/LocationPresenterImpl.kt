package framework.telegram.message.ui.location.presenter

import android.content.Context
import android.util.Log
import com.trello.rxlifecycle3.android.FragmentEvent
import com.umeng.analytics.MobclickAgent
import framework.telegram.message.ui.location.ClientLocationManager
import framework.telegram.message.ui.location.bean.ClientLatLng
import framework.telegram.message.ui.location.bean.ClientLocation
import framework.telegram.message.ui.location.bean.ClientLocationStore
import framework.telegram.support.BaseApp
import framework.telegram.support.tools.exception.ClientException
import framework.telegram.support.tools.DeviceUtils
import io.reactivex.Observable

class LocationPresenterImpl : LocationContract.Presenter {
    private var sLastRequestLocationTime: Long = 0

    private var mLastGpsStatus: Boolean = false
    private var mClientLocationManager: ClientLocationManager? = null

    private val mContext: Context?
    private val mView: LocationContract.View
    private val mViewObservalbe: Observable<FragmentEvent>

    constructor(view: LocationContract.View, context: Context?, observable: Observable<FragmentEvent>) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mContext = context
    }


    /**
     * 获取当前位置信息
     *
     * @param checkOpenGps 是否检测GPS开关
     * @param hasAddress   是否包含地址位置信息
     * @param immediately  是否使用短时间内的缓存地理位置(false会获取最新的地理位置)
     */
    override fun getClientLocation(checkOpenGps: Boolean, hasAddress: Boolean, immediately: Boolean) {
        getClientLocation(checkOpenGps, hasAddress, immediately, true)
    }

    /**
     * 获取当前位置信息
     *
     * @param checkOpenGps 是否检测GPS开关
     * @param hasAddress   是否包含地址位置信息
     * @param immediately  是否使用短时间内的缓存地理位置(false会获取最新的地理位置)
     * @param quick        是否快速获取(false获取时间比较长)
     */
    override fun getClientLocation(checkOpenGps: Boolean, hasAddress: Boolean, immediately: Boolean, quick: Boolean) {
        if (checkOpenGps ) {
            if (!DeviceUtils.isOpenGPS(mContext)) {
                mView.onCheckGpsDisEnable()
            }
        }

        if (hasAddress) {
            val clientLocation = ClientLocationStore.getLastClientLocation()
            if (clientLocation != null ) {
                mView.onGetClientLocation(clientLocation, true)
            }

            if (immediately && clientLocation != null && System.currentTimeMillis() - sLastRequestLocationTime < 5 * 60 * 1000) {
                return
            } else {
                sLastRequestLocationTime = System.currentTimeMillis()
            }
        } else {
            val clientLatLng = ClientLocationStore.getLastClientLatLng()
            if (clientLatLng != null) {
                mView.onGetClientLocation(clientLatLng, true)
            }

            if (immediately && clientLatLng != null && System.currentTimeMillis() - sLastRequestLocationTime < 5 * 60 * 1000) {
                return
            } else {
                sLastRequestLocationTime = System.currentTimeMillis()
            }
        }

        if (mClientLocationManager == null) {
            mContext?.let {
                mClientLocationManager = ClientLocationManager(mContext, object : ClientLocationManager.ClientLocationListener {

                    override fun onSuccess(clientLocationManager: ClientLocationManager, clientLatLng: ClientLatLng?) {
                        mView.onGetClientLocation(clientLatLng, false)
                    }

                    override fun onSuccess(clientLocationManager: ClientLocationManager, clientLocation: ClientLocation?) {
                        mView.onGetClientLocation(clientLocation, false)
                    }

                    override fun onError(clientLocationManager: ClientLocationManager, e: ClientException) {
                        mView.onGetClientLocationError(e)
                        MobclickAgent.reportError(BaseApp.app, e)
                    }
                })
            }

        }

        mClientLocationManager?.requestLocationAndAddress(quick)
    }

    override fun onActivityResume() {
        val currentGpsStatus = DeviceUtils.isOpenGPS(mContext)
        if (mLastGpsStatus != currentGpsStatus && currentGpsStatus) {
            //GPS开关状态发生改变,且开关为打开,则通知ActivityGPS开关发生了变化
            mView.onGPSChangeEnable()
        }

        mLastGpsStatus = currentGpsStatus
    }

    override fun onActivityCreate() {
        mLastGpsStatus = DeviceUtils.isOpenGPS(mContext)
    }

    override fun onActivityRelease() {
        if (mClientLocationManager != null) {
            mClientLocationManager?.release()
        }
    }
}