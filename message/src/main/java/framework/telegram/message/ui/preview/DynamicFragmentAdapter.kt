package framework.telegram.message.ui.preview

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.alibaba.android.arouter.launcher.ARouter
import framework.ideas.common.model.im.MessageModel
import framework.telegram.message.bridge.Constant
import java.util.ArrayList

/**
 * Created by yanggl on 2019/11/8 13:37
 */
class DynamicFragmentAdapter(fm: FragmentManager,
                             private val mMessageModels:MutableList<MessageModel>,
                             private val mChatType:Int,
                             private val mTargetId:Long,
                             private val mIsSilentPlay:Boolean) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        mMessageModels[position].let {
            if (it.type == MessageModel.MESSAGE_TYPE_IMAGE) {
                return  ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_PREVIEW_IMAGE_FRAGMENT)
                        .withInt("chatType", mChatType)
                        .withLong("targetId", mTargetId)
                        .withLong("messageLocalId", it.id)
                        .withLong("msgId", it.msgId)
                        .withString("imageFileBackupUri", it.imageMessageContent.imageFileBackupUri)
                        .withString("imageThumbFileUri", it.imageMessageContent.imageThumbFileUri)
                        .withString("imageFileUri", it.imageMessageContent.imageFileUri)
                        .withString("attachmentKey", it.attachmentKey)
                        .navigation() as DownloadPicturePreviewFragment
            } else {
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
                        .withBoolean("isSilentPlay",mIsSilentPlay)
                        .navigation() as DownloadVideoPreviewFragment
            }
        }
    }

    override fun getCount(): Int {
        return mMessageModels.size
    }
}