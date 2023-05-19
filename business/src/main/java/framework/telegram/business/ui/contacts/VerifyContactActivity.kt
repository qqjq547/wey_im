package framework.telegram.business.ui.contacts

import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.im.domain.pb.ContactsProto
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_ADD_FRIEND_FROM
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_UID
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.ContactsHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.FriendHttpProtocol
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.utils.ScreenUtils
import kotlinx.android.synthetic.main.bus_me_activity_item_edit.*

/**
 * Created by lzh on 19-6-7.
 * INFO:
 * 填写验证信息
 *
 *
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_CONTACTS_VERIFY_INFO_EDIT)
class VerifyContactActivity : BaseBusinessActivity<BasePresenter>() {

    private val mType: ContactsProto.ContactsAddType? by lazy { intent.getSerializableExtra(KEY_ADD_FRIEND_FROM) as ContactsProto.ContactsAddType? }
    private val mTargetUid by lazy { intent.getLongExtra(KEY_TARGET_UID, 0) }

    private val mGroupId by lazy { intent.getLongExtra(Constant.ARouter_Key.KEY_TARGET_GID, 0) }

    private val mAddToken by lazy { intent.getStringExtra(Constant.ARouter_Key.KEY_ADD_TOKEN)?:"" }

    override fun initData() {
    }

    override fun getLayoutId() = R.layout.bus_me_activity_item_edit

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.add_validation))
        edit_text_context.layoutParams.height = ScreenUtils.dp2px(this, 120f)
        edit_text_context.setText(
            String.format(
                getString(R.string.i_am),
                AccountManager.getLoginAccount(AccountInfo::class.java).getNickName()
            )
        )
        edit_text_context.setSelection(edit_text_context.text!!.length)
        text_view_count.visibility = View.GONE
    }

    override fun initListen() {
        frame_layout_op.setOnClickListener {
            val str = edit_text_context.text.toString()
            if (!TextUtils.isEmpty(str) && mType != null) {
                addFriend(mTargetUid, mGroupId, str, mType!!, ContactsProto.ContactsOperator.ADD)
            }
        }

        edit_text_context.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    private fun addFriend(
        targetUid: Long,
        groupId: Long,
        msg: String,
        type: ContactsProto.ContactsAddType,
        op: ContactsProto.ContactsOperator
    ) {
        HttpManager.getStore(FriendHttpProtocol::class.java)
            .getContactsRelation(object : HttpReq<ContactsProto.ContactsRelationReq>() {
                override fun getData(): ContactsProto.ContactsRelationReq {
                    return ContactsHttpReqCreator.createAddRelation(
                        targetUid,
                        groupId,
                        msg,
                        type,
                        op,
                        mAddToken
                    )
                }
            })
            .getResult(lifecycle(), {
                addFriendAndFinish()
            }, {
                //请求失败
                showErrMsg(it.message)
            })
    }

    private fun addFriendAndFinish() {
        toast(getString(R.string.the_add_request_has_been_sent))
        finish()
    }

    fun showErrMsg(str: String?) {
        toast(str.toString())
    }
}