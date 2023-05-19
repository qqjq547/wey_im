package framework.telegram.ui.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.view.ViewConfiguration
import android.os.Build
import android.annotation.TargetApi
import android.os.Handler
import android.util.Log
import android.widget.EditText
import java.lang.reflect.AccessibleObject.setAccessible





class KeyboardktUtils {

    companion object {

        fun showKeyboard(view: View) {
            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            view.requestFocus()
            imm.showSoftInput(view, 0)
        }

        fun showKeyboardDelay(view: View){
            if (view is EditText){
                view.setSelection(view.text.length)
                view.setFocusable(true)
                view.setFocusableInTouchMode(true)
                view.requestFocus()
                Handler().postDelayed({
                    this.toggleSoftInput(view)
                },100)
            }
        }

        fun hideKeyboard(view: View) {
            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        fun toggleSoftInput(view: View) {
            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(0, 0)
        }

    }


}