package framework.telegram.business.ui.group

import android.os.Bundle
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.im.domain.pb.GroupProto
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.http.HttpException
import framework.telegram.business.ui.widget.ViewUtils
import framework.telegram.support.BaseActivity
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import kotlinx.android.synthetic.main.bus_group_activity_admin_permission.*
import kotlinx.android.synthetic.main.bus_group_activity_admin_permission.custom_toolbar
import kotlinx.android.synthetic.main.bus_administrator_item.*

/**
 * 设置管理员权限
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_ADMIN_PERMISSION)
class AdminPermissionActivity : BaseActivity() {

    private val mGroupId by lazy { intent.getLongExtra("groupId", 0) }
    private val mUid by lazy { intent.getLongExtra(Constant.ARouter_Key.KEY_TARGET_UID, 0) }
    private val mDisplayName by lazy { intent.getStringExtra("displayName") }
    private val mMemberIcon by lazy { intent.getStringExtra("memberIcon") }

    //权限
    private val mIsAdmin by lazy { intent.getBooleanExtra("isAdmin", false) }
    private var mBfUpdateData = false
    private var mBfJoinCheck = false
    private var mBfPushNotice = false
    private var mBfSetAdmin = false

    //在线状态
    private val mOnlineStatus by lazy { intent.getBooleanExtra("onlineStatus", false) }
    private val mLastOnlineTime by lazy { intent.getLongExtra("lastOnlineTime", 0) }
    private val mIsShowLastOnlineTime by lazy { intent.getBooleanExtra("isShowLastOnlineTime", false) }

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bus_group_activity_admin_permission)
        mBfUpdateData = intent.getBooleanExtra("bfUpdateData", false)
        mBfJoinCheck = intent.getBooleanExtra("bfJoinCheck", false)
        mBfPushNotice = intent.getBooleanExtra("bfPushNotice", false)
        mBfSetAdmin = intent.getBooleanExtra("bfSetAdmin", false)

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        if (mUid == mMineUid){
            switch_button_1.setEnable(false)
            switch_button_2.setEnable(false)
            switch_button_3.setEnable(false)
            switch_button_4.setEnable(false)
        }

        custom_toolbar.showCenterTitle(getString(R.string.rights_of_administrators))
        if (mIsAdmin && mUid != mMineUid){
            custom_toolbar.showRightImageView(R.drawable.common_icon_more, {
                val data = mutableListOf<String>()
                data.add(getString(R.string.remove_administrator))
                AppDialog.showBottomListView(this, this, data) { _, index, _ ->
                    when (index) {
                        0 -> {
                            removeAdmin(mUid)
                        }
                    }
                }
            })
        }

        initMem()

        switch_button_1.setData(getString(R.string.modified_group_data), mBfUpdateData) {
            mBfUpdateData = it
        }
        switch_button_2.setData(getString(R.string.receive_income_group_validation), mBfJoinCheck) {
            mBfJoinCheck = it
        }
        switch_button_3.setData(getString(R.string.announce), mBfPushNotice) {
            mBfPushNotice = it
        }
        switch_button_4.setData(getString(R.string.set_up_other_administrators), mBfSetAdmin) {
            mBfSetAdmin = it
        }

        btn_confirm.setOnClickListener {
            if (mIsAdmin) {
                //编辑管理员
                ArouterServiceManager.groupService.editGroupAdminRight(lifecycle(), mGroupId, mUid, mBfJoinCheck, mBfPushNotice, mBfUpdateData, mBfSetAdmin, GroupProto.GroupOperator.EDIT_ADMIN, {
                    toast(getString(R.string.edit_success))
                    finish()
                }, {
                    if (it is HttpException) {
                        toast(it.errMsg)
                    } else {
                        toast(getString(R.string.edit_failure))
                    }

                })
            } else {
                //添加管理员
                ArouterServiceManager.groupService.editGroupAdminRight(lifecycle(), mGroupId, mUid, mBfJoinCheck, mBfPushNotice, mBfUpdateData, mBfSetAdmin, GroupProto.GroupOperator.ADD_ADMIN, {
                    toast(getString(R.string.successfully_added))
                    finish()
                }, {
                    if (it is HttpException) {
                        toast(it.errMsg)
                    } else {
                        toast(getString(R.string.fail_to_add))
                    }

                })
            }
        }
    }

    private fun removeAdmin(uid: Long) {
        ArouterServiceManager.groupService.removeGroupAdmin(lifecycle(), mGroupId, uid, {
            finish()
            toast(getString(R.string.administrator_removed_successfully))
        }, {
            if(it is HttpException){
                toast(it.errMsg)
            }else{
                toast(getString(R.string.failed_to_remove_administrator))
            }
        })
    }

    private fun initMem() {
        iv_avatar.setImageURI(mMemberIcon)
        tv_name.text = mDisplayName
        if (mIsAdmin) {
            tv_sign.visibility = View.VISIBLE
        }
        //在线状态
        v_online_status_point.visibility = View.GONE
        if (mIsShowLastOnlineTime && mOnlineStatus) {
            v_online_status_point.visibility = View.VISIBLE
        }
        tv_status.visibility = View.VISIBLE
        ViewUtils.showOnlineStatus(ArouterServiceManager.messageService, tv_status, mUid, mIsShowLastOnlineTime, mOnlineStatus, mLastOnlineTime)
    }

}
