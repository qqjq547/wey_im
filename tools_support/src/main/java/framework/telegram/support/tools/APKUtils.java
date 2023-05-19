package framework.telegram.support.tools;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * APK操作工具类
 */
public class APKUtils {

    private APKUtils() {
    }

    /**
     * 是否安装高德地图
     *
     * @param context
     * @return
     */
    public static boolean isInstallAMap(Context context) {
        return isInstall(context, "com.autonavi.minimap");
    }

    /**
     * 是否安装百度地图
     *
     * @param context
     * @return
     */
    public static boolean isInstallBaiduMap(Context context) {
        return isInstall(context, "com.baidu.BaiduMap");
    }

    /**
     * 是否安装腾讯地图
     *
     * @param context  context
     * @return 是否安装腾讯地图
     */
    public static boolean isInstallTencentMap(Context context) {
        return isInstall(context, "com.tencent.map");
    }

    /**
     * @param context context
     * @return 是否安装谷歌地图
     */
    public static boolean isInstallGoogleMap(Context context) {
        return isInstall(context, "com.google.android.apps.maps");
    }

    /**
     * @param context     context
     * @param packageName 应用包名
     * @return  是否安装某应用
     */
    public static boolean isInstall(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            return false;
        }

        List<String> pName = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
        if (packageInfos != null) {
            for (int i = 0; i < packageInfos.size(); i++) {
                String pn = packageInfos.get(i).packageName;
                pName.add(pn);
            }
        }
        return pName.contains(packageName);
    }


    public static boolean isApkInstalled(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            return false;
        }
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            packageInfo = null;
            e.printStackTrace();
        }
        return packageInfo != null;
    }


    /**
     * 调用系统安装程序,安装apk文件
     * @param apkFile apk 文件
     * @param context context
     * @return 是否安装成功
     */
    public static boolean installApk(File apkFile, Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.N) {
                Uri contentUri = FileProvider.getUriForFile(context,context.getPackageName(),apkFile);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(contentUri,"application/vnd.android.package-archive");

            }else{
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setDataAndType(Uri.fromFile(apkFile),"application/vnd.android.package-archive");

            }

            context.startActivity(intent);
            return  true;
        } catch (Exception e) {
            e.printStackTrace();
            return  false;
        }
    }


    /**
      * 打开已经安装的应用
      * @param context  context
     * @param packageName 应用包名
     * @return 是否成功打开
     */
    public static boolean openApp(Context context, String packageName) {

        try {
            PackageManager manager = context.getPackageManager();
            Intent intent = manager.getLaunchIntentForPackage(packageName);
            intent.putExtra("caller", "yourpet");  // 使用有宠SDK ,打开游戏相关
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return  false;
        }
    }

}
