package framework.telegram.business.ui.me

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.UserProto
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.BuildConfig
import framework.telegram.business.BusinessApplication
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_ME_INFO
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_ME_INFO_CHAT
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_ME_INFO_COMMON
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_ME_INFO_NOTICE
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_ME_INFO_PRIVACY
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_ME_INFO_SAVE
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.event.MeRedPointChangeEvent
import framework.telegram.business.bridge.event.UserInfoChangeEvent
import framework.telegram.business.sp.CommonPref
import framework.telegram.business.ui.base.BaseBusinessFragment
import framework.telegram.business.ui.me.presenter.MeDetailContract
import framework.telegram.business.ui.me.presenter.MeDetailPresenterImpl
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_LOCATION_CHOICE_ACTIVITY
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.ExpandClass.toast
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_me_fragment.*

/**
 * Created by lzh on 19-5-15.
 * INFO: 账户信息及app设置
 * 1.   账户信息更新
 * 2.   隐私设置
 * 3.   通知设置
 * 4.   安全设置
 * 5.   通用设置
 * 6.   账户信息更新时同步更新ui
 */
class MeFragment : BaseBusinessFragment<MeDetailContract.Presenter>(), MeDetailContract.View {
    /*
        账户信息
     */
    private var accountInfo: AccountInfo? = null
    private var mTopTextView: TextView? = null

    companion object {
        fun newInstance(): MeFragment = MeFragment()
    }

    override val fragmentName: String
        get() = "MeFragment"

    override fun getLayoutId() = R.layout.bus_me_fragment

    override fun initView() {
        accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
        custom_toolbar.showLeftTextView(accountInfo?.getNickName() ?: "") {
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            it.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            it.ellipsize = TextUtils.TruncateAt.END
            it.maxLines = 1
            mTopTextView = it
        }

        custom_toolbar.setToolbarSize(64f)

        if (BuildConfig.DEBUG) {
            text_view_debug_name.text =
                    "Número de telefone ：${accountInfo?.getPhone()} \n" +
                            "uid ：${accountInfo?.getUserId()} \n" +
                            "Apelido ：${accountInfo?.getNickName()} \n"
        }
        text_view_68_number.text = getString(R.string.six_eight_number) + ":" + accountInfo?.getIdentify()


        me_item_view_message.setData(getString(R.string.bus_me_message), R.drawable.bus_icon_message) {
            ARouter.getInstance().build(ROUNTE_BUS_ME_INFO_NOTICE).navigation()
        }

        me_item_view_chat.setData(getString(R.string.string_setting_chat)
                , R.drawable.bus_icon_chat) {
            ARouter.getInstance().build(ROUNTE_BUS_ME_INFO_CHAT).navigation()
        }

        me_item_view_secret.setData(getString(R.string.bus_me_secret)
                , R.drawable.bus_icon_secret) {
            ARouter.getInstance().build(ROUNTE_BUS_ME_INFO_PRIVACY).navigation()
        }

        me_item_view_safe.setData(getString(R.string.bus_me_safe),
                R.drawable.bus_icon_safe) {
            ARouter.getInstance().build(ROUNTE_BUS_ME_INFO_SAVE).navigation()
        }

        me_item_view_common.setData(getString(R.string.bus_me_common),
                R.drawable.bus_icon_common) {
            ARouter.getInstance().build(ROUNTE_BUS_ME_INFO_COMMON).navigation()
        }

        setMeRedPoint()

        MeDetailPresenterImpl(this, this.context!!, lifecycle()).start()
    }


    @SuppressLint("CheckResult")
    override fun initListen() {
        EventBus.getFlowable(UserInfoChangeEvent::class.java)
                .bindToLifecycle(this@MeFragment)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (activity != null) {
                        userInfoChange()
                    }
                }

        linear_layout_me.setOnClickListener {
            ARouter.getInstance().build(ROUNTE_BUS_ME_INFO).navigation()
        }
    }

    override fun onResume() {
        super.onResume()
        EventBus.publishEvent(MeRedPointChangeEvent())
        setMeRedPoint()
    }

    override fun initData() {
        accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
        if (accountInfo != null && accountInfo?.getAvatar() != null) {
            image_view_icon.setImageURI(accountInfo?.getAvatar())
            mTopTextView?.text = accountInfo?.getNickName()
        }
    }

    override fun showLoading() {

    }

    /**
     * 获取账户详细信息成功后，更新账户详情信息并进行是否显示红点的校验
     */
    override fun getDetailInfoSuccess(info: UserProto.DetailResp) {
        BusinessApplication.updateAccountInfo(info.bfPassword, info.signature, info.privacy, info.viewType, info.phone, info.clearTime.number,info.qrCode)
        EventBus.publishEvent(MeRedPointChangeEvent())
        setMeRedPoint()
    }

    override fun showErrMsg(str: String?) {
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as MeDetailPresenterImpl
    }

    override fun isLazyLoad(): Boolean = true

    override fun isActive() = true

    /**
     * @see UserInfoChangeEvent 账户信息变更事件
     * 监听到UserInfoChangeEvent事件触发后更新ui
     */
    private fun userInfoChange() {
        initData()
    }

    /**
     * 在对应的itemView中显示红点
     * 校验是否显示红点的规则为
     * 1.   通知栏权限是否开启
     * 2.   账户的密码是否已经设置
     */
    private fun setMeRedPoint() {
        if (me_item_view_message == null || me_item_view_safe == null)
            return
        val permission = SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()).getFirstOpenMessagePermission(false)
        me_item_view_message.setExtraIcon(if (permission) 0 else R.drawable.common_oval_f50d2e_8)
        val hasPassword = accountInfo?.getBfPassword() ?: false
        me_item_view_safe.setExtraIcon(if (hasPassword) 0 else R.drawable.common_oval_f50d2e_8)
    }
}
