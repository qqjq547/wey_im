package framework.telegram.business.ui.other

import android.os.Bundle
import android.text.TextUtils
import android.widget.TextView
import com.alibaba.android.arouter.facade.annotation.Route
import com.codersun.fingerprintcompat.FingerManager
import com.dds.gestureunlock.GestureUnlock
import com.im.domain.pb.CommonProto
import com.im.domain.pb.SysProto
import com.manusunny.pinlock.PinCodeUnlock
import framework.telegram.business.ArouterServiceManager
import framework.telegram.support.BaseActivity
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_CLEAR_ACCOUNT
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.SysHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.SystemHttpProtocol
import framework.telegram.business.sp.CommonPref
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import kotlinx.android.synthetic.main.bus_clear_account_activity.*
import kotlinx.android.synthetic.main.bus_clear_account_activity.custom_toolbar
import kotlinx.android.synthetic.main.bus_link_activity.*

@Route(path = ROUNTE_BUS_CLEAR_ACCOUNT)
class ClearAccountActivity : BaseActivity() {

    private val userId by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bus_clear_account_activity)

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        when {
            PinCodeUnlock.getInstance().isPinCodeSet(this, userId.toString()) -> {
                custom_toolbar.showCenterTitle(getString(R.string.forget_applock_pincode))
                findViewById<TextView>(R.id.tip_title).text = getString(R.string.forget_applock_pincode)
            }
            GestureUnlock.getInstance().isGestureCodeSet(this, userId.toString()) -> {
                custom_toolbar.showCenterTitle(getString(R.string.forget_applock_password))
                findViewById<TextView>(R.id.tip_title).text = getString(R.string.forget_applock_password)
            }
            else -> finish()
        }

        text_view_clear_account.setOnClickListener {

            AppDialog.show(this@ClearAccountActivity, this@ClearAccountActivity) {
                message(text = getString(R.string.app_lock_clear_account_tip))
                negativeButton(text = context.getString(R.string.confirm_two), click = {
                    ArouterServiceManager.systemService.clearAccount(this@ClearAccountActivity)
                })
                positiveButton(text = context.getString(R.string.cancel))
            }
        }
    }
}