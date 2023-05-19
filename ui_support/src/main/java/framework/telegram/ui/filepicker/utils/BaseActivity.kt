package framework.telegram.ui.filepicker.utils

import androidx.appcompat.app.AppCompatActivity
import framework.telegram.support.BaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 *
 * @author rosuh
 * @date 2019/1/7
 */
abstract class BaseFilePickerActivity: BaseActivity(), CoroutineScope by MainScope() {

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}