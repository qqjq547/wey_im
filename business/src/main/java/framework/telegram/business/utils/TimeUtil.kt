package framework.telegram.business.utils

import java.text.SimpleDateFormat

/**
 * Created by lzh on 19-6-4.
 * INFO:
 */
object TimeUtil {
    private const val YEAR = (365 * 24 * 3600).toLong()
    private const val MONTH = (30 * 24 * 3600).toLong()
    private const val WEEK = (7 * 24 * 3600).toLong()
    private const val DAY = (24 * 3600).toLong()
    private const val HOUR: Long = 3600
    private const val MINUTE: Long = 60

    fun timeFormat(time: Long): String {
        val curTime = System.currentTimeMillis()
        val difTime = (curTime - time) / 1000
        if (difTime < 0)
            return ""
        return if (difTime < DAY) {
            SimpleDateFormat("HH:mm").format(time)
        } else {
            SimpleDateFormat("MM/dd/yyyy").format(time)
        }
    }

    fun timeFormat1(time: Long): String {
        if (time < 0)
            return ""
        return SimpleDateFormat("MM-dd-yyyy").format(time)
    }
}