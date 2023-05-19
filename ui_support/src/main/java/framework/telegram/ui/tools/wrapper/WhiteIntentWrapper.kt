package framework.telegram.ui.tools.wrapper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import framework.telegram.ui.tools.RomUtils
import framework.telegram.ui.tools.wrapper.impl.*
import framework.telegram.ui.tools.wrapper.impl.EmuiRom


/**
 * Created by lzh
 * time: 2018/4/17.
 * info:
 */
class WhiteIntentWrapper {

    val tag = "WhiteIntent"

    var intent: Intent? = null
    var type: Int = 0
    var command:String=""

    constructor(intent: Intent, type: Int,command:String) {
        this.intent = intent
        this.type = type
        this.command = command

    }

    /**
     * 安全地启动一个Activity
     */
    fun startActivitySafely(activityContext: Activity) {
        try {
            activityContext.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    companion object {

        private var phoneRom: IRom? = null

        private val sIntentWrapperList by lazy { mutableListOf<WhiteIntentWrapper>() }

        /**
         * 判断本机上是否有能处理当前Intent的Activity
         */
        fun doesActivityExists(context: Context, intent: Intent): Boolean {
            val pm = context.packageManager
            val list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            return list != null && list.size > 0
        }

        private fun getIntentWrapperList(context: Context,commandList:List<String>): List<WhiteIntentWrapper> {
            sIntentWrapperList.clear()
            when {
                RomUtils.isEmui -> phoneRom = EmuiRom()
                RomUtils.isMiui -> phoneRom = MiuiRom()
                RomUtils.isVivo -> phoneRom = VivoRom()
                RomUtils.isOppo -> phoneRom = OppoRom()
                RomUtils.isLeTv -> phoneRom = LetvRom()
                RomUtils.isFlyme -> phoneRom = FlymeRom()
                RomUtils.isSamsung -> phoneRom = SamsungRom()
                RomUtils.isLenovo -> phoneRom = LenovoRom()
                RomUtils.isZTE -> phoneRom = ZTERom()
                RomUtils.isGionee -> phoneRom = GioneeRom()
                RomUtils.isKupai -> phoneRom = KupaiRom()
                RomUtils.isSony -> phoneRom = SonyRom()
                RomUtils.isLG -> phoneRom = LGRom()
                RomUtils.isHTC -> phoneRom = HTCRom()
                else -> {
                    phoneRom = OtherRom()
                }
            }
            phoneRom?.getIntent(context, sIntentWrapperList,commandList)
            return sIntentWrapperList
        }

        fun whiteListMatters(activity: Activity?, reason: String,commandList:MutableList<String>){
            if (activity != null) {
                val intentWrapperList = getIntentWrapperList(activity,commandList)
                phoneRom?.showDialog(reason, activity,  intentWrapperList)
            }
        }

        fun getApplicationName(context: Context): String {
            val pm: PackageManager
            val ai: ApplicationInfo
            return try {
                pm = context.packageManager
                ai = pm.getApplicationInfo(context.packageName, 0)
                pm.getApplicationLabel(ai).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                context.packageName
            }

        }

        fun getString(context: Context, name: String, vararg format: Any): String {
            try {
                val resId = context.resources.getIdentifier(name, "string", context.packageName)
                if (resId > 0) {
                    return context.getString(resId, *format)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return ""
        }

        @SuppressLint("LongLogTag")
        @RequiresApi(api = Build.VERSION_CODES.M)
        fun isSystemWhiteList(context:Context ):Boolean {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = context.getPackageName()
            val isWhite = pm.isIgnoringBatteryOptimizations(packageName)
            Log.e("SystemUtil","SystemUtil.isSystemWhiteList.packageName="+packageName+",isWhite="+isWhite);
            return isWhite;
        }
    }


}