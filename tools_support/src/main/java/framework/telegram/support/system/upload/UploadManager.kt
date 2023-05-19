package framework.telegram.support.system.upload

import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.system.upload.Constant.Common.UPLOAD_KEY
import framework.telegram.support.system.upload.http.HttpManager
import framework.telegram.support.system.upload.http.getResult
import framework.telegram.support.system.upload.progress.ProgressListener
import framework.telegram.support.system.upload.progress.ProgressManager
import framework.telegram.support.tools.MD5
import framework.telegram.support.tools.ThreadUtils
import okhttp3.MultipartBody.FORM
import java.io.File
import java.net.URLEncoder
import android.R.string
import android.util.Log
import framework.telegram.support.system.gson.GsonInstanceCreater
import framework.telegram.support.system.network.http.HttpLogger
import framework.telegram.support.system.upload.Constant.Common.BASE_URL_UPLOAD
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import com.liulishuo.okdownload.DownloadTask.enqueue
import java.io.IOException


/**
 * Created by lzh on 20-1-11.
 * INFO:
 */
object UploadManager {

    enum class UPLOAD_TYPE(var type: Int) {
        COMMON_PIC(1001),
        COMMON_AUDIO(1002),
        COMMON_VIDEO(1003),
        COMMON_FILE(1004),
        COMMON_LOG(1005),
        CHAT_PIC(1101),
        CHAT_AUDIO(1102),
        CHAT_VIDEO(1103),
        CHAT_FILE(1104),
        CHAT_LOG(1105),
        CHAT_EMOTICON(1106),
    }

    private const val TAG = "UploadManager"

    private const val SIGH = "sign"
    private const val TIME = "time"
    private const val SPACE_TYPE = "spaceType"
    private const val FILE = "file"

    fun uploadFile(file: File, type: UPLOAD_TYPE, resultListener: ResultListener) {
        val curTime = System.currentTimeMillis().toString()
        val body = ProgressManager.createCustomRequestBody(FORM, file, object : ProgressListener {
            override fun onProgress(totalBytes: Long, remainingBytes: Long, done: Boolean) {
                ThreadUtils.runOnUIThread {
                    AppLogcat.logger.d(TAG, "totalBytes $totalBytes remainingBytes $remainingBytes ")
                    resultListener.onProgress(totalBytes - remainingBytes, totalBytes)
                }
            }
        })
        val builder = MultipartBody.Builder()
        builder.addFormDataPart(SIGH, MD5.md5("$curTime@$UPLOAD_KEY"))
        builder.addFormDataPart(TIME, curTime)
        builder.addFormDataPart(SPACE_TYPE, type.type.toString())
        builder.addFormDataPart(FILE, URLEncoder.encode(file.name, "UTF-8"), body)
        builder.setType(FORM)

        val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
//                .addNetworkInterceptor(logInterceptor)
                .build()

        val request = Request.Builder()
                .url(BASE_URL_UPLOAD)
                .post(builder.build())
                .build()

        val call = client.newCall(request)
        //4. 执行Call对象（call 是interface 实际执行的是RealCall）中的 `enqueue`方法
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                resultListener.onFailure(e)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                try {
                    Log.i("http_log","ss "+response.body().toString())
                    val gson = GsonInstanceCreater.defaultGson.fromJson(response.body()?.string(), CommonResult::class.java)
                    if (gson != null && gson.code == 200L) {
                        Log.i("http_log", "responseBody   ${gson.data}")
                        resultListener.onSuccess(gson.data)
                    } else {
                        Log.i("http_log", "网络错误")
                        resultListener.onFailure(Throwable("Error"))
                    }
                }catch (e:Exception){
                    Log.i("http_log", "网络错误")
                    resultListener.onFailure(Throwable("Error"))
                }
            }
        })
    }

    private val logInterceptor by lazy {
        val interceptor = HttpLoggingInterceptor(HttpLogger())
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        interceptor
    }

    fun startUploadTask() {

    }
}