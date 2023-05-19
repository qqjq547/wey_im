package framework.telegram.support.tools;

public class LocationUtils {
    private LocationUtils() {

    }

    /**
     * 计算个性化距离显示km和m
     *
     * @param distance 距离
     * @return String 显示内容
     */
    public static String covertSelfDistance(int distance) {
        if (distance < 0) {
            return "";
        }
        if (distance < 1) {
            return "1m";
        } else if (distance < 1000) {
            return distance + "m";
        } else {
            float dis = Math.round(distance / 100f) / 10f;
            return dis + "km";
        }
    }

    /*
     * 地球半径（单位：米）
     */
    public static final double EARTH_RADIUS = 6378137;

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    /**
     * 计算两个经纬度之间的距离
     *
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     * @return
     */
    public static int calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        if (Math.abs(lat1) > 90 || Math.abs(lng1) > 180 || Math.abs(lat2) > 90 || Math.abs(lng2) > 180) {
            return 0;
        }

        try {
            double radLat1 = rad(lat1);
            double radLat2 = rad(lat2);
            double a = radLat1 - radLat2;
            double b = rad(lng1) - rad(lng2);

            double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
            s = s * EARTH_RADIUS;
            s = Math.round(s * 10000) / 10000;
            return (int) s;
        } catch (Exception e) {
            return 0;
        }
    }
}
