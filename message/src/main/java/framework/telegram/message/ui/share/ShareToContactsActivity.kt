package framework.telegram.message.ui.share

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.event.ShareFinishEvent
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_SHARE_SELECT_CONTACTS
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.ui.share.adapter.ShareContactsAdapter
import framework.telegram.support.BaseActivity
import framework.telegram.support.system.event.EventBus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import kotlinx.android.synthetic.main.msg_activity_share_contacts.*
import kotlinx.android.synthetic.main.msg_search.*

@Route(path = ROUNTE_MSG_SHARE_SELECT_CONTACTS)
class ShareToContactsActivity: BaseActivity(){

    private val mRealm by lazy { ArouterServiceManager.groupService.getContactsRealm() }

    private val mModelsList by lazy { ArrayList<MultiItemEntity>() }

    private val mAdapter by lazy { ShareContactsAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.msg_activity_share_contacts)

        initTitleBar()
        initList()
        initData()
    }

    private fun initTitleBar() {
        custom_toolbar.showCenterTitle(getString(R.string.send_to))
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        text_view_search_icon.setOnClickListener {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT)
                    .withInt(Constant.Search.SEARCH_TYPE, Constant.Search.SEARCH_SHARE_CONTACTS).navigation()
        }
    }

    @SuppressLint("CheckResult")
    private fun initData() {
        val dataList = ArrayList<ContactDataModel>()
        mRealm.executeTransactionAsync(Realm.Transaction { realm ->
            val models = realm.where(ContactDataModel::class.java)?.equalTo("bfMyContacts", true)?.sort("letter")?.findAll()
            models?.forEach {
                dataList.add(it.copyContactDataModel())
            }
        }, Realm.Transaction.OnSuccess {
            val tmpList = mutableListOf<ContactDataModel>()
            val tmpStarList = mutableListOf<ContactDataModel>()
            dataList.forEach {info->
                if (info.isBfStar) {
                    tmpStarList.add(info.copyContactDataModel())
                }
                tmpList.add(info)
            }
            mModelsList.clear()
            mModelsList.addAll(getSortList(tmpList, tmpStarList))
            mAdapter.setNewData(mModelsList)
        })
    }

    @SuppressLint("CheckResult")
    private fun initList() {
        index_fast_scroll_recycler_view.initIndexFastScrollRecyclerView(
                LinearLayoutManager(this),
                mAdapter,
                false)
        index_fast_scroll_recycler_view.indexFastScrollController()
                .setIndexBarColor(this.resources.getColor(R.color.white))
                .setIndexBarTextColor(this.resources.getColor(R.color.a2a4a7))
                .setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
                .setIndexbarMargin(0f)
                .setIndexTextSize(11)

        EventBus.getFlowable(ShareFinishEvent::class.java)
                .bindToLifecycle(this@ShareToContactsActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    finish()
                }

    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.removeAllChangeListeners()
        mRealm.close()
    }

    /**
     * 获取排序的列表
     */
    private fun getSortList(allContacts: MutableList<ContactDataModel>, startContacts: List<ContactDataModel>): MutableList<MultiItemEntity> {
        val tmpList1 = mutableListOf<ContactDataModel>()
        val tmpList2 = mutableListOf<ContactDataModel>()
        allContacts.forEach {
            if (it?.letter.equals("#")){
                tmpList2.add(it)
            }else{
                tmpList1.add(it)
            }
        }
        allContacts.clear()
        allContacts.addAll(tmpList1)
        allContacts.addAll(tmpList2)

        val dataList = mutableListOf<MultiItemEntity>()
        if (startContacts.isNotEmpty()) {
            dataList.add(TitleModel(getString(R.string.string_star), TitleModel.TITLE_HEAD, 0))
            dataList.addAll(startContacts)
        }
        var prevSection = ""

        for (i in allContacts.indices) {
            val currentItem = allContacts[i]
            val currentSection = currentItem.letter
            if (prevSection != currentSection) {
                dataList.add(TitleModel(currentSection, TitleModel.TITLE_HEAD, 0))
                prevSection = currentSection ?: ""
            }
            dataList.add(currentItem)
        }
        return dataList
    }


}

