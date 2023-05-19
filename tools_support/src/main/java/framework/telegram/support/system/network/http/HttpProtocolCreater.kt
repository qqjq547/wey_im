package framework.telegram.support.system.network.http

import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import java.util.*

object HttpProtocolCreater {

    fun <T> createProtoBufProtocol(host: String, clazz: Class<T>, interceptor: Interceptor, loggingInterceptor: Interceptor): T {
        return HttpAdapterCreater.createProtoBufRetrofit(host, interceptor, loggingInterceptor).create(clazz)
    }

    fun <T> crateUploadJsonRetrofit(host: String, clazz: Class<T>): T {
        return HttpAdapterCreater.crateUploadJsonRetrofit(host).create(clazz)
    }
}
