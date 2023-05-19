package framework.telegram.ui.filepicker.config

import androidx.recyclerview.widget.RecyclerView
import android.view.View

/**
 *
 * @author rosu
 * @date 2018/11/26
 */
interface FileItemOnClickListener {

    fun onItemClick(itemAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
                    itemView: View,
                    position: Int)

    fun onItemChildClick(itemAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
                         itemView: View,
                         position: Int)

    fun onItemLongClick(itemAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
                        itemView: View,
                        position: Int)
}