package framework.telegram.business.ui.group

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.support.BaseActivity
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.http.HttpException
import framework.telegram.message.bridge.event.DisableGroupMessageEvent
import framework.telegram.message.bridge.event.GroupInfoChangeEvent
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ActivitiesHelper
import kotlinx.android.synthetic.main.bus_group_activity_create_group_notice.*
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.wordfilter.WordFilter
import framework.telegram.ui.dialog.AppDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_group_activity_create_group_notice.btn_confirm
import kotlinx.android.synthetic.main.bus_group_activity_create_group_notice.custom_toolbar


@Route(path = Constant.ARouter.ROUNTE_BUS_GROUP_CREATE_NOTICE)
class CreateGroupNoticeActivity : BaseActivity() {

    private val mGroupId by lazy { intent.getLongExtra("groupId", 0) }
    private val mUid by lazy { intent.getLongExtra("uid", 0) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bus_group_activity_create_group_notice)

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            areYouSure()
        }
        custom_toolbar.showCenterTitle(getString(R.string.issue_new_announcements))

        tv_num.text = "0/500"
        et_content.addTextChangedListener(MyTextWatcher(et_content, tv_num, btn_confirm))
        switch_button.isChecked = false

        btn_confirm.isEnabled = false
        btn_confirm.setBackgroundResource(R.drawable.common_corners_trans_d4d6d9_6_0)
        btn_confirm.setOnClickListener {
            val content = et_content.text.toString()
            if (switch_button.isChecked) {
                AppDialog.show(this@CreateGroupNoticeActivity, this@CreateGroupNoticeActivity) {
                    positiveButton(text = getString(R.string.issue), click = {
                        commit(content)
                    })
                    message(text = getString(R.string.determine_the_release))

                    cancelOnTouchOutside(true)
                    cancelable(true)
                    negativeButton(text = getString(R.string.cancel))
                }
            } else {
                commit(content)
            }
        }

        EventBus.getFlowable(DisableGroupMessageEvent::class.java)
            .bindToLifecycle(this@CreateGroupNoticeActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.groupId == mGroupId) {
                    if (ActivitiesHelper.getInstance().topActivity == this@CreateGroupNoticeActivity) {
                        AppDialog.show(
                            this@CreateGroupNoticeActivity,
                            this@CreateGroupNoticeActivity
                        ) {
                            positiveButton(text = getString(R.string.confirm), click = {
                                //清空聊天记录
                                finish()
                            })
                            cancelOnTouchOutside(false)
                            message(text = getString(R.string.string_group_dismiss_title))
                        }
                    } else {
                        finish()
                    }
                }
            }
    }

    override fun onBackPressed() {
        areYouSure()
    }

    private fun areYouSure() {
        if (TextUtils.isEmpty(et_content.text.toString())) {
            finish()
        } else {
            AppDialog.show(this@CreateGroupNoticeActivity, this@CreateGroupNoticeActivity) {
                positiveButton(text = getString(R.string.confirm), click = {
                    finish()
                })
                message(text = getString(R.string.abandon_the_editing))

                cancelOnTouchOutside(true)
                cancelable(true)
                negativeButton(text = getString(R.string.cancel))
            }
        }
    }

    private fun commit(content: String) {
        btn_confirm.isClickable = false
        ArouterServiceManager.groupService.pushNotice(
            lifecycle(),
            mGroupId,
            switch_button.isChecked,
            WordFilter.doFilter(content),
            {
                EventBus.publishEvent(GroupInfoChangeEvent(mGroupId))
                toast(getString(R.string.release_success))

                //通知所有成员
                ArouterServiceManager.messageService.sendGroupNoticeMessageToGroup(
                    it,
                    WordFilter.doFilter(content),
                    switch_button.isChecked,
                    mUid,
                    mGroupId
                )

                val intent = Intent()
                intent.putExtra("newNoticeId", it)
                setResult(Activity.RESULT_OK, intent)
                finish()
            },
            {
                if (it is HttpException) {
                    toast(it.errMsg)
                    btn_confirm.isClickable = true
                } else {
                    btn_confirm.isClickable = true
                    toast(getString(R.string.post_failure))
                }
            })
    }

    class MyTextWatcher(val et: EditText, val tv: TextView, val btn: Button) : TextWatcher {

        private val charMaxNum = 500 // 允许输入的字数
        private var temp: CharSequence? = null // 监听前的文本
        private var editStart: Int = 0 // 光标开始位置
        private var editEnd: Int = 0 // 光标结束位置

        override fun afterTextChanged(s: Editable?) {
            editStart = et.selectionStart
            editEnd = et.selectionEnd
            tv.text = "${temp?.length}/${charMaxNum}" //把输入temp中记录的字符个数显示在TextView上
            temp?.let {
                if (it.length > charMaxNum) {//限制输入
                    s?.delete(editStart - 1, editEnd)
                    val tempSelection = editStart
                    et.text = s
                    et.setSelection(tempSelection)
                }
                if (it.isEmpty()) {
                    btn.isEnabled = false
                    btn.setBackgroundResource(R.drawable.common_corners_trans_d4d6d9_6_0)
                } else {
                    btn.isEnabled = true
                    btn.setBackgroundResource(R.drawable.bus_corners_178aff_trans_6_0)
                }
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            temp = s//temp = s 用于记录当前正在输入文本的个数
        }
    }
}
