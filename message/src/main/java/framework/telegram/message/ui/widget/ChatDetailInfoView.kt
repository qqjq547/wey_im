package framework.telegram.message.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import framework.telegram.message.R
import framework.telegram.support.tools.TimeUtils
import kotlinx.android.synthetic.main.msg_chat_group_item_detail_info.view.*

/**
 * Created by lzh on 19-7-17.
 * INFO:ChatDetailInfoView
 */
class ChatDetailInfoView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private val mContext: Context = context

    init {
        LayoutInflater.from(mContext).inflate(R.layout.msg_chat_group_item_detail_info, this)
    }

    fun setData(name: String, rid: Int, time: Long) {
        image_view_name.text = name
        image_view.setImageResource(rid)
        image_view_time1.text = TimeUtils.getYYDFormatTime(time)
        image_view_time2.text = TimeUtils.getHMFormatTime(time)
    }

}