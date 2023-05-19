package framework.telegram.business.ui.contacts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_PHONE
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.contacts.adapter.PhoneContactsAdapter
import framework.telegram.business.ui.contacts.bean.PhoneContactsBean
import framework.telegram.business.ui.contacts.presenter.PhoneContactsContract
import framework.telegram.business.ui.contacts.presenter.PhoneContactsPresenterImpl
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import kotlinx.android.synthetic.main.bus_recycler_inside_linear_with_toolbar.*

/**
 * Created by lzh on 19-5-27.
 * INFO:新朋友
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_CONTACTS_PHONE_CONTACTS)
class PhoneContactsActivity : BaseBusinessActivity<PhoneContactsContract.Presenter>(), PhoneContactsContract.View {
    companion object {
        internal const val GET_PERMISSIONS_REQUEST_CODE = 101
    }

    private val mAdapter by lazy {
        PhoneContactsAdapter({
//            mPresenter?.makeFriend(it, ContactsProto.ContactsOperator.ADD_REQ)
        }, { id,phone->
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACT_DETAIL)
                    .withString(KEY_TARGET_PHONE, phone)
                    .withLong(Constant.ARouter_Key.KEY_TARGET_UID, id).navigation()
        })
    }
    private var mPageNum = 1


    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as PhoneContactsPresenterImpl
    }

    override fun isActive() = true

    override fun getLayoutId() = R.layout.bus_recycler_inside_linear_with_toolbar

    override fun initView() {
        common_recycler.initSingleTypeRecycleView(LinearLayoutManager(this), mAdapter, false)
        common_recycler.refreshController().setEnablePullToRefresh(true)

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.mobile_phone_contacts))
    }

    override fun initListen() {
        common_recycler.refreshController().setOnRefreshListener {
            mPageNum = 1
            mPresenter?.getContactsList(mPageNum, 20)

        }
        common_recycler.loadMoreController().setOnLoadMoreListener {
            mPresenter?.getContactsList(mPageNum, 20)
        }
        common_recycler.emptyController().setEmpty(R.drawable.common_icon_empty_data)
    }

    override fun initData() {
        PhoneContactsPresenterImpl(this, this, lifecycle())

        showLoading()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                common_recycler.refreshController().setEnablePullToRefresh(false)
                requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), GET_PERMISSIONS_REQUEST_CODE)
            } else {
                common_recycler.refreshController().setEnablePullToRefresh(true)
                mPresenter?.updateData()
            }
        } else {
            common_recycler.refreshController().setEnablePullToRefresh(true)
            mPresenter?.updateData()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            GET_PERMISSIONS_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (this.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        common_recycler.refreshController().setEnablePullToRefresh(true)
                        mPresenter?.updateData()
                    }else{
                        dialog?.dismiss()
                        common_recycler.refreshController().setEnablePullToRefresh(false)
                    }
                }
            }
        }
    }

    override fun refreshListUI(pageNum: Int, list: MutableList<PhoneContactsBean>) {
        mPageNum = pageNum
        mAdapter.setNewData(list)
        common_recycler.refreshController().refreshComplete()
        dialog?.dismiss()
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@PhoneContactsActivity,this@PhoneContactsActivity)
    }

    override fun showEmpty() {
        dialog?.dismiss()
    }

    override fun showError(errStr: String?) {
        dialog?.dismiss()
        toast(errStr.toString())
    }
}