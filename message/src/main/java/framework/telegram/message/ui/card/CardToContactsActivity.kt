package framework.telegram.message.ui.card

/**
 * Created by lzh on 19-7-3.
 * INFO:
 */

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.common.TitleModel.TITLE_HEAD
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT
import framework.telegram.business.bridge.Constant.Search.SEARCH_SHARE_CARD_CONTACTS
import framework.telegram.business.bridge.Constant.Search.SEARCH_TYPE
import framework.telegram.business.bridge.Constant.Search.TARGET_NAME
import framework.telegram.business.bridge.Constant.Search.TARGET_PIC
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.bridge.event.ShareCardEvent
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.ui.card.adapter.CardContactsAdapter
import framework.telegram.support.BaseActivity
import framework.telegram.support.system.event.EventBus
import framework.telegram.ui.recyclerview.sticky.StickyItemDecoration
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import kotlinx.android.synthetic.main.msg_activity_forward_contacts.*
import kotlinx.android.synthetic.main.msg_contacts_item_head2.*
import kotlinx.android.synthetic.main.msg_search.*

@Route(path = Constant.ARouter.ROUNTE_MSG_CARD_SELECT_CONTACTS)
class CardToContactsActivity : BaseActivity() {

    private val mRealm by lazy { ArouterServiceManager.groupService.getContactsRealm() }

    private val mTargetPic by lazy { intent.getStringExtra("targetPic")?:"" }

    private val mTargetName by lazy { intent.getStringExtra("targetName")?:"" }

    private val mModelsList by lazy { ArrayList<MultiItemEntity>() }

    private val mAdapter by lazy { CardContactsAdapter(mTargetPic, mTargetName) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.msg_activity_forward_contacts)

        initTitleBar()
        initList()
        initData()
    }

    private fun initTitleBar() {
        custom_toolbar.showCenterTitle(getString(R.string.select_contact))
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        sticky_head_container.setDataCallback {
            val item = mModelsList.get(it)
            if (item is TitleModel) {
                val title = item.title
                if (getString(R.string.string_star).equals(title)) {
                    text_view_head_name2.text = getString(R.string.string_star_sign)
                } else {
                    text_view_head_name2.text = title
                }
            }
        }
        text_view_search_icon.setOnClickListener {
            ARouter.getInstance().build(ROUNTE_BUS_SEARCH_CONTACT)
                    .withInt(SEARCH_TYPE, SEARCH_SHARE_CARD_CONTACTS)
                    .withString(TARGET_PIC, mTargetPic)
                    .withString(TARGET_NAME, mTargetName).navigation()
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
            dataList.forEach { info ->
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
                .addItemDecoration(StickyItemDecoration(sticky_head_container, TITLE_HEAD))

        EventBus.getFlowable(ShareCardEvent::class.java)
                .bindToLifecycle(this@CardToContactsActivity)
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
            if (it.letter.equals("#")) {
                tmpList2.add(it)
            } else {
                tmpList1.add(it)
            }
        }
        allContacts.clear()
        allContacts.addAll(tmpList1)
        allContacts.addAll(tmpList2)

        val dataList = mutableListOf<MultiItemEntity>()
        if (startContacts.isNotEmpty()) {
            dataList.add(TitleModel(getString(R.string.string_star), TITLE_HEAD, 0))
            dataList.addAll(startContacts)
        }
        var prevSection = ""
        for (i in allContacts.indices) {
            val currentItem = allContacts[i]
            val currentSection = currentItem.letter
            if (prevSection != currentSection) {
                dataList.add(TitleModel(currentSection, TITLE_HEAD, 0))
                prevSection = currentSection ?: ""
            }
            dataList.add(currentItem)
        }
        return dataList
    }
}

