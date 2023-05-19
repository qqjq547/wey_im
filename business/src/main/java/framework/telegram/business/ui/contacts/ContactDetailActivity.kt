package framework.telegram.business.ui.contacts

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.alibaba.android.arouter.core.LogisticsCenter
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bigkoo.pickerview.view.OptionsPickerView
import com.im.domain.pb.ContactsProto
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_SAME_GROUP_CHAT
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_ADD_FRIEND_FROM
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_ADD_TOKEN
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_GID
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_PHONE
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_UID
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.contacts.presenter.ContactDetailContract
import framework.telegram.business.ui.contacts.presenter.ContactDetailPresenterImpl
import framework.telegram.business.ui.group.SameGroupChatsActivity.Companion.TARGET_ID
import framework.telegram.business.ui.widget.ViewUtils
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_FORWARD_CHATS
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO
import framework.telegram.message.bridge.event.OnlineStatusChangeEvent
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.MsgFireTimePickerUtil
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.menu.FloatMenu
import framework.telegram.ui.tools.Helper
import framework.telegram.ui.videoplayer.utils.NetworkUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_contacts_activity_friend_detail.*
import kotlinx.android.synthetic.main.bus_contacts_item_friend_cell.view.*
import kotlinx.android.synthetic.main.bus_contacts_item_friend_head1.*
import kotlinx.android.synthetic.main.bus_contacts_item_friend_head1.view.*
import kotlinx.android.synthetic.main.bus_contacts_item_friend_head2.*
import kotlinx.android.synthetic.main.bus_contacts_item_friend_head2.view.*


