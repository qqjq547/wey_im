package framework.telegram.support.tools;


import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Looper;
import android.os.Process;
import android.os.Build.VERSION;
import android.util.DisplayMetrics;
import java.lang.Character.UnicodeBlock;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class AndroidUtils {
    public AndroidUtils() {
    }

    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static String getCurProcessName(Context context) {
        try {
            int pid = Process.myPid();
            ActivityManager mActivityManager = (ActivityManager)context.getSystemService("activity");
            List<RunningAppProcessInfo> runningAppProcessInfos = mActivityManager.getRunningAppProcesses();
            if (runningAppProcessInfos != null) {
                Iterator var4 = runningAppProcessInfos.iterator();

                while(var4.hasNext()) {
                    RunningAppProcessInfo appProcess = (RunningAppProcessInfo)var4.next();
                    if (appProcess.pid == pid) {
                        return appProcess.processName;
                    }
                }
            }
        } catch (Exception var6) {
        }

        return null;
    }

    public static PackageInfo getPackageInfo(Context context) {
        PackageInfo info = null;

        try {
            info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (NameNotFoundException var3) {
        }

        return info;
    }

    public static ApplicationInfo getApplicationInfo(Context context) {
        ApplicationInfo info = null;

        try {
            info = context.getPackageManager().getApplicationInfo(context.getPackageName(), 128);
        } catch (NameNotFoundException var3) {
        }

        return info;
    }

    public static void changeLanguage(Context context, int i) {
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        DisplayMetrics dm = resources.getDisplayMetrics();
        switch(i) {
            case 0:
                config.locale = Locale.ENGLISH;
                break;
            case 1:
                config.locale = Locale.SIMPLIFIED_CHINESE;
                break;
            case 2:
                config.locale = Locale.getDefault();
        }

        resources.updateConfiguration(config, dm);
    }

    public static Locale getLanguage(Context context) {
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        return config.locale;
    }

    public static int getApiLevel() {
        return VERSION.SDK_INT;
    }

    public static boolean hasFroyo() {
        return VERSION.SDK_INT >= 8;
    }

    public static boolean hasGingerbread() {
        return VERSION.SDK_INT >= 9;
    }

    public static boolean hasHoneycomb() {
        return VERSION.SDK_INT >= 11;
    }

    public static boolean hasHoneycombMR1() {
        return VERSION.SDK_INT >= 12;
    }

    public static boolean hasICS() {
        return VERSION.SDK_INT >= 14;
    }

    public static boolean hasJellyBean() {
        return VERSION.SDK_INT >= 16;
    }

    public static boolean hasJellyBeanMr1() {
        return VERSION.SDK_INT >= 17;
    }

    public static boolean hasJellyBeanMr2() {
        return VERSION.SDK_INT >= 18;
    }

    public static boolean hasKitkat() {
        return VERSION.SDK_INT >= 19;
    }

    public static boolean hasLollipop() {
        return VERSION.SDK_INT >= 21;
    }

    public static boolean hasM() {
        return VERSION.SDK_INT >= 23;
    }

    public static int getSDKVersionInt() {
        return VERSION.SDK_INT;
    }

    public static boolean isCompatible() {
        return VERSION.SDK_INT >= 14;
    }

    public static String getPlatformVersion() {
        return VERSION.RELEASE;
    }

    public static final boolean isChinese(char c) {
        UnicodeBlock ub = UnicodeBlock.of(c);
        return ub == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || ub == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == UnicodeBlock.GENERAL_PUNCTUATION || ub == UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }
}
