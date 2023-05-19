package framework.telegram.business.ui.widget

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import framework.telegram.business.R
import kotlinx.android.synthetic.main.bus_item_view.view.*

/**
 * 用于头像
 */
class AvatarSettingItemView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private val mContext: Context = context

    init {
        LayoutInflater.from(mContext).inflate(R.layout.bus_item_view, this)
    }

    fun setData(name: String, value: String = "", url: String = "", rid: Int = 0, listen: (() -> Unit)) {
        setData(name, value, url, rid, false, listen)
    }

    fun setDataNonePoint(name: String, value: String = "", url: String = "", rid: Int = 0, listen: (() -> Unit)) {
        setData(name, value, url, rid, true, listen)
    }

    fun setData(value: String) {
        app_text_view_value.text = value
    }

    fun setData(name: String, value: String = "", url: String = "", rid: Int = 0, nonePoint: Boolean, listen: (() -> Unit)) {
        app_text_view_name.text = name
        app_text_view_value.text = value

        if (nonePoint) {
            app_text_view_value.setCompoundDrawables(null, null, null, null)
        }

        if (!TextUtils.isEmpty(url)) {
            image_view_icon.setImageURI(url)
            image_view_icon.visibility = View.VISIBLE
        }

        if (rid != 0) {
            image_view_icon2.setBackgroundResource(rid)
        }

        this.setOnClickListener {
            listen.invoke()
        }
    }

    fun setDataTextColor(color: Int) {
        app_text_view_name.setTextColor(color)
    }

    fun setDataValue(value: String) {
        app_text_view_value.text = value
    }

    fun setDataUrl(url: String) {
        image_view_icon.setImageURI(url)
    }

    fun setExtraIcon( extraRid: Int =0,margin:Int) {
        if (extraRid!=0){
            image_view_icon2.visibility= View.VISIBLE
            image_view_icon2.setBackgroundResource(extraRid)
            val layoutParams = image_view_icon2.layoutParams as RelativeLayout.LayoutParams
            layoutParams.rightMargin = margin
        }else{
            image_view_icon2.visibility= View.GONE
        }
    }
}