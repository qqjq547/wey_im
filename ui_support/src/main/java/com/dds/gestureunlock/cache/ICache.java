package com.dds.gestureunlock.cache;

import android.content.Context;

/**
 * 密码逻辑接口
 * Created by dds on 2019/2/13.
 * android_shuai@163.com
 */
public interface ICache {

    // 判断是否设置了手势密码
    boolean isGestureCodeSet(Context context, String key);

    // 获取手势密码
    String getGestureCodeSet(Context context, String key);

    // 清空手势密码
    void clearGestureCode(Context context, String key);

    // 设置手势密码
    void setGestureCode(Context context, String key, String gestureCode);

    void setUnlockErrorMax(Context context, String key, long time);

    long getUnlockErrorMax(Context context, String key);

    void setUnlockErrorCount(Context context, String key, int leftCount);

    int getUnlockErrorCount(Context context, String key);

    void setTotalUnlockErrorCount(Context context, String key, int count);

    int getTotalUnlockErrorCount(Context context, String key);
}
