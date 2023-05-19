package framework.telegram.message.ui.preview

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.RecallMessageEvent
import framework.telegram.message.db.RealmCreator
import framework.telegram.message.event.ScreenShotStateEvent
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.support.BaseActivity
import framework.telegram.support.StatusBarUtil
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.screenshot.ScreenShotsUtils
import framework.telegram.ui.dialog.AppDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.msg_common_preview_activity.*

/**
 * Created by lzh on 19-8-26.
 * INFO:图片+视频的详情页(私密版)
 */
@Route(path = Constant.ARouter.ROUNTE_MSG_PREVIEW_PRIVATE_ACTIVITY)
class PreviewPrivateActivity : BaseActivity() {

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private val mMessageLocalId by lazy { intent.getLongExtra("messageLocalId", -1) }

    private val mChatType by lazy { intent.getIntExtra("chatType", -1) }

    private val mTargetId by lazy { intent.getLongExtra("targetId", -1) }

    private val mIsGroup by lazy { intent.getBooleanExtra("group", false) }

    private val mIsSilentPlay by lazy { intent.getBooleanExtra("isSilentPlay", false) }

    private val mMessageModels = mutableListOf<MessageModel>()
    private val mAdapter: DynamicFragmentAdapter by lazy { DynamicFragmentAdapter(supportFragmentManager) }
    private var curPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.msg_common_preview_activity)
        overridePendingTransition(R.anim.anim_alpha_in, 0)
        initView()
        initData()
        bindEvents()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun isPortraitScreen():Boolean{
        return false
    }

    private fun initView() {
        custom_toolbar.setToolbarColor(R.color.transparent)
        custom_toolbar.setStatuBarColor(R.color.transparent)
        StatusBarUtil.fullscreen(this@PreviewPrivateActivity, true)
    }

    @SuppressLint("CheckResult")
    private fun bindEvents() {
        EventBus.getFlowable(RecallMessageEvent::class.java)
                .bindToLifecycle(this@PreviewPrivateActivity)
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
                                AppDialog.show(this@PreviewPrivateActivity, this@PreviewPrivateActivity) {
                                    title(text = getString(R.string.hint))
                                    message(text = getString(R.string.the_message_you_are_viewing_has_been_deleted))
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

        EventBus.getFlowable(ScreenShotStateEvent::class.java)
                .bindToLifecycle(this@PreviewPrivateActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (mTargetId == it.targetId){
                        if (it.open) {
                            ScreenShotsUtils.startScreenShotsListen(this@PreviewPrivateActivity) {
                                ArouterServiceManager.messageService.sendScreenShotsPackage(mMineUid, mTargetId)
                            }
                        }else{
                            ScreenShotsUtils.stopScreenShotsListen(this@PreviewPrivateActivity)
                        }
                    }
                }
    }

    private fun initData() {
        if (mChatType == ChatModel.CHAT_TYPE_PVT) {
            RealmCreator.executePvtChatTransactionAsync(mMineUid, mTargetId, { realm ->
                val msg = realm.where(MessageModel::class.java)
                        .beginGroup()
                        .equalTo("type", MessageModel.MESSAGE_TYPE_IMAGE)
                        .or()
                        .equalTo("type", MessageModel.MESSAGE_TYPE_VIDEO)
                        .endGroup()
                        .equalTo("id", mMessageLocalId)
                        .findFirst()

                msg?.copyMessage()?.let {
                    mMessageModels.add(it)
                }
            }, {
                setAdapter()
            }, {
                finish()
            })
        }else if (mChatType == ChatModel.CHAT_TYPE_GROUP){
            RealmCreator.executeGroupChatTransactionAsync(mMineUid, mTargetId, { realm ->
                val msg = realm.where(MessageModel::class.java)
                        .beginGroup()
                        .equalTo("type", MessageModel.MESSAGE_TYPE_IMAGE)
                        .or()
                        .equalTo("type", MessageModel.MESSAGE_TYPE_VIDEO)
                        .endGroup()
                        .equalTo("id", mMessageLocalId)
                        .findFirst()

                msg?.copyMessage()?.let {
                    mMessageModels.add(it)
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
        view_page.setCurrentShowView(all_layout)
        mAdapter.notifyDataSetChanged()
    }

    inner class DynamicFragmentAdapter(private val fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            mMessageModels[position].let {
                if (it.type == MessageModel.MESSAGE_TYPE_IMAGE) {
                    return ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_PREVIEW_IMAGE_FRAGMENT)
                            .withInt("chatType", mChatType)
                            .withLong("targetId", mTargetId)
                            .withLong("msgId", it.msgId)
                            .withLong("messageLocalId", it.id)
                            .withString("imageFileBackupUri", it.imageMessageContent.imageFileBackupUri)
                            .withString("imageThumbFileUri", it.imageMessageContent.imageThumbFileUri)
                            .withString("imageFileUri", it.imageMessageContent.imageFileUri)
                            .withString("attachmentKey", it.attachmentKey)
                            .withLong("expireTime", it.expireTime)
                            .withInt("snapchatTime", it.snapchatTime)
                            .withBoolean("private", !mIsGroup)
                            .navigation() as LazyFragment
                } else {
                    val time = if (it.videoMessageContent.videoTime > it.snapchatTime) (it.videoMessageContent.videoTime).toInt() else it.snapchatTime
                    return ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_PREVIEW_VIDEO_FRAGMENT)
                            .withInt("chatType", mChatType)
                            .withLong("targetId", mTargetId)
                            .withLong("messageLocalId", it.id)
                            .withLong("msgId", it.msgId)
                            .withString("videoFileBackupUri", it.videoMessageContent.videoFileBackupUri)
                            .withString("videoFileUri", it.videoMessageContent.videoFileUri)
                            .withString("videoThumbFileBackupUri", it.videoMessageContent.videoThumbFileBackupUri)
                            .withString("videoThumbFileUri", it.videoMessageContent.videoThumbFileUri)
                            .withString("attachmentKey", it.attachmentKey)
                            .withLong("expireTime", it.expireTime)
                            .withInt("snapchatTime", time)
                            .withBoolean("private", !mIsGroup)
                            .withBoolean("isSilentPlay",mIsSilentPlay)
                            .navigation() as LazyFragment
                }
            }
        }

        override fun getCount(): Int {
            return mMessageModels.size
        }
    }


    override fun onPause() {
        super.onPause()
        ScreenShotsUtils.stopScreenShotsListen(this@PreviewPrivateActivity)
    }

    override fun onResume() {
        super.onResume()
        ArouterServiceManager.contactService.getContactInfo(lifecycle(), mTargetId, { contactInfoModel, _ ->
            if (contactInfoModel.isBfScreenshot) {
                ScreenShotsUtils.startScreenShotsListen(this@PreviewPrivateActivity) {
                    ArouterServiceManager.messageService.sendScreenShotsPackage(mMineUid, mTargetId)
                }
            }
        })
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.anim_alpha_out)
    }
}