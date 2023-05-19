package framework.telegram.business.ui.group

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.http.HttpException
import framework.telegram.business.ui.group.adapter.AdministratorAdapter
import framework.telegram.business.ui.group.bean.AdministratorModel
import framework.telegram.support.BaseActivity
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.menu.FloatMenu
import kotlinx.android.synthetic.main.bus_group_activity_administrator.*
import kotlinx.android.synthetic.main.bus_group_activity_administrator.custom_toolbar

/**
 * 管理管理员
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_ADMINISTRATOR)
class AdministratorActivity : BaseActivity() {

    companion object {
        const val ADMINISTRATOR_ACTIVITY_RESULT = 0x1001
    }

    private val mGroupId by lazy { intent.getLongExtra("groupId", 0) }
    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }
    private val isSetAdmin by lazy { intent.getBooleanExtra("bfSetAdmin", false) }

    private val mData = arrayListOf<AdministratorModel>()

    private var mRightText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bus_group_activity_administrator)

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.administrator))

        val adapter = AdministratorAdapter()
        if (isSetAdmin) {
            custom_toolbar.showRightTextView(getString(R.string.add), {
            }, 0, {
                mRightText = it
            })

            adapter.setOnItemClickListener { _, _, position ->
                editAdmin(position)
            }
            adapter.setOnItemLongClickListener { _, _, position ->

                //群主和自己不能编辑
                if (position <= 2|| mData[position].uid==mMineUid) {
                    return@setOnItemLongClickListener false
                }
                val floatMenu = FloatMenu(this)

                val items = mutableListOf<String>()
                items.add(getString(R.string.remove))

                floatMenu.items(*items.toTypedArray())

                floatMenu.show(rv.popPoint)
                floatMenu.setOnItemClickListener { _, text ->
                    when (text) {
                        getString(R.string.remove) -> {
                            removeAdmin(mData[position].uid)
                        }
                    }
                }
                return@setOnItemLongClickListener false
            }
        }

        rv.initMultiTypeRecycleView(LinearLayoutManager(this), adapter, false)
        rv.refreshController().setEnablePullToRefresh(false)
    }

    private fun editAdmin(position: Int) {
        if (position > 2) {
            //进入权限编辑页
            editAdmin(true,
                    mGroupId,
                    mData[position].uid,
                    mData[position].getDisplayName(),
                    mData[position].icon,
                    mData[position].onlineStatus,
                    mData[position].lastOnlineTime,
                    mData[position].isShowLastOnlineTime,
                    mData[position].right?.bfUpdateData!!,
                    mData[position].right?.bfJoinCheck!!,
                    mData[position].right?.bfPushNotice!!,
                    mData[position].right?.bfSetAdmin!!)
        }
    }

    private fun editAdmin(isAdmin: Boolean,
                          groupId: Long,
                          uid: Long,
                          displayName: String,
                          memberIcon: String,
                          onlineStatus: Boolean,
                          lastOnlineTime: Long,
                          isShowLastOnlineTime: Boolean,
                          bfUpdateData: Boolean,
                          bfJoinCheck: Boolean,
                          bfPushNotice: Boolean,
                          bfSetAdmin: Boolean) {
        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_ADMIN_PERMISSION)
                .withBoolean("isAdmin", isAdmin)
                .withLong("groupId", groupId)
                .withLong(Constant.ARouter_Key.KEY_TARGET_UID, uid)
                .withString("displayName", displayName)
                .withString("memberIcon", memberIcon)
                .withBoolean("onlineStatus", onlineStatus)
                .withLong("lastOnlineTime", lastOnlineTime)
                .withBoolean("isShowLastOnlineTime", isShowLastOnlineTime)
                .withBoolean("bfUpdateData", bfUpdateData)
                .withBoolean("bfJoinCheck", bfJoinCheck)
                .withBoolean("bfPushNotice", bfPushNotice)
                .withBoolean("bfSetAdmin", bfSetAdmin)
                .navigation()
    }

    private fun removeAdmin(uid: Long) {
        ArouterServiceManager.groupService.removeGroupAdmin(lifecycle(), mGroupId, uid, {
            getData()
            toast(getString(R.string.administrator_removed_successfully))
        }, {
            if(it is HttpException){
                toast(it.errMsg)
            }else{
                toast(getString(R.string.failed_to_remove_administrator))
            }
        })
    }

    private fun addAdmin(size:Int){
        if (mData.size >= size+3) {
            AppDialog.show(this@AdministratorActivity, this@AdministratorActivity) {
                message(text = getString(R.string.the_number_of_administrators_has_reached_the_limit))
                positiveButton(text = getString(R.string.confirm), click = {
                    dismiss()
                })

                cancelOnTouchOutside(true)
                cancelable(true)
            }
            return@addAdmin
        }
        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_OPERATE)
                .withInt("operateType", OperateGroupMemberActivity.OPERATE_TYPE_ADD_ADMIN)
                .withLong("groupId", mGroupId)
                .navigation(this@AdministratorActivity, ADMINISTRATOR_ACTIVITY_RESULT)
    }

    override fun onResume() {
        super.onResume()
        getData()
    }

    private fun getData() {
        mData.clear()
        ArouterServiceManager.groupService.getGroupAdminList(lifecycle(), mGroupId, { resp ->
            val title1 = AdministratorModel(AdministratorModel.Type.TEXT)
            title1.titleName = getString(R.string.group_owner)
            mData.add(title1)

            val hostUser = AdministratorModel(AdministratorModel.Type.ITEM)
            hostUser.remarkName = resp.hostUser.user.friendRelation.remarkName
            hostUser.nickName = resp.hostUser.user.nickName
            hostUser.groupNickName = resp.hostUser.groupNickName
            hostUser.icon = resp.hostUser.user.icon
            hostUser.uid = resp.hostUser.user.uid
            hostUser.right = resp.hostUser.right
            hostUser.onlineStatus = resp.hostUser.user.userOnOrOffline.online
            hostUser.lastOnlineTime = resp.hostUser.user.userOnOrOffline.createTime
            hostUser.isShowLastOnlineTime = resp.hostUser.user.userOnOrOffline.bfShow
            hostUser.isSetAdmin= false
            mData.add(hostUser)

            val title2 = AdministratorModel(AdministratorModel.Type.TEXT)
            title2.titleName = String.format(getString(R.string.admin_match),resp.adminListList.size,resp.adminNumMax)
            mData.add(title2)

            resp.adminListList.forEach {
                val user = AdministratorModel(AdministratorModel.Type.ITEM)
                user.remarkName = it.user.friendRelation.remarkName
                user.nickName = it.user.nickName
                user.groupNickName = it.groupNickName
                user.icon = it.user.icon
                user.uid = it.user.uid
                user.right = it.right
                user.onlineStatus = it.user.userOnOrOffline.online
                user.lastOnlineTime = it.user.userOnOrOffline.createTime
                user.isShowLastOnlineTime = it.user.userOnOrOffline.bfShow
                user.isSetAdmin= isSetAdmin
                mData.add(user)
            }
            rv.itemController().setNewData(mData)

            if (isSetAdmin&&mRightText!=null){
                mRightText?.setOnClickListener {
                    addAdmin(resp.adminNumMax)
                }
            }
        }, {
            if(it is HttpException){
                toast(it.errMsg)
            }else{
                toast(getString(R.string.failed_to_get_administrator_list))
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADMINISTRATOR_ACTIVITY_RESULT && resultCode == Activity.RESULT_OK) {
            val uid = data?.getLongExtra(Constant.ARouter_Key.KEY_TARGET_UID, 0)
            val displayName = data?.getStringExtra("displayName")
            val memberIcon = data?.getStringExtra("memberIcon")
            val onlineStatus = data?.getBooleanExtra("onlineStatus", false)
            val lastOnlineTime = data?.getLongExtra("lastOnlineTime", 0)
            val isShowLastOnlineTime = data?.getBooleanExtra("isShowLastOnlineTime", false)

            val admin = searchAdmin(uid!!)
            if (admin == null) {
                //非管理员
                editAdmin(false,
                        mGroupId,
                        uid,
                        displayName!!,
                        memberIcon!!,
                        onlineStatus!!,
                        lastOnlineTime!!,
                        isShowLastOnlineTime!!,
                        true,
                        true,
                        true,
                        false)
            } else {
                //管理员
                editAdmin(true,
                        mGroupId,
                        admin.uid,
                        admin.getDisplayName(),
                        admin.icon,
                        admin.onlineStatus,
                        admin.lastOnlineTime,
                        admin.isShowLastOnlineTime,
                        admin.right?.bfUpdateData!!,
                        admin.right?.bfJoinCheck!!,
                        admin.right?.bfPushNotice!!,
                        admin.right?.bfSetAdmin!!)
            }
        }
    }

    private fun searchAdmin(u: Long): AdministratorModel? {
        mData.forEach {
            if (it.uid == u) return it
        }
        return null
    }
}
