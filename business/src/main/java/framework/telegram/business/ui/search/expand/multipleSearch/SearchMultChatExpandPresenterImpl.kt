package framework.telegram.business.ui.search.expand.multipleSearch

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.text.TextUtils
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.ActivityEvent
import com.umeng.analytics.MobclickAgent
import framework.ideas.common.model.common.SearchChatModel
import framework.ideas.common.model.common.SearchMergeChatModel
import framework.ideas.common.model.common.SearchMoreModel
import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.ui.search.contract.SearchContract
import framework.telegram.business.ui.search.db.Constant.ROW_CHAT_CONTENT
import framework.telegram.business.ui.search.db.Constant.ROW_CHAT_ID
import framework.telegram.business.ui.search.db.Constant.ROW_CHAT_TYPE
import framework.telegram.business.ui.search.db.Constant.ROW_INDEX_ID
import framework.telegram.business.ui.search.db.Constant.ROW_MSG_ID
import framework.telegram.business.ui.search.db.Constant.ROW_MSG_LOCAL_ID
import framework.telegram.business.ui.search.db.Constant.ROW_MSG_TIME
import framework.telegram.business.ui.search.db.Constant.ROW_MSG_TYPE
import framework.telegram.business.ui.search.db.SearchDbManager
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.pinyin.FastPinyin
import framework.telegram.support.tools.language.LocalManageUtil
import framework.telegram.support.tools.language.LocalManageUtil.VI
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.realm.Case
import io.realm.Realm
import java.util.*

class SearchMultChatExpandPresenterImpl : SearchContract.Presenter {
    private var mContext: Context?
    private var mView: SearchContract.View
    private var mObservalbe: Observable<ActivityEvent>

    private var mLastDisposable: Disposable? = null

    constructor(
        view: SearchContract.View,
        context: Context?,
        observable: Observable<ActivityEvent>
    ) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context

