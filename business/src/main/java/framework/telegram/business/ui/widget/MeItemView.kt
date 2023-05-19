package framework.telegram.business.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import framework.telegram.business.R
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import kotlinx.android.synthetic.main.bus_me_item.view.*

/**
 * Created by lzh on 19-5-16.
 * INFO:
 */
class MeItemView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private val mContext: Context = context

    init {
        LayoutInflater.from(mContext).inflate(R.layout.bus_me_item, this)
    }


    fun setData(name: String, rid: Int,extraRid: Int =0, listen: (() -> Unit)) {
        app_text_view_name.text = name
        image_view_icon.setImageResource(rid)
        if (extraRid!=0)
            image_view_icon_extra.setBackgroundResource(extraRid)
        this.setOnClickListener {
            listen.invoke()
        }
    }

    fun setExtraIcon( extraRid: Int =0) {
        if (extraRid!=0){
            image_view_icon_extra.visibility= View.VISIBLE
            image_view_icon_extra.setBackgroundResource(extraRid)
        }else{
            image_view_icon_extra.visibility= View.GONE
        }
    }

}