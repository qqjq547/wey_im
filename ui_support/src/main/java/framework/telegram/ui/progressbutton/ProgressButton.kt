package framework.telegram.ui.progressbutton

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.Gravity
import androidx.annotation.IdRes
import androidx.appcompat.widget.AppCompatButton
import framework.telegram.ui.R

/**
 * @author ounk
 */
class ProgressButton: AppCompatButton {

    private val MAX_PROGRESS = 100

    private var isInit = false

    private var mProgress = 0

    private val mProgressPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            strokeWidth = 10f
        }
    }

    private var mProgressText: String = ""

    //  进度条颜色
    @IdRes
    private var mProgressBarColor: Int = 0
    //  进度条背景颜色
    @IdRes
    private var mProgressBarBgColor: Int = 0
    //  字体颜色
    @IdRes
    private var mProgressTextColor: Int = 0
    //  圆角角度
    private var mCornerRadius: Float = 10f

    private val mDrawable: StateListDrawable = StateListDrawable()

    private var mProgressBGDrawable: GradientDrawable? = null

    private var mProgressDrawable: GradientDrawable? = null

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        if (isInit)
            return

        context.let {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressButton)
            mProgressBarColor = typedArray.getColor(R.styleable.ProgressButton_progress_bar_color, Color.RED)
            mProgressBarBgColor = typedArray.getColor(R.styleable.ProgressButton_progress_bar_bg_color, Color.BLACK)
//            mProgressTextColor = typedArray.getColor(R.styleable.ProgressButton_text_color, Color.WHITE)
            mCornerRadius = typedArray.getDimension(R.styleable.ProgressButton_button_corner_radius, 10f)


            //TODO 内部做圆角的交互ui
//            val pressDrawable: GradientDrawable = GradientDrawable()
//            val normalDrawable: GradientDrawable = resources.getDrawable(R.drawable.common_corners_trans_178aff_8_0).mutate() as GradientDrawable
//            mDrawable.addState(intArrayOf(android.R.attr.state_pressed), pressDrawable)
//            mDrawable.addState(intArrayOf(), normalDrawable)

            mProgressDrawable = resources.getDrawable(R.drawable.progress_button_rect).mutate() as GradientDrawable
            mProgressBGDrawable = resources.getDrawable(R.drawable.progress_button_background_rect).mutate() as GradientDrawable

            mProgressDrawable?.let {
                it.setColor(mProgressBarColor)
                it.cornerRadius = mCornerRadius
            }
            mProgressBGDrawable?.let {
                it.setColor(mProgressBarBgColor)
                it.cornerRadius = mCornerRadius
            }


            gravity = Gravity.CENTER
            mProgressPaint.color = mProgressBarColor
//            setTextColor(mProgressTextColor)
            background = mProgressBGDrawable

            typedArray.recycle()
            isInit = true
        }

    }


    override fun onDraw(canvas: Canvas?) {
        drawProgress(canvas, mProgress)
        if (mProgressText != "")
            drawText(mProgressText)
        super.onDraw(canvas)
    }

    fun setProgress(progress: Int) {
        mProgress = progress
        postInvalidate()
    }

    fun setProgressAndText(progress: Int, text: String) {
        mProgress = progress
        mProgressText = text
        postInvalidate()
    }

    private fun drawProgress(canvas: Canvas?, progress: Int) {
        background = mProgressBGDrawable
        if (progress < MAX_PROGRESS) {
            canvas?.let {
//                val left = 0f
//                val top = mCornerRadius - progress
//                val right = (width * mProgress / MAX_PROGRESS).toFloat()
//                val bottom = height.toFloat()
//                val rect = RectF(left, top, right, bottom)
//                canvas.drawRect(rect, mProgressPaint)
                mProgressDrawable?.let {
                    it.setBounds(0, 0, measuredWidth * progress / MAX_PROGRESS, measuredHeight)
                    it.draw(canvas)
                }

            }
        }else
            background = mProgressDrawable
    }

    private fun drawText(text: String) {
        setText(text)
    }

}