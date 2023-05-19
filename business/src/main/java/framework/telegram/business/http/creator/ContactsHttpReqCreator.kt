package framework.telegram.business.http.creator

import com.im.domain.pb.CommonProto
import com.im.domain.pb.ContactsProto
import com.im.domain.pb.GroupProto
import framework.telegram.business.http.getClientInfo


class ContactsHttpReqCreator {

    companion object {
        /**
         * 获取联系人列表
         */
        fun createContactsListReq(pageNum: Int, pageSize: Int): ContactsProto.ContactsListReq {
            return ContactsProto.ContactsListReq.newBuilder().setClientInfo(getClientInfo())
                .setPageNum(pageNum).setPageSize(pageSize).build()
        }

        /**
         * 联系人详情req
         */
        fun createContactsDetail(targetUid: Long, groupId: Long): ContactsProto.ContactsDetailReq {
            return ContactsProto.ContactsDetailReq.newBuilder().setClientInfo(getClientInfo())
                .setTargetUid(targetUid).setGroupId(groupId).build()
        }

        /**
         * 查找联系人req
         */
        fun findContacts(
            phoneNum: String,
            userId: Long,
            findSign: String
        ): ContactsProto.FindContactsListReq {
            return ContactsProto.FindContactsListReq.newBuilder().setClientInfo(getClientInfo())
                .setPhoneNum(phoneNum).setTargetUid(userId).setFindSign(findSign).build()
        }

        /**
         * 更新联系人关系req
         */
        fun createAddRelation(
            targetUid: Long,
            groupId: Long,
            msg: String,
            type: ContactsProto.ContactsAddType,
            op: ContactsProto.ContactsOperator,
            addToken: String
        ): ContactsProto.ContactsRelationReq {
            return ContactsProto.ContactsRelationReq.newBuilder().setClientInfo(getClientInfo())
                .setTargetUid(targetUid).setGroupId(groupId).setMsg(msg).setType(type).setOp(op)
                .setAddToken(addToken).build()
        }

        fun createDeleteRelation(
            targetUid: Long,
            op: ContactsProto.ContactsOperator
        ): ContactsProto.ContactsRelationReq {
            return ContactsProto.ContactsRelationReq.newBuilder().setClientInfo(getClientInfo())
                .setTargetUid(targetUid).setOp(op).build()
        }

        /**
         * 联系人申请列表req
         */
        fun getContactsApplyList(): ContactsProto.ContactsApplyListReq {
            return ContactsProto.ContactsApplyListReq.newBuilder().setClientInfo(getClientInfo())
                .build()
        }

        /**
         * 待审核联系人详情req
         */
        fun createUnAuditDetail(recordId: Long): ContactsProto.ContactsUnAuditDetailReq {
            return ContactsProto.ContactsUnAuditDetailReq.newBuilder()
                .setClientInfo(getClientInfo())
                .setApplyUid(recordId).build()
        }

        /**
         * 更新好友申请req
         */
        fun createContactsApply(
            recordId: Long,
            op: ContactsProto.ContactsOperator
        ): ContactsProto.UpdateContactsApplyReq {
            return ContactsProto.UpdateContactsApplyReq.newBuilder().setClientInfo(getClientInfo())
                .setApplyUid(recordId).setOp(op).build()
        }

        /**
         * 拉黑/移除拉黑联系人req
         */
        fun createBlackContacts(
            targetUid: Long,
            op: ContactsProto.ContactsOperator
        ): ContactsProto.UpdateBlackContactsReq {
            return ContactsProto.UpdateBlackContactsReq.newBuilder().setClientInfo(getClientInfo())
                .setTargetUid(targetUid).setOp(op).build()
        }

        /**
         * 更新联系人详情req
         */
        fun updateContacts(
            param: ContactsProto.ContactsParam,
            op: ContactsProto.ContactsOperator
        ): ContactsProto.UpdateContactsReq {
            return ContactsProto.UpdateContactsReq.newBuilder().setClientInfo(getClientInfo())
                .setParam(param).setOp(op).build()
        }

        /**
         * 上传手机通讯录req
         */
        fun uploadContacts(
            bfFirst: Boolean,
            bfEnd: Boolean,
            list: MutableList<ContactsProto.UploadMobileParam>
        ): ContactsProto.UploadContactsReq {
            return ContactsProto.UploadContactsReq.newBuilder().addAllMobileList(list)
                .setClientInfo(getClientInfo()).setBfFirst(bfFirst).setBfEnd(bfEnd)
                .build()
        }

        /**
         * 获取手机通讯录req
         */
        fun getMobileContacts(pageNum: Int, pageSize: Int): ContactsProto.MobileContactsReq {
            return ContactsProto.MobileContactsReq.newBuilder().setPageNum(pageNum)
                .setPageSize(pageSize)
                .setClientInfo(getClientInfo()).build()
        }

        /**
         * 获取黑名单列表req
         */
        fun createBlackContacts(pageNum: Int, pageSize: Int): ContactsProto.BlackListReq {
            return ContactsProto.BlackListReq.newBuilder().setPageNum(pageNum).setPageSize(pageSize)
                .setClientInfo(getClientInfo()).build()
        }

        /**
         * 获取联系人投诉req
         */
        fun createComplaintContacts(
            targetUid: Long,
            msg: String,
            type: Int,
            picture: String
        ): ContactsProto.ComplaintContactsReq {
            var realType = ContactsProto.ComplaintType.BAD_MSG
            when (type) {
                1 -> {
                    realType = ContactsProto.ComplaintType.BAD_MSG
                }
                2 -> {
                    realType = ContactsProto.ComplaintType.HARASS
                }
                3 -> {
                    realType = ContactsProto.ComplaintType.VIOLATION
                }
            }
            return ContactsProto.ComplaintContactsReq.newBuilder().setType(realType)
                .setTargetUid(targetUid).setMsg(msg).setPicUrls(picture)
                .setClientInfo(getClientInfo()).build()
        }

        /**
         * 获取群投诉req
         */
        fun createComplaintGr(
            targetUid: Long,
            msg: String,
            type: Int,
            picture: String
        ): GroupProto.GroupReportReq {
            return GroupProto.GroupReportReq.newBuilder().setType(type).setGroupId(targetUid)
                .setContent(msg).setPicUrls(picture)
                .setClientInfo(getClientInfo()).build()
        }

        fun createGroupSearch(
            groupId: Long,
            keyword: String,
            pageNum: Int,
            pageSize: Int,
            filterType: CommonProto.FilterType
        ): GroupProto.SearchGroupMemberReq {
            return GroupProto.SearchGroupMemberReq.newBuilder().setGroupId(groupId)
                .setKeyword(keyword).setPageNum(pageNum).setPageSize(pageSize)
                .setFilterType(filterType.ordinal)
                .setClientInfo(getClientInfo()).build()
        }

        fun createContactDetailFromQr(
            targetUid: Long,
            qrCode: String
        ): ContactsProto.ContactsDetailFromQrCodeReq {
            return ContactsProto.ContactsDetailFromQrCodeReq.newBuilder().setTargetUid(targetUid)
                .setQrCode(qrCode)
                .setClientInfo(getClientInfo()).build()
        }

    }
}