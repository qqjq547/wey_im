package framework.telegram.business.ui.group

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.CommonProto
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.http.HttpException
import framework.telegram.business.ui.group.GroupNoticeActivity.common.REQUEST_CODE
import framework.telegram.message.bridge.event.DisableGroupMessageEvent
import framework.telegram.support.BaseActivity
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ActivitiesHelper
import kotlinx.android.synthetic.main.bus_group_activity_group_notice.*
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_group_activity_group_notice.custom_toolbar
import kotlinx.android.synthetic.main.bus_contacts_selectable_item.*
import java.text.SimpleDateFormat

@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_NOTICE)
class GroupNoticeActivity : BaseActivity() {

    object common{
        val REQUEST_CODE=1001
    }

    private val mGroupId by lazy { intent.getLongExtra("groupId", 0) }
    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }
    private val mHasPermission by lazy { intent.getBooleanExtra("bfPushNotice", false) }
    private var mRightText: TextView? = null

    private var mNoticeId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bus_group_activity_group_notice)

        mNoticeId = intent.getLongExtra("noticeId", 0)

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        custom_toolbar.showCenterTitle(getString(R.string.group_of_announcement))

        EventBus.getFlowable(DisableGroupMessageEvent::class.java)
                .bindToLifecycle(this@GroupNoticeActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.groupId == mGroupId) {
                        if (ActivitiesHelper.getInstance().topActivity == this@GroupNoticeActivity){
                            AppDialog.show(this@GroupNoticeActivity, this@GroupNoticeActivity) {
                                positiveButton(text = getString(R.string.confirm), click = {
                                    //清空聊天记录
                                    finish()
                                })
                                cancelOnTouchOutside(false)
                                message(text = getString(R.string.string_group_dismiss_title))
                            }
                        }else{
                            finish()
                        }
                    }
                }

        if (mHasPermission) {
            btn_confirm.visibility = View.VISIBLE
            custom_toolbar.showRightTextView(getString(R.string.empty), {
                AppDialog.show(this@GroupNoticeActivity, this@GroupNoticeActivity) {
                    positiveButton(text = getString(R.string.confirm), click = {
                        ArouterServiceManager.groupService.clearNotice(lifecycle(), mGroupId, {
                            mNoticeId=0
                            getData()
                        }, {
                            if(it is HttpException){
                                toast(it.errMsg)
                            }else{
                                toast(getString(R.string.empty_the_failure))
                            }

                        })
                    })
                    message(text = getString(R.string.make_sure_to_empty_the_group_announcement))

                    cancelOnTouchOutside(true)
                    cancelable(true)
                    negativeButton(text = getString(R.string.cancel))
                }
            }, 0, {
                mRightText = it
            })
        }else {
            btn_confirm.visibility = View.GONE
        }

        text_view_online_status.visibility = View.VISIBLE
        check_box_selected.visibility = View.GONE
        btn_confirm.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_CREATE_NOTICE)
                    .withLong("groupId", mGroupId)
                    .withLong("uid", mMineUid).navigation(this@GroupNoticeActivity,REQUEST_CODE)
        }

        getData()
    }

    private fun getData() {
        ArouterServiceManager.groupService.getNotice(lifecycle(), mGroupId,mNoticeId, {
            if (TextUtils.isEmpty(it.notice)) {
                ll_content.visibility = View.GONE
                ll_empty.visibility = View.VISIBLE
                if (mRightText != null) {
                    mRightText?.visibility = View.GONE
                }
            } else {
                ll_content.visibility = View.VISIBLE
                ll_empty.visibility = View.GONE
                if (mRightText != null) {
                    mRightText?.visibility = View.VISIBLE
                }

                image_view_icon.setImageURI(it.editUser.user.icon)
                app_text_view_name.text = getDisplayName(it.editUser.groupNickName, it.editUser.user.nickName, it.editUser.user.friendRelation.remarkName)
                text_view_online_status.text = getTimeStr(it.releaseTime)
                when {
                    it.editUser.type.number == CommonProto.GroupMemberType.HOST.number -> {
                        //群主
                        text_view_flag.visibility = View.VISIBLE
                        tv_admin.visibility = View.GONE
                    }
                    it.editUser.type.number == CommonProto.GroupMemberType.MANAGE.number -> {
                        //管理员
                        text_view_flag.visibility = View.GONE
                        tv_admin.visibility = View.VISIBLE
                    }
                    else -> {
                        text_view_flag.visibility = View.GONE
                        tv_admin.visibility = View.GONE
                    }
                }

                tv_content.isFindUrl = true
//                tv_content.setUrlColor(ContextCompat.getColor(BaseApp.app, R.color.c178aff))
                tv_content.text = it.notice
            }
        }, {
            toast(getString(R.string.failed_to_get_group_announcement))
        })
    }

    private fun getTimeStr(releaseTime: Long): String? {
        return if (releaseTime == 0L) {
            "-"
        } else {
            val formatter = SimpleDateFormat("dd-MM-yyyy")
            formatter.format(releaseTime)
        }
    }

    private fun getDisplayName(groupNickName: String?, nickName: String?, remarkName: String?): String? {
        val displayName = if (TextUtils.isEmpty(remarkName)) groupNickName else remarkName
        return if (TextUtils.isEmpty(displayName)) nickName else displayName
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode== Activity.RESULT_OK){
            val noticeId= data?.getLongExtra("newNoticeId",-1L)
            if (noticeId != -1L){
                mNoticeId=noticeId!!
                getData()
            }
        }
    }
}
