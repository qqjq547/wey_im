package framework.telegram.support.tools;


import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;
import android.os.Build.VERSION;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;

import framework.telegram.support.tools.exception.ClientException;

public class DeviceUtils {
    public DeviceUtils() {
    }

    public static final boolean isOpenGPS(Context context) {
        if (context == null) {
            return false;
        } else {
            try {
                LocationManager locationManager = (LocationManager)context.getSystemService("location");
                return locationManager.isProviderEnabled("gps");
            } catch (Exception var2) {
                return false;
            }
        }
    }

    public static boolean useTextureView() {
        return VERSION.SDK_INT >= 16 && !isCyanogenMod();
    }

    private static boolean isCyanogenMod() {
        return System.getProperty("os.version").contains("cyanogenmod") || Build.HOST.contains("cyanogenmod");
    }

    public static long getBootTime() {
        return SystemClock.elapsedRealtime() > 0L ? System.currentTimeMillis() - SystemClock.elapsedRealtime() : 0L;
    }

    public static boolean isEmulator(Context context) throws Exception {
        try {
            if (!Build.PRODUCT.equals("sdk") && !Build.PRODUCT.equals("google_sdk") && !Build.PRODUCT.equals("sdk_x86") && !Build.PRODUCT.equals("vbox86p")) {
                if (Build.MANUFACTURER.equals("Genymotion")) {
                    throw new RuntimeException(new Exception("isEmulator!!! Build.MANUFACTURER is:" + Build.MANUFACTURER));
                } else if (!Build.BRAND.equals("generic") && !Build.BRAND.equals("generic_x86")) {
                    if (!Build.DEVICE.equals("generic") && !Build.DEVICE.equals("generic_x86") && !Build.DEVICE.equals("vbox86p")) {
                        if (!Build.MODEL.equals("sdk") && !Build.MODEL.equals("google_sdk") && !Build.MODEL.contains("Emulator") && !Build.MODEL.equals("Android SDK built for x86")) {
                            if (!Build.HARDWARE.equals("goldfish") && !Build.HARDWARE.equals("vbox86")) {
                                if (!Build.FINGERPRINT.contains("generic/sdk/generic") && !Build.FINGERPRINT.contains("generic_x86/sdk_x86/generic_x86") && !Build.FINGERPRINT.contains("generic/google_sdk/generic") && !Build.FINGERPRINT.contains("generic/vbox86p/vbox86p") && !Build.FINGERPRINT.contains("Genymotion")) {
                                    TelephonyManager localTelephonyManager = (TelephonyManager)context.getSystemService("phone");
                                    if (localTelephonyManager.getSimOperatorName().equals("Android")) {
                                        throw new RuntimeException(new Exception("isEmulator!!! getSimOperatorName is:" + localTelephonyManager.getSimOperatorName()));
                                    } else if (localTelephonyManager.getNetworkOperatorName().equals("Android")) {
                                        throw new RuntimeException(new Exception("isEmulator!!! getNetworkOperatorName is:" + localTelephonyManager.getNetworkOperatorName()));
                                    } else {
                                        return false;
                                    }
                                } else {
                                    throw new RuntimeException(new ClientException("isEmulator!!! Build.FINGERPRINT is:" + Build.FINGERPRINT));
                                }
                            } else {
                                throw new RuntimeException(new Exception("isEmulator!!! Build.HARDWARE is:" + Build.HARDWARE));
                            }
                        } else {
                            throw new RuntimeException(new Exception("isEmulator!!! Build.MODEL is:" + Build.MODEL));
                        }
                    } else {
                        throw new RuntimeException(new Exception("isEmulator!!! Build.DEVICE is:" + Build.DEVICE));
                    }
                } else {
                    throw new RuntimeException(new Exception("isEmulator!!! Build.BRAND is:" + Build.BRAND));
                }
            } else {
                throw new RuntimeException(new Exception("isEmulator!!! Build.PRODUCT is:" + Build.PRODUCT));
            }
        } catch (Throwable var2) {
            return false;
        }
    }

    public static String getDeviceName() {
        String manufacturer = getManufacturer();
        String model = getDeviceModel();
        if (model != null && model.startsWith(manufacturer)) {
            return model;
        } else {
            return manufacturer != null ? manufacturer + " " + model : "Unknown";
        }
    }

    public static String getDeviceModel() {
        return StringUtils.trim(Build.MODEL);
    }

