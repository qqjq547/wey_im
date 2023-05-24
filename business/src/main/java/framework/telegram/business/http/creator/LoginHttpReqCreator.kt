package framework.telegram.business.http.creator

import com.im.domain.pb.CommonProto
import com.im.domain.pb.LoginProto
import android.R.string
import android.util.Log
import framework.telegram.business.UserDHKeysHelper
import framework.telegram.business.http.getClientInfo
import framework.telegram.business.http.getClientInfoWithOutSessionId
import framework.telegram.business.utils.InstallIdUtil
import framework.telegram.support.BaseApp
import framework.telegram.support.BaseApp.Companion.app
import framework.telegram.support.tools.AndroidUtils
import framework.telegram.support.tools.DeviceUtils


class LoginHttpReqCreator {

    companion object {
        /**
         * 密码登录
         */
        fun createLoginByPasswordReq(phone: String, password: String, countryCode: String): LoginProto.LoginReq {
            val clientInfo = getClientInfoWithOutSessionId()

            val sysVer = "Android " + AndroidUtils.getPlatformVersion()
            val sysMac = InstallIdUtil.getInstallationId()
            val sysModel = DeviceUtils.getDeviceModel()

            return LoginProto.LoginReq.newBuilder().setClientInfo(clientInfo)
                    .setCountryCode(countryCode).setPhone(phone).setLoginType(CommonProto.LoginType.PASSWORD)
                    .setSysVersion(sysVer).setSysMac(sysMac).setSysModel(sysModel)
                    .setLoginMode(CommonProto.LoginMode.HAND)
                    .setPassword(password).build()
        }

        /**
         * 验证码登录
         */
        fun createLoginBySmdCodeReq(phone: String, smsCode: String, countryCode: String): LoginProto.LoginReq {
            val clientInfo = getClientInfoWithOutSessionId()

            val sysVer = "Android " + AndroidUtils.getPlatformVersion()
            val sysMac = InstallIdUtil.getInstallationId()
            val sysModel = DeviceUtils.getDeviceModel()

            return LoginProto.LoginReq.newBuilder().setClientInfo(clientInfo)
                    .setCountryCode(countryCode).setPhone(phone).setLoginType(CommonProto.LoginType.SMS_CODE)
                    .setLoginMode(CommonProto.LoginMode.HAND)
                    .setSysVersion(sysVer).setSysMac(sysMac).setSysModel(sysModel)
                    .setType(CommonProto.GetSmsCodeType.LOGIN).setSmsCode(smsCode).build()
        }

        /**
         * 系统自动登录 密码
         */
        fun createLoginAutoPasswordReq(phone: String, password: String, countryCode: String): LoginProto.LoginReq {
            val clientInfo = getClientInfoWithOutSessionId()

            val sysVer = "Android " + AndroidUtils.getPlatformVersion()
            val sysMac = InstallIdUtil.getInstallationId()
            val sysModel = DeviceUtils.getDeviceModel()

            return LoginProto.LoginReq.newBuilder().setClientInfo(clientInfo)
                    .setCountryCode(countryCode).setPhone(phone).setLoginType(CommonProto.LoginType.PASSWORD)
                    .setLoginMode(CommonProto.LoginMode.SYS_AUTO)
                    .setPassword(password)
                    .setSysVersion(sysVer).setSysMac(sysMac).setSysModel(sysModel).build()
        }

        /**
         * 系统自动登录 短息
         */
        fun createLoginAutoSmsReq(phone: String, token: String, countryCode: String): LoginProto.LoginReq {
            val clientInfo = getClientInfoWithOutSessionId()

            val sysVer = "Android " + AndroidUtils.getPlatformVersion()
            val sysMac = InstallIdUtil.getInstallationId()
            val sysModel = DeviceUtils.getDeviceModel()

            return LoginProto.LoginReq.newBuilder().setClientInfo(clientInfo)
                    .setCountryCode(countryCode).setPhone(phone).setLoginType(CommonProto.LoginType.SMS_CODE)
                    .setLoginMode(CommonProto.LoginMode.SYS_AUTO)
                    .setToken(token)
                    .setSysVersion(sysVer).setSysMac(sysMac).setSysModel(sysModel).build()
        }


        /**
         * 注册
         */
        fun createRegisterReq(registerType: Int, pwd: String, phone: String, smsCode: String, countryCode: String, publicKeyHex: String): LoginProto.RegReq {
            val clientInfo = getClientInfoWithOutSessionId()
            val sysVer = "Android " + AndroidUtils.getPlatformVersion()
            val sysMac = InstallIdUtil.getInstallationId()
            val sysModel = DeviceUtils.getDeviceModel()

            return LoginProto.RegReq.newBuilder().setClientInfo(clientInfo)
                .setPhoneRegMode(registerType)
                .setPassword(pwd)
                .setCountryCode(countryCode).setPhone(phone).setSmsCode(smsCode)
                .setSysVersion(sysVer).setSysMac(sysMac).setSysModel(sysModel).setPublicKey(publicKeyHex).build()
        }

        fun createWebLoginByQrCodeReq(qrCode: String, status: CommonProto.WebLoginStatus): LoginProto.WebLoginByQrCodeReq {
            val clientInfo = getClientInfo()
            return LoginProto.WebLoginByQrCodeReq.newBuilder().setClientInfo(clientInfo)
                    .setQrCode(qrCode).setStatus(status)
                    .build()
        }

        fun createWebLogoutReq(): LoginProto.WebLogoutReq {
            val clientInfo = getClientInfo()
            return LoginProto.WebLogoutReq.newBuilder().setClientInfo(clientInfo)
                    .build()
        }

        fun createLogoutReq(): LoginProto.LogoutReq {
            val clientInfo = getClientInfo()
            return LoginProto.LogoutReq.newBuilder().setClientInfo(clientInfo)
                    .build()
        }

        fun createFindPasswordReq(phone: String, smsCode: String, countryCode: String, password: String): LoginProto.ForgetPasswordReq {
            val clientInfo = getClientInfoWithOutSessionId()
            return LoginProto.ForgetPasswordReq.newBuilder()
                    .setClientInfo(clientInfo)
                    .setSmsCode(smsCode).setPhone(phone).setPassword(password).setCountryCode(countryCode)
                    .build()
        }

        fun createGetUrlsReq(): LoginProto.GetUrlsReq {
            return LoginProto.GetUrlsReq.newBuilder().setClientInfo(getClientInfo()).build()
        }
    }
}