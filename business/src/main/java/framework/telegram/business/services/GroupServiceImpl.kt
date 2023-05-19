package framework.telegram.business.services

import android.content.Context
import android.util.Log
import com.alibaba.android.arouter.core.LogisticsCenter
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.CommonProto
import com.im.domain.pb.GroupProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.group.GroupMemberModel
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_SERVICE_GROUP
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.bridge.service.IGroupService
import framework.telegram.business.db.RealmCreator
import framework.telegram.business.http.HttpException
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.GroupHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.GroupHttpProtocol
import framework.telegram.business.sp.CommonPref
import framework.telegram.message.bridge.event.*
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.log.AppLogcat
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.ExpandClass.toast
import io.reactivex.Observable
import io.realm.Realm
import io.realm.Sort
import kotlin.math.max

@Route(path = ROUNTE_SERVICE_GROUP, name = "群服务")
class GroupServiceImpl : IGroupService {


    override fun init(context: Context) {

    }

    override fun clearGroupReq(observable: Observable<ActivityEvent>?, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .clearGroupNotification(object : HttpReq<GroupProto.ClearGroupReq>() {
                    override fun getData(): GroupProto.ClearGroupReq {
                        return GroupHttpReqCreator.createClearGroupNotification()
                    }
                })
                .getResult(observable, {
                    complete?.invoke()
                }, {
                    error?.invoke(it)
                })
    }

