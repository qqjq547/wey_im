package framework.telegram.message.ui.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.customview.getCustomView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_SEARCH_CONTACT
import framework.telegram.business.bridge.Constant.Search.SEARCH_CARD_CONTACTS
import framework.telegram.business.bridge.Constant.Search.SEARCH_TYPE
import framework.telegram.business.bridge.Constant.Search.TARGET_NAME
import framework.telegram.business.bridge.Constant.Search.TARGET_PIC
import framework.telegram.business.bridge.event.SearchCardEvent
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.ui.telephone.adapter.SelectNameCardContactsAdapter
import framework.telegram.support.BaseActivity
import framework.telegram.support.system.event.EventBus
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.recyclerview.sticky.StickyItemDecoration
import framework.telegram.ui.text.AppTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.msg_contacts_item_head2.*
import kotlinx.android.synthetic.main.msg_search.*
import kotlinx.android.synthetic.main.msg_select_name_card_activity_contacts.*
import java.util.*


/**
 *
 */
@Route(path = Constant.ARouter.ROUNTE_MSG_SELECT_CONTACT_CARD)
class SelectContactsCardActivity : BaseActivity() {

    private val mAdapter by lazy { SelectNameCardContactsAdapter() }

    private val mListDatas by lazy { ArrayList<MultiItemEntity>() }

    private val mTargetName by lazy { intent.getStringExtra("targetName") }
    private val mTargetPic by lazy { intent.getStringExtra("targetPic") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.msg_select_name_card_activity_contacts)
        initView()
        loadContacts()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        custom_toolbar.showCenterTitle(getString(R.string.choose_a_business_card))

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        text_view_search_icon.setOnClickListener {
            ARouter.getInstance().build(ROUNTE_BUS_SEARCH_CONTACT)
                    .withInt(SEARCH_TYPE, SEARCH_CARD_CONTACTS)
                    .withString(TARGET_PIC,mTargetPic)
                    .withString(TARGET_NAME,mTargetName).navigation()
        }

        mAdapter.setOnItemClickListener { _, _, position ->
            val info = mAdapter.data[position] as SelectNameCardContactsAdapter.ItemData
            val data = info.data as ContactDataModel

            AppDialog.showCustomView(this@SelectContactsCardActivity, R.layout.common_dialog_share_item, null) {
                getCustomView().findViewById<AppImageView>(R.id.image_view_icon2).setImageURI(mTargetPic)
                getCustomView().findViewById<AppImageView>(R.id.image_view_icon1).setImageURI(data.icon)
                getCustomView().findViewById<AppTextView>(R.id.app_text_view_name).text = String.format(getString(R.string.recommend_mat),data.displayName,mTargetName)

                positiveButton(text = getString(R.string.confirm), click = {
                    val intent = Intent()
                    intent.putExtra("uid", data.uid)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                })
                negativeButton(text = getString(R.string.cancel))
                title(text = getString(R.string.send_a_card))
            }
        }
        mAdapter.setNewData(mListDatas)
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
        text_view_head_name2.text = ""
        text_view_head_name2.visibility = View.VISIBLE

        sticky_head_container.setDataCallback { index ->
            mListDatas.let {
                val data = it[index]
                if (data.itemType == 1) {
                    val title = (data as SelectNameCardContactsAdapter.ItemData).data as String
                    if (getString(R.string.string_star).equals(title)){
                        text_view_head_name2 .text = getString(R.string.string_star_sign)
                    }else{
                        text_view_head_name2.text =title
                    }
                }
            }
        }

        EventBus.getFlowable(SearchCardEvent::class.java)
                .bindToLifecycle(this@SelectContactsCardActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val intent = Intent()
                    intent.putExtra("uid", it.uid)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
    }

    @SuppressLint("CheckResult")
    private fun loadContacts() {
        ArouterServiceManager.contactService.getAllContact({

            val tmpList = mutableListOf<ContactDataModel>()
            val tmpStarList = mutableListOf<ContactDataModel>()
            it.forEach {info->
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
     * 获取排序的列表 视频聊天
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
            dataList.add(SelectNameCardContactsAdapter.ItemData(getString(R.string.string_star)))
            startContacts.forEach {
                dataList.add(SelectNameCardContactsAdapter.ItemData(it) )
            }
        }
        var prevSection = ""

        for (i in allContacts.indices) {
            val currentItem = allContacts[i]
            val currentSection = currentItem.letter
            if (prevSection != currentSection) {
                dataList.add(SelectNameCardContactsAdapter.ItemData(currentSection))
                prevSection = currentSection ?: ""
            }
            dataList.add(SelectNameCardContactsAdapter.ItemData(currentItem))
        }
        return dataList
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }
}
