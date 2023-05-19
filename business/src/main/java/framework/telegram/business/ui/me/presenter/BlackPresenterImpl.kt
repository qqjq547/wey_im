package framework.telegram.business.ui.me.presenter

import android.annotation.SuppressLint
import android.content.Context
import com.alibaba.android.arouter.launcher.ARouter
import com.trello.rxlifecycle3.RxLifecycle
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.db.RealmCreator
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.ExpandClass.toast
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults

class BlackPresenterImpl : BlackContract.Presenter, RealmChangeListener<RealmResults<ContactDataModel>> {

    private val mContext: Context
    private val mView: BlackContract.View
    private val mViewObservalbe: Observable<ActivityEvent>

    private val mUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }
    private val mRealm by lazy { RealmCreator.getContactsRealm(mUid) }
    private var mContactModels: RealmResults<ContactDataModel>? = null

    constructor(view: BlackContract.View, context: Context, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {
        // 同步黑名单
        ArouterServiceManager.contactService.syncAllBlackContact(mViewObservalbe, {
            loadData()
        }, {
            mView.showErrMsg(mContext.getString(R.string.failed_to_get_blacklist))
        })
    }

    override fun setBlack(isBlack: Boolean,targetUid:Long) {
        ArouterServiceManager.contactService.setContactBlack(null, targetUid, isBlack, {
            mView.showBlackInfo(isBlack)
        }) {
            mContext?.toast(it)
        }
    }

    @SuppressLint("CheckResult")
    private fun loadData() {
        Flowable.just<Realm>(mRealm)
                .compose(RxLifecycle.bindUntilEvent(mViewObservalbe, ActivityEvent.DESTROY))
                .subscribeOn(AndroidSchedulers.mainThread())
                .map { it.where(ContactDataModel::class.java)?.equalTo("bfMyBlack", true)?.findAllAsync() }
                .observeOn(AndroidSchedulers.mainThread()).subscribe {
                    mContactModels = it
                    mContactModels?.addChangeListener(this@BlackPresenterImpl)
                }
    }

    override fun onChange(t: RealmResults<ContactDataModel>) {
        if (!t.isValid)
            return

        val listDatas = mutableListOf<ContactDataModel>()
        for (i in t.indices) {
            if (t[i] != null) {
                listDatas.add(t[i]!!)
            }
        }

        mView.showData(listDatas)
    }

    override fun destory() {
        mRealm.removeAllChangeListeners()
        mRealm.close()
    }
}