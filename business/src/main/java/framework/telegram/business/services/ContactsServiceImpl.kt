package framework.telegram.business.services

import android.content.Context
import android.text.TextUtils
import android.widget.TextView
import com.alibaba.android.arouter.facade.annotation.Route
import com.im.domain.pb.CommonProto
import com.im.domain.pb.ContactsProto
import com.im.domain.pb.UserProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.contacts.ContactReqModel
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.rlog.RLogManager
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_SERVICE_CONTACT
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.service.IContactService
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.http.HttpException
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.ContactsHttpReqCreator
import framework.telegram.business.http.creator.UserHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.FriendHttpProtocol
import framework.telegram.business.http.protocol.UserHttpProtocol
import framework.telegram.business.sp.CommonPref
import framework.telegram.business.ui.widget.ViewUtils
import framework.telegram.message.bridge.event.FireStatus
import framework.telegram.message.bridge.event.FireStatusChangeEvent
import framework.telegram.message.bridge.event.FriendInfoChangeEvent
import framework.telegram.message.bridge.event.GroupMemberChangeEvent
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.system.pinyin.FastPinyin
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import io.reactivex.Observable

@Route(path = ROUNTE_SERVICE_CONTACT, name = "联系人服务")
class ContactsServiceImpl : IContactService {

    override fun getContactInfoFromQr(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        qrCode: String,
        complete: ((ContactDataModel, String) -> Unit)?,
        error: (() -> Unit)?
    ) {
        HttpManager.getStore(FriendHttpProtocol::class.java)
            .getContactDetailFromQr(object : HttpReq<ContactsProto.ContactsDetailFromQrCodeReq>() {
                override fun getData(): ContactsProto.ContactsDetailFromQrCodeReq {
                    return ContactsHttpReqCreator.createContactDetailFromQr(userId, qrCode)
                }
            })
            .getResult(observable, { result ->
                updateContactInfo(observable, result.contactsDetail, {
                    complete?.invoke(it, result.contactsDetail.addToken)
                }, error)
            }, {
                AppLogcat.logger.e("demo", "更新联系人信息失败！！！")
                error?.invoke()
            })
    }

    override fun init(context: Context) {

    }

