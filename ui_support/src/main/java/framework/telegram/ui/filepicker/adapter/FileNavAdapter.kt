package framework.telegram.ui.filepicker.adapter

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import framework.telegram.ui.filepicker.adapter.BaseAdapter
import framework.telegram.ui.filepicker.FilePickerActivity
import  framework.telegram.ui.R
import framework.telegram.ui.filepicker.bean.FileNavBeanImpl

/**
 *
 * @author rosu
 * @date 2018/11/21
 */
class FileNavAdapter(private val activity: FilePickerActivity, var data: MutableList<FileNavBeanImpl>) :
    BaseAdapter(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return NavListHolder(activity.layoutInflater, parent)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, postion: Int) {
        (holder as NavListHolder).bind(data[postion], postion)
    }

    override fun getItem(position: Int): FileNavBeanImpl?{
        return if (position >= 0 && position < data.size) {
            data[position]
        } else {
            null
        }
    }

    inner class NavListHolder(inflater: LayoutInflater, val parent: ViewGroup):
        RecyclerView.ViewHolder(inflater.inflate(R.layout.item_nav_file_picker, parent, false)){

        private var mBtnDir: TextView? = null

        private var pos:Int? = null

        fun bind(item: FileNavBeanImpl?, position:Int) {
            pos = position
            mBtnDir = itemView.findViewById(R.id.tv_btn_nav_file_picker)
            mBtnDir?.text = item!!.dirName
        }
    }
}