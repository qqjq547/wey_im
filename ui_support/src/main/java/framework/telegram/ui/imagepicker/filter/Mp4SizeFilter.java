package framework.telegram.ui.imagepicker.filter;

import android.content.Context;

import java.util.HashSet;
import java.util.Set;

import framework.telegram.ui.imagepicker.MimeType;
import framework.telegram.ui.imagepicker.internal.entity.IncapableCause;
import framework.telegram.ui.imagepicker.internal.entity.Item;

public class Mp4SizeFilter extends Filter {

    private int mMaxSize;

    private String mErrorTitle;

    private String mErrorMsg;

    public Mp4SizeFilter(int maxSizeInBytes, String errorTitle, String errorMsg) {
        mMaxSize = maxSizeInBytes;
        mErrorMsg = errorMsg;
        mErrorTitle = errorTitle;
    }

    @Override
    public Set<MimeType> constraintTypes() {
        return new HashSet<MimeType>() {{
            add(MimeType.MP4);
        }};
    }

    @Override
    public IncapableCause filter(Context context, Item item) {
        if (!needFiltering(context, item))
            return null;

        if (item.size > mMaxSize) {
            return new IncapableCause(IncapableCause.DIALOG, mErrorTitle, mErrorMsg);
        }

        return null;
    }

}
