package framework.telegram.ui.text;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;

import framework.telegram.ui.emoji.EmojiTextView;

public class AppTextView extends EmojiTextView {
    public AppTextView(Context context) {
        super(context);
//        this.setBackgroundColor(Color.RED);
    }

    public AppTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}
