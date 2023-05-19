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
class GioneeRom : SystemRom() {

    override val tag = "GioneeRom"

    //金立 应用自启
    private val GIONEE = 0x80

    override fun getIntent(context: Context, sIntentWrapperList: MutableList<WhiteIntentWrapper>, commandList: List<String>) {
        super.getIntent(context, sIntentWrapperList, commandList)

        (0 until commandList.size).forEach {
            when (commandList[it]) {
                COMMAND_START_YOURSELF -> {
                    //金立 应用自启
                    Log.d("WhiteIntent", "金立手机")
                    val gioneeIntent = Intent()
                    gioneeIntent.component = ComponentName("com.gionee.softmanager", "com.gionee.softmanager.MainActivity")
                    gioneeIntent.putExtra("packageName", context.packageName)
                    gioneeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    Log.d("WhiteIntent", "尝试通过com.gionee.softmanager.MainActivity跳转自启动设置")
                    if (WhiteIntentWrapper.doesActivityExists(context, gioneeIntent)) {
                        Log.d("WhiteIntent", "可通过com.gionee.softmanager.MainActivity跳转自启动设置")
                        sIntentWrapperList.add(WhiteIntentWrapper(gioneeIntent, GIONEE, COMMAND_START_YOURSELF))
                    } else {
                        Log.e("WhiteIntent", "不可通过com.gionee.softmanager.MainActivity跳转自启动设置")
                    }
                }
            }
        }
    }

    override fun showDialog(reason: String, a: Activity,wrapperList: List<WhiteIntentWrapper>) {
        super.showDialog(reason, a,  wrapperList)
        val applicationName = WhiteIntentWrapper.getApplicationName(a)
//        when (intent.type) {
//            GIONEE -> {
//                DialogImpl(a, WhiteIntentWrapper.getString(a, "reason_jl_title", applicationName),
//                        WhiteIntentWrapper.getString(a, "reason_jl_content", reason, applicationName, applicationName, applicationName),
//                        WhiteIntentWrapper.getString(a, "ok"),
//                        WhiteIntentWrapper.getString(a, "cancel"), {
//                    intent.startActivitySafely(a)
//                })
//                wrapperList.add(intent)
//            }
//        }
    }
}