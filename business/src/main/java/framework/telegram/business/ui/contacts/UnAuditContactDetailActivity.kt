package framework.telegram.business.ui.contacts

import android.graphics.Color
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.ContactsProto
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_UID
import framework.telegram.business.R
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.contacts.presenter.UnAuditContactContract
import framework.telegram.business.ui.contacts.presenter.UnAuditContactPresenterImpl
import framework.telegram.support.BaseApp
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.menu.FloatMenu
import framework.telegram.ui.tools.Helper
import kotlinx.android.synthetic.main.bus_contacts_activity_friend_detail.*
import kotlinx.android.synthetic.main.bus_contacts_item_friend_cell.view.*
import kotlinx.android.synthetic.main.bus_contacts_item_friend_head3.*

/**
 * Created by lzh on 19-5-27.
 * INFO:
 *
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_CONTACTS_UN_AUDIT_FRIEND_DETAIL)
class UnAuditContactDetailActivity : BaseBusinessActivity<UnAuditContactContract.Presenter>(), UnAuditContactContract.View {

    private val mTargetUid by lazy { intent.getLongExtra(KEY_TARGET_UID, 0) }

    private var mInfo: ContactsProto.ContactsRecordBase? = null

    private var mIsSetBlack = false

    override fun getLayoutId() = R.layout.bus_contacts_activity_un_friend_detail

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        custom_toolbar.showRightImageView(R.drawable.common_icon_more, {
            val list = mutableListOf<String>()
            if (!mIsSetBlack) {
                list.add(getString(R.string.add_to_blacklist))
            } else {
                list.add(getString(R.string.remove_from_blacklist))
            }
            AppDialog.showBottomListView(this, this, list
            ) { _, index, _ ->
                when (index) {
                    0 -> {
                        mPresenter?.setBlack(mTargetUid, !mIsSetBlack)
                    }
                }
            }
        })

    }

    override fun initListen() {
        frame_layout_add.setOnClickListener {
            mPresenter?.makeFriend(mInfo?.userInfo?.uid
                    ?: 0, ContactsProto.ContactsOperator.ADD_REQ)
            it.isClickable = false
        }
    }

    override fun initData() {
        if (this.mTargetUid <= 0L) {
            finish()
            return
        }

        UnAuditContactPresenterImpl(this, this, lifecycle()).start()
        mPresenter?.getDataDetail(mTargetUid)
    }

    override fun showSetBlackUI(isBlack: Boolean) {
        if (isBlack) {
            app_text_view_user_type.text = getString(R.string.the_user_has_been_blocked)
            app_text_view_user_type.setTextColor(Color.RED)
            mIsSetBlack = true
        } else {
            val fromType = when (mInfo?.type) {
                ContactsProto.ContactsAddType.PHONE -> getString(R.string.cell_phone_number)
                ContactsProto.ContactsAddType.CODE -> getString(R.string.scan_a_qr_code)
                ContactsProto.ContactsAddType.CROWD -> getString(R.string.group_chat)
                ContactsProto.ContactsAddType.CARD -> getString(R.string.business_card)
                ContactsProto.ContactsAddType.LINK -> getString(R.string.string_link)
                ContactsProto.ContactsAddType.IDENTIFY -> getString(R.string.six_eight_number)
                else -> ""
            }
            val from = String.format(getString(R.string.the_other_person_searches),fromType)
            app_text_view_user_type.text = from
            app_text_view_user_type.setTextColor(ContextCompat.getColor(BaseApp.app, R.color.a2a4a7))
            mIsSetBlack = false
        }
    }

    override fun showLoading() {
    }


    override fun showEmpty() {
    }

    override fun refreshUI(info: ContactsProto.ContactsRecordBase) {
        mInfo = info
        mIsSetBlack = mInfo?.bfMyBlack ?: false

        showSetBlackUI(mIsSetBlack)

        app_image_view_icon.setImageURI(info.userInfo.icon)
        app_image_view_icon.setOnClickListener {
            ARouter.getInstance().build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_AVATAR_PREVIEW)
                    .withString("imageUrl", info.userInfo.icon)
                    .navigation()
        }
        app_text_view_username.text = info.userInfo.nickName
        app_text_view_user_mark.text = info.msg

        friend_item_sex.setData(getString(R.string.gender), getSexText(mInfo?.userInfo?.gender?.number ?: 0))
        friend_item_mark.setData(getString(R.string.personalized_signature), if (TextUtils.isEmpty(mInfo?.signature)) getString(R.string.they_didn_t_write_anything) else mInfo?.signature!!)

        if (!TextUtils.isEmpty(info.userInfo.identify) && info.userInfo.identify!="--"){
            friend_item_number.setData(getString(R.string.six_eight_number), info.userInfo.identify)
            friend_item_number.setOnLongClickListener {
                showCopyDialog(friend_item_number.app_text_view_value, info.userInfo.identify)
                return@setOnLongClickListener false
            }
            friend_item_number.visibility = View.VISIBLE
        }else{
            friend_item_number.visibility = View.GONE
        }
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as UnAuditContactContract.Presenter
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun showError(errStr: String?) {
        toast(errStr?:"")
        frame_layout_add.isClickable = true
    }

    override fun destory() {
        finish()
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