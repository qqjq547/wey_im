package framework.telegram.message.http.creator

import com.im.domain.pb.CommonProto
import com.im.domain.pb.SysProto
import com.im.domain.pb.UserProto
import framework.telegram.message.http.getClientInfo
import framework.telegram.message.http.getClientInfoWithOutSessionId


class UserHttpReqCreator {

    companion object {

        fun getEmoticon(): UserProto.GetEmoticonReq {
            return UserProto.GetEmoticonReq.newBuilder().setClientInfo(getClientInfo()).build()
        }

        fun addEmoticon(url: String, width: Int, height: Int): UserProto.AddEmoticonReq {
            val emoticon = CommonProto.EmoticonBase.newBuilder().setEmoticonUrl(url).setWidth(width).setHeight(height).build()
            return UserProto.AddEmoticonReq.newBuilder().setClientInfo(getClientInfo())
                    .setEmoticon(emoticon).build()
        }


        fun addEmoticon(id: Long, url: String, width: Int, height: Int): UserProto.AddEmoticonReq {
            val emoticon = CommonProto.EmoticonBase.newBuilder().setEmoticonId(id).setEmoticonUrl(url).setWidth(width).setHeight(height).build()
            return UserProto.AddEmoticonReq.newBuilder().setClientInfo(getClientInfo())
                    .setEmoticon(emoticon).build()
        }

        fun delEmoticons(ids: List<Long>): UserProto.DelEmoticonReq {
            return UserProto.DelEmoticonReq.newBuilder().setClientInfo(getClientInfo()).addAllEmoticonIds(ids).build()
        }
    }
}