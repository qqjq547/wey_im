package framework.telegram.ui.widget;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.TouchDelegate;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public class ViewUtils {

    private static long lastClickTime;

    private ViewUtils() {

    }

    public static void expandViewTouchDelegate(final View view, final int value) {
        expandViewTouchDelegate(view, value, value, value, value);
    }

    /**
     * 扩大View的触摸和点击响应范围,最大不超过其父View范围
     *
     * @param view
     * @param top
     * @param bottom
     * @param left
     * @param right
     */
    public static void expandViewTouchDelegate(final View view, final int top,
                                               final int bottom, final int left, final int right) {

        ((View) view.getParent()).post(new Runnable() {
            @Override
            public void run() {
                Rect bounds = new Rect();
                view.setEnabled(true);
                view.getHitRect(bounds);

                bounds.top -= top;
                bounds.bottom += bottom;
                bounds.left -= left;
                bounds.right += right;

                TouchDelegate touchDelegate = new TouchDelegate(bounds, view);

                if (View.class.isInstance(view.getParent())) {
                    ((View) view.getParent()).setTouchDelegate(touchDelegate);
                }
            }
        });
    }

    /**
     * 还原View的触摸和点击响应范围,最小不小于View自身范围
     *
     * @param view
     */
    public static void restoreViewTouchDelegate(final View view) {

        ((View) view.getParent()).post(new Runnable() {
            @Override
            public void run() {
                Rect bounds = new Rect();
                bounds.setEmpty();
                TouchDelegate touchDelegate = new TouchDelegate(bounds, view);

                if (View.class.isInstance(view.getParent())) {
                    ((View) view.getParent()).setTouchDelegate(touchDelegate);
                }
            }
        });
    }

    /**
     * 滑动ScrollView 到底部
     *
     * @param scroll
     */
    public static void scrollToBottom(final ScrollView scroll) {
        if (scroll != null) {
            scroll.post(new Runnable() {

                @Override
                public void run() {
                    scroll.fullScroll(View.FOCUS_DOWN);
                }
            });
        }
    }

    public static void clearCompoundDrawable(TextView textView) {
        textView.setCompoundDrawables(null, null, null, null);
    }

    public static void setLeftDrawable(TextView textView, int drawableId) {
        Drawable drawable = textView.getResources().getDrawable(drawableId);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        textView.setCompoundDrawables(drawable, null, null, null);
    }

    public static void setRightDrawable(TextView textView, int drawableId) {
        Drawable drawable = textView.getResources().getDrawable(drawableId);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        textView.setCompoundDrawables(null, null, drawable, null);
    }

    public static void setTopDrawable(TextView textView, int drawableId) {
        Drawable drawable = textView.getResources().getDrawable(drawableId);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        textView.setCompoundDrawables(null, drawable, null, null);
    }

    public static void setBottomDrawable(TextView textView, int drawableId) {
        Drawable drawable = textView.getResources().getDrawable(drawableId);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        textView.setCompoundDrawables(null, null, null, drawable);
    }

    public static void setLeftDrawable(TextView textView, Drawable drawable) {
        if (drawable != null) {
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        }
        textView.setCompoundDrawables(drawable, null, null, null);
    }

    public static void setRightDrawable(TextView textView, Drawable drawable) {
        if (drawable != null) {
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        }
        textView.setCompoundDrawables(null, null, drawable, null);
    }

    public static void setTopDrawable(TextView textView, Drawable drawable) {
        if (drawable != null) {
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        }
        textView.setCompoundDrawables(null, drawable, null, null);
    }

    public static void setBottomDrawable(TextView textView, Drawable drawable) {
        if (drawable != null) {
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        }
        textView.setCompoundDrawables(null, null, null, drawable);
    }

    public static void registerDoubleClickListener(View view, final OnDoubleClickListener listener) {
        if (listener == null || view == null) return;

        view.setOnClickListener(new View.OnClickListener() {
            private static final int DOUBLE_CLICK_TIME = 333;        //双击间隔时间333毫秒
            private boolean waitDouble = true;


            @Override
            public void onClick(View v) {
                if (waitDouble) {
                    waitDouble = false;//与执行双击事件
                    v.postDelayed(() -> {
                        if (!waitDouble) {
                            //如果过了等待事件还是预执行双击状态，则视为单击
                            waitDouble = true;

                            if (isFastDoubleClick(666)) {
                                return;
                            }

                            listener.OnSingleClick(v);
                        }
                    }, DOUBLE_CLICK_TIME);
                } else {
                    waitDouble = true;
                    listener.OnDoubleClick(v);    //执行双击
                }
            }

            public boolean isFastDoubleClick(int deltaTime) {
                long time = System.currentTimeMillis();
                long timeD = time - lastClickTime;
                if (0 < timeD && timeD < deltaTime) {
                    return true;
                }

                lastClickTime = time;
                return false;
            }
        });
    }

    public interface OnDoubleClickListener {
        void OnSingleClick(View v);

        void OnDoubleClick(View v);
    }
}
