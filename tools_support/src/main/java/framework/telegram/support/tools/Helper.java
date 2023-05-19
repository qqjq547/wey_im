package framework.telegram.support.tools;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {
    private static Typeface sTypefaceCache;
    private static Map<String, Integer> sColorCacheMap = new HashMap();

    private Helper() {
    }

    public static String getMac(String mac) {
        if (!TextUtils.isEmpty(mac) && mac.length() == 12) {
            String[] macs = new String[6];

            int i;
            for (i = 0; i <= 5; ++i) {
                macs[i] = mac.substring(i * 2, i * 2 + 2);
            }

            mac = macs[0];

            for (i = 1; i < macs.length; ++i) {
                mac = mac + ":" + macs[i];
            }

            return mac.toUpperCase();
        } else {
            return mac;
        }
    }

    public static String getTrimMac(String mac) {
        return TextUtils.isEmpty(mac) ? mac : mac.replaceAll(":", "").toUpperCase();
    }

    public static int parseColor(String color, int defaultColor) {
        if (sColorCacheMap.containsKey(color)) {
            return (Integer) sColorCacheMap.get(color);
        } else {
            int value = defaultColor;

            try {
                if (!TextUtils.isEmpty(color)) {
                    if (color.charAt(0) == '#') {
                        value = Color.parseColor(color);
                        sColorCacheMap.put(color, value);
                    } else {
                        value = Color.parseColor("#" + color);
                        sColorCacheMap.put(color, value);
                    }
                }
            } catch (IllegalArgumentException var4) {
                var4.printStackTrace();
            }

            return value;
        }
    }

    public static boolean isTelephonyEnabled(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        return tm != null && 5 == tm.getSimState();
    }

    public static CharSequence fromHtml(String content) {
        return (CharSequence) (TextUtils.isEmpty(content) ? "" : Html.fromHtml(content));
    }

    public static Typeface getCustomTypeface(Context context) {
        if (sTypefaceCache != null) {
            return sTypefaceCache;
        } else {
            try {
                sTypefaceCache = Typeface.createFromAsset(context.getAssets(), "font/meng.ttf");
                if (sTypefaceCache == null) {
                    sTypefaceCache = Typeface.DEFAULT;
                }
            } catch (Throwable var2) {
                sTypefaceCache = Typeface.DEFAULT;
            }

            return sTypefaceCache;
        }
    }

    public static void setPrimaryClip(Context context, String text) {
        ClipboardManager cmb = (ClipboardManager) context.getSystemService("clipboard");
        cmb.setPrimaryClip(ClipData.newPlainText((CharSequence) null, text));
    }

    public static String getFileExtension(String fileName) {
        return !TextUtils.isEmpty(fileName) && fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0 ? fileName.substring(fileName.lastIndexOf(".") + 1) : "";
    }

    public static String getFileNameNoExtension(String fileName) {
        return !TextUtils.isEmpty(fileName) ? fileName.substring(0, Math.max(0, fileName.lastIndexOf("."))) : "";
    }

    public static String replaceBlank(String str) {
        String dest = "";
        if (!TextUtils.isEmpty(str)) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }

        return dest;
    }

    public static byte[] int2Bytes(int res) {
        byte[] result = new byte[]{(byte) (res >>> 24), (byte) (res >>> 16), (byte) (res >>> 8), (byte) res};
        return result;
    }

    public static int bytes2Int(byte[] res) {
        byte[] a = new byte[4];
        int i = a.length - 1;

        for (int j = res.length - 1; i >= 0; --j) {
            if (j >= 0) {
                a[i] = res[j];
            } else {
                a[i] = 0;
            }

            --i;
        }

        int v0 = (a[0] & 255) << 24;
        int v1 = (a[1] & 255) << 16;
        int v2 = (a[2] & 255) << 8;
        int v3 = a[3] & 255;
        return v0 + v1 + v2 + v3;
    }

    public static byte[] short2Bytes(short s) {
        byte[] desc = new byte[]{(byte) (s >> 0), (byte) (s >> 8)};
        return desc;
    }

    public static short bytes2Short(byte[] b) {
        return (short) (b[1] << 8 | b[0] & 255);
    }

    public static void logCaller(int index) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        if (stackTraceElements != null) {
            if (index <= 0) {
                index = stackTraceElements.length;
            }

            index = Math.min(index, stackTraceElements.length);

            for (int i = 0; i < index; ++i) {
                Log.d("LogCaller", stackTraceElements[i].getClassName() + "." + stackTraceElements[i].getMethodName());
            }
        }

    }

    public static void logCaller() {
        logCaller(4);
    }

    public static boolean emojiCompat() {
        return true;
    }

    public static Bitmap takeScreenShot(Activity activity) {
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap b1 = view.getDrawingCache();
        Rect frame = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        int statusBarHeight = frame.top;
        int width = activity.getWindowManager().getDefaultDisplay().getWidth();
        int height = activity.getWindowManager().getDefaultDisplay().getHeight();
        Bitmap b;
        if (b1.getHeight() == height) {
            b = Bitmap.createBitmap(b1, 0, statusBarHeight, width, height - statusBarHeight);
        } else {
            b = Bitmap.createBitmap(b1, 0, 0, width, height - statusBarHeight);
        }

        view.destroyDrawingCache();
        return b;
    }

    public static String trimSpaceBetweenWrap(String input) {
        StringBuffer sb = new StringBuffer();
        Pattern p = Pattern.compile("\n(\\s+)\n");
        Matcher m = p.matcher(input);

        while (m.find()) {
            m.appendReplacement(sb, m.group().replace(m.group(1), ""));
        }

        m.appendTail(sb);
        return sb.toString();
    }

    public static int[] getRatioArray(double[] array) {
        if (array == null) {
            return null;
        } else {
            int[] ratioArray = new int[array.length];
            double max = 0.0D;
            int maxIndex = 0;

            for (int i = 0; i < array.length; ++i) {
                if (array[i] > max) {
                    max = array[i];
                    maxIndex = i;
                }
            }

            double sum = 0.0D;

            int i;
            for (i = 0; i < array.length; ++i) {
                double temp = array[i] + 0.5D;
                array[i] = array[i] == 0.0D ? 0.0D : (array[i] <= 1.0D ? 1.0D : (double) ((int) temp));
                if (i != maxIndex) {
                    sum += array[i];
                }
            }

            array[maxIndex] = 100.0D - sum;

            for (i = 0; i < array.length; ++i) {
                ratioArray[i] = (int) array[i];
            }

            return ratioArray;
        }
    }
}
