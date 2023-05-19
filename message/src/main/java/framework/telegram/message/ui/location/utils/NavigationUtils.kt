package framework.telegram.message.ui.location.utils

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import framework.telegram.message.R
import framework.telegram.message.ui.location.bean.ClientLocation
import framework.telegram.message.ui.location.bean.LocationBean
import framework.telegram.support.tools.APKUtils
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.dialog.AppDialog
import java.util.*


/**
 * Created by hyf on 15/11/24.
 */
object NavigationUtils {

    fun navigation(activity: AppCompatActivity, clientLocation: ClientLocation?, locationBean: LocationBean?) {
        if (clientLocation == null || locationBean == null) {
            activity.toast(activity.getString(R.string.pet_text_1086))
        }

        val maps = ArrayList<String>()
        if (APKUtils.isInstallAMap(activity)) {
            maps.add(activity.getString(R.string.pet_text_1130))
        }

        if (APKUtils.isInstallBaiduMap(activity)) {
            maps.add(activity.getString(R.string.pet_text_884))
        }

        if (APKUtils.isInstallTencentMap(activity)) {
            maps.add(activity.getString(R.string.pet_text_642))
        }

        if (MapUtils.isShouldLoadGoogleMap(activity, clientLocation!!.floatLat, clientLocation.floatLng) && APKUtils.isInstallGoogleMap(activity)) {
            maps.add(activity.getString(R.string.google_navigation))
        }

        if (!maps.isEmpty()) {
            AppDialog.showList(activity, maps) { _, index, text ->
                if (activity.getString(R.string.pet_text_1130).equals(text)) {
                    try {
                        val intent = Intent()
                        intent.addCategory("android.intent.category.DEFAULT")
                        intent.data = UriUtils.parseUri("androidamap://route?sourceApplication=YourPet&sname=" + clientLocation.addr
                                + "&slat=" + clientLocation.floatLat
                                + "&slon=" + clientLocation.floatLng
                                + "&dlat=" + locationBean?.getLat()
                                + "&dlon=" + locationBean?.getLng()
                                + "&dname=" + locationBean?.address
                                + "&dev=0&m=0&t=2")
                        intent.setPackage("com.autonavi.minimap")
                        activity.startActivity(intent)
                    } catch (e: Exception) {
                        activity.toast(activity.getString(R.string.pet_text_1086))
                    }
                } else if (activity.getString(R.string.pet_text_884).equals(text)) {
                    try {
                        val intent = Intent.getIntent("intent://map/direction?origin=latlng:"
                                + clientLocation.floatLat + "," + clientLocation.floatLng + "|name:" + clientLocation.addr
                                + "&destination=latlng:" + locationBean?.getLat() + "," + locationBean?.getLng()
                                + "|name:" + locationBean?.address + "&mode=driving&src=YourPet&coord_type=gcj02#Intent;scheme=bdapp;package=com.baidu.BaiduMap;end")
                        activity.startActivity(intent)
                    } catch (e: Exception) {
                        activity.toast(activity.getString(R.string.pet_text_1086))
                    }
                } else if (activity.getString(R.string.pet_text_642).equals(text)) {
                    try {
                        val intent = Intent()
                        intent.addCategory("android.intent.category.DEFAULT")
                        intent.data = UriUtils.parseUri("qqmap://map/routeplan?type=drive&from=" + clientLocation.addr
                                + "&fromcoord=" + clientLocation.floatLat + "," + clientLocation.floatLng
                                + "&to=" + locationBean?.address + "&tocoord=" + locationBean?.getLat() + "," + locationBean?.getLng())
                        intent.setPackage("com.tencent.map")
                        activity.startActivity(intent)
                    } catch (e: Exception) {
                        activity.toast(activity.getString(R.string.pet_text_1086))
                    }
                } else if (activity.getString(R.string.google_navigation).equals(text)) {
                    try {
                        val gmmIntentUri = UriUtils.parseUri("google.navigation:q=" + locationBean?.getLat() + "," + locationBean?.getLng())
                        val intent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        intent.addCategory("android.intent.category.DEFAULT")
                        intent.setPackage("com.google.android.apps.maps")
                        activity.startActivity(intent)
                    } catch (e: Exception) {
                        activity.toast(activity.getString(R.string.pet_text_1086))
                    }
                }
            }

        } else {
            activity.toast(activity.getString(R.string.pet_text_414))
        }
    }
}
