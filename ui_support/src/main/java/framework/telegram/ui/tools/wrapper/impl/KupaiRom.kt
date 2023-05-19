package framework.telegram.ui.tools.wrapper.impl

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import framework.telegram.ui.tools.wrapper.Constant
import framework.telegram.ui.tools.wrapper.DialogImpl
import framework.telegram.ui.tools.wrapper.WhiteIntentWrapper


/**
 * Created by lzh
 * time: 2018/4/17.
 * info:
 */
class KupaiRom : SystemRom() {

    override val tag = "KupaiRom"

    //酷派 自启动管理
    private val COOLPAD = 0x70

    override fun getIntent(context: Context, sIntentWrapperList: MutableList<WhiteIntentWrapper>, commandList: List<String>) {
        super.getIntent(context, sIntentWrapperList, commandList)
        (0 until commandList.size).forEach {
            when (commandList[it]) {
                Constant.COMMAND_START_YOURSELF -> {
                    //酷派 自启动管理
                    Log.d("WhiteIntent", "酷派手机")
                    val coolpadIntent = context.packageManager.getLaunchIntentForPackage("com.yulong.android.security")
                    Log.d("WhiteIntent", "尝试通过getLaunchIntentForPackage(com.yulong.android.security)跳转酷管家")
                    if (coolpadIntent != null) {
                        coolpadIntent.putExtra("packageName", context.packageName)
                        coolpadIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        Log.d("WhiteIntent", "可通过getLaunchIntentForPackage(com.yulong.android.security)跳转酷管家")
                        sIntentWrapperList.add(WhiteIntentWrapper(coolpadIntent, COOLPAD, Constant.COMMAND_START_YOURSELF))
                    } else {
                        Log.e("WhiteIntent", "不可通过getLaunchIntentForPackage(com.yulong.android.security)跳转酷管家")
                    }
                }
            }
        }
    }

    override fun showDialog(reason: String, a: Activity,  wrapperList: List<WhiteIntentWrapper>) {
        super.showDialog(reason, a,  wrapperList)
        val applicationName = WhiteIntentWrapper.getApplicationName(a)
//        when (intent.type) {
//            COOLPAD -> {
//                DialogImpl(a, WhiteIntentWrapper.getString(a, "reason_cp_title", applicationName),
//                        WhiteIntentWrapper.getString(a, "reason_cp_content", reason, applicationName, applicationName),
//                        WhiteIntentWrapper.getString(a, "ok"),
//                        WhiteIntentWrapper.getString(a, "cancel"), {
//                    intent.startActivitySafely(a)
//                })
//                wrapperList.add(intent)
//            }
//        }
    }
}