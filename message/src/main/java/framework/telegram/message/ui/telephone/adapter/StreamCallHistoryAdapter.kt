package framework.telegram.message.ui.telephone.adapter

import android.annotation.SuppressLint
import android.text.TextPaint
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.chad.library.adapter.base.BaseViewHolder
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.StreamCallModel
import framework.telegram.message.R
import framework.telegram.message.bridge.bean.StreamCallItem
import framework.telegram.support.BaseApp
import framework.telegram.support.tools.TimeUtils
import framework.telegram.ui.doubleclick.recycler.AppBaseQuickAdapter
import framework.telegram.ui.image.AppImageView

class StreamCallHistoryAdapter : AppBaseQuickAdapter<StreamCallItem, BaseViewHolder>(R.layout.msg_stream_call_history_item) {

    private val mCheckedMessageList by lazy { LinkedHashMap<String, StreamCallItem>() }

    private var mCheckedMessageListener: ((Int) -> Unit)? = null

    var isMultiCheckMode = false

    fun setCheckable(msg: StreamCallItem, checkedMessageListener: ((Int) -> Unit)? = null) {
        isMultiCheckMode = true
        mCheckedMessageList.clear()
        mCheckedMessageList[msg.data.sessionId] = msg
        mCheckedMessageListener = checkedMessageListener
        mCheckedMessageListener?.invoke(mCheckedMessageList.size)
        notifyDataSetChanged()
    }

    fun setUnCheckable() {
        isMultiCheckMode = false
        mCheckedMessageList.clear()
        mCheckedMessageListener = null
        notifyDataSetChanged()
    }

    fun setAllChecked(isChecked: Boolean): Int {
        if (isChecked) {
            mCheckedMessageList.clear()
            data.forEach { msgModel ->
                mCheckedMessageList[msgModel.data.sessionId] = msgModel
            }
            notifyDataSetChanged()
        } else {
            mCheckedMessageList.clear()
            notifyDataSetChanged()
        }

        return mCheckedMessageList.size
    }

    fun getCheckableMessages(): ArrayList<StreamCallItem> {
        return ArrayList(mCheckedMessageList.values)
    }