    override fun getContactInfo(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        complete: ((ContactDataModel, Boolean) -> Unit)?,
        error: (() -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        var copyContactDataModel: ContactDataModel? = null
        RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
            val contactDataModel =
                realm.where(ContactDataModel::class.java).equalTo("uid", userId).findFirst()
            if (contactDataModel != null) {
                copyContactDataModel = contactDataModel.copyContactDataModel()
            }
        }, {
            if (copyContactDataModel != null) {
                AppLogcat.logger.d("demo", "获取联系人缓存信息成功！！！")
                complete?.invoke(copyContactDataModel!!, true)
            } else {
                updateContactInfoByNet(observable, userId, 0, { model, _, _ ->
                    complete?.invoke(model, false)
                }, error)
            }
        }, {
            AppLogcat.logger.e("demo", "获取联系人信息失败！！！")
            error?.invoke()
        })
    }

    override fun getContactInfoCache(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        complete: ((ContactDataModel) -> Unit)?,
        error: (() -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        var copyContactDataModel: ContactDataModel? = null
        RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
            val contactDataModel =
                realm.where(ContactDataModel::class.java).equalTo("uid", userId).findFirst()
            if (contactDataModel != null) {
                copyContactDataModel = contactDataModel.copyContactDataModel()
            }
        }, {
            if (copyContactDataModel != null) {
                AppLogcat.logger.d("demo", "获取联系人缓存信息成功！！！")
                complete?.invoke(copyContactDataModel!!)
            } else {
                AppLogcat.logger.d("demo", "获取联系人缓存信息成功！！！")
                error?.invoke()
            }
        }, {
            AppLogcat.logger.e("demo", "获取联系人缓存信息失败！！！")
            error?.invoke()
        })
    }

    override fun getContactsInfoCache(
        observable: Observable<ActivityEvent>?,
        userIds: List<Long>,
        complete: ((List<ContactDataModel>) -> Unit)?,
        error: (() -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val contactDataModels = ArrayList<ContactDataModel>()
        RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
            userIds.forEach {
                val contactDataModel =
                    realm.where(ContactDataModel::class.java).equalTo("uid", it).findFirst()
                contactDataModel?.let {
                    contactDataModels.add(contactDataModel.copyContactDataModel())
                }
            }
        }, {
            AppLogcat.logger.d("demo", "获取联系人缓存信息成功！！！")
            complete?.invoke(contactDataModels)
        }, {
            AppLogcat.logger.e("demo", "获取联系人信息失败！！！")
            error?.invoke()
        })
    }

    override fun isInContactList(
        userId: Long,
        complete: ((Boolean) -> Unit)?,
        error: (() -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        var hasContact = false
        RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
            val contactDataModel =
                realm.where(ContactDataModel::class.java).equalTo("uid", userId).findFirst()
            contactDataModel?.let {
                hasContact = true
            }
        }, {
            complete?.invoke(hasContact)
        }, {
            error?.invoke()
        })
    }

    override fun updateContactInfo(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        cacheComplete: ((ContactDataModel) -> Unit)?,
        updateComplete: ((ContactDataModel) -> Unit)?,
        error: (() -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        var copyContactDataModel: ContactDataModel? = null
        RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
            val contactDataModel =
                realm.where(ContactDataModel::class.java).equalTo("uid", userId).findFirst()
            if (contactDataModel != null) {
                copyContactDataModel = contactDataModel.copyContactDataModel()
            }
        }, {
            if (copyContactDataModel != null) {
                AppLogcat.logger.d("demo", "获取联系人缓存信息成功！！！")
                cacheComplete?.invoke(copyContactDataModel!!)
            }

            updateContactInfoByNet(observable, userId, 0, { model, _, _ ->
                updateComplete?.invoke(model)
            }, error)
        }, {
            AppLogcat.logger.e("demo", "获取联系人信息失败！！！")
            error?.invoke()
        })
    }

    override fun updateContactInfoByNet(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        groupId: Long,
        complete: ((ContactDataModel, Int, String) -> Unit)?,
        error: (() -> Unit)?
    ) {
        HttpManager.getStore(FriendHttpProtocol::class.java)
            .getContactsDetail(object : HttpReq<ContactsProto.ContactsDetailReq>() {
                override fun getData(): ContactsProto.ContactsDetailReq {
                    return ContactsHttpReqCreator.createContactsDetail(userId, groupId)
                }
            })
            .getResult(observable, { result ->
                updateContactInfo(observable, result.contactsDetail, {
                    complete?.invoke(
                        it,
                        result.contactsDetail.groupShutupTime,
                        result.contactsDetail.addToken
                    )
                }, error)
            }, {
                AppLogcat.logger.e("demo", "更新联系人信息失败！！！")
                error?.invoke()
            })
    }

    override fun updateContactInfo(
        observable: Observable<ActivityEvent>?,
        detail: CommonProto.ContactsDetailBase,
        complete: ((ContactDataModel) -> Unit)?,
        error: (() -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val userId = detail.userInfo.uid
        var copyContactDataModel: ContactDataModel? = null
        var baseInfoChanged = false
        RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
            val contactModel = ContactDataModel.createContact(
                userId,
                detail.userInfo.nickName,
                detail.userInfo.gender.number,
                detail.userInfo.icon,
                detail.userInfo.friendRelation.remarkName,
                detail.bfStar,
                detail.bfMyBlack,
                detail.bfDisturb,
                detail.bfVerify,
                detail.userInfo.friendRelation.bfFriend,
                detail.letter,
                detail.depict,
                detail.signature,
                true,
                detail.userInfo.userOnOrOffline.online,
                detail.userInfo.userOnOrOffline.createTime,
                detail.userInfo.userOnOrOffline.bfShow,
                detail.phone,
                false,
                detail.bfReadCancel,
                detail.msgCancelTime,
                detail.bfScreenshot,
                detail.bfReadReceipt,
                detail.commonGroupNum,
                detail.userInfo.bfCancel,
                detail.userInfo.bfBanned,
                detail.userInfo.identify
            )

            val oldModel =
                realm.where(ContactDataModel::class.java).equalTo("uid", userId).findFirst()
            if (oldModel != null) {
                contactModel.isBfDisShowOnline = oldModel.isBfDisShowOnline
                contactModel.deleteMe = oldModel.deleteMe

                if (oldModel.displayName != contactModel.displayName || oldModel.icon != contactModel.icon || oldModel.nickName != contactModel.nickName || oldModel.isBfDisturb != contactModel.isBfDisturb) {
                    baseInfoChanged = true
                }
            }
            copyContactDataModel = contactModel?.copyContactDataModel()
            realm.copyToRealmOrUpdate(contactModel!!)
        }, {
            if (copyContactDataModel != null) {
                if (baseInfoChanged) {
                    val displayName = copyContactDataModel!!.displayName
                    val nickName = copyContactDataModel!!.nickName
                    val icon = copyContactDataModel!!.icon
                    val bfDisturb = copyContactDataModel!!.isBfDisturb
                    ArouterServiceManager.messageService.resetChaterInfo(
                        ChatModel.CHAT_TYPE_PVT,
                        userId,
                        displayName,
                        nickName,
                        icon,
                        bfDisturb
                    )
                    ArouterServiceManager.messageService.resetStreamCallChaterInfo(
                        userId,
                        displayName,
                        nickName,
                        icon
                    )
                    ArouterServiceManager.messageService.getAllChat({
                        it.forEach { chat ->
                            if (chat.chaterType == ChatModel.CHAT_TYPE_GROUP) {
                                ArouterServiceManager.groupService.updateGroupMemberInfoFromContact(
                                    chat.chaterId,
                                    detail,
                                    { isChange ->
                                        if (isChange) {
                                            EventBus.publishEvent(GroupMemberChangeEvent(chat.chaterId))
                                        }
                                    })
                            }
                        }
                    })
                }

                AppLogcat.logger.d("demo", "更新联系人信息成功！！！")

                EventBus.publishEvent(FriendInfoChangeEvent(copyContactDataModel!!))

                complete?.invoke(copyContactDataModel!!)
            } else {
                AppLogcat.logger.e("demo", "更新联系人信息失败！！！")
                error?.invoke()
            }
        }, {
            AppLogcat.logger.e("demo", "更新联系人信息失败！！！")
            error?.invoke()
        })
    }

    override fun updateContactInfo(
        observable: Observable<ActivityEvent>?,
        uid: Long,
        bfReadReceipt: Boolean,
        complete: ((ContactDataModel) -> Unit)?,
        error: (() -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        var copyContactDataModel: ContactDataModel? = null
        RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
            val oldModel = realm.where(ContactDataModel::class.java).equalTo("uid", uid).findFirst()
            if (oldModel != null) {
                val contactModel = oldModel.copyContactDataModel()
                contactModel.isReadReceipt = bfReadReceipt

                copyContactDataModel = contactModel?.copyContactDataModel()
                realm.copyToRealmOrUpdate(contactModel!!)
            }
        }, {
            if (copyContactDataModel != null) {
                AppLogcat.logger.d("demo", "更新联系人信息成功！！！")

                EventBus.publishEvent(FriendInfoChangeEvent(copyContactDataModel!!))

                complete?.invoke(copyContactDataModel!!)
            } else {
                AppLogcat.logger.e("demo", "更新联系人信息失败！！！")
                error?.invoke()
            }
        }, {
            AppLogcat.logger.e("demo", "更新联系人信息失败！！！")
            error?.invoke()
        })
    }

    override fun syncAllContact(
        observable: Observable<ActivityEvent>?,
        complete: (() -> Unit)?,
        error: (() -> Unit)?
    ) {
        updateAllContact(observable, 1, 200, complete, error)
    }

    private fun updateAllContact(
        observable: Observable<ActivityEvent>?,
        pageNo: Int,
        pageSize: Int,
        complete: (() -> Unit)?,
        error: (() -> Unit)?
    ) {
        HttpManager.getStore(FriendHttpProtocol::class.java)
            .getContactsList(object : HttpReq<ContactsProto.ContactsListReq>() {
                override fun getData(): ContactsProto.ContactsListReq {
                    return ContactsHttpReqCreator.createContactsListReq(pageNo, pageSize)
                }
            })
            .getResult(observable, {
                val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                val updateContactModels = ArrayList<ContactDataModel>()
                val changedBaseInfoContacts = mutableListOf<Long>()
                RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
                    if (it.contactsListList?.isNotEmpty() == true) {
                        val contactModels = ArrayList<ContactDataModel>()
                        it.contactsListList?.forEach { result ->
                            val contact = ContactDataModel.createContact(
                                result.userInfo.uid,
                                result.userInfo.nickName,
                                result.userInfo.gender.number,
                                result.userInfo.icon,
                                result.userInfo.friendRelation.remarkName,
                                result.bfStar,
                                result.bfMyBlack,
                                result.bfDisturb,
                                result.bfVerify,
                                result.userInfo.friendRelation.bfFriend,
                                result.letter,
                                result.depict,
                                result.signature,
                                true,
                                result.userInfo.userOnOrOffline.online,
                                result.userInfo.userOnOrOffline.createTime,
                                result.userInfo.userOnOrOffline.bfShow,
                                result.phone,
                                false,
                                result.bfReadCancel,
                                result.msgCancelTime,
                                result.bfScreenshot,
                                result.bfReadReceipt,
                                result.commonGroupNum,
                                result.userInfo.bfCancel,
                                result.userInfo.bfBanned,
                                result.userInfo.identify
                            )

                            val oldModel = realm.where(ContactDataModel::class.java)
                                .equalTo("uid", result.userInfo.uid).findFirst()
                            if (oldModel != null) {
                                contact.isBfDisShowOnline = oldModel.isBfDisShowOnline
                                contact.deleteMe = oldModel.deleteMe

                                if (oldModel.displayName != contact.displayName || oldModel.icon != contact.icon || oldModel.nickName != contact.nickName || oldModel.isBfDisturb != contact.isBfDisturb) {
                                    changedBaseInfoContacts.add(oldModel.uid)
                                }
                            }

                            contactModels.add(contact)
                            updateContactModels.add(contact.copyContactDataModel())
                        }
                        realm.copyToRealmOrUpdate(contactModels)
                    }
                }, {
                    if (it.contactsListCount < pageSize) {
                        //取完了
                        RLogManager.d("User", "获取联系人成功(${it.contactsListCount})，结束同步！！！")

                        val fireStutsList = mutableListOf<FireStatus>()
                        //更新会话列表
                        updateContactModels.forEach { contact ->
                            if (changedBaseInfoContacts.contains(contact.uid)) {
                                ArouterServiceManager.messageService.resetChaterInfo(
                                    ChatModel.CHAT_TYPE_PVT,
                                    contact.uid,
                                    contact.displayName,
                                    contact.nickName,
                                    contact.icon,
                                    contact.isBfDisturb
                                )
                                ArouterServiceManager.messageService.resetStreamCallChaterInfo(
                                    contact.uid,
                                    contact.displayName,
                                    contact.nickName,
                                    contact.icon
                                )
                            }
                            fireStutsList.add(FireStatus(contact.uid, contact.isBfReadCancel))
                        }
                        EventBus.publishEvent(FireStatusChangeEvent(fireStutsList))

                        //结束同步
                        complete?.invoke()
                    } else {
                        RLogManager.d("User", "获取联系人成功(${it.contactsListCount})，继续加载下一页")
                        updateAllContact(observable, pageNo + 1, pageSize, complete, error)
                    }
                }, {
                    RLogManager.d("User", "获取联系人失败，结束同步！！！")
                    error?.invoke()
                })
            }, {
                RLogManager.d("User", "获取联系人失败！！！")
                error?.invoke()
            })
    }

    override fun getAllContact(
        complete: ((List<ContactDataModel>) -> Unit)?,
        error: (() -> Unit)?
    ) {
        val contactDataModels = mutableListOf<ContactDataModel>()
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
            val contacts = realm.where(ContactDataModel::class.java)?.equalTo("bfMyContacts", true)
                ?.sort("letter")?.findAll()
            contacts?.forEach { contact ->
                contactDataModels.add(contact.copyContactDataModel())
            }
        }, {
            AppLogcat.logger.d("demo", "获取所有联系人成功！！！")
            complete?.invoke(contactDataModels)
        }, {
            AppLogcat.logger.e("demo", "获取所有联系人失败！！！")
            error?.invoke()
        })
    }

    override fun agreeContactReq(
        observable: Observable<ActivityEvent>?,
        applyUid: Long,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        HttpManager.getStore(FriendHttpProtocol::class.java)
            .updateContactsApply(object : HttpReq<ContactsProto.UpdateContactsApplyReq>() {
                override fun getData(): ContactsProto.UpdateContactsApplyReq {
                    return ContactsHttpReqCreator.createContactsApply(
                        applyUid,
                        ContactsProto.ContactsOperator.ADD_REQ
                    )
                }
            })
            .getResult(observable, {
                val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                updateContactInfo(observable, applyUid, null, null, null)//更新资料

                RealmCreator.executeContactReqsTransactionAsync(myUid, { realm ->
                    val reqModel = realm.where(ContactReqModel::class.java).equalTo("uid", applyUid)
                        .findFirst()
                    reqModel?.let {
                        reqModel.type = 1
                        realm.copyToRealm(reqModel)
                    }
                }, complete, error)
            }, {
                //请求失败
                error?.invoke(it)
            })
    }

    override fun deleteContactReq(
        observable: Observable<ActivityEvent>?,
        applyUid: Long,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        HttpManager.getStore(FriendHttpProtocol::class.java)
            .updateContactsApply(object : HttpReq<ContactsProto.UpdateContactsApplyReq>() {
                override fun getData(): ContactsProto.UpdateContactsApplyReq {
                    return ContactsHttpReqCreator.createContactsApply(
                        applyUid,
                        ContactsProto.ContactsOperator.DEL_REQ
                    )
                }
            })
            .getResult(observable, {
                val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                // 删除好友请求
                RealmCreator.executeContactReqsTransactionAsync(myUid, { realm ->
                    val reqModel = realm.where(ContactReqModel::class.java).equalTo("uid", applyUid)
                        .findFirst()
                    reqModel?.deleteFromRealm()
                }, complete, error)
            }, {
                //请求失败
                error?.invoke(it)
            })
    }

    override fun updateAllContactReq(
        observable: Observable<ActivityEvent>?,
        complete: (() -> Unit)?,
        error: (() -> Unit)?
    ) {
        HttpManager.getStore(FriendHttpProtocol::class.java)
            .getContactsApplyList(object : HttpReq<ContactsProto.ContactsApplyListReq>() {
                override fun getData(): ContactsProto.ContactsApplyListReq {
                    return ContactsHttpReqCreator.getContactsApplyList()
                }
            })
            .getResult(observable, {
                val friendReqModels = ArrayList<ContactReqModel>()

                if (!it.unRecordListList.isNullOrEmpty()) {
                    it.unRecordListList?.forEach { record ->
                        friendReqModels.add(
                            ContactReqModel.createContactReqModel(
                                record.userInfo.uid,
                                0,
                                record.userInfo.nickName,
                                record.userInfo.gender.number,
                                record.userInfo.icon,
                                record.userInfo.friendRelation.bfFriend,
                                record.userInfo.friendRelation.remarkName,
                                record.msg,
                                record.bfMyBlack,
                                record.modifyTime,
                                record.type.number,
                                record.signature
                            )
                        )
                    }
                }

                if (!it.recordListList.isNullOrEmpty()) {
                    it.recordListList?.forEach { record ->
                        friendReqModels.add(
                            ContactReqModel.createContactReqModel(
                                record.userInfo.uid,
                                1,
                                record.userInfo.nickName,
                                record.userInfo.gender.number,
                                record.userInfo.icon,
                                record.userInfo.friendRelation.bfFriend,
                                record.userInfo.friendRelation.remarkName,
                                record.msg,
                                record.bfMyBlack,
                                record.modifyTime,
                                record.type.number,
                                record.signature
                            )
                        )
                    }
                }

                val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                RealmCreator.executeContactReqsTransactionAsync(myUid, { realm ->
                    realm.delete(ContactReqModel::class.java)
                    realm.copyToRealm(friendReqModels)
                }, {
                    AppLogcat.logger.d("demo", "同步联系人添加列表成功！！！")
                    complete?.invoke()
                }, {
                    AppLogcat.logger.e("demo", "同步联系人添加列表失败！！！")
                    error?.invoke()
                })
            }, {
                AppLogcat.logger.e("demo", "同步联系人添加列表失败！！！")
                error?.invoke()
            })
    }

    override fun setContactMessageQuiet(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        isQuiet: Boolean,
        complete: (() -> Unit)?,
        error: ((String) -> Unit)?
    ) {
        val param =
            ContactsProto.ContactsParam.newBuilder().setBfDisturb(isQuiet).setContactsId(userId)
                .build()
        setContactParam(observable, userId, ContactsProto.ContactsOperator.DISTURB, param, {
            it.isBfDisturb = isQuiet
        }, complete, error)
    }

    override fun setContactStar(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        isStar: Boolean,
        complete: (() -> Unit)?,
        error: ((String) -> Unit)?
    ) {
        val param =
            ContactsProto.ContactsParam.newBuilder().setBfStar(isStar).setContactsId(userId).build()
        setContactParam(
            observable,
            userId,
            ContactsProto.ContactsOperator.STAR,
            param,
            { it.isBfStar = isStar },
            complete,
            error
        )
    }

    override fun setContactNote(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        nickName: String,
        note: String,
        complete: (() -> Unit)?,
        error: ((String) -> Unit)?
    ) {
        val param =
            ContactsProto.ContactsParam.newBuilder().setNoteName(note).setContactsId(userId).build()
        setContactParam(observable, userId, ContactsProto.ContactsOperator.REMARK, param, {
            it.noteName = note
            it.searchNoteName = FastPinyin.toAllPinyin(note)
            it.shortNoteName = FastPinyin.toAllPinyinFirst(note, false)
            if (!TextUtils.isEmpty(note)) {
                it.letter = FastPinyin.toPinyin(note)
            } else {
                it.letter = FastPinyin.toPinyin(nickName)
            }
        }, {
            complete?.invoke()
            ArouterServiceManager.messageService.resetChaterInfo(
                ChatModel.CHAT_TYPE_PVT,
                userId,
                note,
                nickName
            )
            ArouterServiceManager.messageService.resetStreamCallChaterInfo(userId, note, nickName)
            ArouterServiceManager.messageService.getAllChat({
                it.forEach { chatModel ->
                    if (chatModel.chaterType == ChatModel.CHAT_TYPE_GROUP) {
                        ArouterServiceManager.groupService.updateGroupMemberRemarkName(
                            chatModel.chaterId,
                            userId,
                            note
                        )
                    }
                }
            })
        }, error)
    }

    override fun setContactDescribe(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        describe: String,
        complete: (() -> Unit)?,
        error: ((String) -> Unit)?
    ) {
        val param =
            ContactsProto.ContactsParam.newBuilder().setDepict(describe).setContactsId(userId)
                .build()
        setContactParam(
            observable,
            userId,
            ContactsProto.ContactsOperator.DESCRIBE,
            param,
            { it.depict = describe },
            complete,
            error
        )
    }

    override fun setBurnAfterRead(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        burnAfterRead: Boolean,
        complete: (() -> Unit)?,
        error: ((String) -> Unit)?
    ) {
        val param = ContactsProto.ContactsParam.newBuilder().setBfReadCancel(burnAfterRead)
            .setContactsId(userId).build()
        setContactParam(
            observable,
            userId,
            ContactsProto.ContactsOperator.READ_CANCEL,
            param,
            { it.isBfReadCancel = burnAfterRead },
            complete,
            error
        )
    }

    override fun setBurnAfterReadTime(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        time: Int,
        complete: (() -> Unit)?,
        error: ((String) -> Unit)?
    ) {
        val param =
            ContactsProto.ContactsParam.newBuilder().setMsgCancelTime(time).setContactsId(userId)
                .build()
        setContactParam(
            observable,
            userId,
            ContactsProto.ContactsOperator.READ_CANCEL_TIME,
            param,
            { it.msgCancelTime = time },
            complete,
            error
        )
    }

    override fun setContactScreenshot(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        screenshot: Boolean,
        complete: (() -> Unit)?,
        error: ((String) -> Unit)?
    ) {
        val param = ContactsProto.ContactsParam.newBuilder().setBfScreenshot(screenshot)
            .setContactsId(userId).build()
        setContactParam(
            observable,
            userId,
            ContactsProto.ContactsOperator.SCREENSHOT,
            param,
            { it.isBfScreenshot = screenshot },
            complete,
            error
        )
    }

    private fun setContactParam(
        observable: Observable<ActivityEvent>?,
        uid: Long,
        op: ContactsProto.ContactsOperator,
        param: ContactsProto.ContactsParam,
        updateModelUnit: (ContactDataModel) -> Unit,
        complete: (() -> Unit)? = null,
        error: ((String) -> Unit)? = null
    ) {
        HttpManager.getStore(FriendHttpProtocol::class.java)
            .updateContacts(object : HttpReq<ContactsProto.UpdateContactsReq>() {
                override fun getData(): ContactsProto.UpdateContactsReq {
                    return ContactsHttpReqCreator.updateContacts(param, op)
                }
            })
            .getResult(observable, {
                val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
                    val contactModel =
                        realm.where(ContactDataModel::class.java).equalTo("uid", uid).findFirst()
                    contactModel?.let {
                        updateModelUnit.invoke(contactModel)
                        realm.copyToRealmOrUpdate(contactModel)
                    }
                }, {
                    complete?.invoke()
                }, { t ->
                    error?.invoke(t.message ?: "")
                })
            }, {
                error?.invoke(it.message ?: "")
            })
    }

    override fun setContactBlack(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        isBlack: Boolean,
        complete: (() -> Unit)?,
        error: ((String) -> Unit)?
    ) {
        val isBlackOp =
            if (isBlack) ContactsProto.ContactsOperator.ADD_BLACK else ContactsProto.ContactsOperator.DEL_BLACK
        HttpManager.getStore(FriendHttpProtocol::class.java)
            .updateBlackContacts(object : HttpReq<ContactsProto.UpdateBlackContactsReq>() {
                override fun getData(): ContactsProto.UpdateBlackContactsReq {
                    return ContactsHttpReqCreator.createBlackContacts(userId, isBlackOp)
                }
            })
            .getResult(observable, {
                val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
                    val contactModel =
                        realm.where(ContactDataModel::class.java).equalTo("uid", userId).findFirst()
                    contactModel?.let { copyContactModel ->
                        copyContactModel.isBfMyBlack = isBlack
                        //会引起bug：拉黑，删除，取消拉黑好友后，通讯录出现该好友问题
//                            copyContactModel.isBfMyContacts = !isBlack
                        realm.copyToRealmOrUpdate(copyContactModel)
                    }
                }, {
                    complete?.invoke()
                }, { t ->
                    error?.invoke(t.message ?: "")
                })
            }, {
                error?.invoke(it.message ?: "")
            })
    }

    override fun setContactDelete(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        complete: (() -> Unit)?,
        error: (() -> Unit)?
    ) {
        HttpManager.getStore(FriendHttpProtocol::class.java)
            .getContactsRelation(object : HttpReq<ContactsProto.ContactsRelationReq>() {
                override fun getData(): ContactsProto.ContactsRelationReq {
                    return ContactsHttpReqCreator.createDeleteRelation(
                        userId,
                        ContactsProto.ContactsOperator.DEL
                    )
                }
            })
            .getResult(observable, {
                // 删除会话记录
                ArouterServiceManager.messageService.deleteChat(ChatModel.CHAT_TYPE_PVT, userId)
                // 删除通话记录
                ArouterServiceManager.messageService.deleteStreamCallHistory(userId)

                // 从联系人或黑名单中删除此人
                val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
                    val contactDataModel =
                        realm.where(ContactDataModel::class.java).equalTo("uid", userId).findFirst()
                    contactDataModel?.deleteFromRealm()
                }, complete, {
                    error?.invoke()
                })
            }, {
                //请求失败
                error?.invoke()
            })
    }

    override fun syncAllBlackContact(
        observable: Observable<ActivityEvent>?,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        val tmpList = ArrayList<CommonProto.UserBase>()
        updateAllBlackContact(tmpList, observable, 1, 50, complete, error)
    }

    private fun updateAllBlackContact(
        tmpList: ArrayList<CommonProto.UserBase>,
        observable: Observable<ActivityEvent>?,
        pageNum: Int,
        pageSize: Int,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        HttpManager.getStore(FriendHttpProtocol::class.java)
            .getBlackContacts(object : HttpReq<ContactsProto.BlackListReq>() {
                override fun getData(): ContactsProto.BlackListReq {
                    return ContactsHttpReqCreator.createBlackContacts(pageNum, pageSize)
                }
            })
            .getResult(observable, {
                if (it.blackListList?.isNotEmpty() == true) {
                    tmpList.addAll(it.blackListList)
                }

                if (it.blackListList.count() < pageSize) {
                    //取完了
                    val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                    RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
                        val blackUsers =
                            realm.where(ContactDataModel::class.java).equalTo("bfMyBlack", true)
                                .findAll()
                        blackUsers.forEach { blackUser ->
                            blackUser.isBfMyBlack = false
                        }
                        realm.copyToRealmOrUpdate(blackUsers)

                        val contactModels = ArrayList<ContactDataModel>()
                        it.blackListList?.forEach { result ->
                            val contactModel = realm.where(ContactDataModel::class.java)
                                ?.equalTo("uid", result.uid)?.findFirst()
                            contactModels.add(
                                if (contactModel == null) {
                                    ContactDataModel.createContact(
                                        result.uid,
                                        result.nickName,
                                        result.gender.number,
                                        result.icon,
                                        result.friendRelation.remarkName,
                                        false,
                                        true,
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
                                        true,
                                        0,
                                        false,
                                        false,
                                        ""
                                    )
                                } else {
                                    contactModel.isBfMyBlack = true
                                    contactModel
                                }
                            )
                        }
                        realm.copyToRealmOrUpdate(contactModels)
                    }, {
                        AppLogcat.logger.d("demo", "获取黑名单用户成功，结束同步！！！")
                        complete?.invoke()
                    }, { throwable ->
                        AppLogcat.logger.e("demo", "获取黑名单用户失败，结束同步！！！")
                        error?.invoke(throwable)
                    })
                } else {
                    AppLogcat.logger.d("demo", "获取黑名单用户成功，继续加载下一页")
                    updateAllBlackContact(
                        tmpList,
                        observable,
                        pageNum + 1,
                        pageSize,
                        complete,
                        error
                    )
                }
            }, {
                error?.invoke(it)
            })
    }

    override fun syncAllDisShowOnlineContact(
        observable: Observable<ActivityEvent>?,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        val tmpList = ArrayList<CommonProto.UserBase>()
        updateAllDisShowOnlineContact(tmpList, observable, 1, 50, complete, error)
    }

    private fun updateAllDisShowOnlineContact(
        tmpList: ArrayList<CommonProto.UserBase>,
        observable: Observable<ActivityEvent>?,
        pageNum: Int,
        pageSize: Int,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        HttpManager.getStore(UserHttpProtocol::class.java)
            .getInvisibleList(object : HttpReq<UserProto.InvisibleListReq>() {
                override fun getData(): UserProto.InvisibleListReq {
                    return UserHttpReqCreator.createInvisibleListReq(pageNum, pageSize)
                }
            })
            .getResult(observable, {
                if (it.invisibleListList?.isNotEmpty() == true) {
                    tmpList.addAll(it.invisibleListList)
                }

                if (it.invisibleListList.count() < pageSize) {
                    //取完了
                    val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                    RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
                        val disShowOnlineUsers = realm.where(ContactDataModel::class.java)
                            .equalTo("bfDisShowOnline", true).findAll()
                        disShowOnlineUsers.forEach { blackUser ->
                            blackUser.isBfDisShowOnline = false
                        }
                        realm.copyToRealmOrUpdate(disShowOnlineUsers)

                        val contactModels = ArrayList<ContactDataModel>()
                        it.invisibleListList?.forEach { result ->
                            val contactModel = realm.where(ContactDataModel::class.java)
                                ?.equalTo("uid", result.uid)?.findFirst()
                            contactModels.add(
                                if (contactModel == null) {
                                    ContactDataModel.createContact(
                                        result.uid,
                                        result.nickName,
                                        result.gender.number,
                                        result.icon,
                                        result.friendRelation.remarkName,
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
                                        true,
                                        0,
                                        false,
                                        false,
                                        ""
                                    )
                                } else {
                                    contactModel.isBfDisShowOnline = true
                                    contactModel
                                }
                            )
                        }
                        realm.copyToRealmOrUpdate(contactModels)
                    }, {
                        AppLogcat.logger.d("demo", "获取永远不可见用户成功，结束同步！！！")
                        complete?.invoke()
                    }, { throwable ->
                        AppLogcat.logger.e("demo", "获取永远不可见用户失败，结束同步！！！")
                        error?.invoke(throwable)
                    })
                } else {
                    AppLogcat.logger.d("demo", "获取永远不可见用户成功，继续加载下一页")
                    updateAllDisShowOnlineContact(
                        tmpList,
                        observable,
                        pageNum + 1,
                        pageSize,
                        complete,
                        error
                    )
                }
            }, {
                error?.invoke(it)
            })
    }

    override fun addDisShowOnlineContacts(
        observable: Observable<ActivityEvent>?,
        uids: List<Long>,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        HttpManager.getStore(UserHttpProtocol::class.java)
            .updateInvisibleList(object : HttpReq<UserProto.UpdateInvisibleListReq>() {
                override fun getData(): UserProto.UpdateInvisibleListReq {
                    return UserHttpReqCreator.createUpdateInvisibleList(
                        uids,
                        UserProto.UserOperator.ADD_INVISIBLE
                    )
                }
            })
            .getResult(observable, {
                syncAllDisShowOnlineContact(observable, complete, error)
            }, {
                error?.invoke(it)
            })
    }

    override fun deleteDisShowOnlineContact(
        observable: Observable<ActivityEvent>?,
        uid: Long,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        HttpManager.getStore(UserHttpProtocol::class.java)
            .updateInvisibleList(object : HttpReq<UserProto.UpdateInvisibleListReq>() {
                override fun getData(): UserProto.UpdateInvisibleListReq {
                    return UserHttpReqCreator.createUpdateInvisibleList(
                        listOf(uid),
                        UserProto.UserOperator.DEL_INVISIBLE
                    )
                }
            })
            .getResult(observable, {
                val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
                    val disShowOnlineUser =
                        realm.where(ContactDataModel::class.java).equalTo("uid", uid).findFirst()
                    disShowOnlineUser?.let {
                        disShowOnlineUser.isBfDisShowOnline = false
                        realm.copyToRealmOrUpdate(disShowOnlineUser)
                    }
                }, complete, error)
            }, {
                error?.invoke(it)
            })
    }

    override fun getAllDisShowOnlineCacheContacts(
        complete: ((List<ContactDataModel>) -> Unit)?,
        error: (() -> Unit)?
    ) {
        val contactDataModels = mutableListOf<ContactDataModel>()
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
            val contacts =
                realm.where(ContactDataModel::class.java)?.equalTo("bfDisShowOnline", true)
                    ?.sort("letter")?.findAll()
            contacts?.forEach { contact ->
                contactDataModels.add(contact.copyContactDataModel())
            }
        }, {
            AppLogcat.logger.d("demo", "获取不可见在线状态用户成功！！！")
            complete?.invoke(contactDataModels)
        }, {
            AppLogcat.logger.e("demo", "获取不可见在线状态用户失败！！！")
            error?.invoke()
        })
    }

    override fun setContactLastOnline(
        observable: Observable<ActivityEvent>?,
        onlineStatus: List<CommonProto.UserOnOrOffLine>,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeContactsTransactionAsync(myUid, { realm ->
            onlineStatus.forEach {
                val contactDataModel =
                    realm.where(ContactDataModel::class.java).equalTo("uid", it.uid).findFirst()
                if (contactDataModel != null) {
                    contactDataModel.isOnlineStatus = it.online
                    contactDataModel.lastOnlineTime = it.createTime
                    contactDataModel.isShowLastOnlineTime = it.bfShow
                    realm.copyToRealmOrUpdate(contactDataModel)
                }
            }
        }, {
            AppLogcat.logger.d("demo", "修改联系人在线状态成功！！！")
            complete?.invoke()
        }, {
            AppLogcat.logger.e("demo", "修改联系人在线状态失败！！！")
            error?.invoke(it)
        })
    }

    override fun showOnlineStatus(
        uid: Long,
        isShowLastOnlineTime: Boolean,
        isOnlineStatus: Boolean,
        lastOnlineTime: Long,
        statusView: TextView
    ) {
        ViewUtils.showOnlineStatus(
            ArouterServiceManager.messageService,
            uid,
            isShowLastOnlineTime,
            isOnlineStatus,
            lastOnlineTime,
            statusView
        )
    }

    override fun addFriend(
        identify: String,
        userId: Long,
        findSign: String,
        complete: () -> Unit,
        error: (Int, String) -> Unit
    ) {
        getAddToken(identify, userId, findSign, { uid, addToken ->
            HttpManager.getStore(FriendHttpProtocol::class.java)
                .getContactsRelation(object : HttpReq<ContactsProto.ContactsRelationReq>() {
                    override fun getData(): ContactsProto.ContactsRelationReq {
                        return ContactsHttpReqCreator.createAddRelation(
                            uid,
                            0,
                            "",
                            ContactsProto.ContactsAddType.REQ_MSG,
                            ContactsProto.ContactsOperator.ADD,
                            addToken
                        )
                    }
                })
                .getResult(null, {
                    complete.invoke()
                }, {
                    //请求失败
                    if (it is HttpException) {
                        if (it.errCode == 5105) {
                            error.invoke(5105, addToken)
                        } else {
                            error.invoke(it.errCode, it.message ?: "")
                        }
                    } else {
                        error.invoke(0, it.message ?: "")
                    }
                })
        }, error)
    }

    override fun getAddToken(
        identify: String,
        userId: Long,
        findSign: String,
        complete: (Long, String) -> Unit,
        error: (Int, String) -> Unit
    ) {
        HttpManager.getStore(FriendHttpProtocol::class.java)
            .findContacts(object : HttpReq<ContactsProto.FindContactsListReq>() {
                override fun getData(): ContactsProto.FindContactsListReq {
                    return ContactsHttpReqCreator.findContacts(identify, userId, findSign)
                }
            })
            .getResult(null, { resp ->
                if (resp.detailListCount == 1) {
                    val user = resp.detailListList[0]
                    complete.invoke(user.userInfo.uid, user.addToken)
                } else {
                    error.invoke(0, "")
                }
            }, {
                //请求失败
                if (it is HttpException) {
                    error.invoke(it.errCode, it.message ?: "")
                } else {
                    error.invoke(0, it.message ?: "")
                }
            })
    }

    override fun checkFriendShip(
        myUid: Long,
        targetUid: Long,
        complete: (Boolean) -> Unit,
        error: ((Throwable) -> Unit)?
    ) {
        var deleteMe = false
        RealmCreator.executeContactsExTransactionAsync(myUid, { realm ->
            val contactModel =
                realm.where(ContactDataModel::class.java).equalTo("uid", targetUid).findFirst()
            contactModel?.let {
                deleteMe = contactModel.deleteMe
            }
        }, {
            complete.invoke(deleteMe)
        }, { t ->
            error?.invoke(t)
        })

    }

    override fun updataFriendShip(
        targetUid: Long,
        isDeleteMe: Boolean,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    ) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeContactsExTransactionAsync(myUid, { realm ->
            val contactModel =
                realm.where(ContactDataModel::class.java).equalTo("uid", targetUid).findFirst()
            contactModel?.let {
                contactModel.deleteMe = isDeleteMe
                realm.copyToRealmOrUpdate(contactModel)
            }
        }, {
            complete?.invoke()
        }, { t ->
            error?.invoke(t)
        })
    }
}