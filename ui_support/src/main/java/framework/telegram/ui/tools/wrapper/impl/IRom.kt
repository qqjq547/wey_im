package framework.telegram.ui.tools.wrapper.impl

import android.app.Activity
import android.content.Context
import framework.telegram.ui.tools.wrapper.WhiteIntentWrapper

/**
 * Created by lzh
 * time: 2018/4/17.
 * info:
 */
interface IRom {
    fun getIntent(context: Context, sIntentWrapperList: MutableList<WhiteIntentWrapper>, commandList: List<String>)

    fun showDialog(reason: String, a: Activity, wrapperList: List<WhiteIntentWrapper>)
}