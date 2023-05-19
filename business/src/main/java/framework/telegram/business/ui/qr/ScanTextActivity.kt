package framework.telegram.business.ui.qr

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.support.BaseActivity
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.IntentUtils
import framework.telegram.ui.tools.Helper
import kotlinx.android.synthetic.main.bus_me_activity_text_qr.*

@Route(path = Constant.ARouter.ROUNTE_BUS_QR_TEXT_SCAN)
class ScanTextActivity : BaseActivity() {

    private val mText by lazy { intent.getStringExtra("text") }

    private val mHtml by lazy { intent.getStringExtra("html") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bus_me_activity_text_qr)

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        custom_toolbar.showCenterTitle(getString(R.string.string_qr_result))

        if (!TextUtils.isEmpty(mHtml)){
            image_view.background= getSimpleDrawable(R.drawable.icon_qr_result_link)
            text_view_name.text = mHtml
            text_view_name.setTextColor(getSimpleColor(R.color.c178aff))
            text_view_title.text = getString(R.string.string_qr_link)
            text_view_1.visibility = View.VISIBLE
            text_view_2.text = getString(R.string.string_qr_go_link)
        }else{
            image_view.background= getSimpleDrawable(R.drawable.icon_qr_result_text)
            text_view_title.text = getString(R.string.string_qr_text)
            text_view_name.text = mText
            text_view_name.setTextColor(getSimpleColor(R.color.black))
            text_view_1.visibility = View.GONE
            text_view_2.text = getString(R.string.string_qr_copy_text)
        }

        text_view_1.setOnClickListener {
            Helper.setPrimaryClip(this@ScanTextActivity,mHtml)
            toast(getString(R.string.copy_success))
        }

        text_view_2.setOnClickListener {
            if (!TextUtils.isEmpty(mHtml)){
                startActivity(IntentUtils.openLink(mHtml))
            }else{
                Helper.setPrimaryClip(this@ScanTextActivity,mText)
                toast(getString(R.string.copy_success))
            }
        }

    }
}