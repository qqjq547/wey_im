package framework.telegram.business.ui.me

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.sp.CommonPref
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.PrivacyContract
import framework.telegram.business.ui.me.presenter.PrivacyPresenterImpl
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.ExpandClass.toast
import kotlinx.android.synthetic.main.bus_me_activity_clear_account.*

/**
 * Created by lzh on 19-11-7.
 * INFO:清楚账号
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_CLEAR_ACCOUNT)
class ClearAccountActivity : BaseBusinessActivity<BasePresenter>(){

    override fun getLayoutId() = R.layout.bus_me_activity_clear_account

    private val mRadioList by lazy { mutableListOf(radio_button3,radio_button1,radio_button2,radio_button4) }

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.string_clear_account))

        val clearTime = AccountManager.getLoginAccount(AccountInfo::class.java).getClearTime()
        mRadioList[clearTime].isChecked = true
    }

    override fun initListen() {
        layout1.setOnClickListener {
            finishActivityForResult(1)
        }

        layout2.setOnClickListener {
            finishActivityForResult(2)
        }

        layout3.setOnClickListener {
            finishActivityForResult(0)
        }

        layout4.setOnClickListener {
            finishActivityForResult(3)
        }

        radio_button1.setOnClickListener { finishActivityForResult(1) }
        radio_button2.setOnClickListener { finishActivityForResult(2) }
        radio_button3.setOnClickListener { finishActivityForResult(0) }
        radio_button4.setOnClickListener { finishActivityForResult(3) }
    }

    private fun finishActivityForResult(target:Int){
        for (index in 0 until mRadioList.size){
            mRadioList[index].isChecked = index == target
        }
        val intent = Intent()
        intent.putExtra("target", target)
        setResult(Activity.RESULT_OK,intent)
        finish()
    }

    override fun initData() {
    }
}