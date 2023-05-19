package framework.telegram.business.ui.contacts

import android.app.Activity
import android.content.Intent
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.support.mvp.BasePresenter
import kotlinx.android.synthetic.main.bus_contacts_activity_complaint.*

/**
 * Created by lzh on 19-7-5.
 * INFO:
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_CONTACTS_COMPLAINT)
class ComplaintActivity: BaseBusinessActivity<BasePresenter>() {

    private val targetUId by lazy { intent.getLongExtra("targetUId", 0)}
    private val mType by lazy { intent.getIntExtra("type", 0)}// 0 举报人  1 举报群

    private val EDIT_REQUEST_CODE = 111

    override fun getLayoutId() = R.layout.bus_contacts_activity_complaint

    override fun initView() {
        custom_toolbar.showCenterTitle(getString(R.string.complaints_reasons))
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        if (mType == 0){
            text1.text = getString(R.string.bad_information)
            text2.text = getString(R.string.attack_action)
            text3.text = getString(R.string.illegal_behavior)
        }else{
            text1.text = getString(R.string.group_report_1)
            text2.text = getString(R.string.group_report_2)
            text3.text = getString(R.string.group_report_3)
            text4.text = getString(R.string.group_report_4)
            text5.text = getString(R.string.illegal_behavior)

            linear_layout_4.visibility = View.VISIBLE
            linear_layout_5.visibility = View.VISIBLE
            line4.visibility = View.VISIBLE
            line5.visibility = View.VISIBLE
        }
    }

    override fun initListen() {
        linear_layout_1.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACTS_COMPLAINT_EDIT)
                    .withInt("reportType", 1)
                    .withInt("type", mType)
                    .withLong("targetUId", targetUId).navigation(this@ComplaintActivity,EDIT_REQUEST_CODE)
        }
        linear_layout_2.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACTS_COMPLAINT_EDIT)
                    .withInt("reportType", 2)
                    .withInt("type", mType)
                    .withLong("targetUId", targetUId).navigation(this@ComplaintActivity,EDIT_REQUEST_CODE)

        }
        linear_layout_3.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACTS_COMPLAINT_EDIT)
                    .withInt("reportType", 3)
                    .withInt("type", mType)
                    .withLong("targetUId", targetUId).navigation(this@ComplaintActivity,EDIT_REQUEST_CODE)
        }

        linear_layout_4.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACTS_COMPLAINT_EDIT)
                    .withInt("reportType", 4)
                    .withInt("type", mType)
                    .withLong("targetUId", targetUId).navigation(this@ComplaintActivity,EDIT_REQUEST_CODE)
        }

        linear_layout_5.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACTS_COMPLAINT_EDIT)
                    .withInt("reportType", 5)
                    .withInt("type", mType)
                    .withLong("targetUId", targetUId).navigation(this@ComplaintActivity,EDIT_REQUEST_CODE)
        }
    }

    override fun initData() {
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == EDIT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            finish()
        }
    }

}