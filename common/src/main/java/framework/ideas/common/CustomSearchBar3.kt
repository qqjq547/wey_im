package framework.ideas.common

import android.content.Context
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import framework.telegram.ui.utils.ScreenUtils
import kotlinx.android.synthetic.main.bus_search_editable.view.*

/**
 * Created by lzh on 19-5-16.
 * INFO: 白色背景
 */
class CustomSearchBar3 @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {

    private var mSerchBarContentCall : ((String)->Unit)? = null
    private var mSerchBarAllSelectCall : (()->Unit)? = null

    //是否需要主题
    private val mContext: Context = context
    private val size by lazy { ScreenUtils.dp2px(this.context, 15f) }

    init {
        LayoutInflater.from(mContext).inflate(R.layout.bus_search_editable, this)
        this.orientation = HORIZONTAL
        this.setBackgroundColor(ContextCompat.getColor(this.context, R.color.white))
        this.setPadding(size, 0, size, 0)
        val layoutParams = LayoutParams(0, ScreenUtils.dp2px(this.context, 30f), 1.0f)
        relative_layout.layoutParams = layoutParams
        text_view_cancel.visibility = View.GONE
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

        search_cancel.setOnClickListener {
            edit_text_search.setText("")
        }


        text_view_cancel.setOnClickListener {
            mSerchBarAllSelectCall?.invoke()
        }
    }

    fun setSearBarListen(listen:((String)->Unit),listen1:(()->Unit)){
        mSerchBarContentCall = listen
        mSerchBarAllSelectCall = listen1
    }

    fun setSearchText(text:String){
        edit_text_search.setText(text)
    }

    fun setAllSelect(string:String){
        text_view_cancel.visibility = View.VISIBLE
        text_view_cancel.text = string
    }
}