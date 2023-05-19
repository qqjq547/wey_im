package framework.telegram.message.ui.location.bean

import android.text.TextUtils
import com.google.gson.Gson
import framework.telegram.support.BaseApp
import framework.telegram.support.system.storage.sp.core.Default
import framework.telegram.support.system.storage.sp.core.IdeasPreference
import framework.telegram.support.system.storage.sp.core.Key
import framework.telegram.support.system.storage.sp.core.SpName
import com.airbnb.lottie.LottieCompositionFactory.fromJson



/**
 * Created by lzh on 19-5-30.
 * INFO:
 */
object ClientLocationStore {

    private val mLocationPreferences by lazy { IdeasPreference().create(BaseApp.app, LocationPreferences::class.java) }

    private var mLastClientLocation: ClientLocation? = null
    private var mLastClientLatLng: ClientLatLng? = null

    fun getLastClientLocation(): ClientLocation? {
        if (mLastClientLocation != null) {
            return mLastClientLocation
        }
        val result = mLocationPreferences.getLocationPreferences()
        return if (!TextUtils.isEmpty(result)) {
            gsonToClientLocation( result)
        } else null

    }

    fun saveLastClientLocation(location: ClientLocation) {
        mLastClientLocation = location

        val clientLatLng = gsonToString(location)
        mLocationPreferences.putLocationPreferences(clientLatLng)
    }

    fun getLastClientLatLng(): ClientLatLng? {
        if (mLastClientLatLng != null) {
            return mLastClientLatLng
        }
        val result = mLocationPreferences.getLatLngPreferences()
        return if (!TextUtils.isEmpty(result)) {
            gsonToClientLatLng( result)
        } else null

    }

    fun saveLastClientLatLng(latlng: ClientLatLng) {
        mLastClientLatLng = latlng

        val clientLatLng = gsonToString(latlng)
        mLocationPreferences.putLatLngPreferences(clientLatLng)
    }

    private fun gsonToString(value: ClientLocation): String {
        return Gson().toJson(value)
    }

    private fun gsonToString(value: ClientLatLng): String {
        return Gson().toJson(value)
    }

    private fun gsonToClientLocation(value: String): ClientLocation {
        return Gson().fromJson(value, ClientLocation::class.java)
    }

    private fun gsonToClientLatLng(value: String): ClientLatLng {
        return Gson().fromJson(value, ClientLatLng::class.java)
    }


}
