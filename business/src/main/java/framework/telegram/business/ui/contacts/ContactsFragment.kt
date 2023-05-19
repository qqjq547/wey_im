package framework.telegram.business.ui.contacts

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_CONTACTS_GROUP_CHAT
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_JOIN_CONTACTS_REQ_LIST
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_QR_SCAN
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_SEARCH_MULT_CHAT_EXPAND
import framework.telegram.business.bridge.event.JumpContactTopEvent
import framework.telegram.business.ui.base.BaseBusinessFragment
import framework.telegram.business.ui.contacts.adapter.ContactsAdapter
import framework.telegram.business.ui.contacts.bean.ContactItemBean
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.EMPTY_TITLE
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.ITEM_CONTACT
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.ITEM_CONTACT_REQ
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.ITEM_GROUPS
import framework.telegram.business.ui.contacts.presenter.ContactsContract
import framework.telegram.business.ui.contacts.presenter.ContactsPresenterImpl
import framework.telegram.message.bridge.event.JoinContactReqEvent
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.menu.FloatMenu
import framework.telegram.ui.menu.MenuItem
import framework.telegram.ui.qr.decoding.Intents
import framework.telegram.ui.recyclerview.sticky.StickyItemDecoration
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bus_contacts_fragment.*
import kotlinx.android.synthetic.main.bus_contacts_item_head.*
import kotlinx.android.synthetic.main.bus_search.*

/**
 * Created by lzh on 19-5-14.
 * INFO:通讯录
 *
 */
class ContactsFragment : BaseBusinessFragment<ContactsContract.Presenter>(), ContactsContract.View {

    override val fragmentName: String
        get() {
            return "ContactsFragment"
        }

    companion object {
        fun newInstance(): ContactsFragment = ContactsFragment()

        const val QRCODE_REQUEST_CODE = 0x1999
        const val EDIT_CONTECT_NOTENAME_REQUEST_CODE = 0x1998
        const val GET_PERMISSIONS_REQUEST_CODE = 123
    }

    private val mAdapter by lazy { ContactsAdapter() }

    private var mTitleBarRightView: View? = null

    private var mFloatMenu: FloatMenu? = null

    override fun isLazyLoad(): Boolean = true

    override fun getLayoutId() = R.layout.bus_contacts_fragment

    override fun initView() {
        initTitleBar()

        initRecyclerView()
        text_view_head_name.visibility = View.GONE
        text_view_head_name.text = ""
    }

    private fun initTitleBar() {
        custom_toolbar.showLeftTextView(getString(R.string.bus_contacts_contacts)) {
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            it.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        }

        custom_toolbar.showRightImageView(R.drawable.common_icon_add) {
            mTitleBarRightView = it
        }

        custom_toolbar.setToolbarSize(64f)
    }

