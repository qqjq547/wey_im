package framework.telegram.business.utils

/**
 * Created by lzh on 19-5-24.
 * INFO:
 */
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import android.text.TextUtils
import android.util.Patterns
import java.util.regex.Pattern

object ValidationUtils {

    fun isChinaMobileNumberValidation(text: CharSequence): Boolean {
        if (isEmpty(text)) {
            return false
        } else {
            val p = Pattern.compile("^(1)\\d{10}$")
            val m = p.matcher(text)
            return m.matches()
        }
    }

    fun isRange(text: CharSequence, min: Int, max: Int): Boolean {
        if (isEmpty(text)) {
            return false
        } else {
            try {
                val value = Integer.parseInt(text.toString())
                if (value > min && value < max) {
                    return true
                }
            } catch (var4: NumberFormatException) {
            }

            return false
        }
    }

    fun isEmail(text: CharSequence): Boolean {
        return if (isEmpty(text)) false else Patterns.EMAIL_ADDRESS.matcher(text).matches()
    }

    fun isNumber(text: CharSequence): Boolean {
        return if (isEmpty(text)) false else text.toString().matches("[0-9]*".toRegex())
    }

    fun isEmptyContent(text: CharSequence): Boolean {
        return if (isEmpty(text)) true else text.toString().matches("(\\s|\n|\t)+".toRegex())
    }

    fun length(text: CharSequence?, minLength: Int, maxLength: Int): Boolean {
        return if (text == null) {
            false
        } else if (minLength > 0 && maxLength > 0) {
            text.length >= minLength && text.length <= maxLength
        } else if (minLength > 0 && maxLength < 0) {
            text.length >= minLength
        } else {
            text.length <= maxLength
        }
    }

    fun isPassword(text: CharSequence): Boolean {
        return length(text, 6, 16)
    }

    fun isEmpty(text: CharSequence): Boolean {
        return TextUtils.isEmpty(text)
    }

    fun isEmptyOrNull(text: CharSequence): Boolean {
        return TextUtils.isEmpty(text) || "null".equals(text.toString(), ignoreCase = true)
    }
}
