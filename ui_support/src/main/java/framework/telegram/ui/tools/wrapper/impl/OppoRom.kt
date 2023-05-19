package framework.telegram.ui.tools.wrapper.impl

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import framework.telegram.ui.tools.wrapper.Constant.COMMAND_BACKSTAGE
import framework.telegram.ui.tools.wrapper.Constant.COMMAND_START_YOURSELF
import framework.telegram.ui.tools.wrapper.DialogImpl
import framework.telegram.ui.tools.wrapper.WhiteIntentWrapper


/**
 * Created by lzh
 * time: 2018/4/17.
 * info:
 */
class OppoRom : SystemRom() {

    override val tag = "OppoRom"

    //Oppo 自启动管理
    private val OPPO_PM = 0x50//权限管理
    private val OPPO_SM = 0x51//自启动管理
    private val OPPO_GOD = 0x52

    override fun getIntent(context: Context, sIntentWrapperList: MutableList<WhiteIntentWrapper>, commandList: List<String>) {
        super.getIntent(context, sIntentWrapperList, commandList)
        (0 until commandList.size).forEach {
            when (commandList[it]) {
                COMMAND_START_YOURSELF -> {
                    //Oppo 自启动管理
                    Log.d("WhiteIntent", "Oppo手机")
                    var oppoIntent = Intent()
                    oppoIntent.component = ComponentName.unflattenFromString("com.coloros.safecenter/com.coloros.privacypermissionsentry.PermissionTopActivity")
                    oppoIntent.putExtra("packageName", context.packageName)
                    oppoIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    Log.d("WhiteIntent", "尝试通过com.coloros.privacypermissionsentry.PermissionTopActivity跳转权限隐私设置")
                    if (WhiteIntentWrapper.doesActivityExists(context, oppoIntent)) {
                        Log.d("WhiteIntent", "可通过com.coloros.privacypermissionsentry.PermissionTopActivity跳转权限隐私设置")
                        sIntentWrapperList.add(WhiteIntentWrapper(oppoIntent, OPPO_PM, COMMAND_START_YOURSELF))
                    } else {
                        Log.e("WhiteIntent", "不可通过com.coloros.privacypermissionsentry.PermissionTopActivity跳转权限隐私设置")
                        oppoIntent = Intent()
                        oppoIntent.component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                        oppoIntent.putExtra("packageName", context.packageName)
                        oppoIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        Log.d("WhiteIntent", "尝试通过com.coloros.safecenter.permission.startup.StartupAppListActivity跳转自启动设置")
                        if (WhiteIntentWrapper.doesActivityExists(context, oppoIntent)) {
                            Log.d("WhiteIntent", "可通过com.coloros.safecenter.permission.startup.StartupAppListActivity跳转自启动设置")
                            sIntentWrapperList.add(WhiteIntentWrapper(oppoIntent, OPPO_SM, COMMAND_START_YOURSELF))
                        } else {
                            Log.e("WhiteIntent", "不可通过com.coloros.safecenter.permission.startup.StartupAppListActivity跳转自启动设置")
                            oppoIntent = Intent()
                            oppoIntent.component = ComponentName("com.color.safecenter", "com.color.safecenter.permission.startup.StartupAppListActivity")
                            oppoIntent.putExtra("packageName", context.packageName)
                            oppoIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            Log.d("WhiteIntent", "尝试通过com.color.safecenter.permission.startup.StartupAppListActivity跳转自启动设置")
                            if (WhiteIntentWrapper.doesActivityExists(context, oppoIntent)) {
                                Log.d("WhiteIntent", "可通过com.color.safecenter.permission.startup.StartupAppListActivity跳转自启动设置")
                                sIntentWrapperList.add(WhiteIntentWrapper(oppoIntent, OPPO_SM, COMMAND_START_YOURSELF))
                            } else {
                                Log.e("WhiteIntent", "不可通过com.color.safecenter.permission.startup.StartupAppListActivity跳转自启动设置")
                                oppoIntent = Intent()
                                oppoIntent.component = ComponentName("com.color.safecenter", "com.color.safecenter.permission.PermissionManagerActivity")
                                oppoIntent.putExtra("packageName", context.packageName)
                                oppoIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                Log.d("WhiteIntent", "尝试通过com.color.safecenter.permission.PermissionManagerActivity跳转自启动设置")
                                if (WhiteIntentWrapper.doesActivityExists(context, oppoIntent)) {
                                    Log.d("WhiteIntent", "可通过com.color.safecenter.permission.PermissionManagerActivity跳转自启动设置")
                                    sIntentWrapperList.add(WhiteIntentWrapper(oppoIntent, OPPO_PM, COMMAND_START_YOURSELF))
                                } else {
                                    Log.e("WhiteIntent", "不可通过com.color.safecenter.permission.PermissionManagerActivity跳转自启动设置")
                                    oppoIntent = Intent()
                                    oppoIntent.component = ComponentName.unflattenFromString("com.oppo.safe/.permission.startup.StartupAppListActivity")
                                    oppoIntent.putExtra("packageName", context.packageName)
                                    oppoIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    Log.d("WhiteIntent", "尝试通过com.oppo.safe.permission.startup.StartupAppListActivity跳转自启动设置")
                                    if (WhiteIntentWrapper.doesActivityExists(context, oppoIntent)) {
                                        Log.d("WhiteIntent", "可通过com.oppo.safe.permission.startup.StartupAppListActivity跳转自启动设置")
                                        sIntentWrapperList.add(WhiteIntentWrapper(oppoIntent, OPPO_SM, COMMAND_START_YOURSELF))
                                    } else {
                                        Log.e("WhiteIntent", "不可通过com.oppo.safe.permission.startup.StartupAppListActivity跳转自启动设置")
                                    }
                                }
                            }
                        }
                    }

                }
                COMMAND_BACKSTAGE -> {
                    //OPPO 纯净后台
                    var oppoGodIntent = Intent()
                    oppoGodIntent.component = ComponentName.unflattenFromString("com.oppo.purebackground/.PurebackgroundTopActivity")
                    oppoGodIntent.putExtra("packageName", context.packageName)
                    oppoGodIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    Log.d("WhiteIntent", "尝试通过com.oppo.purebackground.PurebackgroundTopActivity跳转纯净后台设置页")
                    if (WhiteIntentWrapper.doesActivityExists(context, oppoGodIntent)) {
                        Log.d("WhiteIntent", "可通过com.oppo.purebackground.PurebackgroundTopActivity跳转纯净后台设置页")
                        sIntentWrapperList.add(WhiteIntentWrapper(oppoGodIntent, OPPO_GOD, COMMAND_BACKSTAGE))
                    } else {
                        Log.e("WhiteIntent", "不可通过com.oppo.purebackground.PurebackgroundTopActivity跳转纯净后台设置页")
                        oppoGodIntent = Intent()
                        oppoGodIntent.component = ComponentName.unflattenFromString("com.color.purebackground.PurebackgroundTopActivity")
                        oppoGodIntent.putExtra("packageName", context.packageName)
                        oppoGodIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        Log.d("WhiteIntent", "尝试通过com.color.purebackground.PurebackgroundTopActivity跳转纯净后台设置页")
                        if (WhiteIntentWrapper.doesActivityExists(context, oppoGodIntent)) {
                            Log.d("WhiteIntent", "可通过com.color.purebackground.PurebackgroundTopActivity跳转纯净后台设置页")
                            sIntentWrapperList.add(WhiteIntentWrapper(oppoGodIntent, OPPO_GOD, COMMAND_BACKSTAGE))
                        } else {
                            Log.e("WhiteIntent", "不可通过com.coloros.purebackground.PurebackgroundTopActivity跳转纯净后台设置页")
                            oppoGodIntent = Intent()
                            oppoGodIntent.component = ComponentName.unflattenFromString("com.coloros.purebackground.PurebackgroundTopActivity")
                            oppoGodIntent.putExtra("packageName", context.packageName)
                            oppoGodIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            Log.d("WhiteIntent", "尝试通过com.coloros.purebackground.PurebackgroundTopActivity跳转纯净后台设置页")
                            if (WhiteIntentWrapper.doesActivityExists(context, oppoGodIntent)) {
                                Log.d("WhiteIntent", "可通过com.coloros.purebackground.PurebackgroundTopActivity跳转纯净后台设置页")
                                sIntentWrapperList.add(WhiteIntentWrapper(oppoGodIntent, OPPO_GOD, COMMAND_BACKSTAGE))
                            } else {
                                Log.e("WhiteIntent", "不可通过com.coloros.purebackground.PurebackgroundTopActivity跳转纯净后台设置页")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun showDialog(reason: String, a: Activity,  wrapperList: List<WhiteIntentWrapper>) {
        super.showDialog(reason, a,  wrapperList)
        val applicationName = WhiteIntentWrapper.getApplicationName(a)
//        when (intent.type) {
//            OPPO_SM -> {
//                DialogImpl(a, WhiteIntentWrapper.getString(a, "reason_oppo_sm_title", applicationName),
//                        WhiteIntentWrapper.getString(a, "reason_oppo_sm_content", reason, applicationName, applicationName),
//                        WhiteIntentWrapper.getString(a, "ok"),
//                        WhiteIntentWrapper.getString(a, "cancel"), {
//                    intent.startActivitySafely(a)
//                })
//                wrapperList.add(intent)
//            }
//            OPPO_PM -> {
//                DialogImpl(a, WhiteIntentWrapper.getString(a, "reason_oppo_pm_title", applicationName),
//                        WhiteIntentWrapper.getString(a, "reason_oppo_pm_content", reason, applicationName, applicationName),
//                        WhiteIntentWrapper.getString(a, "ok"),
//                        WhiteIntentWrapper.getString(a, "cancel"), {
//                    intent.startActivitySafely(a)
//                })
//                wrapperList.add(intent)
//            }
//            OPPO_GOD -> {
//                DialogImpl(a, WhiteIntentWrapper.getString(a, "reason_oppo_god_title", applicationName),
//                        WhiteIntentWrapper.getString(a, "reason_oppo_god_content", reason, applicationName, applicationName),
//                        WhiteIntentWrapper.getString(a, "ok"),
//                        WhiteIntentWrapper.getString(a, "cancel"), {
//                    intent.startActivitySafely(a)
//                })
//                wrapperList.add(intent)
//            }
//        }
    }
}