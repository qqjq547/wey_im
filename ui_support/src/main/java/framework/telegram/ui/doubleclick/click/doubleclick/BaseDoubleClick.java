package framework.telegram.ui.doubleclick.click.doubleclick;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import framework.telegram.ui.doubleclick.listener.IOnClickListener;
import framework.telegram.ui.doubleclick.listener.OnClickListenerProxy;

/**
 * @Description:
 * @Author: liys
 * @CreateDate: 2019/8/26 17:24
 * @UpdateUser: 更新者
 * @UpdateDate: 2019/8/26 17:24
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */
public abstract class BaseDoubleClick implements IViewDoubleClick {

    Activity mActivity;

    @Override
    public void register(Activity activity){
        mActivity = activity;
    }

    @Override
    public void hookView(final View view, final long delayTime) {
        hookView(view, delayTime, null);
    }

    @Override
    public void hookView(final View view, final long delayTime, final IOnClickListener iOnClickListener) {
        if(mActivity == null){
            return;
        }
        mActivity.getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                finalHookView(view, delayTime, iOnClickListener);
            }
        });
    }

    /**
     * hook点击事件
     * @param view
     * @param delayTime
     * @param iOnClickListener 替换成自定义监听器
     */
    @Override
    public void finalHookView(View view, long delayTime, IOnClickListener iOnClickListener) {
        if(iOnClickListener == null){
            iOnClickListener = new OnClickListenerProxy(delayTime);
        }
        try {
            Class viewClazz = Class.forName("android.view.View");
            //事件监听器都是这个实例保存的
            Method listenerInfoMethod = viewClazz.getDeclaredMethod("getListenerInfo");
            if (!listenerInfoMethod.isAccessible()) {
                listenerInfoMethod.setAccessible(true);
            }
            Object listenerInfoObj = listenerInfoMethod.invoke(view);

            @SuppressLint("PrivateApi")
            Class listenerInfoClazz = Class.forName("android.view.View$ListenerInfo");

            Field onClickListenerField = listenerInfoClazz.getDeclaredField("mOnClickListener");
            //修改修饰符带来不能访问的问题
            if (!onClickListenerField.isAccessible()) {
                onClickListenerField.setAccessible(true);
            }
            View.OnClickListener mOnClickListener = (View.OnClickListener) onClickListenerField.get(listenerInfoObj);

            if(mOnClickListener instanceof IOnClickListener) { //已经hook过了
                IOnClickListener clickListener = ((IOnClickListener) mOnClickListener);
                if (iOnClickListener.getType() == clickListener.getType()) { //本次==上一次
                    mOnClickListener = clickListener.getOnclickListener(); //覆盖
                }
            }
            iOnClickListener.setOnclickListener(mOnClickListener);
            //更换成自己的点击事件
            onClickListenerField.set(listenerInfoObj, iOnClickListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void release(){
        mActivity = null;
    }

}
