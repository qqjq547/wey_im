package framework.telegram.ui.doubleclick.recycler;

import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;

import framework.telegram.ui.doubleclick.helper.ViewDoubleHelper;

/**
 * Created by yanggl on 2019/10/29 14:14
 */
public class AppOnItemChildClickListener implements BaseQuickAdapter.OnItemChildClickListener {

    private BaseQuickAdapter.OnItemChildClickListener mListener;

    public AppOnItemChildClickListener(BaseQuickAdapter.OnItemChildClickListener l){
        mListener = l;
    }

    private long lastTime=0;

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
        long t=System.currentTimeMillis();
        if (mListener!=null && (t-lastTime)> ViewDoubleHelper.mDelayTime){
            mListener.onItemChildClick(adapter,view,position);
        }
        lastTime=t;
    }
}
