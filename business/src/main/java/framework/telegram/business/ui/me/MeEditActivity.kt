package framework.telegram.business.ui.me

import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.im.domain.pb.UserProto
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.event.UserInfoChangeEvent
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.InfoContract
import framework.telegram.business.ui.me.presenter.InfoPresenterImpl
import framework.telegram.business.utils.CustomCoinNameFilter
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.KeyboardktUtils
import framework.telegram.ui.utils.ScreenUtils
import kotlinx.android.synthetic.main.bus_me_activity_item_edit.*


/**
 * Created by lzh on 19-6-7.
 * INFO:
 * @see mActivityType
 * 根据传入的type不同代表了不同的编辑
 * 0： 改昵称,改备注
 * 1： 个性签名
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_INFO_EDIT)
class MeEditActivity : BaseBusinessActivity<InfoContract.Presenter>(), InfoContract.View {
    /*
         改昵称,改备注
     */
    private val TYPE_NICKNAME = 0

    /*
        个性签名
     */
    private val TYPE_SIGNATURE = 1

    /*
        编辑的类型
        0： 改昵称,改备注
        1： 个性签名
     */
    private val mActivityType by lazy { intent.getIntExtra("type", 0) }

    /*
        根据编辑类型不同，会有不同的内容
     */
    private val mContent by lazy { intent.getStringExtra("content") ?: "" }

    override fun getLayoutId() = R.layout.bus_me_activity_item_edit

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }


        if (mActivityType == TYPE_NICKNAME) {
            custom_toolbar.showCenterTitle(getString(R.string.bus_me_nickname))
            edit_text_context.filters =
                arrayOf<InputFilter>(CustomCoinNameFilter(Constant.Bus.MAX_TEXT_NAME))
            edit_text_context.maxLines = 1
        } else if (mActivityType == TYPE_SIGNATURE) {
            custom_toolbar.showCenterTitle(getString(R.string.bus_me_signature))
            edit_text_context.filters =
                arrayOf<InputFilter>(InputFilter.LengthFilter(Constant.Bus.MAX_TEXT_COUNT))
            edit_text_context.layoutParams.height = ScreenUtils.dp2px(this, 120f)
            text_view_count.text = (Constant.Bus.MAX_TEXT_COUNT - mContent.length).toString()
            text_view_count.visibility = View.VISIBLE
        }

        KeyboardktUtils.showKeyboardDelay(edit_text_context)

    }

    override fun initListen() {
        frame_layout_op.setOnClickListener {
            if (mActivityType == TYPE_NICKNAME) {
                val str = edit_text_context.text.toString()

                if (!TextUtils.isEmpty(str)) {
                    val userParam = UserProto.UserParam.newBuilder().setNickName(str).build()
                    val list = listOf(UserProto.UserOperator.NICK_NAME)
                    mPresenter?.savePerfectInfo(str, list, userParam)
                } else {
                    toast(getString(R.string.bus_me_nickname_error))
                }

            } else if (mActivityType == TYPE_SIGNATURE) {
                val str = edit_text_context.text.toString()
                val userParam = UserProto.UserParam.newBuilder().setSignature(str).build()
                val list = listOf(UserProto.UserOperator.SIGNATURE)
                mPresenter?.savePerfectInfo(str, list, userParam)
            }
        }

        edit_text_context.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val str = s.toString()

                if (mActivityType == TYPE_NICKNAME) {
                    if (!TextUtils.isEmpty(str)) {
                        image_view_delete.visibility = View.VISIBLE
                    } else {
                        image_view_delete.visibility = View.GONE
                    }
                } else if (mActivityType == TYPE_SIGNATURE) {
                    text_view_count.text = (Constant.Bus.MAX_TEXT_COUNT - str.length).toString()
                }

            }
        })

        image_view_delete.setOnClickListener {
            edit_text_context.setText("")
        }

        all_layout.setOnClickListener {
            KeyboardktUtils.hideKeyboard(all_layout)
        }
    }

    override fun initData() {
        edit_text_context.setText(mContent)
        InfoPresenterImpl(this, this, lifecycle()).start()
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@MeEditActivity, this@MeEditActivity)
    }

    /**
     * 保存完善账户信息成功
     */
    override fun savePerfectInfoSuccess(str: String) {
        dialog?.dismiss()
        toast(getString(R.string.bus_me_update_change))
        val accountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }
        if (mActivityType == TYPE_NICKNAME) {
            accountInfo.putNickName(str)
        } else if (mActivityType == TYPE_SIGNATURE) {
            accountInfo.putSignature(str)
        }
        EventBus.publishEvent(UserInfoChangeEvent())
        finish()
    }

    override fun showErrMsg(str: String?) {
        dialog?.dismiss()
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as InfoPresenterImpl
    }

    override fun isActive() = true

}