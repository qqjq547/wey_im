package framework.telegram.ui.recyclerview;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;

import framework.telegram.ui.R;

public class AppRecyclerView extends FrameLayout {

    enum Type {
        SingleType,
        MultiType,
        IndexFastScrollType
    }

    private boolean isInit = false;

    private GestureDetector mGestureDetector = null;

    private Type mType = null;

    private RefreshController mRefreshController;
    private LoadMoreController mLoadMoreController;
    private RecyclerViewController mRecyclerViewController;
    private ItemController mItemController;
    private HeaderController mHeaderController;
    private FooterController mFooterController;
    private EmptyController mEmptyController;
    private ErrorController mErrorController;
    private IndexFastScrollController mIndexFastScrollController;
    private DiffController mDiffController;

    public AppRecyclerView(Context context) {
        super(context);
    }

    public AppRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AppRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AppRecyclerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void initSingleTypeRecycleView(RecyclerView.LayoutManager layoutManager, BaseQuickAdapter adapter, boolean multiPage) {
        mType = Type.SingleType;
        init(layoutManager, adapter);
        adapter.closeLoadAnimation();
        adapter.setUpFetchEnable(false);
        adapter.setUpFetchListener(null);
        if (multiPage) {
//            adapter.setPreLoadNumber(3);//下面剩余3个item时开始加载更多
            adapter.setEnableLoadMore(true);
            adapter.setOnLoadMoreListener(this::onLoadMore, findViewById(R.id.recycler_view));
        } else {
            adapter.setEnableLoadMore(false);
            adapter.setOnLoadMoreListener(null, findViewById(R.id.recycler_view));
        }
    }

    public void initMultiTypeRecycleView(RecyclerView.LayoutManager layoutManager, BaseMultiItemQuickAdapter adapter, boolean multiPage) {
        mType = Type.MultiType;
        init(layoutManager, adapter);
        adapter.closeLoadAnimation();
        adapter.setUpFetchEnable(false);
        adapter.setUpFetchListener(null);
        if (multiPage) {
//            adapter.setPreLoadNumber(3);//下面剩余3个item时开始加载更多
            adapter.setEnableLoadMore(true);
            adapter.setOnLoadMoreListener(this::onLoadMore, findViewById(R.id.recycler_view));
        } else {
            adapter.setEnableLoadMore(false);
            adapter.setOnLoadMoreListener(null, findViewById(R.id.recycler_view));
        }
    }

    public void initIndexFastScrollRecyclerView(RecyclerView.LayoutManager layoutManager, BaseMultiItemQuickAdapter adapter, boolean multiPage) {
        mType = Type.IndexFastScrollType;
        init(layoutManager, adapter);
        adapter.closeLoadAnimation();
        adapter.setUpFetchEnable(false);
        adapter.setUpFetchListener(null);
        this.setWillNotDraw(false);
        if (multiPage) {
//            adapter.setPreLoadNumber(3);//下面剩余3个item时开始加载更多
            adapter.setEnableLoadMore(true);
            adapter.setOnLoadMoreListener(this::onLoadMore, findViewById(R.id.recycler_view));
        } else {
            adapter.setEnableLoadMore(false);
            adapter.setOnLoadMoreListener(null, findViewById(R.id.recycler_view));
        }
    }

