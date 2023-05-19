package framework.telegram.app.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.facebook.drawee.backends.pipeline.Fresco
import com.fm.openinstall.OpenInstall
import com.fm.openinstall.listener.AppInstallAdapter
import com.fm.openinstall.model.AppData
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.telegram.app.Constant.ARouter.ROUNTE_APP_MAIN
import framework.telegram.app.R
import framework.telegram.app.activity.presenter.MainImpl
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.UpdatePresenterImpl
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.event.JumpContactTopEvent
import framework.telegram.business.bridge.event.JumpStreamTopEvent
import framework.telegram.business.bridge.event.JumpTpUnreadChatEvent
import framework.telegram.business.bridge.event.MeRedPointChangeEvent
import framework.telegram.business.sp.CommonPref
import framework.telegram.business.ui.contacts.ContactsFragment
import framework.telegram.business.ui.me.MeFragment
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.JoinContactReqEvent
import framework.telegram.message.bridge.event.MainToChatMessageEvent
import framework.telegram.message.bridge.event.UnreadMessageEvent
import framework.telegram.message.ui.IMultiCheckCallCallback
import framework.telegram.message.ui.IMultiCheckChatCallback
import framework.telegram.message.ui.IMultiCheckable
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseFragment
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.log.core.utils.SysUtils
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.JumpPermissionManagement
import framework.telegram.support.tools.NotificationUtils
import framework.telegram.support.tools.permission.MPermission
import framework.telegram.support.tools.permission.annotation.OnMPermissionDenied
import framework.telegram.support.tools.permission.annotation.OnMPermissionGranted
import framework.telegram.support.tools.permission.annotation.OnMPermissionNeverAskAgain
import framework.telegram.ui.dialog.AppDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.app_main_activity.*

@Route(path = ROUNTE_APP_MAIN)
class MainActivity : BaseActivity(), IMultiCheckable {

    // 最后一次点击退出的时间
    private var mLastExitTime: Long = 0

    enum class PAGE {
        MSG, ADD, PHOTO, ME
    }

    private var mUpdatePresenterImpl: UpdatePresenterImpl? = null
    private var mMainImpl: MainImpl? = null

    private val mFromLogin by lazy { intent.getBooleanExtra("fromLogin", false) }

    private val mSp by lazy { SharePreferencesStorage.createStorageInstance(CommonPref::class.java) }

    private val commonPref by lazy {
        SharePreferencesStorage.createStorageInstance(
            CommonPref::class.java,
            AccountManager.getLoginAccountUUid()
        )
    }

