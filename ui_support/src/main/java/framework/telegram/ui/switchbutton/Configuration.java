package framework.telegram.ui.switchbutton;


import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import java.lang.reflect.Field;

class Configuration implements Cloneable {
    private Drawable mOnDrawable = null;
    private Drawable mOffDrawable = null;
    private Drawable mThumbDrawable = null;
    private int mOnColor;
    private int mOffColor;
    private int mThumbColor;
    private int mThumbPressedColor;
    private int mThumbMarginTop;
    private int mThumbMarginBottom;
    private int mThumbMarginLeft;
    private int mThumbMarginRight;
    private int mThumbWidth;
    private int mThumbHeight;
    private float density;
    private int mVelocity;
    private float mRadius;
    private float mMeasureFactor;
    private Rect mInsetBounds;

    private Configuration() {
        this.mOnColor = Configuration.Default.DEFAULT_ON_COLOR;
        this.mOffColor = Configuration.Default.DEFAULT_OFF_COLOR;
        this.mThumbColor = Configuration.Default.DEFAULT_THUMB_COLOR;
        this.mThumbPressedColor = Configuration.Default.DEFAULT_THUMB_PRESSED_COLOR;
        this.mThumbMarginTop = 0;
        this.mThumbMarginBottom = 0;
        this.mThumbMarginLeft = 0;
        this.mThumbMarginRight = 0;
        this.mThumbWidth = -1;
        this.mThumbHeight = -1;
        this.mVelocity = -1;
        this.mRadius = -1.0F;
        this.mMeasureFactor = 0.0F;
    }

    public static Configuration getDefault(float density) {
        Configuration defaultConfiguration = new Configuration();
        defaultConfiguration.density = density;
        defaultConfiguration.setThumbMarginInPixel(defaultConfiguration.getDefaultThumbMarginInPixel());
        defaultConfiguration.mInsetBounds = new Rect(Configuration.Default.DEFAULT_INNER_BOUNDS, Configuration.Default.DEFAULT_INNER_BOUNDS, Configuration.Default.DEFAULT_INNER_BOUNDS, Configuration.Default.DEFAULT_INNER_BOUNDS);
        return defaultConfiguration;
    }

    public void setBackDrawable(Drawable offDrawable, Drawable onDrawable) {
        if (onDrawable == null && offDrawable == null) {
            throw new IllegalArgumentException("back drawable can not be null");
        } else {
            if (offDrawable != null) {
                this.mOffDrawable = offDrawable;
                if (onDrawable != null) {
                    this.mOnDrawable = onDrawable;
                } else {
                    this.mOnDrawable = this.mOffDrawable;
                }
            }

        }
    }

    void setOffDrawable(Drawable offDrawable) {
        if (offDrawable == null) {
            throw new IllegalArgumentException("off drawable can not be null");
        } else {
            this.mOffDrawable = offDrawable;
        }
    }

    void setOnDrawable(Drawable onDrawable) {
        if (onDrawable == null) {
            throw new IllegalArgumentException("on drawable can not be null");
        } else {
            this.mOnDrawable = onDrawable;
        }
    }

    public Drawable getOnDrawable() {
        return this.mOnDrawable;
    }

    public Drawable getOffDrawable() {
        return this.mOffDrawable;
    }

    public void setThumbDrawable(Drawable thumbDrawable) {
        if (thumbDrawable == null) {
            throw new IllegalArgumentException("thumb drawable can not be null");
        } else {
            this.mThumbDrawable = thumbDrawable;
        }
    }

    public Drawable getThumbDrawable() {
        return this.mThumbDrawable;
    }

    public void setThumbMargin(int top, int bottom, int left, int right) {
        this.mThumbMarginTop = (int)((float)top * this.density);
        this.mThumbMarginBottom = (int)((float)bottom * this.density);
        this.mThumbMarginLeft = (int)((float)left * this.density);
        this.mThumbMarginRight = (int)((float)right * this.density);
    }

