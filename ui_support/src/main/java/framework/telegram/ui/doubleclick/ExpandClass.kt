package framework.telegram.ui.doubleclick

import android.view.View

/**
 * Created by yanggl on 2019/10/29 16:59
 */
object ExpandClass {
    fun View.setOnNoDoubleClickListten(listener: (v: View?) -> Unit) {
        this.setOnClickListener(NoDoubleClickListener(listener))
    }
}