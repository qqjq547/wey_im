package framework.telegram.ui.selectText;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.TintContextWrapper;

import java.util.ArrayList;
import java.util.List;

import framework.telegram.support.BaseApp;
import framework.telegram.ui.R;
import framework.telegram.ui.menu.Display;

/**
 * Created by wangyang53 on 2018/3/28.
 */

public class OperationView extends LinearLayout {
    private final String TAG = OperationView.class.getSimpleName();
    private Context mContext;
    private LinearLayout ll_list;
    //    private View arrow;
    private List<OperationItem> operationList = new ArrayList<>();
    private OperationItemClickListener listener = null;
    private int screenHeight, screenWidth;
    private final int MIN_MARGIN_LEFT_RIGHT = 20;
    private final int MIN_MARGIN_TOP = CursorView.getFixHeight();
    private @DrawableRes int bgResource = R.drawable.bg_operation;
    private int textViewColor = Color.BLACK;
    private int dividerColor = Color.WHITE;
    private int mBackgroundDrawable = R.drawable.bg_shadow;

    private int mMeasuredWidth = 0;
    private int mMeasuredHeight= 0;
    private int paddingRight = Display.dip2px(BaseApp.app, 12);
    private int paddingTop = Display.dip2px(BaseApp.app, 9);

    private HorizontalScrollView scrollView;

    public OperationView(Context context) {
        super(context);
        init(context);
    }

    public OperationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public OperationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        setOrientation(VERTICAL);

        if (operationList.size() == 0){
            OperationItem item1 = new OperationItem();
            item1.action = OperationItem.ACTION_COPY;
            item1.name = mContext.getString(R.string.copy);

            OperationItem item2 = new OperationItem();
            item2.action = OperationItem.ACTION_SELECT_ALL;
            item2.name =  mContext.getString(R.string.all);

            OperationItem item3 = new OperationItem();
            item3.action = OperationItem.ACTION_FORWARD;
            item3.name = mContext.getString(R.string.forward);


//            OperationItem item4 = new OperationItem();
//            item4.action = OperationItem.ACTION_CANCEL;
//            item4.name = mContext.getString(R.string.cancel);

            operationList.add(item1);
            operationList.add(item2);
            operationList.add(item3);
        }



//        setBackgroundColor(Color.TRANSPARENT);
        setBackgroundResource(mBackgroundDrawable);
//        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        ll_list = new LinearLayout(context);
        ll_list.setOrientation(LinearLayout.HORIZONTAL);
        ll_list.setBackgroundResource(bgResource);
        int paddingTop = Display.dip2px(context, 12);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll_list.setLayoutParams(lp);
        for (int i = 0; i < operationList.size(); i++) {
            final OperationItem item = operationList.get(i);
            TextView textView = new TextView(context);
            textView.setTextSize(15);
            MarginLayoutParams layoutParams = new MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textView.setLayoutParams(layoutParams);
            textView.setPadding(paddingRight, paddingTop, paddingRight, paddingTop);
            textView.setTextColor(textViewColor);
            textView.setText(item.name);


            textView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onOperationClick(item);
                    }
                }
            });
            ll_list.addView(textView);
        }


//        arrow = new View(context);
//        arrow.setBackgroundResource(R.drawable.triangle_down);
//        RelativeLayout.LayoutParams arrowLp = new RelativeLayout.LayoutParams(17, 17);
//        arrow.setLayoutParams(arrowLp);

        scrollView = new HorizontalScrollView(context);

        scrollView.setScrollBarStyle(SCROLLBARS_INSIDE_INSET);

        scrollView.addView(ll_list);

        addView(scrollView, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));




        Activity activity = null;
        if (mContext instanceof TintContextWrapper){
            activity = (Activity)((TintContextWrapper) mContext).getBaseContext();
        }else {
            activity = (Activity) mContext;
        }

        WindowManager manager = activity.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        screenWidth = outMetrics.widthPixels;
        screenHeight = outMetrics.heightPixels;
    }

    public void reinit(Context context){
        scrollView.removeView(ll_list);
        ll_list = new LinearLayout(context);
        ll_list.setOrientation(LinearLayout.HORIZONTAL);
        ll_list.setBackgroundResource(bgResource);

//        ll_list.setPadding(paddingTop, paddingTop, paddingTop, paddingTop);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll_list.setLayoutParams(lp);
        for (int i = 0; i < operationList.size(); i++) {
            final OperationItem item = operationList.get(i);
            TextView textView = new TextView(context);
            MarginLayoutParams layoutParams = new MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textView.setLayoutParams(layoutParams);
            textView.setTextColor(textViewColor);
            textView.setPadding(paddingRight, paddingTop, paddingRight, paddingTop);
            textView.setText(item.name);


            textView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onOperationClick(item);
                    }
                }
            });
            ll_list.addView(textView);
        }
        scrollView.addView(ll_list);
    }

    public void update(Point left, Point right) {
        if (mMeasuredWidth == 0 || mMeasuredHeight == 0){
            measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mMeasuredWidth = getMeasuredWidth();
            mMeasuredHeight = getMeasuredHeight();
        }

        Log.d(TAG, "update:" + left + "  " + right);
        int centerArrowX = (left.x + right.x) / 2;
        if (centerArrowX < left.x)
            centerArrowX = left.x;
        int x, y;
        x = centerArrowX - 209 / 2;
        if (x + mMeasuredWidth > screenWidth - MIN_MARGIN_LEFT_RIGHT) {
           // x = screenWidth - MIN_MARGIN_LEFT_RIGHT - mMeasuredWidth;
            x = MIN_MARGIN_LEFT_RIGHT;
        } else if (x < MIN_MARGIN_LEFT_RIGHT) {
            x = MIN_MARGIN_LEFT_RIGHT;
        }

        boolean down = true;
        y = left.y - MIN_MARGIN_TOP - mMeasuredHeight;
        if (y < screenHeight / 5) {
            y = right.y + MIN_MARGIN_TOP;
            down = false;
        }
        if (y > screenHeight / 5 * 4) {
            y = screenHeight / 2;
        }

        setX(x);
        setY(y);
//        setArrow(down, (int) (centerArrowX - getX()));
    }

//    public void setArrow(boolean down, int x) {
//        if (down) {
//            arrow.setBackgroundResource(R.drawable.triangle_down);
//            removeView(arrow);
//            addView(arrow);
//        } else {
//            arrow.setBackgroundResource(R.drawable.triangle_up);
//            removeView(arrow);
//            addView(arrow, 0);
//        }
//
//        arrow.setX(x);
//        invalidate();
//    }


    public void addOperationClickListener(OperationItemClickListener listener) {
        this.listener = listener;
    }

    public void setOperationList(List<OperationItem> list){
        this.operationList = list;
        reinit(mContext);
    }

    public void setTextViewColor(int color){
        this.textViewColor = color;
    }

    public void setBgResource(@DrawableRes int res){
        this.bgResource = res;
    }

    public void setDividerColor(int color){
        this.dividerColor = color;
    }

    public interface OperationItemClickListener {
        void onOperationClick(OperationItem item);
    }
}
