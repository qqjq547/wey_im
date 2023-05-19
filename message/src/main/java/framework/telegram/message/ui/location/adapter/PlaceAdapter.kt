package framework.telegram.message.ui.location.adapter

import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseViewHolder
import framework.telegram.business.bridge.search.StringUtil
import framework.telegram.message.R
import framework.telegram.message.ui.location.bean.POIBean
import framework.telegram.ui.doubleclick.recycler.AppBaseQuickAdapter

/**
 * Created by lzh on 19-8-28.
 * INFO:
 */
class PlaceAdapter( var isShowCheckBox:Boolean  ) : AppBaseQuickAdapter<POIBean, BaseViewHolder>(R.layout.msg_location_choice_item) {

    var mKeyword =""

    fun setKeyword(keyword:String){
        mKeyword = keyword
    }

    override fun convert(helper: BaseViewHolder, item: POIBean) {
        helper.getView<TextView>(R.id.text_view_1)?.text =   StringUtil.setHitTextColor(mKeyword, item.name)
       val textView2 =  helper.getView<TextView>(R.id.text_view_2)
        if (!TextUtils.isEmpty(item.address)){
            textView2.visibility = View.VISIBLE
            textView2.text =  StringUtil.setHitTextColor(mKeyword, item.address)
        }else{
            textView2.visibility = View.GONE
        }

        val checkBox = helper.getView<ImageView>(R.id.check_box_selected)
        if (isShowCheckBox){
            checkBox.visibility = if(item.isCheck)  View.VISIBLE else View.GONE
        }else{
            checkBox.visibility = View.GONE
        }

        helper.addOnClickListener(R.id.all_layout)
    }
}