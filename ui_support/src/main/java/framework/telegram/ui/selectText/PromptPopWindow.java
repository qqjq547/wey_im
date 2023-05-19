package framework.telegram.ui.selectText;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import java.util.List;

import framework.telegram.ui.R;

/**
 * Created by wangyang53 on 2018/3/26.
 */

public class PromptPopWindow extends PopupWindow implements CursorView.OnCursorTouchListener {
    private final String TAG = PromptPopWindow.class.getSimpleName();
    private CursorListener mCursorTouchListener;
    private CursorView leftCursor, rightCursor;
    private OperationView mOperationView;
    private Point lastLeft = new Point(), lastRight = new Point();
    private boolean leftCursorVisible = true, rightCursorVisible = true;
    private boolean needShowOperationView = false;

    public PromptPopWindow(Context context) {
        super(context);
        leftCursor = new CursorView(context, true);
        rightCursor = new CursorView(context, false);
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.addView(leftCursor);
        frameLayout.addView(rightCursor);
        mOperationView = new OperationView(context);
        mOperationView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mOperationView.setVisibility(View.GONE);
        frameLayout.addView(mOperationView);

        setContentView(frameLayout);
//        setBackgroundDrawable(new ColorDrawable(Color.BLUE));
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setClippingEnabled(false);
        setOutsideTouchable(true);
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                Log.d(TAG, "cursor  window dismiss");
                if (mCursorTouchListener != null)
                    mCursorTouchListener.onCursorDismiss();
                if (mOperationView != null)
                    mOperationView.setVisibility(View.GONE);
            }
        });
        leftCursor.setOnCursorTouchListener(this);
        rightCursor.setOnCursorTouchListener(this);
        frameLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mCursorTouchListener != null)
                    return mCursorTouchListener.onPopLayoutTouch(view, motionEvent);
                return true;
            }
        });
    }

    /**
     * 设置左右游标位置  分别在文字左下角、右下角
     *
     * @param parent
     * @param left
     * @param right
     */
    public void updateCursor(View parent, Point left, Point right, int startLineTopInWindow, Rect visibleRect) {
        Log.d(TAG, "updateCursor:" + left + "  " + right + "  " + startLineTopInWindow);
        if (left.x <= 0)
            left.x = getWidth() / 2;
        if (!isShowing()) {
            showAtLocation(parent, Gravity.NO_GRAVITY, 0, 0);
        }

        if (leftCursorVisible) {
            leftCursor.setVisibility(View.VISIBLE);
            leftCursor.setX(left.x - leftCursor.getFixWidth());
            leftCursor.setY(left.y);
        } else {
            leftCursor.setVisibility(View.GONE);
        }

        if (rightCursorVisible) {
            rightCursor.setVisibility(View.VISIBLE);
            rightCursor.setX(right.x);
            rightCursor.setY(right.y);
        } else {
            rightCursor.setVisibility(View.GONE);
        }


        this.lastLeft = left;
        lastLeft.y = startLineTopInWindow;
        this.lastRight = right;

        if (needShowOperationView) {
            if (visibleRect.isEmpty()) {
                mOperationView.setVisibility(View.GONE);
                return;
            } else mOperationView.setVisibility(View.VISIBLE);
        }

        if (mOperationView != null) {
            mOperationView.update(lastLeft, lastRight);
        }
    }

    public void setCursorVisible(boolean left, boolean visible) {
        if (left) {
            leftCursorVisible = visible;
        } else {
            rightCursorVisible = visible;
        }

    }

    public void showOperation() {
        needShowOperationView = true;
        if (mOperationView != null) {
            mOperationView.setVisibility(View.VISIBLE);
        }

    }

    public void hideOperation() {
        needShowOperationView = false;
        if (mOperationView != null) {
            mOperationView.setVisibility(View.GONE);
        }
    }

    public void hideCursor(){
        if (leftCursor != null)
            leftCursor.setVisibility(View.GONE);
        if (rightCursor != null)
            rightCursor.setVisibility(View.GONE);
    }


    public void setCursorTouchListener(CursorListener listener) {
        mCursorTouchListener = listener;
    }

    public void addOperationClickListener(OperationView.OperationItemClickListener listener) {
        if (mOperationView != null)
            mOperationView.addOperationClickListener(listener);
    }

    public void setOperationItemList(List<OperationItem> list){
        if (mOperationView != null)
            mOperationView.setOperationList(list);
    }

    @Override
    public boolean onCursorTouch(boolean isLeft, View view, MotionEvent event) {
        if (mCursorTouchListener != null)
            return mCursorTouchListener.OnCursorTouch(isLeft, view, event);
        return true;
    }

    public interface CursorListener {
        boolean OnCursorTouch(boolean isLeft, View view, MotionEvent event);

        boolean onPopLayoutTouch(View view, MotionEvent motionEvent);

        void onCursorDismiss();
    }

}
