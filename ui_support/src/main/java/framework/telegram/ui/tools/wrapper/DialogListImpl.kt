package framework.telegram.ui.tools.wrapper

import android.app.Activity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.ItemListener
import com.afollestad.materialdialogs.list.listItems
import framework.telegram.ui.dialog.AppDialog

/**
 * Created by lzh
 * time: 2018/5/17.
 * info:
 */
class DialogListImpl(activity: Activity, title: String,itemList:MutableList<String>, selection: ItemListener) {
    init {
        try {
            AppDialog(MaterialDialog(activity).listItems(null, itemList, null, true, selection).show {
                noAutoDismiss()
                title(text=title)
            })
        } catch ( e:Exception) {
            e.printStackTrace()
        }
    }
}