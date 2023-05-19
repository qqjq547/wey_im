package framework.telegram.message.ui.telephone

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.manager.ReceiveMessageManager
import framework.telegram.message.ui.telephone.adapter.StreamCallContactsAdapter
import framework.telegram.support.BaseActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.recyclerview.sticky.StickyItemDecoration
import framework.telegram.ui.status.QMUIViewBuilder
import framework.telegram.ui.videoplayer.utils.NetworkUtils
import kotlinx.android.synthetic.main.msg_contacts_item_head.*
import kotlinx.android.synthetic.main.msg_search.*
import kotlinx.android.synthetic.main.msg_stream_call_activity_contacts.*
import java.util.*


/**
 *
 */
//TODO QMUIStatusView
@Route(path = Constant.ARouter.ROUNTE_MSG_STREAM_CALL_CONTACTS)
class StreamCallContactsActivity : BaseActivity() {

    private val mAdapter by lazy { StreamCallContactsAdapter() }

    private val mListDatas by lazy { ArrayList<MultiItemEntity>() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.msg_stream_call_activity_contacts)
        initView()
        loadContacts()
    }

    private fun initView() {
        custom_toolbar.showCenterTitle(getString(R.string.the_new_phone))

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        text_view_search_icon.setOnClickListener {
            ARouter.getInstance().build(framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT)
                    .withInt(framework.telegram.business.bridge.Constant.Search.SEARCH_TYPE, framework.telegram.business.bridge.Constant.Search.SEARCH_NEW_CALL).navigation()
        }
        mAdapter.setOnItemChildClickListener { _, view, position ->
            if (view.id == R.id.image_view_audio) {
                val info = mAdapter.data[position] as StreamCallContactsAdapter.ItemData
                val data = info.data as ContactDataModel
                if (NetworkUtils.isAvailable(BaseApp.app) && ReceiveMessageManager.socketIsLogin) {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO).withLong("targetUid", data.uid).withInt("streamType", 0).navigation()
                } else {
                    toast(getString(R.string.socket_is_error))
                }
            } else if (view.id == R.id.image_view_video) {
                val info = mAdapter.data[position] as StreamCallContactsAdapter.ItemData
                val data = info.data as ContactDataModel
                if (NetworkUtils.isAvailable(BaseApp.app) && ReceiveMessageManager.socketIsLogin) {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO).withLong("targetUid", data.uid).withInt("streamType", 1).navigation()
                } else {
                    toast(getString(R.string.socket_is_error))
                }
            }
        }
        mAdapter.setNewData(mListDatas)
        mAdapter.emptyView = QMUIViewBuilder(QMUIViewBuilder.TYPE.EMPTY_VIEW).setContext(getString(R.string.no_call_recently))
                .setEmptyImage(R.drawable.common_icon_empty_data).build(this)
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
                .addItemDecoration(StickyItemDecoration(sticky_head_container, 1))
        index_fast_scroll_recycler_view.refreshController().setEnablePullToRefresh(false)
        text_view_head_name.text = ""

        sticky_head_container.setDataCallback { index ->
            mListDatas.let {
                val data = it[index]
                if (data.itemType == 1) {
                    val title = (data as StreamCallContactsAdapter.ItemData).data as String
                    if (getString(R.string.string_star).equals(title)) {
                        text_view_head_name.text = getString(R.string.string_star_sign)
                    } else {
                        text_view_head_name.text = title
                    }
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun loadContacts() {
        ArouterServiceManager.contactService.getAllContact({
            val tmpList = mutableListOf<ContactDataModel>()
            val tmpStarList = mutableListOf<ContactDataModel>()
            it.forEach { info ->
                if (info.isBfStar) {
                    tmpStarList.add(info.copyContactDataModel())
                }
                tmpList.add(info)
            }
            mListDatas.clear()
            mListDatas.addAll(getSortList(tmpList, tmpStarList))

            mAdapter.setNewData(mListDatas)
        }, {
            finish()
        })
    }

    /**
     * 获取排序的列表
     */
    private fun getSortList(allContacts: MutableList<ContactDataModel>, startContacts: List<ContactDataModel>): MutableList<MultiItemEntity> {
        val tmpList1 = mutableListOf<ContactDataModel>()
        val tmpList2 = mutableListOf<ContactDataModel>()
        allContacts.forEach {
            if (it?.letter.equals("#")) {
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
            dataList.add(StreamCallContactsAdapter.ItemData(getString(R.string.string_star)))
            startContacts.forEach {
                dataList.add(StreamCallContactsAdapter.ItemData(it))
            }
        }
        var prevSection = ""
        for (i in allContacts.indices) {
            val currentItem = allContacts[i]
            val currentSection = currentItem.letter
            if (prevSection != currentSection) {
                dataList.add(StreamCallContactsAdapter.ItemData(currentSection))
                prevSection = currentSection ?: ""
            }
            dataList.add(StreamCallContactsAdapter.ItemData(currentItem))
        }
        return dataList
    }
}
