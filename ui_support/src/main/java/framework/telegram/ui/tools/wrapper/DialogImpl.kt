package framework.telegram.ui.tools.wrapper

import android.app.Activity
import framework.telegram.ui.dialog.AppDialog

/**
 * Created by lzh
 * time: 2018/5/17.
 * info:
 */
class DialogImpl(activity: Activity, title: String, content: String, positive: String, negative: String,
                 positiveCallback: (() -> Unit)?, negativeCallback: (() -> Unit)? = null) {
    init {
        try {
            AppDialog.show(activity){
                cancelable(false)
                title(text = title)
                message(text = content)
                positiveButton(text= positive) {
                    positiveCallback?.invoke()
                }
                negativeButton(text = negative) {
                    negativeCallback?.invoke()
                }
            }
        } catch ( e:Exception) {
            e.printStackTrace()
        }
    }
}