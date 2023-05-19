package framework.telegram.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import framework.telegram.ui.R;
import framework.telegram.ui.tools.BitmapUtils;
import framework.telegram.ui.utils.ScreenUtils;


/**
 * Created by hyf on 15/9/13.
 */
public class LocationMarkerView extends LinearLayout {

    private TextView mTitleView, mAddressView;
    private LinearLayout mRootLayout;

    public LocationMarkerView(Context context) {
        super(context);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.localtion_marker, this);
        mRootLayout = (LinearLayout) findViewById(R.id.root_layout);
        mTitleView = (TextView) findViewById(R.id.title);
        mAddressView = (TextView) findViewById(R.id.address);
    }

    public void setAddress(String address) {
        if (address == null) {
            address = "";
        }

        String mAddress = address;
        mAddressView.setText(mAddress);
    }

    public void setTitle(String title) {
        if (title == null) {
            title = "";
        }

        String mTitle = title;
        mTitleView.setText(mTitle);
        mTitleView.setVisibility(View.VISIBLE);

        int padding = ScreenUtils.dp2px(getContext(), 10);
        mAddressView.setPadding(padding, 0, padding, padding);
    }

    public Bitmap getBitmap() {
        int widthMeasure = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightMeasure = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        mRootLayout.measure(widthMeasure, heightMeasure);
        int height = mRootLayout.getMeasuredHeight();
        int width = mRootLayout.getMeasuredWidth();

        return BitmapUtils.makeViewBitmap(this, width, height);
    }
}
