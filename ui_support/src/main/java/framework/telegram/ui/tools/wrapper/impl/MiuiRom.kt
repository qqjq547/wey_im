package framework.telegram.ui.tools.wrapper.impl

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.util.Log
import framework.telegram.ui.tools.wrapper.Constant.COMMAND_GOD
import framework.telegram.ui.tools.wrapper.Constant.COMMAND_START_YOURSELF
import framework.telegram.ui.tools.wrapper.DialogImpl
import framework.telegram.ui.tools.wrapper.WhiteIntentWrapper
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


/**
 * Created by lzh
 * time: 2018/4/17.
 * info:
 */
class MiuiRom : SystemRom() {

    override val tag = "MiuiRom"

    //小米 自启动管理
    private val XIAOMI = 0x20
    //小米 神隐模式
    private val XIAOMI_GOD = 0x21

    override fun getIntent(context: Context, sIntentWrapperList: MutableList<WhiteIntentWrapper>, commandList: List<String>) {
        super.getIntent(context, sIntentWrapperList, commandList)
        (0 until commandList.size).forEach {
            when (commandList[it]) {
                COMMAND_START_YOURSELF -> {
                    //小米 自启动管理
                    Log.d("WhiteIntent", "小米手机" + getMiuiVersion())
                    var xiaomiIntent = Intent()
                    xiaomiIntent.action = "miui.intent.action.OP_AUTO_START"
                    xiaomiIntent.addCategory(Intent.CATEGORY_DEFAULT)
                    xiaomiIntent.putExtra("packageName", context.packageName)
                    xiaomiIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    Log.d("WhiteIntent", "尝试通过Action=miui.intent.action.OP_AUTO_START跳转自启动设置页")
                    if (WhiteIntentWrapper.doesActivityExists(context, xiaomiIntent)) {
                        Log.d("WhiteIntent", "可通过Action=miui.intent.action.OP_AUTO_START跳转自启动设置页")
                        sIntentWrapperList.add(WhiteIntentWrapper(xiaomiIntent, XIAOMI, COMMAND_START_YOURSELF))
                    } else {
                        Log.e("WhiteIntent", "不可通过Action=miui.intent.action.OP_AUTO_START跳转自启动设置页")
                        xiaomiIntent = Intent()
                        xiaomiIntent.component = ComponentName.unflattenFromString("com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity")
                        xiaomiIntent.putExtra("packageName", context.packageName)
                        xiaomiIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        Log.d("WhiteIntent", "尝试通过com.miui.permcenter.autostart.AutoStartManagementActivity跳转自启动设置页")
                        if (WhiteIntentWrapper.doesActivityExists(context, xiaomiIntent)) {
                            Log.d("WhiteIntent", "可通过com.miui.permcenter.autostart.AutoStartManagementActivity跳转自启动设置页")
                            sIntentWrapperList.add(WhiteIntentWrapper(xiaomiIntent, XIAOMI, COMMAND_START_YOURSELF))
                        } else {
                            Log.e("WhiteIntent", "不可通过com.miui.permcenter.autostart.AutoStartManagementActivity跳转自启动设置页")
                            xiaomiIntent = Intent("miui.intent.action.APP_PERM_EDITOR")
                            xiaomiIntent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity")
                            xiaomiIntent.putExtra("extra_pkgname", context.packageName)
                            xiaomiIntent.putExtra("packageName", context.packageName)
                            xiaomiIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            Log.d("WhiteIntent", "尝试通过com.miui.permcenter.permissions.AppPermissionsEditorActivity跳转自启动设置页")
                            if (WhiteIntentWrapper.doesActivityExists(context, xiaomiIntent)) {
                                Log.d("WhiteIntent", "可通过com.miui.permcenter.permissions.AppPermissionsEditorActivity跳转自启动设置页")
                                sIntentWrapperList.add(WhiteIntentWrapper(xiaomiIntent, XIAOMI, COMMAND_START_YOURSELF))
                            } else {
                                Log.e("WhiteIntent", "不可通过com.miui.permcenter.permissions.AppPermissionsEditorActivity跳转自启动设置页")
                                xiaomiIntent = Intent("miui.intent.action.APP_PERM_EDITOR")
                                xiaomiIntent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                                xiaomiIntent.putExtra("extra_pkgname", context.packageName)
                                xiaomiIntent.putExtra("packageName", context.packageName)
                                xiaomiIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                Log.d("WhiteIntent", "尝试通过com.miui.permcenter.permissions.PermissionsEditorActivity跳转自启动设置页")
                                if (WhiteIntentWrapper.doesActivityExists(context, xiaomiIntent)) {
                                    Log.d("WhiteIntent", "可通过com.miui.permcenter.permissions.PermissionsEditorActivity跳转自启动设置页")
                                    sIntentWrapperList.add(WhiteIntentWrapper(xiaomiIntent, XIAOMI, COMMAND_START_YOURSELF))
                                } else {
                                    Log.e("WhiteIntent", "不可通过com.miui.permcenter.permissions.PermissionsEditorActivity跳转自启动设置页")
                                }
                            }
                        }
                    }
                }
                COMMAND_GOD -> {
                    //小米 神隐模式
                    val xiaomiGodIntent = Intent()
                    xiaomiGodIntent.component = ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
                    xiaomiGodIntent.putExtra("package_name", context.packageName)
                    xiaomiGodIntent.putExtra("package_label", WhiteIntentWrapper.getApplicationName(context))
                    xiaomiGodIntent.putExtra("packageName", context.packageName)
                    xiaomiGodIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    Log.d("WhiteIntent", "尝试通过com.miui.powerkeeper.ui.HiddenAppsConfigActivity跳转神隐模式设置页")
                    if (WhiteIntentWrapper.doesActivityExists(context, xiaomiGodIntent)) {
                        Log.d("WhiteIntent", "可通过com.miui.powerkeeper.ui.HiddenAppsConfigActivity跳转神隐模式设置页")
                        sIntentWrapperList.add(WhiteIntentWrapper(xiaomiGodIntent, XIAOMI_GOD, COMMAND_GOD))
                    } else {
                        Log.e("WhiteIntent", "不可通过com.miui.powerkeeper.ui.HiddenAppsConfigActivity跳转神隐模式设置页")
                    }
                }
            }
        }
    }

