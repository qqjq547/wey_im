package framework.telegram.business.ui.contacts

import android.annotation.SuppressLint
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.ContactsProto
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.contacts.ContactReqModel
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.contacts.adapter.ContactReqListAdapter
import framework.telegram.business.ui.contacts.presenter.ContactReqListContract
import framework.telegram.business.ui.contacts.presenter.ContactReqListPresenterImpl
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.ui.menu.FloatMenu
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.bus_recycler_inside_linear_with_toolbar.*

/**
 * Created by lzh on 19-5-27.
 * INFO:新朋友
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_JOIN_CONTACTS_REQ_LIST)
class ContactReqListActivity : BaseBusinessActivity<ContactReqListContract.Presenter>(), ContactReqListContract.View, RealmChangeListener<RealmResults<ContactReqModel>> {

    private val mAdapter: ContactReqListAdapter by lazy {
        ContactReqListAdapter({
            mPresenter?.makeFriend(it, ContactsProto.ContactsOperator.ADD_REQ)
        },{ type, recordId ->
            if (type == 1) {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACT_DETAIL)
                        .withLong(Constant.ARouter_Key.KEY_TARGET_UID, recordId).navigation()
            } else {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACTS_UN_AUDIT_FRIEND_DETAIL)
                        .withLong(Constant.ARouter_Key.KEY_TARGET_UID, recordId).navigation()
            }
        },{  recordId ->
            val floatMenu = FloatMenu(this@ContactReqListActivity)
            floatMenu.items(getString(R.string.delete))
            floatMenu.show(common_recycler.popPoint)
            floatMenu.setOnItemClickListener { _, text ->
                if (text == getString(R.string.delete)) {
                    mPresenter?.makeFriend(recordId, ContactsProto.ContactsOperator.DEL_REQ)
                }
            }

        }
        )
    }

    override fun getLayoutId() = R.layout.bus_recycler_inside_linear_with_toolbar

    override fun initView() {
        custom_toolbar.showCenterTitle(getString(R.string.bus_contacts_new_friend))
    }

    private val mRealm by lazy { RealmCreator.getContactReqsRealm(AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()) }
    private var mContactReqs: RealmResults<ContactReqModel>? = null

    override fun onDestroy() {
        super.onDestroy()
        mRealm.removeAllChangeListeners()
        mRealm.close()
    }

    @SuppressLint("CheckResult")
    override fun initListen() {
        common_recycler.initSingleTypeRecycleView(LinearLayoutManager(this), mAdapter, false)
        common_recycler.emptyController().setEmpty()

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        common_recycler.refreshController().setEnablePullToRefresh(false)
    }

    @SuppressLint("CheckResult")
    override fun initData() {
        Flowable.just<Realm>(mRealm)
                .bindToLifecycle(this@ContactReqListActivity)
                .subscribeOn(AndroidSchedulers.mainThread())
                .map { it.where(ContactReqModel::class.java)?.sort("modifyTime", Sort.DESCENDING)?.findAllAsync() }
                .observeOn(AndroidSchedulers.mainThread()).subscribe {
                    mContactReqs = it
                    mContactReqs?.addChangeListener(this@ContactReqListActivity)
                }

        ContactReqListPresenterImpl(this, this, lifecycle()).start()
    }

    override fun onChange(t: RealmResults<ContactReqModel>) {
        if (!t.isValid )
            return

        if (ActivitiesHelper.isDestroyedActivity(this@ContactReqListActivity)) {
            return
        }

        val list = mutableListOf<ContactReqModel>()
        t.forEach {
            list.add(it)
        }
        mPresenter?.updateData(list)
    }

    override fun showLoading() {

    }

    override fun refreshListUI(list: MutableList<Any>) {
        common_recycler.itemController().setNewData(list)
        common_recycler.refreshController().refreshComplete()
    }

    override fun showEmpty() {
    }

    override fun showError(errStr: String?) {
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as ContactReqListContract.Presenter
    }

    override fun isActive(): Boolean = true

}