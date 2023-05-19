package framework.telegram.ui.filepicker.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object UriOpenUtils {

    fun openUri(context: Context, url: String) {
        try {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = Uri.parse(url)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val availableIntent = findBrowers(context, intent, 0)
            if (availableIntent != null) {
                context.startActivity(availableIntent)
            } else {
                intent.setPackage(null)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(Intent.createChooser(intent, ""))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val browersList by lazy {
        val list = mutableListOf<String>()
        list.add("com.android.chrome")
        list.add("com.UCMobile")
        list.add("com.ucmobile.lite")
        list.add("com.tencent.mtt")
        list.add("com.qihoo.browser")
        list.add("com.qihoo.contents")
        list.add("com.android.browser")
        list.add("sogou.mobile.explorer")
        list.add("com.oupeng.mini.android")
        list.add("com.ijinshan.browser_fast")
        list
    }

    private fun findBrowers(context: Context, intent: Intent, index: Int): Intent? {
        intent.setPackage(browersList[index])
        return if (intent.resolveActivity(context.packageManager) != null) {
            intent
        } else {
            if (index == (browersList.size - 1)) {
                null
            } else {
                findBrowers(context, intent, index + 1)
            }
        }
    }
}