package framework.telegram.support.tools.language;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import java.util.Locale;

import framework.telegram.support.system.storage.sp.SharePreferencesStorage;

public class LocalManageUtil {

    public static final int FOLLOW_SYSTEM = 0;
    public static final int ENGLISH = 1;
    public static final int SIMPLIFIED_CHINESE = 2;
    public static final int TRADITIONAL_CHINESE = 3;
    public static final int VI = 4;
    public static final int MX = 5; // 墨西哥西班牙语
    public static final int BR = 7; // 巴西葡萄牙语

    private static int mSelectLanguage = -1;

    /**
     * 判断当前选择的语言
     *
     * @return
     */
    public static int getCurLanguaue() {
        switch (getSetLanguageLocale().getLanguage()) {
            case "zh":
                if (getSetLanguageLocale().getCountry().equals("CN")) {
                    Log.e("dsjfsdhfkj", "SIMPLIFIED_CHINESE");
                    return SIMPLIFIED_CHINESE;
                } else {
                    Log.e("dsjfsdhfkj", "TRADITIONAL_CHINESE");
                    return TRADITIONAL_CHINESE;
                }

            case "vi":
                Log.e("dsjfsdhfkj", "VI");
                return VI;

            case "mx":
                Log.e("dsjfsdhfkj", "MX");
                return MX;

            case "br":
                Log.e("dsjfsdhfkj", "BR");
                return BR;

            default:
                Log.e("dsjfsdhfkj", "ENGLISH");
                return ENGLISH;
        }
    }

    public static int getSelectLanguage() {
        if (mSelectLanguage == -1) {
            mSelectLanguage = SharePreferencesStorage.createStorageInstance(LanguagePref.class).getLanguage(0);
        }
        return mSelectLanguage;
    }

    /**
     * 获取选择的语言设置
     *
     * @return
     */
    public static Locale getSetLanguageLocale() {
        switch (getSelectLanguage()) {
            case FOLLOW_SYSTEM:
                return getSystemLocale();
            case SIMPLIFIED_CHINESE:
                return Locale.CHINA;
            case TRADITIONAL_CHINESE:
                return Locale.TAIWAN;

            case VI:
                return new Locale("vi");

            case MX:
                return new Locale("es");

            case BR:
                return new Locale("pt");

            default:
                return Locale.ENGLISH;
        }
    }


    public static void saveSelectLanguage(Context context, int select) {
        mSelectLanguage = select;
        SharePreferencesStorage.createStorageInstance(LanguagePref.class).putLanguage(select);
        MultiLanguage.setApplicationLanguage(context);
    }


    private static Locale systemCurrentLocal = Locale.ENGLISH;

    /**
     * 获取系统的locale
     *
     * @return Locale对象
     */
    public static Locale getSystemLocale() {
        return systemCurrentLocal;
    }

    public static void saveSystemCurrentLanguage(Context context) {
        systemCurrentLocal = MultiLanguage.getSystemLocal(context);
    }




    public static String getCurrentCountryCode(){

        switch (getCurLanguaue()){

            case  SIMPLIFIED_CHINESE:

                return "+86";

            case  TRADITIONAL_CHINESE:

                return "+886";

            case VI:

                return "+84";

            case BR:

                return "+55";

            case MX:

                return "52";

            default:

                return "+44";

        }
    }

}
