package framework.telegram.business.bridge.service

import android.content.Context
import com.alibaba.android.arouter.facade.template.IProvider

interface IQrService : IProvider {

    fun resultHandler(url:String,context: Context?,loading :((Boolean)->Unit), complete :(()->Unit),error: (String) -> Unit)
}