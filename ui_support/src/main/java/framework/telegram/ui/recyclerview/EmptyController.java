package framework.telegram.ui.recyclerview;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;

import framework.telegram.ui.R;
import framework.telegram.ui.status.QMUIViewBuilder;

public class EmptyController {


    private RecyclerView mRecyclerView;
    private BaseQuickAdapter mAdapter;


    EmptyController(RecyclerView recyclerView, BaseQuickAdapter adapter){
        this.mRecyclerView = recyclerView;
        this.mAdapter = adapter;
    }

    public void setEmpty(String content ){
        View emptyView = new QMUIViewBuilder(QMUIViewBuilder.TYPE.EMPTY_VIEW,content).build(mRecyclerView.getContext());
        if (emptyView != null)
            mAdapter.setEmptyView(emptyView);
    }

    public void setTopEmpty(String content ){
        View emptyView = new QMUIViewBuilder(QMUIViewBuilder.TYPE.EMPTY_VIEW,content).setIsTopView().build(mRecyclerView.getContext());
        if (emptyView != null)
            mAdapter.setEmptyView(emptyView);
    }

    public void setEmpty(){
        View emptyView = new QMUIViewBuilder(QMUIViewBuilder.TYPE.EMPTY_VIEW,mRecyclerView.getContext().getString(R.string.no_data)).build(mRecyclerView.getContext());
        if (emptyView != null)
            mAdapter.setEmptyView(emptyView);
    }

    public void setEmpty(String content ,int rid){
        View emptyView = new QMUIViewBuilder(QMUIViewBuilder.TYPE.EMPTY_VIEW,content)
                .setEmptyImage(rid).build(mRecyclerView.getContext());
        if (emptyView != null)
            mAdapter.setEmptyView(emptyView);
    }

    public void setEmpty(int rid){
        View emptyView = new QMUIViewBuilder(QMUIViewBuilder.TYPE.EMPTY_VIEW,mRecyclerView.getContext().getString(R.string.no_data))
                .setEmptyImage(rid).build(mRecyclerView.getContext());
        if (emptyView != null)
            mAdapter.setEmptyView(emptyView);
    }
}
