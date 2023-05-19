package framework.telegram.support.tools

import android.content.Context
import android.text.TextUtils
import framework.telegram.support.BaseApp
import framework.telegram.support.R

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {

    private const val YEAR = (365 * 24 * 3600).toLong()
    private const val MONTH = (30 * 24 * 3600).toLong()
    private const val WEEK = (7 * 24 * 3600).toLong()
    private const val DAY = (24 * 3600).toLong()
    private const val HOUR: Long = 3600
    private const val MINUTE: Long = 60

    fun currentTimeMillis(): Long {
        val zone = TimeZone.getTimeZone("GMT-8:00")
        return Calendar.getInstance(zone).timeInMillis
    }

    fun isTime24(context: Context): Boolean {
        val cv = context.contentResolver
        val strTimeFormat = android.provider.Settings.System.getString(
            cv,
            android.provider.Settings.System.TIME_12_24
        )
        return if (TextUtils.isEmpty(strTimeFormat)) {
            false
        } else strTimeFormat == "24"
    }

    fun timeFormatForDynamic(timestamp: Long, currentTime: Long): String {
        val timeGap = (currentTime - timestamp) / 1000// 与现在时间相差秒数
        val timeStr: String

        timeStr = when {
            timeGap < MINUTE -> BaseApp.app.getString(R.string.just_online)
            timeGap < HOUR -> String.format(
                BaseApp.app.getString(R.string.min_online),
                (timeGap / MINUTE).toString()
            )
            timeGap < DAY -> String.format(
                BaseApp.app.getString(R.string.hou_online),
                (timeGap / HOUR).toString()
            )
            timeGap < WEEK -> String.format(
                BaseApp.app.getString(R.string.day_online),
                (timeGap / DAY).toString()
            )
            timeGap < MONTH -> String.format(
                BaseApp.app.getString(R.string.week_online),
                timeGap / WEEK
            )
            else -> {
                val dateFormat = SimpleDateFormat("dd-MM-yyyy ")
                BaseApp.app.getString(R.string.online_time, dateFormat.format(timestamp))
            }
        }
        return timeStr
    }

    fun timeFormatForDeadlineMillis(remainTime: Long): String {
        val timeGap = (remainTime) / 1000 // 与现在时间相差秒数
        val timeStr: String

        timeStr = when {
            timeGap < MINUTE -> String.format(BaseApp.app.getString(R.string.sec_mat), timeGap)
            timeGap < HOUR -> String.format(
                BaseApp.app.getString(R.string.min_mat),
                timeGap / MINUTE
            )
            timeGap < DAY -> String.format(BaseApp.app.getString(R.string.hou_mat), timeGap / HOUR)
            timeGap < WEEK -> String.format(BaseApp.app.getString(R.string.day_mat), timeGap / DAY)
            timeGap < MONTH -> String.format(
                BaseApp.app.getString(R.string.week_mat),
                timeGap / WEEK
            )
            else -> {
                (timeGap / MONTH).toString() + BaseApp.app.getString(R.string.mon)
            }
        }
        return timeStr
    }

    fun timeFormatForDeadline(timeGap: Int): String {
        return when {
            timeGap < MINUTE -> String.format(BaseApp.app.getString(R.string.sec_mat), timeGap)
            timeGap < HOUR -> String.format(
                BaseApp.app.getString(R.string.min_mat),
                timeGap / MINUTE
            )
            timeGap < DAY -> String.format(BaseApp.app.getString(R.string.hou_mat), timeGap / HOUR)
            else -> {
                String.format(BaseApp.app.getString(R.string.day_mat), timeGap / DAY)
            }
        }
    }

    fun timeFormatToChat(context: Context, time: Long): String {
        // 定义最终返回的结果字符串
        val timeFormat: String
        val c = Calendar.getInstance()
        c.timeInMillis = time

        val currentTime = Calendar.getInstance()

        val hour = c.get(Calendar.HOUR_OF_DAY)
        val day = c.get(Calendar.DAY_OF_MONTH)
        val week = c.get(Calendar.WEEK_OF_MONTH)
        val month = c.get(Calendar.MONTH)
        val year = c.get(Calendar.YEAR)
        val min = c.get(Calendar.MINUTE)

        val hourStr: String
        hourStr = if (hour < 10) {
            "0$hour"
        } else {
            "" + hour
        }
        val minStr: String
        minStr = if (min < 10) {
            "0$min"
        } else {
            "" + min
        }
        timeFormat =
            if (year == currentTime.get(Calendar.YEAR) && month == currentTime.get(Calendar.MONTH)) {
                if (currentTime.get(Calendar.DAY_OF_MONTH) == day) {//同日
                    "$hourStr:$minStr"
                } else if (currentTime.get(Calendar.DAY_OF_MONTH) == day + 1) {//昨天
                    BaseApp.app.getString(R.string.yesterday)
                } else if (year == currentTime.get(Calendar.YEAR) && month == currentTime.get(
                        Calendar.MONTH
                    ) && currentTime.get(Calendar.DAY_OF_MONTH) == day + 2
                ) {//前天
                    BaseApp.app.getString(R.string.the_day_before_yesterday)
                } else if (week == currentTime.get(Calendar.WEEK_OF_MONTH) && currentTime.get(
                        Calendar.DAY_OF_MONTH
                    ) >= day + 3 && currentTime.get(Calendar.DAY_OF_MONTH) <= day + 7
                ) {
                    val arr = arrayOf(
                        BaseApp.app.getString(R.string.sun),
                        BaseApp.app.getString(R.string.monday),
                        BaseApp.app.getString(R.string.tue),
                        BaseApp.app.getString(R.string.wed),
                        BaseApp.app.getString(R.string.thurs),
                        BaseApp.app.getString(R.string.fri),
                        BaseApp.app.getString(R.string.Sat)
                    )
                    arr[(c.get(Calendar.DAY_OF_WEEK) - 1) % 7] + " $hourStr:$minStr"
                } else {
                    "" + (month + 1) + "-" + day + "-" + year.toString()
                }
            } else {
                "" + (month + 1) + "-" + day + "-" + year.toString()
            }
        return timeFormat
    }

    fun timeFormatToMessageTimeline(context: Context, time: Long): String {
        // 定义最终返回的结果字符串
        val timeFormat: String
        val c = Calendar.getInstance()
        c.timeInMillis = time

        val currentTime = Calendar.getInstance()

        val hour = c.get(Calendar.HOUR_OF_DAY)
        val day = c.get(Calendar.DAY_OF_MONTH)
        val week = c.get(Calendar.WEEK_OF_MONTH)
        val month = c.get(Calendar.MONTH)
        val year = c.get(Calendar.YEAR)
        val min = c.get(Calendar.MINUTE)

        val hourStr =
            if (hour < 10) {
                "0$hour"
            } else {
                "" + hour
            }

        val minStr = if (min < 10) {
            "0$min"
        } else {
            "" + min
        }
        timeFormat =
            if (year == currentTime.get(Calendar.YEAR) && month == currentTime.get(Calendar.MONTH)) {
                if (currentTime.get(Calendar.DAY_OF_MONTH) == day) { //同日
                    "$hourStr:$minStr"
                } else if (currentTime.get(Calendar.DAY_OF_MONTH) == day + 1) {//昨天
                    String.format(BaseApp.app.getString(R.string.yesterday_mat), hourStr, minStr)
                } else if (currentTime.get(Calendar.DAY_OF_MONTH) == day + 2) {//前天
                    String.format(
                        BaseApp.app.getString(R.string.the_day_before_yesterday_mat),
                        hourStr,
                        minStr
                    )
                } else {
                    if (currentTime.get(Calendar.WEEK_OF_MONTH) == week) {
                        val arr = arrayOf(
                            BaseApp.app.getString(R.string.sun),
                            BaseApp.app.getString(R.string.monday),
                            BaseApp.app.getString(R.string.tue),
                            BaseApp.app.getString(R.string.wed),
                            BaseApp.app.getString(R.string.thurs),
                            BaseApp.app.getString(R.string.fri),
                            BaseApp.app.getString(R.string.Sat)
                        )
                        arr[currentTime.get(Calendar.DAY_OF_WEEK) - 1] + " $hourStr:$minStr"
                    } else {
                        "" + (month + 1) + "-" + day + "-" + year.toString() + " " + hourStr + ":" + minStr
                    }
                }
            } else {
                "" + (month + 1) + "-" + day + "-" + year.toString() + " " + hourStr + ":" + minStr
            }
        return timeFormat
    }

    fun timeFormatToMediaDuration(t: Long): String {
        val time = t / 1000
        var returnValue = if (time > 0) {
            val h = time / 3600
            val mAnds = time % 3600
            val m = mAnds / 60
            val s = mAnds % 60
            val hour = if (h < 10) "0$h" else h.toString()
            val min = if (m < 10) "0$m" else m.toString()
            val ss = if (s < 10) "0$s" else s.toString()
            "$hour:$min:$ss"
        } else {
            "00:00:00"
        }

        if (findCount(returnValue, ":") > 1 && returnValue.startsWith("00:")) {
            returnValue = returnValue.substring(3)
        }

        return returnValue
    }

    private fun findCount(text: String, find: String): Int {
        return (text.length - text.replace(find, "").length) / find.length
    }

    fun getYYDFormatTime(timeMills: Long): String {
        return try {
            if (timeMills == 0L) {
                "-"
            } else {
                val formatter = SimpleDateFormat("MM-dd-yyyy")
                formatter.format(timeMills)
            }
        } catch (var3: Exception) {
            "-"
        }

    }

    fun getYYDFormatTime2(timeMills: Long): String {
        return try {
            if (timeMills == 0L) {
                "-"
            } else {
                val formatter = SimpleDateFormat("MM/dd/yyyy")
                formatter.format(timeMills)
            }
        } catch (var3: Exception) {
            "-"
        }

    }

    fun getHMFormatTime(timeMills: Long): String {
        return try {
            return if (timeMills == 0L) {
                "-"
            } else {
                val formatter = SimpleDateFormat("HH:mm")
                formatter.format(timeMills)
            }
        } catch (var3: Exception) {
            "-"
        }
    }

    fun getYMDHMSFormatTime(timeMills: Long): String {
        return try {
            return if (timeMills == 0L) {
                "-"
            } else {
                val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                formatter.format(timeMills)
            }
        } catch (var3: Exception) {
            "-"
        }
    }
}
