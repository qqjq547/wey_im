package framework.telegram.ui.status

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.qmuiteam.qmui.layout.QMUILinearLayout
import framework.telegram.ui.R
import framework.telegram.ui.utils.ScreenUtils
import android.view.Gravity
import android.view.ViewGroup
import framework.telegram.ui.widget.QMUIEmptyViewNew


/**
 * @author ounk
 * 替换现有比较复杂的StatusView
 */
class QMUIViewBuilder {

    //  想要调用的qmui库中哪些view
    //  此版本只接入了QMUIEmptyView
    private var type: TYPE = TYPE.NULL

    //  文字内容
    private var content: String = ""

    //  点击文字
    private var btnText: String = ""

    //  点击事件
    private var onClickListener: View.OnClickListener? = null

    //空页面的图
    private var emptyRid = 0

    //是否用指定的view，给搜索用的
    private var isTop = false

    /**
     * @param type 想要调用的qmui库中哪些view
     */
    constructor(type: TYPE){
        this.type = type
    }

    /**
     * @param type 想要调用的qmui库中哪些view
     * @param content 文字内容
     */
    constructor(type: TYPE, content: String) {
        this.type = type
        this.content = content
    }


    /**
     * @param type 想要调用的qmui库中哪些view
     * @param content 文字内容
     * @param btnText 按钮文字
     * @param onClickListener 按钮点击事件
     */
    constructor(type: TYPE, btnText: String, onClickListener: View.OnClickListener?){
        this.type = type
        this.content = content
        this.btnText = btnText
        this.onClickListener = onClickListener
    }

    fun setType(type: TYPE): QMUIViewBuilder {
        this.type = type
        return this
    }

    fun setContext(content: String): QMUIViewBuilder {
        this.content = content
        return this
    }

    fun setButton(btnText: String, onClickListener: View.OnClickListener): QMUIViewBuilder{
        this.btnText = btnText
        this.onClickListener = onClickListener
        return this
    }

    fun setEmptyImage(rid:Int): QMUIViewBuilder {
        this.emptyRid = rid
        return this
    }

    fun setIsTopView(): QMUIViewBuilder {
        this.isTop = true
        return this
    }

    fun build(fragment: Fragment): View?{
        return build(fragment.context)
    }

    fun build(context: Context?): View? {
        context?.let {
            when (type) {
                TYPE.EMPTY_VIEW -> {
                    val tmp = QMUIEmptyViewNew(context)
                    tmp.visibility = View.VISIBLE
                    if (isTop){
                        tmp.setTopText(content)
                    }else{
                        tmp.setDetailText(content)
                        tmp.setImageView(emptyRid)
                    }
                    return tmp
                }

                TYPE.ERROR_VIEW -> {
                    val tmp = QMUIEmptyViewNew(context)
                    tmp.visibility = View.VISIBLE
                    tmp.setDetailText(content)
                    tmp.setButton(btnText, onClickListener)
                    return tmp
                }

                TYPE.LOADING_VIEW -> {
//                    val tmp = QMUILoadingView(context,16,Color.parseColor("#2c2c2c"))
                    val tmp = QMUIEmptyViewNew(context)
                    tmp.setDetailText(content)
                    tmp.visibility = View.VISIBLE
                    tmp.setLoadingShowing(true)
                    return tmp
                }
                else -> return null
            }
        }
        return null
    }


    //  qmui库的类型
    enum class TYPE {
        NULL,
        EMPTY_VIEW,
        ERROR_VIEW,
        LOADING_VIEW
    }
}

