package framework.telegram.business.ui.me

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.TextUtils
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.adapter.MeContactsAdapter
import framework.telegram.business.ui.me.bean.ContactsBean
import framework.telegram.business.ui.me.presenter.ContactsContract
import framework.telegram.business.ui.me.presenter.ContactsPresenterImpl
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.KeyboardktUtils
import framework.telegram.ui.utils.ScreenUtils
import kotlinx.android.synthetic.main.bus_me_activity_phone_contacts.*

@Route(path = Constant.ARouter.ROUNTE_BUS_ME_PHONE_CONTACTS)
class MeContactsActivity : BaseBusinessActivity<ContactsContract.Presenter>(), ContactsContract.View {

    private var mRightView: TextView? = null

    private val mCheckList by lazy { ArrayList<ContactsBean>() }

    private val mAdapter: MeContactsAdapter by lazy {
        MeContactsAdapter { contactsBean: ContactsBean ->

            if (mCheckList.isNotEmpty()) {
                val iterator = mCheckList.iterator()
                var _is = false
                while (iterator.hasNext()) {
                    val bean = iterator.next()
                    if (bean == contactsBean) {
                        iterator.remove()
                        _is = true
                    }
                }
                if (!_is)
                    mCheckList.add(contactsBean)
            } else
                mCheckList.add(contactsBean)


            if (mCheckList.isNotEmpty()) {
                mRightView?.text = String.format(getString(R.string.confirm_match),mCheckList.size)
                mRightView?.setTextColor(getSimpleColor(R.color.c178aff))
            } else {
                mRightView?.text = getString(R.string.confirm)
                mRightView?.setTextColor(getSimpleColor(R.color.d4d6d9))
            }

            custom_search_bar.setSearchText("")
            KeyboardktUtils.hideKeyboard(custom_search_bar)

        }
    }

    override fun getLayoutId(): Int {
        return R.layout.bus_me_activity_phone_contacts
    }

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            onBackPressed()
        }
        custom_toolbar.showCenterTitle(getString(R.string.address_book))
        custom_toolbar.showRightTextView(getString(R.string.confirm), {
            if (mCheckList.isNotEmpty()) {
                var phone = ""
                mCheckList.forEach {
                    phone = phone + it.getPhone().trim() + ";"
                }
                mCheckList.clear()
                mRightView?.text = getString(R.string.confirm)
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("smsto:$phone")

                val shareHost = getString(R.string.invite_to_join_68) + if (!TextUtils.isEmpty(Constant.Common.DOWNLOAD_HTTP_HOST)){
                    Constant.Common.DOWNLOAD_HTTP_HOST
                }else{
                    "https://www.bufa.chat"
                }
                intent.putExtra("sms_body", shareHost)
                startActivity(intent)
            }
        }) {
            val size = ScreenUtils.dp2px(this, 10f)
            mRightView = it
            val lp = mRightView?.layoutParams as LinearLayout.LayoutParams
            lp.rightMargin = ScreenUtils.dp2px(this, 10f)
            mRightView?.setPadding(size, 0, size, 0)
            mRightView?.setTextColor(getSimpleColor(R.color.d4d6d9))
        }

        index_fast_scroll_recycler_view.initIndexFastScrollRecyclerView(
                LinearLayoutManager(this),
                mAdapter,
                false)
        index_fast_scroll_recycler_view.indexFastScrollController()
                .setIndexBarColor(this.resources.getColor(R.color.white))
                .setIndexBarTextColor(this.resources.getColor(R.color.a2a4a7))
                .setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
                .setIndexbarMargin(0f)
                .setIndexTextSize(11)
        index_fast_scroll_recycler_view.visibility = View.VISIBLE
    }

    override fun initListen() {

        custom_search_bar.setSearBarListen {
            mAdapter.filter(it, mCheckList)
            if (TextUtils.isEmpty(it))
                linear_layout_share.visibility = View.VISIBLE
            else
                linear_layout_share.visibility = View.GONE
        }


        linear_layout_share.setOnClickListener {
            showLoading()
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            val shareHost = getString(R.string.invite_to_join_68) + if (!TextUtils.isEmpty(Constant.Common.DOWNLOAD_HTTP_HOST)){
                Constant.Common.DOWNLOAD_HTTP_HOST
            }else{
                "https://www.bufa.chat"
            }
            intent.putExtra(Intent.EXTRA_TEXT, shareHost)
            startActivityForResult(Intent.createChooser(intent, shareHost), 0)
        }

    }

    override fun initData() {
        ContactsPresenterImpl(this, this, lifecycle()).start()
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as ContactsPresenterImpl
    }

    override fun isActive(): Boolean = true

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        mPresenter?.onActivityResult(requestCode, resultCode, data)
    }

    override fun refreshUI(newData: MutableList<ContactsBean>) {
        mAdapter.setNewData(newData)
    }

    override fun onResume() {
        super.onResume()
        mCheckList.clear()
        mRightView?.text = getString(R.string.confirm)
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@MeContactsActivity, this@MeContactsActivity)
    }

    override fun showFinish() {
        dialog?.dismiss()
//        mStatusView.showContentView()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mPresenter?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}