/**
 * Created by lzh on 19-5-27.
 * INFO:
 *
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_CONTACT_DETAIL)
class ContactDetailActivity : BaseBusinessActivity<ContactDetailContract.Presenter>(),
    ContactDetailContract.View {

    companion object {
        const val REQUEST_CODE_EDIT_CONTACT_NOTENAME = 0x1111
        const val REQUEST_CODE_EDIT_CONTACT_DESCRIBE = 0x1112
    }

    private val mMineUid by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
    }

    private val mTargetUid by lazy { intent.getLongExtra(KEY_TARGET_UID, 0) }

    private val mGroupId by lazy { intent.getLongExtra(KEY_TARGET_GID, 0) }

    private val mTargetPhone: String by lazy { intent.getStringExtra(KEY_TARGET_PHONE) ?: "" }

    //当这个开关打开，高级能互相添加，低级能添加高级，高级能添加低级，低级不能互相添加 (低级=普通成员) ；开关关了，就都可以添加
    private val mIsForbidJoin by lazy { intent.getBooleanExtra("isForbidJoinFriend", false) }

    private val mGroupNickName by lazy {
        intent.getStringExtra(Constant.ARouter_Key.KEY_GROUP_NICKNAME) ?: ""
    }

    private var mAddContactType: ContactsProto.ContactsAddType? = null

    private var mInfo: ContactDataModel? = null

    private var mBlackTextView: TextView? = null

    private var mIsMyBack = false

    private var mAddToken = ""

    private var mMoreOperater: ImageView? = null

    private var mOptionsPickerView: OptionsPickerView<String>? = null

    override fun getLayoutId() = R.layout.bus_contacts_activity_friend_detail

    override fun initView() {
        initToolsBar()

        mAddContactType =
            intent.getSerializableExtra(KEY_ADD_FRIEND_FROM) as ContactsProto.ContactsAddType?

        mAddToken = intent.getStringExtra(KEY_ADD_TOKEN) ?: ""
    }

    private fun initToolsBar() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        if (mMineUid != mTargetUid) {
            custom_toolbar.showRightImageView(R.drawable.common_icon_more, {
                if (mMoreOperater?.visibility != View.VISIBLE) {
                    return@showRightImageView
                }
                val data = mutableListOf<String>()
                if (!mIsMyBack) {
                    data.add(getString(R.string.add_to_blacklist))
                } else {
                    data.add(getString(R.string.remove_from_blacklist))
                }
                if (mInfo?.isBfMyContacts == true) {
                    data.add(getString(R.string.delete_contact))
                }
                AppDialog.showBottomListView(this, this, data) { _, index, _ ->
                    when (index) {
                        0 -> {
                            if (mIsMyBack) {
                                mPresenter?.setBlack(false)
                            } else {
                                AppDialog.show(
                                    this@ContactDetailActivity,
                                    this@ContactDetailActivity
                                ) {
                                    message(text = getString(R.string.add_to_blacklist_message))
                                    positiveButton(text = getString(R.string.confirm), click = {
                                        mPresenter?.setBlack(true)
                                    })
                                    negativeButton(text = getString(R.string.cancel)) {
                                        cancel()
                                    }
                                }
                            }
                        }
                        1 -> {
                            AppDialog.show(this@ContactDetailActivity, this@ContactDetailActivity) {
                                message(text = getString(R.string.string_remove_contact_confirm))
                                positiveButton(text = getString(R.string.confirm), click = {
                                    mPresenter?.deleteFriend(ContactsProto.ContactsOperator.DEL)
                                })
                                negativeButton(text = getString(R.string.cancel)) {
                                    cancel()
                                }
                            }
                        }
                    }
                }
            }) {
                mMoreOperater = it
                mMoreOperater?.visibility = View.GONE
            }
        }

        custom_toolbar.showRightTextView(getString(R.string.the_user_has_been_blocked), index = 0) {
            mBlackTextView = it
            it.setTextColor(getSimpleColor(R.color.f50d2e))
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            mBlackTextView?.visibility = View.GONE
        }
    }

    @SuppressLint("CheckResult")
    override fun initListen() {
        EventBus.getFlowable(OnlineStatusChangeEvent::class.java)
            .bindToLifecycle(this@ContactDetailActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.uid == mTargetUid) {
                    mPresenter?.getUpdateDetailInfo()
                }
            }

        friend_item_mark.setListen {
            if (mInfo != null) {
                //修改备注名
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACTS_INFO_EDIT)
                    .withString("title", getString(R.string.remark_name))
                    .withString("defaultContent", mInfo?.noteName)
                    .withInt("type", 0)
                    .withLong("uid", mTargetUid)
                    .navigation(this@ContactDetailActivity, REQUEST_CODE_EDIT_CONTACT_NOTENAME)
            }
        }

        friend_item_describe.setListen {
            if (mInfo != null) {
                //修改描述
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACTS_INFO_EDIT)
                    .withString("title", getString(R.string.describe))
                    .withString("defaultContent", mInfo?.depict)
                    .withInt("type", 1)
                    .withLong("uid", mTargetUid)
                    .navigation(this@ContactDetailActivity, REQUEST_CODE_EDIT_CONTACT_DESCRIBE)
            }
        }

        friend_item_share.setListen {
            if (mInfo != null) {
                ARouter.getInstance().build(ROUNTE_MSG_FORWARD_CHATS)
                    .withLong("targetId", mTargetUid)
                    .navigation()
            }
        }

        friend_item_same_group_chat.setListen {
            mInfo?.let {
                ARouter.getInstance().build(ROUNTE_BUS_SAME_GROUP_CHAT)
                    .withLong(TARGET_ID, mTargetUid)
                    .navigation()
            }
        }

        frame_layout_add.setOnClickListener {
            mInfo?.let {
                if (it.isBfVerify) {
                    // 编辑验证信息  mGroupId
                    ARouter.getInstance()
                        .build(Constant.ARouter.ROUNTE_BUS_CONTACTS_VERIFY_INFO_EDIT)
                        .withSerializable(
                            KEY_ADD_FRIEND_FROM, mAddContactType
                                ?: ContactsProto.ContactsAddType.REQ_MSG
                        )
                        .withLong(KEY_TARGET_GID, mGroupId)
                        .withString(KEY_ADD_TOKEN, mAddToken)
                        .withLong(KEY_TARGET_UID, mTargetUid).navigation()
                } else {
                    // 直接添加好友
                    mPresenter?.addFriend(
                        "",
                        mGroupId,
                        mAddContactType
                            ?: ContactsProto.ContactsAddType.REQ_MSG,
                        ContactsProto.ContactsOperator.ADD,
                        mAddToken
                    )
                }
            }
        }

        frame_layout_send.setOnClickListener {
            mInfo?.let {
                // 发送消息
                ARouter.getInstance()
                    .build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY)
                    .withLong("targetUid", it.uid).navigation()
            }
        }

        app_text_view_user_video.setOnClickListener {
            // 拨打视频电话
            if (NetworkUtils.isAvailable(BaseApp.app) && ArouterServiceManager.messageService.socketIsLogin()) {
                ARouter.getInstance().build(ROUNTE_MSG_STREAM_CALL_GO)
                    .withLong("targetUid", mTargetUid).withInt("streamType", 1).navigation()
            } else {
                toast(getString(R.string.socket_is_error))
            }
        }

        app_text_view_user_call.setOnClickListener {
            // 拨打语音电话
            if (NetworkUtils.isAvailable(BaseApp.app) && ArouterServiceManager.messageService.socketIsLogin()) {
                ARouter.getInstance().build(ROUNTE_MSG_STREAM_CALL_GO)
                    .withLong("targetUid", mTargetUid).withInt("streamType", 0).navigation()
            } else {
                toast(getString(R.string.socket_is_error))
            }
        }

        app_text_view_user_star.setOnClickListener {
            if (mInfo != null) {
                // 设置为星标好友
                mPresenter?.setStarFriend(mInfo?.isBfStar == false)
            }
        }

        friend_item_message.setData(
            getString(R.string.message_do_not_disturb),
            mInfo?.isBfDisturb
        ) {
            // 设置为消息免打扰
            mPresenter?.setDisturb(mInfo?.isBfDisturb == false)
        }
    }

    @SuppressLint("CheckResult")
    override fun initData() {
        if (this.mTargetUid <= 0L) {
            finish()
            return
        }

        ContactDetailPresenterImpl(mTargetUid, mGroupId, this, this, lifecycle()).start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_EDIT_CONTACT_NOTENAME && resultCode == Activity.RESULT_OK) {
            val text = data?.getStringExtra("text")
            mPresenter?.setNoteName(mInfo!!.nickName, text!!)
        } else if (requestCode == REQUEST_CODE_EDIT_CONTACT_DESCRIBE && resultCode == Activity.RESULT_OK) {
            val text = data?.getStringExtra("text")
            mPresenter?.setDescribe(text!!)
        }
    }

    override fun showUserInfo(info: ContactDataModel) {
        normal_background_progress.visibility = View.GONE
        normal_background_progress.clearAnimation()
        mInfo = info
        refreshUI(info)
    }

    override fun showBlackInfo(bfMyBlack: Boolean) {
        mIsMyBack = bfMyBlack
        //是否拉黑
        if (mInfo?.isBfMyContacts == true && mInfo?.uid ?: 0 != mMineUid) {
            when {
                mInfo?.isBfCancel == true -> {
                    mBlackTextView?.text = getString(R.string.other_account_unregistration1)
                    mBlackTextView?.visibility = View.VISIBLE
                }
                bfMyBlack -> {
                    mBlackTextView?.text = getString(R.string.the_user_has_been_blocked)
                    mBlackTextView?.visibility = View.VISIBLE
                }
                else -> mBlackTextView?.visibility = View.GONE
            }
        } else {
            if (bfMyBlack) {
                relative_layout_friend_head2.app_text_view_user_phone2.setTextColor(getSimpleColor(R.color.f50d2e))
                relative_layout_friend_head2.app_text_view_user_phone2.text =
                    getString(R.string.the_user_has_been_blocked)
            } else {
                if (TextUtils.isEmpty(mTargetPhone))
                    relative_layout_friend_head2.app_text_view_user_phone2.visibility = View.GONE
                else {
                    relative_layout_friend_head2.app_text_view_user_phone2.setTextColor(
                        getSimpleColor(R.color.a2a4a7)
                    )
                    relative_layout_friend_head2.app_text_view_user_phone2.text = mTargetPhone
                }
            }
        }
    }

    override fun showNoteName(noteName: String) {
        mInfo?.noteName = noteName
        val nikeName = mInfo?.nickName ?: ""
        friend_item_mark.setData(
            getString(R.string.remark_name),
            noteName,
            R.drawable.common_icon_friend_arrow
        )
        relative_layout_friend_head.app_text_view_username.text = mInfo?.displayName
        relative_layout_friend_head.app_text_view_user_mark.text =
            if (TextUtils.isEmpty(noteName)) "" else nikeName
    }

    override fun showDescribe(describe: String) {
        mInfo?.depict = describe
        friend_item_describe.setData(
            getString(R.string.describe),
            describe,
            R.drawable.common_icon_friend_arrow
        )
    }

    override fun showDisturb(disturb: Boolean) {
        mInfo?.isBfDisturb = disturb
        friend_item_message.setData(disturb)
    }

    override fun showStarFriend(bfStar: Boolean) {
        // 星标
        mInfo?.isBfStar = bfStar
        val draw =
            getSimpleDrawable(if (bfStar) R.drawable.bus_contacts_icon_friend_star else R.drawable.bus_contacts_icon_friend_star_unselect)
        app_text_view_user_star.setCompoundDrawablesWithIntrinsicBounds(null, draw, null, null)
    }

    private fun refreshUI(info: ContactDataModel) {
        mInfo = info
        if (mInfo?.isBfMyContacts == true && mInfo?.deleteMe == false && mInfo?.uid ?: 0 != mMineUid) {
            // 是我的联系人，且不是我自己
            // 联系人(包含黑名单用户)
            relative_layout_friend_head.app_image_view_icon.setImageURI(mInfo?.icon)
            relative_layout_friend_head.app_image_view_icon.setOnClickListener {
                ARouter.getInstance()
                    .build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_AVATAR_PREVIEW)
                    .withString("imageUrl", mInfo?.icon)
                    .navigation()
            }
            relative_layout_friend_head.app_text_view_username.text = mInfo?.nickName
            showNoteName(info.noteName ?: "")
            showDescribe(info.depict ?: "")
            showDisturb(info.isBfDisturb)

            friend_item_share.setData(getString(R.string.share_contacts), "")

            friend_item_same_group_chat.setData(
                getString(R.string.bus_group_same_chat),
                "" + mInfo?.commonGroupNum,
                R.drawable.common_icon_friend_arrow
            )

            friend_item_mark.visibility = View.VISIBLE
            friend_item_describe.visibility = View.VISIBLE
            friend_item_message.visibility = View.VISIBLE
            friend_item_same_group_chat.visibility = View.VISIBLE
            friend_item_share.visibility = View.VISIBLE

            // 星标
            showStarFriend(mInfo?.isBfStar ?: false)

            relative_layout_friend_head.visibility = View.VISIBLE
            relative_layout_friend_head2.visibility = View.GONE
            frame_layout_add.visibility = View.GONE
            mMoreOperater?.visibility = View.VISIBLE

            custom_toolbar.setToolbarColor(R.color.white)
            line.visibility = View.GONE

            //判断是否显示手机号
            friend_item_phone.visibility = View.VISIBLE
            val userPhone = mInfo?.phone
            if (!TextUtils.isEmpty(userPhone)) {
                friend_item_phone.app_text_view_value.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.c178aff
                    )
                )
                friend_item_phone.setData(getString(R.string.phone_number_two), userPhone!!)
                friend_item_phone.setListen {
                    showPhoneDialog(userPhone)
                }
                friend_item_phone.setOnLongClickListener {
                    val str = userPhone?.replace("+", "")?.replace(" ", "")
                    showCopyDialog(friend_item_phone, str)
                    return@setOnLongClickListener false
                }
                friend_item_phone.app_text_view_value.setOnClickListener {
                    showPhoneDialog(userPhone)
                }
                friend_item_phone.app_text_view_value.setOnLongClickListener {
                    val str = userPhone?.replace("+", "")?.replace(" ", "")
                    showCopyDialog(friend_item_phone.app_text_view_value, str)
                    return@setOnLongClickListener false
                }
            } else {
                friend_item_phone.app_text_view_value.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.a2a4a7
                    )
                )
                friend_item_phone.setData(
                    getString(R.string.phone_number_two),
                    getString(R.string.contacts_do_not_display_phones)
                )
                friend_item_phone.setListen {

                }
                friend_item_phone.setOnLongClickListener {
                    return@setOnLongClickListener false
                }
                friend_item_phone.app_text_view_value.setOnClickListener {
                }
                friend_item_phone.app_text_view_value.setOnLongClickListener {
                    return@setOnLongClickListener false
                }
            }
        } else {
            // 非联系人
            relative_layout_friend_head2.app_image_view_icon2.setImageURI(mInfo?.icon)
            relative_layout_friend_head2.app_image_view_icon2.setOnClickListener {
                ARouter.getInstance()
                    .build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_AVATAR_PREVIEW)
                    .withString("imageUrl", mInfo?.icon)
                    .navigation()
            }
            relative_layout_friend_head2.app_text_view_username2.text = mInfo?.nickName
            if (TextUtils.isEmpty(mTargetPhone))
                relative_layout_friend_head2.app_text_view_user_phone2.visibility = View.GONE
            else
                relative_layout_friend_head2.app_text_view_user_phone2.text = mTargetPhone

            friend_item_mark.visibility = View.GONE
            friend_item_describe.visibility = View.GONE
            friend_item_message.visibility = View.GONE
            friend_item_same_group_chat.visibility = View.GONE
            friend_item_share.visibility = View.GONE

            custom_toolbar.setToolbarColor(R.color.f8fafd)
            line.visibility = View.VISIBLE

            relative_layout_friend_head.visibility = View.GONE
            relative_layout_friend_head2.visibility = View.VISIBLE
            if (mInfo?.uid ?: 0 != mMineUid && !mIsForbidJoin && !TextUtils.isEmpty(mAddToken)) {
                frame_layout_add.visibility = View.VISIBLE
                mMoreOperater?.visibility = View.VISIBLE

            } else {
                frame_layout_add.visibility = View.GONE
                mMoreOperater?.visibility = View.GONE
            }
        }

        mIsMyBack = mInfo?.isBfMyBlack ?: false
        //是否拉黑
        showBlackInfo(mIsMyBack)

        //在线状态
        if (mInfo?.isShowLastOnlineTime == true && mInfo?.isOnlineStatus == true) {
            text_view_online_status_point.visibility = View.VISIBLE
        } else {
            text_view_online_status_point.visibility = View.GONE
        }

        mInfo?.let {
            ViewUtils.showOnlineStatus(
                ArouterServiceManager.messageService,
                it.uid,
                it.isShowLastOnlineTime,
                it.isOnlineStatus,
                it.lastOnlineTime,
                text_view_user_online_status
            )
        }

        //如果是从群成员列表过来的，显示群昵称
        if (!TextUtils.isEmpty(mGroupNickName)) {
            friend_item_group_nickname.visibility = View.VISIBLE
            friend_item_group_nickname.setData(
                getString(R.string.group_of_nicknames),
                mGroupNickName
            )
            view_1.visibility = View.VISIBLE
        } else {
            friend_item_group_nickname.visibility = View.GONE
        }


        friend_item_sex.setData(getString(R.string.gender), getSexText(mInfo?.sex ?: 0))
        friend_item_name.setData(
            getString(R.string.personalized_signature),
            if (TextUtils.isEmpty(mInfo?.signature)) getString(R.string.they_didn_t_write_anything) else mInfo?.signature!!
        )

        friend_item_sex.visibility = View.VISIBLE
        friend_item_name.visibility = View.VISIBLE

        if (!TextUtils.isEmpty(mInfo?.identify) && mInfo?.identify != "--") {
            friend_item_number.visibility = View.VISIBLE
            friend_item_number.setData(getString(R.string.six_eight_number), mInfo?.identify ?: "")
            friend_item_number.setOnLongClickListener {
                showCopyDialog(friend_item_number.app_text_view_value, mInfo?.identify ?: "")
                return@setOnLongClickListener false
            }
        } else {
            friend_item_number.visibility = View.GONE
        }
    }

    private fun showPhoneDialog(str: String?) {
        val userPhone = str?.replace("+", "")?.replace(" ", "")
        val list = ArrayList<String>()
        list.add(getString(R.string.copy_number))
        list.add(getString(R.string.making_a_call))
        AppDialog.showBottomListView(
            this@ContactDetailActivity,
            this@ContactDetailActivity,
            list
        ) { _, index, _ ->
            when (index) {
                0 -> {
                    Helper.setPrimaryClip(BaseApp.app, userPhone)
                    toast(getString(R.string.copy_success))
                }
                1 -> {
                    AppDialog.show(this@ContactDetailActivity, this@ContactDetailActivity) {
                        positiveButton(text = getString(R.string.call_out), click = {
                            //跳转拨打电话界面
                            val intent = Intent()
                            intent.action = Intent.ACTION_DIAL//设置活动类型
                            intent.data = Uri.parse("tel:$userPhone")//设置数据
                            startActivity(intent)//开启意图
                        })
                        message(text = str)

                        cancelOnTouchOutside(true)
                        cancelable(true)
                        negativeButton(text = getString(R.string.cancel))
                    }
                }
            }
        }
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

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as ContactDetailContract.Presenter
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun showErrorMsg(errStr: String?) {
        normal_background_progress.visibility = View.GONE
        normal_background_progress.clearAnimation()
        if (!TextUtils.isEmpty(errStr)) {
            toast(errStr!!)
        }
    }

    override fun showAddFriendMsg() {
        toast(getString(R.string.request_to_add_a_friend_has_been_sent))
    }

    private fun getSexText(sex: Int): String {
        return when (sex) {
            1 -> {
                getString(R.string.man)
            }
            2 -> {
                getString(R.string.woman)
            }
            else -> {
                getString(R.string.confidentiality)
            }
        }
    }

    override fun showLoading() {
        normal_background_progress.visibility = View.VISIBLE
    }

    override fun deleteFriend() {
        val postcard = ARouter.getInstance().build("/app/act/main")
        LogisticsCenter.completion(postcard)
        val tClass = postcard.destination
        ActivitiesHelper.getInstance().closeToTarget(tClass)
    }

    override fun showBanWord(banTime: Int) {     //禁言
        if (banTime != 0) {
            friend_item_group_ban_work.setData(
                getString(R.string.string_ban_word),
                MsgFireTimePickerUtil.timeValueBanTimeName(banTime),
                R.drawable.common_icon_friend_arrow
            )
            friend_item_group_ban_work.setListen {
                if (banTime == -1) {
                    if (mOptionsPickerView == null) {
                        mOptionsPickerView = MsgFireTimePickerUtil.showSelectBanTimePicker(
                            this,
                            banTime
                        ) { _, timeValue ->
                            mPresenter?.setBanWord(timeValue)
                        }
                    }
                    mOptionsPickerView?.show()
                } else {
                    AppDialog.showBottomListView(
                        this,
                        this,
                        mutableListOf(getString(R.string.string_relieve_forbidden))
                    ) { _, index, _ ->
                        when (index) {
                            0 -> {
                                mPresenter?.setBanWord(-1)
                            }
                        }
                    }
                }
            }
            friend_item_group_ban_work.visibility = View.VISIBLE
            view_1.visibility = View.VISIBLE
        } else {
            friend_item_group_ban_work.visibility = View.GONE
        }
    }

    override fun setAddToken(addToken: String) {
        if (!TextUtils.isEmpty(addToken)) {
            mAddToken = addToken
        }
    }
}