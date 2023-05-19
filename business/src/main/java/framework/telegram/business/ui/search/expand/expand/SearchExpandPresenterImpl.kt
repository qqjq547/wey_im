package framework.telegram.business.ui.search.expand.expand

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.text.TextUtils
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.common.SearchChatModel
import framework.ideas.common.model.common.SearchMergeChatModel
import framework.ideas.common.model.common.SearchMoreModel
import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.Search.SEARCH_CHAT_EXPAND
import framework.telegram.business.bridge.Constant.Search.SEARCH_CONTACTS_EXPAND
import framework.telegram.business.bridge.Constant.Search.SEARCH_GROUP_EXPAND
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
import framework.telegram.support.account.AccountManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.realm.Case

class SearchExpandPresenterImpl : SearchContract.Presenter {
    private var mContext: Context?
    private var mView: SearchContract.View
    private var mObservalbe: Observable<ActivityEvent>
    private var mSearchType = 0

    private var mLastDisposable : Disposable?=null

    constructor(view: SearchContract.View, context: Context?, observable: Observable<ActivityEvent>,searchType:Int) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        this.mSearchType = searchType
        view.setPresenter(this)
    }

    override fun start() {
    }

    @SuppressLint("CheckResult")
    override fun getDataSearchList(keyword: String, dataList: MutableList<MultiItemEntity>, pageNum: Int) {
        mLastDisposable?.dispose()
        if (!TextUtils.isEmpty(keyword)){
            when(mSearchType){
                SEARCH_CHAT_EXPAND->{
                    getChatData(keyword){list->
                        dataList.addAll(list)
                        mView.getDataListSuccess(dataList,false)
                    }
                }
                SEARCH_GROUP_EXPAND->{
                    getGroupData(keyword){list->
                        dataList.addAll(list)
                        mView.getDataListSuccess(dataList,false)
                    }
                }
                SEARCH_CONTACTS_EXPAND->{
                    getContactData(keyword){list->
                        dataList.addAll(list)
                        mView.getDataListSuccess(dataList,false)
                    }
                }
            }
        }else{
            mView.getDataListSuccess(mutableListOf(),false)
        }
    }

    override fun destroy() {
    }

    private fun getChatData(keyword: String, call: (( List<MultiItemEntity>) -> Unit)) {
        val dataList = mutableListOf<MultiItemEntity>()
        if (!TextUtils.isEmpty(keyword)){
            mLastDisposable = Observable.create<Cursor?> {
                it.onNext(SearchDbManager.query(arrayOf(ROW_INDEX_ID,  ROW_CHAT_CONTENT,ROW_MSG_TYPE, "COUNT(1) AS count","MAX($ROW_MSG_TIME) AS time")
                        , "$ROW_CHAT_CONTENT MATCH ?", arrayOf("$keyword*"), ROW_INDEX_ID, null,  "$ROW_MSG_TIME DESC")!!)
                it.onComplete()
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe { cursor ->
                while (cursor?.moveToNext() == true) {
                    val indexId = cursor.getLong(cursor.getColumnIndex(ROW_INDEX_ID))
                    val content = cursor.getString(cursor.getColumnIndex(ROW_CHAT_CONTENT))
                    val count = cursor.getInt(cursor.getColumnIndex("count"))
                    val type = cursor.getInt(cursor.getColumnIndex(ROW_MSG_TYPE))
                    val data = SearchMergeChatModel(indexId,content,count,type)
                    dataList.add(data)
                }
                cursor?.close()
                if (dataList.size > 0){
                    dataList.add(0,TitleModel(mContext?.getString(R.string.string_search_chat), Constant.Search.SEARCH_ITEM_TITLE, 0))
                }
                call.invoke(dataList)
            }
        }else{
            call.invoke( dataList)
        }
    }

    private fun getContactData(keyword: String, call: (( List<MultiItemEntity>) -> Unit)){
        val dataList = mutableListOf<MultiItemEntity>()
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeContactsExTransactionAsync(myUid, { realm ->
            val tempList = mutableListOf<ContactDataModel>()
            val contacts = realm.where(ContactDataModel::class.java)?.
                    equalTo("bfMyContacts", true)?.
                    equalTo("bfMyBlack", false)?.
                    beginGroup()?.
                    like("nickName", "*$keyword*", Case.INSENSITIVE)?.
                    or()?.
                    like("noteName", "*$keyword*", Case.INSENSITIVE)?.
                    or()?.
                    like("searchPhone", "*$keyword*", Case.INSENSITIVE)?.
                    or()?.
                    like("shortNoteName", "*$keyword*", Case.INSENSITIVE)?.
                    or()?.
                    like("shortNickName", "*$keyword*", Case.INSENSITIVE)?.
                    or()?.
                    like("identify", "*$keyword*", Case.INSENSITIVE)?.
                    endGroup()?.findAll()
            contacts?.forEach{ model ->
                tempList.add(model.copyContactDataModel())
            }

            val command =  realm.where(ContactDataModel::class.java)?.
                    equalTo("bfMyContacts", true)?.
                    equalTo("bfMyBlack", false)?.
                    beginGroup()?.
                    like("searchNickName", "*$keyword*", Case.INSENSITIVE)?.
                    or()?.
                    like("searchNoteName", "*$keyword*", Case.INSENSITIVE)?.
                    endGroup()
            contacts?.forEach {
                command?.notEqualTo("uid",it.uid)
            }
            val contacts2 =command?.findAll()
            contacts2?.forEach {
                if ((!TextUtils.isEmpty(it.searchNickName) && it.searchNickName.contains(keyword.toUpperCase()) &&
                                !TextUtils.isEmpty(it.shortNickName) && it.shortNickName.contains(keyword.first().toUpperCase()))){
                    tempList.add(it.copyContactDataModel())
                }else if(!TextUtils.isEmpty(it.searchNoteName) && it.searchNoteName.contains(keyword.toUpperCase()) &&
                        !TextUtils.isEmpty(it.shortNoteName) && it.shortNoteName.contains(keyword.first().toUpperCase())){
                    tempList.add(it.copyContactDataModel())
                }
            }

            if (!tempList.isNullOrEmpty()) {
                dataList.add(TitleModel(mContext?.getString(R.string.contacts_man), Constant.Search.SEARCH_ITEM_TITLE, 0))
            }
            tempList.forEach { model ->
                dataList.add(model.copyContactDataModel())
            }
            }, {
                    call.invoke( dataList)
            }
        )
    }

    private fun getGroupData(keyword: String, call: (( List<MultiItemEntity>) -> Unit)){
        val dataList = mutableListOf<MultiItemEntity>()
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeChatHistroyTransactionAsync(myUid, { realm ->
            val tempList = mutableListOf<MultiItemEntity>()
            val chatModel = realm.where(ChatModel::class.java)?.
                    equalTo("chaterType", ChatModel.CHAT_TYPE_GROUP)?.
                    like("chaterName", "*$keyword*", Case.INSENSITIVE)?.
                    limit(4)?.
                    findAll()
            chatModel?.forEach {
                tempList.add(it.copyChat())
            }

            if (chatModel?.size?:0 < 4 ){//
                val command =  realm.where(GroupInfoModel::class.java)
                        ?.equalTo("bfAddress", true)
                        ?.like("name", "*$keyword*", Case.INSENSITIVE)
                        ?.limit(4)
                chatModel?.forEach {
                    command?.notEqualTo("groupId",it.chaterId)
                }
                val contacts =command?.findAll()
                contacts?.forEach {
                    tempList.add(it.copyGroupInfoModel())
                }
            }

//            if (groups?.size?:0 < 4 ){//先查中文，拼音首字母缩写匹配，如果匹配不够4个，再匹配拼音全拼
//                val command =  realm.where(GroupInfoModel::class.java)
//                        ?.like("searchName", "*$keyword*", Case.INSENSITIVE)
//                groups?.forEach {
//                    command?.notEqualTo("groupId",it.groupId)
//                }
//                val contacts2 =command?.findAll()
//                contacts2?.forEach {
//                    if (TextUtilIsEmit.shortName.contains(keyword.toUpperCase()) ){
//                        tempList.add(it)
//                    }
//                }
//            }

            if (!tempList.isNullOrEmpty()) {
                dataList.add(TitleModel(mContext?.getString(R.string.bus_contacts_ground_chat), Constant.Search.SEARCH_ITEM_TITLE, 0, dataList.size>0))
            }

            tempList.forEach {model ->
                dataList.add(model)
            }

        },{
            call.invoke( dataList)
        })
    }
}

