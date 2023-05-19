package framework.telegram.app;

import framework.telegram.app.activity.MainActivity;
import framework.telegram.ui.doubleclick.click.annotation.AAddDoubleClick;

/**
 * Created by yanggl on 2019/10/30 10:34
 */
public interface UnifiedDoubleClick {

    //不需要处理重复点击的Activity.
//    @ACancelActivity(activitys = {
//            PrivateChatActivity.class,
//            GroupChatActivity.class,
//    })
//    void cancelActivity();


    //单独处理DoubleClick的View
    @AAddDoubleClick(activity = MainActivity.class,
            addIds = {R.id.frame_layout_messages,R.id.frame_layout_contacts,R.id.frame_layout_phone,R.id.frame_layout_me,R.id.doubleClickMessageId},
            times = {0})
    void privateChatActivity();
}
