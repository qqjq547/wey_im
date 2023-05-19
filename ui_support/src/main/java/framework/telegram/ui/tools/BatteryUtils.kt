package framework.telegram.ui.tools

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi


/**
 * Created by lzh on 18-4-3.
 */
object BatteryUtils {

    fun isIgnoringBatteryOptimizations(application: Application):Boolean  {
        var isIgnoring = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager =  application.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (powerManager != null) {
                isIgnoring = powerManager.isIgnoringBatteryOptimizations(application.packageName)
            }
        }
        return isIgnoring
    }
}