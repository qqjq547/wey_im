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

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseSectionQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

import framework.telegram.ui.R;
import framework.telegram.ui.emoji.EmojiTextView;
import framework.telegram.ui.image.AppImageView;
import framework.telegram.ui.utils.ScreenUtils;

/**
 * @author Hieu Rocker (rockerhieu@gmail.com)
 */
class IconFaceAdapter extends BaseSectionQuickAdapter<IconFaceItem, BaseViewHolder> {

    private SizeCallback mSizeCallback;

    private int mType;

    public IconFaceAdapter(int type, List data, SizeCallback callback) {
        super(type == 3 ? R.layout.petface_item_dynamic_view : R.layout.petface_item, R.layout.petface_item_head, data);
        mSizeCallback = callback;
        mType = type;
    }

    @Override
    protected void convertHead(BaseViewHolder helper, IconFaceItem item) {
        TextView icon = (TextView) helper.itemView;
        //这是给EmojiTextView
        if (TextUtils.isEmpty(item.name)) {
            ViewGroup.LayoutParams params = icon.getLayoutParams();
            params.height = 0;
            icon.setLayoutParams(params);

            icon.setVisibility(View.GONE);
        } else {
            ViewGroup.LayoutParams params = icon.getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            icon.setLayoutParams(params);

            icon.setText(item.name);
            icon.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void convert(BaseViewHolder helper, IconFaceItem item) {
        if (mSizeCallback != null) {
            helper.itemView.setLayoutParams(new RecyclerView.LayoutParams(mSizeCallback.getWidth(), mSizeCallback.getHeight()));
        }

        if (mType == 3) {
            AppImageView icon = helper.itemView.findViewById(R.id.image_view);
            icon.setImageURI(item.path);
            icon.setTag(R.id.icon_image_path,item.path);
        } else {
            EmojiTextView icon = (EmojiTextView) helper.itemView;
            //这是给EmojiTextView
            icon.setEmojiSize(ScreenUtils.dp2px(helper.itemView.getContext(), 31));
            //IconFaceTextView
            icon.setIconSize(ScreenUtils.dp2px(helper.itemView.getContext(), 32));

            if (item.resId > 0) {
                icon.setText(item.getSpan(icon.getContext(), ScreenUtils.dp2px(helper.itemView.getContext(), 31)));
            } else {
                icon.setText(item.name);
            }
        }
    }

    public interface SizeCallback {
        int getWidth();

        int getHeight();
    }
}