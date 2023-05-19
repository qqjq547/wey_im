package framework.telegram.ui.tools;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {

    private Helper() {
    }

    public static boolean isDestroyedActivity(Activity activity) {
        if (activity == null) {
            return true;
        } else {
            if (Build.VERSION.SDK_INT >= 17) {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return true;
                }
            } else if (activity.isFinishing()) {
                return true;
            }

            return false;
        }
    }

    public static void setPrimaryClip(Context context, String text) {
        ClipboardManager cmb = (ClipboardManager) context.getSystemService("clipboard");
        cmb.setPrimaryClip(ClipData.newPlainText((CharSequence) null, text));
    }

    public static boolean isFullScreen() {
        return Build.VERSION.SDK_INT >= 19;
    }
}
