package framework.telegram.business.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.CompoundButton
import android.widget.FrameLayout
import framework.telegram.business.R
import kotlinx.android.synthetic.main.bus_me_item_switch_button.view.*

/**
 * Created by lzh on 19-5-16.
 * INFO:
 */
class SwitchButtonView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private val mContext: Context = context
    private var listen: ((isChecked: Boolean) -> Unit)? = null

    init {
        LayoutInflater.from(mContext).inflate(R.layout.bus_me_item_switch_button, this)
    }

    fun setData(name: String, defaultChecked: Boolean? = false, listen: ((isChecked: Boolean) -> Unit)) {
        this.listen = listen
        text_view_name.text = name
        setData(defaultChecked ?: false)
    }

    fun setData(checked: Boolean) {
        switch_button.setOnCheckedChangeListener(null)
        switch_button.isChecked = checked
        switch_button.setOnCheckedChangeListener(onCheckedChangeList)
    }

    fun setEnable(isEnabled:Boolean){
        switch_button.isEnabled = isEnabled
    }

    fun isChecked(): Boolean {
        return switch_button.isChecked
    }

    private val onCheckedChangeList = CompoundButton.OnCheckedChangeListener { _, isChecked -> listen?.invoke(isChecked) }
}