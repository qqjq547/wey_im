package framework.telegram.business.http.protocol;

import com.im.domain.pb.GroupProto;

import framework.telegram.support.system.network.http.HttpReq;
import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface GroupHttpProtocol {

    /**
     * 创建群聊req
     */
    @POST("group/groupCreate")
    Observable<GroupProto.GroupCreateResp> groupCreate(@Body HttpReq<GroupProto.GroupCreateReq> req);

    @POST("group/groupUpdate")
    Observable<GroupProto.GroupUpdateResp> groupUpdate(@Body HttpReq<GroupProto.GroupUpdateReq> req);

    /**
     * 我的群聊列表req
     */
    @POST("group/groupContactList")
    Observable<GroupProto.GroupContactListResp> getGroupList(@Body HttpReq<GroupProto.GroupContactListReq> req);

    @POST("group/groupDetail")
    Observable<GroupProto.GroupDetailResp> groupDetail(@Body HttpReq<GroupProto.GroupDetailReq> req);

    @POST("group/groupMemberList")
    Observable<GroupProto.GroupMemberListResp> groupMemberList(@Body HttpReq<GroupProto.GroupMemberListReq> req);

    @POST("group/groupExit")
    Observable<GroupProto.GroupExitResp> groupExit(@Body HttpReq<GroupProto.GroupExitReq> req);

    @POST("group/groupMember")
    Observable<GroupProto.GroupMemberResp> updateGroupMember(@Body HttpReq<GroupProto.GroupMemberReq> req);

    @POST("group/groupTransfer")
    Observable<GroupProto.GroupTransferResp> groupTransfer(@Body HttpReq<GroupProto.GroupTransferReq> req);

    @POST("group/groupReqList")
    Observable<GroupProto.GroupReqListResp> groupReqList(@Body HttpReq<GroupProto.GroupReqListReq> req);

    @POST("group/groupCheckJoin")
    Observable<GroupProto.GroupCheckJoinResp> groupCheckJoin(@Body HttpReq<GroupProto.GroupCheckJoinReq> req);

    @POST("group/groupUserCheckJoin")
    Observable<GroupProto.GroupUserCheckJoinResp> groupUserCheckJoin(@Body HttpReq<GroupProto.GroupUserCheckJoinReq> req);

    @POST("group/groupQrCode")
    Observable<GroupProto.GroupQrCodeResp> getGroupQrCode(@Body HttpReq<GroupProto.GroupQrCodeReq> req);

    @POST("group/groupDetailFromQrCode")
    Observable<GroupProto.GroupDetailFromQrCodeResp> groupDetailFromQrCode(@Body HttpReq<GroupProto.GroupDetailFromQrCodeReq> req);

    @POST("group/groupJoin")
    Observable<GroupProto.GroupJoinResp> groupJoin(@Body HttpReq<GroupProto.GroupJoinReq> req);

    @POST("group/groupMemberDetail")
    Observable<GroupProto.GroupMemberDetailResp> groupMemberDetail(@Body HttpReq<GroupProto.GroupMemberDetailReq> req);

    @POST("group/delGroupReqRecord")
    Observable<GroupProto.DelGroupReqRecordResp> delGroupReqRecord(@Body HttpReq<GroupProto.DelGroupReqRecordReq> req);

    /**
     * 群管理列表
     * @param req
     * @return
     */
    @POST("group/groupAdminList")
    Observable<GroupProto.GroupAdminListResp> groupAdminList(@Body HttpReq<GroupProto.GroupAdminListReq> req);

    /**
     * 移除管理员
     * @param req
     * @return
     */
    @POST("group/groupRemoveAdmin")
    Observable<GroupProto.GroupRemoveAdminResp> groupRemoveAdmin(@Body HttpReq<GroupProto.GroupRemoveAdminReq> req);

    /**
     * 编辑管理员权限
     * @param req
     * @return
     */
    @POST("group/groupEditAdminRight")
    Observable<GroupProto.GroupEditAdminRightResp> groupEditAdminRight(@Body HttpReq<GroupProto.GroupEditAdminRightReq> req);

    /**
     * 获取群公告详情
     * @param req
     * @return
     */
    @POST("group/groupNoticeDetail")
    Observable<GroupProto.GroupNoticeDetailResp> groupNoticeDetail(@Body HttpReq<GroupProto.GroupNoticeDetailReq> req);


    /**
     * 好友共同群聊列表
     * @param req
     * @return
     */
    @POST("group/friendCommonGroupList")
    Observable<GroupProto.FriendCommonGroupListResp> friendCommonGroupList(@Body HttpReq<GroupProto.FriendCommonGroupListReq> req);

    /**
     * 群成员禁言
     * @param req
     * @return
     */
    @POST("group/groupMemberShutup")
    Observable<GroupProto.GroupMemberShutupResp> setGroupMemberBanWord(@Body HttpReq<GroupProto.GroupMemberShutupReq> req);

    @POST("group/clearGroupReq")
    Observable<GroupProto.ClearGroupResp> clearGroupNotification(@Body HttpReq<GroupProto.ClearGroupReq> req);

    @POST("group/disableGroup")
    Observable<GroupProto.DisableGroupResp> disableGroup(@Body HttpReq<GroupProto.DisableGroupReq> req);

    /**
     * 群投诉req
     */
    @POST("group/groupReport")
    Observable<GroupProto.GroupReportResp> getComplanintGroup(@Body HttpReq<GroupProto.GroupReportReq> req);

    @POST("group/searchGroupMember")
    Observable<GroupProto.SearchGroupMemberResp> getSearchGroupMember(@Body HttpReq<GroupProto.SearchGroupMemberReq> req);
}

