package framework.telegram.ui.face


import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import framework.telegram.ui.R
import framework.telegram.ui.utils.ScreenUtils

/**
 * Created by xiaoqi on 2017/12/11.
 */

class FloatView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.petface_item_dynamic, this)
    }

//     val mPopPoint = Point()
//
//     val mClickPoint = Point()
//
//    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
//
//        mPopPoint.x = ev.rawX.toInt()
//        mPopPoint.y = ev.rawY.toInt()
//        mClickPoint.x = ev.x.toInt()
//        mClickPoint.y = ev.y.toInt()
//        Log.i("lzh", "FloatView   x ${mPopPoint.x} y ${mPopPoint.y}")
//        return super.dispatchTouchEvent(ev)
//    }
}