    public void setThumbMarginInPixel(int top, int bottom, int left, int right) {
        this.mThumbMarginTop = top;
        this.mThumbMarginBottom = bottom;
        this.mThumbMarginLeft = left;
        this.mThumbMarginRight = right;
    }

    public void setThumbMargin(int top, int bottom, int leftAndRight) {
        this.setThumbMargin(top, bottom, leftAndRight, leftAndRight);
    }

    public void setThumbMargin(int topAndBottom, int leftAndRight) {
        this.setThumbMargin(topAndBottom, topAndBottom, leftAndRight, leftAndRight);
    }

    public void setThumbMargin(int margin) {
        this.setThumbMargin(margin, margin, margin, margin);
    }

    public void setThumbMarginInPixel(int marginInPixel) {
        this.setThumbMarginInPixel(marginInPixel, marginInPixel, marginInPixel, marginInPixel);
    }

    public int getDefaultThumbMarginInPixel() {
        return (int)((float)Configuration.Default.DEFAULT_THUMB_MARGIN * this.density);
    }

    public int getThumbMarginTop() {
        return this.mThumbMarginTop;
    }

    public int getThumbMarginBottom() {
        return this.mThumbMarginBottom;
    }

    public int getThumbMarginLeft() {
        return this.mThumbMarginLeft;
    }

    public int getThumbMarginRight() {
        return this.mThumbMarginRight;
    }

    public float getDensity() {
        return this.density;
    }

    public void setRadius(float radius) {
        this.mRadius = radius;
    }

    public float getRadius() {
        return this.mRadius < 0.0F ? (float)Configuration.Default.DEFAULT_RADIUS : this.mRadius;
    }

    public void setVelocity(int velocity) {
        this.mVelocity = velocity;
    }

    public int getVelocity() {
        return this.mVelocity;
    }

    public void setOnColor(int onColor) {
        this.mOnColor = onColor;
    }

    public int getOnColor(int onColor) {
        return this.mOnColor;
    }

    public void setOffColor(int offColor) {
        this.mOffColor = offColor;
    }

    public int getOffColor() {
        return this.mOffColor;
    }

    public void setThumbColor(int thumbColor) {
        this.mThumbColor = thumbColor;
    }

    public int getThumbColor() {
        return this.mThumbColor;
    }

    public void setThumbWidthAndHeightInPixel(int width, int height) {
        if (width > 0) {
            this.mThumbWidth = width;
        }

        if (height > 0) {
            this.mThumbHeight = height;
        }

    }

    public void setThumbWidthAndHeight(int width, int height) {
        this.setThumbWidthAndHeightInPixel((int)((float)width * this.density), (int)((float)height * this.density));
    }

    public Drawable getOffDrawableWithFix() {
        return this.mOffDrawable != null ? this.mOffDrawable : this.getDrawableFromColor(this.mOffColor);
    }

    public Drawable getOnDrawableWithFix() {
        return this.mOnDrawable != null ? this.mOnDrawable : this.getDrawableFromColor(this.mOnColor);
    }

    public Drawable getThumbDrawableWithFix() {
        if (this.mThumbDrawable != null) {
            return this.mThumbDrawable;
        } else {
            StateListDrawable drawable = new StateListDrawable();
            Drawable normalDrawable = this.getDrawableFromColor(this.mThumbColor);
            Drawable pressedDrawable = this.getDrawableFromColor(this.mThumbPressedColor);
            int[] stateSet = null;

            try {
                Field stateField = View.class.getDeclaredField("PRESSED_ENABLED_STATE_SET");
                stateField.setAccessible(true);
                stateSet = (int[])((int[])stateField.get((Object)null));
            } catch (Exception var6) {
                var6.printStackTrace();
            }

            if (stateSet != null) {
                drawable.addState(stateSet, pressedDrawable);
            }

            drawable.addState(new int[0], normalDrawable);
            return drawable;
        }
    }

