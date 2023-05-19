package framework.telegram.ui.tools

import android.os.Build



/**
 * 第三方ROM系统类别判断
 * Created by ftevxk on 18-4-3.
 */
object RomUtils {

    /**
     * 是否是华为系统
     *
     * @return
     */
    val isEmui: Boolean
        get() = Build.MANUFACTURER.toLowerCase().contains("Huawei".toLowerCase())

    /**
     * 是否是荣耀系统
     *
     * @return
     */
    val isHonor: Boolean
        get() = Build.MANUFACTURER.toLowerCase().contains("Honor".toLowerCase())

    /**
     * 是否是小米系统
     *
     * @return
     */
    val isMiui: Boolean
        get() = Build.MANUFACTURER.toLowerCase().contains("Xiaomi".toLowerCase())

    /**
     * 是否是Vivo系统
     *
     * @return
     */
    val isVivo: Boolean
        get() = Build.MANUFACTURER.toLowerCase().contains("vivo".toLowerCase())

    /**
     * 是否是Oppo系统
     *
     * @return
     */
    val isOppo: Boolean
        get() = Build.MANUFACTURER.toLowerCase().contains("OPPO".toLowerCase())

    /**
     * 是否是乐视系统
     *
     * @return
     */
    val isLeTv: Boolean
        get() = Build.MANUFACTURER.toLowerCase().contains("Letv".toLowerCase())

    /**
     * 是否是魅族系统
     *
     * @return
     */
    val isFlyme: Boolean
        get() = Build.MANUFACTURER.toLowerCase().contains("Meizu".toLowerCase())

    /**
     * 是否是三星手机
     *
     * @return
     */
    val isSamsung: Boolean
        get() = Build.MANUFACTURER.toLowerCase().contains("samsung".toLowerCase())

    val isLenovo: Boolean
        get() = Build.MANUFACTURER.toLowerCase().contains("LENOVO".toLowerCase())

    val isZTE: Boolean
        get() = Build.MANUFACTURER.toLowerCase().contains("ZTE".toLowerCase())

    val isGionee: Boolean
        get() = Build.MANUFACTURER.toLowerCase().contains("gionee".toLowerCase())

    val isKupai: Boolean
        get() = Build.MANUFACTURER.toLowerCase().contains("YuLong".toLowerCase())

    val isSony: Boolean
        get() = Build.MANUFACTURER.toLowerCase().contains("Sony".toLowerCase())

    val isLG: Boolean
        get() = Build.MANUFACTURER.toLowerCase().contains("LG".toLowerCase())

    val isHTC: Boolean
        get() = Build.MANUFACTURER.toLowerCase().contains("HTC".toLowerCase())
}