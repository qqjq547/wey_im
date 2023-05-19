package framework.telegram.business.ui.other

import android.app.Activity
import android.content.Intent
import com.dds.gestureunlock.GestureUnlockActivity
import com.manusunny.pinlock.SetPinActivity
import framework.telegram.business.R
import framework.telegram.business.sp.CommonPref
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.ui.dialog.AppDialog

class AppSetPinUnlockActivity : SetPinActivity() {

    companion object {

        fun gotoCreatePinCodeForResult(activity: Activity, key: String, showOtherLockType: Boolean = true) {
            val intent = Intent(activity, AppSetPinUnlockActivity::class.java)
            intent.putExtra(KEY, key)
            intent.putExtra(SHOW_OTHER_LOCK_TYPE,showOtherLockType )
            activity.startActivityForResult(intent, CREATE_REQUEST_CODE)
        }
    }

    override fun onChangeLockType() {
        val list = mutableListOf<String>()
        list.add(getString(R.string.applock_gesture_type))

        AppDialog.showBottomListView(this@AppSetPinUnlockActivity, this@AppSetPinUnlockActivity, list) { _, index, text ->
            when (text) {
                getString(R.string.applock_gesture_type) -> {
                    AppGestureUnlockActivity.gotoCreateGestureCodeForResult(this@AppSetPinUnlockActivity, key)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GestureUnlockActivity.CREATE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }
}