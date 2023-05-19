package com.caesar.musicspectrumbarlibrary;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

import framework.telegram.ui.R;
import framework.telegram.ui.utils.ScreenUtils;

/**
 * created by Caesar on 2019/6/11
 * email : 15757855271@163.com
 */
public class MusicSpectrumBar extends View {

    //上次拦截滑动的坐标
    private float mLastXIntercept = 0f;
    private float mLastYIntercept = 0f;
    private Paint paint;
    private ArrayList<SpectrumData> myCData;
    private long key;
    private int[] highD = {1, 3, 5, 4, 6, 2, 7, 5, 6, 3, 2, 1, 2, 1, 2, 6, 5, 4, 2, 7, 5, 2, 3, 1, 2, 1, 3, 2, 1};
    private String colorStr = "#178aff";
    private OnSeekChangeListener listener;
    private int currentT = -1;
    private int roundAngle;
    private int poseType;
    private float gapMultiple;
    private int unSelectColor;
    private float spectMultiple;
//    private static Bitmap peekBmp;
//    private static int bottomMargin;

    public MusicSpectrumBar(Context context) {
        this(context, null);
    }

    public MusicSpectrumBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MusicSpectrumBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.MusicSpectrumBar);
        roundAngle = array.getInt(R.styleable.MusicSpectrumBar_roundAngle, 5);
        poseType = array.getInt(R.styleable.MusicSpectrumBar_poseType, 0);
        gapMultiple = array.getFloat(R.styleable.MusicSpectrumBar_gapMultiple, 2);
        unSelectColor = array.getColor(R.styleable.MusicSpectrumBar_unSelectColor, Color.WHITE);
        spectMultiple = array.getFloat(R.styleable.MusicSpectrumBar_spectMultiple, (float) 0.5);

//        if (peekBmp == null || peekBmp.isRecycled()) {
//            peekBmp = BitmapFactory.decodeResource(getResources(), R.drawable.icon_audio_peek);
//        }
//        if (bottomMargin == 0) {
//            bottomMargin = ScreenUtils.dp2px(getContext(), 2);
//        }

        array.recycle();
        clearItems();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (myCData.isEmpty()) {
            return;
        }

        if (paint == null) {
            paint = new Paint();
            paint.setAntiAlias(true);
        }

        for (int i = 0; i < highD.length; i++) {
            if (i <= currentT) {
                paint.setColor(Color.parseColor(colorStr));
            } else {
                paint.setColor(unSelectColor);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                canvas.drawRoundRect(myCData.get(i).getLeft(), myCData.get(i).getTop(), myCData.get(i).getRight(), myCData.get(i).getBottom(), roundAngle, roundAngle, paint);
            } else {
                canvas.drawRect(myCData.get(i).getLeft(), myCData.get(i).getTop(), myCData.get(i).getRight(), myCData.get(i).getBottom(), paint);
            }

//            if (i == currentT) {
//                canvas.drawBitmap(peekBmp, myCData.get(i).getRight(), 0, paint);
//            }
        }
    }


    /**
     * 强迫症完美主义者实现
     *
     * @return 完美
     */
    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private float startTouchX = 0.0f;
    private float startCurrentT = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int viewAllWidth = getMeasuredWidth();
        float leftX;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startTouchX = event.getX();
                startCurrentT = currentT;
            case MotionEvent.ACTION_MOVE:
                leftX = event.getX() - startTouchX;
                currentT = (int) (startCurrentT + ((leftX / viewAllWidth) * highD.length));
                currentT = Math.min(Math.max(-1, currentT), highD.length);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                startTouchX = 0.0f;
                startCurrentT = -1;
                if (listener != null) {
                    listener.onStopTrackingTouch();
                }
            case MotionEvent.ACTION_CANCEL:
                startTouchX = 0.0f;
                startCurrentT = -1;
        }
        return super.onTouchEvent(event);
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = x - mLastXIntercept;
                float deltaY = y - mLastYIntercept;
                if (Math.abs(deltaX) < 1 && Math.abs(deltaY) > 10 * Math.abs(deltaX)) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
            default:
                break;
        }
        mLastXIntercept = x;
        mLastYIntercept = y;
        return super.dispatchTouchEvent(event);
    }

    /**
     * 设置每条频谱线的属性
     */
    private void setItems() {
        int viewAllWidth = getMeasuredWidth();
        int viewAllHigh = getMeasuredHeight();
        float lineWidth = ((float) viewAllWidth) / ((highD.length - 1) * (1 + gapMultiple) + 1);
        float lineMinHigh = ((float) viewAllHigh) / getMaxIntArr();
        float spectMinHigh = (float) viewAllHigh * (1 - spectMultiple) / (getMaxIntArr() - 1);
        if (myCData.isEmpty()) {
            for (int i = 0; i < highD.length; i++) {
                float lineStartW = (float) i * (1 + gapMultiple) * lineWidth;
                float lineStartH = 0;
                if (poseType == 0) {
                    lineStartH = ((float) viewAllHigh - highD[i] * lineMinHigh) / 2;
                    myCData.add(new SpectrumData(lineStartW, lineStartW + lineWidth, lineStartH, lineStartH + highD[i] * lineMinHigh, colorStr));
                } else if (poseType == 1) {
                    lineStartH = (float) viewAllHigh - highD[i] * lineMinHigh;
                    myCData.add(new SpectrumData(lineStartW, lineStartW + lineWidth, lineStartH, lineStartH + highD[i] * lineMinHigh, colorStr));
                } else if (poseType == 2) {
                    myCData.add(new SpectrumData(lineStartW, lineStartW + lineWidth, lineStartH, lineStartH + highD[i] * lineMinHigh, colorStr));
                } else {
                    myCData.add(new SpectrumData(lineStartW, lineStartW + lineWidth, spectMinHigh * (highD[i] - 1), spectMinHigh * (highD[i] - 1) + (float) viewAllHigh * spectMultiple, colorStr));
                }
            }
        }
    }

    /**
     * 获取频谱进度条中最长一条
     *
     * @return 最长的长度倍数
     */
    private int getMaxIntArr() {
        int maxSin = 0;
        for (int item : highD) {
            if (item > maxSin) {
                maxSin = item;
            }
        }
        return maxSin;
    }

    /**
     * 清除进度条里的频谱数据
     */
    private void clearItems() {
        if (myCData == null) {
            myCData = new ArrayList<>();
        }
        myCData.clear();
    }

    /**
     * 设置进度的属性数据
     *
     * @param highDArr 频谱条的数据组
     */
    public void setDatas(long key, int[] highDArr) {
        post(() -> {
            if (this.key == 0 || this.key != key) {
                this.key = key;
                this.highD = highDArr;
                clearItems();
                setItems();
                invalidate();
            }
        });
    }

    /**
     * 设置当前进度
     *
     * @param current 进度 0-100
     */
    public void setCurrent(int current) {
        if (current < 0) {
            currentT = -1;
        } else {
            currentT = highD.length * current / 100;
        }

        invalidate();
    }

    /**
     * 进度 0-100
     *
     * @return
     */
    public int getCurrent() {
        return currentT * 100 / highD.length;
    }

    /**
     * 长度
     *
     * @return
     */
    public int getLength() {
        return highD.length;
    }

    /**
     * 设置监听器,跟seekbar的滑动监听一样
     *
     * @param listener 监听器
     */
    public void setOnSeekBarChangeListener(OnSeekChangeListener listener) {
        this.listener = listener;
    }

    public interface OnSeekChangeListener {
        void onStopTrackingTouch();
    }
}
