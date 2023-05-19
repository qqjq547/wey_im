package framework.telegram.support.tools.screenshot

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler

class ScreenShotsContentObserver(uri: Uri, handler: Handler, callback: (selfChange: Boolean, uri: Uri?) -> Unit): ContentObserver(handler){
    private val mCallback = callback

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        mCallback(selfChange, uri)
    }


}