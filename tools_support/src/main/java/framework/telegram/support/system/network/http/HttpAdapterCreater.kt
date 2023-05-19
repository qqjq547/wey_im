package framework.telegram.support.system.network.http

import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.protobuf.ProtoConverterFactory

object HttpAdapterCreater {

    private val mDefaultProtoConverterFactory by lazy { ProtoConverterFactory.create() }

    private val mDefaultGsonConverterFactory by lazy { GsonConverterFactory.create() }

    private val mDefaultRxJavaCallAdapterFactory by lazy { RxJava2CallAdapterFactory.create() }

    fun createProtoBufRetrofit(baseUrl: String, interceptor: Interceptor, logInterceptor: Interceptor): Retrofit {
        return Retrofit.Builder()
                .client(HttpClientCreater.newOkHttpClient(interceptor, logInterceptor))
                .baseUrl(baseUrl)
                .addConverterFactory(mDefaultProtoConverterFactory)
                .addCallAdapterFactory(mDefaultRxJavaCallAdapterFactory)
                .build()
    }

    fun crateUploadJsonRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
                .client(HttpClientCreater.uploadOkHttpClient)//这个httpClient 千万别添加拦击，否则拦截会让进度跑多一遍
                .baseUrl(baseUrl)
                .addConverterFactory(mDefaultGsonConverterFactory)
                .addCallAdapterFactory(mDefaultRxJavaCallAdapterFactory)
                .build()
    }
}
