package framework.telegram.business.ui.group

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.TextUtils
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.utils.CustomCoinNameFilter
import framework.telegram.support.BaseActivity
import framework.telegram.support.tools.ExpandClass.toast
import kotlinx.android.synthetic.main.bus_group_activity_edit.*

@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_EDIT_NAME)
class GroupNameEditActivity : BaseActivity() {

    private val mTitle by lazy { intent.getStringExtra("title") ?: "" }

    private val mMaxCount by lazy { intent.getIntExtra("max_count", Constant.Bus.MAX_TEXT_NAME) }

    private val mDefaultValue by lazy { intent.getStringExtra("defaultValue") ?: "" }

    private val mEditType by lazy { intent.getIntExtra("editType", 0) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bus_group_activity_edit)

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        custom_toolbar.showCenterTitle(mTitle)

        edit_text_context.filters = arrayOf<InputFilter>(CustomCoinNameFilter(mMaxCount))
        edit_text_context.setText(mDefaultValue)

        frame_layout_op.setOnClickListener {
            val text = edit_text_context.text.toString().trim()
            if (mEditType == 2) {
                val intent = Intent()
                intent.putExtra("text", text)
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else {
                if (TextUtils.isEmpty(text)) {
                    toast(getString(R.string.the_input_cannot_be_empty))
                    return@setOnClickListener
                }
                val intent = Intent()
                intent.putExtra("text", text)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }
}