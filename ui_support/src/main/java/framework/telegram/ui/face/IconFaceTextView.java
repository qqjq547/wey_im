package framework.telegram.ui.face;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatTextView;

import framework.telegram.ui.link.LinkTextView;
import framework.telegram.ui.utils.ScreenUtils;

/**
 * @author Hieu Rocker (rockerhieu@gmail.com).
 */
public class IconFaceTextView extends LinkTextView {
    private float mIconSize;
    private float mIconFaceTextSize;

    public IconFaceTextView(Context context) {
        super(context);
        init(null);
    }

    public IconFaceTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        mIconFaceTextSize = (int) getTextSize();
        mIconSize = getTextSize() + ScreenUtils.dp2px(getContext(), 4);
//        setBackgroundColor(Color.RED);
    }

    @Override
    public void setText(CharSequence text, TextView.BufferType type) {
        if (!TextUtils.isEmpty(text)) {
            SpannableStringBuilder builder = new SpannableStringBuilder(text);
            IconFaceHandler.addIconFaces(getContext(), builder, mIconSize, mIconFaceTextSize, 0, text.toString().length());
            text = builder;
        }

        super.setText(text, type);
    }

    public void setIconSize(int pixels) {
        mIconSize = pixels;
        super.setText(getText());
    }
}
