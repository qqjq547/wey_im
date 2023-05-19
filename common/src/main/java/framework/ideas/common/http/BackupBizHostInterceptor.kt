package framework.ideas.common.http

import android.text.TextUtils
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import okhttp3.Interceptor
import okhttp3.Response

open class BackupBizHostInterceptor(private val getUrlRespCall: () -> String) : Interceptor {

    companion object {
        private var newHosts = ArrayList<String>()
        private var connectRetryLastTime = 0L
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
            if (newHosts.isEmpty()) {
                reqNewHost()
            }

            run outside@{
                val newHostsTmp = ArrayList(newHosts)
                newHostsTmp.forEach {
                    val host = UriUtils.getHost(it)
                    val port = UriUtils.getPort(it)
                    request = if (port > 0) {
                        request.newBuilder().url(request.url().newBuilder().host(host).port(port).build()).build()
                    } else {
                        request.newBuilder().url(request.url().newBuilder().host(host).build()).build()
                    }
                    try {
                        response = chain.proceed(request)
                        if (response?.code() == 200) {
                            // 结果可用，返回
                            return@outside
                        } else {
                            // 结果不可用，删除此缓存
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

    @Synchronized
    private fun reqNewHost() {
        if (System.currentTimeMillis() - connectRetryLastTime > 60 * 1000) {
            connectRetryLastTime = System.currentTimeMillis()
            val newHost = getUrlRespCall.invoke()
            if (!TextUtils.isEmpty(newHost)) {
                newHosts.clear()
                newHosts.add(newHost)
            }
        }
    }
}