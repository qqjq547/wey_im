package framework.telegram.message.http.creator

import com.im.domain.pb.GroupProto
import framework.telegram.message.http.getClientInfo


class MessageHttpReqCreator {

    companion object {

        fun createGroupMsgReceipt(messageId: Long, groupId: Long, lastTime: Long): GroupProto.GroupMsgReceiptReq {
            return GroupProto.GroupMsgReceiptReq.newBuilder().setClientInfo(getClientInfo())
                    .setPageSize(20).setLastTime(lastTime)
                    .setGroupId(groupId).setMsgId(messageId).build()
        }
    }
}