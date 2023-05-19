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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TableLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.facebook.common.util.UriUtil;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import framework.telegram.support.BaseApp;
import framework.telegram.support.account.AccountManager;
import framework.telegram.support.system.gson.GsonInstanceCreater;
import framework.telegram.support.system.storage.sp.core.IdeasPreference;
import framework.telegram.ui.R;
import framework.telegram.ui.emoji.EmojiManager;
import framework.telegram.ui.emoji.base.Emoji;
import framework.telegram.ui.emoji.base.EmojiCategory;
import framework.telegram.ui.face.dynamic.DynamicFaceBean;
import framework.telegram.ui.utils.ScreenUtils;
import framework.telegram.ui.utils.UriUtils;
import framework.telegram.ui.widget.indicator.IconPageIndicator;
import framework.telegram.ui.widget.indicator.IconPagerAdapter;

/**
 * @author Hieu Rocker (rockerhieu@gmail.com).
 */
public class IconFacePageView extends FrameLayout {
    private OnIconFaceListener mOnIconFaceListener;
    private IconFacePagerAdapter mIconFacePagerAdapter;

    private FaceSpData mFaceSpData = null;
    private FaceClickCountPreferences mPreferences;
    private List<IconFaceGridView> allIconFaceGridViews = new ArrayList<>();

    private TabLayout mTabLayout;
    private ViewPager mIconFacesPager;
    private int mCurPager = 0;

    public IconFacePageView(@NonNull Context context) {
        super(context);
        initData();
        initView();
    }

