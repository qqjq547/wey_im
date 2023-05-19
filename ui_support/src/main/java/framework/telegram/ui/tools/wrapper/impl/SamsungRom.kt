package framework.telegram.ui.tools.wrapper.impl

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import framework.telegram.ui.R
import framework.telegram.ui.tools.wrapper.Constant.COMMAND_BATTERY_MANAGER
import framework.telegram.ui.tools.wrapper.Constant.COMMAND_START_YOURSELF
import framework.telegram.ui.tools.wrapper.DialogImpl
import framework.telegram.ui.tools.wrapper.DialogListImpl
import framework.telegram.ui.tools.wrapper.WhiteIntentWrapper

/**
 * Created by lzh
 * time: 2018/4/17.
 * info:
 */
class SamsungRom : SystemRom() {

    override val tag = "SamsungRom"

    //三星
    private val SAMSUNG = 0x30

    private val SAMSUNG_BATTERY = 0x40

    override fun getIntent(context: Context, sIntentWrapperList: MutableList<WhiteIntentWrapper>, commandList: List<String>) {
        super.getIntent(context, sIntentWrapperList, commandList)
        (0 until commandList.size).forEach {
            when (commandList[it]) {
                COMMAND_START_YOURSELF -> {
                    //三星自启动应用程序管理
                    Log.d("WhiteIntent", "三星手机")
                    var samsungIntent: Intent? = Intent()
                    samsungIntent = Intent()
                    samsungIntent.component = ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.cstyleboard.SmartManagerDashBoardActivity")
                    samsungIntent.putExtra("packageName", context.packageName)
                    samsungIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    if (WhiteIntentWrapper.doesActivityExists(context, samsungIntent)) {
                        sIntentWrapperList.add(WhiteIntentWrapper(samsungIntent, SAMSUNG, COMMAND_START_YOURSELF))
                    }
                }
                COMMAND_BATTERY_MANAGER -> {
                    Log.d("WhiteIntent", "三星手机")
                    var samsungIntent: Intent? = Intent()
                    samsungIntent = Intent()
                    samsungIntent.component = ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.cstyleboard.SmartManagerDashBoardActivity")
                    samsungIntent.putExtra("packageName", context.packageName)
                    samsungIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    if (WhiteIntentWrapper.doesActivityExists(context, samsungIntent)) {
                        sIntentWrapperList.add(WhiteIntentWrapper(samsungIntent, SAMSUNG_BATTERY, COMMAND_START_YOURSELF))
                    }
                }
            }
        }
    }

    override fun showDialog(reason: String, a: Activity, wrapperList: List<WhiteIntentWrapper>) {
        super.showDialog(reason, a, wrapperList)
        val listName = mutableListOf<String>()
        wrapperList.forEach {
            when (it.type) {
                SAMSUNG -> {
                    listName.add(a.getString(R.string.add_since_launching_the_whitelist))
                }
                SAMSUNG_BATTERY -> {
                    listName.add(a.getString(R.string.unmonitored_applications))
                }
            }
        }

        DialogListImpl(a, a.getString(R.string.it_can_improve_the_accuracy_of_message_arrival), listName) { _, index, _ ->
            try {
                a.startActivity(wrapperList[index].intent)
            } catch (e: Exception) {
                Toast.makeText(a, a.getString(R.string.start_up_is_not_supported), Toast.LENGTH_SHORT).show()
            }
        }
    }
}