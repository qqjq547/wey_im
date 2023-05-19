package framework.telegram.support.tools;


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Environment;
import android.os.Build.VERSION;
import java.io.File;

public class EnvironmentUtils {
    public EnvironmentUtils() {
    }

    public static boolean isExternalStorageMountedReadWrite() {
        String externalStorageState = Environment.getExternalStorageState();
        return "mounted".equals(externalStorageState);
    }

    public static boolean isExternalStorageExist() {
        String externalStorageState = Environment.getExternalStorageState();
        return "mounted".equals(externalStorageState);
    }

    public static File getExternalStorageDirectory() {
        return Environment.getExternalStorageDirectory();
    }

    public static File getExternalStorageDirectoryIfExist() {
        boolean sdCardExist = isExternalStorageExist();
        boolean sdCardEnable = isExternalStorageMountedReadWrite();
        return sdCardExist && sdCardEnable ? getExternalStorageDirectory() : null;
    }

    public static File getExternalCacheDir(Context context) {
        try {
            if (VERSION.SDK_INT >= 8) {
                return getExternalCacheDirFroyo(context);
            } else {
                File externalStorageDirectory = Environment.getExternalStorageDirectory();
                return new File(externalStorageDirectory, "Android/data/" + context.getPackageName() + "/cache");
            }
        } catch (Exception var3) {
            File externalStorageDirectory = Environment.getExternalStorageDirectory();
            return new File(externalStorageDirectory, "Android/data/" + context.getPackageName() + "/cache");
        }
    }

    @TargetApi(8)
    private static File getExternalCacheDirFroyo(Context context) {
        return context.getExternalCacheDir();
    }

    public static File getExternalFilesDir(Context context, String type) {
        if (VERSION.SDK_INT >= 8) {
            return getExternalFilesDirFroyo(context, type);
        } else {
            File externalStorageDirectory = Environment.getExternalStorageDirectory();
            return new File(externalStorageDirectory, "Android/data/" + context.getPackageName() + "/files");
        }
    }

    @TargetApi(8)
    private static File getExternalFilesDirFroyo(Context context, String type) {
        return context.getExternalFilesDir(type);
    }
}
