package framework.telegram.app.activity.welcome

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alibaba.android.arouter.facade.annotation.Route
import com.facebook.common.util.UriUtil
import com.facebook.drawee.backends.pipeline.Fresco
import framework.telegram.app.Constant
import framework.telegram.app.R
import framework.telegram.support.BaseFragment
import kotlinx.android.synthetic.main.app_welcome_item.*


@Route(path = Constant.ARouter.ROUNTE_APP_WELCOME_FRAGMENT)
class WelcomeFragment : BaseFragment() {

    private val mTitle by lazy { arguments?.getString("title", "") }
    private val mRid by lazy { arguments?.getInt("rid", 0) }
    private val mFirst by lazy { arguments?.getBoolean("isFirst", false) }

    override val fragmentName: String
        get() {
            return "WelcomeFragment"
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.app_welcome_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        welcome_text_view.text = mTitle
        val uri = Uri.Builder().scheme(UriUtil.LOCAL_RESOURCE_SCHEME).path("" + mRid).build()
        val controller = Fresco.newDraweeControllerBuilder()
                .setUri(uri)
                .setAutoPlayAnimations(mFirst == true)
                .build()
        welcome_image_view.controller = controller
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser){
            if (mFirst == false){
                startGif()
            }
        }else{
//            welcome_image_view?.set()
        }
    }

    private fun startGif(){
        val animate = welcome_image_view.controller?.animatable
        if (animate?.isRunning == false) {
            animate.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        welcome_image_view?.clearAnimation()
    }
}


