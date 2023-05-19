package com.dds.gestureunlock.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.appcompat.widget.AppCompatImageView;

import com.dds.gestureunlock.JsConst;

public class CircleImageView extends AppCompatImageView {

    private Paint paint = null;
    private static final int strokeWidth = 2;
    private int normalColor;
    private int selectedColor;
    private int errorColor;
    private int currentColor;
    private int state;
    private int blockWidth;
    private int radius;
    private boolean isShowTrack = true;
    private int unSelectColor1;
    private int unSelectColor2;
    private int lineColor;

    public int getRadius() {
        return radius;
    }

    public CircleImageView(Context context) {
        super(context);
    }

    public CircleImageView(Context context, int width, int normalColor,
                           int selectedColor, int errorColor,int unSelectColor1,int unSelectColor2, int lineColor, boolean isShowTrack) {
        super(context);
        paint = new Paint();
        paint.setAntiAlias(true);

        this.normalColor = normalColor;
        this.selectedColor = selectedColor;
        this.errorColor = errorColor;
        this.currentColor = normalColor;
        this.blockWidth = width;
        this.radius = blockWidth / 2;
        this.isShowTrack = isShowTrack;
        this.unSelectColor1 = unSelectColor1;
        this.unSelectColor2 = unSelectColor2;
        this.lineColor = lineColor;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (state != JsConst.POINT_STATE_NORMAL) {
            if (state == JsConst.POINT_STATE_SELECTED && !isShowTrack) {
                return;
            }

//            paint.setStyle(Paint.Style.FILL);
//            paint.setColor(unSelectColor1);
//            canvas.drawCircle(blockWidth / 2, blockWidth / 2, radius , paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            paint.setColor(lineColor);
            canvas.drawCircle(blockWidth / 2, blockWidth / 2, radius - 3, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(currentColor);
            canvas.drawCircle(blockWidth / 2, blockWidth / 2, radius / 3, paint);


        }else {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(unSelectColor1);
            canvas.drawCircle(blockWidth / 2, blockWidth / 2, radius , paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(unSelectColor2);
            canvas.drawCircle(blockWidth / 2, blockWidth / 2, radius / 3, paint);
        }
    }

    public void setCurrentState(int state) {
        this.state = state;
        switch (state) {
            case JsConst.POINT_STATE_NORMAL:
                this.currentColor = normalColor;
                break;
            case JsConst.POINT_STATE_SELECTED:
                if (isShowTrack) {
                    this.currentColor = selectedColor;
                } else {
                    this.currentColor = normalColor;
                }
                break;
            case JsConst.POINT_STATE_WRONG:
                this.currentColor = errorColor;
                break;
        }
        invalidate();
    }
}
