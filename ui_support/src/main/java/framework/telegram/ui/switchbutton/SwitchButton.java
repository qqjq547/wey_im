package framework.telegram.ui.switchbutton;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CompoundButton;

import androidx.annotation.RequiresApi;

import framework.telegram.ui.R;

public class SwitchButton extends CompoundButton {
    private static boolean SHOW_RECT = false;
    private boolean mIsChecked;
    private Configuration mConf;
    private Rect mSafeZone;
    private Rect mBackZone;
    private Rect mThumbZone;
    private RectF mSaveLayerZone;
    private AnimationController mAnimationController;
    private SwitchButton.SBAnimationListener mOnAnimateListener;
    private boolean isAnimating;
    private float mStartX;
    private float mStartY;
    private float mLastX;
    private float mCenterPos;
    private int mTouchSlop;
    private int mClickTimeout;
    private Paint mRectPaint;
    private Rect mBounds;
    private boolean isButtonDrawableNone;
    private OnCheckedChangeListener mOnCheckedChangeListener;

    @SuppressLint({"NewApi"})
    public SwitchButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mIsChecked = false;
        this.mOnAnimateListener = new SwitchButton.SBAnimationListener();
        this.isAnimating = false;
        this.mBounds = null;
        this.initView();
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SwitchButton);
        this.mConf.setThumbMarginInPixel(ta.getDimensionPixelSize(R.styleable.SwitchButton_thumb_margin, this.mConf.getDefaultThumbMarginInPixel()));
        this.mConf.setThumbMarginInPixel(ta.getDimensionPixelSize(R.styleable.SwitchButton_thumb_marginTop, this.mConf.getThumbMarginTop()), ta.getDimensionPixelSize(R.styleable.SwitchButton_thumb_marginBottom, this.mConf.getThumbMarginBottom()), ta.getDimensionPixelSize(R.styleable.SwitchButton_thumb_marginLeft, this.mConf.getThumbMarginLeft()), ta.getDimensionPixelSize(R.styleable.SwitchButton_thumb_marginRight, this.mConf.getThumbMarginRight()));
        this.mConf.setRadius((float) ta.getInt(R.styleable.SwitchButton_buttonRadius, Configuration.Default.DEFAULT_RADIUS));
        this.mConf.setThumbWidthAndHeightInPixel(ta.getDimensionPixelSize(R.styleable.SwitchButton_thumb_width, -1), ta.getDimensionPixelSize(R.styleable.SwitchButton_thumb_height, -1));
        this.mConf.setMeasureFactor(ta.getFloat(R.styleable.SwitchButton_measureFactor, -1.0F));
        this.mConf.setInsetBounds(ta.getDimensionPixelSize(R.styleable.SwitchButton_insetLeft, 0), ta.getDimensionPixelSize(R.styleable.SwitchButton_insetTop, 0), ta.getDimensionPixelSize(R.styleable.SwitchButton_insetRight, 0), ta.getDimensionPixelSize(R.styleable.SwitchButton_insetBottom, 0));
        int velocity = ta.getInteger(R.styleable.SwitchButton_animationVelocity, -1);
        this.mAnimationController.setVelocity(velocity);
        this.fetchDrawableFromAttr(ta);
        ta.recycle();
        if (VERSION.SDK_INT >= 11) {
            this.setLayerType(LAYER_TYPE_SOFTWARE, (Paint) null);
        }

    }

    public SwitchButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwitchButton(Context context) {
        this(context, (AttributeSet) null);
    }

    private void initView() {
        this.mConf = Configuration.getDefault(this.getContext().getResources().getDisplayMetrics().density);
        this.mTouchSlop = ViewConfiguration.get(this.getContext()).getScaledTouchSlop();
        this.mClickTimeout = ViewConfiguration.getPressedStateDuration() + ViewConfiguration.getTapTimeout();
        this.mAnimationController = AnimationController.getDefault().init(this.mOnAnimateListener);
        this.mBounds = new Rect();
        if (SHOW_RECT) {
            this.mRectPaint = new Paint();
            this.mRectPaint.setStyle(Style.STROKE);
        }

    }

    private void fetchDrawableFromAttr(TypedArray ta) {
        if (this.mConf != null) {
            this.mConf.setOffDrawable(this.fetchDrawable(ta, R.styleable.SwitchButton_offDrawable, R.styleable.SwitchButton_offColor, Configuration.Default.DEFAULT_OFF_COLOR));
            this.mConf.setOnDrawable(this.fetchDrawable(ta, R.styleable.SwitchButton_onDrawable, R.styleable.SwitchButton_onColor, Configuration.Default.DEFAULT_ON_COLOR));
            this.mConf.setThumbDrawable(this.fetchThumbDrawable(ta));
        }
    }

    private Drawable fetchDrawable(TypedArray ta, int attrId, int alterColorId, int defaultColor) {
        Drawable tempDrawable = ta.getDrawable(attrId);
        if (tempDrawable == null) {
            int tempColor = ta.getColor(alterColorId, defaultColor);
            tempDrawable = new GradientDrawable();
            ((GradientDrawable) tempDrawable).setCornerRadius(this.mConf.getRadius());
            ((GradientDrawable) tempDrawable).setColor(tempColor);
        }

        return (Drawable) tempDrawable;
    }

    private Drawable fetchThumbDrawable(TypedArray ta) {
        Drawable tempDrawable = ta.getDrawable(R.styleable.SwitchButton_thumbDrawable);
        if (tempDrawable != null) {
            return tempDrawable;
        } else {
            int normalColor = ta.getColor(R.styleable.SwitchButton_thumbColor, Configuration.Default.DEFAULT_THUMB_COLOR);
            int pressedColor = ta.getColor(R.styleable.SwitchButton_thumbPressedColor, Configuration.Default.DEFAULT_THUMB_PRESSED_COLOR);
            StateListDrawable drawable = new StateListDrawable();
            GradientDrawable normalDrawable = new GradientDrawable();
            normalDrawable.setCornerRadius(this.mConf.getRadius());
            normalDrawable.setColor(normalColor);
            GradientDrawable pressedDrawable = new GradientDrawable();
            pressedDrawable.setCornerRadius(this.mConf.getRadius());
            pressedDrawable.setColor(pressedColor);
            drawable.addState(View.PRESSED_ENABLED_STATE_SET, pressedDrawable);
            drawable.addState(new int[0], normalDrawable);
            return drawable;
        }
    }

    public void setConfiguration(Configuration conf) {
        if (this.mConf == null) {
            this.mConf = Configuration.getDefault(conf.getDensity());
        }

        this.mConf.setOffDrawable(conf.getOffDrawableWithFix());
        this.mConf.setOnDrawable(conf.getOnDrawableWithFix());
        this.mConf.setThumbDrawable(conf.getThumbDrawableWithFix());
        this.mConf.setThumbMarginInPixel(conf.getThumbMarginTop(), conf.getThumbMarginBottom(), conf.getThumbMarginLeft(), conf.getThumbMarginRight());
        this.mConf.setThumbWidthAndHeightInPixel(conf.getThumbWidth(), conf.getThumbHeight());
        this.mConf.setVelocity(conf.getVelocity());
        this.mConf.setMeasureFactor(conf.getMeasureFactor());
        this.mAnimationController.setVelocity(this.mConf.getVelocity());
        this.requestLayout();
        this.setup();
        this.setChecked(this.mIsChecked);
    }

    public Configuration getConfiguration() {
        return this.mConf;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.setMeasuredDimension(this.measureWidth(widthMeasureSpec), this.measureHeight(heightMeasureSpec));
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.setup();
    }

    private void setup() {
        this.setupBackZone();
        this.setupSafeZone();
        this.setupThumbZone();
        this.setupDrawableBounds();
        if (this.getMeasuredWidth() > 0 && this.getMeasuredHeight() > 0) {
            this.mSaveLayerZone = new RectF(0.0F, 0.0F, (float) this.getMeasuredWidth(), (float) this.getMeasuredHeight());
        }

        ViewGroup parent = (ViewGroup) this.getParent();
        if (parent != null) {
            parent.setClipChildren(false);
        }

    }

    private void setupSafeZone() {
        int w = this.getMeasuredWidth();
        int h = this.getMeasuredHeight();
        if (w > 0 && h > 0) {
            if (this.mSafeZone == null) {
                this.mSafeZone = new Rect();
            }

            int left = this.getPaddingLeft() + (this.mConf.getThumbMarginLeft() > 0 ? this.mConf.getThumbMarginLeft() : 0);
            int right = w - this.getPaddingRight() - (this.mConf.getThumbMarginRight() > 0 ? this.mConf.getThumbMarginRight() : 0) + -this.mConf.getShrinkX();
            int top = this.getPaddingTop() + (this.mConf.getThumbMarginTop() > 0 ? this.mConf.getThumbMarginTop() : 0);
            int bottom = h - this.getPaddingBottom() - (this.mConf.getThumbMarginBottom() > 0 ? this.mConf.getThumbMarginBottom() : 0) + -this.mConf.getShrinkY();
            this.mSafeZone.set(left, top, right, bottom);
            this.mCenterPos = (float) (this.mSafeZone.left + (this.mSafeZone.right - this.mSafeZone.left - this.mConf.getThumbWidth()) / 2);
        } else {
            this.mSafeZone = null;
        }

    }

    private void setupBackZone() {
        int w = this.getMeasuredWidth();
        int h = this.getMeasuredHeight();
        if (w > 0 && h > 0) {
            if (this.mBackZone == null) {
                this.mBackZone = new Rect();
            }

            int left = this.getPaddingLeft() + (this.mConf.getThumbMarginLeft() > 0 ? 0 : -this.mConf.getThumbMarginLeft());
            int right = w - this.getPaddingRight() - (this.mConf.getThumbMarginRight() > 0 ? 0 : -this.mConf.getThumbMarginRight()) + -this.mConf.getShrinkX();
            int top = this.getPaddingTop() + (this.mConf.getThumbMarginTop() > 0 ? 0 : -this.mConf.getThumbMarginTop());
            int bottom = h - this.getPaddingBottom() - (this.mConf.getThumbMarginBottom() > 0 ? 0 : -this.mConf.getThumbMarginBottom()) + -this.mConf.getShrinkY();
            this.mBackZone.set(left, top, right, bottom);
        } else {
            this.mBackZone = null;
        }

    }

    private void setupThumbZone() {
        int w = this.getMeasuredWidth();
        int h = this.getMeasuredHeight();
        if (w > 0 && h > 0) {
            if (this.mThumbZone == null) {
                this.mThumbZone = new Rect();
            }

            int left = this.mIsChecked ? this.mSafeZone.right - this.mConf.getThumbWidth() : this.mSafeZone.left;
            int right = left + this.mConf.getThumbWidth();
            int top = this.mSafeZone.top;
            int bottom = top + this.mConf.getThumbHeight();
            this.mThumbZone.set(left, top, right, bottom);
        } else {
            this.mThumbZone = null;
        }

    }

    private void setupDrawableBounds() {
        if (this.mBackZone != null) {
            this.mConf.getOnDrawable().setBounds(this.mBackZone);
            this.mConf.getOffDrawable().setBounds(this.mBackZone);
        }

        if (this.mThumbZone != null) {
            this.mConf.getThumbDrawable().setBounds(this.mThumbZone);
        }

    }

    private int measureWidth(int measureSpec) {
        int measuredWidth = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int minWidth = (int) ((float) this.mConf.getThumbWidth() * this.mConf.getMeasureFactor() + (float) this.getPaddingLeft() + (float) this.getPaddingRight());
        int innerMarginWidth = this.mConf.getThumbMarginLeft() + this.mConf.getThumbMarginRight();
        if (innerMarginWidth > 0) {
            minWidth += innerMarginWidth;
        }

        if (specMode == 1073741824) {
            measuredWidth = Math.max(specSize, minWidth);
        } else {
            measuredWidth = minWidth;
            if (specMode == -2147483648) {
                measuredWidth = Math.min(specSize, minWidth);
            }
        }

        measuredWidth += this.mConf.getInsetBounds().left + this.mConf.getInsetBounds().right;
        return measuredWidth;
    }

    private int measureHeight(int measureSpec) {
        int measuredHeight = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int minHeight = this.mConf.getThumbHeight() + this.getPaddingTop() + this.getPaddingBottom();
        int innerMarginHeight = this.mConf.getThumbMarginTop() + this.mConf.getThumbMarginBottom();
        if (innerMarginHeight > 0) {
            minHeight += innerMarginHeight;
        }

        if (specMode == 1073741824) {
            measuredHeight = Math.max(specSize, minHeight);
        } else {
            measuredHeight = minHeight;
            if (specMode == -2147483648) {
                measuredHeight = Math.min(specSize, minHeight);
            }
        }

        measuredHeight += this.mConf.getInsetBounds().top + this.mConf.getInsetBounds().bottom;
        return measuredHeight;
    }

    public void setButtonDrawableIsNone(boolean isNone) {
        this.isButtonDrawableNone = isNone;
        this.invalidate();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!this.isButtonDrawableNone) {
            canvas.getClipBounds(this.mBounds);
            if (this.mBounds != null && this.mConf.needShrink()) {
                this.mBounds.inset(this.mConf.getInsetX(), this.mConf.getInsetY());
                canvas.clipRect(this.mBounds, Op.REPLACE);
                canvas.translate((float) this.mConf.getInsetBounds().left, (float) this.mConf.getInsetBounds().top);
            }

            boolean useGeneralDisableEffect = !this.isEnabled() && this.notStatableDrawable();
            if (useGeneralDisableEffect) {
                canvas.saveLayerAlpha(this.mSaveLayerZone, 127);
            }

            this.mConf.getOffDrawable().draw(canvas);
            this.mConf.getOnDrawable().setAlpha(this.calcAlpha());
            this.mConf.getOnDrawable().draw(canvas);
            this.mConf.getThumbDrawable().draw(canvas);
            if (useGeneralDisableEffect) {
                canvas.restore();
            }

            if (SHOW_RECT) {
                this.mRectPaint.setColor(Color.parseColor("#AA0000"));
                canvas.drawRect(this.mBackZone, this.mRectPaint);
                this.mRectPaint.setColor(Color.parseColor("#00FF00"));
                canvas.drawRect(this.mSafeZone, this.mRectPaint);
                this.mRectPaint.setColor(Color.parseColor("#0000FF"));
                canvas.drawRect(this.mThumbZone, this.mRectPaint);
            }

        }
    }

    private boolean notStatableDrawable() {
        boolean thumbStatable = this.mConf.getThumbDrawable() instanceof StateListDrawable;
        boolean onStatable = this.mConf.getOnDrawable() instanceof StateListDrawable;
        boolean offStatable = this.mConf.getOffDrawable() instanceof StateListDrawable;
        return !thumbStatable || !onStatable || !offStatable;
    }

    private int calcAlpha() {
        int alpha = 255;
        if (this.mSafeZone != null && this.mSafeZone.right != this.mSafeZone.left) {
            int backWidth = this.mSafeZone.right - this.mConf.getThumbWidth() - this.mSafeZone.left;
            if (backWidth > 0) {
                alpha = (this.mThumbZone.left - this.mSafeZone.left) * 255 / backWidth;
            }
        }

        return alpha;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!this.isAnimating && this.isEnabled()) {
            int action = event.getAction();
            float deltaX = event.getX() - this.mStartX;
            float deltaY = event.getY() - this.mStartY;
            boolean nextStatus = this.mIsChecked;
            switch (action) {
                case 0:
                    this.catchView();
                    this.mStartX = event.getX();
                    this.mStartY = event.getY();
                    this.mLastX = this.mStartX;
                    this.setPressed(true);
                    break;
                case 1:
                case 3:
                    this.setPressed(false);
                    nextStatus = this.getStatusBasedOnPos();
                    float time = (float) (event.getEventTime() - event.getDownTime());
                    if (deltaX < (float) this.mTouchSlop && deltaY < (float) this.mTouchSlop && time < (float) this.mClickTimeout) {
                        this.performClick();
                    } else {
                        this.slideToChecked(nextStatus);
                    }
                    break;
                case 2:
                    float x = event.getX();
                    this.moveThumb((int) (x - this.mLastX));
                    this.mLastX = x;
            }

            this.invalidate();
            return true;
        } else {
            return false;
        }
    }

    private boolean getStatusBasedOnPos() {
        return (float) this.mThumbZone.left > this.mCenterPos;
    }

    public void invalidate() {
        if (this.mBounds != null && this.mConf.needShrink()) {
            this.invalidate(this.mBounds);
        } else {
            super.invalidate();
        }

    }

    public boolean performClick() {
        return super.performClick();
    }

    private void catchView() {
        ViewParent parent = this.getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }

    }

    public void setChecked(boolean checked) {
        this.setChecked(checked, true);
    }

    public void setChecked(boolean checked, boolean trigger) {
        if (this.mThumbZone != null) {
            this.moveThumb(checked ? this.getMeasuredWidth() : -this.getMeasuredWidth());
        }

        this.setCheckedInClass(checked, trigger);
    }

    public boolean isChecked() {
        return this.mIsChecked;
    }

    public void toggle() {
        this.toggle(true);
    }

    public void toggle(boolean animated) {
        if (animated) {
            this.slideToChecked(!this.mIsChecked);
        } else {
            this.setChecked(!this.mIsChecked);
        }

    }

    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mConf != null) {
            this.setDrawableState(this.mConf.getThumbDrawable());
            this.setDrawableState(this.mConf.getOnDrawable());
            this.setDrawableState(this.mConf.getOffDrawable());
        }
    }

    private void setDrawableState(Drawable drawable) {
        if (drawable != null) {
            int[] myDrawableState = this.getDrawableState();
            drawable.setState(myDrawableState);
            this.invalidate();
        }

    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
        this.mOnCheckedChangeListener = onCheckedChangeListener;
    }

    private void setCheckedInClass(boolean checked) {
        this.setCheckedInClass(checked, true);
    }

    private void setCheckedInClass(boolean checked, boolean trigger) {
        if (this.mIsChecked != checked) {
            this.mIsChecked = checked;
            this.refreshDrawableState();
            if (this.mOnCheckedChangeListener != null && trigger) {
                this.mOnCheckedChangeListener.onCheckedChanged(this, this.mIsChecked);
            }

        }
    }

    public void slideToChecked(boolean checked) {
        if (!this.isAnimating) {
            int from = this.mThumbZone.left;
            int to = checked ? this.mSafeZone.right - this.mConf.getThumbWidth() : this.mSafeZone.left;
            this.mAnimationController.startAnimation(from, to);
        }
    }

    private void moveThumb(int delta) {
        int newLeft = this.mThumbZone.left + delta;
        int newRight = this.mThumbZone.right + delta;
        if (newLeft < this.mSafeZone.left) {
            newLeft = this.mSafeZone.left;
            newRight = newLeft + this.mConf.getThumbWidth();
        }

        if (newRight > this.mSafeZone.right) {
            newRight = this.mSafeZone.right;
            newLeft = newRight - this.mConf.getThumbWidth();
        }

        this.moveThumbTo(newLeft, newRight);
    }

    private void moveThumbTo(int newLeft, int newRight) {
        this.mThumbZone.set(newLeft, this.mThumbZone.top, newRight, this.mThumbZone.bottom);
        this.mConf.getThumbDrawable().setBounds(this.mThumbZone);
    }

    class SBAnimationListener implements AnimationController.OnAnimateListener {
        SBAnimationListener() {
        }

        public void onAnimationStart() {
            SwitchButton.this.isAnimating = true;
        }

        public boolean continueAnimating() {
            return SwitchButton.this.mThumbZone.right < SwitchButton.this.mSafeZone.right && SwitchButton.this.mThumbZone.left > SwitchButton.this.mSafeZone.left;
        }

        public void onFrameUpdate(int frame) {
            SwitchButton.this.moveThumb(frame);
            SwitchButton.this.postInvalidate();
        }

        public void onAnimateComplete() {
            SwitchButton.this.setCheckedInClass(SwitchButton.this.getStatusBasedOnPos());
            SwitchButton.this.isAnimating = false;
        }
    }
}
