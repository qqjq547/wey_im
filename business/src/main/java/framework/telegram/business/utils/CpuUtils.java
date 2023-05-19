package framework.telegram.business.utils;

import android.util.Log;

import java.lang.reflect.Method;

public class CpuUtils {

    public static boolean checkIfCPUx86() {
        //1. Check CPU architecture: arm or x86
        if (getSystemProperty("ro.product.cpu.abi", "arm").contains("x86")) {
            //The CPU is x86
            return false;
        } else {
            return false;
        }
    }


    private static String getSystemProperty(String key, String defaultValue) {
        String value = defaultValue;
        try {
            Class<?> clazz= Class.forName("android.os.SystemProperties");
            Method get = clazz.getMethod("get", String.class, String.class);
            value = (String)(get.invoke(clazz, key, ""));
        } catch (Exception e) {
            Log.d("getSystemProperty", "key = " + key + ", error = " + e.getMessage());
        }

        Log.d("getSystemProperty",  key + " = " + value);

        return value;
    }

}
