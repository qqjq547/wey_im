package framework.telegram.support.tools

import android.app.Application
import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.core.content.ContextCompat
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseFragment

/**
 * Created by lzh on 19-5-18.
 * INFO:
 */
object ExpandClass {

    fun BaseActivity?.toast(message: CharSequence): Toast {
        return imToast(this, message)
    }

    fun BaseFragment?.toast(message: CharSequence): Toast {
        return imToast(this?.context, message)
    }

    fun Application?.toast(message: CharSequence): Toast {
        return imToast(this, message)
    }

    fun Context?.toast(message: CharSequence): Toast {
        return imToast(this, message)
    }

    private fun imToast(context: Context?, message: CharSequence): Toast {
        return Toast.makeText(context, message, Toast.LENGTH_SHORT)
                .apply {
                    show()
                }
    }

    private fun imLongToast(context: Context?, message: CharSequence): Toast {
        return Toast.makeText(context, message, Toast.LENGTH_LONG)
                .apply {
                    show()
                }
    }


    fun Context.getSimpleDrawable(drawable: Int): Drawable? {
        return ContextCompat.getDrawable(this, drawable)
    }

    fun Context.getSimpleColor(color: Int): Int {
        return ContextCompat.getColor(this, color)
    }
}