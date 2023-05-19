package framework.telegram.ui.link;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Patterns;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkTextView extends AppCompatTextView {

    private boolean isFindUrl = false;

    private int mUrlColor = Color.parseColor("#178aff");

    public LinkTextView(Context context) {
        super(context);
    }

    public LinkTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LinkTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (isFindUrl && !TextUtils.isEmpty(text) && getContext() instanceof AppCompatActivity) {
            boolean finded = false;
            SpannableString spannableString = new SpannableString(text);
            Matcher urlMatcher = Pattern.compile("([hH][tT]{2}[pP]://|[hH][tT]{2}[pP][sS]://|[wW]{3}.|[wW][aA][pP].|[fF][tT][pP].|[fF][iI][lL][eE].)[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]").matcher(text);
            while (urlMatcher.find()) {
                String url = urlMatcher.group();
                int start = urlMatcher.start();
                int end = urlMatcher.end();
                spannableString.setSpan(new AutolinkSpan((AppCompatActivity) getContext(), mUrlColor, url), start, end, 0);
                finded = true;
            }

            super.setText(spannableString, type);

            if (finded) {
                super.setMovementMethod(new LinkMovementMethod());
            }
        } else {
            super.setText(text, type);
        }
    }

    public void setUrlColor(int color) {
        mUrlColor = color;
    }

    public void setFindUrl(boolean findUrl) {
        isFindUrl = findUrl;
    }

    public boolean isFindUrl() {
        return isFindUrl;
    }
}
