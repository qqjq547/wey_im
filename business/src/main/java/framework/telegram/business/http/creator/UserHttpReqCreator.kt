package framework.telegram.business.http.creator

import com.im.domain.pb.UserProto
import framework.telegram.business.http.getClientInfo


class UserHttpReqCreator {

    companion object {

        fun createUpdateTokenReq(aliPushToken: String, hwToken: String): UserProto.UpdateTokenReq {
            val build = UserProto.UpdateTokenReq.newBuilder()
            return build.setClientInfo(getClientInfo())
                    .setToken(aliPushToken).setHwToken(hwToken).build()
        }

        fun createUserUpdateReq(opList: List<UserProto.UserOperator>, userParam: UserProto.UserParam): UserProto.UpdateReq {
            val build = UserProto.UpdateReq.newBuilder()
            build.addAllOps(opList)
            return build.setClientInfo(getClientInfo())
                    .setUserParam(userParam).build()
        }

        fun createUserUpdateReq(uid: Long): UserProto.DetailReq {
            return UserProto.DetailReq.newBuilder().setClientInfo(getClientInfo())
                    .setUid(uid).build()
        }

        fun createCheckPasswordReq(password: String): UserProto.ValidatePasswordReq {
            return UserProto.ValidatePasswordReq.newBuilder().setClientInfo(getClientInfo())
                    .setPassword(password).build()
        }

        fun createUpdatePhoneReq(countryCode: String, phone: String, smsCode: String): UserProto.UpdatePhoneReq {
            return UserProto.UpdatePhoneReq.newBuilder().setClientInfo(getClientInfo())
                    .setCountryCode(countryCode).setPhone(phone).setSmsCode(smsCode).build()
        }

        fun createSetPasswordReq(password: String): UserProto.PasswordReq {
            return UserProto.PasswordReq.newBuilder().setClientInfo(getClientInfo())
                    .setPassword(password).build()
        }

        fun createUpdatePasswordReq(oldPassword: String, password: String): UserProto.UpdatePasswordReq {
            return UserProto.UpdatePasswordReq.newBuilder().setClientInfo(getClientInfo())
                    .setPassword(password).setOldPassword(oldPassword).build()
        }

        fun createUpdatePasswordFromSmsCodeReq(countryCode: String, phone: String, smsCode: String, password: String): UserProto.UpdatePasswordFromSmsCodeReq {
            return UserProto.UpdatePasswordFromSmsCodeReq.newBuilder().setClientInfo(getClientInfo())
                    .setPassword(password).setCountryCode(countryCode).setPhone(phone).setSmsCode(smsCode).build()
        }

        fun createInvisibleListReq(pageNum: Int, pageSize: Int): UserProto.InvisibleListReq {
            return UserProto.InvisibleListReq.newBuilder().setClientInfo(getClientInfo())
                    .setPageNum(pageNum).setPageSize(pageSize).build()
        }

        fun createUpdateInvisibleList(targetUids: List<Long>, op: UserProto.UserOperator): UserProto.UpdateInvisibleListReq {
            return UserProto.UpdateInvisibleListReq.newBuilder().setClientInfo(getClientInfo())
                    .addAllTargetUids(targetUids).setOp(op).build()
        }

    }
}