package framework.telegram.ui.badge;

import android.content.Context;
import android.os.Build;

import framework.telegram.ui.tools.RomUtils;

public class BadgeNumberManager {
    private Context mContext;

    private BadgeNumberManager(Context context) {
        mContext = context;
    }

    public static BadgeNumberManager from(Context context) {
        return new BadgeNumberManager(context);
    }

    private static final BadgeNumberManager.Impl IMPL;


    /**
     * 设置应用在桌面上显示的角标数字
     *
     * @param number 显示的数字
     */
    public void setBadgeNumber(int number) {
        IMPL.setBadgeNumber(mContext, number);
    }

    interface Impl {
        void setBadgeNumber(Context context, int number);
    }

    static class ImplHuaWei implements Impl {

        @Override
        public void setBadgeNumber(Context context, int number) {
            BadgeNumberManagerHuaWei.setBadgeNumber(context, number);
        }
    }

    static class ImplVIVO implements Impl {

        @Override
        public void setBadgeNumber(Context context, int number) {
            BadgeNumberManagerVIVO.setBadgeNumber(context, number);
        }
    }

    static class ImplOPPO implements Impl {

        @Override
        public void setBadgeNumber(Context context, int number) {
            BadgeNumberManagerOPPO.setBadgeNumber(context, number);
        }
    }

    static class ImplSamsung implements Impl {

        @Override
        public void setBadgeNumber(Context context, int number) {
            BadgeNumberManagerSamsung.setBadgeNumber(context, number);
        }
    }

    static class ImplBase implements Impl {

        @Override
        public void setBadgeNumber(Context context, int number) {
            //do nothing
        }
    }

    static {
        if (RomUtils.INSTANCE.isEmui()) {
            IMPL = new ImplHuaWei();
        } else if (RomUtils.INSTANCE.isHonor()) {
            IMPL = new ImplHuaWei();
        } else if (RomUtils.INSTANCE.isVivo()) {
            IMPL = new ImplVIVO();
        } else if (RomUtils.INSTANCE.isOppo()) {
            IMPL = new ImplOPPO();
        } else if (RomUtils.INSTANCE.isSamsung()) {
            IMPL = new ImplSamsung();
        } else {
            IMPL = new ImplBase();
        }
    }
}
