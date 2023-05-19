package framework.telegram.business.ui.contacts

/**
 * Created by lzh on 19-6-20.
 * INFO:
 */

import android.app.Activity
import android.content.Intent
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.utils.CustomCoinNameFilter
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.ui.utils.ScreenUtils
import kotlinx.android.synthetic.main.bus_me_activity_item_edit.*


/**
 * Created by lzh on 19-6-7.
 * INFO:
 * type 0 单行
 * type 1 多行
 *
 * from 0 备注名
 * from 1 描述
 * from 2 个性签名
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_CONTACTS_INFO_EDIT)
class ContactEditActivity : BaseBusinessActivity<BasePresenter>() {
    override fun getLayoutId() = R.layout.bus_me_activity_item_edit

    private val mUid by lazy { intent.getLongExtra("uid", 0) }
    private val mType by lazy { intent.getIntExtra("type", 0) }
    private val mTitle by lazy { intent.getStringExtra("title") ?: "" }
    private val mDefaultContent by lazy { intent.getStringExtra("defaultContent") ?: "" }
    private val mNickName by lazy { intent.getStringExtra("nickName") ?: "" }

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        if (mType == 0) {
            edit_text_context.maxLines = 1
            edit_text_context.filters =
                arrayOf<InputFilter>(CustomCoinNameFilter(Constant.Bus.MAX_TEXT_NAME))
        } else {
            edit_text_context.layoutParams.height = ScreenUtils.dp2px(this, 120f)
            edit_text_context.filters =
                arrayOf<InputFilter>(InputFilter.LengthFilter(Constant.Bus.MAX_TEXT_COUNT))
            text_view_count.text = (Constant.Bus.MAX_TEXT_COUNT - mDefaultContent.length).toString()
            text_view_count.visibility = View.VISIBLE
        }
        custom_toolbar.showCenterTitle(mTitle)
        edit_text_context.setText(mDefaultContent)
    }

    override fun initListen() {
        frame_layout_op.setOnClickListener {
            val text = edit_text_context.text.toString()
            val intent = Intent()
            intent.putExtra("uid", mUid)
            intent.putExtra("text", text)
            intent.putExtra("nickName", mNickName)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        edit_text_context.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val str = s.toString()
                if (mType == 0) {
                    if (!TextUtils.isEmpty(str)) {
                        image_view_delete.visibility = View.VISIBLE
                    } else {
                        image_view_delete.visibility = View.GONE
                    }
                } else {
                    text_view_count.text = (Constant.Bus.MAX_TEXT_COUNT - str.length).toString()
                }
            }
        })
    }

    override fun initData() {
    }

}