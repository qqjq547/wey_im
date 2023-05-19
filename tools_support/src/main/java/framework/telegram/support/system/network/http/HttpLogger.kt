package framework.telegram.support.system.network.http

import android.util.Log
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Created by lzh on 19-6-3.
 * INFO:
 */
class HttpLogger : HttpLoggingInterceptor.Logger {
   override fun log(message: String) {
        Log.d("HttpLogInfo", message)
    }
}