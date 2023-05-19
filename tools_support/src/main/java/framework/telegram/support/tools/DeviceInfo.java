package framework.telegram.support.tools;


import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DeviceInfo {
    private final Context context;
    private final TelephonyManager tm;
    private final String initialVal;
    private static final String LOGTAG = "DeviceInfo";
    public static final String DEFAULT_VALUE = "na";

    public DeviceInfo(Context context) {
        this.context = context;
        this.tm = (TelephonyManager)context.getSystemService("phone");
        this.initialVal = "na";
    }

    public String getLibraryVersion() {
        String version = "1.1.7";
        int versionCode = 9;
        return version + "-" + versionCode;
    }

    public String getAndroidID() {
        String result = this.initialVal;

        try {
            result = Secure.getString(this.context.getContentResolver(), "android_id");
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getModel() {
        String result = this.initialVal;

        try {
            result = Build.MODEL;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return this.handleIllegalCharacterInResult(result);
    }

    public String getBuildBrand() {
        String result = this.initialVal;

        try {
            result = Build.BRAND;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return this.handleIllegalCharacterInResult(result);
    }

    public String getBuildHost() {
        String result = this.initialVal;

        try {
            result = Build.HOST;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getBuildTags() {
        String result = this.initialVal;

        try {
            result = Build.TAGS;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public long getBuildTime() {
        long result = 0L;

        try {
            result = Build.TIME;
        } catch (Throwable var4) {
            var4.printStackTrace();
        }

        return result;
    }

    public String getBuildUser() {
        String result = this.initialVal;

        try {
            result = Build.USER;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getBuildVersionRelease() {
        String result = this.initialVal;

        try {
            result = VERSION.RELEASE;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getScreenDisplayID() {
        String result = this.initialVal;

        try {
            WindowManager wm = (WindowManager)this.context.getSystemService("window");
            Display display = wm.getDefaultDisplay();
            result = String.valueOf(display.getDisplayId());
        } catch (Throwable var4) {
            var4.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getBuildVersionCodename() {
        String result = this.initialVal;

        try {
            result = VERSION.CODENAME;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getBuildVersionIncremental() {
        String result = this.initialVal;

        try {
            result = VERSION.INCREMENTAL;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public int getBuildVersionSDK() {
        int result = 0;

        try {
            result = VERSION.SDK_INT;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        return result;
    }

    public String getBuildID() {
        String result = this.initialVal;

        try {
            result = Build.ID;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String[] getSupportedABIS() {
        String[] result = new String[]{"-"};

        try {
            if (VERSION.SDK_INT >= 21) {
                result = Build.SUPPORTED_ABIS;
            }
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length == 0) {
            result = new String[]{"-"};
        }

        return result;
    }

    public String getStringSupportedABIS() {
        String result = this.initialVal;

        try {
            if (VERSION.SDK_INT >= 21) {
                String[] supportedABIS = Build.SUPPORTED_ABIS;
                StringBuilder supportedABIString = new StringBuilder();
                if (supportedABIS.length <= 0) {
                    supportedABIString.append(this.initialVal);
                } else {
                    String[] var4 = supportedABIS;
                    int var5 = supportedABIS.length;

                    for(int var6 = 0; var6 < var5; ++var6) {
                        String abis = var4[var6];
                        supportedABIString.append(abis).append("_");
                    }

                    supportedABIString.deleteCharAt(supportedABIString.lastIndexOf("_"));
                }

                result = supportedABIString.toString();
            }
        } catch (Throwable var8) {
            var8.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return this.handleIllegalCharacterInResult(result);
    }

    public String getStringSupported32bitABIS() {
        String result = this.initialVal;

        try {
            if (VERSION.SDK_INT >= 21) {
                String[] supportedABIS = Build.SUPPORTED_32_BIT_ABIS;
                StringBuilder supportedABIString = new StringBuilder();
                if (supportedABIS.length <= 0) {
                    supportedABIString.append(this.initialVal);
                } else {
                    String[] var4 = supportedABIS;
                    int var5 = supportedABIS.length;

                    for(int var6 = 0; var6 < var5; ++var6) {
                        String abis = var4[var6];
                        supportedABIString.append(abis).append("_");
                    }

                    supportedABIString.deleteCharAt(supportedABIString.lastIndexOf("_"));
                }

                result = supportedABIString.toString();
            }
        } catch (Throwable var8) {
            var8.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return this.handleIllegalCharacterInResult(result);
    }

    public String getStringSupported64bitABIS() {
        String result = this.initialVal;

        try {
            if (VERSION.SDK_INT >= 21) {
                String[] supportedABIS = Build.SUPPORTED_64_BIT_ABIS;
                StringBuilder supportedABIString = new StringBuilder();
                if (supportedABIS.length <= 0) {
                    supportedABIString.append(this.initialVal);
                } else {
                    String[] var4 = supportedABIS;
                    int var5 = supportedABIS.length;

                    for(int var6 = 0; var6 < var5; ++var6) {
                        String abis = var4[var6];
                        supportedABIString.append(abis).append("_");
                    }

                    supportedABIString.deleteCharAt(supportedABIString.lastIndexOf("_"));
                }

                result = supportedABIString.toString();
            }
        } catch (Throwable var8) {
            var8.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return this.handleIllegalCharacterInResult(result);
    }

    public String[] getSupported32bitABIS() {
        String[] result = new String[]{"-"};

        try {
            if (VERSION.SDK_INT >= 21) {
                result = Build.SUPPORTED_32_BIT_ABIS;
            }
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length == 0) {
            result = new String[]{"-"};
        }

        return result;
    }

    public String[] getSupported64bitABIS() {
        String[] result = new String[]{"-"};

        try {
            if (VERSION.SDK_INT >= 21) {
                result = Build.SUPPORTED_64_BIT_ABIS;
            }
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length == 0) {
            result = new String[]{"-"};
        }

        return result;
    }

    public String getManufacturer() {
        String result = this.initialVal;

        try {
            result = Build.MANUFACTURER;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return this.handleIllegalCharacterInResult(result);
    }

    public String getResolution() {
        String result = this.initialVal;

        try {
            WindowManager wm = (WindowManager)this.context.getSystemService("window");
            Display display = wm.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            result = metrics.heightPixels + "x" + metrics.widthPixels;
        } catch (Throwable var5) {
            var5.printStackTrace();
        }

        if (result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getCarrier() {
        String result = this.initialVal;

        try {
            if (this.tm != null && this.tm.getPhoneType() != 2) {
                result = this.tm.getNetworkOperatorName().toLowerCase(Locale.getDefault());
            }
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result.length() == 0) {
            result = this.initialVal;
        }

        return this.handleIllegalCharacterInResult(result);
    }

    public String getDevice() {
        String result = this.initialVal;

        try {
            result = Build.DEVICE;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getBootloader() {
        String result = this.initialVal;

        try {
            result = Build.BOOTLOADER;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getBoard() {
        String result = this.initialVal;

        try {
            result = Build.BOARD;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getDisplayVersion() {
        String result = this.initialVal;

        try {
            result = Build.DISPLAY;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getLanguage() {
        String result = this.initialVal;

        try {
            result = Locale.getDefault().getLanguage();
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getCountry() {
        String result = this.initialVal;

        try {
            if (this.tm.getSimState() == 5) {
                result = this.tm.getSimCountryIso().toLowerCase(Locale.getDefault());
            } else {
                Locale locale = Locale.getDefault();
                result = locale.getCountry().toLowerCase(locale);
            }
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result.length() == 0) {
            result = this.initialVal;
        }

        return this.handleIllegalCharacterInResult(result);
    }

    public String getNetworkType() {
        int networkStatePermission = this.context.checkCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE");
        String result = this.initialVal;
        if (networkStatePermission == 0) {
            try {
                ConnectivityManager cm = (ConnectivityManager)this.context.getSystemService("connectivity");
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork == null) {
                    result = "Unknown";
                } else if (activeNetwork.getType() != 1 && activeNetwork.getType() != 6) {
                    if (activeNetwork.getType() == 0) {
                        TelephonyManager manager = (TelephonyManager)this.context.getSystemService("phone");
                        if (manager.getSimState() == 5) {
                            switch(manager.getNetworkType()) {
                                case 0:
                                    result = "Cellular - Unknown";
                                    break;
                                case 1:
                                case 2:
                                case 4:
                                case 7:
                                case 11:
                                    result = "Cellular - 2G";
                                    break;
                                case 3:
                                case 5:
                                case 6:
                                case 8:
                                case 9:
                                case 10:
                                case 12:
                                case 15:
                                    result = "Cellular - 3G";
                                    break;
                                case 13:
                                    result = "Cellular - 4G";
                                    break;
                                case 14:
                                default:
                                    result = "Cellular - Unknown Generation";
                            }
                        }
                    }
                } else {
                    result = "Wifi/WifiMax";
                }
            } catch (Throwable var6) {
                var6.printStackTrace();
            }
        }

        if (result.length() == 0) {
            result = this.initialVal;
        }

        return this.handleIllegalCharacterInResult(result);
    }

    public String getOSCodename() {
        String codename = this.initialVal;
        switch(VERSION.SDK_INT) {
            case 1:
                codename = "First Android Version. Yay !";
                break;
            case 2:
                codename = "Base Android 1.1";
                break;
            case 3:
                codename = "Cupcake";
                break;
            case 4:
                codename = "Donut";
                break;
            case 5:
            case 6:
            case 7:
                codename = "Eclair";
                break;
            case 8:
                codename = "Froyo";
                break;
            case 9:
            case 10:
                codename = "Gingerbread";
                break;
            case 11:
            case 12:
            case 13:
                codename = "Honeycomb";
                break;
            case 14:
            case 15:
                codename = "Ice Cream Sandwich";
                break;
            case 16:
            case 17:
            case 18:
                codename = "Jelly Bean";
                break;
            case 19:
                codename = "Kitkat";
                break;
            case 20:
                codename = "Kitkat Watch";
                break;
            case 21:
            case 22:
                codename = "Lollipop";
                break;
            case 23:
                codename = "Marshmallow";
        }

        return codename;
    }

    public String getOSVersion() {
        String result = this.initialVal;

        try {
            result = VERSION.RELEASE;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getWifiMAC() {
        String result = this.initialVal;

        try {
            if (this.context.checkCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE") == 0) {
                WifiManager wm = (WifiManager)this.context.getSystemService("wifi");
                result = wm.getConnectionInfo().getMacAddress();
            }
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getIMEI() {
        boolean hasReadPhoneStatePermission = this.context.checkCallingOrSelfPermission("android.permission.READ_PHONE_STATE") == 0;
        if (!hasReadPhoneStatePermission) {
            return this.initialVal;
        } else {
            try {
                TelephonyManager tm = (TelephonyManager)this.context.getSystemService("phone");
                String imei = tm.getDeviceId();
                List<String> IMEIS = new ArrayList();
                if (this.checkimei(imei.trim())) {
                    IMEIS.add(imei.trim());
                }

                TelephonyManager telephonyManager2;
                String imeiphone2;
                try {
                    telephonyManager2 = (TelephonyManager)this.context.getSystemService("phone1");
                    imeiphone2 = telephonyManager2.getDeviceId();
                    if (imeiphone2 != null && this.checkimei(imeiphone2) && !IMEIS.contains(imeiphone2)) {
                        IMEIS.add(imeiphone2);
                    }
                } catch (Throwable var13) {
                }

                try {
                    telephonyManager2 = (TelephonyManager)this.context.getSystemService("phone2");
                    imeiphone2 = telephonyManager2.getDeviceId();
                    if (imeiphone2 != null && this.checkimei(imeiphone2) && !IMEIS.contains(imeiphone2)) {
                        IMEIS.add(imeiphone2);
                    }
                } catch (Throwable var12) {
                }

                List<String> imeis = this.IMEI_initQualcommDoubleSim();
                String item;
                Iterator var16;
                if (imeis != null && imeis.size() > 0) {
                    var16 = imeis.iterator();

                    while(var16.hasNext()) {
                        item = (String)var16.next();
                        if (!IMEIS.contains(item)) {
                            IMEIS.add(item);
                        }
                    }
                }

                imeis = this.IMEI_initMtkSecondDoubleSim();
                if (imeis != null && imeis.size() > 0) {
                    var16 = imeis.iterator();

                    while(var16.hasNext()) {
                        item = (String)var16.next();
                        if (!IMEIS.contains(item)) {
                            IMEIS.add(item);
                        }
                    }
                }

                imeis = this.IMEI_initMtkDoubleSim();
                if (imeis != null && imeis.size() > 0) {
                    var16 = imeis.iterator();

                    while(var16.hasNext()) {
                        item = (String)var16.next();
                        if (!IMEIS.contains(item)) {
                            IMEIS.add(item);
                        }
                    }
                }

                imeis = this.IMEI_initSpreadDoubleSim();
                if (imeis != null && imeis.size() > 0) {
                    var16 = imeis.iterator();

                    while(var16.hasNext()) {
                        item = (String)var16.next();
                        if (!IMEIS.contains(item)) {
                            IMEIS.add(item);
                        }
                    }
                }

                StringBuilder IMEI_SB = new StringBuilder();
                Integer TIMES_TEMP = 1;

                for(Iterator var8 = IMEIS.iterator(); var8.hasNext(); TIMES_TEMP = TIMES_TEMP + 1) {
                    String item1 = (String)var8.next();
                    if (TIMES_TEMP > 1) {
                        IMEI_SB.append('\n');
                    }

                    IMEI_SB.append(item1);
                }

                String imeis_tmp = IMEI_SB.toString().trim();
                if (TextUtils.isEmpty(imeis_tmp)) {
                    imeis_tmp = this.initialVal;
                }

                return imeis_tmp;
            } catch (Throwable var14) {
                return this.initialVal;
            }
        }
    }

    private Boolean checkimeisame(String imei) {
        char firstchar = '0';
        if (imei.length() > 0) {
            firstchar = imei.charAt(0);
        }

        Boolean issame = true;

        for(int i = 0; i < imei.length(); ++i) {
            char ch = imei.charAt(i);
            if (firstchar != ch) {
                issame = false;
                break;
            }
        }

        return issame;
    }

    private Boolean checkimei(String IMEI) {
        Integer LEN = IMEI.length();
        return LEN > 10 && LEN < 20 && !this.checkimeisame(IMEI.trim()) ? true : false;
    }

    private List<String> IMEI_initMtkDoubleSim() {
        try {
            TelephonyManager tm = (TelephonyManager)this.context.getSystemService("phone");
            Class c = Class.forName("com.android.internal.telephony.Phone");

            Integer simId_1;
            Integer simId_2;
            try {
                Field fields1 = c.getField("GEMINI_SIM_1");
                fields1.setAccessible(true);
                simId_1 = (Integer)fields1.get((Object)null);
                Field fields2 = c.getField("GEMINI_SIM_2");
                fields2.setAccessible(true);
                simId_2 = (Integer)fields2.get((Object)null);
            } catch (Throwable var9) {
                simId_1 = 0;
                simId_2 = 1;
            }

            Method m1 = TelephonyManager.class.getDeclaredMethod("getDeviceIdGemini", Integer.TYPE);
            String imei_1 = ((String)m1.invoke(tm, simId_1)).trim();
            String imei_2 = ((String)m1.invoke(tm, simId_2)).trim();
            List<String> imeis = new ArrayList();
            if (this.checkimei(imei_1)) {
                imeis.add(imei_1);
            }

            if (this.checkimei(imei_2)) {
                imeis.add(imei_2);
            }

            return imeis;
        } catch (Throwable var10) {
            return null;
        }
    }

    private List<String> IMEI_initMtkSecondDoubleSim() {
        try {
            TelephonyManager tm = (TelephonyManager)this.context.getSystemService("phone");
            Class c = Class.forName("com.android.internal.telephony.Phone");

            Integer simId_1;
            Integer simId_2;
            try {
                Field fields1 = c.getField("GEMINI_SIM_1");
                fields1.setAccessible(true);
                simId_1 = (Integer)fields1.get((Object)null);
                Field fields2 = c.getField("GEMINI_SIM_2");
                fields2.setAccessible(true);
                simId_2 = (Integer)fields2.get((Object)null);
            } catch (Throwable var11) {
                simId_1 = 0;
                simId_2 = 1;
            }

            Method mx = TelephonyManager.class.getMethod("getDefault", Integer.TYPE);
            TelephonyManager tm1 = (TelephonyManager)mx.invoke(tm, simId_1);
            TelephonyManager tm2 = (TelephonyManager)mx.invoke(tm, simId_2);
            String imei_1 = tm1.getDeviceId().trim();
            String imei_2 = tm2.getDeviceId().trim();
            List<String> imeis = new ArrayList();
            if (this.checkimei(imei_1)) {
                imeis.add(imei_1);
            }

            if (this.checkimei(imei_2)) {
                imeis.add(imei_2);
            }

            return imeis;
        } catch (Throwable var12) {
            return null;
        }
    }

    private List<String> IMEI_initSpreadDoubleSim() {
        try {
            Class<?> c = Class.forName("com.android.internal.telephony.PhoneFactory");
            Method m = c.getMethod("getServiceName", String.class, Integer.TYPE);
            String spreadTmService = (String)m.invoke(c, "phone", 1);
            TelephonyManager tm = (TelephonyManager)this.context.getSystemService("phone");
            String imei_1 = tm.getDeviceId().trim();
            TelephonyManager tm1 = (TelephonyManager)this.context.getSystemService(spreadTmService);
            String imei_2 = tm1.getDeviceId().trim();
            List<String> imeis = new ArrayList();
            if (this.checkimei(imei_1)) {
                imeis.add(imei_1);
            }

            if (this.checkimei(imei_2)) {
                imeis.add(imei_2);
            }

            return imeis;
        } catch (Throwable var9) {
            return null;
        }
    }

    public List<String> IMEI_initQualcommDoubleSim() {
        try {
            Class<?> cx = Class.forName("android.telephony.MSimTelephonyManager");
            Object obj = this.context.getSystemService("phone_msim");
            Integer simId_1 = 0;
            Integer simId_2 = 1;
            Method md = cx.getMethod("getDeviceId", Integer.TYPE);
            String imei_1 = ((String)md.invoke(obj, simId_1)).trim();
            String imei_2 = ((String)md.invoke(obj, simId_2)).trim();
            List<String> imeis = new ArrayList();
            if (this.checkimei(imei_1)) {
                imeis.add(imei_1);
            }

            if (this.checkimei(imei_2)) {
                imeis.add(imei_2);
            }

            return imeis;
        } catch (Throwable var9) {
            return null;
        }
    }

    public String getIMSI() {
        String result = this.initialVal;
        boolean hasReadPhoneStatePermission = this.context.checkCallingOrSelfPermission("android.permission.READ_PHONE_STATE") == 0;

        try {
            if (hasReadPhoneStatePermission) {
                result = this.tm.getSubscriberId();
            }
        } catch (Throwable var4) {
            var4.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getSerial() {
        String result = this.initialVal;

        try {
            result = Build.SERIAL;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getSIMSerial() {
        String result = this.initialVal;

        try {
            result = this.tm.getSimSerialNumber();
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getGSFID() {
        Uri URI = Uri.parse("content://com.google.android.gsf.gservices");
        String ID_KEY = "android_id";
        String[] params = new String[]{"android_id"};
        Cursor c = this.context.getContentResolver().query(URI, (String[])null, (String)null, params, (String)null);
        if (c == null) {
            return null;
        } else if (c.moveToFirst() && c.getColumnCount() >= 2) {
            try {
                String gsfID = Long.toHexString(Long.parseLong(c.getString(1)));
                c.close();
                return gsfID;
            } catch (NumberFormatException var6) {
                c.close();
                return this.initialVal;
            }
        } else {
            c.close();
            return null;
        }
    }

    public String getBluetoothMAC() {
        String result = this.initialVal;

        try {
            if (this.context.checkCallingOrSelfPermission("android.permission.BLUETOOTH") == 0) {
                BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
                result = bta.getAddress();
            }
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getPsuedoUniqueID() {
        String devIDShort = "35" + Build.BOARD.length() % 10 + Build.BRAND.length() % 10;
        if (VERSION.SDK_INT >= 21) {
            devIDShort = devIDShort + Build.SUPPORTED_ABIS[0].length() % 10;
        } else {
            devIDShort = devIDShort + Build.CPU_ABI.length() % 10;
        }

        devIDShort = devIDShort + (Build.DEVICE.length() % 10 + Build.MANUFACTURER.length() % 10 + Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10);

        String serial;
        try {
            serial = Build.class.getField("SERIAL").get((Object)null).toString();
            return (new UUID((long)devIDShort.hashCode(), (long)serial.hashCode())).toString();
        } catch (Throwable var4) {
            serial = "ESYDV000";
            return (new UUID((long)devIDShort.hashCode(), (long)serial.hashCode())).toString();
        }
    }

    public String getPhoneNo() {
        String result = this.initialVal;

        try {
            if (this.tm.getLine1Number() != null) {
                result = this.tm.getLine1Number();
                if (result.equals("")) {
                    result = this.initialVal;
                }
            }
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getProduct() {
        String result = this.initialVal;

        try {
            result = Build.PRODUCT;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getFingerprint() {
        String result = this.initialVal;

        try {
            result = Build.FINGERPRINT;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getHardware() {
        String result = this.initialVal;

        try {
            result = Build.HARDWARE;
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getRadioVer() {
        String result = this.initialVal;

        try {
            if (VERSION.SDK_INT >= 14) {
                result = Build.getRadioVersion();
            }
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getIPAddress(boolean useIPv4) {
        String result = this.initialVal;

        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            Iterator var4 = interfaces.iterator();

            while(var4.hasNext()) {
                NetworkInterface intf = (NetworkInterface)var4.next();
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                Iterator var7 = addrs.iterator();

                while(var7.hasNext()) {
                    InetAddress addr = (InetAddress)var7.next();
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = addr instanceof Inet4Address;
                        if (useIPv4) {
                            if (isIPv4) {
                                result = sAddr;
                            }
                        } else if (!isIPv4) {
                            int delim = sAddr.indexOf(37);
                            result = delim < 0 ? sAddr : sAddr.substring(0, delim);
                        }
                    }
                }
            }
        } catch (Throwable var12) {
            var12.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getUA() {
        String system_ua = System.getProperty("http.agent");
        String result = system_ua;

        try {
            if (VERSION.SDK_INT >= 17) {
                StringBuilder var10000 = new StringBuilder();
                (new WebView(this.context)).getSettings();
                result = var10000.append(WebSettings.getDefaultUserAgent(this.context)).append("__").append(system_ua).toString();
            } else {
                result = (new WebView(this.context)).getSettings().getUserAgentString() + "__" + system_ua;
            }
        } catch (Throwable var4) {
            var4.printStackTrace();
        }

        if (result.length() < 0 || result == null) {
            result = system_ua;
        }

        return result;
    }

    public int[] getDisplayXYCoordinates(MotionEvent event) {
        int[] coordinates = new int[]{0, 0};

        try {
            if (event.getAction() == 0) {
                coordinates[0] = (int)event.getX();
                coordinates[1] = (int)event.getY();
            }
        } catch (Throwable var4) {
            var4.printStackTrace();
        }

        return coordinates;
    }

    public String getAppName() {
        PackageManager pm = this.context.getPackageManager();

        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(this.context.getPackageName(), 0);
        } catch (NameNotFoundException var5) {
            ai = null;
            var5.printStackTrace();
        }

        String result = (String)((String)(ai != null ? pm.getApplicationLabel(ai) : this.initialVal));
        return result;
    }

    public String getAppVersion() {
        String result = this.initialVal;

        try {
            result = this.context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException var3) {
            var3.printStackTrace();
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getAppVersionCode() {
        String result = this.initialVal;

        try {
            result = String.valueOf(this.context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0).versionCode);
        } catch (NameNotFoundException var3) {
            var3.printStackTrace();
        }

        if (result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    public String getStore() {
        String result = this.initialVal;
        if (VERSION.SDK_INT >= 3) {
            try {
                result = this.context.getPackageManager().getInstallerPackageName(this.context.getPackageName());
            } catch (Throwable var3) {
                Log.i("DeviceInfo", "Can't get Installer package");
            }
        }

        if (result == null || result.length() == 0) {
            result = this.initialVal;
        }

        return result;
    }

    private String handleIllegalCharacterInResult(String result) {
        if (result.indexOf(" ") > 0) {
            result = result.replaceAll(" ", "_");
        }

        return result;
    }
}