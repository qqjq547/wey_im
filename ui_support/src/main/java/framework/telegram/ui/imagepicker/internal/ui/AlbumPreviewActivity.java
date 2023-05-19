package framework.telegram.ui.imagepicker.internal.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import framework.telegram.support.tools.language.LocalManageUtil;
import framework.telegram.ui.imagepicker.internal.entity.Album;
import framework.telegram.ui.imagepicker.internal.entity.Item;
import framework.telegram.ui.imagepicker.internal.entity.SelectionSpec;
import framework.telegram.ui.imagepicker.internal.model.AlbumMediaCollection;
import framework.telegram.ui.imagepicker.internal.ui.adapter.PreviewPagerAdapter;

public class AlbumPreviewActivity extends BasePreviewActivity implements
        AlbumMediaCollection.AlbumMediaCallbacks {

    public static final String EXTRA_ALBUM = "extra_album";
    public static final String EXTRA_ITEM = "extra_item";

    private AlbumMediaCollection mCollection = new AlbumMediaCollection();

    private Album mAlbum;

    private boolean mIsAlreadySetPosition;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SelectionSpec.getInstance().hasInited) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        mCollection.onCreate(this, this);
        mAlbum = getIntent().getParcelableExtra(EXTRA_ALBUM);
        mCollection.load(mAlbum);

        Item item = getIntent().getParcelableExtra(EXTRA_ITEM);
        if (mSpec.countable) {
            mCheckView.setCheckedNum(mSelectedCollection.checkedNumOf(item));
        } else {
            mCheckView.setChecked(mSelectedCollection.isSelected(item));
        }
        updateSize(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCollection.onDestroy();
    }

    @Override
    public void onAlbumMediaLoad(Cursor cursor) {
        List<Item> items = new ArrayList<>();
        while (cursor.moveToNext()) {
            Item item = Item.valueOf(cursor);
            items.add(item);
        }
//        cursor.close();

        if (items.isEmpty()) {
            return;
        }

        PreviewPagerAdapter adapter = (PreviewPagerAdapter) mPager.getAdapter();
        adapter.addAll(items);
        adapter.notifyDataSetChanged();
        if (!mIsAlreadySetPosition) {
            //onAlbumMediaLoad is called many times..
            mIsAlreadySetPosition = true;
            Item selected = getIntent().getParcelableExtra(EXTRA_ITEM);
            int selectedIndex = items.indexOf(selected);
            mPager.setCurrentItem(selectedIndex, false);
            mPreviousPos = selectedIndex;
            selectRecyclerItem(mAdapter.getMediaItem(selectedIndex));
        }
    }

    @Override
    public void onAlbumMediaReset() {

    }

    @Override
    public void onBottomItemClick(int position, Item target) {
        PreviewPagerAdapter adapter = (PreviewPagerAdapter) mPager.getAdapter();
        List<Item> list = adapter.getAll();
        for (int i = 0; i < list.size(); i++){
           if (list.get(i).equals(target)){
               mPager.setCurrentItem(i);
           }
        }
    }
}
