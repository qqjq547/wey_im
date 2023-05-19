package framework.telegram.support.tools

import android.util.Log

/**
 * Created by lzh on 19-10-22.
 * INFO:
 */
object CalculateTimeHepler{

    private val timeList = mutableMapOf<String,Long>()

    fun setInitTime(action :String){
        timeList[action]=System.currentTimeMillis()
        Log.i("lzh","$action  start")

    }

    fun logConsumeTime(action :String){
        val initTime = timeList[action]
        initTime?.let {
            if (initTime != 0L){
                Log.i("lzh","$action  consume: ${(System.currentTimeMillis() - initTime).toFloat()/1000} 秒")
            }
            timeList.remove(action)
        }


    }
}