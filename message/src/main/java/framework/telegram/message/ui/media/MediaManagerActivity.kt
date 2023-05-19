package framework.telegram.message.ui.media

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.trello.rxlifecycle3.kotlin.bindToLifecycle
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.DisableGroupMessageEvent
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseFragment
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.ui.dialog.AppDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.msg_activity_media.*


@Route(path = Constant.ARouter.ROUNTE_MSG_MEDIA_MANAGER)
class MediaManagerActivity: BaseActivity(){
    private val mChaterId: Long by lazy {
        intent.getLongExtra("chaterId",0L)
    }
    private val mChatType: Int by lazy {
        intent.getIntExtra("chatType",-1)
    }

    private val mCurPager: Int by lazy {
        intent.getIntExtra("curPager",0)
    }

    private val mSwitchView by lazy {
        LayoutInflater.from(this).inflate(R.layout.msg_fragment_media_switch,null)
    }

    private val mMediaBtn by lazy {
        mSwitchView.findViewById<TextView>(R.id.btn_media)
    }

    private val mFileBtn by lazy {
        mSwitchView.findViewById<TextView>(R.id.btn_file)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.msg_activity_media)


        initToolbar()
        initViewPager()
    }


    @SuppressLint("CheckResult")
    private fun initToolbar(){
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            onBackPressed()
        }
        custom_toolbar.addCenterView(mSwitchView,-1f,40f)

        EventBus.getFlowable(DisableGroupMessageEvent::class.java)
                .bindToLifecycle(this@MediaManagerActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.groupId == mChaterId) {
                        if (ActivitiesHelper.getInstance().topActivity == this@MediaManagerActivity){
                            AppDialog.show(this@MediaManagerActivity, this@MediaManagerActivity) {
                                positiveButton(text = getString(R.string.confirm), click = {
                                    //清空聊天记录
                                    finish()
                                })
                                cancelOnTouchOutside(false)
                                message(text = getString(R.string.string_group_dismiss_title))
                            }
                        }else{
                            finish()
                        }
                    }
                }
    }

    private fun initViewPager(){
        val mFragmentList= listOf<BaseFragment>(
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_MEDIA_MANAGER_MEDIA)
                        .withLong("chaterId",mChaterId)
                        .withInt("chatType",mChatType)
                        .navigation() as BaseFragment,
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_MEDIA_MANAGER_FILE)
                        .withLong("chaterId",mChaterId)
                        .withInt("chatType",mChatType)
                        .navigation() as BaseFragment
        )
        viewpager.offscreenPageLimit = 2
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        viewPagerAdapter.setData(mFragmentList)
        viewpager.adapter = viewPagerAdapter
        viewpager.addOnPageChangeListener(viewPagerAdapter)
        viewpager.currentItem = mCurPager

        mMediaBtn.setOnClickListener {
            viewpager.currentItem = 0
        }
        mFileBtn.setOnClickListener {
            viewpager.currentItem = 1
        }
    }

    private fun changeUI(position: Int){
        if (position == 0) {
            mFileBtn.typeface = Typeface.DEFAULT
            mFileBtn.setBackgroundColor(Color.TRANSPARENT)
            mMediaBtn.typeface = Typeface.DEFAULT_BOLD
            mMediaBtn.setBackgroundResource(R.drawable.msg_corners_trans_white_6_0)
        }
        else if (position == 1) {
            mMediaBtn.typeface = Typeface.DEFAULT
            mMediaBtn.setBackgroundColor(Color.TRANSPARENT)
            mFileBtn.typeface = Typeface.DEFAULT_BOLD
            mFileBtn.setBackgroundResource(R.drawable.msg_corners_trans_white_6_0)
        }
    }

    private inner class ViewPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm), ViewPager.OnPageChangeListener {

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

        override fun getItem(position: Int): Fragment {
            return mFragments?.get(position)!!
        }

        override fun getPageTitle(position: Int): CharSequence {
            return ""
        }

        override fun onPageScrollStateChanged(state: Int) {

        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

        }

        override fun onPageSelected(position: Int) {
            changeUI(position)
        }

    }
}