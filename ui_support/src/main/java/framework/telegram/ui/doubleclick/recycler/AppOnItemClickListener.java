package framework.telegram.ui.doubleclick.recycler;

import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;

import framework.telegram.ui.doubleclick.helper.ViewDoubleHelper;

/**
 * Created by yanggl on 2019/10/29 14:09
 */
public class AppOnItemClickListener implements BaseQuickAdapter.OnItemClickListener {

    private BaseQuickAdapter.OnItemClickListener mListener;

    public AppOnItemClickListener(BaseQuickAdapter.OnItemClickListener mListener){
        this.mListener = mListener;
    }

    private long lastTime=0;

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        long t=System.currentTimeMillis();
        if (mListener!=null && (t-lastTime)> ViewDoubleHelper.mDelayTime){
            mListener.onItemClick(adapter,view,position);
        }
        lastTime=t;
    }
}
