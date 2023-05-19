package framework.telegram.ui.imagepicker.internal.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import java.util.List;

import framework.telegram.support.BaseActivity;
import framework.telegram.ui.R;
import framework.telegram.ui.dialog.AppDialog;
import framework.telegram.ui.imagepicker.internal.entity.IncapableCause;
import framework.telegram.ui.imagepicker.internal.entity.Item;
import framework.telegram.ui.imagepicker.internal.entity.SelectionSpec;
import framework.telegram.ui.imagepicker.internal.model.SelectedItemCollection;
import framework.telegram.ui.imagepicker.internal.ui.adapter.BottomPreviewAdapter;
import framework.telegram.ui.imagepicker.internal.ui.adapter.PreviewPagerAdapter;
import framework.telegram.ui.imagepicker.internal.ui.widget.CheckRadioView;
import framework.telegram.ui.imagepicker.internal.ui.widget.CheckView;
import framework.telegram.ui.imagepicker.internal.utils.PhotoMetadataUtils;
import framework.telegram.ui.imagepicker.internal.utils.Platform;

public abstract class BasePreviewActivity extends BaseActivity implements View.OnClickListener,
        ViewPager.OnPageChangeListener, BottomPreviewAdapter.onBottomItemClickListener<Item> {

    public static final String EXTRA_DEFAULT_BUNDLE = "extra_default_bundle";
    public static final String EXTRA_RESULT_BUNDLE = "extra_result_bundle";
    public static final String EXTRA_RESULT_APPLY = "extra_result_apply";
    public static final String EXTRA_RESULT_ORIGINAL_ENABLE = "extra_result_original_enable";
    public static final String CHECK_STATE = "checkState";

    protected final SelectedItemCollection mSelectedCollection = new SelectedItemCollection(this);
    protected SelectionSpec mSpec;
    protected ViewPager mPager;

    protected PreviewPagerAdapter mAdapter;

    protected RecyclerView mRecyclerView;
    protected BottomPreviewAdapter mBottomAdapter;

    protected CheckView mCheckView;
    protected TextView mButtonBack;
    protected TextView mSize;

    //确定按钮
    private LinearLayout mLlApply;
    private TextView mTvCount;
    private TextView mTvMaxCount;

    protected int mPreviousPos = -1;

    private LinearLayout mOriginalLayout;
    private CheckRadioView mOriginal;
    protected boolean mOriginalEnable;

    private FrameLayout mBottomToolbar;
    private FrameLayout mTopToolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(SelectionSpec.getInstance().themeId);
        super.onCreate(savedInstanceState);
        if (!SelectionSpec.getInstance().hasInited) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        setContentView(R.layout.activity_media_preview);
        if (Platform.hasKitKat()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        mSpec = SelectionSpec.getInstance();
        if (mSpec.needOrientationRestriction()) {
            setRequestedOrientation(mSpec.orientation);
        }

        if (savedInstanceState == null) {
            mSelectedCollection.onCreate(getIntent().getBundleExtra(EXTRA_DEFAULT_BUNDLE));
            mOriginalEnable = getIntent().getBooleanExtra(EXTRA_RESULT_ORIGINAL_ENABLE, false);
        } else {
            mSelectedCollection.onCreate(savedInstanceState);
            mOriginalEnable = savedInstanceState.getBoolean(CHECK_STATE);
        }
        mButtonBack = (TextView) findViewById(R.id.button_back);
        mLlApply = (LinearLayout) findViewById(R.id.ll_apply);
        mSize = (TextView) findViewById(R.id.size);
        mButtonBack.setOnClickListener(this);
        mLlApply.setOnClickListener(this);
        mTvCount = findViewById(R.id.tv_count);
        mTvMaxCount = findViewById(R.id.tv_max_count);
        mTvMaxCount.setText(" / "+String.valueOf(mSpec.maxImageSelectable==0?mSpec.maxSelectable:mSpec.maxImageSelectable));

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.addOnPageChangeListener(this);
        mAdapter = new PreviewPagerAdapter(getSupportFragmentManager(), null);
        mPager.setAdapter(mAdapter);
        mCheckView = (CheckView) findViewById(R.id.check_view);
        mCheckView.setCountable(mSpec.countable);
        mBottomToolbar = findViewById(R.id.bottom_toolbar);
        mTopToolbar = findViewById(R.id.top_toolbar);

        if (mSpec.countable) {
            mBottomAdapter = new BottomPreviewAdapter(mSelectedCollection.asList(), this);
            mRecyclerView = findViewById(R.id.recycler_view_had_been_chosen);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            mRecyclerView.setAdapter(mBottomAdapter);
        }
        mCheckView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Item item = mAdapter.getMediaItem(mPager.getCurrentItem());
                if (mSelectedCollection.isSelected(item)) {
                    mSelectedCollection.remove(item);
                    if (mSpec.countable) {
                        mBottomAdapter.remove(item);
                        mCheckView.setCheckedNum(CheckView.UNCHECKED);
                    } else {
                        mCheckView.setChecked(false);
                    }
                } else {
                    if (assertAddSelection(item)) {
                        mSelectedCollection.add(item);
                        if (mSpec.countable) {
                            mBottomAdapter.add(item);
                            mCheckView.setCheckedNum(mSelectedCollection.checkedNumOf(item));
                        } else {
                            mCheckView.setChecked(true);
                        }
                    }
                }
                updateApplyButton();

                if (mSpec.onSelectedListener != null) {
                    mSpec.onSelectedListener.onSelected(
                            mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString());
                }
                if (mSpec.countable)
                    mBottomAdapter.notifyDataSetChanged();
            }
        });


        mOriginalLayout = findViewById(R.id.originalLayout);
        mOriginal = findViewById(R.id.original);
        mOriginalLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int count = countOverMaxSize();
                if (count > 0) {
                    AppDialog.Companion.show(BasePreviewActivity.this, BasePreviewActivity.this, materialDialog -> {
                        materialDialog.title(null, "");
                        materialDialog.message(null, getString(R.string.error_over_original_count, count, mSpec.originalMaxSize), null);
                        return null;
                    });
                    return;
                }

                mOriginalEnable = !mOriginalEnable;
                mOriginal.setChecked(mOriginalEnable);
                if (!mOriginalEnable) {
                    mOriginal.setColor(Color.WHITE);
                }


                if (mSpec.onCheckedListener != null) {
                    mSpec.onCheckedListener.onCheck(mOriginalEnable);
                }
            }
        });

        updateApplyButton();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mSelectedCollection.onSaveInstanceState(outState);
        outState.putBoolean("checkState", mOriginalEnable);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        sendBackResult(false);
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_back) {
            onBackPressed();
        } else if (v.getId() == R.id.ll_apply) {
            sendBackResult(true);
            finish();
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        PreviewPagerAdapter adapter = (PreviewPagerAdapter) mPager.getAdapter();
        if (mPreviousPos != -1 && mPreviousPos != position) {
            ((PreviewItemFragment) adapter.instantiateItem(mPager, mPreviousPos)).resetView();

            Item item = adapter.getMediaItem(position);
            if (mSpec.countable) {
                int checkedNum = mSelectedCollection.checkedNumOf(item);
                mCheckView.setCheckedNum(checkedNum);
                if (checkedNum > 0) {
                    mCheckView.setEnabled(true);
                } else {
                    mCheckView.setEnabled(!mSelectedCollection.maxSelectableReached(item));
                }
            } else {
                boolean checked = mSelectedCollection.isSelected(item);
                mCheckView.setChecked(checked);
                if (checked) {
                    mCheckView.setEnabled(true);
                } else {
                    mCheckView.setEnabled(!mSelectedCollection.maxSelectableReached(item));
                }
            }
            updateSize(item);
        }
        selectRecyclerItem(adapter.getMediaItem(position));
        mPreviousPos = position;


    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private void updateApplyButton() {
        int selectedCount = mSelectedCollection.count();
        if (selectedCount == 0) {
            mLlApply.setEnabled(false);
            mLlApply.setBackgroundResource(R.drawable.bus_corners_d4d6d9_trans_6_0);
        } else if (selectedCount == 1 && mSpec.singleSelectionModeEnabled()) {
            mLlApply.setEnabled(true);
            mLlApply.setBackgroundResource(R.drawable.bus_corners_178aff_trans_6_0);
        } else {
            mLlApply.setEnabled(true);
            mLlApply.setBackgroundResource(R.drawable.bus_corners_178aff_trans_6_0);
            mTvCount.setText(String.valueOf(selectedCount));
        }

        if (mSpec.originalable) {
            mOriginalLayout.setVisibility(View.VISIBLE);
            updateOriginalState();
        } else {
            mOriginalLayout.setVisibility(View.GONE);
        }
    }


    private void updateOriginalState() {
        mOriginal.setChecked(mOriginalEnable);
        if (!mOriginalEnable) {
            mOriginal.setColor(Color.WHITE);
        }

        if (countOverMaxSize() > 0) {
            if (mOriginalEnable) {
                AppDialog.Companion.show(BasePreviewActivity.this, BasePreviewActivity.this, materialDialog -> {
                    materialDialog.title(null, "");
                    materialDialog.message(null, getString(R.string.error_over_original_size, mSpec.originalMaxSize), null);
                    return null;
                });

                mOriginal.setChecked(false);
                mOriginal.setColor(Color.WHITE);
                mOriginalEnable = false;
            }
        }
    }


    private int countOverMaxSize() {
        int count = 0;
        int selectedCount = mSelectedCollection.count();
        for (int i = 0; i < selectedCount; i++) {
            Item item = mSelectedCollection.asList().get(i);
            if (item.isImage()) {
                float size = PhotoMetadataUtils.getSizeInMB(item.size);
                if (size > mSpec.originalMaxSize) {
                    count++;
                }
            }
        }
        return count;
    }

    protected void updateSize(Item item) {
        if (item.isGif()) {
            mSize.setVisibility(View.VISIBLE);
            mSize.setText(PhotoMetadataUtils.getSizeInMB(item.size) + "M");
        } else {
            mSize.setVisibility(View.GONE);
        }

        if (item.isVideo()) {
            mOriginalLayout.setVisibility(View.GONE);
        } else if (mSpec.originalable) {
            mOriginalLayout.setVisibility(View.VISIBLE);
        }
    }

    protected void sendBackResult(boolean apply) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESULT_BUNDLE, mSelectedCollection.getDataWithBundle());
        intent.putExtra(EXTRA_RESULT_APPLY, apply);
        intent.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
        setResult(Activity.RESULT_OK, intent);
    }

    private boolean assertAddSelection(Item item) {
        IncapableCause cause = mSelectedCollection.isAcceptable(item);
        IncapableCause.handleCause(this, cause);
        return cause == null;
    }

    protected void selectRecyclerItem(Item item){
        List<Item> list=mSelectedCollection.asList();
        boolean has=false;
        for (int i=0;i<list.size();i++){
            Item item1=list.get(i);
            if (item1.equals(item)){
                has=true;
                mBottomAdapter.setSelectPos(i);
                mRecyclerView.scrollToPosition(i);
            }
        }
        if (mBottomAdapter!=null && !has){
            mBottomAdapter.setSelectPos(-1);
        }
    }

    @Override
    public void onBottomItemClick(int position, Item item) {

    }
}
