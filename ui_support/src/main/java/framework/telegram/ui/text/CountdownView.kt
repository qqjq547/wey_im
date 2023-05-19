package framework.telegram.ui.text

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.widget.TextView
import framework.telegram.ui.R

/**
 * Created by lzh on 19-5-16.
 * INFO:
 */
class CountdownButton : TextView {

    private val mHandler: Handler = Handler()
    private var mCountTime = 60
    private var defaultStr = ""
    private var otherStr = ""
    private var countingStr = ""

    private var waitTextColorId = 0
    private var enableTextColorId = 0

    constructor(mContext: Context, attrSet: AttributeSet) : super(mContext,attrSet) {
        defaultStr = mContext.getString(R.string.get_code)
        this.text = defaultStr
        otherStr = mContext.getString(R.string.regain)
        countingStr = "%ss "
        waitTextColorId = mContext.resources.getColor(R.color.a2a4a7)
        enableTextColorId = mContext.resources.getColor(R.color.c178aff)
        this.setTextColor(enableTextColorId)
    }

    fun setCountDownText(defaultStr: String, retryStr: String, countingStr: String) {
        this.defaultStr = defaultStr
        this.otherStr = retryStr
        this.countingStr = countingStr
    }

    /**
     *倒计时，并处理点击事件
     **/
    fun sendVerifyCode() {
        mHandler.postDelayed(countDown, 0)
    }

    fun getClickStatus(): Boolean{
        return mCountTime >= 59
    }

    fun setCountTime(countTime:Int){
        mCountTime = countTime
    }

    /**
     *  销毁时清空
     */
    override fun onDetachedFromWindow() {
        removeRunnable()
        super.onDetachedFromWindow()
    }

    /*
        倒计时
     */
    private val countDown = object : Runnable {
        @SuppressLint("SetTextI18n")
        override fun run() {
            this@CountdownButton.text = String.format(countingStr,mCountTime.toString())
            this@CountdownButton.isEnabled = false
            this@CountdownButton.setTextColor(waitTextColorId)

            if (mCountTime > 0) {
                mHandler.postDelayed(this, 1000)
            } else {
                resetCounter()
                removeRunnable()
            }
            mCountTime--
        }
    }

    fun removeRunnable() {
        //  清空所有callback和message
        mHandler.removeCallbacksAndMessages(null)
    }

    //重置按钮状态
    fun resetCounter(vararg text: String) {
        this.isEnabled = true
        if (text.isNotEmpty() && "" != text[0]) {
            this.text = text[0]
        } else {
            this.text = otherStr
            this.setTextColor(enableTextColorId)
        }
        mCountTime = 60
    }
}