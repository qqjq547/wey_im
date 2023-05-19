package framework.telegram.support.system.network.http

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object HttpClientCreater {

//    private val logInterceptor by lazy {
//        val interceptor = HttpLoggingInterceptor(HttpLogger())
//        interceptor.level = HttpLoggingInterceptor.Level.BODY
//        interceptor
//    }

    val uploadOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
    }

    val imageLoaderOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
    }

    fun newOkHttpClient(interceptor: Interceptor, loggingInterceptor: Interceptor): OkHttpClient {
        return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(interceptor)
                .addInterceptor(loggingInterceptor)
//                .addNetworkInterceptor(logInterceptor)
                .build()
    }

    fun newOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS).build()
    }
}
