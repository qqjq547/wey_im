package framework.telegram.message.ui.search

import android.content.Context
import android.text.TextPaint
import android.text.TextUtils
import android.widget.ImageView
import android.widget.TextView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.bridge.search.BaseSearchAdapter
import framework.telegram.business.bridge.search.StringUtil
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.manager.ReceiveMessageManager
import framework.telegram.support.BaseApp
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.videoplayer.utils.NetworkUtils

/**
 * Created by lzh on 19-5-21.
 * INFO:通讯录 adapter
 */
@Route(path = Constant.ARouter.ROUNTE_SERVICE_ADAPTER_NEW_CALL, name = "搜索个人聊天")
class SearchNewCallAdapter : BaseSearchAdapter<MultiItemEntity, BaseViewHolder>(null) {
    override fun setSearchTargetId(targerId: Long) {
    }

    override fun setExtra(dataSet: Set<Long>) {
    }

    override fun setSearchType(searchType: Int) {
    }

    override fun setExtra(mapList: List<MutableMap<String, String>>) {
    }

    override fun init(context: Context?) {

    }

    private var mKeyword = ""

    override fun setKeyword(keyword: String) {
        mKeyword = keyword
    }

    override fun getAdapter(): BaseSearchAdapter<MultiItemEntity, BaseViewHolder> = this

    override fun addItems() {
        putLayout(Constant.Search.CONTACT_ITEM_TYPE, R.layout.msg_contacts_item)
    }

    override fun convertImpl(helper: BaseViewHolder, item: MultiItemEntity) {
        when (item.itemType) {
            Constant.Search.CONTACT_ITEM_TYPE -> {
                item.let {
                    val itemData = item as ContactDataModel
                    helper.setText(R.id.app_text_view_name, StringUtil.setHitTextColor(mKeyword, itemData.displayName
                            ?: ""))
                    (helper.getView<TextView>(R.id.app_text_view_name)?.paint as TextPaint).isFakeBoldText = true
                    helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(UriUtils.parseUri(itemData.icon))

                    helper.getView<ImageView>(R.id.image_view_audio).setOnClickListener {
                        if (NetworkUtils.isAvailable(BaseApp.app) && ReceiveMessageManager.socketIsLogin) {
                            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO).withLong("targetUid", itemData.uid).withInt("streamType", 0).navigation()
                        } else {
                            BaseApp.app.toast(BaseApp.app.getString(R.string.socket_is_error))
                        }
                    }
                    helper.getView<ImageView>(R.id.image_view_video).setOnClickListener {
                        if (NetworkUtils.isAvailable(BaseApp.app) && ReceiveMessageManager.socketIsLogin) {
                            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO).withLong("targetUid", itemData.uid).withInt("streamType", 1).navigation()
                        } else {
                            BaseApp.app.toast(BaseApp.app.getString(R.string.socket_is_error))
                        }
                    }
                }
            }
        }
    }
}

