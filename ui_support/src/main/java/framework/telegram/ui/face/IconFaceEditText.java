/*
 * Copyright 2014 Hieu Rocker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package framework.telegram.ui.face;

import android.content.Context;
import android.text.SpannableString;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

import framework.telegram.ui.utils.ScreenUtils;

/**
 * @author Hieu Rocker (rockerhieu@gmail.com).
 */
public class IconFaceEditText extends AppCompatEditText {
    private float mIconFaceSize;
    private float mIconFaceTextSize;

    public IconFaceEditText(Context context) {
        super(context);
        mIconFaceSize = (int) getTextSize();
        mIconFaceTextSize = (int) getTextSize();
    }

    public IconFaceEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        mIconFaceTextSize = (int) getTextSize();
        mIconFaceSize = getTextSize() + ScreenUtils.dp2px(getContext(), 4);
    }

    public void setHintText(CharSequence hint) {
        SpannableString spannableString = new SpannableString(hint);
        IconFaceHandler.addIconFaces(getContext(), spannableString, mIconFaceSize, mIconFaceTextSize, 0, hint.length());
        setHint(spannableString);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        IconFaceHandler.addIconFaces(getContext(), getText(), mIconFaceSize, mIconFaceTextSize, start, lengthAfter);
    }
}
