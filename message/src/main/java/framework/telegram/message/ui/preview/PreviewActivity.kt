package framework.telegram.message.ui.preview

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.alibaba.android.arouter.facade.annotation.Route
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.RecallMessageEvent
import framework.telegram.message.db.RealmCreator
import framework.telegram.support.BaseActivity
import framework.telegram.support.StatusBarUtil
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.ui.dialog.AppDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.msg_common_preview_activity.*
import android.view.WindowManager
import androidx.core.content.ContextCompat


/**
 * Created by lzh on 19-8-26.
 * INFO:图片+视频的详情页
 */
@Route(path = Constant.ARouter.ROUNTE_MSG_PREVIEW_ACTIVITY)
class PreviewActivity : BaseActivity() {

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private val mMessageLocalId by lazy { intent.getLongExtra("messageLocalId", -1) }

    private val mChatType by lazy { intent.getIntExtra("chatType", -1) }

    private val mTargetId by lazy { intent.getLongExtra("targetId", -1) }

    //  是否进行倒叙排序
    private val mShouldReverse by lazy { intent.getBooleanExtra("shouldReverse", false) }

    private val mIsSilentPlay by lazy { intent.getBooleanExtra("isSilentPlay", false) }

    private val mMessageModels = mutableListOf<MessageModel>()

    private val mAdapter: DynamicFragmentAdapter by lazy { DynamicFragmentAdapter(supportFragmentManager,mMessageModels,mChatType,mTargetId,mIsSilentPlay) }

    private var curPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.msg_common_preview_activity)

//        window.decorView.setBackgroundColor(ContextCompat.getColor(this@PreviewActivity, R.color.white))
//        val rootView = window.decorView.findViewById<View>(android.R.id.content)
//        rootView.setBackgroundColor(ContextCompat.getColor(this@PreviewActivity, R.color.transparent))

        overridePendingTransition(R.anim.anim_alpha_in, 0)
        initView()
        initData()
        bindEvents()

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun isPortraitScreen(): Boolean {
        return false
    }

    private fun initView() {
        StatusBarUtil.fullscreen(this@PreviewActivity, true)

        custom_toolbar.setToolbarColor(R.color.transparent)
        custom_toolbar.setStatuBarColor(R.color.transparent)

//        image_download.setOnClickListener {
//          val fragment = mAdapter.instantiateItem(view_page,view_page.currentItem)
//            if (fragment is DownloadPicturePreviewFragment){
//                fragment.savePicture()
//            }
//        }
    }

    @SuppressLint("CheckResult")
    private fun bindEvents() {
        EventBus.getFlowable(RecallMessageEvent::class.java)
                .bindToLifecycle(this@PreviewActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { recallMsg ->
                    if (mTargetId == recallMsg.targetId) {
                        var recallMsgIndex = -1
                        mMessageModels.forEachIndexed { index, messageModel ->
                            if (messageModel.msgId == recallMsg.msgId) {
                                recallMsgIndex = index
                            }
                        }

                        if (recallMsgIndex > -1) {
                            if (recallMsgIndex == view_page?.currentItem) {
                                val fragment = supportFragmentManager.fragments[recallMsgIndex]
                                if (fragment is LazyFragment) {
                                    fragment.pauseFragment()
                                }
                                AppDialog.show(this@PreviewActivity, this@PreviewActivity) {
                                    title(text = getString(R.string.hint))
                                    message(text = getString(R.string.the_message_has_been_deleted_by_the_other_party))
                                    cancelOnTouchOutside(false)
                                    cancelable(false)

                                    positiveButton(text = getString(R.string.confirm), click = {
                                        finish()
                                    })
                                }
                            } else {
                                val model = mMessageModels[recallMsgIndex]
                                mMessageModels.remove(model)
                                mAdapter.notifyDataSetChanged()
                                if (mMessageModels.isNullOrEmpty()) {
                                    finish()
                                }
                            }
                        }
                    }
                }
    }

    private fun initData() {
        var msgModels: RealmResults<MessageModel>? = null
        if (mChatType == ChatModel.CHAT_TYPE_PVT) {
            RealmCreator.executePvtChatTransactionAsync(mMineUid, mTargetId, { realm ->
                msgModels = realm.where(MessageModel::class.java)
                        .beginGroup()
                        .equalTo("type", MessageModel.MESSAGE_TYPE_IMAGE)
                        .or()
                        .equalTo("type", MessageModel.MESSAGE_TYPE_VIDEO)
                        .endGroup()
                        .lessThanOrEqualTo("snapchatTime", 0)
                        .sort("time", if (mShouldReverse) Sort.DESCENDING else Sort.ASCENDING)
                        .findAll()//
                msgModels?.forEach {
                    val msg = it.copyMessage()
                    if (mMessageLocalId == msg.id) {
                        curPosition = msgModels?.indexOf(it) ?: 0
                    }
                    mMessageModels.add(msg)
                }
            }, {
                setAdapter()
            }, {
                finish()
            })
        } else {
            RealmCreator.executeGroupChatTransactionAsync(mMineUid, mTargetId, { realm ->
                msgModels = realm.where(MessageModel::class.java)
                        .beginGroup()
                        .equalTo("type", MessageModel.MESSAGE_TYPE_IMAGE)
                        .or()
                        .equalTo("type", MessageModel.MESSAGE_TYPE_VIDEO)
                        .endGroup()
                        .lessThanOrEqualTo("snapchatTime", 0)
                        .sort("time", if (mShouldReverse) Sort.DESCENDING else Sort.ASCENDING)
                        .findAll()
                msgModels?.forEach {
                    val msg = it.copyMessage()
                    if (mMessageLocalId == msg.id) {
                        curPosition = msgModels?.indexOf(it) ?: 0
                    }
                    mMessageModels.add(msg)
                }
            }, {
                setAdapter()
            }, {
                finish()
            })
        }
    }

    private fun setAdapter() {
        view_page.offscreenPageLimit = 3
        view_page.adapter = mAdapter
        view_page.currentItem = curPosition
        view_page.setIAnimClose(object : DragViewPager.IAnimClose {
            override fun onPictureClick() {
                finish()
            }

            override fun onPictureRelease(view: View?) {
                finish()
            }
        })
        view_page.setTouchMove (object :DragViewPager.ITouchMove{
            override fun onMove() {//隐藏
                setItemVisiable(curPosition,false)
            }

            override fun onUp() {
                setItemVisiable(curPosition,true)
            }
        })
        view_page.setOnPageChangeListener(object : ViewPager.OnPageChangeListener{
            override fun onPageScrollStateChanged(state: Int) {
                if (state == 0){
                    setItemVisiable(curPosition,true)
                }
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                curPosition = position
            }
        })
        mAdapter.notifyDataSetChanged()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.anim_alpha_out)
    }

    private fun setItemVisiable(curPosition:Int,visiable:Boolean){
        val fragment = mAdapter.instantiateItem(view_page,curPosition)
        if (fragment is DownloadPicturePreviewFragment){
            fragment.setButtonLayout(visiable)
        }else if(fragment is DownloadVideoPreviewFragment){
            fragment.setButtonLayout(visiable)
        }
    }
}