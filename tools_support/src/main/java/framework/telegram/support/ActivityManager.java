package framework.telegram.support;

import android.app.Activity;

import java.util.Stack;

/**
 * Created by yanggl on 2019/9/27 16:52
 */
public class ActivityManager {
    private static final Stack<Activity> sActivityStack = new Stack<>();

    /**
     * 添加Activity到堆栈
     */
    public static void addActivity(Activity activity) {
        sActivityStack.add(activity);
    }

    /**
     * 删除堆栈中的Activity
     */
    public static void removeActivity(Activity activity) {
        if (sActivityStack.isEmpty()) {
            return;
        }
        sActivityStack.remove(activity);
    }

    /**
     * 获取当前Activity（堆栈中最后一个压入的）
     */
    public static Activity currentActivity() {

        Activity activity = sActivityStack.lastElement();
        return activity;
    }

    /**
     * 结束当前Activity（堆栈中最后一个压入的）
     */
    public static void finishActivity() {
        Activity activity = sActivityStack.lastElement();
        finishActivity(activity);
    }

    /**
     * 结束指定的Activity
     */
    public static void finishActivity(Activity activity) {
        if (activity != null) {
            sActivityStack.remove(activity);
            activity.finish();
        }
    }

    /**
     * 结束指定类名的Activity
     */
    public static void finishActivity(Class<?> cls) {
        for (Activity activity : sActivityStack) {
            if (activity.getClass().equals(cls)) {
                finishActivity(activity);
                return;
            }
        }

    }

    //获取指定类名的Activity
    public static Activity getActivity(Class<?> cls) {
        for (Activity activity : sActivityStack) {
            if (activity.getClass().equals(cls)) {
                return activity;
            }
        }
        return null;
    }

    /**
     * 结束所有Activity
     */
    public static void finishAllActivity() {
        for (int i = 0, size = sActivityStack.size(); i < size; i++) {
            if (null != sActivityStack.get(i)) {
                sActivityStack.get(i).finish();
            }
        }
        sActivityStack.clear();
    }

    public static void finishAllOtherActivity(Activity activity) {
        for (int i = 0, size = sActivityStack.size(); i < size; i++) {
            if (null != sActivityStack.get(i) && sActivityStack.get(i) != activity) {
                sActivityStack.get(i).finish();
            }
        }
        sActivityStack.clear();
    }

    public static void recreateAllOtherActivity(Activity activity) {
        for (int i = 0, size = sActivityStack.size(); i < size; i++) {
            if (null != sActivityStack.get(i) && sActivityStack.get(i) != activity) {
                sActivityStack.get(i).recreate();
            }
        }
    }

}
