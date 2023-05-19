package framework.telegram.business.ui.me

import android.content.res.Resources
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.sp.CommonPref
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.InfoContract
import framework.telegram.support.ActivityManager
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.ui.utils.ScreenUtils
import kotlinx.android.synthetic.main.bus_me_activity_config_language.*
import kotlinx.android.synthetic.main.bus_me_activity_config_language.custom_toolbar
import kotlinx.android.synthetic.main.bus_me_activity_font_size.*

@Route(path = Constant.ARouter.ROUNTE_BUS_ME_FONT_SIZE)
class FontSizeActivity : BaseBusinessActivity<InfoContract.Presenter>() {

    private val messageTextSize=16
    private val multiple1=14.toFloat()/messageTextSize.toFloat()
    private val multiple2=16.toFloat()/messageTextSize.toFloat()
    private val multiple3=18.toFloat()/messageTextSize.toFloat()
    private val multiple4=21.toFloat()/messageTextSize.toFloat()
    private val multiple5=24.toFloat()/messageTextSize.toFloat()

    private var mMagnificationTimes=0f//放大倍数

    override fun getLayoutId() = R.layout.bus_me_activity_font_size

    override fun initView() {
        if (Build.VERSION.SDK_INT >= 23) {
            status_bar.setBackgroundColor(ContextCompat.getColor(this@FontSizeActivity, framework.ideas.common.R.color.f8fafd))
        }else{
            status_bar.setBackgroundColor(ContextCompat.getColor(this@FontSizeActivity, framework.ideas.common.R.color.c27000000))
        }
        iv_back.setOnClickListener {
            finish()
        }
        tv_done.setOnClickListener {
            SharePreferencesStorage.createStorageInstance(CommonPref::class.java).putFontSize(mMagnificationTimes)
            finish()
        }
    }

    override fun initListen() {
        fsv_font_size.setChangeCallbackListener {
            when (it){
                0->{
                    mMagnificationTimes=multiple1
                }
                1->{
                    mMagnificationTimes=multiple2
                }
                2->{
                    mMagnificationTimes=multiple3
                }
                3->{
                    mMagnificationTimes=multiple4
                }
                4->{
                    mMagnificationTimes=multiple5
                }
            }

            //改变当前页面大小
            changeTextSize((mMagnificationTimes * 16).toInt())
        }

        val magnificationTimes = SharePreferencesStorage.createStorageInstance(CommonPref::class.java).getFontSize()
        when(magnificationTimes){
            multiple1 ->{
                fsv_font_size.setDefaultPosition(0)
            }
            multiple2 ->{
                fsv_font_size.setDefaultPosition(1)
            }
            multiple3 ->{
                fsv_font_size.setDefaultPosition(2)
            }
            multiple4 ->{
                fsv_font_size.setDefaultPosition(3)
            }
            multiple5 ->{
                fsv_font_size.setDefaultPosition(4)
            }
        }
    }

    override fun initData() {
    }

    /**
     * 改变textsize 大小
     */
    private fun changeTextSize(dimension: Int) {
        tv_font_size1.textSize = dimension.toFloat()
        tv_font_size2.textSize = dimension.toFloat()
        tv_font_size3.textSize = dimension.toFloat()
    }

    /**
     * 重新配置缩放系数
     * @return
     */
    override fun getResources(): Resources {
        val res = super.getResources()
        val config = res.configuration
        config.fontScale = 1f//1 设置正常字体大小的倍数
        res.updateConfiguration(config, res.displayMetrics)
        return res
    }
}
