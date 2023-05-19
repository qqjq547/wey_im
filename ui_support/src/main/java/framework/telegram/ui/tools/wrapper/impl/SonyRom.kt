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
class SonyRom : SystemRom() {

    override val tag = "SonyRom"

    //索尼 自启管理
    private val SONY = 0x120

    override fun getIntent(context: Context, sIntentWrapperList: MutableList<WhiteIntentWrapper>, commandList: List<String>) {
        super.getIntent(context, sIntentWrapperList, commandList)
        (0 until commandList.size).forEach {
            when (commandList[it]) {
                COMMAND_START_YOURSELF -> {
                    //索尼自启动应用程序管理
                    Log.d("WhiteIntent", "索尼手机")
                    val sonyIntent = Intent()
                    sonyIntent.component = ComponentName("com.sonymobile.cta", "com.sonymobile.cta.SomcCTAMainActivity")
                    sonyIntent.putExtra("packageName", context.packageName)
                    sonyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    Log.d("WhiteIntent", "尝试通过com.sonymobile.cta.SomcCTAMainActivity跳转自启动设置")
                    if (WhiteIntentWrapper.doesActivityExists(context, sonyIntent)) {
                        Log.d("WhiteIntent", "可通过com.sonymobile.cta.SomcCTAMainActivity跳转自启动设置")
                        sIntentWrapperList.add(WhiteIntentWrapper(sonyIntent, SONY, COMMAND_START_YOURSELF))
                    } else {
                        Log.e("WhiteIntent", "不可通过com.sonymobile.cta.SomcCTAMainActivity跳转自启动设置")
                    }
                }
            }
        }
    }

    override fun showDialog(reason: String, a: Activity,  wrapperList: List<WhiteIntentWrapper>) {
        super.showDialog(reason, a, wrapperList)
        val applicationName = WhiteIntentWrapper.getApplicationName(a)
//        when (intent.type) {
//            SONY -> {
//                DialogImpl(a, WhiteIntentWrapper.getString(a, "reason_sony_title", applicationName),
//                        WhiteIntentWrapper.getString(a, "reason_sony_content", reason, applicationName, applicationName),
//                        WhiteIntentWrapper.getString(a, "ok"),
//                        WhiteIntentWrapper.getString(a, "cancel"), {
//                    intent.startActivitySafely(a)
//                })
//                wrapperList.add(intent)
//            }
//        }
    }
}