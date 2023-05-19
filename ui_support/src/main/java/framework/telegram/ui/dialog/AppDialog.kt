package framework.telegram.ui.dialog

import android.app.Activity
import android.content.Context
import android.media.Image
import android.text.TextUtils
import android.view.animation.AnimationUtils
import android.view.animation.RotateAnimation
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BasicGridItem
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.GridItemListener
import com.afollestad.materialdialogs.bottomsheets.gridItems
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.ItemListener
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.listItems
import com.dds.gestureunlock.util.ResourceUtil
import framework.telegram.ui.R

class AppDialog(private val dialog: MaterialDialog) {

    companion object {

        private const val mCornerRadius = 8f

        fun show(activity: Activity, func: MaterialDialog.() -> Unit): AppDialog {
            return AppDialog(MaterialDialog(activity).show {
                this.func()
                this.cornerRadius(mCornerRadius)
            })
        }

        fun show(activity: AppCompatActivity, lifecycleOwner: LifecycleOwner, func: MaterialDialog.() -> Unit): AppDialog {
            return AppDialog(MaterialDialog(activity).show {
                this.func()
                this.cornerRadius(mCornerRadius)
                this.lifecycleOwner(lifecycleOwner)
            })
        }

        fun show(context: Context, lifecycleOwner: LifecycleOwner, func: MaterialDialog.() -> Unit): AppDialog {
            return AppDialog(MaterialDialog(context).show {
                this.func()
                this.cornerRadius(mCornerRadius)
                this.lifecycleOwner(lifecycleOwner)
            })
        }

        fun show(context: Context, func: MaterialDialog.() -> Unit): AppDialog {
            return AppDialog(MaterialDialog(context).show {
                this.func()
                this.cornerRadius(mCornerRadius)
            })
        }

        fun showList(activity: AppCompatActivity, lifecycleOwner: LifecycleOwner, list: List<String>, selection: ItemListener): AppDialog {
            return AppDialog(MaterialDialog(activity).listItems(null, list, null, true, selection).show {
                this.lifecycleOwner(lifecycleOwner)
            })
        }

        fun showList(context: Context, lifecycleOwner: LifecycleOwner, list: List<String>, selection: ItemListener): AppDialog {
            return AppDialog(MaterialDialog(context).listItems(null, list, null, true, selection).show {
                this.lifecycleOwner(lifecycleOwner)
            })
        }

        fun showList(context: Context, list: List<String>, selection: ItemListener): AppDialog {
            return AppDialog(MaterialDialog(context).listItems(null, list, null, true, selection).show {
            })
        }

        fun showList(context: Context, title: String, list: List<String>, selection: ItemListener): AppDialog {
            return AppDialog(MaterialDialog(context).listItems(null, list, null, true, selection).show {
                noAutoDismiss()
                title(text = title)
            })
        }

        fun showList(activity: AppCompatActivity, lifecycleOwner: LifecycleOwner, adapter: RecyclerView.Adapter<*>, selection: ItemListener): AppDialog {
            return AppDialog(MaterialDialog(activity).show {
                this.customListAdapter(adapter)
                this.lifecycleOwner(lifecycleOwner)
            })
        }

        fun showList(context: Context, lifecycleOwner: LifecycleOwner, adapter: RecyclerView.Adapter<*>, selection: ItemListener): AppDialog {
            return AppDialog(MaterialDialog(context).show {
                this.customListAdapter(adapter)
                this.lifecycleOwner(lifecycleOwner)
            })
        }

        fun showCustomView(context: Context, layoutId: Int, lifecycleOwner: LifecycleOwner?, func: MaterialDialog.() -> Unit): AppDialog {
            return AppDialog(MaterialDialog(context).customView(layoutId).show {
                this.func()
                if (lifecycleOwner != null) {
                    this.lifecycleOwner(lifecycleOwner)
                }
            })
        }

        fun showBottomListView(activity: AppCompatActivity, lifecycleOwner: LifecycleOwner, list: List<String>, selection: ItemListener): AppDialog {
            return AppDialog(MaterialDialog(activity, BottomSheet(LayoutMode.WRAP_CONTENT)).listItems(items = list, selection = selection).show {
                this.lifecycleOwner(lifecycleOwner)
            })
        }

        fun showBottomListView(activity: AppCompatActivity, lifecycleOwner: LifecycleOwner, title: String, list: List<String>, selection: ItemListener): AppDialog {
            return AppDialog(MaterialDialog(activity, BottomSheet(LayoutMode.WRAP_CONTENT)).listItems(items = list, selection = selection).show {
                if (!TextUtils.isEmpty(title))
                    title(text = title)
                this.lifecycleOwner(lifecycleOwner)
            })
        }

        fun showBottomListView(context: Context,  title: String, list: List<String>, selection: ItemListener): AppDialog {
            return AppDialog(MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT)).listItems(items = list, selection = selection).show {
                if (!TextUtils.isEmpty(title))
                    title(text = title)
            })
        }

        fun showBottomListWithIconView(activity: AppCompatActivity, lifecycleOwner: LifecycleOwner, list: List<BasicGridItem>, selection: GridItemListener<BasicGridItem>): AppDialog {
            val bottomSheet = BottomSheet(LayoutMode.WRAP_CONTENT)
            return AppDialog(MaterialDialog(activity, bottomSheet).gridItems(list, selection = selection).show {
                this.lifecycleOwner(lifecycleOwner)
            })
        }

        fun showLoadingView(activity: AppCompatActivity, lifecycleOwner: LifecycleOwner): AppDialog {
            return AppDialog(MaterialDialog(activity).customView(R.layout.common_dialog_loading).show {
                this.lifecycleOwner(lifecycleOwner)
                val imageView = this.getCustomView().findViewById<ImageView>(R.id.loading_view)
                val animation = AnimationUtils.loadAnimation(imageView.context, R.anim.anim_rotate)
                imageView.startAnimation(animation)
                cancelable(false)
            })
        }

        fun showProgressView(context: Context, lifecycleOwner: LifecycleOwner, func: MaterialDialog.() -> Unit): AppDialog {
            return AppDialog(MaterialDialog(context).customView(R.layout.common_dialog_progress).show {
                this.func()
                this.lifecycleOwner(lifecycleOwner)
                cancelable(false)
            })
        }
    }

    fun dismiss() {
        dialog.dismiss()
    }
}
