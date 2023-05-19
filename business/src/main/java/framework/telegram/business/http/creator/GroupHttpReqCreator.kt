package framework.telegram.business.http.creator

import android.text.TextUtils
import com.im.domain.pb.CommonProto
import com.im.domain.pb.GroupProto
import framework.telegram.business.http.getClientInfo


class GroupHttpReqCreator {

    companion object {
        /**
         * 我的群聊列表列表
         */
        fun createGroupListReq(): GroupProto.GroupContactListReq {
            return GroupProto.GroupContactListReq.newBuilder().setClientInfo(getClientInfo()).build()
        }

        /**
         * 创建群聊
         */
        fun createGroupCreateReq(memberIds: List<Long>, groupName: String, pic: String?): GroupProto.GroupCreateReq {
            val reqParams = GroupProto.GroupCreateReq.newBuilder().setClientInfo(getClientInfo())
                    .addAllMembers(memberIds)
                    .setGroupName(groupName)
            if (!TextUtils.isEmpty(pic))
                reqParams.pic = pic
            return reqParams.build()
        }

        fun createGroupUpdateReq(op: GroupProto.GroupOperator, groupParam: GroupProto.GroupParam): GroupProto.GroupUpdateReq {
            return GroupProto.GroupUpdateReq.newBuilder().setClientInfo(getClientInfo())
                    .setOp(op).setGroupParam(groupParam).build()
        }

        fun createGroupDetailReq(groupId: Long): GroupProto.GroupDetailReq {
            return GroupProto.GroupDetailReq.newBuilder().setClientInfo(getClientInfo()).setGroupId(groupId).build()
        }

        fun createGroupNoticeDetail(groupId: Long,noticeId: Long): GroupProto.GroupNoticeDetailReq {
            return GroupProto.GroupNoticeDetailReq.newBuilder().setClientInfo(getClientInfo()).setGroupId(groupId).setNoticeId(noticeId).build()
        }

        fun createGroupMemberListReq(groupId: Long, lastUpdateTime: Long, pageNo: Int, pageSize: Int): GroupProto.GroupMemberListReq {
            return GroupProto.GroupMemberListReq.newBuilder().setClientInfo(getClientInfo()).setGroupId(groupId).setTime(lastUpdateTime).setPageNum(pageNo).setPageSize(pageSize).build()
        }

        fun createGroupExitReq(groupId: Long): GroupProto.GroupExitReq {
            return GroupProto.GroupExitReq.newBuilder().setClientInfo(getClientInfo()).setGroupId(groupId).build()
        }

        fun createUpdateGroupMemberReq(groupId: Long, op: GroupProto.GroupOperator, members: List<Long>): GroupProto.GroupMemberReq {
            return GroupProto.GroupMemberReq.newBuilder().setClientInfo(getClientInfo()).setGroupId(groupId).setOp(op).addAllMembers(members).build()
        }

        fun createGroupTransferReq(groupId: Long, uid: Long): GroupProto.GroupTransferReq {
            return GroupProto.GroupTransferReq.newBuilder().setClientInfo(getClientInfo()).setGroupId(groupId).setTransferUid(uid).build()
        }

        fun createGroupReqListReq(pageNum: Int, pageSize: Int): GroupProto.GroupReqListReq {
            return GroupProto.GroupReqListReq.newBuilder().setClientInfo(getClientInfo()).setPageNum(pageNum).setPageSize(pageSize).build()
        }

        fun createGroupCheckJoinReq(groupReqId: Long, operate: Boolean): GroupProto.GroupCheckJoinReq {
            return GroupProto.GroupCheckJoinReq.newBuilder().setClientInfo(getClientInfo()).setGroupReqId(groupReqId).setFlag(operate).build()
        }

        fun createGroupUserCheckJoinReq(groupReqId: Long, operate: Boolean): GroupProto.GroupUserCheckJoinReq {
            return GroupProto.GroupUserCheckJoinReq.newBuilder().setClientInfo(getClientInfo()).setGroupReqId(groupReqId).setFlag(operate).build()
        }

        fun createGroupQrReq(groupId: Long, force: Boolean): GroupProto.GroupQrCodeReq {
            return GroupProto.GroupQrCodeReq.newBuilder().setClientInfo(getClientInfo()).setGroupId(groupId).setForce(force).build()
        }

        fun createGroupDetailFromQrCodeReq(groupId: Long, qrCode: String,idCode:String): GroupProto.GroupDetailFromQrCodeReq {
            return GroupProto.GroupDetailFromQrCodeReq.newBuilder().setClientInfo(getClientInfo()).setGroupId(groupId).setQrCode(qrCode).setIdCode(idCode).build()
        }

        fun createGroupJoinReq(groupId: Long, msg: String, reqType: CommonProto.GroupReqType, addToken: String): GroupProto.GroupJoinReq {
            return GroupProto.GroupJoinReq.newBuilder().setClientInfo(getClientInfo()).setGroupId(groupId).setMsg(msg).setReqType(reqType).setAddToken(addToken).build()
        }

        /**
         * 群成员详情请求 [groupUrl/group/groupMemberDetail]
         */
        fun createGroupMemberDetail(groupId: Long, memberUids: List<Long>): GroupProto.GroupMemberDetailReq {
            return GroupProto.GroupMemberDetailReq.newBuilder().setClientInfo(getClientInfo()).setGroupId(groupId).addAllMemberUids(memberUids).build()
        }

        fun createDelGroupReqRecord(groupReqId: Long): GroupProto.DelGroupReqRecordReq {
            return GroupProto.DelGroupReqRecordReq.newBuilder().setClientInfo(getClientInfo()).setGroupReqId(groupReqId).build()
        }

        fun createGroupAdminListReqRecord(groupReqId: Long): GroupProto.GroupAdminListReq {
            return GroupProto.GroupAdminListReq.newBuilder().setClientInfo(getClientInfo()).setGroupId(groupReqId).build()
        }

        fun createGroupRemoveAdminReqRecord(groupReqId: Long,adminUid:Long): GroupProto.GroupRemoveAdminReq {
            return GroupProto.GroupRemoveAdminReq.newBuilder().setClientInfo(getClientInfo()).setGroupId(groupReqId).setAdminUid(adminUid).build()
        }

        fun createGroupEditAdminRightReqRecord(groupReqId: Long,targetUid: Long,adminRightBase : CommonProto.AdminRightBase, groupOperator: GroupProto.GroupOperator): GroupProto.GroupEditAdminRightReq {
            return GroupProto.GroupEditAdminRightReq.newBuilder().setClientInfo(getClientInfo()).setGroupId(groupReqId).setTargetUid(targetUid).setRightParam(adminRightBase).setOp(groupOperator).build()
        }

        fun createSameGroupChat(targetUid: Long,page:Int,pageSize:Int): GroupProto.FriendCommonGroupListReq {
            return GroupProto.FriendCommonGroupListReq.newBuilder().setClientInfo(getClientInfo()).setContactsId(targetUid).setPageNum(page).setPageSize(pageSize).build()
        }

        fun createGroupMemberBanword( groupId: Long, targetUid: Long, time: Int): GroupProto.GroupMemberShutupReq {
            return GroupProto.GroupMemberShutupReq.newBuilder().setClientInfo(getClientInfo()).setGroupId(groupId).setTargetUid(targetUid).setShutupTime(time).build()
        }

        fun createClearGroupNotification( ): GroupProto.ClearGroupReq {
            return GroupProto.ClearGroupReq.newBuilder().setClientInfo(getClientInfo()).build()
        }

        fun createDisableGroup( groupId: Long): GroupProto.DisableGroupReq {
            return GroupProto.DisableGroupReq.newBuilder().setClientInfo(getClientInfo()).setGroupId(groupId).build()
        }

    }
}