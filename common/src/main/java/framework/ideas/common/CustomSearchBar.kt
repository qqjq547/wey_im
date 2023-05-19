package framework.ideas.common

import android.content.Context
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import framework.telegram.ui.utils.ScreenUtils
import framework.ideas.common.R
import kotlinx.android.synthetic.main.bus_search_editable.view.*

/**
 * Created by lzh on 19-5-16.
 * INFO:
 */
class CustomSearchBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {

    private var mSerchBarContentCall : ((String)->Unit)? = null
    private var mSerchBarCancelCall : (()->Unit)? = null

    //是否需要主题
    private val mContext: Context = context
    private val size by lazy { ScreenUtils.dp2px(this.context, 13f) }

    init {
        LayoutInflater.from(mContext).inflate(R.layout.bus_search_editable, this)
        this.orientation = HORIZONTAL
        this.setBackgroundColor(ContextCompat.getColor(this.context, R.color.f8fafd))
        this.setPadding(size, 0, size, 0)
        val layoutParams = LayoutParams(0, ScreenUtils.dp2px(this.context, 40f), 1.0f)
        relative_layout.layoutParams = layoutParams

        initListen()
    }

    private fun initListen() {
        edit_text_search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val str = s.toString()
                if (!TextUtils.isEmpty(str)) {
                    search_cancel.visibility = View.VISIBLE
                } else {
                    search_cancel.visibility = View.GONE
                }
                mSerchBarContentCall?.invoke(str)
            }
        })

        text_view_cancel.setOnClickListener {
            mSerchBarCancelCall?.invoke()
        }

        search_cancel.setOnClickListener {
            edit_text_search.setText("")
        }
    }

    fun setSearBarListen(listen1:(()->Unit),listen:((String)->Unit)){
        mSerchBarContentCall = listen
        mSerchBarCancelCall = listen1
    }

    fun getEditView(): EditText = edit_text_search

}