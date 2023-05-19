package framework.ideas.common.webview

/**
 * Created by Administrator on 2015/5/11.
 */
object WebUrlConfig {

    //用户协议
    val userProtocolUrl_cn: String
        get() = "/cn/agreement.html"
    val userProtocolUrl_tc: String
        get() = "/tc/agreement.html"
    val userProtocolUrl_en: String
        get() = "/en/agreement.html"
    val userProtocolUrl_vi: String
        get() = "/agreement.html"

    val explainUrl: String
        get() = "/common_68/explain.html"

    val helpUlr: String
        get() = "/common_68/help.html"

    val networkErrorUrl: String
        get() = "http://imoctopus.co/tc/network_tips.html"
}
