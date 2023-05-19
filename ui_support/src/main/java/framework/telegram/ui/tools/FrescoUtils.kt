package framework.telegram.ui.tools

import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.postprocessors.IterativeBoxBlurPostProcessor
import com.facebook.imagepipeline.request.ImageRequestBuilder
import framework.telegram.ui.utils.UriUtils

object FrescoUtils {

    /**
     * 以高斯模糊显示。
     *
     * @param draweeView View。
     * @param url        url.
     * @param iterations 迭代次数，越大越魔化。
     * @param blurRadius 模糊图半径，必须大于0，越大越模糊。
     */
    fun showUrlBlur(draweeView: SimpleDraweeView, url: String?, iterations: Int = 3, blurRadius: Int = 5) {
        try {
            val uri = UriUtils.parseUri(url)
            val request = ImageRequestBuilder.newBuilderWithSource(uri).setPostprocessor(IterativeBoxBlurPostProcessor(iterations, blurRadius)).build()
            val controller = Fresco.newDraweeControllerBuilder()
                    .setOldController(draweeView.controller)
                    .setImageRequest(request)
                    .build()
            draweeView.controller = controller
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}