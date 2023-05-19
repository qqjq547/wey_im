package framework.telegram.app.activity.welcome

import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.app.Constant.ARouter.ROUNTE_APP_WELCOME
import framework.telegram.app.R
import framework.telegram.support.BaseActivity
import kotlinx.android.synthetic.main.app_welcome_activity.*


/**
 * Created by lzh on 19-5-22.
 * INFO: 欢迎页
 */
@Route(path = ROUNTE_APP_WELCOME)
class WelcomeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_welcome_activity)
        initView()
    }

    private fun initView() {
        //设置图片加载器
        banner.setImageLoader(GlideImageLoader())
        //设置图片集合
        val images = mutableListOf<BannerData>()
        images.add(BannerData(R.drawable.icon_welcome_page_1,getString(R.string.string_welcome_title_1)))
        images.add(BannerData(R.drawable.icon_welcome_page_2,getString(R.string.string_welcome_title_2)))
        images.add(BannerData(R.drawable.icon_welcome_page_3,getString(R.string.string_welcome_title_3)))
        banner.setImages(images)
        //banner设置方法全部调用完毕时最后调用
        banner.isAutoPlay(false)
        banner.start()

        sure.setOnClickListener {
            finish()
        }
    }
}