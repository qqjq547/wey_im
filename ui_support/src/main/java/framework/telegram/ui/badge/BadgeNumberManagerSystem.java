package framework.telegram.ui.badge;

import androidx.core.app.NotificationCompat;

/**
 * vivo机型的桌面角标设置管理类
 */
public class BadgeNumberManagerSystem {

    public static NotificationCompat.Builder setBadgeNumber(NotificationCompat.Builder notificationBuilder, int number) {
        notificationBuilder.setNumber(number).setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL);
        return notificationBuilder;
    }
}