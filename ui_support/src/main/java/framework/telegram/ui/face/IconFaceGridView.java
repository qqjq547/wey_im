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

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;

import java.util.Arrays;

import framework.telegram.support.tools.ResourceUtils;
import framework.telegram.ui.R;
import framework.telegram.ui.image.AppImageView;
import framework.telegram.ui.utils.ScreenUtils;


/**
 * @author Hieu Rocker (rockerhieu@gmail.com)
 */
public class IconFaceGridView extends FrameLayout {
    private OnIconFaceItemClickedListener mOnIconFaceItemClickedListener;
    private IconFaceItem[] mData;
    private IconFaceAdapter mIconFaceAdapter;
    private int mType;
    private int mRow;
    private int mColumn;
    private RecyclerView mGridView;

    private FloatWindow mFloatWindow = new FloatWindow((Activity)getContext());

    public IconFaceGridView(Context context, IconFaceItem[] emojicons, OnIconFaceItemClickedListener listener, int type, int row, int column) {
        super(context);
        mData = emojicons;
        mOnIconFaceItemClickedListener = listener;
        mType = type;
        mRow = row;
        mColumn = column;
        initView();
    }

    public void refreshData(IconFaceItem[] emojicons) {
        mData = emojicons;
        mIconFaceAdapter.setNewData(Arrays.asList(mData));
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.petface_grid, this);

        mGridView= findViewById(R.id.face_recycler_view);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), mColumn);
        mGridView.setLayoutManager(gridLayoutManager);
        mGridView.setItemAnimator(null);

        mIconFaceAdapter = new IconFaceAdapter(mType, Arrays.asList(mData), new IconFaceAdapter.SizeCallback() {
            @Override
            public int getWidth() {
                return getMeasuredWidth() / mColumn;
            }

            @Override
            public int getHeight() {
                return Math.min(getMeasuredWidth() / mColumn, getMeasuredHeight() / mRow);
            }
        });
        mIconFaceAdapter.setOnItemClickListener((adapter, view, position) -> {
            if (mType == 3 ){
                if (!isLongClick){
                    mOnIconFaceItemClickedListener.onIconFaceItemClicked(mIconFaceAdapter.getItem(position), mType);
                }else {
                    isLongClick = false;
                }
            }else {
                mOnIconFaceItemClickedListener.onIconFaceItemClicked(mIconFaceAdapter.getItem(position), mType);
            }
        });
        mGridView.setAdapter(mIconFaceAdapter);
    }

    public interface OnIconFaceItemClickedListener {
        void onIconFaceItemClicked(IconFaceItem iconFaceItem, int type);
    }

    private View mLastView = null;
    private boolean isLongClick = false;

    public void showBrowseWindow(float x, float y,float height){
        View view = mGridView.findChildViewUnder(x,y);
        if (view != null && !view.equals(mLastView)){
            if (mType == 3 && view instanceof FloatView ){

                AppImageView icon = view.findViewById(R.id.image_view);
                Object object = icon.getTag(R.id.icon_image_path);
                mFloatWindow.dismiss();
                if (object !=null){
                    String str = (String) object ;
                    if (!TextUtils.isEmpty(str) && ResourceUtils.isHttpScheme(str)){
                        mFloatWindow.show(height,str,view);
                    }
                }


            }
            mLastView= view;
        }
        isLongClick = true;
    }

    public void dimissBrowseWindow(){
        if (mFloatWindow!=null){
            mFloatWindow.dismiss();
        }
        mLastView = null;
        //这里要延迟，不然会跟点击混再一起
        new Handler().postDelayed(() -> isLongClick = false, 300);

    }
}
