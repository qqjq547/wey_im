package framework.telegram.ui.utils;

public class TimeUtils {
    private TimeUtils() {
    }

    public static String timeFormatToMediaDuration(long time) {
        String returnValue;
        time = time / 1000;
        if (time > 0) {
            long h = time / 3600;
            long mAnds = time % 3600;
            long m = mAnds / 60;
            long s = mAnds % 60;
            String hour = h < 10 ? "0" + h : String.valueOf(h);
            String min = m < 10 ? "0" + m : String.valueOf(m);
            String ss = s < 10 ? "0" + s : String.valueOf(s);
            returnValue = hour + ":" + min + ":" + ss;
        } else {
            returnValue = "00:00:00";
        }

        if (returnValue.startsWith("00:00:")) {
            returnValue = returnValue.substring(3);
        }

        return returnValue;
    }
}
