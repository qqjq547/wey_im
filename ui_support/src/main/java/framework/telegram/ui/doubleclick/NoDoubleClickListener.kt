package framework.telegram.ui.doubleclick

import android.view.View
import framework.telegram.ui.doubleclick.helper.ViewDoubleHelper

/**
 * Created by yanggl on 2019/10/29 15:55
 */
class NoDoubleClickListener(val mListener: (v: View?) -> Unit) : View.OnClickListener {

    private var lastTime=0L

    override fun onClick(v: View?) {
        val t=System.currentTimeMillis()
        if ((t-lastTime)> ViewDoubleHelper.mDelayTime){
            mListener.invoke(v)
        }
        lastTime=t
    }

}