    public static String getLanguage(Context context) {
        if (context == null) {
            return "";
        } else {
            Configuration conf = context.getResources().getConfiguration();
            String locale = conf.locale.getDisplayName(conf.locale);
            return locale != null && locale.length() > 1 ? Character.toUpperCase(locale.charAt(0)) + locale.substring(1) : "";
        }
    }

    public static String getManufacturer() {
        return StringUtils.trim(Build.MANUFACTURER);
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & 15) >= 3;
    }

    public static String getCpuInfo() {
        String cpuInfo = "";

        try {
            if ((new File("/proc/cpuinfo")).exists()) {
                FileReader fr = new FileReader("/proc/cpuinfo");
                BufferedReader localBufferedReader = new BufferedReader(fr, 8192);
                cpuInfo = localBufferedReader.readLine();
                localBufferedReader.close();
                if (cpuInfo != null) {
                    cpuInfo = cpuInfo.split(":")[1].trim().split(" ")[0];
                }
            }
        } catch (IOException var3) {
        } catch (Exception var4) {
        }

        return cpuInfo;
    }

    public static boolean isSupportCameraLedFlash(PackageManager pm) {
        if (pm != null) {
            FeatureInfo[] features = pm.getSystemAvailableFeatures();
            if (features != null) {
                FeatureInfo[] var2 = features;
                int var3 = features.length;

                for(int var4 = 0; var4 < var3; ++var4) {
                    FeatureInfo f = var2[var4];
                    if (f != null && "android.hardware.camera.flash".equals(f.name)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean isSupportCameraHardware(Context context) {
        return context != null && context.getPackageManager().hasSystemFeature("android.hardware.camera");
    }

    public static String getDeviceId(Context context) {
        return (new DeviceInfo(context)).getAndroidID();
    }

    public static String getMac(Context context) {
        return (new DeviceInfo(context)).getWifiMAC();
    }

    public static String getIMSI(Context context) {
        return (new DeviceInfo(context)).getIMSI();
    }

    public static boolean isMeizuBrand() {
        return Build.BRAND.toLowerCase().contains("meizu");
    }

    public static boolean isOnePlusBrand() {
        return Build.BRAND.toLowerCase().contains("oneplus");
    }

    public static boolean isXiaomiBrand() {
        return getManufacturer().toLowerCase().contains("xiaomi") || Build.BRAND.toLowerCase().contains("xiaomi") || Build.MODEL.toLowerCase().equals("2013022");
    }

    public static boolean isHuaweiBrand() {
        return Build.BRAND.toLowerCase().contains("huawei");
    }

    public static boolean isCoolpadBrand() {
        return Build.BRAND.toLowerCase().contains("yulong") || getManufacturer().toLowerCase().contains("coolpad");
    }

    public static boolean isOppoBrand() {
        return Build.BRAND.toLowerCase().contains("oppo") || getManufacturer().toLowerCase().contains("alps");
    }

    public static boolean isSamsungBrand() {
        return Build.BRAND.toLowerCase().contains("samsung");
    }

    public static boolean isLenovoBrand() {
        return Build.BRAND.toLowerCase().contains("lenovo");
    }

    public static boolean isSmartisanBrand(Context context) {
        if (Build.BRAND.toLowerCase().contains("smartisan")) {
            return true;
        } else {
            return TextUtils.equals(SystemPropertiesProxy.get(context, "ro.rommanager.developerid"), "smartisan");
        }
    }

    public static boolean isVivoBrand() {
        return Build.BRAND.toLowerCase().contains("vivo");
    }

    public static boolean isAliyunBrand(Context context) {
        return !TextUtils.isEmpty(SystemPropertiesProxy.get(context, "ro.yunos.build.version")) || !TextUtils.isEmpty(SystemPropertiesProxy.get(context, "ro.yunos.version"));
    }

    public static boolean isMeituBrand(Context context) {
        return !TextUtils.isEmpty(SystemPropertiesProxy.get(context, "ro.build.version.meios"));
    }

    public static boolean isIUniBrand(Context context) {
        return !TextUtils.isEmpty(SystemPropertiesProxy.get(context, "ro.gn.iuniznvernumber"));
    }

    public static boolean isZteBrand() {
        return getDeviceModel().toLowerCase().contains("zte");
    }

    public static boolean isHTCBrand() {
        return getManufacturer().toLowerCase().contains("htc") || getManufacturer().toLowerCase().contains("hum");
    }

    public static boolean isLGBrand() {
        return getManufacturer().toLowerCase().contains("lg");
    }

    public static boolean isMotoBrand() {
        return getManufacturer().toLowerCase().contains("moto") || getManufacturer().toLowerCase().contains("motorola") || getManufacturer().toLowerCase().contains("mot") || getManufacturer().toLowerCase().contains("FIH");
    }

    public static boolean isFlymeSafeInstalled(Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo("com.meizu.safe", 1);
            ActivityInfo[] var2 = pi.activities;
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                ActivityInfo activityInfo = var2[var4];
                if (TextUtils.equals("com.meizu.safe.security.SecSettingsActivity", activityInfo.name) && activityInfo.exported) {
                    return true;
                }
            }
        } catch (Exception var6) {
        }

        return false;
    }

    public static boolean isHuaWeiEMUILollipop() {
        return VERSION.SDK_INT >= 21 && Build.BRAND.equalsIgnoreCase("HUAWEI");
    }

    public static boolean isIUNILollipop() {
        return VERSION.SDK_INT >= 21 && Build.BRAND.equalsIgnoreCase("IUNI");
    }

    public static boolean isLenovoS820e() {
        return getDeviceModel().contains("S820e");
    }

    public static boolean isZteU9180() {
        return getDeviceModel().toLowerCase().contains("U9180");
    }

    public static boolean isMeiZuNote2() {
        String deviceModel = getDeviceModel().toLowerCase();
        return deviceModel.contains("m2 note");
    }

    public static boolean isMiuiV4(Context context) {
        String version = SystemPropertiesProxy.get(context, "ro.miui.ui.version.name");
        return !TextUtils.isEmpty(version) && version.toLowerCase().equals("v4");
    }

    public static boolean isMiuiV5(Context context) {
        String version = SystemPropertiesProxy.get(context, "ro.miui.ui.version.name");
        return !TextUtils.isEmpty(version) && version.toLowerCase().equals("v5");
    }

    public static boolean isMiuiV6(Context context) {
        String version = SystemPropertiesProxy.get(context, "ro.miui.ui.version.name");
        return !TextUtils.isEmpty(version) && version.toLowerCase().equals("v6");
    }

    public static boolean isMiuiV7(Context context) {
        String version = SystemPropertiesProxy.get(context, "ro.miui.ui.version.name");
        return !TextUtils.isEmpty(version) && version.toLowerCase().equals("v7");
    }

    public static boolean isMiuiV8(Context context) {
        String version = SystemPropertiesProxy.get(context, "ro.miui.ui.version.name");
        return !TextUtils.isEmpty(version) && version.toLowerCase().equals("v8");
    }

    public static boolean hasMiuiV8(Context context) {
        String version = SystemPropertiesProxy.get(context, "ro.miui.ui.version.name");

        try {
            if (!TextUtils.isEmpty(version) && Integer.valueOf(version.toLowerCase().replaceAll("v", "")) >= 8) {
                return true;
            }
        } catch (Exception var3) {
        }

        return false;
    }

    public static boolean isMiui(Context context) {
        String version = SystemPropertiesProxy.get(context, "ro.miui.ui.version.name");

        try {
            if (!TextUtils.isEmpty(version) && version.toLowerCase().startsWith("v")) {
                return true;
            }
        } catch (Exception var3) {
        }

        return false;
    }

    public static boolean isEMUI(Context context) {
        String version = SystemPropertiesProxy.get(context, "ro.build.version.emui");

        try {
            if (!TextUtils.isEmpty(version) && version.toLowerCase().startsWith("emotionui")) {
                return true;
            }
        } catch (Exception var3) {
        }

        return false;
    }

    public static boolean isFlyme(Context context) {
        try {
            Method method = Build.class.getMethod("hasSmartBar");
            return method != null;
        } catch (Exception var3) {
            String meizuFlymeOSFlag = SystemPropertiesProxy.get(context, "ro.build.display.id", "");
            return !TextUtils.isEmpty(meizuFlymeOSFlag) && meizuFlymeOSFlag.toLowerCase().contains("flyme");
        }
    }

    public static boolean isDevice(String... devices) {
        String model = getDeviceModel().toLowerCase();
        if (devices != null) {
            String[] var2 = devices;
            int var3 = devices.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                String device = var2[var4];
                if (model.contains(device.toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isNeedNotice(Context context) {
        return isHuaweiBrand() || isLenovoBrand() || isVivoBrand() || isXiaomiBrand() || isMeizuBrand() || isOppoBrand() || isCoolpadBrand() || isSamsungBrand() || isSmartisanBrand(context);
    }
}
