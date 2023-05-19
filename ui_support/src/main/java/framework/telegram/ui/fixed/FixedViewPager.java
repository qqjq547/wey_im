package framework.telegram.ui.fixed;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;

import androidx.viewpager.widget.ViewPager;
import framework.telegram.ui.tools.Helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

public class FixedViewPager extends ViewPager {
    private static final float LEFT_VALID_DISTANCE = -200.0F;
    private static final float RIGHT_VALID_DISTANCE = 300.0F;
    private float mLastPointActionDownX;
    private GestureDetector mGestureDetector;
    private FixedViewPager.OnEdgeFlingListener mOnEdgeFlingListener;
    private int mJumpToPosition;
    private OnPageChangeListener mOnPageChangeListener;
    private List<OnPageChangeListener> mOnPageChangeListeners;
    private boolean isCanScroll = true;
    private OnGestureListener onGestureListener = new SimpleOnGestureListener() {
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                float startX = e1 == null?FixedViewPager.this.mLastPointActionDownX:e1.getX();
                if(e2 != null) {
//                    LogUtils.e("e2 x : " + e2.getX());
                }

                float distance = e2.getX() - startX;
                if(FixedViewPager.this.getCurrentItem() == 0 && distance > 300.0F && FixedViewPager.this.mOnEdgeFlingListener != null) {
                    FixedViewPager.this.mOnEdgeFlingListener.onLeftEdgeFling();
                } else if(FixedViewPager.this.getCurrentItem() == FixedViewPager.this.getAdapter().getCount() - 1 && distance < -200.0F && FixedViewPager.this.mOnEdgeFlingListener != null) {
                    FixedViewPager.this.mOnEdgeFlingListener.onRightEdgeFling();
                }
            } catch (Exception var7) {
                var7.printStackTrace();
            }

            return false;
        }
    };

    public FixedViewPager(Context context) {
        super(context);
        this.mGestureDetector = new GestureDetector(context, this.onGestureListener);
    }

    public FixedViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mGestureDetector = new GestureDetector(context, this.onGestureListener);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if(this.isCanScroll) {
            try {
                this.mGestureDetector.onTouchEvent(ev);
                return super.onTouchEvent(ev);
            } catch (Exception var3) {
                var3.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(this.isCanScroll) {
            try {
                if(ev != null && ev.getAction() == 0) {
                    this.mLastPointActionDownX = ev.getX();
                }

                return super.onInterceptTouchEvent(ev);
            } catch (Exception var3) {
                var3.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    public void setOnEdgeFlingListener(FixedViewPager.OnEdgeFlingListener mOnEdgeFlingListener) {
        this.mOnEdgeFlingListener = mOnEdgeFlingListener;
    }

    public void setCurrentItem(int item) {
        super.setCurrentItem(item);
        this.mJumpToPosition = item;
        (new FixedViewPager.ViewPagerSetCurrentRunnable()).run();
    }

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        super.setOnPageChangeListener(listener);
        this.mOnPageChangeListener = listener;
    }

    public void addOnPageChangeListener(OnPageChangeListener listener) {
        super.addOnPageChangeListener(listener);
        if(listener != null) {
            if(this.mOnPageChangeListeners == null) {
                this.mOnPageChangeListeners = new ArrayList();
            }

            this.mOnPageChangeListeners.add(listener);
        }
    }

    public void removeOnPageChangeListener(OnPageChangeListener listener) {
        super.removeOnPageChangeListener(listener);
        if(listener != null) {
            if(this.mOnPageChangeListeners != null && this.mOnPageChangeListeners.contains(listener)) {
                this.mOnPageChangeListeners.remove(listener);
            }

        }
    }

    public void setCanScroll(boolean isCanScroll) {
        this.isCanScroll = isCanScroll;
    }

    public interface OnEdgeFlingListener {
        void onLeftEdgeFling();

        void onRightEdgeFling();
    }

    private class ViewPagerSetCurrentRunnable implements Runnable {
        private ViewPagerSetCurrentRunnable() {
        }

        public void run() {
            if(!(FixedViewPager.this.getContext() instanceof Activity) || !Helper.isDestroyedActivity((Activity)FixedViewPager.this.getContext())) {
                if(FixedViewPager.this.mJumpToPosition >= 0) {
                    if(FixedViewPager.this.getChildCount() == 0) {
                        FixedViewPager.this.postDelayed(FixedViewPager.this.new ViewPagerSetCurrentRunnable(), 20L);
                    } else {
                        if(FixedViewPager.this.mJumpToPosition == 0) {
                            if(FixedViewPager.this.mOnPageChangeListener != null) {
                                FixedViewPager.this.mOnPageChangeListener.onPageSelected(0);
                            }

                            if(FixedViewPager.this.mOnPageChangeListeners != null) {
                                Iterator var1 = FixedViewPager.this.mOnPageChangeListeners.iterator();

                                while(var1.hasNext()) {
                                    OnPageChangeListener onPageChangeListener = (OnPageChangeListener)var1.next();
                                    if(onPageChangeListener != null) {
                                        onPageChangeListener.onPageSelected(0);
                                    }
                                }
                            }
                        } else {
                            FixedViewPager.this.setCurrentItem(FixedViewPager.this.mJumpToPosition, false);
                        }

                        FixedViewPager.this.mJumpToPosition = -1;
                    }
                }

            }
        }
    }
}
