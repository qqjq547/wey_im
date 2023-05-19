package framework.telegram.ui.tools.wrapper.impl

import android.app.Activity
import android.content.Context
import framework.telegram.ui.tools.wrapper.WhiteIntentWrapper


/**
 * Created by lzh
 * time: 2018/4/17.
 * info:
 */
class HTCRom : SystemRom() {

    override val tag = "HTCRom"


    override fun getIntent(context: Context, sIntentWrapperList: MutableList<WhiteIntentWrapper>, commandList: List<String>) {
        super.getIntent(context, sIntentWrapperList, commandList)
    }

    override fun showDialog(reason: String, a: Activity, wrapperList: List<WhiteIntentWrapper>) {
        super.showDialog(reason, a,  wrapperList)
        val applicationName = WhiteIntentWrapper.getApplicationName(a)
//        when (intent.type) {
//            DOZE -> {
//
//            }
//        }
    }
}