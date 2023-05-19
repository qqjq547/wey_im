package framework.telegram.business.ui.me

import android.content.Intent
import android.text.TextUtils
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.meituan.android.walle.WalleChannelReader
import framework.telegram.business.BuildConfig
import framework.telegram.business.R
import framework.telegram.business.UpdatePresenterImpl
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.sp.CommonPref
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.ChannelInfoBean
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.gson.GsonInstanceCreater
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.system.log.core.utils.SysUtils
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import kotlinx.android.synthetic.main.bus_me_activity_about.*
import java.io.IOException

/**
 * Created by lzh on 19-6-7.
 * INFO:关于
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_ME_ABOUT)
class AboutActivity : BaseBusinessActivity<BasePresenter>() {
    override fun getLayoutId() = R.layout.bus_me_activity_about

    private var mClickCount = 0

    private var mLastClickTime = 0L

    override fun initView() {

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.about))

        app_text_view_version.text = "v${SysUtils.getAppVersionName(this)}.${BuildConfig.versionDateTime}"
        app_text_view_version.setOnClickListener {
            if (System.currentTimeMillis() - mLastClickTime > 1500) {
                mClickCount = 0
            } else {
                mClickCount++
            }

            mLastClickTime = System.currentTimeMillis()

            if (mClickCount >= 5) {
                mClickCount = 0
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_SYSTEM_RLOG).navigation()
            }
        }

        if (BuildConfig.DEBUG) {
            val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)

            val sp = SharePreferencesStorage.createStorageInstance(CommonPref::class.java)
            app_text_view_host.text = "biz->${sp.getBizUrl()}"

            if (accountInfo.getUseWebSocket()) {
                app_text_view_session.text = "${accountInfo.getWebSocketAddress()}"
            } else {
                app_text_view_session.text = "tcp://${accountInfo.getSocketIp()}:${accountInfo.getSocketPort()}"
            }
        }


        me_item_view_4.setData(getString(R.string.version_updating), "") {
            UpdatePresenterImpl(this, this, lifecycle()).start(showCanUpDialog = true, showNotUpdateDialog = true)
        }

        me_item_view_7.setData(getString(R.string.Share_68), "") {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            val shareHost = getString(R.string.invite_to_join_68) + if (!TextUtils.isEmpty(Constant.Common.DOWNLOAD_HTTP_HOST)) {
                Constant.Common.DOWNLOAD_HTTP_HOST
            } else {
                "https://www.bufa.chat"
            }
            intent.putExtra(Intent.EXTRA_TEXT, shareHost)
            startActivity(Intent.createChooser(intent, shareHost))
        }
    }

    override fun initListen() {

    }

    override fun initData() {
        AppLogcat.logger.i(application.packageName, getChannelInfo())
    }

    private fun getChannelInfo(): String {
        try {
            val copyFileStr = WalleChannelReader.getChannel(BaseApp.app)
            if (!TextUtils.isEmpty(copyFileStr)) {
                val data = copyFileStr?.split(",".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
                if (data != null && data.size >= 3) {
                    val channelInfo = ChannelInfoBean()
                    channelInfo.channelName = data[0]
                    channelInfo.channelCode = Integer.valueOf(data[1])
                    channelInfo.isInstallPatch = TextUtils.isEmpty(data[2]) || "1" == data[2]
                    return GsonInstanceCreater.defaultGson.toJson(channelInfo)
                }
            }
            throw IOException()
        } catch (e: IOException) {
            e.printStackTrace()
            return getString(R.string.failed_to_read_channel_information)
        }
    }
}