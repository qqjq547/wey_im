package framework.telegram.business.bridge.search

/**
 * Created by lzh on 19-6-6.
 * INFO:
 */

import android.text.Html
import android.text.Spanned
import androidx.core.text.HtmlCompat
import android.text.style.ForegroundColorSpan
import android.text.SpannableString
import androidx.core.content.ContextCompat
import framework.telegram.business.bridge.R
import framework.telegram.support.BaseApp
import java.util.regex.Pattern


object StringUtil {

    fun setHitTextColor(keyword: String, str: String): Spanned {
        val s = SpannableString(str)
        var wordReg =""
        keyword.forEach {
            wordReg += "$it(\\s*)"
        }
        wordReg = "(?i)($wordReg)"///< 用(?i)来忽略大小写
        val p = Pattern.compile(wordReg)
        val m = p.matcher(s)
        while (m.find()) {
            val start = m.start()
            val end = m.end()
            s.setSpan(ForegroundColorSpan(ContextCompat.getColor(BaseApp.app, R.color.c178aff)), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return s
    }


    fun setRedHitTextColor(key: String, str: String): Spanned {
        val color = "<font color=\"#fe0005\">$key</font>"
        val stringBuild = StringBuilder(str) //
        val result = stringBuild.replace(Regex(key)) {
            color
        }
        return HtmlCompat.fromHtml(result, Html.FROM_HTML_MODE_LEGACY)
    }

}

