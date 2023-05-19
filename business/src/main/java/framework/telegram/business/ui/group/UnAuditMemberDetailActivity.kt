package framework.telegram.business.ui.group

import android.app.Activity
import android.content.Intent
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.CommonProto
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_FROM_SOURCE
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_GROUP_NAME
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_RECORD_ID
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_REQ_MSG
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_UID
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.group.presenter.UnAuditMemberContract
import framework.telegram.business.ui.group.presenter.UnAuditMemberPresenterImpl
import framework.telegram.support.BaseApp
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.menu.FloatMenu
import framework.telegram.ui.tools.Helper
import kotlinx.android.synthetic.main.bus_contacts_item_friend_cell.view.*
import kotlinx.android.synthetic.main.bus_contacts_item_friend_head3.*
import kotlinx.android.synthetic.main.bus_group_activity_un_member_detail.*

/**
 * Created by lzh on 19-5-27.
 * INFO:
 *
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_UN_AUDIT_MEMBER_DETAIL)
class UnAuditMemberDetailActivity : BaseBusinessActivity<UnAuditMemberContract.Presenter>(),
    UnAuditMemberContract.View {

    private val mTargetUid by lazy { intent.getLongExtra(KEY_TARGET_UID, 0) }

    private val mRecordId by lazy { intent.getLongExtra(KEY_RECORD_ID, 0) }

    private val mReqMsg by lazy { intent.getStringExtra(KEY_REQ_MSG) ?: "" }

    private val mGroupName by lazy { intent.getStringExtra(KEY_GROUP_NAME) ?: "" }

    private val mFromSource by lazy { intent.getStringExtra(KEY_FROM_SOURCE) ?: "" }

    private var mAppDialog: AppDialog? = null

    override fun getLayoutId() = R.layout.bus_group_activity_un_member_detail

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

    }

    override fun initListen() {
        frame_layout_agree.setOnClickListener {
            //同意
            mPresenter?.makeOperate(mRecordId, true)
        }

        frame_layout_refuse.setOnClickListener {
            //拒绝
            mPresenter?.makeOperate(mRecordId, false)
        }
    }

    override fun initData() {
        if (this.mTargetUid <= 0L || this.mRecordId <= 0L) {
            finish()
            return
        }

        UnAuditMemberPresenterImpl(this, this, lifecycle(), mTargetUid).start()
    }

    override fun showLoading() {
        mAppDialog = AppDialog.showLoadingView(
            this@UnAuditMemberDetailActivity,
            this@UnAuditMemberDetailActivity
        )
    }

    override fun dissmissLoading() {
        mAppDialog?.dismiss()
    }

    override fun operateReq(op: Boolean) {
        val intent = Intent()
        intent.putExtra(Constant.ARouter_Key.KEY_OPERATE, op)
        intent.putExtra(
            Constant.ARouter_Key.KEY_POSITION,
            getIntent().getIntExtra(Constant.ARouter_Key.KEY_POSITION, -1)
        )
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun refreshUI(contact: ContactDataModel) {
        app_image_view_icon.setImageURI(contact.icon)
        app_image_view_icon.setOnClickListener {
            ARouter.getInstance()
                .build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_AVATAR_PREVIEW)
                .withString("imageUrl", contact.icon)
                .navigation()
        }
        app_text_view_username.text = contact.displayName
        app_text_view_user_mark.text = mReqMsg

        val builder = SpannableStringBuilder(
            String.format(
                getString(R.string.apply_to_join_mat),
                mGroupName
            )
        )//这里有一个空格
        builder.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    this@UnAuditMemberDetailActivity,
                    R.color.c178aff
                )
            ),
            getString(R.string.apply_to_join).length + 1,
            getString(R.string.apply_to_join).length + 1 + mGroupName.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        app_text_view_user_type.text = builder

        var sex = getString(R.string.confidentiality)
        if (contact.sex == CommonProto.Gender.FEMALE.number) {
            sex = getString(R.string.woman)
        } else if (contact.sex == CommonProto.Gender.MALE.number) {
            sex = getString(R.string.man)
        }

        friend_item_source.setData(getString(R.string.source), mFromSource)

        friend_item_sex.setData(getString(R.string.gender), sex)

        if (TextUtils.isEmpty(contact.signature)) {
            friend_item_sign.setData(
                getString(R.string.personalized_signature),
                getString(R.string.they_didn_t_write_anything)
            )
        } else {
            friend_item_sign.setData(getString(R.string.personalized_signature), contact.signature)
        }

        if (!TextUtils.isEmpty(contact.identify) && contact.identify != "--") {
            friend_item_number.visibility = View.VISIBLE
            friend_item_number.setData(getString(R.string.six_eight_number), contact.identify)
            friend_item_number.setOnLongClickListener {
                showCopyDialog(friend_item_number.app_text_view_value, contact.identify)
                return@setOnLongClickListener false
            }
        } else {
            friend_item_number.visibility = View.GONE
        }
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as UnAuditMemberContract.Presenter
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun showError(errStr: String) {
        if (!TextUtils.isEmpty(errStr)) {
            toast(errStr)
        }
    }

    override fun destory() {
        finish()
    }

    private fun showCopyDialog(v: View, str: String?) {
        val floatMenu = FloatMenu(this)
        val items = mutableListOf<String>()
        items.add(getString(R.string.copy))
        floatMenu.items(*items.toTypedArray())
        floatMenu.showDropDown(v, v.width - 5, 0)
        floatMenu.setOnItemClickListener { _, text ->
            when (text) {
                getString(R.string.copy) -> {
                    Helper.setPrimaryClip(BaseApp.app, str)
                    toast(getString(R.string.copy_success))
                }
            }
        }
    }
}