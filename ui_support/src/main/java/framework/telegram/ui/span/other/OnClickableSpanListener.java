package framework.telegram.ui.span.other;

import android.widget.TextView;

import framework.telegram.ui.span.customspan.CustomClickableSpan;

/**
 * ClickableSpan Listener
 * Created by iWgang on 15/12/3.
 * https://github.com/iwgang/SimplifySpan
 */
public interface OnClickableSpanListener {

    void onClick(TextView tv, CustomClickableSpan clickableSpan);

}
