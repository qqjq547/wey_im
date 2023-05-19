package com.dds.gestureunlock;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.dds.gestureunlock.cache.ICache;
import com.dds.gestureunlock.cache.SpCache;
import com.dds.gestureunlock.util.ResourceUtil;

/**
 * 手势密码入口
 */
public class GestureUnlock {
    private static GestureUnlock sGestureUnlock;
    private ICache cache;
    public static int UNLOCK_ERROR_WAIT_TIME = 5 * 50 * 1000;

    public static GestureUnlock getInstance() {
        if (sGestureUnlock == null) {
            sGestureUnlock = new GestureUnlock();
        }
        return sGestureUnlock;
    }

    public void init(Context applicationContext) {
        ResourceUtil.init(applicationContext);
        cache = new SpCache();
    }

    public void init(Context applicationContext, ICache cache) {
        ResourceUtil.init(applicationContext);
        this.cache = cache;
    }

    public boolean isGestureCodeSet(Context context, String key) {
        return !TextUtils.isEmpty(getGestureCodeSet(context, key));
    }

    public String getGestureCodeSet(Context context, String key) {
        return cache.getGestureCodeSet(context, key);
    }

    public void clearGestureCode(Context context, String key) {
        cache.clearGestureCode(context, key);
    }

    public void clearUnlockErrorMax(Context context, String key) {
        cache.setUnlockErrorMax(context, key, 0);
    }

    public void setGestureCode(Context context, String key, String gestureCode) {
        cache.setGestureCode(context, key, gestureCode);
    }

    public void setUnlockErrorMax(Context context, String key) {
        cache.setUnlockErrorMax(context, key, System.currentTimeMillis());
    }

    public long getUnlockErrorMax(Context context, String key) {
        return cache.getUnlockErrorMax(context, key);
    }

    public void setUnlockErrorCount(Context context, String key, int leftCount) {
        cache.setUnlockErrorCount(context, key, leftCount);
    }

    public int getUnlockErrorCount(Context context, String key) {
        return cache.getUnlockErrorCount(context, key);
    }

    public void setUnlockErrorTotalCount(Context context, String key, int count) {
        cache.setTotalUnlockErrorCount(context, key, count);
    }

    public int getUnlockErrorTotalCount(Context context, String key) {
        return cache.getTotalUnlockErrorCount(context, key);
    }
}
