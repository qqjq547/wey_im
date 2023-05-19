package framework.telegram.support.tools;


import android.content.Context;
import dalvik.system.DexFile;
import java.io.File;
import java.lang.reflect.Method;

public class SystemPropertiesProxy {
    private SystemPropertiesProxy() {
    }

    public static String get(Context context, String key) {
        String ret = "";

        try {
            ClassLoader cl = context.getClassLoader();
            Class SystemProperties = cl.loadClass("android.os.SystemProperties");
            Class[] paramTypes = new Class[]{String.class};
            Method get = SystemProperties.getMethod("get", paramTypes);
            Object[] params = new Object[]{new String(key)};
            ret = (String)get.invoke(SystemProperties, params);
        } catch (Exception var8) {
            ret = "";
        }

        return ret;
    }

    public static String get(Context context, String key, String def) {
        String ret;
        try {
            ClassLoader cl = context.getClassLoader();
            Class SystemProperties = cl.loadClass("android.os.SystemProperties");
            Class[] paramTypes = new Class[]{String.class, String.class};
            Method get = SystemProperties.getMethod("get", paramTypes);
            Object[] params = new Object[]{new String(key), new String(def)};
            ret = (String)get.invoke(SystemProperties, params);
        } catch (Exception var9) {
            ret = def;
        }

        return ret;
    }

    public static Integer getInt(Context context, String key, int def) {
        Integer ret = def;

        try {
            ClassLoader cl = context.getClassLoader();
            Class SystemProperties = cl.loadClass("android.os.SystemProperties");
            Class[] paramTypes = new Class[]{String.class, Integer.TYPE};
            Method getInt = SystemProperties.getMethod("getInt", paramTypes);
            Object[] params = new Object[]{new String(key), new Integer(def)};
            ret = (Integer)getInt.invoke(SystemProperties, params);
        } catch (Exception var9) {
            ret = def;
        }

        return ret;
    }

    public static Long getLong(Context context, String key, long def) {
        Long ret = def;

        try {
            ClassLoader cl = context.getClassLoader();
            Class SystemProperties = cl.loadClass("android.os.SystemProperties");
            Class[] paramTypes = new Class[]{String.class, Long.TYPE};
            Method getLong = SystemProperties.getMethod("getLong", paramTypes);
            Object[] params = new Object[]{new String(key), new Long(def)};
            ret = (Long)getLong.invoke(SystemProperties, params);
        } catch (Exception var10) {
            ret = def;
        }

        return ret;
    }

    public static Boolean getBoolean(Context context, String key, boolean def) {
        Boolean ret = def;

        try {
            ClassLoader cl = context.getClassLoader();
            Class SystemProperties = cl.loadClass("android.os.SystemProperties");
            Class[] paramTypes = new Class[]{String.class, Boolean.TYPE};
            Method getBoolean = SystemProperties.getMethod("getBoolean", paramTypes);
            Object[] params = new Object[]{new String(key), new Boolean(def)};
            ret = (Boolean)getBoolean.invoke(SystemProperties, params);
        } catch (Exception var9) {
            ret = def;
        }

        return ret;
    }

    public static void set(Context context, String key, String val) {
        try {
            new DexFile(new File("/system/app/Settings.apk"));
            ClassLoader cl = context.getClassLoader();
            Class SystemProperties = Class.forName("android.os.SystemProperties");
            Class[] paramTypes = new Class[]{String.class, String.class};
            Method set = SystemProperties.getMethod("set", paramTypes);
            Object[] params = new Object[]{new String(key), new String(val)};
            set.invoke(SystemProperties, params);
        } catch (Exception var9) {
        }

    }
}
