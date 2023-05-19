package framework.telegram.business.ui.group

import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.GroupProto
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_ADD_TOKEN
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_IDCODE
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_QRCODE
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_GID
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.group.presenter.JoinGroupContract
import framework.telegram.business.ui.group.presenter.JoinGroupPresenterImpl
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import kotlinx.android.synthetic.main.bus_group_activity_join.*
import kotlinx.android.synthetic.main.bus_group_activity_un_member_detail.custom_toolbar
import kotlinx.android.synthetic.main.bus_group_activity_un_member_detail_invite.app_image_view_group_icon
import kotlinx.android.synthetic.main.bus_group_activity_un_member_detail_invite.app_text_view_group_name

/**
 * Created by lzh on 19-5-27.
 * INFO:
 *
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_JOIN)
class JoinGroupActivity : BaseBusinessActivity<JoinGroupContract.Presenter>(),
    JoinGroupContract.View {

    companion object {
        internal const val EDIT_REQUEST_CODE = 0x1000
    }

    private val mGroupId by lazy { intent.getLongExtra(KEY_TARGET_GID, 0) }

    private val mQrCode by lazy { intent.getStringExtra(KEY_QRCODE) ?: "" }

    private val mIDCode by lazy { intent.getStringExtra(KEY_IDCODE) ?: "" }

    private val mAddToken by lazy { intent.getStringExtra(KEY_ADD_TOKEN) ?: "" }

    private var mAppDialog: AppDialog? = null

    override fun getLayoutId() = R.layout.bus_group_activity_join

    override fun checkFields(): Boolean {
        return mGroupId > 0 && !TextUtils.isEmpty(mQrCode)
    }

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
    }

    override fun initListen() {
        frame_layout_join.setOnClickListener {
            if (mPresenter?.isJoinCheck() == true) {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_EDIT_NAME)
                    .withString(
                        "defaultValue",
                        String.format(
                            getString(R.string.i_am),
                            AccountManager.getLoginAccount(AccountInfo::class.java).getNickName()
                        )
                    )
                    .withInt("max_count", Constant.Bus.MAX_TEXT_NAME)
                    .withString("title", getString(R.string.the_group_of_reason))
                    .navigation(this@JoinGroupActivity, EDIT_REQUEST_CODE)
            } else {
                mPresenter?.joinGroup(mGroupId, "")
            }
        }
    }

    override fun initData() {
        JoinGroupPresenterImpl(this, this, lifecycle(), mGroupId, mQrCode, mIDCode, mAddToken).start()
    }

    override fun showLoading() {
        mAppDialog = AppDialog.showLoadingView(this@JoinGroupActivity, this@JoinGroupActivity)
    }

    override fun dissmissLoading() {
        mAppDialog?.dismiss()
    }

    override fun joinSuccess(bfJoinCheck: Boolean) {
        if (bfJoinCheck) {
            toast(getString(R.string.please_wait_patiently_for_group_manager_review))
            finish()
        } else {
            toast(getString(R.string.join_group_successfully))
            jumpToChat()
        }
    }

    override fun jumpToChat() {
        ARouter.getInstance()
            .build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_GROUP_CHAT_ACTIVITY)
            .withLong("targetGid", mGroupId).navigation()
        finish()
    }

    override fun refreshUI(groupBase: GroupProto.GroupBase, isBan: Boolean) {
        app_image_view_group_icon.setImageURI(groupBase.pic)
        app_text_view_group_name.text = groupBase.name
        text_view_group_member_count.text =
            String.format(getString(R.string.general_mat), groupBase.memberCount)
        setJoinBtn(isBan)
    }

    private fun setJoinBtn(isBan: Boolean) {
        frame_layout_join.visibility = View.VISIBLE
        if (isBan) {
            text_ban.visibility = View.VISIBLE
            frame_layout_join.isEnabled = false
            frame_layout_join.background =
                getSimpleDrawable(R.drawable.common_corners_trans_d4d6d9_6_0)
        } else {
            text_ban.visibility = View.GONE
            frame_layout_join.isEnabled = true
            frame_layout_join.background =
                getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
        }
    }


    override fun showError(errStr: String) {
        if (!TextUtils.isEmpty(errStr)) {
            toast(errStr)
        }
    }

    override fun destory() {
        finish()
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as JoinGroupContract.Presenter
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == EDIT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val text = data?.getStringExtra("text") ?: ""
            if (!TextUtils.isEmpty(text)) {
                mPresenter?.joinGroup(mGroupId, text)
            }
        }
    }
}