    override fun getGroupInfo(observable: Observable<ActivityEvent>?, groupId: Long, complete: ((GroupInfoModel, Boolean) -> Unit)?, error: (() -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        var copyGroupInfoModel: GroupInfoModel? = null
        RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
            val groupModel = realm.where(GroupInfoModel::class.java).equalTo("groupId", groupId).findFirst()
            if (groupModel != null) {
                copyGroupInfoModel = groupModel.copyGroupInfoModel()
            }
        }, {
            if (copyGroupInfoModel != null) {
                AppLogcat.logger.d("demo", "获取群信息缓存成功！！！")
                complete?.invoke(copyGroupInfoModel!!, true)
            } else {
                updateGroupInfoByNet(observable, groupId, {
                    complete?.invoke(it, false)
                }, error)
            }
        }, {
            AppLogcat.logger.e("demo", "获取群信息失败！！！")
            error?.invoke()
        })
    }

    override fun updateGroupInfo(observable: Observable<ActivityEvent>?, groupId: Long, cacheComplete: ((GroupInfoModel) -> Unit)?, updateComplete: ((GroupInfoModel) -> Unit)?, error: (() -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        var copyGroupInfoModel: GroupInfoModel? = null
        RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
            val groupModel = realm.where(GroupInfoModel::class.java).equalTo("groupId", groupId).findFirst()
            groupModel?.let {
                copyGroupInfoModel = groupModel.copyGroupInfoModel()
            }
        }, {
            if (copyGroupInfoModel != null) {
                AppLogcat.logger.d("demo", "获取群信息缓存成功！！！")
                cacheComplete?.invoke(copyGroupInfoModel!!)
            }

            AppLogcat.logger.e("demo", "22222222！！！")
            updateGroupInfoByNet(observable, groupId, updateComplete, error)
        }, {
            AppLogcat.logger.e("demo", "获取群信息失败！！！")
            error?.invoke()
        })
    }

    override fun updateGroupInfoByCache(observable: Observable<ActivityEvent>?, groupId: Long, cacheComplete: ((GroupInfoModel) -> Unit)?, error: (() -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        var copyGroupInfoModel: GroupInfoModel? = null
        RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
            val groupModel = realm.where(GroupInfoModel::class.java).equalTo("groupId", groupId).findFirst()
            groupModel?.let {
                copyGroupInfoModel = groupModel.copyGroupInfoModel()
            }
        }, {
            if (copyGroupInfoModel != null) {
                AppLogcat.logger.d("demo", "获取群信息缓存成功！！！")
                cacheComplete?.invoke(copyGroupInfoModel!!)
            } else {
                AppLogcat.logger.e("demo", "获取群信息缓存失败！！！")
                error?.invoke()
            }
        }, {
            AppLogcat.logger.e("demo", "获取群信息缓存失败！！！")
            error?.invoke()
        })
    }

    override fun isGroupMemberByNet(observable: Observable<ActivityEvent>?, groupId: Long, complete: ((Boolean) -> Unit)?) {
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .groupDetail(object : HttpReq<GroupProto.GroupDetailReq>() {
                    override fun getData(): GroupProto.GroupDetailReq {
                        return GroupHttpReqCreator.createGroupDetailReq(groupId)
                    }
                })
                .getResult(observable, { result ->
                    complete?.invoke(true)
                }, {
                    complete?.invoke(false)
                })
    }

    override fun updateGroupInfoByNet(observable: Observable<ActivityEvent>?, groupId: Long, updateComplete: ((GroupInfoModel) -> Unit)?, error: (() -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .groupDetail(object : HttpReq<GroupProto.GroupDetailReq>() {
                    override fun getData(): GroupProto.GroupDetailReq {
                        return GroupHttpReqCreator.createGroupDetailReq(groupId)
                    }
                })
                .getResult(observable, { result ->
                    var copyGroupInfoModel: GroupInfoModel? = null
                    RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
                        val groupModel = if (result.memberType.number == CommonProto.GroupMemberType.HOST.number) {
                            //群主默认拥有所有权限
                            GroupInfoModel.createGroup(result.group.groupId, result.group.hostId, result.group.name, result.group.pic,
                                    result.group.createTime, result.groupNickName, result.bfStar, result.bfDisturb, result.bfAddress,
                                    result.group.memberCount.toInt(), result.group.bfJoinFriend, result.group.bfShutup,
                                    result.group.bfJoinCheck, true, true, true,
                                    result.memberType.number, result.groupNotice.notice, result.groupNotice.noticeId, true, result.group.bfGroupReadCancel, result.group.groupMsgCancelTime, result.group.bfBanned)
                        } else {
                            GroupInfoModel.createGroup(result.group.groupId, result.group.hostId, result.group.name, result.group.pic,
                                    result.group.createTime, result.groupNickName, result.bfStar, result.bfDisturb, result.bfAddress,
                                    result.group.memberCount.toInt(), result.group.bfJoinFriend, result.group.bfShutup,
                                    result.group.bfJoinCheck, result.right.bfUpdateData, result.right.bfPushNotice, result.right.bfSetAdmin,
                                    result.memberType.number, result.groupNotice.notice, result.groupNotice.noticeId, true, result.group.bfGroupReadCancel, result.group.groupMsgCancelTime, result.group.bfBanned)
                        }
                        realm.copyToRealmOrUpdate(groupModel)
                        copyGroupInfoModel = groupModel.copyGroupInfoModel()
                    }, {
                        if (copyGroupInfoModel != null) {
                            // 更新头像
                            val displayName = copyGroupInfoModel!!.name
                            val icon = copyGroupInfoModel!!.pic
                            val bfDisturb = copyGroupInfoModel!!.bfDisturb
                            ArouterServiceManager.messageService.resetChaterInfo(ChatModel.CHAT_TYPE_GROUP, groupId, displayName, null, icon, bfDisturb)

                            AppLogcat.logger.d("demo", "更新群信息成功！！！")
                            updateComplete?.invoke(copyGroupInfoModel!!)

                            if (copyGroupInfoModel?.bfGroupBanned == true) {
                                EventBus.publishEvent(BanGroupMessageEvent(copyGroupInfoModel?.groupId
                                        ?: 0))
                            }
                            EventBus.publishEvent(FireStatusChangeEvent(mutableListOf(FireStatus(copyGroupInfoModel?.groupId
                                    ?: 0L, copyGroupInfoModel?.bfGroupReadCancel ?: false))))
                        } else {
                            AppLogcat.logger.e("demo", "更新群信息失败！！！")
                            error?.invoke()
                        }
                    }, {
                        AppLogcat.logger.e("demo", "更新群信息失败！！！")
                        error?.invoke()
                    })
                }, {
                    if (it is HttpException && it.errCode == 1001) {
                        RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
                            val groupModel = realm.where(GroupInfoModel::class.java).equalTo("groupId", groupId).findFirst()
                            if (groupModel != null) {
                                groupModel.isBfMember = false
                                realm.copyToRealmOrUpdate(groupModel)
                            }
                        }, {
                            AppLogcat.logger.e("demo", "更新群信息失败！！！")
                            error?.invoke()
                        }, {
                            AppLogcat.logger.e("demo", "更新群信息失败！！！")
                            error?.invoke()
                        })
                    } else {
                        AppLogcat.logger.e("demo", "更新群信息失败！！！${it.message}")
                        error?.invoke()
                    }
                })
    }


    /**
     * 同步数据,只要第一页，就会清空本地的数据，重新从网络获取
     * 群设置界面，群会话页面，只会获取前10条+群数量
     * type 1.(群会话)
     * type 2.(群设置)
     * type 3.(群成员)
     * type 4.(@)
     */
    override fun syncGroupAllMemberInfoNew(observable: Observable<ActivityEvent>?, pageNo: Int, pageSize: Int, groupId: Long, type: Int,
                                           complete: ((Int, Boolean, List<GroupMemberModel>) -> Unit)?, cacheComplete: ((Int, List<GroupMemberModel>) -> Unit)?, error: (() -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val sp = SharePreferencesStorage.createStorageInstance(CommonPref::class.java, "${myUid}_$groupId")
        var lastUpdateTime = 0L
        if (pageNo == 1) {
            lastUpdateTime = sp.getGroupMemberUpdateTime(0)
        }
        ArouterServiceManager.groupService.getGroupInfo(null, groupId, { groupInfo, _ ->
            val groupMemberModels = ArrayList<GroupMemberModel>()
            RealmCreator.executeGroupMembersTransactionAsync(myUid, groupId, { realm ->
                val allMembers = realm.where(GroupMemberModel::class.java)?.findAll()
                allMembers?.forEach { groupMemberModel ->
                    groupMemberModels.add(groupMemberModel.copyGroupMemberModel())
                }
            }, {
                when (type) {
                    1 -> when {
                        groupMemberModels.size == 0 -> {
                            Log.i("lzh", "type 1 net  $pageNo")
                            updateGroupAllMemberInfoByNet(observable, groupId, 0, pageNo, pageSize, type, complete, error)
                        }
                        groupInfo.memberCount != groupMemberModels.size && System.currentTimeMillis() - sp.getGroupMemberLastUpdateTime() > 2 * 60 * 60 * 1000 -> {
                            Log.i("lzh", "type 1 net  $pageNo")
                            updateGroupAllMemberInfoByNet(observable, groupId, lastUpdateTime, pageNo, pageSize, type, complete, error)
                        }
                        else -> {
                            Log.i("lzh", "type 1 local  $pageNo")
                            complete?.invoke(groupMemberModels.size, false, groupMemberModels)
                        }
                    }
                    2 -> {
                        cacheComplete?.invoke(groupMemberModels.size, groupMemberModels)
                        updateGroupAllMemberInfoByNet(observable, groupId, lastUpdateTime, pageNo, pageSize, type, complete, error)
                    }
                    3 -> {
                        if (groupInfo.memberCount != groupMemberModels.size) {
                            Log.i("lzh", "type 3 net $pageNo")
                            updateGroupAllMemberInfoByNet(observable, groupId, 0, pageNo, pageSize, type, complete, error)
                        } else {
                            Log.i("lzh", "type 3 local $pageNo")
                            complete?.invoke(groupMemberModels.size, false, groupMemberModels)
                        }
                    }
                    else -> {
                        if (groupInfo.memberCount != groupMemberModels.size) {
                            Log.i("lzh", "type 4 net $pageNo")
                            updateGroupAllMemberInfoByNet(observable, groupId, 0, pageNo, pageSize, type, complete, error)
                        } else {
                            Log.i("lzh", "type 4 local $pageNo")
                            complete?.invoke(groupMemberModels.size, false, groupMemberModels)
                        }
                    }
                }
            })
        })
    }

    private fun updateGroupAllMemberInfoByNet(observable: Observable<ActivityEvent>?, groupId: Long, lastUpdateTime: Long, pageNo: Int, pageSize: Int, type: Int,
                                              complete: ((Int, Boolean, List<GroupMemberModel>) -> Unit)?, error: (() -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        val sp = SharePreferencesStorage.createStorageInstance(CommonPref::class.java, "${myUid}_$groupId")
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .groupMemberList(object : HttpReq<GroupProto.GroupMemberListReq>() {
                    override fun getData(): GroupProto.GroupMemberListReq {
                        Log.i("lzh", "请求参数  groupId $groupId  lastUpdateTime $lastUpdateTime  pageNo $pageNo  pageSize $pageSize  ")
                        return GroupHttpReqCreator.createGroupMemberListReq(groupId, lastUpdateTime, pageNo, pageSize)
                    }
                })
                .getResult(observable, {
                    val memberNumberCount = it.count.toInt()
                    if (pageNo == 1 && it.membersCount == 0) {
                        // 数据没有改变过(请求第一页，但是并没有下发数据，说明数据没有发生改变)
                        AppLogcat.logger.d("demo", "群成员数据暂时没有发生改变！！！")
                        val groupMemberModels = ArrayList<GroupMemberModel>()
                        RealmCreator.executeGroupMembersTransactionAsync(myUid, groupId, { realm ->
                            val allMembers = realm.where(GroupMemberModel::class.java)?.findAll()
                            allMembers?.forEach { groupMemberModel ->
                                groupMemberModels.add(groupMemberModel.copyGroupMemberModel())
                            }
                        }, {
                            complete?.invoke(memberNumberCount, false, groupMemberModels)
                        })
                    } else {
                        val groupMemberModels = ArrayList<GroupMemberModel>()
                        Log.i("lzh", "返回数据  membersList size ${it.membersList.size}  count $memberNumberCount ")
                        if (it.membersList?.isNotEmpty() == true) {
                            it.membersList.forEach { member ->
                                groupMemberModels.add(GroupMemberModel.createGroupMember(member.user.uid, member.user.nickName, member.user.gender.number,
                                        member.user.icon, member.user.friendRelation.bfFriend, member.user.friendRelation.remarkName, member.groupId,
                                        member.type.number, member.groupNickName, member.score,
                                        member.user.userOnOrOffline.online, member.user.userOnOrOffline.createTime, member.user.userOnOrOffline.bfShow))
                            }
                        }
                        //把数据同步到本地,异步同步，不影响ＵＩ
                        RealmCreator.executeGroupMembersTransactionAsync(myUid, groupId, { realm ->
                            //如果是第一页，且lastUpdateTime == 0　就删除所有本地的数据
                            if (pageNo == 1 && lastUpdateTime == 0L) {
                                val allMembers = realm.where(GroupMemberModel::class.java)?.findAll()
                                allMembers?.deleteAllFromRealm()
                            }
                            if (groupMemberModels.isNotEmpty()) {
                                realm.copyToRealmOrUpdate(groupMemberModels)
                            }
                        })

                        if (pageNo == 1 && (type == 1 || type == 2)) {
                            sp.putGroupMemberTempUpdateTime(it.lastUpdateTime)
                        }

                        if (it.membersCount < pageSize && (type == 1 || type == 2)) {
                            AppLogcat.logger.d("demo", "获取群成员成功，结束同步！！！")
                            //结束同步
                            sp.putGroupMemberUpdateTime(sp.getGroupMemberTempUpdateTime())
                        }
                        val hasMore = if (it.membersList.size == 0) {
                            false
                        } else it.membersList.size >= pageSize

                        complete?.invoke(memberNumberCount, hasMore, groupMemberModels)
                    }
                    if (type == 1) {
                        //更新群成员，添加2分钟检测，两分钟内
                        sp.putGroupMemberLastUpdateTime(System.currentTimeMillis())
                    }
                }, {
                    AppLogcat.logger.e("demo", "获取群成员失败！！！")
                    error?.invoke()
                })
    }


    override fun getGroupMemberInfo(observable: Observable<ActivityEvent>?, groupId: Long, uid: Long, complete: ((GroupMemberModel, Boolean) -> Unit)?, error: (() -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        var copyGroupMemberInfoModel: GroupMemberModel? = null
        RealmCreator.executeGroupMembersTransactionAsync(myUid, groupId, { realm ->
            val groupMemberModel = realm.where(GroupMemberModel::class.java).equalTo("uid", uid).findFirst()
            groupMemberModel?.let {
                copyGroupMemberInfoModel = groupMemberModel.copyGroupMemberModel()
            }
        }, {
            if (copyGroupMemberInfoModel != null) {
                AppLogcat.logger.d("demo", "获取群成员信息缓存成功！！！")
                complete?.invoke(copyGroupMemberInfoModel!!, true)
            } else {
                updateGroupMemberInfoByNet(observable, myUid, groupId, uid, {
                    complete?.invoke(it, false)
                }, error)
            }
        }, {
            AppLogcat.logger.d("demo", "获取群成员信息失败！！！")
            error?.invoke()
        })
    }


    private fun updateGroupMemberInfoByNet(observable: Observable<ActivityEvent>?, myUid: Long, groupId: Long, uid: Long, updateComplete: ((GroupMemberModel) -> Unit)?, error: (() -> Unit)?) {
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .groupMemberDetail(object : HttpReq<GroupProto.GroupMemberDetailReq>() {
                    override fun getData(): GroupProto.GroupMemberDetailReq {
                        return GroupHttpReqCreator.createGroupMemberDetail(groupId, listOf(uid))
                    }
                })
                .getResult(observable, { result ->
                    val memberInfo = result.groupMembersList[0]
                    if (memberInfo != null) {
                        var copyGroupMemberInfoModel: GroupMemberModel? = null
                        RealmCreator.executeGroupMembersTransactionAsync(myUid, groupId, { realm ->
                            val groupMemberModel = GroupMemberModel.createGroupMember(memberInfo.user.uid, memberInfo.user.nickName, memberInfo.user.gender.number,
                                    memberInfo.user.icon, memberInfo.user.friendRelation.bfFriend, memberInfo.user.friendRelation.remarkName,
                                    memberInfo.groupId, memberInfo.type.number, memberInfo.groupNickName, memberInfo.score,
                                    memberInfo.user.userOnOrOffline.online, memberInfo.user.userOnOrOffline.createTime, memberInfo.user.userOnOrOffline.bfShow)
                            realm.copyToRealmOrUpdate(groupMemberModel)
                            copyGroupMemberInfoModel = groupMemberModel.copyGroupMemberModel()
                        }, {
                            if (copyGroupMemberInfoModel != null) {
                                AppLogcat.logger.d("demo", "更新群成员信息成功！！！")
                                updateComplete?.invoke(copyGroupMemberInfoModel!!)
                            } else {
                                AppLogcat.logger.e("demo", "更新群成员信息失败！！！")
                                error?.invoke()
                            }
                        }, {
                            AppLogcat.logger.e("demo", "更新群成员信息失败！！！")
                            error?.invoke()
                        })
                    } else {
                        AppLogcat.logger.e("demo", "更新群成员信息失败！！！")
                        error?.invoke()
                    }
                }, {
                    AppLogcat.logger.e("demo", "更新群成员信息失败！！！")
                    error?.invoke()
                })
    }

    override fun isGotoGroupSettingPermission(groupId: Long, uid: Long, complete: ((Boolean) -> Unit)?, error: (() -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
            val groupModel = realm.where(GroupInfoModel::class.java).equalTo("groupId", groupId).findFirst()
            if (groupModel == null || groupModel.isBfMember) {
                complete?.invoke(true)
            } else {
                complete?.invoke(false)
            }
        })
    }

    override fun updateGroupMemberRemarkName(groupId: Long, uid: Long, nickName: String, complete: (() -> Unit)?, error: (() -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeGroupMembersTransactionAsync(myUid, groupId, { realm ->
            val member = realm.where(GroupMemberModel::class.java)?.equalTo("uid", uid)?.findFirst()
            if (member != null) {
                member.remarkName = nickName
                realm.copyToRealmOrUpdate(member)
            }
        }, {
            EventBus.publishEvent(GroupMemberChangeEvent(groupId))
            complete?.invoke()
        }, {
            error?.invoke()
        })
    }

    override fun updateGroupMemberPermission(groupId: Long, uid: Long, memberPermission: Int, complete: (() -> Unit)?, error: (() -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeGroupMembersTransactionAsync(myUid, groupId, { realm ->
            val member = realm.where(GroupMemberModel::class.java)?.equalTo("uid", uid)?.findFirst()
            if (member != null) {
                member.type = memberPermission
                realm.copyToRealmOrUpdate(member)
            }
        }, {
            if (uid == myUid) {//如果uid 是 自己，就改变
                RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
                    val groupModel = realm.where(GroupInfoModel::class.java).equalTo("groupId", groupId).findFirst()
                    groupModel?.let {
                        groupModel.memberRole = memberPermission
                        realm.copyToRealmOrUpdate(groupModel)
                    }
                }, {
                    EventBus.publishEvent(GroupMemberChangeEvent(groupId))
                    complete?.invoke()
                })
            } else {
                EventBus.publishEvent(GroupMemberChangeEvent(groupId))
                complete?.invoke()
            }
        }, {
            error?.invoke()
        })
    }

    override fun transferGroupOwner(groupId: Long, oldOwnerUid: Long, newOwnerUid: Long, complete: (() -> Unit)?, error: (() -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeGroupMembersTransactionAsync(myUid, groupId, { realm ->
            val oldOwnerModel = realm.where(GroupMemberModel::class.java).equalTo("uid", oldOwnerUid).findFirst()
            val newOwnerModel = realm.where(GroupMemberModel::class.java).equalTo("uid", newOwnerUid).findFirst()
            if (oldOwnerModel != null && newOwnerModel != null) {
                oldOwnerModel.type = 2
                newOwnerModel.type = 0
                realm.copyToRealmOrUpdate(listOf(oldOwnerModel, newOwnerModel))
            }
        }, {
            RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
                val groupModel = realm.where(GroupInfoModel::class.java).equalTo("groupId", groupId).findFirst()
                groupModel?.let {
                    if (newOwnerUid == myUid) {//如果是自己，就改变
                        groupModel.memberRole = 0
                    } else {
                        groupModel.memberRole = 2
                    }
                    realm.copyToRealmOrUpdate(groupModel)
                }
            }, {
                AppLogcat.logger.d("demo", "删除群成员信息成功！！！")
                complete?.invoke()
            })
        }, {
            AppLogcat.logger.e("demo", "删除群成员信息失败！！！")
            error?.invoke()
        })
    }

    override fun updateGroupMemberInfoFromContact(groupId: Long, memberInfo: CommonProto.ContactsDetailBase, complete: ((Boolean) -> Unit)?, error: (() -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        var isChange = false
        RealmCreator.executeGroupMembersTransactionAsync(myUid, groupId, { realm ->
            val memberModel = realm.where(GroupMemberModel::class.java).equalTo("uid", memberInfo.userInfo.uid).findFirst()
            if (memberModel != null) {
                // 判断有没有更改
                if (memberModel.icon != memberInfo.userInfo?.icon ||
                        memberModel.nickName != memberInfo.userInfo?.nickName ||
                        memberModel.remarkName != memberInfo.userInfo?.friendRelation?.remarkName) {
                    isChange = true

                    memberModel.icon = memberInfo.userInfo?.icon
                    memberModel.nickName = memberInfo.userInfo?.nickName
                    memberModel.remarkName = memberInfo.userInfo?.friendRelation?.remarkName
                    realm.copyToRealmOrUpdate(memberModel)
                }
            }
        }, {
            AppLogcat.logger.d("demo", "更新群成员信息成功！！！")
            complete?.invoke(isChange)
        }, {
            AppLogcat.logger.e("demo", "添加群成员信息失败！！！")
            error?.invoke()
        })
    }

    override fun updateGroupMemberInfoFromSocket(groupId: Long, memberInfo: CommonProto.GroupMemberBase, complete: ((Boolean) -> Unit)?, error: (() -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        var isChange = false
        var isInsert = false
        RealmCreator.executeGroupMembersTransactionAsync(myUid, groupId, { realm ->
            var memberModel = realm.where(GroupMemberModel::class.java).equalTo("uid", memberInfo.user.uid).findFirst()
            if (memberModel != null) {
                // 判断有没有更改
                if (memberModel.icon != memberInfo.user?.icon ||
                        memberModel.nickName != memberInfo.user?.nickName ||
                        memberModel.remarkName != memberInfo.user?.friendRelation?.remarkName ||
                        memberModel.groupNickName != memberInfo.groupNickName) {
                    isChange = true
                }
            } else {
                isInsert = true
            }

            // 更新到数据库
            memberModel = GroupMemberModel.createGroupMember(memberInfo.user.uid, memberInfo.user.nickName, memberInfo.user.gender.number,
                    memberInfo.user.icon, memberInfo.user.friendRelation.bfFriend, memberInfo.user.friendRelation.remarkName,
                    memberInfo.groupId, memberInfo.type.number, memberInfo.groupNickName, memberInfo.score,
                    memberInfo.user.userOnOrOffline.online, memberInfo.user.userOnOrOffline.createTime, memberInfo.user.userOnOrOffline.bfShow)
            realm.copyToRealmOrUpdate(memberModel)
        }, {
            RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
                val groupModel = realm.where(GroupInfoModel::class.java).equalTo("groupId", groupId).findFirst()
                groupModel?.let {
                    if (isInsert) {
                        groupModel.memberCount = groupModel.memberCount + 1
                    }
                    if (memberInfo.user.uid == myUid) {//如果是自己，就改变
                        groupModel.memberRole = memberInfo.typeValue
                    }
                    realm.copyToRealmOrUpdate(groupModel)
                }
            }, {
                AppLogcat.logger.d("demo", "更新群成员信息成功！！！")
                complete?.invoke(isChange || isInsert)
            })
        }, {
            AppLogcat.logger.e("demo", "更新群成员信息失败！！！")
            error?.invoke()
        })
    }

    override fun deleteGroupMemberInfo(groupId: Long, uid: Long, complete: (() -> Unit)?, error: (() -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeGroupMembersTransactionAsync(myUid, groupId, { realm ->
            val groupMemberModel = realm.where(GroupMemberModel::class.java).equalTo("uid", uid).findFirst()
            groupMemberModel?.let {
                it.deleteFromRealm()
            }
        }, {
            RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
                val groupModel = realm.where(GroupInfoModel::class.java).equalTo("groupId", groupId).findFirst()
                groupModel?.let {
                    groupModel.memberCount = max(1, groupModel.memberCount - 1)
                    realm.copyToRealmOrUpdate(groupModel)
                }
            }, {
                // 删除指定的用户的所有回执记录并将用户发送的消息所需回执数量-1
                ArouterServiceManager.messageService.deleteToGroupAllMessageReceipt(groupId, uid)

                AppLogcat.logger.d("demo", "删除群成员信息成功！！！")
                complete?.invoke()
            })
        }, {
            AppLogcat.logger.e("demo", "删除群成员信息失败！！！")
            error?.invoke()
        })
    }

    override fun getAllGroupMembersInfoByCache(groupId: Long, getCount: Long, complete: ((List<GroupMemberModel>, Long) -> Unit)?, error: (() -> Unit)?) {
        val groupMemberInfos = ArrayList<GroupMemberModel>()
        var groupMemberCount = 0L
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeGroupMembersTransactionAsync(myUid, groupId, { realm ->
            groupMemberCount = realm.where(GroupMemberModel::class.java)?.count() ?: 0L
            val groupMemberModels = realm.where(GroupMemberModel::class.java)?.sort("sortScore", Sort.ASCENDING)?.limit(getCount)?.findAll()
            groupMemberModels?.forEach {
                groupMemberInfos.add(it.copyGroupMemberModel())
            }
        }, {
            AppLogcat.logger.d("demo", "获取群成员信息成功！！！")
            complete?.invoke(groupMemberInfos, groupMemberCount)
        }, {
            AppLogcat.logger.e("demo", "获取群成员信息失败！！！")
            error?.invoke()
        })
    }

    override fun getGroupMembersInfoByCache(groupId: Long, userId: List<Long>, complete: ((List<GroupMemberModel>) -> Unit)?, error: (() -> Unit)?) {
        val groupMemberInfos = ArrayList<GroupMemberModel>()
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeGroupMembersTransactionAsync(myUid, groupId, { realm ->
            userId.forEach {
                val groupMemberModel = realm.where(GroupMemberModel::class.java).equalTo("uid", it).findFirst()
                groupMemberModel?.let {
                    groupMemberInfos.add(groupMemberModel.copyGroupMemberModel())
                }
            }
        }, {
            AppLogcat.logger.d("demo", "获取群成员信息成功！！！")
            complete?.invoke(groupMemberInfos)
        }, {
            AppLogcat.logger.e("demo", "获取群成员信息失败！！！")
            error?.invoke()
        })
    }

    override fun getGroupMemberUids(groupId: Long, complete: ((List<Long>) -> Unit)?, error: (() -> Unit)?) {
        val groupMemberIds = ArrayList<Long>()
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeGroupMembersTransactionAsync(myUid, groupId, { realm ->
            val groupMemberModels = realm.where(GroupMemberModel::class.java).findAll()
            groupMemberModels.forEach {
                groupMemberIds.add(it.uid)
            }
        }, {
            AppLogcat.logger.d("demo", "获取群成员uid成功！！！")
            complete?.invoke(groupMemberIds)
        }, {
            AppLogcat.logger.e("demo", "获取群成员uid失败！！！")
            error?.invoke()
        })
    }

    override fun updateMyGroupChats(observable: Observable<ActivityEvent>?, complete: (() -> Unit)?, error: (() -> Unit)?) {
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .getGroupList(object : HttpReq<GroupProto.GroupContactListReq>() {
                    override fun getData(): GroupProto.GroupContactListReq {
                        return GroupHttpReqCreator.createGroupListReq()
                    }
                })
                .getResult(observable, {
                    val temp = mutableListOf<GroupProto.GroupBase>()
                    if (!it.groupsList.isNullOrEmpty()) {
                        temp.addAll(it.groupsList)
                    }

                    if (temp.isNotEmpty()) {
                        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                        val localGroupInfos = mutableListOf<GroupInfoModel>()
                        val idList = mutableListOf<Long>()
                        RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
                            temp.forEach { groupBean ->
                                var groupModel = realm.where(GroupInfoModel::class.java).equalTo("groupId", groupBean.groupId).findFirst()
                                idList.add(groupBean.groupId)
                                if (groupModel == null) {
                                    groupModel = GroupInfoModel.createGroup(groupBean.groupId, groupBean.hostId, groupBean.name, groupBean.pic,
                                            groupBean.createTime, "", false, false, true,
                                            groupBean.memberCount.toInt(), groupBean.bfJoinFriend, groupBean.bfShutup,
                                            false, false, false, false, 2, "", 0, true, groupBean.bfGroupReadCancel, groupBean.groupMsgCancelTime, groupBean.bfBanned)
                                    groupModel?.let { groupModelNotNull ->
                                        realm.copyToRealm(groupModelNotNull)
                                    }
                                } else {
                                    groupModel.hostId = groupBean.hostId
                                    groupModel.name = groupBean.name
                                    groupModel.pic = groupBean.pic
                                    groupModel.bfJoinCheck = groupBean.bfJoinCheck
                                    groupModel.createTime = groupBean.createTime
                                    groupModel.bfAddress = true
                                    groupModel.bfGroupReadCancel = groupBean.bfGroupReadCancel
                                    groupModel.groupMsgCancelTime = groupBean.groupMsgCancelTime
                                    realm.copyToRealmOrUpdate(groupModel)
                                }
                            }
                        }, {
                            RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
                                val command = realm.where(GroupInfoModel::class.java)
                                idList.forEach { id ->
                                    command.notEqualTo("groupId", id)
                                }
                                val list = command.findAll()
                                list.forEach { model ->
                                    model.bfAddress = false
                                }
                                realm.copyToRealmOrUpdate(list)
                            }, {

                            }) {

                            }


                            localGroupInfos.forEach { localInfo ->
                                var match = false
                                temp.forEach { netInfo ->
                                    if (localInfo.groupId == netInfo.groupId) {
                                        match = true
                                    }
                                }
                                if (match) {

                                }

                            }
                            AppLogcat.logger.d("demo", "同步我的群成功！！！")
                            complete?.invoke()
                        }, {
                            AppLogcat.logger.e("demo", "同步我的群失败！！！")
                            error?.invoke()
                        })
                    } else {
                        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                        RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
                            realm.delete(GroupInfoModel::class.java)
                        }, {
                            AppLogcat.logger.d("demo", "同步我的群成功！！！")
                            complete?.invoke()
                        }, {
                            AppLogcat.logger.e("demo", "同步我的群失败！！！")
                            error?.invoke()
                        })
                    }
                }, {
                    AppLogcat.logger.e("demo", "同步我的群失败！！！")
                    error?.invoke()
                })
    }

