package framework.telegram.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;

import com.qmuiteam.qmui.layout.IQMUILayout;
import com.qmuiteam.qmui.layout.QMUIFrameLayout;
import com.qmuiteam.qmui.widget.popup.QMUIBasePopup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import framework.telegram.ui.R;
import framework.telegram.ui.utils.ScreenUtils;


/**
 * 提供一个浮层，支持自定义浮层的内容，支持在指定 {@link View} 的任一方向旁边展示该浮层，支持自定义浮层出现/消失的动画。
 * <p>
 * Created by cgspine on 15/11/24.
 */
public class QMUISreenPopup extends QMUIBasePopup {
    public static final int ANIM_AUTO = 4;

    public static final int DIRECTION_NONE = 2;
    protected int mAnimStyle;
    protected int mX = -1;
    protected int mY = -1;
    private int mPreferredDirection;

    public QMUISreenPopup(Context context) {
        this(context, DIRECTION_NONE);
    }

    public QMUISreenPopup(Context context, int preferredDirection) {
        super(context);
        mAnimStyle = ANIM_AUTO;
        mPreferredDirection = preferredDirection;
    }


    @Override
    protected Point onShowBegin(View parent, View attachedView) {
        calculatePosition(attachedView);
        setAnimationStyle();
        return new Point(mX , mY );
    }

    @Override
    protected void onWindowSizeChange() {

    }

    private void calculatePosition(View attachedView) {
        mX = (mScreenSize.x - mWindowWidth) / 2;
        mY = (mScreenSize.y - mWindowHeight) / 2;
    }

    /**
     * Set animation style
     */
    private void setAnimationStyle() {
        mWindow.setAnimationStyle(R.style.QMUI_Animation_PopUpMenu_Center);
    }

    /**
     * 菜单弹出动画
     *
     * @param mAnimStyle 默认是 ANIM_AUTO
     */
    public void setAnimStyle(int mAnimStyle) {
        this.mAnimStyle = mAnimStyle;
    }

    @Override
    public void setContentView(View root) {
        if (root.getBackground() != null) {
            if (root instanceof IQMUILayout) {
                ((IQMUILayout) root).setRadius(getRootLayoutRadius(mContext));
            } else {
                QMUIFrameLayout clipLayout = new QMUIFrameLayout(mContext);
                clipLayout.setRadius(getRootLayoutRadius(mContext));
                clipLayout.addView(root);
                root = clipLayout;
            }
        }
        root.setLayoutParams(generateLayoutParam(
                ScreenUtils.getScreenWidth(root.getContext()), ScreenUtils.getScreenHeight(root.getContext())));
        @SuppressLint("InflateParams") FrameLayout layout = (FrameLayout) LayoutInflater.from(mContext).inflate(getRootLayout(), null, false);
        layout.addView(root);

        super.setContentView(layout);
    }

    /**
     * the root layout: must provide ids: arrow_down(ImageView), arrow_up(ImageView), box(FrameLayout)
     *
     * @return
     */
    @LayoutRes
    protected int getRootLayout() {
        return R.layout.preview_popup_window;
    }

    protected int getRootLayoutRadius(Context context) {
        return 0;
    }


    public ViewGroup.LayoutParams generateLayoutParam(int width, int height) {
        return new FrameLayout.LayoutParams(width, height);
    }

    @IntDef({DIRECTION_NONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Direction {
    }

}