    public float getMeasureFactor() {
        if (this.mMeasureFactor <= 0.0F) {
            this.mMeasureFactor = Configuration.Default.DEFAULT_MEASURE_FACTOR;
        }

        return this.mMeasureFactor;
    }

    public void setMeasureFactor(float measureFactor) {
        if (measureFactor <= 0.0F) {
            this.mMeasureFactor = Configuration.Default.DEFAULT_MEASURE_FACTOR;
        }

        this.mMeasureFactor = measureFactor;
    }

    public Rect getInsetBounds() {
        return this.mInsetBounds;
    }

    public void setInsetBounds(int left, int top, int right, int bottom) {
        this.setInsetLeft(left);
        this.setInsetTop(top);
        this.setInsetRight(right);
        this.setInsetBottom(bottom);
    }

    public void setInsetLeft(int left) {
        if (left > 0) {
            left = -left;
        }

        this.mInsetBounds.left = left;
    }

    public void setInsetTop(int top) {
        if (top > 0) {
            top = -top;
        }

        this.mInsetBounds.top = top;
    }

    public void setInsetRight(int right) {
        if (right > 0) {
            right = -right;
        }

        this.mInsetBounds.right = right;
    }

    public void setInsetBottom(int bottom) {
        if (bottom > 0) {
            bottom = -bottom;
        }

        this.mInsetBounds.bottom = bottom;
    }

    public int getInsetX() {
        return this.getShrinkX() / 2;
    }

    public int getInsetY() {
        return this.getShrinkY() / 2;
    }

    public int getShrinkX() {
        return this.mInsetBounds.left + this.mInsetBounds.right;
    }

    public int getShrinkY() {
        return this.mInsetBounds.top + this.mInsetBounds.bottom;
    }

    public boolean needShrink() {
        return this.mInsetBounds.left + this.mInsetBounds.right + this.mInsetBounds.top + this.mInsetBounds.bottom != 0;
    }

    private Drawable getDrawableFromColor(int color) {
        GradientDrawable tempDrawable = new GradientDrawable();
        tempDrawable.setCornerRadius(this.getRadius());
        tempDrawable.setColor(color);
        return tempDrawable;
    }

    int getThumbWidth() {
        int width = this.mThumbWidth;
        if (width < 0) {
            if (this.mThumbDrawable != null) {
                width = this.mThumbDrawable.getIntrinsicWidth();
                if (width > 0) {
                    return width;
                }
            }

            if (this.density <= 0.0F) {
                throw new IllegalArgumentException("density must be a positive number");
            }

            width = (int)((float)Configuration.Limit.MIN_THUMB_SIZE * this.density);
        }

        return width;
    }

    int getThumbHeight() {
        int height = this.mThumbHeight;
        if (height < 0) {
            if (this.mThumbDrawable != null) {
                height = this.mThumbDrawable.getIntrinsicHeight();
                if (height > 0) {
                    return height;
                }
            }

            if (this.density <= 0.0F) {
                throw new IllegalArgumentException("density must be a positive number");
            }

            height = (int)((float)Configuration.Limit.MIN_THUMB_SIZE * this.density);
        }

        return height;
    }

    static class Limit {
        static int MIN_THUMB_SIZE = 20;

        Limit() {
        }
    }

    static class Default {
        static int DEFAULT_OFF_COLOR = Color.parseColor("#E3E3E3");
        static int DEFAULT_ON_COLOR = Color.parseColor("#02BFE7");
        static int DEFAULT_THUMB_COLOR = Color.parseColor("#FFFFFF");
        static int DEFAULT_THUMB_PRESSED_COLOR = Color.parseColor("#fafafa");
        static int DEFAULT_THUMB_MARGIN = 2;
        static int DEFAULT_RADIUS = 999;
        static float DEFAULT_MEASURE_FACTOR = 2.0F;
        static int DEFAULT_INNER_BOUNDS = 0;

        Default() {
        }
    }
}
