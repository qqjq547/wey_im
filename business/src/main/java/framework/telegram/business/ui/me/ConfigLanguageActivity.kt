package framework.telegram.business.ui.me

import android.content.ServiceConnection
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.facebook.common.util.UriUtil
import com.trello.rxlifecycle3.RxLifecycle
import com.trello.rxlifecycle3.android.ActivityEvent
import com.trello.rxlifecycle3.android.FragmentEvent
import framework.ideas.common.db.RealmCreatorManager
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.InfoContract
import framework.telegram.support.ActivityManager
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.language.LocalManageUtil
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.bus_me_activity_config_language.*
import kotlinx.android.synthetic.main.bus_me_activity_config_language.custom_toolbar

@Route(path = Constant.ARouter.ROUNTE_BUS_ME_CONFIG_LANGUAGE)
class ConfigLanguageActivity : BaseBusinessActivity<InfoContract.Presenter>() {

    override fun getLayoutId() = R.layout.bus_me_activity_config_language

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.language))

        updateView()
    }

    private fun updateView() {
        iv_follow_system.setImageResource(0)
        iv_zh.setImageResource(0)
        iv_zh_t.setImageResource(0)
        iv_en.setImageResource(0)
        iv_thai.setImageResource(0)
        iv_vi.setImageResource(0)

        iv_mx.setImageResource(0)
        iv_in.setImageResource(0)
        iv_br.setImageResource(0)
        iv_tr.setImageResource(0)

        when(LocalManageUtil.getSelectLanguage()){
            LocalManageUtil.FOLLOW_SYSTEM->{
                iv_follow_system.setImageResource(R.drawable.common_contacts_icon_check_selected)
            }
            LocalManageUtil.SIMPLIFIED_CHINESE->{
                iv_zh.setImageResource(R.drawable.common_contacts_icon_check_selected)
            }
            LocalManageUtil.TRADITIONAL_CHINESE->{
                iv_zh_t.setImageResource(R.drawable.common_contacts_icon_check_selected)
            }
            LocalManageUtil.ENGLISH->{
                iv_en.setImageResource(R.drawable.common_contacts_icon_check_selected)
            }
            LocalManageUtil.THAI->{
                iv_thai.setImageResource(R.drawable.common_contacts_icon_check_selected)
            }
            LocalManageUtil.VI->{
                iv_vi.setImageResource(R.drawable.common_contacts_icon_check_selected)
            }

            LocalManageUtil.ES_MX->{
                iv_mx.setImageResource(R.drawable.common_contacts_icon_check_selected)
            }

            LocalManageUtil.HI_IN->{
                iv_in.setImageResource(R.drawable.common_contacts_icon_check_selected)
            }


            LocalManageUtil.PT_BR->{
                iv_br.setImageResource(R.drawable.common_contacts_icon_check_selected)
            }

            LocalManageUtil.TR_TR->{
                iv_tr.setImageResource(R.drawable.common_contacts_icon_check_selected)
            }
        }
    }

    override fun initListen() {
        fl_follow_system.setOnClickListener {
            switchLanguage(LocalManageUtil.FOLLOW_SYSTEM)
        }
        fl_zh.setOnClickListener {
            switchLanguage(LocalManageUtil.SIMPLIFIED_CHINESE)
        }
        fl_zh_t.setOnClickListener {
            switchLanguage(LocalManageUtil.TRADITIONAL_CHINESE)
        }
        fl_en.setOnClickListener {
            switchLanguage(LocalManageUtil.ENGLISH)
        }
        fl_thai.setOnClickListener {
            switchLanguage(LocalManageUtil.THAI)
        }
        fl_vi.setOnClickListener {
            switchLanguage(LocalManageUtil.VI)
        }

        fl_mx.setOnClickListener {
            switchLanguage(LocalManageUtil.ES_MX)
        }

        fl_in.setOnClickListener {
            switchLanguage(LocalManageUtil.HI_IN)
        }

        fl_br.setOnClickListener {
            switchLanguage(LocalManageUtil.PT_BR)
        }

        fl_tr.setOnClickListener {
            switchLanguage(LocalManageUtil.TR_TR)
        }
    }

    private fun switchLanguage(language:Int){
        if(language==LocalManageUtil.getSelectLanguage()){
            finish()
            return
        }
        LocalManageUtil.saveSelectLanguage(this, language)
        updateView()
        ActivityManager.recreateAllOtherActivity(this@ConfigLanguageActivity)

        updateChats()
        updateGroupNotify()
        finish()
    }

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }
    private val mRealm by lazy { RealmCreator.getChatsHistoryRealm(mMineUid) }
    /**
     * 更新所有会话的最后一条消息
     */
    private fun updateChats() {
        Flowable.just<Realm>(mRealm)
                .compose(RxLifecycle.bindUntilEvent(lifecycle(), ActivityEvent.DESTROY))
                .subscribeOn(AndroidSchedulers.mainThread())
                .map { it.where(ChatModel::class.java)?.sort("isTop", Sort.DESCENDING, "lastMsgTime", Sort.DESCENDING)?.findAllAsync() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    updateChatsStep2(it)
                }
    }

    private fun updateChatsStep2(chatModels: RealmResults<ChatModel>?){
        chatModels?.forEach { chatModelIem->
            if (chatModelIem.chaterType!=ChatModel.CHAT_TYPE_GROUP_NOTIFY){
                ArouterServiceManager.messageService.findChatLastMsg(chatModelIem.chaterType,mMineUid, chatModelIem.chaterId, {
                    ArouterServiceManager.messageService.updateChatLastMsg(mMineUid, chatModelIem.chaterType, chatModelIem.chaterId, it, changeTime = false)
                })
            }
        }
    }

    /**
     * 查询聊天记录中有没有群通知，有就要更新
     */
    fun updateGroupNotify() {
        ArouterServiceManager.messageService.executeChatsHistoryTransactionAsync(mMineUid, { realm ->
            val chat = realm.where(ChatModel::class.java).equalTo("chaterType", ChatModel.CHAT_TYPE_GROUP_NOTIFY).and().findFirst()
            if (chat != null) {
                chat.chaterName = BaseApp.app.getString(R.string.group_of_notice)
                realm.copyToRealmOrUpdate(chat)
            }
        }, null, null)
    }

    override fun initData() {
    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.removeAllChangeListeners()
        mRealm.close()
    }
}
