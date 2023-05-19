package framework.telegram.business.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import framework.telegram.business.R
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import kotlinx.android.synthetic.main.bus_contacts_item_friend_cell.view.*

/**
 * Created by lzh on 19-5-16.
 * INFO:
 */
class FriendDetailItem @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private var mOnClickCall: (() -> Unit)? = null

    private val mContext: Context = context

    init {
        LayoutInflater.from(mContext).inflate(R.layout.bus_contacts_item_friend_cell, this)
        initListen()
    }

    private fun initListen() {
        this.setOnClickListener {
            mOnClickCall?.invoke()
        }
    }

    fun setListen(listen: (() -> Unit)) {
        mOnClickCall = listen
    }

    fun setData(name: String, value: String, rid: Int = 0,hintStr :String = "") {
        app_text_view_name.text = name
        app_text_view_value.text = value
        app_text_view_value.hint = hintStr
        if (rid != 0)
            app_text_view_value.setCompoundDrawablesWithIntrinsicBounds(null, null, mContext.getSimpleDrawable(rid), null)
    }

    fun setTextRight(isRight:Boolean){
        if (isRight){
            app_text_view_value.gravity = Gravity.RIGHT
        }else{
            app_text_view_value.gravity = Gravity.LEFT
        }

    }
}