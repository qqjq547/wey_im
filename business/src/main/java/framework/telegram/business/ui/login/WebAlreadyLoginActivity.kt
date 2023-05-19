package framework.telegram.business.ui.login

import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.LoginProto
import com.im.domain.pb.UserProto
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.LoginHttpReqCreator
import framework.telegram.business.http.creator.UserHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.UserHttpProtocol
import framework.telegram.support.BaseActivity
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.BitUtils
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.Helper
import framework.telegram.ui.dialog.AppDialog
import kotlinx.android.synthetic.main.bus_web_already_login_activity.*
import kotlinx.android.synthetic.main.bus_web_confirm_login_activity.custom_toolbar

/**
 * Created by yanggl on 2019/10/22 14:59
 */
@Route(path = Constant.ARouter.ROUNTE_MSG_WEB_ALREADY_LOGIN)
class WebAlreadyLoginActivity : BaseActivity() {

    private val mAccountInfo by lazy { AccountManager.getLoginAccount(AccountInfo::class.java) }

    private var mPrivacy= mAccountInfo.getPrivacy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bus_web_already_login_activity)

        custom_toolbar.setToolbarColor(R.color.white)
        custom_toolbar.setBackIcon(R.drawable.close_blue) {
            finish()
        }

        var isOpen = !BitUtils.checkBitValue(Helper.int2Bytes(mPrivacy)[1], 0)
        updateView(isOpen)
        ll_mute.setOnClickListener {
            ll_mute.isEnabled=false
            isOpen=!isOpen
            val finalPrivacy = BitUtils.setBitValue(mPrivacy,1,0,!isOpen)
            val userParam = UserProto.UserParam.newBuilder().setPrivacy(finalPrivacy).build()
            val opList = listOf(UserProto.UserOperator.PRIVACY)
            HttpManager.getStore(UserHttpProtocol::class.java)
                    .updateUserInfo(object : HttpReq<UserProto.UpdateReq>() {
                        override fun getData(): UserProto.UpdateReq {
                            return UserHttpReqCreator.createUserUpdateReq(opList, userParam)
                        }
                    })
                    .getResult(null, {
                        //请求成功
                        mPrivacy = finalPrivacy
                        mAccountInfo.putPrivacy(finalPrivacy)
                        updateView(isOpen)
                        ll_mute.isEnabled=true
                    }, {
                        //请求失败
                        updateView(!isOpen)
                        ll_mute.isEnabled=true
                    })

        }
        ll_file.setOnClickListener {
            ARouter.getInstance().build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY).withLong("targetUid", framework.telegram.message.bridge.Constant.Common.FILE_TRANSFER_UID).navigation()
        }

        btn_logout.setOnClickListener {
            AppDialog.show(this@WebAlreadyLoginActivity,this@WebAlreadyLoginActivity){
                positiveButton(text = getString(R.string.string_go_back), click = {
                    //退出桌面端
                    HttpManager.getStore(UserHttpProtocol::class.java)
                            .webLogout(object : HttpReq<LoginProto.WebLogoutReq>() {
                                override fun getData(): LoginProto.WebLogoutReq {
                                    return LoginHttpReqCreator.createWebLogoutReq()
                                }
                            })
                            .getResult(null, {
                                //请求成功
                                ArouterServiceManager.settingService.setMuteStatus(false)
                                finish()
                            }, {
                                //请求失败
                                toast(getString(R.string.logout_web_fail))
                            })
                })
                negativeButton(text = getString(R.string.cancel))
                title(text = getString(R.string.logout_web))
            }
        }
    }

    private fun updateView(isOpen:Boolean) {
        if (isOpen){
            iv_mute.setImageResource(R.drawable.web_mute_open)
        }else{
            iv_mute.setImageResource(R.drawable.web_mute)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.anim_down_out)
    }

    override fun isPortraitScreen():Boolean = false //这里不设置竖屏，因为windowIsTranslucent 的问题，这里其实还是竖屏
}