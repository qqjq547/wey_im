package framework.telegram.message.ui.location.utils;

import android.content.Context;

import com.amap.api.location.CoordinateConverter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;


/**
 * by zst
 * Created on 2016/3/31.
 */
public class MapUtils {

    private MapUtils() {
    }

    /**
     * 是否需要加载谷歌地图
     *
     * @param context
     * @param lat     纬度
     * @param lng     经度
     * @return
     */
    public static boolean isShouldLoadGoogleMap(Context context, double lat, double lng){
        if(lat != 0 && lng != 0 && !isAMapDataAvailable(context, lat, lng)
                && GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS){
            return true;
        }
        return false;
    }

    /**
     * 判断位置所在区域 true-所处大陆、港澳地区
     *
     * @param context
     * @param lat
     * @param lng
     * @return
     */
    private static boolean isAMapDataAvailable(Context context, double lat, double lng) {
        CoordinateConverter converter = new CoordinateConverter(context);
        return converter.isAMapDataAvailable(lat, lng);
    }

    /**
     * 判断是否使用谷歌附近地标（通过判断是否在中国境内，如果高德地图能用，就是再中国境内，否则不是）
     *
     * @param context
     * @param lat     纬度
     * @param lng     经度
     * @return
     */
    public static boolean isGoogleMapData(Context context, double lat, double lng){
        if(lat != 0 && lng != 0 && !isAMapDataAvailable(context, lat, lng) ){
            return true;
        }
        return false;
    }
}
