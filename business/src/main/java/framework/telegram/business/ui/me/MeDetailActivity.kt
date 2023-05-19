package framework.telegram.business.ui.me

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.CommonProto
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_ME_INFO_EDIT
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.event.UserInfoChangeEvent
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.MeInfoContract
import framework.telegram.business.ui.me.presenter.MeInfoPresenterImpl
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.menu.FloatMenu
import framework.telegram.ui.tools.Helper
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_group_activity_group_setting.*
import kotlinx.android.synthetic.main.bus_item_view.view.*
import kotlinx.android.synthetic.main.bus_me_activity_info.*
import kotlinx.android.synthetic.main.bus_me_activity_info.custom_toolbar

/**
 * Created by lzh on 19-6-7.
 * INFO: 账户详细信息
 * 1.   更换头像
 * 2.   更改昵称
 * 3.   更改个性签名
 * 4.   更改性别
 * 5.   二维码生成，扫描及保存
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_INFO)
class MeDetailActivity : BaseBusinessActivity<MeInfoContract.Presenter>(), MeInfoContract.View {

    override fun isActive() = true

    override fun getLayoutId() = R.layout.bus_me_activity_info

    private val mAccountInfo by lazy {
        AccountManager.getLoginAccount(AccountInfo::class.java)
    }

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.bus_me_profile))
    }

    @SuppressLint("CheckResult")
    override fun initListen() {
        EventBus.getFlowable(UserInfoChangeEvent::class.java)
            .bindToLifecycle(this@MeDetailActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                initData()
            }
    }

    override fun initData() {
        MeInfoPresenterImpl(this, this, this, lifecycle()).start()

        me_item_view_icon.setData(getString(R.string.bus_me_head), "", mAccountInfo.getAvatar()) {
            AppDialog.showBottomListView(
                this,
                this,
                mutableListOf(getString(R.string.bus_me_camera), getString(R.string.bus_me_galley))
            ) { _, index, _ ->
                when (index) {
                    0 -> {
                        mPresenter?.clickTakePhoto()
                    }
                    1 -> {
                        mPresenter?.clickPickPhoto()
                    }
                }
            }
        }

        me_item_view_nick_name.setData(
            getString(R.string.bus_me_nickname),
            mAccountInfo.getNickName()
        ) {
            ARouter.getInstance().build(ROUNTE_BUS_ME_INFO_EDIT).withInt("type", 0)
                .withString("content", mAccountInfo.getNickName()).navigation()
        }

        me_item_view_number.setData(
            getString(R.string.six_eight_number),
            mAccountInfo.getIdentify(),
            nonePoint = true
        ) {

        }

        me_item_view_number.setOnLongClickListener {
            showCopyDialog(me_item_view_number.app_text_view_value, mAccountInfo.getIdentify())
            return@setOnLongClickListener false

        }

        me_item_view_phone.setData(
            getString(R.string.bus_me_phone),
            mAccountInfo.getDisplayPhone()
        ) {

        }
        me_item_view_phone.setOnLongClickListener {
            val userPhone = me_item_view_phone.app_text_view_value.text.toString()?.replace("+", "")
                ?.replace(" ", "")
            showCopyDialog(me_item_view_phone.app_text_view_value, userPhone)
            return@setOnLongClickListener false
        }
        me_item_view_phone.app_text_view_value.setCompoundDrawables(null, null, null, null)

        me_item_view_mark.setData(
            getString(R.string.bus_me_signature),
            mAccountInfo.getSignature()
        ) {
            ARouter.getInstance().build(ROUNTE_BUS_ME_INFO_EDIT).withInt("type", 1)
                .withString("content", mAccountInfo.getSignature()).navigation()
        }

        me_item_view_sex.setData(
            getString(R.string.bus_me_sex),
            getSexText(mAccountInfo.getSex())
        ) {
            AppDialog.showBottomListView(
                this@MeDetailActivity,
                this@MeDetailActivity,
                listOf(
                    getString(R.string.bus_me_male),
                    getString(R.string.bus_me_female),
                    getString(R.string.bus_me_secrecy)
                )
            )
            { _, index, str ->
                when (index) {
                    0 -> {
                        mPresenter?.saveSexInfo(1, str, CommonProto.Gender.MALE)
                    }
                    1 -> {
                        mPresenter?.saveSexInfo(2, str, CommonProto.Gender.FEMALE)
                    }
                    2 -> {
                        mPresenter?.saveSexInfo(0, str, CommonProto.Gender.SECRECY)
                    }
                }
            }
        }

        me_item_view_qr.setData(
            getString(R.string.bus_me_my_qr_code),
            "",
            rid = R.drawable.bus_icon_qr
        ) {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_QR_SWEEP).navigation()
        }

        //暂时屏蔽此功能 2021-8-26
//        me_item_view_link.setData(getString(R.string.string_my_link), "", rid = R.drawable.bus_icon_link) {
//            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_COMMON_LINK).withInt("type",0)
//                    .withLong("targetId",mAccountInfo.getUserId()).navigation()
//        }
        me_item_view_link.visibility = View.GONE
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

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@MeDetailActivity, this@MeDetailActivity)
    }

    override fun showErrMsg(str: String?) {
        dialog?.dismiss()
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as MeInfoPresenterImpl
    }

    /**
     * 上传图片成功
     * 然后发送账户信息更新通知
     */
    override fun savePicInfoSuccess(str: String) {
        toast(getString(R.string.bus_me_update_change))
        dialog?.dismiss()
        val accountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }
        accountInfo.putAvatar(str)
        me_item_view_icon.setDataUrl(str)
        EventBus.publishEvent(UserInfoChangeEvent())
    }

    /**
     * 更新性别信息成功
     * 然后发送账户信息更新通知
     */
    override fun saveSexSuccess(info: Int, name: String) {
        toast(getString(R.string.bus_me_update_change))
        val accountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }
        accountInfo.putSex(info)
        me_item_view_sex.setDataValue(getSexText(mAccountInfo.getSex()))
        EventBus.publishEvent(UserInfoChangeEvent())
    }

    /**
     * 请求权限
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mPresenter?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * 监听跳转回来的Intent，然后分发跳转结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mPresenter?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter?.onDestroy()
    }

    /**
     * 转换服务器的性别序号并返回字符串
     * @param sex 性别序号
     */
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

}