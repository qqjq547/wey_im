package framework.telegram.message.ui.face

/**
 * Created by hyf on 20-1-4.
 * INFO:
 */
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseViewHolder
import com.facebook.common.util.UriUtil
import framework.telegram.message.R
import framework.telegram.ui.doubleclick.recycler.AppBaseQuickAdapter
import framework.telegram.ui.face.dynamic.DynamicFaceBean
import framework.telegram.ui.image.AppImageView

class DynamicFaceManagerAdapter(private val mSizeCallback: SizeCallback?, private val mItemClick: ((face: DynamicFaceBean) -> Unit)) : AppBaseQuickAdapter<DynamicFaceBean, BaseViewHolder>(R.layout.msg_dynamic_face_item) {

    private val mCheckedMessageList by lazy { mutableListOf<DynamicFaceBean>() }

    private var isClickable = false

    private var mCheckedMessageListener: ((Int) -> Unit)? = null

    override fun convert(helper: BaseViewHolder, item: DynamicFaceBean?) {

        if (mSizeCallback != null) {
            helper.itemView.layoutParams = RecyclerView.LayoutParams(mSizeCallback.getWidth(), mSizeCallback.getHeight())
        }

        helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item?.path)

        helper.getView<RelativeLayout>(R.id.reletive_layout_all)?.setOnClickListener {
            if (item != null && !isClickable) {
                mItemClick.invoke(item)
            }
        }

        val checkbox = helper.getView<CheckBox>(R.id.check_box_msg)
        if (isClickable) {
            checkbox.visibility = View.VISIBLE
            checkbox.tag = item
            checkbox.setOnCheckedChangeListener(null)
            var finded = false
            mCheckedMessageList.forEach {
                if (it.id == item?.id) {
                    finded = true
                }
            }
            checkbox.isChecked = finded
            checkbox.setOnCheckedChangeListener(mOnCheckedChangeListener)
        } else {
            checkbox?.visibility = View.GONE
        }
    }

    fun setCheckable(checkedMessageListener: ((Int) -> Unit)? = null) {
        remove(0)

        isClickable = true
        mCheckedMessageList.clear()
        mCheckedMessageListener = checkedMessageListener
        mCheckedMessageListener?.invoke(mCheckedMessageList.size)
        notifyDataSetChanged()
    }

    fun setUnCheckable() {
        addData(0, DynamicFaceBean(-1, UriUtil.getUriForResourceId(R.drawable.ic_add_dynamic_face).toString(), 0, 0))

        isClickable = false
        mCheckedMessageList.clear()
        mCheckedMessageListener?.invoke(mCheckedMessageList.size)
        mCheckedMessageListener = null
        notifyDataSetChanged()
    }

    fun getCheckableMessages(): ArrayList<DynamicFaceBean> {
        return ArrayList(mCheckedMessageList)
    }

    private val mOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        val msgModel = buttonView.tag as DynamicFaceBean
        if (!isChecked) {
            mCheckedMessageList.remove(msgModel)
        } else {
            mCheckedMessageList.add(msgModel)
        }
        mCheckedMessageListener?.invoke(mCheckedMessageList.size)
    }

    interface SizeCallback {
        fun getWidth(): Int

        fun getHeight(): Int
    }
}
