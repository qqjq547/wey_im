package framework.telegram.business.bridge.bean

import framework.telegram.support.system.storage.sp.core.Key
import framework.telegram.support.system.storage.sp.core.SpName

@SpName("user_preferences")
interface AccountInfo {

    @Key("userId")
    fun putUserId(value: Long)

    @Key("userId")
    fun getUserId(): Long

    @Key("test")
    fun putTest(value: String)

    @Key("test")
    fun getTest(): String

    @Key("phone")
    fun putPhone(value: String)

    @Key("phone")
    fun getPhone(): String

    @Key("nickName")
    fun putNickName(value: String)

    @Key("nickName")
    fun getNickName(): String

    @Key("password")
    fun putPassWord(value: String)

    @Key("password")
    fun getPassWord(): String

    @Key("icon")
    fun putAvatar(value: String)

    @Key("icon")
    fun getAvatar(): String

    @Key("sex")
    fun putSex(sex: Int)

    @Key("sex")//保密 0 ；男 1 ； 女 2
    fun getSex(): Int

    @Key("privacy")
    fun putPrivacy(privacy: Int)

    @Key("privacy")
    fun getPrivacy(): Int

    @Key("birthday")
    fun putBirthday(birthday: String)

    @Key("birthday")
    fun getBirthday(): String

    @Key("loginReg")
    fun putLoginReg(loginReg: Boolean)

    @Key("loginReg")
    fun getLoginReg(): Boolean

    @Key("sessionId")
    fun putSessionId(value: String)

    @Key("sessionId")
    fun getSessionId(): String

    @Key("countryCode")
    fun putCountryCode(value: String)

    @Key("countryCode")
    fun getCountryCode(): String

    //    SMS_CODE     = 0 // 短信验证码
//    PASSWORD     = 1 // 用户密码
    @Key("loginType")
    fun getLoginType(): Int

    @Key("loginType")
    fun putLoginType(value: Int)

    @Key("token")
    fun putToken(value: String)

    @Key("token")
    fun getToken(): String

    @Key("socketIp_2")
    fun putSocketIp(value: String)

    @Key("socketIp_2")
    fun getSocketIp(): String

    @Key("socketPort_2")
    fun putSocketPort(value: Int)

    @Key("socketPort_2")
    fun getSocketPort(): Int

    @Key("useWebSocket_2")
    fun putUseWebSocket(value: Boolean)

    @Key("useWebSocket_2")
    fun getUseWebSocket(): Boolean

    @Key("webSocketAddress_2")
    fun putWebSocketAddress(value: String)

    @Key("webSocketAddress_2")
    fun getWebSocketAddress(): String

    @Key("signature")
    fun putSignature(value: String)

    @Key("signature")
    fun getSignature(): String

    @Key("bfPassword")
    fun putBfPassword(value: Boolean)

    @Key("bfPassword")
    fun getBfPassword(): Boolean

    @Key("onlineViewType")
    fun putOnlineViewType(value: Int)

    @Key("onlineViewType")
    fun getOnlineViewType(): Int

    @Key("displayPhone")
    fun putDisplayPhone(value: String)

    @Key("displayPhone")
    fun getDisplayPhone(): String

    @Key("webPublicKey")
    fun putWebPublicKey(value: String)

    @Key("webPublicKey")
    fun getWebPublicKey(): String

    @Key("webPublicKeyVersion")
    fun putWebPublicKeyVersion(value: Int)

    @Key("webPublicKeyVersion")
    fun getWebPublicKeyVersion(): Int

    @Key("webOnline")
    fun putWebOnline(value: Int)

    @Key("webOnline")
    fun getWebOnline(): Int

    @Key("clearTime")
    fun putClearTime(value: Int)

    @Key("clearTime")
    fun getClearTime(): Int

    @Key("agoraAppId")
    fun putAgoraAppId(value: String)

    @Key("agoraAppId")
    fun getAgoraAppId(): String

    @Key("identify")
    fun putIdentify(value: String)

    @Key("identify")
    fun getIdentify(): String

    @Key("qrCode")
    fun putQrCode(value: String)

    @Key("qrCode")
    fun getQrCode(): String

    @Key("hwToken")
    fun putHwToken(value: String)

    @Key("hwToken")
    fun getHwToken(): String
}