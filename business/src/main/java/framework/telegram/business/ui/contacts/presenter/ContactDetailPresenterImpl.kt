package framework.telegram.business.ui.contacts.presenter

import android.content.Context
import com.im.domain.pb.ContactsProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.ContactsHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.FriendHttpProtocol
import framework.telegram.message.bridge.event.FriendRelationChangeEvent
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.network.http.HttpReq
import io.reactivex.Observable
import framework.telegram.support.tools.ExpandClass.toast

class ContactDetailPresenterImpl : ContactDetailContract.Presenter {

    private var mContext: Context?
    private var mView: ContactDetailContract.View
    private var mObservalbe: Observable<ActivityEvent>
    private val targetUid: Long
    private val groupId: Long

    constructor(
        targetUid: Long,
        groupId: Long,
        view: ContactDetailContract.View,
        context: Context?,
        observable: Observable<ActivityEvent>
    ) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        this.targetUid = targetUid
        this.groupId = groupId
        view.setPresenter(this)
    }

    override fun start() {
        mView.showLoading()
        getUpdateDetailInfo()
    }

    override fun getUpdateDetailInfo() {
        ArouterServiceManager.contactService.updateContactInfoByNet(
            mObservalbe,
            targetUid,
            groupId,
            { model, time, addToken ->
                mView.setAddToken(addToken)
                mView.showUserInfo(model)
                mView.showBanWord(time)
            }, {
                mView.showErrorMsg(mContext?.getString(R.string.common_fail))
            })
    }

    override fun addFriend(
        msg: String,
        groupId: Long,
        type: ContactsProto.ContactsAddType,
        op: ContactsProto.ContactsOperator,
        addToken: String
    ) {
        HttpManager.getStore(FriendHttpProtocol::class.java)
            .getContactsRelation(object : HttpReq<ContactsProto.ContactsRelationReq>() {
                override fun getData(): ContactsProto.ContactsRelationReq {
                    return ContactsHttpReqCreator.createAddRelation(
                        targetUid,
                        groupId,
                        msg,
                        type,
                        op,
                        addToken
                    )
                }
            })
            .getResult(mObservalbe, {
                getUpdateDetailInfo()

                mView.showAddFriendMsg()
            }, {
                //请求失败
                mView.showErrorMsg(it.message.toString())
            })
    }

    override fun setBlack(isBlack: Boolean) {
        ArouterServiceManager.contactService.setContactBlack(mObservalbe, targetUid, isBlack, {
            mView.showBlackInfo(isBlack)
        }) {
            mContext?.toast(it)
        }
    }

    override fun deleteFriend(op: ContactsProto.ContactsOperator) {
        ArouterServiceManager.contactService.setContactDelete(mObservalbe, targetUid, {
            mView.deleteFriend()
        }) {
            mContext?.toast(mContext?.getString(R.string.failed_to_delete_friends) ?: "")
        }
    }

    override fun setNoteName(nickName: String, note: String) {
        ArouterServiceManager.contactService.setContactNote(
            mObservalbe,
            targetUid,
            nickName,
            note,
            {
                mView.showNoteName(note)
            }) {
            mContext?.toast(mContext?.getString(R.string.failed_to_set_comment_name) ?: "")
        }
    }

    override fun setDescribe(describe: String) {
        ArouterServiceManager.contactService.setContactDescribe(mObservalbe, targetUid, describe, {
            mView.showDescribe(describe)
        }) {
            mContext?.toast(it)
        }
    }

    override fun setStarFriend(star: Boolean) {
        ArouterServiceManager.contactService.setContactStar(mObservalbe, targetUid, star, {
            mView.showStarFriend(star)
        }) {
            mContext?.toast(it)
        }
    }

    override fun setDisturb(disturb: Boolean) {
        ArouterServiceManager.contactService.setContactMessageQuiet(
            mObservalbe,
            targetUid,
            disturb,
            {
                ArouterServiceManager.messageService.setChatIsDisturb(
                    ChatModel.CHAT_TYPE_PVT,
                    targetUid,
                    disturb
                )
                mView.showDisturb(disturb)
            }) {
            mContext?.toast(it)
        }
    }

    override fun setBanWord(banTime: Int) {
        ArouterServiceManager.groupService.setGroupMemberBanTime(
            mObservalbe,
            groupId,
            targetUid,
            banTime,
            {
                mView.showBanWord(banTime)
            }) {
            mContext?.toast(it.toString())
        }
    }
}

