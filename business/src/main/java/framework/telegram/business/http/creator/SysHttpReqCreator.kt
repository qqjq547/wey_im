package framework.telegram.business.http.creator

import com.im.domain.pb.CommonProto
import com.im.domain.pb.SysProto
import com.im.domain.pb.UploadFileProto
import framework.telegram.business.http.getClientInfo
import framework.telegram.business.http.getClientInfoWithOutSessionId

class SysHttpReqCreator {

    companion object {

        fun createGetSmsCodeReq(phoneNum: String, type: CommonProto.GetSmsCodeType, countryCode: String, flag: Int): SysProto.GetSmsCodeReq {
            val clientInfo = getClientInfo()
            return SysProto.GetSmsCodeReq.newBuilder().setClientInfo(clientInfo).setCountryCode(countryCode).setPhone(phoneNum).setType(type).setFlag(flag).build()
        }

        fun createCheckSmsCodeReq(countryCode: String, phone: String, smsCode: String, type: CommonProto.GetSmsCodeType): SysProto.CheckSmsCodeReq {
            return SysProto.CheckSmsCodeReq.newBuilder().setClientInfo(getClientInfo()).setType(type)
                    .setCountryCode(countryCode).setPhone(phone).setSmsCode(smsCode).build()
        }

        fun getToken(type: CommonProto.AttachType, spaceType: CommonProto.AttachWorkSpaceType): SysProto.GetTokenReq {
            val clientInfo = getClientInfoWithOutSessionId()
            return SysProto.GetTokenReq.newBuilder().setClientInfo(clientInfo).setAttachType(type).setAttachWorkspaceType(spaceType).build()
        }

        fun createFeedBackReq(feedbackType: CommonProto.FeedbackType, msg: String,picUrls:String): SysProto.FeedbackReq {
            return SysProto.FeedbackReq.newBuilder().setClientInfo(getClientInfo()).setMsg(msg).setType(feedbackType).setPicUrls(picUrls).build()
        }

        fun createCheckVersionReq(): SysProto.CheckVersionReq {
            return SysProto.CheckVersionReq.newBuilder().setClientInfo(getClientInfo()).build()
        }

        fun createGetKeyPairReq(flag: CommonProto.KeyPairType, targetId: Long): SysProto.GetKeyPairReq {
            return SysProto.GetKeyPairReq.newBuilder().setClientInfo(getClientInfo()).setFlag(flag).setTargetId(targetId).build()
        }

        fun createGetKeyPairOfVerReq(targetId: Long, appVer: Int, webVer: Int): SysProto.GetKeyPairOfVerReq {
            return SysProto.GetKeyPairOfVerReq.newBuilder().setClientInfo(getClientInfo()).setUid(targetId).setAppVer(appVer).setWebVer(webVer).build()
        }

        fun createUpdateKeyPairReq(publicKey: String): SysProto.UpdateKeyPairReq {
            return SysProto.UpdateKeyPairReq.newBuilder().setClientInfo(getClientInfo()).setPublicKey(publicKey).build()
        }

        fun createUpdateOpenInstallReq(params: String, type: Int, channelCode: String): SysProto.OpenInstallReq {
            return SysProto.OpenInstallReq.newBuilder().setClientInfo(getClientInfo()).setParams(params)
                    .setType(type).setChannelCode(channelCode).build()
        }

        fun getUploadToken(): SysProto.GetUploadTokenReq {
            return SysProto.GetUploadTokenReq.newBuilder().setClientInfo(getClientInfoWithOutSessionId()).build()
        }

        fun getUploadUrl(uploadType: Long, attachWorkSpaceType: CommonProto.AttachWorkSpaceType, attachType: CommonProto.AttachType): UploadFileProto.GetUploadUrlReq {
            return UploadFileProto.GetUploadUrlReq.newBuilder().setClientInfo(getClientInfoWithOutSessionId()).setAttachWorkspaceType(attachWorkSpaceType).setAttachType(attachType).setType(uploadType).build()
        }

        fun getInviteLink(targetId: Long,type: CommonProto.InviteLinkType): SysProto.GetInviteLinkReq {
            return SysProto.GetInviteLinkReq.newBuilder().setClientInfo(getClientInfo()).setTargetId(targetId).setType(type).build()
        }

        fun updateInviteLink(targetId: Long,type: CommonProto.InviteLinkType): SysProto.UpdateInviteLinkReq {
            return SysProto.UpdateInviteLinkReq.newBuilder().setClientInfo(getClientInfo()).setTargetId(targetId).setType(type).build()
        }

        fun disableAccount(): SysProto.DisableAccountReq {
            return SysProto.DisableAccountReq.newBuilder().setClientInfo(getClientInfo()).build()
        }


        fun getAwsUpload(): UploadFileProto.GetAwsUploadReq {
            return UploadFileProto.GetAwsUploadReq.newBuilder().setClientInfo(getClientInfoWithOutSessionId()).build()
        }
    }
}