    @SuppressLint("CheckResult")
    override fun initListen() {
        mTitleBarRightView?.setOnClickListener {
            if (mFloatMenu == null || mFloatMenu?.isShowing == false) {
                initFloatMenu()

                val anim = RotateAnimation(0f, 135f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
                anim.fillAfter = true
                anim.duration = 300
                mTitleBarRightView?.startAnimation(anim)

                mFloatMenu?.showDropDown(it, it.width - 5, 0)
            }
        }

        text_view_search_icon.setOnClickListener {
            ARouter.getInstance().build(ROUNTE_BUS_SEARCH_MULT_CHAT_EXPAND).navigation()
        }

        EventBus.getFlowable(JoinContactReqEvent::class.java)
                .bindToLifecycle(this@ContactsFragment)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (activity != null) {
                        mAdapter.notifyDataSetChanged()
                    }
                }

        EventBus.getFlowable(JumpContactTopEvent::class.java)//跳转到顶部
                .bindToLifecycle(this@ContactsFragment)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (activity != null) {
                        index_fast_scroll_recycler_view.recyclerViewController().scrollToPosition(0)
                    }
                }
    }

    private fun initFloatMenu() {
        if (mFloatMenu != null) {
            return
        }
        mFloatMenu = FloatMenu(activity, 1)
        mFloatMenu?.setMenuDrawable(R.drawable.common_half_corners_trans_020419_10_0, R.drawable.pop_selector_black_item, R.color.c8da7d3, R.color.c40446a)
        mFloatMenu?.items(mutableListOf(MenuItem(getString(R.string.common_contacts_make_friend), R.drawable.common_icon_pop_add)
                , MenuItem(getString(R.string.common_contacts_create_ground_chat), R.drawable.common_icon_pop_create_group)
                , MenuItem(getString(R.string.common_contacts_qr_code), R.drawable.common_icon_pop_qr)))
        mFloatMenu?.setOnItemClickListener { _, text ->
            when (text) {
                getString(R.string.common_contacts_make_friend) -> {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACTS_ADD_FRIEND).navigation()
                }
                getString(R.string.common_contacts_create_ground_chat) -> {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE).withInt("operate", 1).navigation()
                }
                getString(R.string.common_contacts_qr_code) -> {
                    if (checkPermission()) {
                        ARouter.getInstance().build(ROUNTE_BUS_QR_SCAN)
                                .withString(Intents.Scan.SCAN_FORMATS, "QR_CODE")
                                .navigation(activity, QRCODE_REQUEST_CODE)
                        // 到MainActivity中解析返回的数据
                    }
                }
            }
        }
        mFloatMenu?.setOnDismissListener {
            val anim = RotateAnimation(135f, 0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
            anim.fillAfter = true
            anim.duration = 300
            mTitleBarRightView?.startAnimation(anim)
        }
    }

    private fun initRecyclerView() {
        sticky_head_container.setDataCallback {
            val str = mPresenter?.getItem(it)?.getTitle()
            if (str == EMPTY_TITLE) {
                text_view_head_name.visibility = View.GONE
            } else {
                text_view_head_name.visibility = View.VISIBLE
            }

            if (getString(R.string.string_star).equals(str)) {
                text_view_head_name.text = getString(R.string.string_star_sign)
            } else {
                text_view_head_name.text = str
            }
        }

        mAdapter.setOnItemClickListener { _, _, position ->
            val data = mPresenter?.getItem(position)
            when (data?.itemType) {
                ITEM_CONTACT -> {
                    val info = data.getInfo()
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACT_DETAIL)
                            .withLong(Constant.ARouter_Key.KEY_TARGET_UID, info?.uid
                                    ?: 0).navigation()
                }
                ITEM_CONTACT_REQ -> {
                    EventBus.publishEvent(JoinContactReqEvent(0))
                    ARouter.getInstance().build(ROUNTE_BUS_JOIN_CONTACTS_REQ_LIST).navigation()
                }
                ITEM_GROUPS -> {
                    ARouter.getInstance().build(ROUNTE_BUS_CONTACTS_GROUP_CHAT).navigation()
                }
                else -> {
                }
            }
        }

        mAdapter.setOnItemLongClickListener { adapter, _, position ->
            if (adapter.getItemViewType(position) == ITEM_CONTACT){
                val item = mPresenter?.getItem(position)?.getInfo()
                val floatMenu = FloatMenu(this.activity)
                floatMenu.items(if (item?.isBfStar == true) getString(R.string.bus_contacts_start_cancel) else getString(R.string.bus_contacts_start)
                        , getString(R.string.bus_contacts_remarks))
                floatMenu.show(index_fast_scroll_recycler_view.popPoint)
                floatMenu.setOnItemClickListener { _, text ->
                    if (text == getString(R.string.bus_contacts_start_cancel) || text == getString(R.string.bus_contacts_start)) {
                        mPresenter?.setStarFriend(item?.uid ?: 0, !item?.isBfStar!!)
                    } else {
                        ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACTS_INFO_EDIT)
                                .withString("title", getString(R.string.remark_name))
                                .withString("defaultContent", item?.noteName)
                                .withString("nickName", item?.nickName)
                                .withInt("type", 0)
                                .withLong("uid", item?.uid ?: 0L)
                                .navigation(activity, EDIT_CONTECT_NOTENAME_REQUEST_CODE)
                        // 到MainActivity中解析返回的数据
                    }
                }
            }
            return@setOnItemLongClickListener false
        }

        index_fast_scroll_recycler_view.initIndexFastScrollRecyclerView(
                LinearLayoutManager(this.context!!),
                mAdapter,
                false)
        index_fast_scroll_recycler_view.indexFastScrollController()
                .setIndexBarColor(this.context!!.resources.getColor(R.color.white))
                .setIndexBarTextColor(this.context!!.resources.getColor(R.color.a2a4a7))
                .setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
                .setIndexbarMargin(0f)
                .setIndexTextSize(11)
                .addItemDecoration(StickyItemDecoration(sticky_head_container, 0))
        index_fast_scroll_recycler_view.refreshController().setEnablePullToRefresh(false)
    }

    @SuppressLint("CheckResult")
    override fun initData() {
        ContactsPresenterImpl(this, this.context!!, lifecycle()).start()
    }

    override fun refreshListUI(list: MutableList<ContactItemBean>) {
        mAdapter.setNewData(list)
    }

    override fun showError(errStr: String?) {
        toast(errStr ?: "")
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as ContactsContract.Presenter
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter?.destory()
    }

    @SuppressLint("CheckResult")
    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.context?.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), GET_PERMISSIONS_REQUEST_CODE)
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            GET_PERMISSIONS_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (this.context?.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        toast(getString(R.string.no_permissions_were_obtained))
                    } else {
                        ARouter.getInstance().build(ROUNTE_BUS_QR_SCAN)
                                .withString(Intents.Scan.SCAN_FORMATS, "QR_CODE")
                                .navigation(activity, QRCODE_REQUEST_CODE)
                        // 到MainActivity中解析返回的数据
                    }
                }
            }
        }
    }
}