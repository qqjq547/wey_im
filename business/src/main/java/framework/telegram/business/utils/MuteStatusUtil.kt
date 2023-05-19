package framework.telegram.business.utils

import framework.telegram.business.sp.CommonPref
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.BitUtils
import framework.telegram.support.tools.Helper

/**
 * Created by yanggl on 2019/10/25 16:39
 */
object MuteStatusUtil {
    private var mWebOnLine = SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()).getWebMute(false)

    fun putWebOnLine(value: Boolean){
        mWebOnLine=value
        SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid()).putWebMute(value)
    }

    fun getWebOnLine():Boolean{
        return mWebOnLine
    }

    /**
     * 是否可震动
     */
    fun getVibrationStatus(privacy:Int,isStream:Boolean):Boolean{
        if (mWebOnLine&& !isStream){
            if (BitUtils.checkBitValue(Helper.int2Bytes(privacy)[1], 0) && BitUtils.checkBitValue(Helper.int2Bytes(privacy)[3], 4)){
                return true
            }
        }else{
            if (BitUtils.checkBitValue(Helper.int2Bytes(privacy)[3], 4)){
                return true
            }
        }
        return false
    }

    /**
     * 是否可发出声音
     */
    fun getVoiceStatus(privacy:Int,isStream:Boolean):Boolean{
         if (mWebOnLine && !isStream){
            if (BitUtils.checkBitValue(Helper.int2Bytes(privacy)[1], 0) && !BitUtils.checkBitValue(Helper.int2Bytes(privacy)[3], 3)){
                return true
            }
        }else{
            if (!BitUtils.checkBitValue(Helper.int2Bytes(privacy)[3], 3)){
                return true
            }
        }
        return false
    }

    /**
     * 是否可以展示推送
     * Helper.int2Bytes(privacy)[1], 0) 静音
     */
    fun isNotification(privacy:Int,isStream:Boolean):Boolean{
        if (isStream ){
            return true
        } else if (mWebOnLine && !BitUtils.checkBitValue(Helper.int2Bytes(privacy)[1], 0)){
            return false
        }
        return true
    }
}