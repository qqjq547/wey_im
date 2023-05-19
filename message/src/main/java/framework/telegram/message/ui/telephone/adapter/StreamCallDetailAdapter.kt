package framework.telegram.message.ui.telephone.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseViewHolder
import framework.telegram.message.R
import framework.telegram.message.bridge.bean.StreamCallItem
import framework.telegram.support.BaseApp
import framework.telegram.support.tools.TimeUtils
import framework.telegram.ui.doubleclick.recycler.AppBaseQuickAdapter
import java.text.SimpleDateFormat
import java.util.*

class StreamCallDetailAdapter : AppBaseQuickAdapter<StreamCallItem, BaseViewHolder>(R.layout.msg_stream_call_detail_item_content) {

//    companion object {
//        val ITEM_HEAD = 0
//        val ITEM_CONTENT = 1
//    }
//
//    init {
//        addItemType(ITEM_HEAD, R.layout.msg_stream_call_detail_item_head)
//        addItemType(ITEM_CONTENT, R.layout.msg_stream_call_detail_item_content)
//        addItemType()
//    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    override fun convert(helper: BaseViewHolder, item: StreamCallItem?) {

        helper?.let {
            if (item?.nearCount == -1) {
                val timeView = helper.getView<TextView>(R.id.text_view_date)
                val date = Date(item.data.reqTime)
                val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
                timeView.text = simpleDateFormat.format(date)
                timeView.visibility = View.VISIBLE
                helper.getView<View>(R.id.item_layout).visibility = View.GONE
            } else {
                if (item?.data != null) {
                    helper.getView<TextView>(R.id.text_view_date).visibility = View.GONE
                    helper.getView<View>(R.id.item_layout).visibility = View.VISIBLE
                    val timeView = helper.getView<TextView>(R.id.text_view_time)
                    val data = item.data
                    timeView.text = TimeUtils.timeFormatToChat(BaseApp.app, data.reqTime)
                    timeView.setTextColor(mContext.resources.getColor(R.color.c9b9b9b))
                    timeView.textSize = 13f
                    timeView.typeface = Typeface.DEFAULT
                    val contentView = helper.getView<TextView>(R.id.text_view_content)
                    val statusView = helper.getView<ImageView>(R.id.image_view_status)
                    val sendTypeView = helper.getView<ImageView>(R.id.image_view_send_type)
                    if (data.isSend == 0) {
                        when (data.status) {
                            0 -> {
                                contentView.text = contentView.context.getString(R.string.did_not_answer)
                                sendTypeView.setImageResource(R.drawable.msg_icon_stream_call_import_red)
                                changeRed(data.streamType,statusView,contentView)
                            }
                            2 -> {
                                contentView.text = contentView.context.getString(R.string.denied)
                                sendTypeView.setImageResource(R.drawable.msg_icon_stream_call_import_grey)
                                changeGray(data.streamType,statusView,contentView)
                            }
                            3 -> {
                                contentView.text = contentView.context.getString(R.string.did_not_answer)
                                sendTypeView.setImageResource(R.drawable.msg_icon_stream_call_import_red)
                                changeRed(data.streamType,statusView,contentView)
                            }
                            4 -> {
                                contentView.text = contentView.context.getString(R.string.string_busy_tone)
                                sendTypeView.setImageResource(R.drawable.msg_icon_stream_call_import_grey)
                                changeGray(data.streamType,statusView,contentView)
                            }
                            5 -> {
                                val calledTime = if (data.endTime > data.startTime) data.endTime - data.startTime else 0
                                contentView.text = String.format(mContext.getString(R.string.call_duration_mat),TimeUtils.timeFormatToMediaDuration(calledTime))
                                sendTypeView.setImageResource(R.drawable.msg_icon_stream_call_import_grey)
                                changeGray(data.streamType,statusView,contentView)
                            }
                        }
                    } else {
                        when (data.status) {
                            0 -> {
                                contentView.text = contentView.context.getString(R.string.canceled)
                                sendTypeView.setImageResource(R.drawable.msg_icon_stream_call_export_grey)
                                changeGray(data.streamType,statusView,contentView)
                            }
                            2 ->{
                                contentView.text = contentView.context.getString(R.string.the_other_party_has_refused)
                                sendTypeView.setImageResource(R.drawable.msg_icon_stream_call_export_grey)
                                changeGray(data.streamType,statusView,contentView)
                            }
                            3 -> {
                                contentView.text = contentView.context.getString(R.string.canceled)
                                sendTypeView.setImageResource(R.drawable.msg_icon_stream_call_export_grey)
                                changeGray(data.streamType,statusView,contentView)
                            }
                            4 -> {
                                contentView.text = contentView.context.getString(R.string.no_answer)
                                sendTypeView.setImageResource(R.drawable.msg_icon_stream_call_export_grey)
                                changeGray(data.streamType,statusView,contentView)
                            }
                            5 -> {
                                val calledTime = if (data.endTime > data.startTime) data.endTime - data.startTime else 0
                                contentView.text = String.format(mContext.getString(R.string.call_duration_mat),TimeUtils.timeFormatToMediaDuration(calledTime))
                                sendTypeView.setImageResource(R.drawable.msg_icon_stream_call_export_grey)
                                changeGray(data.streamType,statusView,contentView)
                            }
                        }
                    }
                }
            }
        }

    }

    private fun changeRed(streamType: Int, statusView: ImageView,textView:TextView){//
        if (streamType == 0){
            statusView.setImageResource(R.drawable.msg_icon_stream_type_audio_red)
        }else{
            statusView.setImageResource(R.drawable.msg_icon_stream_type_video_red)
        }
        textView.setTextColor(Color.RED)
    }

    private fun changeGray(streamType: Int, statusView: ImageView,textView:TextView){
        if (streamType == 0){
            statusView.setImageResource(R.drawable.msg_icon_stream_type_audio_grey)
        }else{
            statusView.setImageResource(R.drawable.msg_icon_stream_type_video_grey)
        }
        textView.setTextColor(mContext.resources.getColor(R.color.c9b9b9b))
    }
}