package framework.telegram.ui.tools.wrapper.impl

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import framework.telegram.ui.tools.wrapper.Constant.COMMAND_START_YOURSELF
import framework.telegram.ui.tools.wrapper.Constant.COMMAND_WHITE_LIST
import framework.telegram.ui.tools.wrapper.DialogImpl
import framework.telegram.ui.tools.wrapper.WhiteIntentWrapper


/**
 * Created by lzh
 * time: 2018/4/17.
 * info:
 */
class EmuiRom : SystemRom() {

    override val tag = "EmuiRom"

    //华为 自启管理
    private val HUAWEI = 0x10
    //华为 锁屏清理
    private val HUAWEI_GOD = 0x11

    override fun getIntent(context: Context, sIntentWrapperList: MutableList<WhiteIntentWrapper>, commandList: List<String>) {
        super.getIntent(context, sIntentWrapperList, commandList)
        var huaweiIntent = Intent()
        (0 until commandList.size).forEach {
            when (commandList[it]) {
                COMMAND_START_YOURSELF -> {
                    huaweiIntent.action = "huawei.intent.action.HSM_BOOTAPP_MANAGER"
                    huaweiIntent.putExtra("packageName", context.packageName)
                    huaweiIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    Log.d(tag, "尝试通过Action=huawei.intent.action.HSM_BOOTAPP_MANAGER跳转自启动设置")

                    if (WhiteIntentWrapper.doesActivityExists(context, huaweiIntent)) {
                        Log.d(tag, "可通过Action=huawei.intent.action.HSM_BOOTAPP_MANAGER跳转自启动设置")
                        sIntentWrapperList.add(WhiteIntentWrapper(huaweiIntent, HUAWEI, COMMAND_START_YOURSELF))
                    } else {
                        Log.e(tag, "不可通过Action==huawei.intent.action.HSM_BOOTAPP_MANAGER跳转自启动设置")
                        huaweiIntent = Intent()
                        huaweiIntent.component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
                        huaweiIntent.putExtra("packageName", context.packageName)
                        huaweiIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        Log.d(tag, "尝试通过com.huawei.systemmanager.optimize.process.ProtectActivity跳转自启动设置")
                        if (WhiteIntentWrapper.doesActivityExists(context, huaweiIntent)) {
                            Log.d(tag, "可通过com.huawei.systemmanager.optimize.process.ProtectActivity跳转自启动设置")
                            sIntentWrapperList.add(WhiteIntentWrapper(huaweiIntent, HUAWEI, COMMAND_START_YOURSELF))
                        } else {
                            Log.e(tag, "不可通过com.huawei.systemmanager.optimize.process.ProtectActivity跳转自启动设置")
                            huaweiIntent = Intent()
                            huaweiIntent.component = ComponentName("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity")
                            huaweiIntent.putExtra("packageName", context.packageName)
                            huaweiIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            Log.d(tag, "尝试通过com.huawei.permissionmanager.ui.MainActivity跳转自启动设置")
                            if (WhiteIntentWrapper.doesActivityExists(context, huaweiIntent)) {
                                Log.d(tag, "可通过com.huawei.permissionmanager.ui.MainActivity跳转自启动设置")
                                sIntentWrapperList.add(WhiteIntentWrapper(huaweiIntent, HUAWEI, COMMAND_START_YOURSELF))
                            } else {
                                Log.e(tag, "不可通过com.huawei.permissionmanager.ui.MainActivity跳转自启动设置")
                            }
                        }
                    }
                }
                COMMAND_WHITE_LIST -> {
                    val huaweiGodIntent = Intent()
                    huaweiGodIntent.component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
                    huaweiIntent.putExtra("packageName", context.packageName)
                    huaweiIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    Log.d(tag, "尝试通过com.huawei.systemmanager.optimize.process.ProtectActivity跳转锁屏清理白名单设置页")
                    if (WhiteIntentWrapper.doesActivityExists(context, huaweiGodIntent)) {
                        Log.d(tag, "可通过com.huawei.systemmanager.optimize.process.ProtectActivity跳转锁屏清理白名单设置页")
                        sIntentWrapperList.add(WhiteIntentWrapper(huaweiGodIntent, HUAWEI_GOD, COMMAND_WHITE_LIST))
                    } else {
                        Log.e(tag, "不可通过com.huawei.systemmanager.optimize.process.ProtectActivity跳转锁屏清理白名单设置页")
                    }
                }
            }
        }
    }

    override fun showDialog(reason: String, a: Activity,  wrapperList: List<WhiteIntentWrapper>) {
        super.showDialog(reason, a,  wrapperList)
        val applicationName = WhiteIntentWrapper.getApplicationName(a)
//        when (intent.type) {
//            HUAWEI_GOD -> {
//                DialogImpl(a, WhiteIntentWrapper.getString(a, "reason_hw_god_title", applicationName),
//                        WhiteIntentWrapper.getString(a, "reason_hw_god_content", reason, applicationName, applicationName),
//                        WhiteIntentWrapper.getString(a, "ok"),
//                        WhiteIntentWrapper.getString(a, "cancel"), {
//                    intent.startActivitySafely(a)
//                })
//                wrapperList.add(intent)
//
//            }
//            HUAWEI -> {
//                DialogImpl(a, WhiteIntentWrapper.getString(a, "reason_hw_title"),
//                        WhiteIntentWrapper.getString(a, "reason_hw_content", applicationName),
//                        WhiteIntentWrapper.getString(a, "ok"),
//                        WhiteIntentWrapper.getString(a, "cancel"), {
//                    intent.startActivitySafely(a)
//                })
//                wrapperList.add(intent)
//
//            }
//        }
    }
}