package framework.telegram.message.http.creator

import com.im.domain.pb.CommonProto
import com.im.domain.pb.SysProto
import framework.telegram.message.http.getClientInfo
import framework.telegram.message.http.getClientInfoWithOutSessionId


class SysHttpReqCreator {

    companion object {

        fun getToken(type: CommonProto.AttachType, spaceType: CommonProto.AttachWorkSpaceType): SysProto.GetTokenReq {
            return SysProto.GetTokenReq.newBuilder().setClientInfo(getClientInfoWithOutSessionId()).setAttachType(type).setAttachWorkspaceType(spaceType).build()
        }

        fun getPlaceList(lat:Int, lng:Int, type: CommonProto.SearchMapType, keyword:String, pageNum:Int, pageSize:Int): SysProto.PlaceListReq {
            return SysProto.PlaceListReq.newBuilder().setClientInfo(getClientInfo())
                    .setType(type).setLat(lat).setLng(lng).setKeyword(keyword).setPageNum(pageNum).setPageSize(pageSize).build()
        }

        fun getGoogleList(lat:Int,lng:Int): SysProto.GoogleUrlReq {
            return SysProto.GoogleUrlReq.newBuilder().setClientInfo(getClientInfo())
                    .setLat(lat).setLng(lng).build()
        }

        fun getUploadToken(): SysProto.GetUploadTokenReq {
            return SysProto.GetUploadTokenReq.newBuilder().setClientInfo(getClientInfoWithOutSessionId()).build()
        }

        fun getUploadUrl(attachWorkSpaceType: CommonProto.AttachWorkSpaceType, attachType: CommonProto.AttachType): SysProto.GetUploadUrlReq {
            return SysProto.GetUploadUrlReq.newBuilder().setClientInfo(getClientInfoWithOutSessionId()).setAttachWorkspaceType(attachWorkSpaceType).setAttachType(attachType).build()
        }
    }
}