    private val mOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        val msgModel = buttonView.tag as StreamCallItem
        if (!isChecked) {
            mCheckedMessageList.remove(msgModel.data.sessionId)
        } else {
            mCheckedMessageList[msgModel.data.sessionId] = msgModel
        }
        mCheckedMessageListener?.invoke(mCheckedMessageList.size)
    }

    private fun bindCheckBox(helper: BaseViewHolder, chatModel: StreamCallItem) {
        val checkbox = helper.getView<CheckBox>(R.id.check_box_msg)
        if (checkbox != null && isMultiCheckMode) {
            checkbox.visibility = View.VISIBLE
            checkbox.tag = chatModel
            checkbox.setOnCheckedChangeListener(null)
            var finded = false
            mCheckedMessageList.forEach {
                if (it.key == chatModel.data.sessionId) {
                    finded = true
                }
            }
            checkbox.isChecked = finded
            checkbox.setOnCheckedChangeListener(mOnCheckedChangeListener)
        } else {
            checkbox?.visibility = View.GONE
        }
    }

    @SuppressLint("SetTextI18n")
    override fun convert(helper: BaseViewHolder, content: StreamCallItem?) {
        if (content == null) {
            return
        }

        bindCheckBox(helper, content)

        helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(content.data.chaterIcon)
        helper.setText(R.id.app_text_view_name, content.data.chaterName)
        helper.setText(R.id.text_view_time, TimeUtils.timeFormatToChat(BaseApp.app, content.data.reqTime))

        if ((content.data.status == 0 || content.data.status == 3)) {
            if (content.data.isSend == 1) {
                helper.setTextColor(R.id.app_text_view_status, ResourcesCompat.getColor(BaseApp.app.resources, R.color.a2a4a7, null))
                helper.setImageResource(R.id.image_view_is_send, R.drawable.msg_icon_stream_call_export_grey)
                if (content.data.streamType == 0) {
                    helper.setImageResource(R.id.image_view_stream_type, R.drawable.msg_icon_stream_type_audio_grey)
                } else if (content.data.streamType == 1) {
                    helper.setImageResource(R.id.image_view_stream_type, R.drawable.msg_icon_stream_type_video_grey)
                }
            } else {
                helper.setTextColor(R.id.app_text_view_status, ResourcesCompat.getColor(BaseApp.app.resources, R.color.f50d2e, null))
                helper.setImageResource(R.id.image_view_is_send, R.drawable.msg_icon_stream_call_import_red)
                if (content.data.streamType == 0) {
                    helper.setImageResource(R.id.image_view_stream_type, R.drawable.msg_icon_stream_type_audio_red)
                } else if (content.data.streamType == 1) {
                    helper.setImageResource(R.id.image_view_stream_type, R.drawable.msg_icon_stream_type_video_red)
                }
            }
        } else {
            if (content.data.isSend == 1) {
                helper.setImageResource(R.id.image_view_is_send, R.drawable.msg_icon_stream_call_export_grey)
            } else {
                helper.setImageResource(R.id.image_view_is_send, R.drawable.msg_icon_stream_call_import_grey)
            }

            helper.setTextColor(R.id.app_text_view_status, ResourcesCompat.getColor(BaseApp.app.resources, R.color.a2a4a7, null))
            if (content.data.streamType == 0) {
                helper.setImageResource(R.id.image_view_stream_type, R.drawable.msg_icon_stream_type_audio_grey)
            } else if (content.data.streamType == 1) {
                helper.setImageResource(R.id.image_view_stream_type, R.drawable.msg_icon_stream_type_video_grey)
            }
        }

        if (content.data.status == 0) {
            if (content.data.isSend == 1) {
//                helper.setText(R.id.app_text_view_status, mContext.getString(R.string.no_answer))
                helper.setText(R.id.app_text_view_status, mContext.getString(R.string.canceled))
            } else {
                helper.setText(R.id.app_text_view_status, mContext.getString(R.string.did_not_answer))
            }
        } else if (content.data.status == 1 || content.data.status == 5) {
            val calledTime = if (content.data.endTime > content.data.startTime) content.data.endTime - content.data.startTime else 0
            helper.setText(R.id.app_text_view_status, String.format(mContext.getString(R.string.call_duration_mat), TimeUtils.timeFormatToMediaDuration(calledTime)))
        } else if (content.data.status == 2) {
            if (content.data.isSend == 1) {
                helper.setText(R.id.app_text_view_status, mContext.getString(R.string.the_other_party_has_refused))
            } else {
                helper.setText(R.id.app_text_view_status, mContext.getString(R.string.denied))
            }
        } else if (content.data.status == 3) {
            if (content.data.isSend == 1) {
                helper.setText(R.id.app_text_view_status, mContext.getString(R.string.canceled))
            } else {
                helper.setText(R.id.app_text_view_status, mContext.getString(R.string.did_not_answer))
            }
        } else if (content.data.status == 4) {
            helper.setText(R.id.app_text_view_status, mContext.getString(R.string.the_other_is_busy))
        } else {
            helper.setText(R.id.app_text_view_status, mContext.getString(R.string.error_condition))
        }

        if (content.nearCount > 0) {
            val statusView = helper.getView<TextView>(R.id.app_text_view_status)
            statusView.text = statusView.text.toString() + "(${content.nearCount + 1})"
        }

        if (isMultiCheckMode) {
            helper.getView<View>(R.id.image_view_stream_call_detail).visibility = View.GONE
        } else {
            helper.getView<View>(R.id.image_view_stream_call_detail).visibility = View.VISIBLE
        }

        helper.addOnClickListener(R.id.image_view_stream_call_detail)
    }
}