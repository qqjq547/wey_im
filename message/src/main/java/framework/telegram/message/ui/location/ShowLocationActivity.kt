package framework.telegram.message.ui.location

import android.Manifest
import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.ui.location.bean.ClientLocationStore
import framework.telegram.message.ui.location.bean.LocationBean
import framework.telegram.message.ui.location.utils.MapUtils
import framework.telegram.message.ui.location.utils.NavigationUtils

import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.BaseFragment
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.permission.MPermission
import framework.telegram.support.tools.permission.annotation.OnMPermissionDenied
import framework.telegram.support.tools.permission.annotation.OnMPermissionGranted
import framework.telegram.support.tools.permission.annotation.OnMPermissionNeverAskAgain
import kotlinx.android.synthetic.main.msg_location_show_activity.*

/**
 * 聊天发送的地图展示页
 * Created by hu on 15/8/28.
 */
@Route(path = Constant.ARouter.ROUNTE_LOCATION_SHOW_ACTIVITY)
class ShowLocationActivity : BaseActivity() {

    private var mFragment: BaseFragment? = null
    private var mBundle: Bundle? = null
    private var latitude = 0.0
    private var longitude = 0.0

    private var mAmapFragment: ShowLocationAMapFragment? = null
    private var mGmapFragment: ShowLocationGMapFragment? = null

    private val mLocationBean by lazy {intent.getSerializableExtra("location") as LocationBean? }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.msg_location_show_activity)
        requestBasicPermission()
        initHeadView()
        initFragment()
    }

    private fun initFragment() {
        mBundle = Bundle()
        mBundle?.putSerializable("location", mLocationBean)

        val clientLatLng = ClientLocationStore.getLastClientLatLng()
        if (clientLatLng != null) {
            latitude = clientLatLng.latitude
            longitude = clientLatLng.longitude
        }
        if (MapUtils.isShouldLoadGoogleMap(this@ShowLocationActivity, latitude , longitude)) {
            mGmapFragment = ShowLocationGMapFragment.newInstance()
            mFragment = mGmapFragment
            mGmapFragment?.arguments = mBundle
            supportFragmentManager.beginTransaction().replace(R.id.content_view, mGmapFragment!!).commitAllowingStateLoss()
        } else {
            mAmapFragment = ShowLocationAMapFragment.newInstance()
            mFragment = mAmapFragment
            mAmapFragment?.arguments = mBundle
            supportFragmentManager.beginTransaction().replace(R.id.content_view, mAmapFragment!!).commitAllowingStateLoss()
        }
    }

    private fun initHeadView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.pet_text_281))

        custom_toolbar.showRightTextView(getString(R.string.pet_text_1030),{
            if (mFragment != null) {
                if(mFragment is ShowLocationAMapFragment) {
                    (mFragment as ShowLocationAMapFragment).let {
                        goLocation()
                    }
                } else if(mFragment is ShowLocationGMapFragment){
                    (mFragment as ShowLocationGMapFragment).let {
                         goLocation()
                    }
                }
            }
        })
    }

    private fun goLocation(){
        val clientLocation = ClientLocationStore.getLastClientLocation()
        if (clientLocation != null) {
            NavigationUtils.navigation(this@ShowLocationActivity, clientLocation, mLocationBean)
        } else {
            BaseApp.app.toast(getString(R.string.pet_text_189))
        }
    }

      /******************权限*****************/
    private val BASIC_PERMISSIONS = arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun requestBasicPermission() {
        MPermission.with(this@ShowLocationActivity)
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
        if (mFragment is ShowLocationAMapFragment) {
            mAmapFragment?.requestAddressedAgain()
        } else {
            mGmapFragment?.requestAddressedAgain()
        }
    }

    @OnMPermissionDenied(500)
    @OnMPermissionNeverAskAgain(500)
    fun onBasicPermissionFailed() {
        //授权失败
        BaseApp.app.toast(getString(R.string.authorization_failed))
    }

    /******************权限  end*****************/
}