//    private fun updateMyGroupChats(observable: Observable<ActivityEvent>?, temp: ArrayList<GroupProto.GroupBase>, complete: (() -> Unit)?, error: (() -> Unit)?) {
//
//    }

    override fun quitGroup(observable: Observable<ActivityEvent>?, groupId: Long, complete: (() -> Unit)?, error: (() -> Unit)?) {
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .groupExit(object : HttpReq<GroupProto.GroupExitReq>() {
                    override fun getData(): GroupProto.GroupExitReq {
                        return GroupHttpReqCreator.createGroupExitReq(groupId)
                    }
                })
                .getResult(observable, {
                    val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                    RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
                        // 从群列表删除
                        val groupModel = realm.where(GroupInfoModel::class.java).equalTo("groupId", groupId).findFirst()
                        groupModel?.deleteFromRealm()
                    }, {
                        //发送退群事件 就关闭所有界面，退出到主界面
                        val postcard = ARouter.getInstance().build("/app/act/main")
                        LogisticsCenter.completion(postcard)
                        val tClass = postcard.destination
                        ActivitiesHelper.getInstance().closeToTarget(tClass)

                        //删除会话和聊天记录
                        ArouterServiceManager.messageService.deleteChat(ChatModel.CHAT_TYPE_GROUP, groupId, {
                            AppLogcat.logger.d("demo", "退群成功！！！")
                            complete?.invoke()
                        }, {
                            AppLogcat.logger.e("demo", "退群失败！！！")
                            error?.invoke()
                        })
                    }, {
                        AppLogcat.logger.e("demo", "退群失败！！！")
                        error?.invoke()
                    })
                }, {
                    AppLogcat.logger.e("demo", "退群失败！！！")
                    error?.invoke()
                })
    }

    override fun saveGroupToContacts(observable: Observable<ActivityEvent>?, groupId: Long, isSave: Boolean, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        val param = GroupProto.GroupParam.newBuilder().setGroupId(groupId).setAddress(isSave).build()
        setGroupParam(observable, groupId, GroupProto.GroupOperator.ADDRESS, param, {
            it.bfAddress = isSave
        }, {
            complete?.invoke()
        }, error)
    }

    override fun setGroupMessageQuiet(observable: Observable<ActivityEvent>?, groupId: Long, isQuiet: Boolean, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        val param = GroupProto.GroupParam.newBuilder().setGroupId(groupId).setDisturb(isQuiet).build()
        setGroupParam(observable, groupId, GroupProto.GroupOperator.DISTURB, param, {
            it.bfDisturb = isQuiet
        }, {
            complete?.invoke()
        }, error)
    }

    override fun setGroupJoinCheck(observable: Observable<ActivityEvent>?, groupId: Long, joinCheck: Boolean, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        val param = GroupProto.GroupParam.newBuilder().setGroupId(groupId).setJoinCheck(joinCheck).build()
        setGroupParam(observable, groupId, GroupProto.GroupOperator.JOIN_CHECK, param, {
            it.bfJoinCheck = joinCheck
        }, {
            complete?.invoke()
        }, error)
    }

    override fun saveGroupPic(observable: Observable<ActivityEvent>?, groupId: Long, groupPic: String, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        val param = GroupProto.GroupParam.newBuilder().setGroupId(groupId).setPic(groupPic).build()
        setGroupParam(observable, groupId, GroupProto.GroupOperator.UPDATE_GROUP_PIC, param, {
            it.pic = groupPic
        }, {
            complete?.invoke()
        }, error)
    }

    override fun saveGroupName(observable: Observable<ActivityEvent>?, groupId: Long, groupName: String, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        val param = GroupProto.GroupParam.newBuilder().setGroupId(groupId).setGroupName(groupName).build()
        setGroupParam(observable, groupId, GroupProto.GroupOperator.UPDATE_GROUP_NAME, param, {
            it.name = groupName
        }, {
            complete?.invoke()
        }, error)
    }

    override fun saveGroupUserName(observable: Observable<ActivityEvent>?, groupId: Long, groupUserName: String, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        val param = GroupProto.GroupParam.newBuilder().setGroupId(groupId).setGroupNickName(groupUserName).build()
        setGroupParam(observable, groupId, GroupProto.GroupOperator.UPDATE_NICK_NAME, param, {
            it.groupNickName = groupUserName
        }, {
            complete?.invoke()
        }, error)
    }

    override fun shutUpGroup(observable: Observable<ActivityEvent>?, groupId: Long, isOpen: Boolean, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        val param = GroupProto.GroupParam.newBuilder().setGroupId(groupId).setShutup(isOpen).build()
        setGroupParam(observable, groupId, GroupProto.GroupOperator.SHUTUP, param, {
            it.forShutupGroup = isOpen
        }, {
            complete?.invoke()
        }, error)
    }

    private fun setGroupParam(observable: Observable<ActivityEvent>?, groupId: Long, op: GroupProto.GroupOperator, param: GroupProto.GroupParam,
                              updateModelUnit: (GroupInfoModel) -> Unit, complete: ((GroupProto.GroupUpdateResp) -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null) {
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .groupUpdate(object : HttpReq<GroupProto.GroupUpdateReq>() {
                    override fun getData(): GroupProto.GroupUpdateReq {
                        return GroupHttpReqCreator.createGroupUpdateReq(op, param)
                    }
                })
                .getResult(observable, {
                    val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                    RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
                        val groupModel = realm.where(GroupInfoModel::class.java).equalTo("groupId", groupId).findFirst()
                        groupModel?.let {
                            updateModelUnit.invoke(groupModel)
                            realm.copyToRealmOrUpdate(groupModel)
                        }
                    }, {
                        complete?.invoke(it)
                    }, {
                        error?.invoke(it)
                    })
                }, {
                    error?.invoke(it)
                })
    }

    override fun saveGroupJoinFriend(observable: Observable<ActivityEvent>?, groupId: Long, isOpen: Boolean, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        val param = GroupProto.GroupParam.newBuilder().setGroupId(groupId).setJoinFriend(isOpen).build()
        setGroupParam(observable, groupId, GroupProto.GroupOperator.JOIN_FRIEND, param, {
            it.forbidJoinFriend = isOpen
        }, {
            complete?.invoke()
        }, error)
    }

    /**
     * 群管理员列表
     */
    override fun getGroupAdminList(observable: Observable<ActivityEvent>?, groupId: Long, complete: ((GroupProto.GroupAdminListResp) -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .groupAdminList(object : HttpReq<GroupProto.GroupAdminListReq>() {
                    override fun getData(): GroupProto.GroupAdminListReq {
                        return GroupHttpReqCreator.createGroupAdminListReqRecord(groupId)
                    }
                })
                .getResult(observable, {
                    complete?.invoke(it)
                }, {
                    AppLogcat.logger.e("demo", "获取管理员列表失败")
                    error?.invoke(it)
                })
    }

    /**
     * 移除管理员
     */
    override fun removeGroupAdmin(observable: Observable<ActivityEvent>?, groupId: Long, adminUid: Long, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .groupRemoveAdmin(object : HttpReq<GroupProto.GroupRemoveAdminReq>() {
                    override fun getData(): GroupProto.GroupRemoveAdminReq {
                        return GroupHttpReqCreator.createGroupRemoveAdminReqRecord(groupId, adminUid)
                    }
                })
                .getResult(observable, {
                    // 重新设置为成员
                    updateGroupMemberPermission(groupId, adminUid, 2)
                    complete?.invoke()
                }, {
                    AppLogcat.logger.e("demo", "移除管理员失败")
                    error?.invoke(it)
                })
    }

    /**
     * 编辑管理员权限
     */
    override fun editGroupAdminRight(observable: Observable<ActivityEvent>?, groupId: Long, targetUid: Long, bfJoinCheck: Boolean, bfPushNotice: Boolean, bfUpdateData: Boolean, bfSetAdmin: Boolean, groupOperator: GroupProto.GroupOperator, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .groupEditAdminRight(object : HttpReq<GroupProto.GroupEditAdminRightReq>() {
                    override fun getData(): GroupProto.GroupEditAdminRightReq {
                        val rightParam = CommonProto.AdminRightBase.newBuilder().setBfJoinCheck(bfJoinCheck).setBfPushNotice(bfPushNotice).setBfUpdateData(bfUpdateData).setBfSetAdmin(bfSetAdmin).build()
                        return GroupHttpReqCreator.createGroupEditAdminRightReqRecord(groupId, targetUid, rightParam, groupOperator)
                    }
                })
                .getResult(observable, {
                    if (groupOperator == GroupProto.GroupOperator.ADD_ADMIN) {
                        // 设置为管理员
                        updateGroupMemberPermission(groupId, targetUid, 1)
                    }

                    complete?.invoke()
                }, {
                    AppLogcat.logger.e("demo", "编辑管理员权限失败  ${it.message}")
                    error?.invoke(it)
                })
    }

    /**
     * 获取群公告
     */
    override fun getNotice(observable: Observable<ActivityEvent>?, groupId: Long, noticeId: Long, complete: ((CommonProto.GroupNoticeBase) -> Unit)?, error: (() -> Unit)?) {
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .groupNoticeDetail(object : HttpReq<GroupProto.GroupNoticeDetailReq>() {
                    override fun getData(): GroupProto.GroupNoticeDetailReq {
                        return GroupHttpReqCreator.createGroupNoticeDetail(groupId, noticeId)
                    }
                })
                .getResult(observable, { result ->
                    complete?.invoke(result.notice)
                }, {
                    AppLogcat.logger.e("demo", "获取群公告失败！！！")
                    error?.invoke()
                })
    }

    /**
     * 发布群公告
     */
    override fun pushNotice(observable: Observable<ActivityEvent>?, groupId: Long, notify: Boolean, content: String, complete: ((Long) -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        val param = GroupProto.GroupParam.newBuilder().setGroupId(groupId).setNotice(content).setBfAll(notify).build()
        setGroupParam(observable, groupId, GroupProto.GroupOperator.NOTICE, param, {
            it.notice = content
        }, {
            complete?.invoke(it.noticeId)
        }, error)
    }

    /**
     * 清除群公告
     */
    override fun clearNotice(observable: Observable<ActivityEvent>?, groupId: Long, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        val param = GroupProto.GroupParam.newBuilder().setGroupId(groupId).build()
        setGroupParam(observable, groupId, GroupProto.GroupOperator.CLEAR_NOTICE, param, {
            it.notice = ""
        }, {
            complete?.invoke()
        }, error)
    }

    override fun getGroupRealm(): Realm {
        return RealmCreator.getGroupsRealm(AccountManager.getLoginAccount(AccountInfo::class.java).getUserId())
    }

    override fun getContactsRealm(): Realm {
        return RealmCreator.getContactsRealm(AccountManager.getLoginAccount(AccountInfo::class.java).getUserId())
    }

    override fun setBurnAfterRead(observable: Observable<ActivityEvent>?, groupId: Long, burnAfterRead: Boolean, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        val param = GroupProto.GroupParam.newBuilder().setGroupId(groupId).setBfGroupReadCancel(burnAfterRead).build()
        setGroupParam(observable, groupId, GroupProto.GroupOperator.GROUP_READ_CANCEL_OP, param, {
            it.bfGroupReadCancel = burnAfterRead
        }, {
            complete?.invoke()
        }, error)
    }

    override fun setBurnAfterReadTime(observable: Observable<ActivityEvent>?, groupId: Long, time: Int, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        val param = GroupProto.GroupParam.newBuilder().setGroupId(groupId).setGroupMsgCancelTime(time).build()
        setGroupParam(observable, groupId, GroupProto.GroupOperator.GROUP_READ_CANCEL_TIME_OP, param, {
            it.groupMsgCancelTime = time
        }, {
            complete?.invoke()
        }, error)
    }

    override fun setGroupMemberBanTime(observable: Observable<ActivityEvent>?, groupId: Long, targetUid: Long, time: Int, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .setGroupMemberBanWord(object : HttpReq<GroupProto.GroupMemberShutupReq>() {
                    override fun getData(): GroupProto.GroupMemberShutupReq {
                        return GroupHttpReqCreator.createGroupMemberBanword(groupId, targetUid, time)
                    }
                })
                .getResult(observable, { result ->
                    complete?.invoke()
                }, {
                    error?.invoke(it)
                })

    }

    override fun getGroupMemberRole(observable: Observable<ActivityEvent>?, groupId: Long, targetUid: Long, complete: ((Int) -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        var groupMember: GroupMemberModel? = null
        RealmCreator.executeGroupMembersTransactionAsync(myUid, groupId, { realm ->
            val member = realm.where(GroupMemberModel::class.java)?.equalTo("uid", targetUid)?.findFirst()
            member?.let {
                groupMember = it.copyGroupMemberModel()
            }
        }, {
            if (groupMember != null) {
                complete?.invoke(groupMember?.type ?: 2)
            } else {
                complete?.invoke(2)
            }
        }, {
            error?.invoke(it)
        })

    }

    override fun disableGroup(observable: Observable<ActivityEvent>?, groupId: Long, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        HttpManager.getStore(GroupHttpProtocol::class.java)
                .disableGroup(object : HttpReq<GroupProto.DisableGroupReq>() {
                    override fun getData(): GroupProto.DisableGroupReq {
                        return GroupHttpReqCreator.createDisableGroup(groupId)
                    }
                })
                .getResult(observable, { result ->
                    complete?.invoke()
                }, {
                    error?.invoke(it)
                })
    }


    override fun deleteGroup(observable: Observable<ActivityEvent>?, groupId: Long, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?) {
        val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
        RealmCreator.executeGroupsTransactionAsync(myUid, { realm ->
            val groupModel = realm.where(GroupInfoModel::class.java).equalTo("groupId", groupId).findFirst()
            groupModel?.let {
                it.deleteFromRealm()
            }
        }, {
            complete?.invoke()
        }, {
            error?.invoke(it)
        })
    }
}