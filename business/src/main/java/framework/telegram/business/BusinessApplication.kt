package framework.telegram.business

import android.content.Context
import com.dds.gestureunlock.GestureUnlock
import com.google.protobuf.compiler.PluginProtos
import com.im.domain.pb.LoginProto
import com.manusunny.pinlock.PinCodeUnlock
import framework.ideas.common.rlog.RLogManager
import framework.telegram.business.bridge.Constant.Common.BIZ_HTTP_CONTACT
import framework.telegram.business.bridge.Constant.Common.BIZ_HTTP_GROUP
import framework.telegram.business.bridge.Constant.Common.BIZ_HTTP_HOST
import framework.telegram.business.bridge.Constant.Common.DOWNLOAD_HTTP_HOST
import framework.telegram.business.bridge.Constant.Common.LOGIN_HTTP_HOST
import framework.telegram.business.bridge.Constant.Common.MAP_HTTP_HOST
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.sp.CommonPref
import framework.telegram.support.account.AccountManager
import framework.ideas.common.http.BackupLoginHostInterceptor
import framework.telegram.business.ui.search.db.SearchDbManager
import framework.telegram.support.BaseApp
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.system.upload.Constant.Common.BASE_URL_UPLOAD
import framework.telegram.support.system.upload.Constant.Common.UPLOAD_WAY_TYPE
import framework.telegram.support.tools.EnvironmentUtils
import framework.telegram.support.tools.FileUtils
import framework.telegram.support.tools.IoUtils
import framework.telegram.support.tools.file.DirManager
import framework.telegram.ui.qr.activity.ZXingLibrary
import java.io.FileWriter
import java.util.*


class BusinessApplication {

    companion object {

        fun init(context: Context) {
            ZXingLibrary.initDisplayOpinion(context)
            SearchDbManager.initDb(context)

            if (framework.telegram.support.BuildConfig.IS_LAB_MODEL) {
                PinCodeUnlock.UNLOCK_ERROR_WAIT_TIME = 60 * 1000
                GestureUnlock.UNLOCK_ERROR_WAIT_TIME = 60 * 1000
            }
        }

        fun setPassword(password: String) {
            val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
            accountInfo.putBfPassword(true)
            updateAccountInfoPassword(password)
        }

        fun updateAccountInfo(bfPassword: Boolean, signature: String, privacy: Int, viewType: Int, displayPhone: String, clearTime: Int, qrCode: String) {
            val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
            accountInfo.putBfPassword(bfPassword)
            accountInfo.putPrivacy(privacy)
            accountInfo.putSignature(signature)
            accountInfo.putOnlineViewType(viewType)
            accountInfo.putDisplayPhone(displayPhone)
            accountInfo.putClearTime(clearTime)
            accountInfo.putQrCode(qrCode)
        }

        fun updateAccountInfoPassword(password: String) {
            val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
            accountInfo.putPassWord(password)
        }

        fun updateAccountInfoPhone(phone: String, countryCode: String) {
            val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
            accountInfo.putCountryCode(countryCode)
            accountInfo.putPhone(phone)
        }

        fun saveAccountInfoByLogin(loginType: Int, userId: Long, phone: String, encryptPwd: String, pwd: String, countryCode: String, it: LoginProto.LoginResp): AccountInfo {
            AccountManager.saveLoginAccountUUid(UUID.nameUUIDFromBytes("$userId".toByteArray()))//必须先保存LoginAccountUuid

            val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
            accountInfo.putPhone(phone)
            accountInfo.putNickName(it.user.nickName)
            accountInfo.putPassWord(encryptPwd)
            accountInfo.putAvatar(it.user.icon)
            accountInfo.putSex(it.user.gender.number)
            accountInfo.putToken(it.token)
            accountInfo.putPrivacy(it.privacy)
            accountInfo.putLoginType(loginType)
            accountInfo.putLoginReg(it.loginReg)
            accountInfo.putCountryCode(countryCode)
            accountInfo.putAgoraAppId(it.agoraAppId)
            accountInfo.putIdentify(it.user.identify)

            accountInfo.putSessionId(it.sessionId)
            accountInfo.putUserId(userId)

            RLogManager.d("Http", "登录成功，设置urls--->")
            saveUrls(it.urls)

            return accountInfo
        }

        fun saveAccountInfoByRegister(loginType: Int, userId: Long, phone: String, password: String, countryCode: String, it: LoginProto.RegResp): AccountInfo {
            AccountManager.saveLoginAccountUUid(UUID.nameUUIDFromBytes("$userId".toByteArray()))//必须先保存LoginAccountUuid
            val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
            accountInfo.putPhone(phone)
            accountInfo.putNickName(it.user.nickName)
            accountInfo.putPassWord(password)
            accountInfo.putAvatar(it.user.icon)
            accountInfo.putSex(it.user.gender.number)
            accountInfo.putToken(it.token)
            accountInfo.putPrivacy(it.privacy)
            accountInfo.putLoginType(loginType)
            accountInfo.putLoginReg(true)
            accountInfo.putCountryCode(countryCode)
            accountInfo.putAgoraAppId(it.agoraAppId)
            accountInfo.putIdentify(it.user.identify)

            accountInfo.putSessionId(it.sessionId)
            accountInfo.putUserId(userId)

            RLogManager.d("Http", "注册成功，设置urls--->")
            saveUrls(it.urls)

            //记录登录时间
            SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()).putLastManualLoginTime(System.currentTimeMillis())

            return accountInfo
        }

