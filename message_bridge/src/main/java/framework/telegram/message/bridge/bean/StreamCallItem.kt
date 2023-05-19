package framework.telegram.message.bridge.bean

import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.im.StreamCallModel

class StreamCallItem(var data: StreamCallModel, var nearCount: Int = 0) : MultiItemEntity {
    override fun getItemType(): Int {
        return data.itemType
    }

    var nearSessionIdList = arrayListOf<String>()
}