    public IconFacePageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initData();
        initView();
    }

    public IconFacePageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData();
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.petfaces, this);

        mTabLayout = findViewById(R.id.tab_layout);
        initTablayout();

        ImageView imageView = findViewById(R.id.icon_delete);
        mIconFacesPager = findViewById(R.id.view_pager_face_pager);

        mIconFacePagerAdapter = new IconFacePagerAdapter(allIconFaceGridViews);

        mIconFacesPager.setAdapter(mIconFacePagerAdapter);
        mIconFacesPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mTabLayout.getTabAt(position).select();
                mCurPager= position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        imageView.setOnClickListener(v -> mOnIconFaceListener.onIconFaceBackspaceClicked());
    }

    private void initTablayout() {
        TabLayout.Tab tab1 = mTabLayout.newTab().setCustomView(R.layout.petfaces_item_tab);
        tab1.getCustomView().findViewById(R.id.image_view).setBackground(ContextCompat.getDrawable(this.getContext(), R.drawable.pet_emoji_0));
        tab1.getCustomView().setBackgroundColor(ContextCompat.getColor(getContext(), R.color.f8f8f8));
        mTabLayout.addTab(tab1);

        TabLayout.Tab tab2 = mTabLayout.newTab().setCustomView(R.layout.petfaces_item_tab);
        tab2.getCustomView().findViewById(R.id.image_view).setBackground(ContextCompat.getDrawable(this.getContext(), R.drawable.emoji_1));
        mTabLayout.addTab(tab2);

        TabLayout.Tab tab3 = mTabLayout.newTab().setCustomView(R.layout.petfaces_item_tab);
        tab3.getCustomView().findViewById(R.id.image_view).setBackground(ContextCompat.getDrawable(this.getContext(), R.drawable.ic_dynamic_face));
        mTabLayout.addTab(tab3);

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            //选中tab
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //选中tab时,TabLayout与ViewPager联动
                mIconFacesPager.setCurrentItem(tab.getPosition());
                for (int i = 0; i < mTabLayout.getTabCount(); i++) {
                    TabLayout.Tab tab1 = mTabLayout.getTabAt(i);
                    if (tab.getPosition() == i) {
                        tab1.getCustomView().setBackgroundColor(ContextCompat.getColor(getContext(), R.color.f8f8f8));
                    } else {
                        tab1.getCustomView().setBackgroundColor(ContextCompat.getColor(getContext(), R.color.white));
                    }
                }
            }

            //未选择tab
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            //重复选中tab
            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

    }

    private void initData() {
        initLongClick();

        mPreferences = new IdeasPreference().create(BaseApp.app, FaceClickCountPreferences.class, AccountManager.INSTANCE.getLoginAccountUUid());
        String dataResult = mPreferences.getCount("");
        if (!TextUtils.isEmpty(dataResult)) {
            mFaceSpData = GsonInstanceCreater.INSTANCE.getDefaultGson().fromJson(dataResult, FaceSpData.class);
        }
        if (mFaceSpData == null) {
            mFaceSpData = new FaceSpData(new ArrayList<>(), new ArrayList<>());
        }

        //自定义本地表情
        List<IconFaceItem> allIconFaces = new ArrayList<>(IconFaceHandler.DATA_SORT);
        List<IconFaceItem> faceIcons1 = new ArrayList<>();

        List data1 = mFaceSpData.getmFaceList1();
        if (data1.size() > 0) {
            faceIcons1.add(new IconFaceItem(getContext().getString(R.string.string_icon_recent)));
            faceIcons1.addAll(data1);
        }

        faceIcons1.add(new IconFaceItem(getContext().getString(R.string.string_icon_all)));
        faceIcons1.addAll(allIconFaces);

        allIconFaceGridViews.add(new IconFaceGridView(getContext(), faceIcons1.toArray(new IconFaceItem[0]), mOnIconFaceItemClickedListener, 1, 5, 7));

        //emoji表情
        EmojiCategory category = EmojiManager.getInstance().getCategories()[0];
        Emoji[] emojis = category.getEmojis();
        List<IconFaceItem> faceIcons2 = new ArrayList<>();

        List data2 = mFaceSpData.getmFaceList2();
        if (data2.size() > 0) {
            faceIcons2.add(new IconFaceItem(getContext().getString(R.string.string_icon_recent)));
            faceIcons2.addAll(data2);
        }

        faceIcons2.add(new IconFaceItem(getContext().getString(R.string.string_icon_all)));
        for (Emoji emoji : emojis) {
            faceIcons2.add(new IconFaceItem(emoji.getUnicode(), emoji.getResource()));
        }
        allIconFaceGridViews.add(new IconFaceGridView(getContext(), faceIcons2.toArray(new IconFaceItem[0]), mOnIconFaceItemClickedListener, 2, 5, 7));

        //动态表情
        List<IconFaceItem> faceIcons3 = new ArrayList<>();
        faceIcons3.add(new IconFaceItem(""));
        faceIcons3.add(new IconFaceItem(0, getContext().getString(R.string.append), UriUtil.getUriForResourceId(R.drawable.ic_add_dynamic_face).toString(), 0, 0));
        allIconFaceGridViews.add(new IconFaceGridView(getContext(), faceIcons3.toArray(new IconFaceItem[0]), mOnIconFaceItemClickedListener, 3, 2, 4));
    }

    public void addDynamicFaceIcons(List<DynamicFaceBean> faces) {
        List<IconFaceItem> faceIcons = new ArrayList<>();
        faceIcons.add(new IconFaceItem(0, getContext().getString(R.string.append), UriUtil.getUriForResourceId(R.drawable.ic_add_dynamic_face).toString(), 0, 0));
        for (int i = 0; i < faces.size(); i++) {
            DynamicFaceBean face = faces.get(i);
            faceIcons.add(new IconFaceItem(face.id, face.name, face.path, face.width, face.height));
        }
        allIconFaceGridViews.get(2).refreshData(faceIcons.toArray(new IconFaceItem[0]));
    }

    public void setOnIconFaceListener(OnIconFaceListener listener) {
        mOnIconFaceListener = listener;
    }

    public static void input(EditText editText, IconFaceItem iconFaceItem) {
        if (editText == null || iconFaceItem == null) {
            return;
        }

        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start < 0) {
            editText.append(iconFaceItem.name);
        } else {
            editText.getText().replace(Math.min(start, end), Math.max(start, end), iconFaceItem.name, 0, iconFaceItem.name.length());
        }
    }

    public static void backspace(EditText editText) {
        KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
        editText.dispatchKeyEvent(event);
    }

    private IconFaceGridView.OnIconFaceItemClickedListener mOnIconFaceItemClickedListener = new IconFaceGridView.OnIconFaceItemClickedListener() {

        @Override
        public void onIconFaceItemClicked(IconFaceItem iconFaceItem, int type) {
            if (iconFaceItem == null) {
                return;
            }

            if (type == 3) {
                mOnIconFaceListener.onIconFaceClicked(iconFaceItem, type);
            } else {
                if (IconFaceHandler.DELETE.name.equals(iconFaceItem.name)) {
                    if (mOnIconFaceListener != null) {
                        mOnIconFaceListener.onIconFaceBackspaceClicked();
                    }
                } else if (!getContext().getString(R.string.string_icon_recent).equals(iconFaceItem.name)
                        && !getContext().getString(R.string.string_icon_all).equals(iconFaceItem.name)) {
                    if (mOnIconFaceListener != null) {
                        mOnIconFaceListener.onIconFaceClicked(iconFaceItem, type);
                        saveSpData(iconFaceItem, type);
                    }
                }
            }
        }
    };

    private void saveSpData(IconFaceItem iconFaceItem, int type) {
        if (type == 1) {
            List list = mFaceSpData.getmFaceList1();
            list.remove(iconFaceItem);
            list.add(0, iconFaceItem);
            list = list.subList(0, list.size() >= 14 ? 14 : list.size());
            mFaceSpData.setmFaceList1(list);
        } else if (type == 2) {
            List list = mFaceSpData.getmFaceList2();
            list.remove(iconFaceItem);
            list.add(0, iconFaceItem);
            list = list.subList(0, list.size() >= 14 ? 14 : list.size());
            mFaceSpData.setmFaceList2(list);
        }
        String data = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(mFaceSpData);
        mPreferences.puCount(data);
    }

    private static class IconFacePagerAdapter extends PagerAdapter implements IconPagerAdapter {
        private List<IconFaceGridView> gridviews;

        public IconFacePagerAdapter(List<IconFaceGridView> gridviews) {
            this.gridviews = gridviews;
        }

        @Override
        public int getIconResId(int index) {
            return R.drawable.select_emoji;
        }

        @Override
        public int getCount() {
            return gridviews == null ? 0 : gridviews.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public void destroyItem(ViewGroup container, int position,
                                Object object) {
            position = position % gridviews.size();
            container.removeView(gridviews.get(position));
        }

        @Override
        public Object instantiateItem(ViewGroup view, int position) {
            position = position % gridviews.size();
            view.addView(gridviews.get(position));
            return gridviews.get(position);
        }
    }

    public interface OnIconFaceListener {
        void onIconFaceBackspaceClicked();

        void onIconFaceClicked(IconFaceItem iconFaceItem, int type);
    }

    Long isTouchDownTime = 0L;
    float x = 0 ;
    float y = 0 ;
    float rawY = 0 ;

    long windowHeight = ScreenUtils.getScreenHeight(BaseApp.app);

    private Runnable mLongPressRunnable;
    private int mCounter;
    private boolean mIsDown = false;

    private void initLongClick(){
        mLongPressRunnable = () -> {
            mCounter--;
            //计数器大于0，说明当前执行的Runnable不是最后一次down产生的。
            if(mCounter == 0 && mIsDown  ) {
                mIconFacePagerAdapter.gridviews.get(mCurPager).showBrowseWindow(x,y,rawY - y);
            }
        };
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()){
            case MotionEvent.ACTION_UP:{
                mIconFacePagerAdapter.gridviews.get(mCurPager).dimissBrowseWindow();
                mIsDown = false;
                break;
            }
            case MotionEvent.ACTION_DOWN:{
                x = ev.getX();
                y = ev.getY();
                rawY = ev.getRawY();
                mIsDown = true;
                isTouchDownTime = System.currentTimeMillis();
                mCounter++;
                postDelayed(mLongPressRunnable, ViewConfiguration.getLongPressTimeout());
                break;
            }
            case MotionEvent.ACTION_MOVE:{
                if (System.currentTimeMillis() - isTouchDownTime > ViewConfiguration.getLongPressTimeout()){
                    mIconFacePagerAdapter.gridviews.get(mCurPager).showBrowseWindow(ev.getX(),ev.getY(),ev.getRawY() - ev.getY());
                    return true;
                }
                break;
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}
