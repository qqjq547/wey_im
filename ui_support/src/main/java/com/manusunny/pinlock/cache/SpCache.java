package com.manusunny.pinlock.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import framework.telegram.support.tools.AESHelper;
import framework.telegram.support.tools.HexString;

public class SpCache implements ICache {

    private final static String SP_PIN_UNLOCK = "sp_pincode_unlock";
    private final static String SP_KEY_PIN_UNLOCK_CODE = "pincode_unlock_code";
    private final static String SP_KEY_PIN_UNLOCK_ERROR_MAX = "pincode_unlock_error_max_time";
    private final static String SP_KEY_PIN_UNLOCK_ERROR_LEFT = "pincode_unlock_error_left_count";
    private final static String SP_KEY_PIN_UNLOCK_ERROR_TOTAL = "pincode_unlock_error_total_count";
    private final static String Secret = "*&^HU%09*&^><?:1";

    @Override
    public boolean isPinCodeSet(Context context, String key) {
        return !TextUtils.isEmpty(getPinCodeSet(context, key));
    }

    @Override
    public String getPinCodeSet(Context context, String key) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_PIN_UNLOCK, Context.MODE_PRIVATE);
            String code = sp.getString(SP_KEY_PIN_UNLOCK_CODE + "_" + key, "");
            String c = AESHelper.decrypt(HexString.hexToBuffer(code), Secret);
            Log.d("demo", "取 c--->   " + key + "   " + c);
            return c;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public void clearPinCode(Context context, String key) {
        setPinCode(context, key, "");
    }

    @Override
    public void setPinCode(Context context, String key, String gestureCode) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_PIN_UNLOCK, Context.MODE_PRIVATE);
            String code = AESHelper.encrypt(gestureCode.getBytes(), Secret);
            sp.edit().putString(SP_KEY_PIN_UNLOCK_CODE + "_" + key, code).apply();
            Log.d("demo", "存 c--->   " + key + "   " + code);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUnlockErrorMax(Context context, String key, long time) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_PIN_UNLOCK, Context.MODE_PRIVATE);
            sp.edit().putLong(SP_KEY_PIN_UNLOCK_ERROR_MAX + "_" + key, time).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getUnlockErrorMax(Context context, String key) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_PIN_UNLOCK, Context.MODE_PRIVATE);
            return sp.getLong(SP_KEY_PIN_UNLOCK_ERROR_MAX + "_" + key, 0L);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public void setUnlockErrorCount(Context context, String key, int leftCount) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_PIN_UNLOCK, Context.MODE_PRIVATE);
            sp.edit().putInt(SP_KEY_PIN_UNLOCK_ERROR_LEFT + "_" + key, leftCount).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getUnlockErrorCount(Context context, String key) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_PIN_UNLOCK, Context.MODE_PRIVATE);
            return sp.getInt(SP_KEY_PIN_UNLOCK_ERROR_LEFT + "_" + key, 5);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public void setTotalUnlockErrorCount(Context context, String key, int count) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_PIN_UNLOCK, Context.MODE_PRIVATE);
            sp.edit().putInt(SP_KEY_PIN_UNLOCK_ERROR_TOTAL + "_" + key, count).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getTotalUnlockErrorCount(Context context, String key) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_PIN_UNLOCK, Context.MODE_PRIVATE);
            return sp.getInt(SP_KEY_PIN_UNLOCK_ERROR_TOTAL + "_" + key, 5);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
