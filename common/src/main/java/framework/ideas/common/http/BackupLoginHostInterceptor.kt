package framework.ideas.common.http

import android.net.Uri
import android.text.TextUtils
import framework.ideas.common.rlog.RLogManager
import framework.telegram.support.BuildConfig
import framework.telegram.support.tools.AESHelper
import framework.telegram.support.tools.Base64
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

open class BackupLoginHostInterceptor : Interceptor {

    companion object {
        var newHosts = ArrayList<String>()

        var CONFIG_HTTP_HOST: String = ""
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        var response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            null
        }

        return if (response?.code() == 200) {
            response
        } else {
            // 假如请求结果不为200，则替换为备用的host
            RLogManager.d("Http", "${request.url()}接口请求失败${response?.code()}--->")

            if (newHosts.isEmpty()) {
                reqNewHost(chain)
            }

            run outside@{
                val newHostsTmp = ArrayList(newHosts)
                newHostsTmp.forEach {
                    val host = UriUtils.getHost(it)
                    val port = UriUtils.getPort(it)
                    val scheme =  UriUtils.getScheme(it)
                    val oldRequestUrl = request.url().toString()
                    request = if (port > 0) {
                        request.newBuilder().url(request.url().newBuilder().host(host).port(port).scheme(scheme).build()).build()
                    } else {
                        request.newBuilder().url(request.url().newBuilder().host(host).scheme(scheme).build()).build()
                    }
                    try {
                        response = chain.proceed(request)
                        if (response?.code() == 200) {
                            // 结果可用，返回
                            RLogManager.d("Http", "替换${oldRequestUrl}为备用域名${request.url()}访问成功--->")
                            return@outside
                        } else {
                            // 结果不可用，删除此缓存
                            RLogManager.d("Http", "替换${oldRequestUrl}为备用域名${request.url()}仍不可用--->")
                            newHosts.remove(it)
                        }
                    } catch (e: Exception) {
                        // 结果不可用，删除此缓存
                        newHosts.remove(it)
                    }
                }
            }

            if (response != null) {
                response!!
            } else {
                chain.proceed(request)
            }
        }
    }

    private fun reqNewHost(chain: Interceptor.Chain) {
        //val newHostReq = Request.Builder().get().url(" https://efdwasfre.lamboim.tech").build()
        val newHostReq = Request.Builder().get().url("https://efdwasfre.futeapi.com").build()
        val newHostResp = try {
            RLogManager.d("Http", "从coding文件服务器获取Http备用域名--->")
            chain.proceed(newHostReq)
        } catch (e: java.lang.Exception) {
            if (!TextUtils.isEmpty(CONFIG_HTTP_HOST)) {
                RLogManager.d("Http", "从自有文件服务器获取Http备用域名--->")
                chain.proceed(Request.Builder().get().url(CONFIG_HTTP_HOST).build())
            } else {
                null
            }
        }

        val newHost = if (newHostResp?.code() == 200) {
            newHostResp.body()?.string() ?: ""
        } else {
            ""
        }

        if (!TextUtils.isEmpty(newHost)) {
            try {
                val array = JSONObject(String(Base64.decode(newHost, Base64.DEFAULT))).getJSONArray(if (BuildConfig.JENKINS_IS_TEST_SERVER) "url_dev" else "url")
                for (index in 0 until array.length()) {
                    newHosts.add(array.getString(index))
                }
                RLogManager.d("Http", "获取Http备用域名成功--->")
            } catch (e: Exception) {
                RLogManager.e("Http", "Http备用域名解析失败--->", e)
                newHosts.clear()
            }
        } else {
            RLogManager.d("Http", "获取Http备用域名失败--->")
            newHosts.clear()
        }
    }
}