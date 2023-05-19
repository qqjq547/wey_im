package framework.telegram.business.ui.group.presenter

import android.content.Context
import com.im.domain.pb.GroupProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.GroupHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.GroupHttpProtocol
import framework.telegram.support.system.network.http.HttpReq
import io.reactivex.Observable

class SameGroupChatsPresenterImpl : SameGroupChatsContract.Presenter {


    private var mContext: Context?
    private var mView: SameGroupChatsContract.View
    private var mObservalbe: Observable<ActivityEvent>

    private var mPageNum = 1

    private var mTargetId = 0L
    private val mPageSize = 20
    private var mList = mutableListOf<GroupProto.GroupBase>()

    constructor(view: SameGroupChatsContract.View,targetId:Long, context: Context?, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        this.mTargetId =targetId
        view.setPresenter(this)
    }

    override fun start() {
        mView.showLoading()
    }

    override fun getFirstDataList() {
        getList(1)
    }

    private fun getList(pageNum:Int){
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .friendCommonGroupList(object : HttpReq<GroupProto.FriendCommonGroupListReq>() {
                    override fun getData(): GroupProto.FriendCommonGroupListReq {
                        return GroupHttpReqCreator.createSameGroupChat(mTargetId,pageNum,mPageSize)
                    }
                }).getResult(mObservalbe,{
                    if (pageNum == 1){
                        mList.clear()
                    }
                    it.groupsList.forEach {bean->
                        mList.add(bean)
                    }
                    if (mList.size>0){
                        mPageNum = pageNum
                        mView.refreshListUI(mList,it.groupsList.size >= mPageSize)
                    }else{
                        mView.showEmpty()
                    }
                }){
                    mView.showError(it.message)
                }
    }

    override fun getDataList(){
        getList(mPageNum+1)

    }
}