    private var mPagePosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_main_activity)

        initView()
        initMultiCheckable()
        initData()
        initListen()

        clickButton(PAGE.MSG)

        OpenInstall.getInstall(installAdapter)

        mUpdatePresenterImpl = UpdatePresenterImpl(this, this, lifecycle())
        mUpdatePresenterImpl?.start(
            showCanUpDialog = mUpdatePresenterImpl?.needCheckVersion() == true,
            showNotUpdateDialog = false
        )

        mMainImpl = MainImpl(this, lifecycle())

        val versionCode = SysUtils.getAppVersionCode(this@MainActivity)
        val localNotificationCode = mSp.getCheckNotificationVersionCode()
        if (mFromLogin || versionCode > localNotificationCode) {
            val result = NotificationUtils.notificationSwitchOn(this@MainActivity)
            if (!result) {
                AppDialog.showCustomView(
                    this@MainActivity,
                    R.layout.bus_main_notification_dialog,
                    null
                ) {
                    view.setBackgroundColor(Color.TRANSPARENT)
                    findViewById<TextView>(R.id.cancel).setOnClickListener {
                        this.dismiss()
                    }
                    findViewById<TextView>(R.id.sure).setOnClickListener {
                        NotificationUtils.showNotificationWindow(this@MainActivity)
                        this.dismiss()
                    }
                }
            }
            mSp.putCheckNotificationVersionCode(versionCode)
        }

        requestBasicPermission()
    }

    override fun onResume() {
        super.onResume()

        //退到后台高斯模糊需求，不同手机系统，不同效果
        if (commonPref.getBlurScreen()) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // 更新最新的未读数量
        EventBus.publishEvent(UnreadMessageEvent())

        val bindData = mSp.getWakeupBindData()
        if (!TextUtils.isEmpty(bindData)) {
            mMainImpl?.uploadOpenInstallData(bindData, 1, mSp.getChannelCode())
            mSp.putWakeupBindData("")
            mSp.putChannelCode("")
        }

        Glide.get(applicationContext).clearMemory()
    }

    private fun initView() {
        view_page.offscreenPageLimit = 4
        view_page.setCanScroll(false)

        setMeRedPoint()
    }

    companion object {
        lateinit var app: Application
    }

    private fun initData() {
        val fragmentList: List<BaseFragment> = listOf(
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_CHATS_FRAGMENT)
                .navigation() as BaseFragment,
            ContactsFragment.newInstance(),
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_HISTORY_FRAGMENT)
                .navigation() as BaseFragment,
            MeFragment.newInstance()
        )

        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        viewPagerAdapter.setData(fragmentList)
        view_page.adapter = viewPagerAdapter

        text_view_new_friend_count.visibility = View.GONE
        text_view_messages_count.visibility = View.GONE
    }

    private fun doubleClick(view: View, click: ((Boolean) -> Unit)) {
        val lastTime = view.getTag(R.id.doubleCheckId)
        val nowTime = System.currentTimeMillis()
        lastTime?.let {
            val checkTime = nowTime - (lastTime) as Long
            if (checkTime in 1..299) {
                click.invoke(true)
            } else {
                click.invoke(false)
            }
        }
        view.setTag(R.id.doubleCheckId, System.currentTimeMillis())
    }

    @SuppressLint("CheckResult")
    private fun initListen() {
        frame_layout_messages.setOnClickListener {
            doubleClick(it) { isDouble ->
                if (isDouble) {
                    EventBus.publishEvent(JumpTpUnreadChatEvent())
                } else {
                    clickButton(PAGE.MSG)
                }
            }
        }

        frame_layout_contacts.setOnClickListener {
            doubleClick(it) { isDouble ->
                if (isDouble) {
                    EventBus.publishEvent(JumpContactTopEvent())
                } else {
                    clickButton(PAGE.ADD)
                }
            }
        }

        frame_layout_phone.setOnClickListener {
            doubleClick(it) { isDouble ->
                if (isDouble) {
                    EventBus.publishEvent(JumpStreamTopEvent())
                } else {
                    clickButton(PAGE.PHOTO)
                }
            }
        }

        frame_layout_me.setOnClickListener {
            clickButton(PAGE.ME)
        }

        EventBus.getFlowable(JoinContactReqEvent::class.java)
            .bindToLifecycle(this@MainActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (JoinContactReqEvent.contactReqCount > 0) {
                    text_view_new_friend_count.text =
                        if (JoinContactReqEvent.contactReqCount <= 99) "${JoinContactReqEvent.contactReqCount}" else "99+"
                    text_view_new_friend_count.visibility = View.VISIBLE
                } else {
                    text_view_new_friend_count.visibility = View.GONE
                }
            }

        EventBus.getFlowable(UnreadMessageEvent::class.java)
            .bindToLifecycle(this@MainActivity)
            .subscribeOn(Schedulers.io())
            .map {
                ArouterServiceManager.messageService.getAllUnreadMessageCount()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it > 0) {
                    text_view_messages_count.text = if (it <= 99) "$it" else "99+"
                    text_view_messages_count.visibility = View.VISIBLE
                } else {
                    text_view_messages_count.visibility = View.GONE
                }
            }

        EventBus.getFlowable(MeRedPointChangeEvent::class.java)
            .bindToLifecycle(this@MainActivity)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                setMeRedPoint()
            }

        EventBus.getFlowable(MainToChatMessageEvent::class.java)
            .bindToLifecycle(this@MainActivity)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                clickButton(PAGE.MSG)
            }
    }

    private fun setMeRedPoint() {
        val hasPassword = AccountManager.getLoginAccount(AccountInfo::class.java).getBfPassword()
        val permission = SharePreferencesStorage.createStorageInstance(
            CommonPref::class.java,
            AccountManager.getLoginAccountUUid()
        ).getFirstOpenMessagePermission(false)
        if (hasPassword && permission) {
            text_view_new_me_count.visibility = View.GONE
        } else {
            text_view_new_me_count.visibility = View.VISIBLE
        }
    }

    override fun onBackPressed() {
        if (layout_check_msg.visibility == View.VISIBLE) {
            dismissCheckMessages()
        } else {
            exitApp()
        }
    }

    private fun exitApp() {
        if (System.currentTimeMillis() - mLastExitTime > 1000) {
            toast(getString(R.string.common_quit_app_tips))
            mLastExitTime = System.currentTimeMillis()
        } else {
            ActivitiesHelper.getInstance().closeAll()
            run {
                Runtime.getRuntime().gc()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mUpdatePresenterImpl?.cancel()
        installAdapter = null

        Glide.get(applicationContext).clearMemory()
        Fresco.getImagePipeline().clearMemoryCaches()
    }

    private fun clickButton(page: PAGE): Int {
        text_view_message.setCompoundDrawablesWithIntrinsicBounds(
            null,
            getSimpleDrawable(R.drawable.app_main_icon_message),
            null,
            null
        )
        text_view_contacts.setCompoundDrawablesWithIntrinsicBounds(
            null,
            getSimpleDrawable(R.drawable.app_main_icon_contacts),
            null,
            null
        )
        text_view_phone.setCompoundDrawablesWithIntrinsicBounds(
            null,
            getSimpleDrawable(R.drawable.app_main_icon_phone),
            null,
            null
        )
        text_view_me.setCompoundDrawablesWithIntrinsicBounds(
            null,
            getSimpleDrawable(R.drawable.app_main_icon_me),
            null,
            null
        )

        text_view_message.setTextColor(getSimpleColor(R.color.c7085b0))
        text_view_contacts.setTextColor(getSimpleColor(R.color.c7085b0))
        text_view_phone.setTextColor(getSimpleColor(R.color.c7085b0))
        text_view_me.setTextColor(getSimpleColor(R.color.c7085b0))

        when (page) {
            PAGE.MSG -> {
                mPagePosition = 0
                text_view_message.setTextColor(getSimpleColor(R.color.c178aff))
                text_view_message.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    getSimpleDrawable(R.drawable.app_main_icon_message_select),
                    null,
                    null
                )
            }
            PAGE.ADD -> {
                mPagePosition = 1
                text_view_contacts.setTextColor(getSimpleColor(R.color.c178aff))
                text_view_contacts.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    getSimpleDrawable(R.drawable.app_main_icon_contacts_select),
                    null,
                    null
                )
            }

            PAGE.PHOTO -> {
                mPagePosition = 2
                text_view_phone.setTextColor(getSimpleColor(R.color.c178aff))
                text_view_phone.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    getSimpleDrawable(R.drawable.app_main_icon_phone_select),
                    null,
                    null
                )
            }

            PAGE.ME -> {
                mPagePosition = 3
                text_view_me.setTextColor(getSimpleColor(R.color.c178aff))
                text_view_me.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    getSimpleDrawable(R.drawable.app_main_icon_me_select),
                    null,
                    null
                )
            }
        }
        view_page.setCurrentItem(mPagePosition, false)
        return mPagePosition
    }

    inner class ViewPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getCount(): Int = mFragments?.size ?: 0

        private var mFragments: List<Fragment>? = null

        fun setData(fragments: List<Fragment>?) {
            mFragments = if (fragments == null) {
                ArrayList()
            } else {
                ArrayList(fragments)
            }
            notifyDataSetChanged()
        }

        fun getFragments(): List<Fragment>? {
            return mFragments
        }

        override fun getItem(position: Int): Fragment {
            return mFragments?.get(position)!!
        }

        override fun getPageTitle(position: Int): CharSequence {
            return ""
        }
    }

    private fun initMultiCheckable() {
        text_view_cancel_check_msg.setOnClickListener {
            dismissCheckMessages()
        }

        tv_delete.setOnClickListener {
            val currentFragment =
                (view_page.adapter as ViewPagerAdapter).getFragments()?.get(view_page.currentItem)
            if (currentFragment is IMultiCheckChatCallback) {
                currentFragment.clickBatchDelete()
            } else if (currentFragment is IMultiCheckCallCallback) {
                currentFragment.clickBatchDelete()
            }
        }

        tv_set_readed.setOnClickListener {
            val currentFragment =
                (view_page.adapter as ViewPagerAdapter).getFragments()?.get(view_page.currentItem)
            if (currentFragment is IMultiCheckChatCallback) {
                currentFragment.clickBatchSetReaded()
            }
        }

        check_box_msg.setOnCheckedChangeListener(onCheckedChangeListener)
    }

    private val onCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        val currentFragment =
            (view_page.adapter as ViewPagerAdapter).getFragments()?.get(view_page.currentItem)
        var msgCount = 0
        if (currentFragment is IMultiCheckChatCallback) {
            msgCount = currentFragment.clickAllChecked(isChecked)
        } else if (currentFragment is IMultiCheckCallCallback) {
            msgCount = currentFragment.clickAllChecked(isChecked)
        }

        setButtonsEnable(msgCount)
    }

    private fun hasUnread(): Boolean {
        val currentFragment =
            (view_page.adapter as ViewPagerAdapter).getFragments()?.get(view_page.currentItem)
        if (currentFragment is IMultiCheckChatCallback) {
            return currentFragment.hasUnread()
        }

        return false
    }

    private fun setButtonsEnable(msgCount: Int) {
        if (msgCount > 0) {
            tv_delete.isEnabled = true
            tv_delete.isClickable = true
            tv_delete.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.f50d2e))

            if (hasUnread()) {
                tv_set_readed.isEnabled = true
                tv_set_readed.isClickable = true
                tv_set_readed.setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.c007afe
                    )
                )
            }
        } else {
            tv_set_readed.isEnabled = false
            tv_set_readed.isClickable = false
            tv_set_readed.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.a2a4a7))

            tv_delete.isEnabled = false
            tv_delete.isClickable = false
            tv_delete.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.a2a4a7))
        }

        text_view_check_msg_title.text = "$msgCount"
    }

    override fun setCheckableMessage(showSetReaded: Boolean, countTitle: String, msgCount: Int) {
        if (showSetReaded) {
            tv_set_readed.visibility = View.VISIBLE
        } else {
            tv_set_readed.visibility = View.GONE
        }

        setButtonsEnable(msgCount)

        multi_checked_count_title.text = countTitle

        val currentFragment =
            (view_page.adapter as ViewPagerAdapter).getFragments()?.get(view_page.currentItem)
        if (currentFragment is IMultiCheckChatCallback) {
            currentFragment.showCheckMessages()
        } else if (currentFragment is IMultiCheckCallCallback) {
            currentFragment.showCheckMessages()
        }

        layout_check_msg.visibility = View.VISIBLE
    }

    override fun setAllChecked(allChecked: Boolean) {
        check_box_msg.setOnCheckedChangeListener(null)
        check_box_msg.isChecked = allChecked
        check_box_msg.setOnCheckedChangeListener(onCheckedChangeListener)
    }

    override fun dismissCheckMessages() {
        val currentFragment =
            (view_page.adapter as ViewPagerAdapter).getFragments()?.get(view_page.currentItem)
        if (currentFragment is IMultiCheckChatCallback) {
            currentFragment.dismissCheckMessages()
        } else if (currentFragment is IMultiCheckCallCallback) {
            currentFragment.dismissCheckMessages()
        }

        check_box_msg.setOnCheckedChangeListener(null)
        check_box_msg.isChecked = false
        check_box_msg.setOnCheckedChangeListener(onCheckedChangeListener)

        layout_check_msg.visibility = View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (ContactsFragment.EDIT_CONTECT_NOTENAME_REQUEST_CODE == requestCode && Activity.RESULT_OK == resultCode) {
            val text = data?.getStringExtra("text") ?: ""
            val nickName = data?.getStringExtra("nickName") ?: ""
            val uid = data?.getLongExtra("uid", 0L) ?: 0L
            if (uid > 0) {
                ArouterServiceManager.contactService.setContactNote(null, uid, nickName, text)
            }
        }
    }

    private var installAdapter: AppInstallAdapter? = object : AppInstallAdapter() {
        override fun onInstall(appData: AppData) {
            val fisrtInstall = mSp.getFirstInstall()
            if (!fisrtInstall) {
                //获取渠道数据
                val channelCode = appData.getChannel()
                //获取自定义数据
                val bindData = appData.getData()

                mMainImpl?.uploadOpenInstallData(bindData, 0, channelCode)
                mSp.putFirstInstall(true)
            }

        }
    }

    /**
     * 以下代码专门用于解决:
     * 切换语言后，MainActivity会进行recreate 和 进程在后台被杀死，重新创建界面的时候
     * 引起MainActivity下方图标与显示的fragment不一致问题
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val pagePosition = savedInstanceState.getInt("mPagePosition", -1)
        if (pagePosition != -1) {
            text_view_message.setCompoundDrawablesWithIntrinsicBounds(
                null,
                getSimpleDrawable(R.drawable.app_main_icon_message),
                null,
                null
            )
            text_view_contacts.setCompoundDrawablesWithIntrinsicBounds(
                null,
                getSimpleDrawable(R.drawable.app_main_icon_contacts),
                null,
                null
            )
            text_view_phone.setCompoundDrawablesWithIntrinsicBounds(
                null,
                getSimpleDrawable(R.drawable.app_main_icon_phone),
                null,
                null
            )
            text_view_me.setCompoundDrawablesWithIntrinsicBounds(
                null,
                getSimpleDrawable(R.drawable.app_main_icon_me),
                null,
                null
            )

            text_view_message.setTextColor(getSimpleColor(R.color.c7085b0))
            text_view_contacts.setTextColor(getSimpleColor(R.color.c7085b0))
            text_view_phone.setTextColor(getSimpleColor(R.color.c7085b0))
            text_view_me.setTextColor(getSimpleColor(R.color.c7085b0))

            when (pagePosition) {
                0 -> {
                    text_view_message.setTextColor(getSimpleColor(R.color.c178aff))
                    text_view_message.setCompoundDrawablesWithIntrinsicBounds(
                        null,
                        getSimpleDrawable(R.drawable.app_main_icon_message_select),
                        null,
                        null
                    )
                }
                1 -> {
                    text_view_contacts.setTextColor(getSimpleColor(R.color.c178aff))
                    text_view_contacts.setCompoundDrawablesWithIntrinsicBounds(
                        null,
                        getSimpleDrawable(R.drawable.app_main_icon_contacts_select),
                        null,
                        null
                    )
                }

                2 -> {
                    text_view_phone.setTextColor(getSimpleColor(R.color.c178aff))
                    text_view_phone.setCompoundDrawablesWithIntrinsicBounds(
                        null,
                        getSimpleDrawable(R.drawable.app_main_icon_phone_select),
                        null,
                        null
                    )
                }

                3 -> {
                    text_view_me.setTextColor(getSimpleColor(R.color.c178aff))
                    text_view_me.setCompoundDrawablesWithIntrinsicBounds(
                        null,
                        getSimpleDrawable(R.drawable.app_main_icon_me_select),
                        null,
                        null
                    )
                }
            }
            mPagePosition = pagePosition
            view_page.setCurrentItem(mPagePosition, false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("mPagePosition", mPagePosition)
        super.onSaveInstanceState(outState)
    }

    /******************权限*****************/
    private val BASIC_PERMISSIONS = arrayOf<String>(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private fun requestBasicPermission() {
        MPermission.with(this@MainActivity)
            .setRequestCode(framework.telegram.business.bridge.Constant.Permission.BASIC_PERMISSION_REQUEST_CODE)
            .permissions(*BASIC_PERMISSIONS)
            .request()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        MPermission.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    @OnMPermissionGranted(framework.telegram.business.bridge.Constant.Permission.BASIC_PERMISSION_REQUEST_CODE)
    fun onBasicPermissionSuccess() {
        // 授权成功
    }

    @OnMPermissionDenied(framework.telegram.business.bridge.Constant.Permission.BASIC_PERMISSION_REQUEST_CODE)
    @OnMPermissionNeverAskAgain(framework.telegram.business.bridge.Constant.Permission.BASIC_PERMISSION_REQUEST_CODE)
    fun onBasicPermissionFailed() {
        //授权失败
        AppDialog.show(this@MainActivity, this@MainActivity) {
            message(text = context.getString(framework.telegram.business.R.string.authorization_failed_fail))
            cancelOnTouchOutside(false)
            cancelable(false)
            negativeButton(
                text = context.getString(framework.telegram.business.R.string.pet_text_508),
                click = {
                    requestBasicPermission()
                })
            positiveButton(text = getString(R.string.pet_text_195), click = {
                ActivitiesHelper.getInstance().closeAll()
                JumpPermissionManagement.GoToSetting(this@MainActivity)
            })
            title(text = context.getString(framework.telegram.business.R.string.hint))
        }
    }
    /******************权限  end*****************/
}