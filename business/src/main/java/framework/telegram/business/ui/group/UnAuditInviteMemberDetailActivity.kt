package framework.telegram.business.ui.group

import android.app.Activity
import android.content.Intent
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.bridge.Constant
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_GROUP_ICON
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_GROUP_NAME
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_POSITION
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_RECORD_ID
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_USER_ICON
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_USER_NAME
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.group.presenter.UnAuditInviteMemberContract
import framework.telegram.business.ui.group.presenter.UnAuditInviteMemberPresenterImpl
import framework.telegram.support.mvp.BasePresenter
import kotlinx.android.synthetic.main.bus_group_activity_un_member_detail.custom_toolbar
import kotlinx.android.synthetic.main.bus_group_activity_un_member_detail.frame_layout_agree
import kotlinx.android.synthetic.main.bus_group_activity_un_member_detail.frame_layout_refuse
import kotlinx.android.synthetic.main.bus_group_activity_un_member_detail_invite.*
import framework.telegram.support.tools.ExpandClass.toast

/**
 * Created by lzh on 19-5-27.
 * INFO:
 *
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_UN_AUDIT_INVITE_MEMBER_DETAIL)
class UnAuditInviteMemberDetailActivity : BaseBusinessActivity<UnAuditInviteMemberContract.Presenter>(), UnAuditInviteMemberContract.View {

    private val mRecordId by lazy { intent.getLongExtra(KEY_RECORD_ID, 0) }

    private val mTargetUserIcon by lazy { intent.getStringExtra(KEY_USER_ICON) }

    private val mTargetUserName by lazy { intent.getStringExtra(KEY_USER_NAME) }

    private val mGroupIcon by lazy { intent.getStringExtra(KEY_GROUP_ICON) }

    private val mGroupName by lazy { intent.getStringExtra(KEY_GROUP_NAME) }

    override fun getLayoutId() = R.layout.bus_group_activity_un_member_detail_invite

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
        if (this.mRecordId <= 0L) {
            finish()
            return
        }

        UnAuditInviteMemberPresenterImpl(this, this, lifecycle()).start()
    }

    override fun operateReq(op: Boolean) {
        val intent = Intent()
        intent.putExtra(KEY_POSITION, getIntent().getIntExtra(KEY_POSITION, -1))
        intent.putExtra(Constant.ARouter_Key.KEY_OPERATE, op)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun refreshUI() {
        app_image_view_group_icon.setImageURI(mGroupIcon)
        app_text_view_group_name.text = mGroupName

        app_image_view_user_icon.setImageURI(mTargetUserIcon)
        app_text_view_user_name.text = mTargetUserName
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as UnAuditInviteMemberContract.Presenter
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun showError(errStr: String?) {
        toast(errStr?:"")
    }

    override fun destory() {
        finish()
    }
}