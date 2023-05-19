package framework.telegram.support.system.upload.http

import android.util.Log
import framework.telegram.support.system.gson.GsonInstanceCreater
import framework.telegram.support.system.network.http.HttpProtocolCreater
import framework.telegram.support.system.upload.CommonResult
import framework.telegram.support.system.upload.Constant
import framework.telegram.support.system.upload.LocalResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by lzh on 19-5-23.
 * INFO:
 */
object HttpManager {

    @Synchronized
    fun <T> getUploadStore(httpStoreClazz: Class<T>): T {
        return HttpProtocolCreater.crateUploadJsonRetrofit(Constant.Common.BASE_URL_UPLOAD, httpStoreClazz)
    }
}

inline fun <reified T> Call<CommonResult>.getResult(classType: Class<T>,
                                                    noinline onNext: (local: LocalResult, result: T) -> Unit,
                                                    noinline onError: ((t: Throwable) -> Unit)? = null
) {
    this.enqueue(object : Callback<CommonResult> {
        override fun onFailure(call: Call<CommonResult>, t: Throwable) {
            onError?.invoke(t)
        }

        override fun onResponse(call: Call<CommonResult>, response: Response<CommonResult>) {
            val data = response.body()
            Log.i("http_log", "result body:${data.toString()}")
            val local = LocalResult(data?.code ?: 0L, data?.desc ?: "")
            if (data?.code == 200L) {
                if (classType == String::class.java) {
                    onNext.invoke(local, data.data as T)
                } else {
                    Log.i("http_log", "返回：${data.data}")
                    val gson = GsonInstanceCreater.defaultGson.fromJson(data.data, classType)
                    if (gson != null) {
                        onNext.invoke(local, gson)
                    } else {
                        Log.i("http_log", "网络错误")
                        onError?.invoke(Throwable("网络错误"))
                    }
                }
            } else {
                if (data == null) {
                    Log.i("http_log", "result data is null")
                    onError?.invoke(Throwable("data is null"))
                } else {
                    Log.i("http_log", "result error code:${data.code} msg:${data.desc}")
                    onError?.invoke(Throwable(data.desc))
                }
            }
        }
    })
}

