package framework.telegram.business.ui.me

import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.sp.CommonPref
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.PrivacyContract
import framework.telegram.business.ui.me.presenter.PrivacyPresenterImpl
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.BitUtils
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.Helper
import framework.telegram.support.tools.file.DirManager
import framework.telegram.ui.dialog.AppDialog
import kotlinx.android.synthetic.main.bus_me_activity_chat_setting.*
import kotlinx.android.synthetic.main.bus_me_activity_item_edit.custom_toolbar


/**
 * Created by lzh on 19-6-7.
 * INFO:聊天设置
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_INFO_CHAT)
class ChatInfoActivity : BaseBusinessActivity<PrivacyContract.Presenter>(), PrivacyContract.View {


    private val mAccountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    override fun getLayoutId() = R.layout.bus_me_activity_chat_setting

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.string_setting_chat))

        val useSpeaker = SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()).getVoiceDefaultUseSpeaker()
        switch_button_1.setData(getString(R.string.use_the_handset_to_play_voice_messages), !useSpeaker) {
            SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()).putVoiceDefaultUseSpeaker(!it)
        }

        val privacy = mAccountInfo.getPrivacy()
        switch_button_2.setData(getString(R.string.string_center_send_msg), !BitUtils.checkBitValue(Helper.int2Bytes(privacy)[1], 3)) {
            mPresenter?.savePerfectInfo(1, 3, !it)
        }

        me_item_view_3.setData(getString(R.string.string_emoji_manage), "") {
            ARouter.getInstance().build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_DYNAMIC_FACE_MANAGER).navigation()
        }

        me_item_view_4.setData(getString(R.string.string_group_sender), "") {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE).withInt("operate", 4).navigation()
        }

        me_item_view_5.setDataNonePoint(getString(R.string.file_storage_location), "") {}
        me_item_view_5.setDataBottomValue(DirManager.getDownloadFileDir(BaseApp.app, AccountManager.getLoginAccountUUid()).absolutePath)
        me_item_view_5.setDataBottomValueVisible(true)

        me_item_view_6.setData(getString(R.string.clear_chat_records), "") {
            AppDialog.show(this@ChatInfoActivity, this@ChatInfoActivity) {
                positiveButton(text = getString(R.string.confirm), click = {
                    ArouterServiceManager.messageService.deleteAllChat()
                })
                message(text = getString(R.string.please_operate_with_caution))
                negativeButton(text = getString(R.string.cancel))
                title(text = getString(R.string.clear_chat_records))
            }
        }
    }

    override fun initListen() {
    }

    override fun initData() {
        PrivacyPresenterImpl(this, this, lifecycle()).start()
    }

    override fun showLoading() {
    }


    override fun showErrMsg(str: String?) {
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as PrivacyPresenterImpl
    }

    override fun isActive() = true

    override fun savePerfectInfoSuccess(index: Int, pos: Int, value: Boolean) {

    }
}