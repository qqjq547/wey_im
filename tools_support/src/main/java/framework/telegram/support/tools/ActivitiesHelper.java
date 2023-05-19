//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package framework.telegram.support.tools;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class ActivitiesHelper {
    private int mCountActivity = 0;//这是用来判断应用是否在前台/后台的

    private long mLastBackgroundAppTime = 0L;

    private LinkedList<Activity> mActs;

    private ActivitiesHelper() {
        this.mActs = new LinkedList();
    }

    public static ActivitiesHelper getInstance() {
        return ActivitiesHelper.SingletonHolder.INSTANCE;
    }

    public void addActivity(Activity act) {
        synchronized (this) {
            this.mActs.addFirst(act);
        }
    }

    public void removeActivity(Activity act) {
        synchronized (this) {
            if (act != null && this.mActs != null && this.mActs.indexOf(act) >= 0) {
                this.mActs.remove(act);
            }

        }
    }

    public Activity getTopActivity() {
        synchronized (this) {
            return this.mActs != null && this.mActs.size() > 0 ? (Activity) this.mActs.get(0) : null;
        }
    }

    public Activity getSecondActivity() {
        synchronized (this) {
            return this.mActs != null && this.mActs.size() > 1 ? (Activity) this.mActs.get(1) : null;
        }
    }

    public void closeAll() {
        synchronized (this) {
            LinkedList activities = new LinkedList(this.mActs);

            while (activities.size() != 0) {
                Activity act = (Activity) activities.poll();
                act.finish();
            }

        }
    }

    public void closeExcept(@NonNull Class<?> activityClass) {
        synchronized (this) {
            LinkedList<Activity> activities = new LinkedList(this.mActs);
            Iterator activityIterator = activities.iterator();

            while (activityIterator.hasNext()) {
                Activity act = (Activity) activityIterator.next();
                if (!act.getClass().getName().equals(activityClass.getName())) {
                    act.finish();
                }
            }

        }
    }

    public void closeTarget(@NonNull Class<?> activityClass) {
        synchronized (this) {
            LinkedList<Activity> activities = new LinkedList(this.mActs);
            Iterator activityIterator = activities.iterator();

            while (activityIterator.hasNext()) {
                Activity act = (Activity) activityIterator.next();
                if (act.getClass().getName().equals(activityClass.getName())) {
                    act.finish();
                }
            }

        }
    }

    public void closeToTarget(@NonNull Class<?> activityClass) {
        synchronized (this) {
            LinkedList<Activity> activities = new LinkedList(this.mActs);
            Iterator activityIterator = activities.iterator();

            while (activityIterator.hasNext()) {
                Activity act = (Activity) activityIterator.next();
                if (act.getClass().getName().equals(activityClass.getName())) {
                    break;
                }
                act.finish();
            }

        }
    }

    public void closeToTarget(@NonNull Activity activity) {
        synchronized (this) {
            LinkedList<Activity> activities = new LinkedList(this.mActs);
            Iterator activityIterator = activities.iterator();

            while (activityIterator.hasNext()) {
                Activity act = (Activity) activityIterator.next();
                if (act == activity) {
                    break;
                }

                act.finish();
            }

        }
    }

    public Activity getTopTargetActivity(@NonNull Class<?> activityClass) {
        synchronized (this) {
            int size = this.mActs.size();

            for (int i = 0; i < size; ++i) {
                Activity act = (Activity) this.mActs.get(i);
                if (act.getClass().getName().equals(activityClass.getName())) {
                    return act;
                }
            }

            return null;
        }
    }

    public Activity getTopTargetActivity(@NonNull String activityClassName) {
        synchronized (this) {
            int size = this.mActs.size();

            for (int i = 0; i < size; ++i) {
                Activity act = (Activity) this.mActs.get(i);
                if (act.getClass().getName().equals(activityClassName)) {
                    return act;
                }
            }

            return null;
        }
    }

    public ArrayList<Activity> getTargetActivity(@NonNull Class<?> activityClass) {
        return getTargetActivity(activityClass.getName());
    }

    public ArrayList<Activity> getTargetActivity(@NonNull String activityClassName) {
        ArrayList<Activity> activities = new ArrayList();
        synchronized (this) {
            int size = this.mActs.size();

            for (int i = 0; i < size; ++i) {
                Activity act = (Activity) this.mActs.get(i);
                if (act.getClass().getName().equals(activityClassName)) {
                    activities.add(act);
                }
            }

            return activities;
        }
    }

    public boolean hasActivity(@NonNull String activityClassName) {
        synchronized (this) {
            int size = this.mActs.size();

            for (int i = 0; i < size; ++i) {
                Activity act = (Activity) this.mActs.get(i);
                if (act.getClass().getName().equals(activityClassName)) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean hasActivity(@NonNull Class<?> activityClass) {
        synchronized (this) {
            int size = this.mActs.size();

            for (int i = 0; i < size; ++i) {
                Activity act = (Activity) this.mActs.get(i);
                if (act.getClass().getName().equals(activityClass.getName())) {
                    return true;
                }
            }

            return false;
        }
    }

    public static void restartActivity(@NonNull Activity activity) {
        Intent restart = new Intent(activity, activity.getClass());
        restart.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.finish();
        activity.overridePendingTransition(0, 0);
        activity.startActivity(restart);
        activity.overridePendingTransition(0, 0);
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

    public void addActivityCount() {
        mCountActivity++;
    }

    public void reduceActivityCount() {
        mCountActivity--;
    }

    public boolean toBackgroud() {
        return mCountActivity == 0;
    }

    public boolean toForeground() {
        return mCountActivity == 1;
    }

    public int getCountActivity() {
        return mCountActivity;
    }

    public long getLastBackgroundAppTime() {
        return mLastBackgroundAppTime;
    }

    public void setLastBackgroundAppTime(long lastBackgroundAppTime) {
        this.mLastBackgroundAppTime = lastBackgroundAppTime;
    }

    private static class SingletonHolder {
        public static final ActivitiesHelper INSTANCE = new ActivitiesHelper();

        private SingletonHolder() {
        }
    }
}