    private void init(RecyclerView.LayoutManager layoutManager, BaseQuickAdapter adapter) {
        if (isInit) {
            throw new RuntimeException("IdeaRecyclerView Has been initialized!");
        }

        LayoutInflater.from(getContext()).inflate(R.layout.custom_idea_recyclerview, this);

        SmartRefreshLayout smartRefreshLayout = findViewById(R.id.refresh_layout);
        smartRefreshLayout.setEnableLoadMore(false);
        smartRefreshLayout.setEnableAutoLoadMore(false);
        smartRefreshLayout.setDisableContentWhenRefresh(true);
        smartRefreshLayout.setOnRefreshListener(refreshLayout -> {
            if (mRefreshController.getOnRefreshListener() != null) {
                mRefreshController.getOnRefreshListener().onRefreshing(AppRecyclerView.this);
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setOverScrollMode(OVER_SCROLL_NEVER);
        recyclerView.setLayoutManager(layoutManager);
        adapter.bindToRecyclerView(recyclerView);

        mRecyclerViewController = new RecyclerViewController(recyclerView, adapter);
        mRefreshController = new RefreshController(smartRefreshLayout);
        mRefreshController.setEnablePullToRefresh(false);
        mLoadMoreController = new LoadMoreController(mRecyclerViewController);
        mHeaderController = new HeaderController(smartRefreshLayout, mRecyclerViewController);
        mFooterController = new FooterController(smartRefreshLayout, mRecyclerViewController);
        mItemController = new ItemController(mRecyclerViewController);
        mEmptyController = new EmptyController(recyclerView, adapter);
        mErrorController = new ErrorController(recyclerView, adapter);
        mIndexFastScrollController = new IndexFastScrollController(recyclerView.getContext(), this, recyclerView, adapter);
        mDiffController = new DiffController(recyclerView, adapter);

        isInit = true;
    }

    private void onLoadMore() {
        if (mLoadMoreController.getOnLoadMoreListener() != null) {
            mLoadMoreController.getOnLoadMoreListener().onLoadMore(this);
        }
    }

    public void destory() {
        mHeaderController.removeAllHeaderView();
        mFooterController.removeAllFooterView();
        mItemController.setNewData(null);
        ((RecyclerView) findViewById(R.id.recycler_view)).setAdapter(null);
    }

    public RefreshController refreshController() {
        if (!isInit) {
            throw new RuntimeException("IdeaRecyclerView Has not been initialized!");
        }

        return mRefreshController;
    }

    public RecyclerViewController recyclerViewController() {
        if (!isInit) {
            throw new RuntimeException("IdeaRecyclerView Has not been initialized!");
        }

        return mRecyclerViewController;
    }

    public ItemController itemController() {
        if (!isInit) {
            throw new RuntimeException("IdeaRecyclerView Has not been initialized!");
        }

        return mItemController;
    }

    public HeaderController headerController() {
        if (!isInit) {
            throw new RuntimeException("IdeaRecyclerView Has not been initialized!");
        }

        return mHeaderController;
    }

    public FooterController footerController() {
        if (!isInit) {
            throw new RuntimeException("IdeaRecyclerView Has not been initialized!");
        }

        return mFooterController;
    }

    public LoadMoreController loadMoreController() {
        if (!isInit) {
            throw new RuntimeException("IdeaRecyclerView Has not been initialized!");
        }

        return mLoadMoreController;
    }

    public EmptyController emptyController() {
        if (!isInit) {
            throw new RuntimeException("IdeaRecyclerView Has not been initialized!");
        }
        return mEmptyController;
    }

    public ErrorController errorController() {
        if (!isInit) {
            throw new RuntimeException("IdeaRecyclerView Has not been initialized!");
        }
        return mErrorController;
    }

    public RecyclerView getRecyclerView() {
        return findViewById(R.id.recycler_view);
    }

    public IndexFastScrollController indexFastScrollController() {
        if (!isInit) {
            throw new RuntimeException("IdeaRecyclerView Has not been initialized!");
        }
        return mIndexFastScrollController;
    }

    public DiffController diffController() {
        if (!isInit) {
            throw new RuntimeException("IdeaRecyclerView Has not been initialized!");
        }
        return mDiffController;
    }

    /**
     * mPopPoint 是给弹出框用的 (floatMenu)
     */
    private Point mPopPoint = new Point();

    public Point getPopPoint() {
        return mPopPoint;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mPopPoint.x = (int) ev.getRawX();
            mPopPoint.y = (int) ev.getRawY();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mType == Type.IndexFastScrollType && mIndexFastScrollController != null)
            mIndexFastScrollController.onSizeChanged(w, h, oldw, oldh);
    }


    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Overlay index bar
        if (mType == Type.IndexFastScrollType && mIndexFastScrollController != null)
            mIndexFastScrollController.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mType == Type.IndexFastScrollType) {
            // Intercept ListView's touch event
            if (mIndexFastScrollController != null && mIndexFastScrollController.onTouchEvent(ev))
                return true;

            if (mGestureDetector == null) {
                mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {
                        return super.onFling(e1, e2, velocityX, velocityY);
                    }

                });
            }
            mGestureDetector.onTouchEvent(ev);
        }

        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mType == Type.IndexFastScrollType
                && mIndexFastScrollController != null && mIndexFastScrollController.contains(ev.getX(), ev.getY()))
            return true;

        return super.onInterceptTouchEvent(ev);
    }

}
