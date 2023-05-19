package framework.telegram.ui.tools.wrapper.impl

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import framework.telegram.ui.tools.wrapper.Constant.COMMAND_CONSUME_POWER
import framework.telegram.ui.tools.wrapper.Constant.COMMAND_START_YOURSELF
import framework.telegram.ui.tools.wrapper.DialogImpl
import framework.telegram.ui.tools.wrapper.WhiteIntentWrapper


/**
 * Created by lzh
 * time: 2018/4/17.
 * info:
 */
class FlymeRom : SystemRom() {

    override val tag = "FlymeRom"

    //魅族 自启动管理
    private val MEIZU = 0x40
    //魅族 待机耗电管理
    private val MEIZU_GOD = 0x41

    @RequiresApi(Build.VERSION_CODES.M)
    override fun getIntent(context: Context, sIntentWrapperList: MutableList<WhiteIntentWrapper>, commandList: List<String>) {
        super.getIntent(context, sIntentWrapperList, commandList)
        (0 until commandList.size).forEach {
            when (commandList[it]) {
                COMMAND_START_YOURSELF -> {
                    //魅族 自启动管理
                    Log.d("WhiteIntent", "魅族手机")
                    var meizuIntent = Intent("com.meizu.safe.security.SHOW_APPSEC")
                    meizuIntent.addCategory(Intent.CATEGORY_DEFAULT)
                    meizuIntent.putExtra("packageName", context.packageName)
                    meizuIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    Log.d("WhiteIntent", "尝试通过Action=com.meizu.safe.security.SHOW_APPSEC跳转自启动设置")
                    if (WhiteIntentWrapper.doesActivityExists(context, meizuIntent)) {
                        Log.d("WhiteIntent", "可通过Action=com.meizu.safe.security.SHOW_APPSEC跳转自启动设置")
                        sIntentWrapperList.add(WhiteIntentWrapper(meizuIntent, MEIZU, COMMAND_START_YOURSELF))
                    } else {
                        Log.e("WhiteIntent", "不可通过Action=com.meizu.safe.security.SHOW_APPSEC跳转自启动设置")
                        meizuIntent = Intent()
                        meizuIntent.component = ComponentName.unflattenFromString("com.meizu.safe/.permission.PermissionMainActivity")
                        meizuIntent.putExtra("packageName", context.packageName)
                        meizuIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        Log.d("WhiteIntent", "尝试通过com.meizu.safe.permission.PermissionMainActivity跳转自启动设置")
                        if (WhiteIntentWrapper.doesActivityExists(context, meizuIntent)) {
                            Log.d("WhiteIntent", "可通过com.meizu.safe.permission.PermissionMainActivity跳转自启动设置")
                            sIntentWrapperList.add(WhiteIntentWrapper(meizuIntent, MEIZU, COMMAND_START_YOURSELF))
                        } else {
                            Log.e("WhiteIntent", "不可通过com.meizu.safe.permission.PermissionMainActivity跳转自启动设置")
                        }
                    }
                }
                COMMAND_CONSUME_POWER -> {
                    //魅族 待机耗电管理
                    val meizuGodIntent = Intent()
                    meizuGodIntent.component = ComponentName("com.meizu.safe", "com.meizu.safe.powerui.PowerAppPermissionActivity")
                    meizuGodIntent.putExtra("packageName", context.packageName)
                    meizuGodIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    Log.d("WhiteIntent", "尝试通过com.meizu.safe.powerui.PowerAppPermissionActivity跳转待机耗电管理页")
                    if (WhiteIntentWrapper.doesActivityExists(context, meizuGodIntent)) {
                        Log.d("WhiteIntent", "可通过com.meizu.safe.powerui.PowerAppPermissionActivity跳转待机耗电管理页")
                        sIntentWrapperList.add(WhiteIntentWrapper(meizuGodIntent, MEIZU_GOD, COMMAND_CONSUME_POWER))
                    } else {
                        Log.e("WhiteIntent", "不可通过com.meizu.safe.powerui.PowerAppPermissionActivity跳转待机耗电管理页")
                    }
                }
            }
        }
    }

    override fun showDialog(reason: String, a: Activity,  wrapperList: List<WhiteIntentWrapper>) {
        super.showDialog(reason, a,  wrapperList)
        val applicationName = WhiteIntentWrapper.getApplicationName(a)
//        when (intent.type) {
//            MEIZU_GOD -> {
//                try {
//                    AlertDialog.Builder(a)
//                            .setCancelable(false)
//                            .setTitle(WhiteIntentWrapper.getString(a, "reason_mz_god_title", WhiteIntentWrapper.getApplicationName(a)))
//                            .setMessage(WhiteIntentWrapper.getString(a, "reason_mz_god_content", reason, WhiteIntentWrapper.getApplicationName(a), WhiteIntentWrapper.getApplicationName(a)))
//                            .setPositiveButton(WhiteIntentWrapper.getString(a, "ok"), DialogInterface.OnClickListener { d, w -> intent.startActivitySafely(a) })
//                            .setNegativeButton(WhiteIntentWrapper.getString(a, "cancel"), null)
//                            .show()
//                    wrapperList.add(intent)
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//                DialogImpl(a, WhiteIntentWrapper.getString(a, "reason_mz_god_title", applicationName),
//                        WhiteIntentWrapper.getString(a, "reason_mz_god_content", reason, applicationName, applicationName),
//                        WhiteIntentWrapper.getString(a, "ok"),
//                        WhiteIntentWrapper.getString(a, "cancel"), {
//                    intent.startActivitySafely(a)
//                })
//                wrapperList.add(intent)
//            }
//            MEIZU -> {
//                DialogImpl(a, WhiteIntentWrapper.getString(a, "reason_mz_title", applicationName),
//                        WhiteIntentWrapper.getString(a, "reason_mz_content", reason, applicationName),
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