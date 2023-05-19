package com.manusunny.pinlock;

import android.content.Context;
import android.text.TextUtils;

import com.manusunny.pinlock.cache.ICache;
import com.manusunny.pinlock.cache.SpCache;

/**
 * Pin密码入口
 */
public class PinCodeUnlock {
    private static PinCodeUnlock sPinCodeUnlock;
    private ICache cache;
    public static int UNLOCK_ERROR_WAIT_TIME = 5 * 50 * 1000;

    public static PinCodeUnlock getInstance() {
        if (sPinCodeUnlock == null) {
            sPinCodeUnlock = new PinCodeUnlock();
        }
        return sPinCodeUnlock;
    }

    public void init(Context applicationContext) {
        cache = new SpCache();
    }

    public void init(Context applicationContext, ICache cache) {
        this.cache = cache;
    }

    public boolean isPinCodeSet(Context context, String key) {
        return !TextUtils.isEmpty(getPinCodeSet(context, key));
    }

    public String getPinCodeSet(Context context, String key) {
        return cache.getPinCodeSet(context, key);
    }

    public void clearPinCode(Context context, String key) {
        cache.clearPinCode(context, key);
    }

    public void clearUnlockErrorMax(Context context, String key) {
        cache.setUnlockErrorMax(context, key, 0);
    }

    public void setPinCode(Context context, String key, String gestureCode) {
        cache.setPinCode(context, key, gestureCode);
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
