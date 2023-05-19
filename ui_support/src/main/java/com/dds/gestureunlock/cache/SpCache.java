package com.dds.gestureunlock.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import framework.telegram.support.tools.AESHelper;
import framework.telegram.support.tools.HexString;

/**
 * Created by dds on 2019/2/13.
 * android_shuai@163.com
 */
public class SpCache implements ICache {

    private final static String SP_GESTURE_UNLOCK = "sp_gesture_unlock";
    private final static String SP_KEY_GESTURE_UNLOCK_CODE = "gesture_unlock_code";
    private final static String SP_KEY_GESTURE_UNLOCK_ERROR_MAX = "gesture_unlock_error_max_time";
    private final static String SP_KEY_GESTURE_UNLOCK_ERROR_LEFT = "gesture_unlock_error_left_count";
    private final static String SP_KEY_GESTURE_UNLOCK_ERROR_TOTAL = "gesture_unlock_error_total_count";
    private final static String Secret = "*&^HU%09*&^><?:1";

    @Override
    public boolean isGestureCodeSet(Context context, String key) {
        return !TextUtils.isEmpty(getGestureCodeSet(context, key));
    }

    @Override
    public String getGestureCodeSet(Context context, String key) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_GESTURE_UNLOCK, Context.MODE_PRIVATE);
            String code = sp.getString(SP_KEY_GESTURE_UNLOCK_CODE + "_" + key, "");
            String c = AESHelper.decrypt(HexString.hexToBuffer(code), Secret);
            Log.d("demo", "取 c--->   " + key + "   " + c);
            return c;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public void clearGestureCode(Context context, String key) {
        setGestureCode(context, key, "");
    }

    @Override
    public void setGestureCode(Context context, String key, String gestureCode) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_GESTURE_UNLOCK, Context.MODE_PRIVATE);
            String code = AESHelper.encrypt(gestureCode.getBytes(), Secret);
            sp.edit().putString(SP_KEY_GESTURE_UNLOCK_CODE + "_" + key, code).apply();
            Log.d("demo", "存 c--->   " + key + "   " + code);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUnlockErrorMax(Context context, String key, long time) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_GESTURE_UNLOCK, Context.MODE_PRIVATE);
            sp.edit().putLong(SP_KEY_GESTURE_UNLOCK_ERROR_MAX + "_" + key, time).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getUnlockErrorMax(Context context, String key) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_GESTURE_UNLOCK, Context.MODE_PRIVATE);
            return sp.getLong(SP_KEY_GESTURE_UNLOCK_ERROR_MAX + "_" + key, 0L);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public void setUnlockErrorCount(Context context, String key, int leftCount) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_GESTURE_UNLOCK, Context.MODE_PRIVATE);
            sp.edit().putInt(SP_KEY_GESTURE_UNLOCK_ERROR_LEFT + "_" + key, leftCount).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getUnlockErrorCount(Context context, String key) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_GESTURE_UNLOCK, Context.MODE_PRIVATE);
            return sp.getInt(SP_KEY_GESTURE_UNLOCK_ERROR_LEFT + "_" + key, 5);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


    @Override
    public void setTotalUnlockErrorCount(Context context, String key, int count) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_GESTURE_UNLOCK, Context.MODE_PRIVATE);
            sp.edit().putInt(SP_KEY_GESTURE_UNLOCK_ERROR_TOTAL + "_" + key, count).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getTotalUnlockErrorCount(Context context, String key) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_GESTURE_UNLOCK, Context.MODE_PRIVATE);
            return sp.getInt(SP_KEY_GESTURE_UNLOCK_ERROR_TOTAL + "_" + key, 5);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
