主要原理：
1.通过反射执行View的getListenerInfo方法获得mListenerInfo
2.通过反射获得mListenerInfo中的mOnClickListener，这就是我们给View设置的点击事件
3.通过暴力反射替换mOnClickListener成我们自己的点击事件
4.在这里‘我们自己的点击事件’就是在原来点击事件上加了个时间判断，不允许快速点击
详见方法BaseDoubleClick.finalHookView(View view, long delayTime, IOnClickListener iOnClickListener)：
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

部署方式：
Application注册Application.ActivityLifecycleCallbacks监听，可以获得Activity创建时的回调onActivityCreated()
通过onActivityCreated()可以获得Activity实例，然后获得Activity的根布局。
获得根布局的所有子view，替换根布局和这些子view的点击事件
详见ViewDoubleHelper.init(Application application, final long delayTime, Class annotationClass)

因为recycleview的特殊原因，并没有对recycleview做hook操作，
需要针对RecyclerView的特殊处理：
项目中大部分适配器均继承自BaseQuickAdapter
BaseQuickAdapter中有两个点击事件的回调mOnItemClickListener和mOnItemChildClickListener，
在BaseQuickAdapter中赋值这两个回调通过setOnItemClickListener()和setOnItemChildClickListener()方法，
继承BaseQuickAdapter重写这两个方法，set回调的过程中换成我们自己的回调,
我们自己的回调就是在原来回调上加了个时间判断，不允许快速点击
另一个常用的适配器BaseMultiItemQuickAdapter也做同样处理