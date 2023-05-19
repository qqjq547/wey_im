package framework.telegram.ui.face


import android.app.Activity
import android.content.Context
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.Xml
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

import java.io.IOException
import java.util.ArrayList

import framework.telegram.ui.R
import framework.telegram.ui.menu.Display
import framework.telegram.ui.menu.MenuItem
import framework.telegram.ui.utils.ScreenUtils

/**
 * Created by xiaoqi on 2017/12/11.
 */

class FloatWindow(private val context: Context, private val mView: View) : PopupWindow(context) {
    private val ANCHORED_GRAVITY = Gravity.TOP or Gravity.START
    private val WINDOW_WIDTH =  ScreenUtils.dp2px(context, 160f)
    private val WINDOW_HEIGHT = ScreenUtils.dp2px(context, 144f)
    private var mImageView:ImageView? = null
    private var mRootView :View?=null
    private val mSreenWidth by  lazy { ScreenUtils.getScreenWidth(context) }

    constructor(activity: Activity) : this(activity, activity.findViewById<View>(android.R.id.content)) {}

    init {
        setBackgroundDrawable(BitmapDrawable())
        contentView = initView()
        width = WINDOW_WIDTH
        height = WINDOW_HEIGHT
    }

    private fun initView():View{
        mRootView = LayoutInflater.from(context).inflate(R.layout.float_window_item, null)
        mImageView = mRootView?.findViewById<ImageView>(R.id.image_view)
        return mRootView!!
    }

    fun show(height:Float,path:String,view:View) {
        val offset =  (WINDOW_WIDTH - view.width) /2
        mImageView?.let {
            Glide.with(context).load(path).into(it)
        }

        when {
            view.right <WINDOW_WIDTH -> mRootView?.setBackgroundResource(R.drawable.icon_gif_left)
            mSreenWidth -  view.left < WINDOW_WIDTH -> mRootView?.setBackgroundResource(R.drawable.icon_gif_right)
            else -> mRootView?.setBackgroundResource(R.drawable.icon_gif_center)
        }

        animationStyle = R.style.Animation_bottom
//        Log.i("lzh", "height ${height.toInt()}    ${view.top}")
        showAtLocation(mView, ANCHORED_GRAVITY, view.left - offset , height.toInt() + view.top  - WINDOW_HEIGHT)
    }
}


