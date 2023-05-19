package framework.telegram.ui.link

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.TextPaint
import android.text.style.URLSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import framework.telegram.support.BaseApp

import java.lang.ref.WeakReference

import framework.telegram.ui.R
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.tools.Helper
import framework.telegram.ui.utils.UriUtils

class AutolinkSpan(val activity: AppCompatActivity, private val color: Int, url: String) : URLSpan(url) {


    override fun onClick(widget: View) {
        if (widget.getTag(R.id.long_click) != null) {
            widget.setTag(R.id.long_click, null)
            return
        }

        // 弹出对话框
        activity.let {
            AppDialog.showBottomListView(it, it, url, listOf(BaseApp.app.getString(R.string.open_links), BaseApp.app.getString(R.string.copylink), BaseApp.app.getString(R.string.cancel))) { dialog, index, _ ->
                when (index) {
                    0 -> {
                        val i = Intent(Intent.ACTION_VIEW)
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        i.data = UriUtils.parseUri(if (url.startsWith("http")) url else "http://$url")
                        widget.context.startActivity(i)
                    }
                    1 -> {
                        Helper.setPrimaryClip(it, url)
                        Toast.makeText(it, BaseApp.app.getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        dialog.dismiss()
                    }
                }
            }
        }
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.color = color
        ds.isUnderlineText = true
    }
}