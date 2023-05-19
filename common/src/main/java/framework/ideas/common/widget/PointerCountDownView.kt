package framework.ideas.common.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.Animation.RESTART
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import framework.ideas.common.R
import framework.telegram.support.tools.TimeUtils
import framework.telegram.ui.utils.ScreenUtils
import kotlinx.android.synthetic.main.common_countdown_layout.view.*
import kotlin.math.ceil


/**
 * Created by lzh on 19-5-16.
 * INFO:
 */
class PointerCountDownView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val animation by lazy {   RotateAnimation(0f, -360f, (size).toFloat(), (size).toFloat())}
    private val size  by lazy { ScreenUtils.dp2px(this.context, 4f) }

    private var mCall: (() -> Unit)? = null

    private var mAnimationRunning = false

    init {
        LayoutInflater.from(context).inflate(R.layout.common_countdown_layout, this)
        this.orientation = HORIZONTAL
        this.background = ContextCompat.getDrawable(context, R.drawable.common_corners_trans_ff000000_9_0)
    }

    fun initCountDownMaxValue(time: Int){
        progress_bar1.maxValue = time
        mAnimationRunning = false
        image_view_pointer.clearAnimation()
    }

    fun initCountDownText(time: Int) {
        progress_bar1.maxValue = time
        progress_bar1.setProgress(time, false)
        text_view_count.text = TimeUtils.timeFormatForDeadline(time)
        image_view_pointer.rotation = 0F

        animation.duration = 2500
        animation.repeatMode = RESTART
        animation.repeatCount = Animation.INFINITE
        animation.interpolator = LinearInterpolator()

        animation.setAnimationListener(object :Animation.AnimationListener{
            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
            }

            override fun onAnimationStart(animation: Animation?) {
                mAnimationRunning = true
            }
        })
        image_view_pointer.clearAnimation()
        mAnimationRunning = false

    }

    fun setCallback(call: (() -> Unit)){
        if (mCall==null)
            mCall = call
    }

    fun setCurProgress(process: Int) {
        if (process < 0) {
            progress_bar1.progress = 0
            mCall?.invoke()
        } else {
            val progress = ceil(((process) / 1000).toDouble())
            progress_bar1.setProgress(progress.toInt(), false)
//            image_view_pointer.rotation = (( (progress %5))*72).toFloat()
            text_view_count.text = TimeUtils.timeFormatForDeadline((progress).toInt())
            if (!mAnimationRunning){
                image_view_pointer.startAnimation(animation)
            }
        }
    }

}