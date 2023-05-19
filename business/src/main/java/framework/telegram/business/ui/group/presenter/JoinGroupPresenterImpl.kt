package framework.telegram.business.ui.group.presenter

import android.content.Context
import com.im.domain.pb.CommonProto
import com.im.domain.pb.GroupProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant.Result.QR_CODE_ERROR
import framework.telegram.business.http.HttpException

import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.GroupHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.GroupHttpProtocol
import framework.telegram.support.system.network.http.HttpReq
import io.reactivex.Observable

class JoinGroupPresenterImpl : JoinGroupContract.Presenter {

    private val mContext: Context?
    private val mView: JoinGroupContract.View
    private val mObservalbe: Observable<ActivityEvent>
    private val mGroupId: Long
    private val mQrCode: String
    private val mIDCode:String
    private val mAddToken:String

    private var mGroupBase: GroupProto.GroupBase? = null

    constructor(view: JoinGroupContract.View, context: Context?, observable: Observable<ActivityEvent>, groupId: Long, qrCode: String,idCode :String, addToken:String) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        this.mGroupId = groupId
        this.mQrCode = qrCode
        this.mIDCode = idCode
        this.mAddToken = addToken
        view.setPresenter(this)
    }

    override fun start() {
        getDataDetail(mGroupId, mQrCode,mIDCode)
    }

    override fun getDataDetail(groupId: Long, qrCode: String,idCode: String) {
        mView.showLoading()
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .groupDetailFromQrCode(object : HttpReq<GroupProto.GroupDetailFromQrCodeReq>() {
                    override fun getData(): GroupProto.GroupDetailFromQrCodeReq {
                        return GroupHttpReqCreator.createGroupDetailFromQrCodeReq(groupId, qrCode,idCode)
                    }
                })
                .getResult(mObservalbe, {
                    mView.dissmissLoading()

                    mGroupBase = it.groupBase
                    if (it.bfMember) {
                        mView.jumpToChat()
                    } else {
                        mView.refreshUI(it.groupBase,it.groupBase.bfBanned)
                    }

                }, {
                    mView.dissmissLoading()
                    if (it is HttpException && it.errCode == QR_CODE_ERROR) {
                        mView.showError(it.errMsg)
                        mView.destory()
                    } else {
                        //请求失败
                        mView.showError(it.message.toString())
                        mView.destory()
                    }
                })
    }

    override fun isJoinCheck(): Boolean {
        return mGroupBase?.bfJoinCheck ?: true
    }

    override fun joinGroup(groupId: Long, msg: String) {
        mView.showLoading()
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .groupJoin(object : HttpReq<GroupProto.GroupJoinReq>() {
                    override fun getData(): GroupProto.GroupJoinReq {
                        return GroupHttpReqCreator.createGroupJoinReq(groupId, msg, CommonProto.GroupReqType.GROUP_QR_CODE, mAddToken)
                    }
                })
                .getResult(mObservalbe, {
                    mView.dissmissLoading()
                    mView.joinSuccess(mGroupBase?.bfJoinCheck ?: true)
                }, {
                    //请求失败
                    mView.dissmissLoading()
                    mView.showError(mContext?.let {c ->
                        String.format(c.getString(R.string.joining_a_group_chat_failed_sign),it.message)
                    }?:"")
                })
    }
}

