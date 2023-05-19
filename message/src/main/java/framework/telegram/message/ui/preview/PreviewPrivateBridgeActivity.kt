package framework.telegram.message.ui.preview

import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.ideas.common.model.im.ChatModel
import framework.telegram.message.bridge.Constant
import framework.telegram.support.BaseActivity

/**
 * Created by lzh on 20-2-27.
 * INFO:
 * 这是一个中间activity ，透明的
 * 为了解决<item name="android:windowIsTranslucent">true</item> 与 横竖屏 冲突的问题
 * windowIsTranslucent 为了解决背景透明的问题 ，但会导致activity的横竖屏出现问题，原因是，该activity的横竖屏将设置不成功，会跟随parent 的横竖屏属性。（BaseActivity 设置横竖屏的代码将失效）
 *
 */
@Route(path = Constant.ARouter.ROUNTE_MSG_PREVIEW_PRIVATE_BRIDGE_ACTIVITY)
class PreviewPrivateBridgeActivity:BaseActivity(){

    private val mMessageLocalId by lazy { intent.getLongExtra("messageLocalId", -1) }

    private val mChatType by lazy { intent.getIntExtra("chatType", -1) }

    private val mTargetId by lazy { intent.getLongExtra("targetId", -1) }

    private val mIsGroup by lazy { intent.getBooleanExtra("group", false) }

    private val mIsSilentPlay by lazy { intent.getBooleanExtra("isSilentPlay", false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_PREVIEW_PRIVATE_ACTIVITY)
                .withLong("messageLocalId", mMessageLocalId)
                .withInt("chatType", mChatType)
                .withLong("targetId", mTargetId)
                .withBoolean("group", mIsGroup)
                .withBoolean("isSilentPlay", mIsSilentPlay)
                .navigation()
        finish()

    }
}