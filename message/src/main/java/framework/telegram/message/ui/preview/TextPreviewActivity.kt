package framework.telegram.message.ui.preview

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.im.ChatModel
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.RecallMessageEvent
import framework.telegram.message.bridge.event.SnapMessageEvent
import framework.telegram.support.system.event.EventBus
import framework.telegram.ui.selectText.OperationItem
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.msg_chat_preview_text.*


@Route(path = Constant.ARouter.ROUNTE_MSG_PREVIEW_TEXT)
class TextPreviewActivity : AppCompatActivity(){

    private val mText by lazy { intent.getStringExtra("text") }
    private val mMsgId by lazy { intent.getLongExtra("msgId",0L) }
    private val mTargetUid by lazy { intent.getLongExtra("targetUid",0L) }
    private val mCopyable by lazy { intent.getBooleanExtra("copyable",true) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.msg_chat_preview_text)
        overridePendingTransition(R.anim.anim_alpha_in_200, 0)
        initView()
    }

    @SuppressLint("CheckResult")
    private fun initView() {

        if (mCopyable) {
            text_view.init()
            text_view.isShowOperateView(mCopyable)
            val operationItemList = arrayListOf<OperationItem>()
                    .also {
                        val item1 = OperationItem()
                        item1.action = OperationItem.ACTION_COPY
                        item1.name = resources.getString(framework.telegram.ui.R.string.copy)

                        val item2 = OperationItem()
                        item2.action = OperationItem.ACTION_SELECT_ALL
                        item2.name = resources.getString(framework.telegram.ui.R.string.all)

                        val item3 = OperationItem()
                        item3.action = OperationItem.ACTION_CANCEL
                        item3.name = resources.getString(framework.telegram.ui.R.string.cancel)

                        it.add(item1)
                        it.add(item2)
                        it.add(item3)
                    }
            text_view.setOperationItemList(operationItemList)
        }

        text_view.text = mText
        text_view.setOnClickListener {
            finish()
        }
        scroll_view.setOnClickListener {
            finish()
        }
        linear_layout.setOnClickListener { finish() }

        EventBus.getFlowable(SnapMessageEvent::class.java)
                .bindToLifecycle(this@TextPreviewActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.msgId == mMsgId) {
                        finish()
                    }
                }


        EventBus.getFlowable(RecallMessageEvent::class.java)
                .bindToLifecycle(this@TextPreviewActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.chatType == ChatModel.CHAT_TYPE_PVT && it.targetId == mTargetUid) {
                        finish()
                    }
                }

    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.anim_alpha_out)
    }
}