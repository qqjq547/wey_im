package framework.telegram.ui.tools.wrapper.impl

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import framework.telegram.ui.tools.wrapper.Constant.COMMAND_START_YOURSELF
import framework.telegram.ui.tools.wrapper.DialogImpl
import framework.telegram.ui.tools.wrapper.WhiteIntentWrapper


/**
 * Created by lzh
 * time: 2018/4/17.
 * info:
 */
class LGRom : SystemRom() {

    override val tag = "LGRom"

    //LG 自启管理
    private val LG = 0x130

    override fun getIntent(context: Context, sIntentWrapperList: MutableList<WhiteIntentWrapper>, commandList: List<String>) {
        super.getIntent(context, sIntentWrapperList, commandList)
        (0 until commandList.size).forEach {
            when (commandList[it]) {
                COMMAND_START_YOURSELF -> {
                    //LG自启动应用程序管理
                    Log.d("WhiteIntent", "LG手机")
                    val lgIntent = Intent("android.intent.action.MAIN")
                    lgIntent.component = ComponentName("com.android.settings", "com.android.settings.Settings\$AccessLockSummaryActivity")
                    lgIntent.putExtra("packageName", context.packageName)
                    lgIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    Log.d("WhiteIntent", "尝试通过com.android.settings.Settings\$AccessLockSummaryActivity跳转自启动设置")
                    if (WhiteIntentWrapper.doesActivityExists(context, lgIntent)) {
                        Log.d("WhiteIntent", "可通过com.android.settings.Settings\$AccessLockSummaryActivity跳转自启动设置")
                        sIntentWrapperList.add(WhiteIntentWrapper(lgIntent, LG, COMMAND_START_YOURSELF))
                    } else {
                        Log.e("WhiteIntent", "不可通过com.android.settings.Settings\$AccessLockSummaryActivity跳转自启动设置")
                    }
                }
            }
        }
    }

    override fun showDialog(reason: String, a: Activity, wrapperList: List<WhiteIntentWrapper>) {
        super.showDialog(reason, a,  wrapperList)
        val applicationName = WhiteIntentWrapper.getApplicationName(a)
//        when (intent.type) {
//            LG -> {
//                DialogImpl(a, WhiteIntentWrapper.getString(a, "reason_lg_title", applicationName),
//                        WhiteIntentWrapper.getString(a, "reason_lg_content", reason, applicationName, applicationName),
//                        WhiteIntentWrapper.getString(a, "ok"),
//                        WhiteIntentWrapper.getString(a, "cancel"), {
//                    intent.startActivitySafely(a)
//                })
//                wrapperList.add(intent)
//            }
//        }
    }
}