        view.setPresenter(this)
    }

    override fun start() {
    }

    @SuppressLint("CheckResult")
    override fun getDataSearchList(
        keyword: String,
        dataList: MutableList<MultiItemEntity>,
        pageNum: Int
    ) {
        mLastDisposable?.dispose()
        if (!TextUtils.isEmpty(keyword)) {
            val myUid = AccountManager.getLoginAccount(AccountInfo::class.java)
                .getUserId()//暂时屏蔽掉 群名字支持 拼音，暂时不去掉
            RealmCreator.executeContactsExTransactionAsync(myUid, { realm ->
                val tempList = mutableListOf<ContactDataModel>()
                val contacts =
                    realm.where(ContactDataModel::class.java)?.equalTo("bfMyContacts", true)
                        ?.equalTo("bfMyBlack", false)?.limit(4)?.beginGroup()
                        ?.like("nickName", "*$keyword*", Case.INSENSITIVE)?.or()
                        ?.like("noteName", "*$keyword*", Case.INSENSITIVE)?.or()
                        ?.like("searchPhone", "*$keyword*", Case.INSENSITIVE)?.or()
                        ?.like("shortNoteName", "*$keyword*", Case.INSENSITIVE)?.or()
                        ?.like("shortNickName", "*$keyword*", Case.INSENSITIVE)?.or()
                        ?.like("identify", "*$keyword*", Case.INSENSITIVE)?.endGroup()?.findAll()

                contacts?.forEach { model ->
                    tempList.add(model.copyContactDataModel())
                }

                if (contacts?.size ?: 0 < 4) {//先查中文，拼音首字母缩写匹配，如果匹配不够4个，再匹配拼音全拼
                    val command =
                        realm.where(ContactDataModel::class.java)?.equalTo("bfMyContacts", true)
                            ?.equalTo("bfMyBlack", false)?.beginGroup()
                            ?.like("searchNickName", "*$keyword*", Case.INSENSITIVE)?.or()
                            ?.like("searchNoteName", "*$keyword*", Case.INSENSITIVE)?.endGroup()
                    contacts?.forEach {
                        command?.notEqualTo("uid", it.uid)
                    }
                    val contacts2 = command?.findAll()
                    contacts2?.forEach {
                        if ((!TextUtils.isEmpty(it.searchNickName) && it.searchNickName.contains(
                                keyword.toUpperCase()
                            ) &&
                                    !TextUtils.isEmpty(it.shortNickName) && it.shortNickName.contains(
                                keyword.first().toUpperCase()
                            ))
                        ) {
                            tempList.add(it.copyContactDataModel())
                        } else if (!TextUtils.isEmpty(it.searchNoteName) && it.searchNoteName.contains(
                                keyword.toUpperCase()
                            ) &&
                            !TextUtils.isEmpty(it.shortNoteName) && it.shortNoteName.contains(
                                keyword.first().toUpperCase()
                            )
                        ) {
                            tempList.add(it.copyContactDataModel())
                        }
                    }
                }

                if (!tempList.isNullOrEmpty()) {
                    dataList.add(
                        TitleModel(
                            mContext?.getString(R.string.contacts_man),
                            Constant.Search.SEARCH_ITEM_TITLE,
                            0
                        )
                    )
                }

                tempList.forEachIndexed { index, model ->
                    if (index < 3) {
                        dataList.add(model.copyContactDataModel())
                    } else if (index == 3) {
                        dataList.add(
                            SearchMoreModel(
                                mContext?.getString(R.string.string_search_more_contact),
                                SearchMoreModel.TITLE_MORE_CONTACTS
                            )
                        )
                    }
                }
            }, {
                RealmCreator.executeChatHistroyTransactionAsync(myUid, { realm ->
                    val tempList = mutableListOf<MultiItemEntity>()
                    val chatModel = realm.where(ChatModel::class.java)
                        ?.equalTo("chaterType", ChatModel.CHAT_TYPE_GROUP)
                        ?.like("chaterName", "*$keyword*", Case.INSENSITIVE)?.limit(4)?.findAll()
                    chatModel?.forEach {
                        tempList.add(it.copyChat())
                    }

                    if (chatModel?.size ?: 0 < 4) {//
                        val command = realm.where(GroupInfoModel::class.java)
                            ?.equalTo("bfAddress", true)
                            ?.like("name", "*$keyword*", Case.INSENSITIVE)
                            ?.limit(4)
                        chatModel?.forEach {
                            command?.notEqualTo("groupId", it.chaterId)
                        }
                        val contacts = command?.findAll()
                        contacts?.forEach {
                            tempList.add(it.copyGroupInfoModel())
                        }
                    }

//                    if (groups?.size?:0 < 4 ){//先查中文，拼音首字母缩写匹配，如果匹配不够4个，再匹配拼音全拼
//                        val command =  realm.where(GroupInfoModel::class.java)
//                                ?.like("searchName", "*$keyword*", Case.INSENSITIVE)
//                        groups?.forEach {
//                            command?.notEqualTo("groupId",it.groupId)
//                        }
//                        val contacts2 =command?.findAll()
//                        contacts2?.forEach {
//                            if (123123it.shortName.contains(keyword.first().toUpperCase()) ){
//                                tempList.add(it)
//                            }
//                        }
//                    }

                    if (!tempList.isNullOrEmpty()) {
                        dataList.add(
                            TitleModel(
                                mContext?.getString(R.string.bus_contacts_ground_chat),
                                Constant.Search.SEARCH_ITEM_TITLE,
                                0,
                                dataList.size > 0
                            )
                        )
                    }

                    tempList.forEachIndexed { index, model ->
                        if (index < 3) {
                            dataList.add(model)
                        } else if (index == 3) {
                            dataList.add(
                                SearchMoreModel(
                                    mContext?.getString(R.string.string_search_more_group),
                                    SearchMoreModel.TITLE_MORE_GROUP
                                )
                            )
                        }
                    }
                    getFileContact(keyword, dataList)
                }, {
                    getDataCall(keyword) { list, hasMore ->
                        if (list.isNotEmpty()) {
                            dataList.add(
                                TitleModel(
                                    mContext?.getString(R.string.string_search_chat),
                                    Constant.Search.SEARCH_ITEM_TITLE,
                                    0,
                                    dataList.size > 0
                                )
                            )
                        }
                        dataList.addAll(list)
                        if (hasMore) {
                            dataList.add(
                                SearchMoreModel(
                                    mContext?.getString(R.string.string_search_more_chat),
                                    SearchMoreModel.TITLE_MORE_CHAT
                                )
                            )
                        }
                        mView.getDataListSuccess(dataList, false)
                    }
                })
            })
        } else {
            mView.getDataListSuccess(mutableListOf(), false)
        }
    }

    private fun getFileContact(keyword: String, dataList: MutableList<MultiItemEntity>) {
        var match = false
        when (LocalManageUtil.getSetLanguageLocale()) {
            Locale.CHINA -> {
                if ("传输助手".contains(keyword)) {
                    match = true
                } else if ("CSZS".contains(keyword.toUpperCase())) {
                    match = true
                } else if ("CHUANSHUZHUSHOU".contains(keyword.toUpperCase())) {
                    if ("CSZS".contains(keyword.first().toUpperCase())) {
                        match = true
                    }
                }
            }
            Locale.TAIWAN -> {
                if ("檔案雙輸".contains(keyword)) {
                    match = true
                } else if ("DASS".contains(keyword.toUpperCase())) {//dàng àn shuāng shū
                    match = true
                } else if ("DANGANSHUANGSHU".contains(keyword.toUpperCase())) {
                    if ("DASS".contains(keyword.first().toUpperCase())) {
                        match = true
                    }
                }
            }
            Locale.ENGLISH -> {
                if ("FileTransfer".contains(keyword)) {
                    match = true
                }
            }
            else -> {
                if ("传输助手".contains(keyword)) {
                    match = true
                } else if ("CSZS".contains(keyword.toUpperCase())) {
                    match = true
                } else if ("CHUANSHUZHUSHOU".contains(keyword.toUpperCase())) {
                    if ("CSZS".contains(keyword.first().toUpperCase())) {
                        match = true
                    }
                }
            }
        }
        if (match) {
            dataList.add(
                TitleModel(
                    mContext?.getString(R.string.string_search_tool),
                    Constant.Search.SEARCH_ITEM_TITLE,
                    0,
                    dataList.size > 0
                )
            )
            //这个是用来显示用的，不会保存的数据库，所以除了必要数据，其他都是默认值，不用纠结他的值
            val contactModel = ContactDataModel.createContact(
                framework.telegram.message.bridge.Constant.Common.FILE_TRANSFER_UID,
                mContext?.getString(R.string.transmission_assistant),
                0,
                "",
                "",
                false,
                false,
                false,
                false,
                false,
                "",
                "",
                "",
                true,
                false,
                0,
                false,
                "",
                false,
                false,
                0,
                false,
                false,
                0,
                false,
                false,
                ""
            )

            dataList.add(contactModel)
        }
    }

    @SuppressLint("CheckResult")
    private fun getDataCall(
        keyword: String,
        call: ((MutableList<SearchMergeChatModel>, Boolean) -> Unit)
    ) {
        val dataList = mutableListOf<SearchMergeChatModel>()
        var hasMore = false
        if (!TextUtils.isEmpty(keyword)) {
            mLastDisposable = Observable.create<Cursor?> {
                it.onNext(
                    SearchDbManager.query(
                        arrayOf(
                            ROW_INDEX_ID,
                            ROW_CHAT_CONTENT,
                            ROW_MSG_TYPE,
                            "COUNT(1) AS count",
                            "MAX($ROW_MSG_TIME) AS time"
                        ),
                        "$ROW_CHAT_CONTENT MATCH ?",
                        arrayOf("$keyword*"),
                        ROW_INDEX_ID,
                        null,
                        "$ROW_MSG_TIME DESC",
                        "4"
                    )!!
                )
                it.onComplete()
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe { cursor ->
                    while (cursor?.moveToNext() == true) {
                        val indexId = cursor.getLong(cursor.getColumnIndex(ROW_INDEX_ID))
                        val content = cursor.getString(cursor.getColumnIndex(ROW_CHAT_CONTENT))
                        val count = cursor.getInt(cursor.getColumnIndex("count"))
                        val type = cursor.getInt(cursor.getColumnIndex(ROW_MSG_TYPE))
                        val data = SearchMergeChatModel(indexId, content, count, type)
                        if (dataList.size <= 2) {
                            dataList.add(data)
                        } else {
                            hasMore = true
                        }
                    }
                    cursor?.close()
                    call.invoke(dataList, hasMore)
                }
        } else {
            call.invoke(dataList, hasMore)
        }
    }

    override fun destroy() {
    }

}


