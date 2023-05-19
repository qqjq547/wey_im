package framework.telegram.message.ui.location

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_LOCATION_SEARCH_ACTIVITY
import framework.telegram.message.ui.location.bean.ClientLocationStore
import framework.telegram.message.ui.location.bean.POIBean
import framework.telegram.message.ui.location.utils.MapUtils
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseFragment
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.permission.MPermission
import framework.telegram.support.tools.permission.annotation.OnMPermissionDenied
import framework.telegram.support.tools.permission.annotation.OnMPermissionGranted
import framework.telegram.support.tools.permission.annotation.OnMPermissionNeverAskAgain
import framework.telegram.ui.status.QMUIStatusView
import kotlinx.android.synthetic.main.msg_location_choice_location_activity.*
import kotlinx.android.synthetic.main.msg_search.*

/**
 * 发送位置
 * Created by hu on 15/8/28.
 */
@Route(path = Constant.ARouter.ROUNTE_LOCATION_CHOICE_ACTIVITY)
class ChoiceLocationActivity : BaseActivity() {

    private var mFragment: BaseFragment? = null
    private var latitude = 0.0
    private var longitude = 0.0
    private var mAmapFragment: ChoiceLocationAMapFragment? = null
    private var mGmapFragment: ChoiceLocationGMapFragment? = null

    private val mQMUIStatusView by lazy { QMUIStatusView(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.msg_location_choice_location_activity)


        requestBasicPermission()
        initHeadView()
        mQMUIStatusView.showLoadingView()
        Handler().postDelayed({
            mQMUIStatusView.dismiss()
            initFragment()
        },500)

    }

    private fun initHeadView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.pet_text_281))

        custom_toolbar.showRightTextView(getString(R.string.pet_text_1172),{
            if (mFragment != null) {
                 if(mFragment is ChoiceLocationAMapFragment) {
                     (mFragment as ChoiceLocationAMapFragment).let {
                         setResultFinish(it.getChoiceLocation())
                     }
                 } else if(mFragment is ChoiceLocationGMapFragment){
                     (mFragment as ChoiceLocationGMapFragment).let {
                         setResultFinish(it.getChoiceLocation())
                     }
                 }
            }
        })

        text_view_search_icon.setOnClickListener {
            ARouter.getInstance().build(ROUNTE_LOCATION_SEARCH_ACTIVITY).navigation(this@ChoiceLocationActivity,REQUEST_CODE_SEARCH_LOCATION)
        }

    }

    private fun setResultFinish(location: POIBean?){
        if (location != null){
            val data = Intent()
            data.putExtra(RESULT_NAME_FROM_CHOOSE, location)
            setResult(Activity.RESULT_OK, data)
            finish()
        }else{
            toast(getString(R.string.pet_text_444))
        }
    }

    private fun initFragment() {
        val clientLatLng = ClientLocationStore.getLastClientLatLng()
        if (clientLatLng != null) {
            latitude = clientLatLng.latitude
            longitude = clientLatLng.longitude
        }

//        mGmapFragment = ChoiceLocationGMapFragment.newInstance()
//        mFragment = mGmapFragment
//        supportFragmentManager.beginTransaction().replace(R.id.content_view, mGmapFragment!!).commitAllowingStateLoss()

        if (MapUtils.isShouldLoadGoogleMap(this@ChoiceLocationActivity, latitude, longitude)) {
            mGmapFragment = ChoiceLocationGMapFragment.newInstance()
            mFragment = mGmapFragment
            supportFragmentManager.beginTransaction().replace(R.id.content_view, mGmapFragment!!).commitAllowingStateLoss()
        } else {
            mAmapFragment = ChoiceLocationAMapFragment.newInstance()
            mFragment = mAmapFragment
            supportFragmentManager.beginTransaction().replace(R.id.content_view, mAmapFragment!!).commitAllowingStateLoss()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (mFragment == null) {
            return
        }

        if (mFragment is ChoiceLocationAMapFragment) {
            mAmapFragment?.onActivityResult(requestCode, resultCode, data)
        } else {
            mGmapFragment?.onActivityResult(requestCode, resultCode, data)
        }

    }

    /******************权限*****************/
    private val BASIC_PERMISSIONS = arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun requestBasicPermission() {
        MPermission.with(this@ChoiceLocationActivity)
                .setRequestCode(500)
                .permissions(*BASIC_PERMISSIONS)
                .request()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        MPermission.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    @OnMPermissionGranted(500)
    fun onBasicPermissionSuccess() {
        // 授权成功
        if (mFragment is ChoiceLocationAMapFragment) {
            mAmapFragment?.requestAddressedAgain()
        } else {
            mGmapFragment?.requestAddressedAgain()
        }
    }

    @OnMPermissionDenied(500)
    @OnMPermissionNeverAskAgain(500)
    fun onBasicPermissionFailed() {
        //授权失败
        toast(getString(R.string.authorization_failed))
    }

    /******************权限  end*****************/

    companion object {

        val RESULT_NAME_FROM_CHOOSE = "data"

        val REQUEST_CODE_SEARCH_LOCATION = 0xfa01
        val REQUEST_CODE_SEND_LOCATION = 0xfa02

    }
}
