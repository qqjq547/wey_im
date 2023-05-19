package framework.telegram.business.ui.me

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.text.TextUtils
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.ideas.common.webview.WebUrlConfig
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.InfoContract
import framework.telegram.business.ui.me.presenter.InfoPresenterImpl
import framework.telegram.support.BaseApp
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.language.LocalManageUtil
import framework.telegram.ui.dialog.AppDialog
import kotlinx.android.synthetic.main.bus_me_activity_common.*
import kotlinx.android.synthetic.main.bus_me_activity_item_edit.custom_toolbar


/**
 * Created by lzh on 19-6-7.
 * INFO:通用
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_INFO_COMMON)
class CommonInfoActivity : BaseBusinessActivity<InfoContract.Presenter>(), InfoContract.View {

    override fun getLayoutId() = R.layout.bus_me_activity_common

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.common))

        item_font_size.setData(getString(R.string.font_size), "") {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_ME_FONT_SIZE).navigation()
        }

        var selectLanguage = "English"//此处字符串无需多语言化
        when (LocalManageUtil.getSelectLanguage()) {
            LocalManageUtil.FOLLOW_SYSTEM -> {
                selectLanguage = getString(R.string.language_follow_system)
            }
            LocalManageUtil.SIMPLIFIED_CHINESE -> {
                selectLanguage = "简体中文"
            }
            LocalManageUtil.TRADITIONAL_CHINESE -> {
                selectLanguage = "繁體中文"
            }
            LocalManageUtil.ENGLISH -> {
                selectLanguage = "English"
            }
            LocalManageUtil.THAI -> {
                selectLanguage = "ภาษาไทย"
            }
            LocalManageUtil.VI -> {
                selectLanguage = "Tiếng Việt"
            }
        }
        item_language.setData(getString(R.string.language), selectLanguage) {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_ME_CONFIG_LANGUAGE).navigation()
        }

        me_item_view_3.setData(getString(R.string.feedback), "") {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_ME_FEEDBACK).navigation()
        }


        val shareHost = if (!TextUtils.isEmpty(Constant.Common.DOWNLOAD_HTTP_HOST)) {
            Constant.Common.DOWNLOAD_HTTP_HOST
        } else {
            "https://www.bufa.chat"
        }
        item_68_table.setData(getString(R.string.string_68_table), shareHost) {
            val i = Intent(Intent.ACTION_VIEW)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            i.data = Uri.parse(if (!TextUtils.isEmpty(Constant.Common.DOWNLOAD_HTTP_HOST)) {
                Constant.Common.DOWNLOAD_HTTP_HOST
            } else {
                "https://www.bufa.chat"
            } + WebUrlConfig.explainUrl)
            startActivity(i)
        }

        item_use_instruction.setData(getString(R.string.string_user_instruction), "") {
            val i = Intent(Intent.ACTION_VIEW)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            i.data = Uri.parse(if (!TextUtils.isEmpty(Constant.Common.DOWNLOAD_HTTP_HOST)) {
                Constant.Common.DOWNLOAD_HTTP_HOST
            } else {
                "https://www.bufa.chat"
            } + WebUrlConfig.helpUlr)
            startActivity(i)
        }

        me_item_view_5.setData(getString(R.string.about), "") {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_ME_ABOUT).navigation()
        }

        me_item_view_6.setData(getString(R.string.clear_storage_title), "") {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CLEAR_STORAGE).navigation()
        }

        text_view_logout.setOnClickListener {
            //todo v1.8.0
//            SearchDbManager.delete(null,null)
            AppDialog.show(this@CommonInfoActivity, this@CommonInfoActivity) {
                positiveButton(text = getString(R.string.confirm), click = {
                    mPresenter?.logout()
                    Handler().postDelayed({
                        BaseApp.app.onUserLogout("")
                    }, 500)
                })
                message(text = getString(R.string.make_sure_to_log_out))
                negativeButton(text = getString(R.string.cancel))
                title(text = getString(R.string.log_out))
            }
        }
    }

    override fun initListen() {
    }

    override fun initData() {
        InfoPresenterImpl(this, this, lifecycle()).start()
    }

    override fun showLoading() {
    }

    override fun savePerfectInfoSuccess(str: String) {
        toast(getString(R.string.successfully_set))
    }

    override fun showErrMsg(str: String?) {
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as InfoPresenterImpl
    }

    override fun isActive() = true

}