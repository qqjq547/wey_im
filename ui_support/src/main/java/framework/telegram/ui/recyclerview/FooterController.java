package framework.telegram.ui.recyclerview;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;

import androidx.annotation.NonNull;
import framework.telegram.ui.R;

public class FooterController {

    private SmartRefreshLayout mSmartRefreshLayout;

    private RecyclerViewController mRecyclerViewController;

    FooterController(@NonNull SmartRefreshLayout smartRefreshLayout, @NonNull RecyclerViewController recyclerViewController) {
        this.mSmartRefreshLayout = smartRefreshLayout;
        this.mRecyclerViewController = recyclerViewController;
    }

    public void setNailFooter(View view, int height) {
        view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (height * view.getContext().getResources().getDisplayMetrics().density)));
        ViewGroup footerLayout = mSmartRefreshLayout.findViewById(R.id.nail_footer_layout);
        footerLayout.removeAllViews();
        footerLayout.addView(view);
        footerLayout.requestLayout();
    }

    public void removeNailFooter() {
        ViewGroup footerLayout = mSmartRefreshLayout.findViewById(R.id.nail_footer_layout);
        footerLayout.removeAllViews();
    }

    public void addFooter(View view) {
        mRecyclerViewController.getAdapter().addFooterView(view);
    }

    public void addFooter(View view, int index) {
        mRecyclerViewController.getAdapter().addFooterView(view, index);
    }

    public void addFooter(View view, int index, int orientation) {
        mRecyclerViewController.getAdapter().addFooterView(view, index, orientation);
    }

    public void removeAllFooterView() {
        mRecyclerViewController.getAdapter().removeAllFooterView();
    }
}