        fun saveUrls(it: LoginProto.UrlInfo) {
            // 普通业务用到的URLs
            LOGIN_HTTP_HOST = it.login
            BIZ_HTTP_HOST = it.biz
            BIZ_HTTP_CONTACT = it.friend
            BIZ_HTTP_GROUP = it.group
            MAP_HTTP_HOST = it.staticMap
            DOWNLOAD_HTTP_HOST = it.download
            BASE_URL_UPLOAD = it.uploadUrl
            UPLOAD_WAY_TYPE = it.uploadServer

            // 消息用到的URLs
            framework.telegram.message.bridge.Constant.Common.LOGIN_HTTP_HOST = it.login
            framework.telegram.message.bridge.Constant.Common.BIZ_HTTP_HOST = it.biz
            framework.telegram.message.bridge.Constant.Common.MAP_HTTP_HOST = it.staticMap

            // 备用host使用到的URLs
            BackupLoginHostInterceptor.CONFIG_HTTP_HOST = it.config

            val sp = SharePreferencesStorage.createStorageInstance(CommonPref::class.java)
            sp.putLoginUrl(it.login)
            sp.putBizUrl(it.biz)
            sp.putFriendUrl(it.friend)
            sp.putGroupUrl(it.group)
            sp.putMapUrl(it.staticMap)
            sp.putDownloadUrl(it.download)
            sp.putConfigUrl(it.config)
            sp.putUploadUrl(it.uploadUrl)
            sp.putUploadServer(it.uploadServer)

            val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
            if (it.socketProtocol == 1) {
                accountInfo.putUseWebSocket(false)
                val sessionAddress = it.session.split(":")
                accountInfo.putSocketIp(sessionAddress[0])
                accountInfo.putSocketPort(sessionAddress[1].toInt())
            } else if (it.socketProtocol == 2) {
                accountInfo.putUseWebSocket(true)
                accountInfo.putWebSocketAddress(it.wss)
            }

            RLogManager.d("Http", "保存socketProtocol--->${it.socketProtocol}")
            if (it.socketProtocol == 1) {
                RLogManager.d("Http", "保存session--->${it.session}")
            } else {
                RLogManager.d("Http", "保存session--->${it.wss}")
            }
            RLogManager.d("Http", "保存LOGIN_HTTP_HOST--->${it.login}")
            RLogManager.d("Http", "保存BIZ_HTTP_HOST--->${it.biz}")
            RLogManager.d("Http", "保存BIZ_HTTP_CONTACT--->${it.friend}")
            RLogManager.d("Http", "保存BIZ_HTTP_GROUP--->${it.group}")
            RLogManager.d("Http", "保存DOWNLOAD_HTTP_HOST--->${it.download}")
            RLogManager.d("Http", "保存CONFIG_HTTP_HOST--->${it.config}")
            RLogManager.d("Http", "保存MAP_HTTP_HOST--->${it.staticMap}")
            RLogManager.d("Http", "保存UPLOAD url --->${it.uploadUrl}   type  ${it.uploadServer}")
        }

        fun onUserLogin() {
            // 同步联系人
            ArouterServiceManager.contactService.syncAllContact(null)
            SearchDbManager.initDb(BaseApp.app)
        }

        fun onUserChangeToken() {
            UserDHKeysHelper.clearMyselfWebSecretKeysCache()
        }

        fun onUserLogout() {

        }
    }
}