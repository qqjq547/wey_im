package framework.telegram.app.activity.welcome

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.facebook.common.util.UriUtil
import com.facebook.drawee.backends.pipeline.Fresco
import framework.telegram.app.R
import framework.telegram.ui.banner.loader.ImageLoaderInterface
import framework.telegram.ui.image.AppImageView

/**
 * Created by lzh on 20-1-4.
 * INFO:
 */
class GlideImageLoader : ImageLoaderInterface<View> {

    override fun displayImage(context: Context, data: Any, view: View,postion:Int) {
        if (data is BannerData) {
            if (postion < 4  ){//切换到最后一页的时候，这里会返回一个4,一个0的，同一个UI，不要显示4的
                val imageView = view.findViewById<AppImageView>(R.id.welcome_image_view)
                val uri = Uri.Builder().scheme(UriUtil.LOCAL_RESOURCE_SCHEME).path("" + data.rid).build()
                val controller = Fresco.newDraweeControllerBuilder()
                        .setUri(uri)
                        .setAutoPlayAnimations(true)
                        .build()
                imageView.controller = controller
            }
            val textView = view.findViewById<TextView>(R.id.welcome_text_view)
            textView.text = data.title
        }
    }

    override fun createImageView(context: Context): View {
        return LayoutInflater.from(context).inflate(R.layout.app_welcome_item_2, null, false)
    }
}