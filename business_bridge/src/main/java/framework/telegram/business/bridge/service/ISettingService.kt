package framework.telegram.business.bridge.service

import com.alibaba.android.arouter.facade.template.IProvider

interface ISettingService : IProvider {

    fun setDefaultUseSpeaker(b:Boolean)
    fun getDefaultUseSpeaker(): Boolean

    fun getFontSize():Float

    /**
     * 是否可发出声音
     */
    fun getVoiceStatus(privacy:Int,isStream:Boolean):Boolean

    /**
     * 是否可震动
     */
    fun getVibrationStatus(privacy:Int,isStream:Boolean):Boolean

    fun setMuteStatus(status:Boolean)
}