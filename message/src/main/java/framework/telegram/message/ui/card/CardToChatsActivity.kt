package framework.telegram.message.ui.card

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.customview.getCustomView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.common.TitleModel.TITLE_SELECT_FRIEND
import framework.ideas.common.model.common.TitleModel.TITLE_SELECT_GROUP
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT
import framework.telegram.business.bridge.Constant.Search.SEARCH_SHARE_CARD_CONTACTS_GROUP
import framework.telegram.business.bridge.Constant.Search.SEARCH_TYPE
import framework.telegram.business.bridge.Constant.Search.TARGET_NAME
import framework.telegram.business.bridge.Constant.Search.TARGET_PIC
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.ShareCardEvent
import framework.telegram.message.db.RealmCreator
import framework.telegram.message.manager.SendMessageManager
import framework.telegram.message.ui.card.adapter.CardChatAdapter
import framework.telegram.support.BaseActivity
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.msg_activity_forward_chats.*
import kotlinx.android.synthetic.main.msg_search.*

@Route(path = Constant.ARouter.ROUNTE_MSG_CARD_CHATS)
class CardToChatsActivity : BaseActivity(), RealmChangeListener<RealmResults<ChatModel>> {

    private val mRealm by lazy { RealmCreator.getChatsHistoryRealm(mMineUid) }

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private val mTargetUId by lazy { intent.getLongExtra("targetId", -1) }

    private val mTargetPic by lazy { intent.getStringExtra("targetPic") }

    private val mTargetName by lazy { intent.getStringExtra("targetName") }

    private val mChatModelsList by lazy { ArrayList<MultiItemEntity>() }

    private var mChatModels: RealmResults<ChatModel>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (mTargetUId <= 0) {
            finish()
            return
        }

        setContentView(R.layout.msg_activity_forward_chats)

        initTitleBar()
        initData()
        initListen()
    }

    @SuppressLint("CheckResult")
    private fun initListen() {
        EventBus.getFlowable(ShareCardEvent::class.java)
                .bindToLifecycle(this@CardToChatsActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    shardCard(event.targetId, event.type)
                }
    }

    private fun initTitleBar() {
        custom_toolbar.showCenterTitle(getString(R.string.send_to))
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        text_view_search_icon.setOnClickListener {
            ARouter.getInstance().build(ROUNTE_BUS_SEARCH_CONTACT)
                    .withInt(SEARCH_TYPE, SEARCH_SHARE_CARD_CONTACTS_GROUP)
                    .withString(TARGET_PIC, mTargetPic)
                    .withString(TARGET_NAME, mTargetName).navigation()
        }
    }

    @SuppressLint("CheckResult")
    private fun initData() {
        val adapter = CardChatAdapter()
        adapter.setNewData(mChatModelsList)

        adapter.setOnItemClickListener { _, _, position ->

            val data = adapter.getItem(position)
            when (adapter.getItemViewType(position)) {
                ChatModel.CHAT_TYPE_PVT, ChatModel.CHAT_TYPE_GROUP, ChatModel.CHAT_TYPE_GROUP_NOTIFY -> {
                    data?.let { chatModel ->
                        if (chatModel is ChatModel) {
                            AppDialog.showCustomView(this@CardToChatsActivity, R.layout.common_dialog_share_item, null) {
                                getCustomView().findViewById<AppImageView>(R.id.image_view_icon1).setImageURI(mTargetPic)
                                getCustomView().findViewById<AppImageView>(R.id.image_view_icon2).setImageURI(chatModel.chaterIcon)
                                val name = if (TextUtils.isEmpty(chatModel.chaterName)) chatModel.chaterNickName else chatModel.chaterName
                                getCustomView().findViewById<AppTextView>(R.id.app_text_view_name).text = String.format(getString(R.string.recommend_mat),mTargetName,name)
                                positiveButton(text = getString(R.string.confirm), click = {
                                    shardCard(chatModel.chaterId, chatModel.chaterType)
                                })
                                negativeButton(text = getString(R.string.cancel))
                                title(text = getString(R.string.send_a_card))
                            }

                        }
                    }
                }
                TITLE_SELECT_FRIEND -> {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_CARD_SELECT_CONTACTS)
                            .withString("targetPic", mTargetPic)
                            .withString("targetName", mTargetName)
                            .navigation()
                }
                TITLE_SELECT_GROUP -> {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_CARD_SELECT_GROUP)
                            .withString("targetPic", mTargetPic)
                            .withString("targetName", mTargetName)
                            .navigation()
                }
            }
        }
        recycler_view_history.initMultiTypeRecycleView(LinearLayoutManager(this@CardToChatsActivity), adapter, false)
        recycler_view_history.refreshController().setEnablePullToRefresh(false)

        loadChatHistory()

    }

    @SuppressLint("CheckResult")
    private fun loadChatHistory() {
        Flowable.just<Realm>(mRealm)
                .bindToLifecycle(this@CardToChatsActivity)
                .subscribeOn(AndroidSchedulers.mainThread())
                .map { it.where(ChatModel::class.java)?.equalTo("chaterType", ChatModel.CHAT_TYPE_PVT)?.or()?.equalTo("chaterType", ChatModel.CHAT_TYPE_GROUP)?.sort("isTop", Sort.DESCENDING, "lastMsgTime", Sort.DESCENDING)?.findAllAsync() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    mChatModels = it
                    mChatModels?.addChangeListener(this@CardToChatsActivity)
                }
    }

    override fun onChange(t: RealmResults<ChatModel>) {
        if (!t.isValid)
            return

        if (ActivitiesHelper.isDestroyedActivity(this@CardToChatsActivity)) {
            return
        }

        mChatModelsList.clear()
        mChatModelsList.add(TitleModel(getString(R.string.choosing_friends), TITLE_SELECT_FRIEND, R.drawable.common_contacts_icon_friend))
        mChatModelsList.add(TitleModel(getString(R.string.select_group_chat), TITLE_SELECT_GROUP, R.drawable.common_contacts_icon_group))
        if (t.size > 0) {
            mChatModelsList.add(TitleModel(getString(R.string.recently), TitleModel.TITLE_HEAD, 0))
        }
        t.forEach {
            mChatModelsList.add(it)
        }
        recycler_view_history.recyclerViewController()?.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.removeAllChangeListeners()
        mRealm.close()
    }

    private fun shardCard(targetId: Long, type: Int) {
        if (type == 0) {
            SendMessageManager.sendNameCardMessageToUser(mTargetUId, null, mMineUid, targetId)
        } else {
            SendMessageManager.sendNameCardMessageToGroup(mTargetUId, null, mMineUid, targetId)
        }
        finish()
    }
}
