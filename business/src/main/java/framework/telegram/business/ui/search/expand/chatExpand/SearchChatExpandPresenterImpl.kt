package framework.telegram.business.ui.search.expand.chatExpand

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.text.TextUtils
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.common.SearchChatModel
import framework.ideas.common.model.common.SearchMergeChatModel
import framework.ideas.common.model.common.TitleModel
import framework.telegram.business.R
import framework.telegram.business.ui.search.contract.SearchContract
import framework.telegram.business.ui.search.db.Constant.ROW_CHAT_CONTENT
import framework.telegram.business.ui.search.db.Constant.ROW_CHAT_ID
import framework.telegram.business.ui.search.db.Constant.ROW_CHAT_TYPE
import framework.telegram.business.ui.search.db.Constant.ROW_INDEX_ID
import framework.telegram.business.ui.search.db.Constant.ROW_MSG_ID
import framework.telegram.business.ui.search.db.Constant.ROW_MSG_LOCAL_ID
import framework.telegram.business.ui.search.db.Constant.ROW_MSG_TIME
import framework.telegram.business.ui.search.db.Constant.ROW_MSG_TYPE
import framework.telegram.business.ui.search.db.Constant.ROW_SENDER_ID
import framework.telegram.business.ui.search.db.SearchDbManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class SearchChatExpandPresenterImpl : SearchContract.Presenter {
    private var mIndexId = 0L
    private var mContext: Context?
    private var mView: SearchContract.View
    private var mObservalbe: Observable<ActivityEvent>

    private var mTargetName :String?

    private var mMatchCount :Int?

    private var mLastDisposable : Disposable?=null

    constructor(view: SearchContract.View, context: Context?, observable: Observable<ActivityEvent>, indexId: Long, targetName: String?, matchCount: Int?) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        this.mIndexId = indexId
        this.mTargetName = targetName
        this.mMatchCount = matchCount

        view.setPresenter(this)
    }

    override fun start() {
    }

    @SuppressLint("CheckResult")
    override fun getDataSearchList(keyword: String, dataList: MutableList<MultiItemEntity>, pageNum: Int) {
        mLastDisposable?.dispose()
        if (!TextUtils.isEmpty(keyword)){
            getDataCall(keyword){list->
                if (!TextUtils.isEmpty(mTargetName))
                    dataList.add(TitleModel( String.format(mContext?.getString(R.string.string_search_head_title)?:"","\"$mTargetName\"",list.size), TitleModel.TITLE_HEAD, 0))
                dataList.addAll(list)
                mView.getDataListSuccess(dataList,false)
            }
        }else{
            mView.getDataListSuccess(mutableListOf(),false)
        }
    }

    override fun destroy() {
    }

    private fun getDataCall(keyword: String, call: (( List<SearchChatModel>) -> Unit)) {
        val dataList = mutableListOf<SearchChatModel>()
        if (!TextUtils.isEmpty(keyword)){
            mLastDisposable = Observable.create<Cursor?> {
                it.onNext(SearchDbManager.query(arrayOf(ROW_CHAT_ID,ROW_CHAT_TYPE,ROW_CHAT_CONTENT,ROW_MSG_ID,ROW_MSG_LOCAL_ID,ROW_MSG_TIME,ROW_MSG_TYPE,ROW_INDEX_ID,ROW_SENDER_ID)
                        , "$ROW_INDEX_ID = ? AND $ROW_CHAT_CONTENT MATCH ?", arrayOf("$mIndexId","$keyword*"), null, null, "$ROW_MSG_TIME DESC")!!)
                it.onComplete()
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe { cursor ->
                while (cursor?.moveToNext() == true) {
                    val chatId = cursor.getLong(cursor.getColumnIndex(ROW_CHAT_ID))
                    val chatType = cursor.getInt(cursor.getColumnIndex(ROW_CHAT_TYPE))
                    val chatContent = cursor.getString(cursor.getColumnIndex(ROW_CHAT_CONTENT))
                    val msgId = cursor.getLong(cursor.getColumnIndex(ROW_MSG_ID))
                    val msgLocalId = cursor.getLong(cursor.getColumnIndex(ROW_MSG_LOCAL_ID))
                    val msgTime = cursor.getLong(cursor.getColumnIndex(ROW_MSG_TIME))
                    val msgType = cursor.getInt(cursor.getColumnIndex(ROW_MSG_TYPE))
                    val indexId = cursor.getLong(cursor.getColumnIndex(ROW_INDEX_ID))
                    val senderId = cursor.getLong(cursor.getColumnIndex(ROW_SENDER_ID))
                    val searchModel = SearchChatModel(chatId, chatType, chatContent, msgId, msgLocalId, msgTime,msgType,indexId,senderId)
                    dataList.add(searchModel)
                }
                cursor?.close()
                call.invoke(dataList)
            }
        }else{
            call.invoke( dataList)
        }

    }
}