    private fun getMiuiVersion(): String? {
        val propName = "ro.miui.ui.version.name"
        val line: String
        var input: BufferedReader? = null
        try {
            val p = Runtime.getRuntime().exec("getprop " + propName)
            input = BufferedReader(
                    InputStreamReader(p.inputStream), 1024)
            line = input!!.readLine()
            input!!.close()
        } catch (ex: IOException) {
            return null
        } finally {
            if (input != null) {
                try {
                    input!!.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
        return line
    }

    override fun showDialog(reason: String, a: Activity, wrapperList: List<WhiteIntentWrapper>) {
        super.showDialog(reason, a,  wrapperList)
        val applicationName = WhiteIntentWrapper.getApplicationName(a)
//        when (intent.type) {
//            XIAOMI_GOD -> {
//                try {
//                    AlertDialog.Builder(a)
//                            .setCancelable(false)
//                            .setTitle(WhiteIntentWrapper.getString(a, "reason_xm_god_title", WhiteIntentWrapper.getApplicationName(a)))
//                            .setMessage(WhiteIntentWrapper.getString(a, "reason_xm_god_content", reason, WhiteIntentWrapper.getApplicationName(a), WhiteIntentWrapper.getApplicationName(a)))
//                            .setPositiveButton(WhiteIntentWrapper.getString(a, "ok"), DialogInterface.OnClickListener { d, w -> intent.startActivitySafely(a) })
//                            .setNegativeButton(WhiteIntentWrapper.getString(a, "cancel"), null)
//                            .show()
//                    wrapperList.add(intent)
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//                DialogImpl(a, WhiteIntentWrapper.getString(a, "reason_xm_god_title", applicationName),
//                        WhiteIntentWrapper.getString(a, "reason_xm_god_content", reason, applicationName, applicationName),
//                        WhiteIntentWrapper.getString(a, "ok"),
//                        WhiteIntentWrapper.getString(a, "cancel"), {
//                    intent.startActivitySafely(a)
//                })
//                wrapperList.add(intent)
//            }
//            XIAOMI -> {
//                DialogImpl(a, WhiteIntentWrapper.getString(a, "reason_xm_title"),
//                        WhiteIntentWrapper.getString(a, "reason_xm_content", applicationName),
//                        WhiteIntentWrapper.getString(a, "ok"),
//                        WhiteIntentWrapper.getString(a, "cancel"), {
//                    intent.startActivitySafely(a)
//                })
//                wrapperList.add(intent)
//            }
//        }
    }
}