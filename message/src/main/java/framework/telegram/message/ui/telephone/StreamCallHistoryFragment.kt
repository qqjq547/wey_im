package framework.telegram.message.ui.telephone

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.ui.IMultiCheckCallCallback
import framework.telegram.message.ui.IMultiCheckable
import framework.telegram.support.BaseFragment
import framework.telegram.support.tools.ThreadUtils
import kotlinx.android.synthetic.main.msg_fragment_stream_call_history.*
import kotlinx.android.synthetic.main.msg_search.*
import java.util.*


@Route(path = Constant.ARouter.ROUNTE_MSG_STREAM_CALL_HISTORY_FRAGMENT)
class StreamCallHistoryFragment : BaseFragment(), IMultiCheckCallCallback {

    override val fragmentName: String
        get() = "StreamCallHistoryFragment"

    private val mFragmentList = listOf<BaseFragment>(
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_HISTORY_FRAGMENT_ALL).navigation() as BaseFragment,
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_HISTORY_FRAGMENT_UNCALL).navigation() as BaseFragment
    )

    private val mFragmentSwitchView by lazy {
        LayoutInflater.from(context).inflate(R.layout.msg_fragment_stream_call_history_switch, null)
    }

    private val mAllCallHistoryBtn by lazy {
        mFragmentSwitchView.findViewById<TextView>(R.id.btn_all_history)
    }

    private val mUnCallHistoryBtn by lazy {
        mFragmentSwitchView.findViewById<TextView>(R.id.btn_uncall_history)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.msg_fragment_stream_call_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {

        initViewPager()
        initTitleBarAndSearch()

        mAllCallHistoryBtn.setOnClickListener {
            viewpager.currentItem = 0
        }
        mUnCallHistoryBtn.setOnClickListener {
            viewpager.currentItem = 1
        }
    }

    private fun initViewPager() {
        activity?.let {
            viewpager.offscreenPageLimit = 2
            val viewPagerAdapter = ViewPagerAdapter(it.supportFragmentManager)
            viewPagerAdapter.setData(mFragmentList)
            viewpager.adapter = viewPagerAdapter
            viewpager.addOnPageChangeListener(viewPagerAdapter)
            viewpager.currentItem = 0
        }
    }

    private fun initTitleBarAndSearch() {
        custom_toolbar.addCenterView(mFragmentSwitchView)
        custom_toolbar.setToolbarSize(64f)
        custom_toolbar.showLeftTextView(getString(R.string.call), {

        }, 0, {
            it.textSize = 24.toFloat()
            it.typeface = Typeface.DEFAULT_BOLD
            it.setTextColor(resources.getColor(R.color.black))
        })

        custom_toolbar.showRightImageView(R.drawable.msg_icon_new_call, {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_CONTACTS).navigation()
        })

        text_view_search_icon.setOnClickListener {
            ARouter.getInstance().build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT)
                    .withInt(framework.telegram.business.bridge.Constant.Search.SEARCH_TYPE, framework.telegram.business.bridge.Constant.Search.SEARCH_CALL).navigation()
        }
    }

    private fun changeUI(position: Int) {
        if (position == 0) {
            mUnCallHistoryBtn.typeface = Typeface.DEFAULT
            mUnCallHistoryBtn.setBackgroundColor(Color.TRANSPARENT)
            mAllCallHistoryBtn.typeface = Typeface.DEFAULT_BOLD
            mAllCallHistoryBtn.setBackgroundResource(R.drawable.msg_corners_trans_white_6_0)
        } else if (position == 1) {
            mAllCallHistoryBtn.typeface = Typeface.DEFAULT
            mAllCallHistoryBtn.setBackgroundColor(Color.TRANSPARENT)
            mUnCallHistoryBtn.typeface = Typeface.DEFAULT_BOLD
            mUnCallHistoryBtn.setBackgroundResource(R.drawable.msg_corners_trans_white_6_0)
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

        fun getFragments(): List<Fragment>? {
            return mFragments
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

    override fun showCheckMessages() {
        text_view_search_icon.isEnabled = false
        text_view_search_icon.isClickable = false
    }

    override fun clickAllChecked(isChecked: Boolean): Int {
        val currentFragment = (viewpager.adapter as ViewPagerAdapter).getFragments()?.get(viewpager.currentItem)
        if (currentFragment is IMultiCheckCallCallback) {
            val result =  currentFragment.clickAllChecked(isChecked)
            (activity as IMultiCheckable).setAllChecked(isChecked)
            return result
        }
        return  0
    }

    override fun clickBatchDelete() {
        val currentFragment = (viewpager.adapter as ViewPagerAdapter).getFragments()?.get(viewpager.currentItem)
        if (currentFragment is IMultiCheckCallCallback) {
            currentFragment.clickBatchDelete()
        }
    }

    override fun dismissCheckMessages() {
        ThreadUtils.runOnUIThread {
            text_view_search_icon.isEnabled = true
            text_view_search_icon.isClickable = true

            val currentFragment = (viewpager.adapter as ViewPagerAdapter).getFragments()?.get(viewpager.currentItem)
            if (currentFragment is IMultiCheckCallCallback) {
                currentFragment.dismissCheckMessages()
            }
        }
    }
}
