package framework.telegram.message.http.creator

import com.im.domain.pb.CommonProto
import com.im.domain.pb.LoginProto
import com.im.domain.pb.SysProto
import com.im.domain.pb.UserProto
import framework.telegram.message.http.getClientInfo
import framework.telegram.message.http.getClientInfoWithOutSessionId


class LoginHttpReqCreator {

    companion object {

        fun createGetUrlsReq(): LoginProto.GetUrlsReq {
            return LoginProto.GetUrlsReq.newBuilder().setClientInfo(getClientInfo()).build()
        }
    }
}