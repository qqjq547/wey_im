package framework.telegram.business.http.protocol;

import com.im.domain.pb.ContactsProto;
import com.im.domain.pb.GroupProto;

import framework.telegram.support.system.network.http.HttpReq;
import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface FriendHttpProtocol {

    /**
     * 联系人列表req
     */
    @POST("contacts/contactsList")
    Observable<ContactsProto.ContactsListResp> getContactsList(@Body HttpReq<ContactsProto.ContactsListReq> req);

    /**
     * 联系人详情req
     */
    @POST("contacts/contactsDetail")
    Observable<ContactsProto.ContactsDetailResp> getContactsDetail(@Body HttpReq<ContactsProto.ContactsDetailReq> req);

    /**
     * 查找联系人req
     */
    @POST("contacts/findContactsList")
    Observable<ContactsProto.FindContactsListResp> findContacts(@Body HttpReq<ContactsProto.FindContactsListReq> req);

    /**
     * 更新联系人关系req
     */
    @POST("contacts/contactsRelation")
    Observable<ContactsProto.ContactsRelationResp> getContactsRelation(@Body HttpReq<ContactsProto.ContactsRelationReq> req);

    /**
     * 联系人申请列表req
     */
    @POST("contacts/contactsApplyList")
    Observable<ContactsProto.ContactsApplyListResp> getContactsApplyList(@Body HttpReq<ContactsProto.ContactsApplyListReq> req);

    /**
     * 待审核联系人详情req
     */
    @POST("contacts/unAuditDetail")
    Observable<ContactsProto.ContactsUnAuditDetailResp> getUnAuditDetail(@Body HttpReq<ContactsProto.ContactsUnAuditDetailReq> req);

    /**
     * 更新好友申请req
     */
    @POST("contacts/updateContactsApply")
    Observable<ContactsProto.UpdateContactsApplyResp> updateContactsApply(@Body HttpReq<ContactsProto.UpdateContactsApplyReq> req);

    /**
     * 拉黑/移除拉黑联系人req
     */
    @POST("contacts/updateBlackContacts")
    Observable<ContactsProto.UpdateBlackContactsResp> updateBlackContacts(@Body HttpReq<ContactsProto.UpdateBlackContactsReq> req);

    /**
     * 更新联系人详情req
     */
    @POST("contacts/updateContacts")
    Observable<ContactsProto.UpdateContactsResp> updateContacts(@Body HttpReq<ContactsProto.UpdateContactsReq> req);

    /**
     * 上传手机通讯录req
     */
    @POST("contacts/uploadContacts")
    Observable<ContactsProto.UploadContactsResp> uploadContacts(@Body HttpReq<ContactsProto.UploadContactsReq> req);

    /**
     * 获取手机通讯录req
     */
    @POST("contacts/mobileContacts")
    Observable<ContactsProto.MobileContactsResp> getMobileContacts(@Body HttpReq<ContactsProto.MobileContactsReq> req);

    /**
     * 获取黑名单列表req
     */
    @POST("contacts/blackList")
    Observable<ContactsProto.BlackListResp> getBlackContacts(@Body HttpReq<ContactsProto.BlackListReq> req);

    /**
     * 联系人投诉req
     */
    @POST("contacts/complaintContacts")
    Observable<ContactsProto.ComplaintContactsResp> getComplanintContacts(@Body HttpReq<ContactsProto.ComplaintContactsReq> req);

    /**
     * 二维码获取联系人详情req
     */
    @POST("contacts/contactsDetailFromQrCode")
    Observable<ContactsProto.ContactsDetailFromQrCodeResp> getContactDetailFromQr(@Body HttpReq<ContactsProto.ContactsDetailFromQrCodeReq> req);
}
