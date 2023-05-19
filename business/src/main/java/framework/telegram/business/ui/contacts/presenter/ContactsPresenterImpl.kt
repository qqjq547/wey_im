package framework.telegram.business.ui.contacts.presenter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.alibaba.android.arouter.launcher.ARouter
import com.trello.rxlifecycle3.RxLifecycle
import com.trello.rxlifecycle3.android.ActivityEvent
import com.trello.rxlifecycle3.android.FragmentEvent
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.service.IContactService
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.ui.contacts.bean.ContactItemBean
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.EMPTY_TITLE
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.ITEM_CONTACT
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.ITEM_CONTACT_REQ
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.ITEM_FOOT
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.ITEM_GROUPS
import framework.telegram.business.ui.contacts.bean.ContactItemBean.Companion.ITEM_HEAD
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.ExpandClass.toast
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults

class ContactsPresenterImpl : ContactsContract.Presenter, RealmChangeListener<RealmResults<ContactDataModel>> {

    private val mContactsList by lazy { ArrayList<ContactItemBean>() }

    private val mRealm by lazy { RealmCreator.getContactsRealm(AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()) }

    private var mContext: Context?
    private var mView: ContactsContract.View
    private var mObservalbe: Observable<FragmentEvent>
    private var mContacts: RealmResults<ContactDataModel>? = null

    constructor(view: ContactsContract.View, context: Context?, observable: Observable<FragmentEvent>) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    @SuppressLint("CheckResult")
    override fun start() {
        Flowable.just<Realm>(mRealm)
                .compose(RxLifecycle.bindUntilEvent(mObservalbe, FragmentEvent.DESTROY))
                .subscribeOn(AndroidSchedulers.mainThread())
                .map { it.where(ContactDataModel::class.java)?.equalTo("bfMyContacts", true)?.and()?.equalTo("bfMyBlack", false)?.sort("letter")?.findAllAsync() }
                .observeOn(AndroidSchedulers.mainThread()).subscribe {
                    mContacts = it
                    mContacts?.addChangeListener(this@ContactsPresenterImpl)
                }
    }

    override fun onChange(t: RealmResults<ContactDataModel>) {
        if (!t.isValid )
            return
        val list = mutableListOf<ContactDataModel>()
        t.forEach {
            if (it.isValid) {
                list.add(it)
            }
        }

        updateData(list)
    }

    override fun destory() {
        mRealm.removeAllChangeListeners()
        mRealm.close()
    }

    override fun getItem(position: Int): ContactItemBean {
        return mContactsList[position]
    }

    private fun updateData(list: MutableList<ContactDataModel>) {
        val tmpList = mutableListOf<ContactItemBean>()
        val tmpStarList = mutableListOf<ContactItemBean>()
        list.forEach { info ->
            if (info.isBfStar) {
                tmpStarList.add(ContactItemBean(ITEM_CONTACT, info))
            }
            tmpList.add(ContactItemBean(ITEM_CONTACT, info))
        }

        mContactsList.clear()
        mContactsList.addAll(getSortList(tmpList, tmpStarList))

        mView.refreshListUI(mContactsList)
    }

    /**
     * 获取排序的列表
     */
    private fun getSortList(allContacts: MutableList<ContactItemBean>, startContacts: List<ContactItemBean>): MutableList<ContactItemBean> {
        val tmpList1 = mutableListOf<ContactItemBean>()
        val tmpList2 = mutableListOf<ContactItemBean>()
        allContacts.forEach {
            if (it.getInfo()?.letter.equals("#")){
                tmpList2.add(it)
            }else{
                tmpList1.add(it)
            }
        }
        allContacts.clear()
        allContacts.addAll(tmpList1)
        allContacts.addAll(tmpList2)

        val dataList = mutableListOf<ContactItemBean>()
        dataList.add(ContactItemBean(ITEM_HEAD, EMPTY_TITLE))
        dataList.add(ContactItemBean(ITEM_CONTACT_REQ, mContext?.getString(R.string.bus_contacts_new_friend)
                ?: ""))
        dataList.add(ContactItemBean(ITEM_GROUPS, mContext?.getString(R.string.bus_contacts_ground_chat)
                ?: ""))

        if (startContacts.isNotEmpty()) {
            dataList.add(ContactItemBean(ITEM_HEAD, mContext?.getString(R.string.string_star)?:""))
            dataList.addAll(startContacts)
        }

        var prevSection = ""
        for (i in allContacts.indices) {
            val currentItem = allContacts[i].getInfo()
            val currentSection = currentItem?.letter
            if (prevSection != currentSection) {
                dataList.add(ContactItemBean(ITEM_HEAD, currentSection ?: ""))
                prevSection = currentSection ?: ""
            }
            dataList.add(ContactItemBean(ITEM_CONTACT, currentItem!!))
        }
        dataList.add(ContactItemBean(ITEM_FOOT,allContacts.size.toString()))
        return dataList
    }

    override fun setStarFriend(uid: Long, star: Boolean) {
        ArouterServiceManager.contactService.setContactStar(null, uid, star) {
            mContext?.toast(it)
        }
    }
}

