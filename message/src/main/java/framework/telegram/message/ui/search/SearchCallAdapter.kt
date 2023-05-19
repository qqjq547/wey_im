package framework.telegram.message.ui.search

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.telegram.business.bridge.search.BaseSearchAdapter
import framework.telegram.business.bridge.search.StringUtil
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.bean.StreamCallItem
import framework.telegram.message.manager.ReceiveMessageManager
import framework.telegram.support.BaseApp
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.TimeUtils
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.videoplayer.utils.NetworkUtils

/**
 * Created by lzh on 19-5-21.
 * INFO:通讯录 adapter
 */
@Route(path = Constant.ARouter.ROUNTE_SERVICE_ADAPTER_CALL, name = "搜索通话记录")
class SearchCallAdapter : BaseSearchAdapter<MultiItemEntity, BaseViewHolder>(null) {
    override fun setSearchTargetId(targerId: Long) {
    }

    override fun setSearchType(searchType: Int) {

    }

    override fun setExtra(dataSet: Set<Long>) {
    }

    override fun setExtra(mapList: List<MutableMap<String, String>>) {
    }

    private var mKeyword = ""

    override fun setKeyword(keyword: String) {
        mKeyword = keyword
    }

    override fun getAdapter(): BaseSearchAdapter<MultiItemEntity, BaseViewHolder> = this

    override fun init(context: Context?) {
    }

    override fun addItems() {
        putLayout(Constant.Search.SEARCH_ITEM_STREAM_CALL, R.layout.msg_stream_call_history_item)
    }

    override fun convertImpl(helper: BaseViewHolder, item: MultiItemEntity) {
        when (item.itemType) {
            Constant.Search.SEARCH_ITEM_STREAM_CALL -> {
                if (item is StreamCallItem) {
                    helper.addOnClickListener(R.id.layout_root_view)
                    helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(item.data.chaterIcon)
                    helper.setText(R.id.app_text_view_name, StringUtil.setHitTextColor(mKeyword, item.data.chaterName
                            ?: ""))
                    helper.setText(R.id.text_view_time, TimeUtils.timeFormatToChat(BaseApp.app, item.data.reqTime))

                    helper.itemView.setOnClickListener {
                        if (NetworkUtils.isAvailable(BaseApp.app) && ReceiveMessageManager.socketIsLogin) {
                            AppDialog.showList(it.context,
                                    listOf(helper.itemView.context.getString(R.string.voice_communication), helper.itemView.context.getString(R.string.video_call))) { _, index, _ ->
                                if (index == 0) {
                                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO).withLong("targetUid", item.data.chaterId).withInt("streamType", 0).navigation()
                                } else if (index == 1) {
                                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO).withLong("targetUid", item.data.chaterId).withInt("streamType", 1).navigation()
                                }
                            }
                        } else {
                            BaseApp.app.toast(BaseApp.app.getString(R.string.socket_is_error))
                        }
                    }

                    if (item.data.status == 0 && item.data.isSend == 0) {
                        if (item.data.isSend == 1) {
                            helper.setImageResource(R.id.image_view_is_send, R.drawable.msg_icon_stream_call_export_grey)
                        } else {
                            helper.setImageResource(R.id.image_view_is_send, R.drawable.msg_icon_stream_call_import_red)
                        }

                        helper.setTextColor(R.id.app_text_view_status, ResourcesCompat.getColor(BaseApp.app.resources, R.color.f50d2e, null))
                        if (item.data.streamType == 0) {
                            helper.setImageResource(R.id.image_view_stream_type, R.drawable.msg_icon_stream_type_audio_red)
                        } else if (item.data.streamType == 1) {
                            helper.setImageResource(R.id.image_view_stream_type, R.drawable.msg_icon_stream_type_video_red)
                        }
                    } else {
                        if (item.data.isSend == 1) {
                            helper.setImageResource(R.id.image_view_is_send, R.drawable.msg_icon_stream_call_export_grey)
                        } else {
                            helper.setImageResource(R.id.image_view_is_send, R.drawable.msg_icon_stream_call_import_grey)
                        }

                        helper.setTextColor(R.id.app_text_view_status, ResourcesCompat.getColor(BaseApp.app.resources, R.color.a2a4a7, null))
                        if (item.data.streamType == 0) {
                            helper.setImageResource(R.id.image_view_stream_type, R.drawable.msg_icon_stream_type_audio_grey)
                        } else if (item.data.streamType == 1) {
                            helper.setImageResource(R.id.image_view_stream_type, R.drawable.msg_icon_stream_type_video_grey)
                        }
                    }

                    if (item.data.status == 0) {
                        if (item.data.isSend == 1) {
                            helper.setText(R.id.app_text_view_status, helper.itemView.context.getString(R.string.no_answer))
                        } else {
                            if (item.nearCount == 0) {
                                helper.setText(R.id.app_text_view_status, helper.itemView.context.getString(R.string.did_not_answer))
                            } else {
                                helper.setText(R.id.app_text_view_status, String.format(helper.itemView.context.getString(R.string.did_not_answer_mat), item.nearCount + 1))
                            }
                        }
                    } else if (item.data.status == 1 || item.data.status == 5) {
                        val calledTime = if (item.data.endTime > item.data.startTime) item.data.endTime - item.data.startTime else 0
                        helper.setText(R.id.app_text_view_status, String.format(helper.itemView.context.getString(R.string.call_duration_mat), TimeUtils.timeFormatToMediaDuration(calledTime)))
                    } else if (item.data.status == 2) {
                        if (item.data.isSend == 1) {
                            helper.setText(R.id.app_text_view_status, helper.itemView.context.getString(R.string.the_other_party_has_refused))
                        } else {
                            helper.setText(R.id.app_text_view_status, helper.itemView.context.getString(R.string.denied))
                        }
                    } else if (item.data.status == 3) {
                        if (item.data.isSend == 1) {
                            helper.setText(R.id.app_text_view_status, helper.itemView.context.getString(R.string.canceled))
                        } else {
                            helper.setText(R.id.app_text_view_status, helper.itemView.context.getString(R.string.counterparty_canceled))
                        }
                    } else if (item.data.status == 4) {
                        helper.setText(R.id.app_text_view_status, helper.itemView.context.getString(R.string.the_other_is_busy))
                    } else {
                        helper.setText(R.id.app_text_view_status, helper.itemView.context.getString(R.string.error_condition))
                    }
                }
            }
        }
    }
}

