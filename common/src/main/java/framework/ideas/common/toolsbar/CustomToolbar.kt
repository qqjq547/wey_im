package framework.ideas.common.toolsbar

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import com.facebook.common.util.UriUtil
import com.facebook.drawee.drawable.ScalingUtils
import framework.ideas.common.R
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView
import kotlinx.android.synthetic.main.common_layout_toolbar.view.*

/**
 * Created by lzh on 19-5-16.
 * INFO:
 */
class CustomToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {

    //是否需要主题
    private val mContext: Context = context
    private var mStatusBar:View? = null

    init {
        LayoutInflater.from(mContext).inflate(R.layout.common_layout_toolbar, this)
        this.orientation = VERTICAL
        if (Build.VERSION.SDK_INT >= 23) {
            mStatusBar = View(mContext)
            val param = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(mContext, 25f))
            mStatusBar?.layoutParams = param
            mStatusBar?.setBackgroundColor(ContextCompat.getColor(this.context, R.color.f8fafd))
            this.addView(mStatusBar, 0)
        }else{
            mStatusBar = View(mContext)
            val param = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(mContext, 25f))
            mStatusBar?.layoutParams = param
            mStatusBar?.setBackgroundColor(ContextCompat.getColor(this.context, R.color.c27000000))
            this.addView(mStatusBar, 0)
        }
    }

    /**
     * 针对登陆注册页面
     * 点击输入框，布局上移
     * 23以下，状态栏不透明，会遮挡布局，
     * 需要设置状态栏透明
     */
    public fun androidMTransparency(transparency:Boolean){
        if (Build.VERSION.SDK_INT < 23) {
            if (transparency){
                mStatusBar?.setBackgroundColor(ContextCompat.getColor(this.context, R.color.transparent))
            }else{
                mStatusBar?.setBackgroundColor(ContextCompat.getColor(this.context, R.color.c27000000))
            }
        }
    }

    /**
     * 设置回退按钮
     * 回调
     */
    fun setBackIcon(icon: Int, listen: ((imageView: ImageView) -> Unit)? = null, callback: (() -> Unit)) {
        image_view_back.setImageResource(icon)
        listen?.invoke(image_view_back)
        image_view_back.setOnClickListener {
            callback.invoke()
        }

        image_view_back.visibility = View.VISIBLE
    }

    fun showLeftView(view: View, onClickCallback: (() -> Unit)? = null, index: Int = 0) {
        linear_layout_left.addView(view, index)
        view.setOnClickListener {
            onClickCallback?.invoke()
        }
        linear_layout_left.visibility = View.VISIBLE
    }

    fun showCustomLeftView(view: View, onClickCallback: (() -> Unit)? = null) {
        val  w = toolbar_layout.measuredWidth - image_view_back.measuredWidth - linear_layout_center.measuredWidth - linear_layout_right.measuredWidth - (dpToPx(mContext,32f)*2.5).toInt()
        val p = ViewGroup.LayoutParams(w,ViewGroup.LayoutParams.WRAP_CONTENT)
        linear_layout_left.addView(view, p)
        view.setOnClickListener {
            onClickCallback?.invoke()
        }
        linear_layout_left.visibility = View.VISIBLE
    }

    /**
     * 添加左上角TextView
     * 可监听点击回调
     * 可以再回调获取textview 自定义样式
     */
    fun showLeftTextView(title: String, onClickCallback: (() -> Unit)? = null, index: Int = 0, listen: ((textView: TextView) -> Unit)? = null) {
        val textView = getDefaultTextView()
        textView.text = title
        listen?.invoke(textView)
        linear_layout_left.addView(textView, index)
        textView.setOnClickListener {
            onClickCallback?.invoke()
        }
        linear_layout_left.visibility = View.VISIBLE
    }

    /**
     * 添加左上角ImageView
     * 可监听点击回调
     * 可以再回调获取ImageView 自定义样式
     */
    fun showLeftImageView(rid: Int, onClickCallback: (() -> Unit)? = null, index: Int = 0, listen: ((imageView: AppImageView) -> Unit)? = null) {
        showLeftImageView(UriUtil.getUriForResourceId(rid),0f,0f, onClickCallback, index, listen)
    }

    /**
     * 添加左上角ImageView
     * 可监听点击回调
     * 可以再回调获取ImageView 自定义样式
     */
    fun showLeftImageView(uri: Uri, height: Float = 0f,width: Float= 0f,onClickCallback: (() -> Unit)? = null, index: Int = 0, listen: ((imageView: AppImageView) -> Unit)? = null) {
        val imageView = getDefaultImageView(height,width)
        imageView.setImageURI(uri)
        listen?.invoke(imageView)
        linear_layout_left.addView(imageView, index)
        imageView.setOnClickListener {
            onClickCallback?.invoke()
        }
        linear_layout_left.visibility = View.VISIBLE
    }

    /**
     * 添加右上角TextView
     * 可监听点击回调
     * 可以再回调获取textview 自定义样式
     */
    fun showRightTextView(title: String, onClickCallback: (() -> Unit)? = null, index: Int = 0, listen: ((textView: TextView) -> Unit)? = null) {
        val textView = getDefaultTextView()
        textView.text = title
        textView.setTextColor(ContextCompat.getColor(mContext, R.color.c178aff))
        listen?.invoke(textView)
        linear_layout_right.addView(textView, index)
        textView.setOnClickListener {
            onClickCallback?.invoke()
        }
        linear_layout_right.visibility = View.VISIBLE
    }

    /**
     * 添加右上角ImageView
     * 可监听点击回调
     * 可以再回调获取ImageView 自定义样式
     */
    fun showRightImageView(rid: Int, onClickCallback: (() -> Unit)? = null, index: Int = 0, listen: ((imageView: AppImageView) -> Unit)? = null) {
        val imageView = getDefaultImageView()
        imageView.setImageResource(rid)
        listen?.invoke(imageView)
        linear_layout_right.addView(imageView, index)
        imageView.setOnClickListener {
            onClickCallback?.invoke()
        }
        linear_layout_right.visibility = View.VISIBLE
    }

    /**
     * 设置居中显示的标题
     * 可以再回调获取textview 自定义样式
     */
    fun showCenterTitle(title: String, listen: ((textView: TextView) -> Unit)? = null) {
        val textView = getDefaultTextView()
        textView.text = title
        listen?.invoke(textView)
        linear_layout_center.addView(textView)
        linear_layout_center.visibility = View.VISIBLE
    }

    fun addCenterView(view: View) {
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        view.layoutParams = layoutParams
        layoutParams.gravity = Gravity.CENTER
        linear_layout_center.addView(view)
        linear_layout_center.visibility = View.VISIBLE
    }

    fun addCenterView(view: View, width: Float, height: Float){
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(mContext,height))
        view.layoutParams = layoutParams
        layoutParams.gravity = Gravity.CENTER
        linear_layout_center.addView(view)
        linear_layout_center.visibility = View.VISIBLE
    }

    /**
     * 获取默认textview
     */
    private fun getDefaultTextView(): TextView {
        val textView = AppTextView(mContext)
        val param = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        param.gravity = Gravity.CENTER
        val size = dpToPx(mContext, 10f)
        textView.setPadding(0, 0, size, 0)
        textView.gravity = Gravity.CENTER
        textView.setTextColor(ContextCompat.getColor(mContext, R.color.black))
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        textView.maxLines = 1
        textView.layoutParams = param
        textView.text = ""
        return textView
    }

    /**
     * 获取默认ImageView
     */
    private fun getDefaultImageView(height: Float = 0f,width: Float= 0f): AppImageView {
        val imageView = AppImageView(mContext)
        imageView.hierarchy.actualImageScaleType = ScalingUtils.ScaleType.CENTER
        val param = LayoutParams(if (height == 0f) WRAP_CONTENT else dpToPx(mContext, height)
                , if (width == 0f) WRAP_CONTENT else dpToPx(mContext, width))
        val size = dpToPx(mContext, 10f)
        param.leftMargin = 0
        param.rightMargin = size
        param.topMargin = 0
        param.bottomMargin = 0
        imageView.layoutParams = param
        return imageView
    }

    /**
     * 设置toolbar的高度，不是这个控件的高度，这个控件的高度包括toolbar + statusBar
     */
    fun setToolbarSize(height: Float) {
        toolbar_layout.layoutParams.height = dpToPx(mContext, height)
    }

    fun setToolbarColor(color: Int) {
        toolbar_layout.setBackgroundColor(ContextCompat.getColor(this.context,color))
    }

    fun setStatuBarColor(color: Int) {
        mStatusBar?.setBackgroundColor(ContextCompat.getColor(this.context,color))
    }

    private fun dpToPx(@NonNull context: Context, dp: Float): Int {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.resources.displayMetrics) + 0.5f)
    }

}