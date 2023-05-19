package framework.telegram.business.services

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.service.ISettingService
import framework.telegram.business.sp.CommonPref
import framework.telegram.business.utils.MuteStatusUtil
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.storage.sp.SharePreferencesStorage

@Route(path = Constant.ARouter.ROUNTE_SERVICE_SETTING, name = "设置服务")
class SettingServiceImpl : ISettingService {
    override fun getFontSize(): Float {
        return SharePreferencesStorage.createStorageInstance(CommonPref::class.java).getFontSize()
    }

    override fun init(context: Context?) {

    }

    override fun getDefaultUseSpeaker(): Boolean {
        return SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()).getVoiceDefaultUseSpeaker()
    }

    override fun setDefaultUseSpeaker(b: Boolean) {
        return SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()).putVoiceDefaultUseSpeaker(b)
    }

    override fun getVoiceStatus(privacy: Int,isStream:Boolean): Boolean {
        return MuteStatusUtil.getVoiceStatus(privacy,isStream)
    }

    override fun getVibrationStatus(privacy: Int,isStream:Boolean): Boolean {
        return MuteStatusUtil.getVibrationStatus(privacy,isStream)
    }

    override fun setMuteStatus(status:Boolean){
        MuteStatusUtil.putWebOnLine(status)
    }
}