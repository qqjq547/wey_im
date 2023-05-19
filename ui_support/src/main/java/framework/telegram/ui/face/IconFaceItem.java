package framework.telegram.ui.face;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ImageSpan;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.chad.library.adapter.base.entity.SectionEntity;

/**
 * Created by hyf on 15/9/15.
 */
public class IconFaceItem extends SectionEntity {

    public long id;
    public String name;
    public int resId;
    public String path;
    public int width;
    public int height;

    public IconFaceItem(String name, int resId) {
        super(false, "");
        this.name = name;
        this.resId = resId;
    }

    public IconFaceItem(long id, String name, String path, int width, int height) {
        super(false, "");
        this.id = id;
        this.name = name;
        this.path = path;
        this.width = width;
        this.height = height;
    }

    public IconFaceItem(String name) {
        super(true, name);
        this.name = name;
    }

    public CharSequence getSpan(Context context, int size) {
        Drawable drawable = ContextCompat.getDrawable(context, resId);
        drawable.setBounds(0, 0, size, size);
        ImageSpan imgSpan = new ImageSpan(drawable);
        SpannableString spanString = new SpannableString("icon");
        spanString.setSpan(imgSpan, 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spanString;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof IconFaceItem) {
            if (resId > 0) {
                return (resId == ((IconFaceItem) obj).resId) && name.equals(((IconFaceItem) obj).name);
            } else if (!TextUtils.isEmpty(path)) {
                return (path.equals(((IconFaceItem) obj).path) && name.equals(((IconFaceItem) obj).name));
            } else if (!TextUtils.isEmpty(name)) {
                return (name.equals(((IconFaceItem) obj).name) );
            }
        }
        return false;
    }
}