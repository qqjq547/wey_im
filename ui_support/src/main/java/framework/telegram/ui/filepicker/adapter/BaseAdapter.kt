package framework.telegram.ui.filepicker.adapter

import androidx.recyclerview.widget.RecyclerView
import framework.telegram.ui.filepicker.bean.FileBean

abstract class BaseAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    abstract fun getItem(position: Int): FileBean?
}