package framework.telegram.business.bridge.service

import com.alibaba.android.arouter.facade.template.IProvider
import com.im.domain.pb.CommonProto
import com.im.domain.pb.GroupProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.group.GroupMemberModel
import io.reactivex.Observable
import io.realm.Realm

/**
 * Created by lzh on 19-5-28.
 * INFO:
 */
interface IGroupService : IProvider {

    /**
     * 获取群资料，若本地有使用本地缓存，本地没有则获取网络最新数据
     *
     * 建立群聊消息会话和进入群聊界面时调用
     *
     * complete Boolean 返回值表示是否是本地缓存
     */
    fun getGroupInfo(observable: Observable<ActivityEvent>?, groupId: Long, complete: ((GroupInfoModel, Boolean) -> Unit)? = null, error: (() -> Unit)? = null)

    /**
     * 获取缓存数据
     *
     */
    fun updateGroupInfo(observable: Observable<ActivityEvent>?, groupId: Long, cacheComplete: ((GroupInfoModel) -> Unit)?, updateComplete: ((GroupInfoModel) -> Unit)?, error: (() -> Unit)? = null)

    fun updateGroupInfoByCache(observable: Observable<ActivityEvent>?, groupId: Long, cacheComplete: ((GroupInfoModel) -> Unit)?, error: (() -> Unit)? = null)

    fun updateGroupInfoByNet(observable: Observable<ActivityEvent>?, groupId: Long, updateComplete: ((GroupInfoModel) -> Unit)?, error: (() -> Unit)? = null)

    fun getGroupMemberInfo(observable: Observable<ActivityEvent>?, groupId: Long, uid: Long, complete: ((GroupMemberModel, Boolean) -> Unit)? = null, error: (() -> Unit)? = null)

    fun syncGroupAllMemberInfoNew(observable: Observable<ActivityEvent>?, pageNo: Int, pageSize: Int, groupId: Long, type: Int = 1, complete: ((Int, Boolean, List<GroupMemberModel>) -> Unit)? = null, cacheComplete: ((Int, List<GroupMemberModel>) -> Unit)? = null, error: (() -> Unit)? = null)

    /**
     * 将同步到群里的成员信息里,通过socket
     */
    fun updateGroupMemberInfoFromSocket(groupId: Long, memberInfo: CommonProto.GroupMemberBase, complete: ((Boolean) -> Unit)? = null, error: (() -> Unit)? = null)

    /**
     * 如果通讯录的人员信息变化,将同步到群里的成员信息里(群Id = 会话ID)
     */
    fun updateGroupMemberInfoFromContact(groupId: Long, memberInfo: CommonProto.ContactsDetailBase, complete: ((Boolean) -> Unit)? = null, error: (() -> Unit)? = null)

    /**
     * 删除一个成员信息
     */
    fun deleteGroupMemberInfo(groupId: Long, uid: Long, complete: (() -> Unit)? = null, error: (() -> Unit)? = null)

    fun isGotoGroupSettingPermission(groupId: Long, uid: Long, complete: ((Boolean) -> Unit)? = null, error: (() -> Unit)? = null)

    fun isGroupMemberByNet(observable: Observable<ActivityEvent>?, groupId: Long, complete: ((Boolean) -> Unit)?)

    fun updateGroupMemberRemarkName(groupId: Long, uid: Long, nickName: String, complete: (() -> Unit)? = null, error: (() -> Unit)? = null)

    fun updateGroupMemberPermission(groupId: Long, uid: Long, memberPermission: Int, complete: (() -> Unit)? = null, error: (() -> Unit)? = null)

    fun transferGroupOwner(groupId: Long, oldOwnerUid: Long, newOwnerUid: Long, complete: (() -> Unit)? = null, error: (() -> Unit)? = null)

    fun getAllGroupMembersInfoByCache(groupId: Long, getCount: Long, complete: ((List<GroupMemberModel>, Long) -> Unit)? = null, error: (() -> Unit)? = null)

    fun getGroupMembersInfoByCache(groupId: Long, userId: List<Long>, complete: ((List<GroupMemberModel>) -> Unit)? = null, error: (() -> Unit)? = null)

    /**
     * 获取群成员列表的所有uid
     */
    fun getGroupMemberUids(groupId: Long, complete: ((List<Long>) -> Unit)? = null, error: (() -> Unit)? = null)

    /**
     * 同步联系人中的群组
     *
     * 进入"我的群组"时调用
     */
    fun updateMyGroupChats(observable: Observable<ActivityEvent>?, complete: (() -> Unit)? = null, error: (() -> Unit)? = null)

    /**
     * 退出群组
     */
    fun quitGroup(observable: Observable<ActivityEvent>?, groupId: Long, complete: (() -> Unit)? = null, error: (() -> Unit)? = null)

    /**
     * 保存群组到联系人列表
     */
    fun saveGroupToContacts(observable: Observable<ActivityEvent>?, groupId: Long, isSave: Boolean, complete: (() -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    /**
     * 群组消息免打扰
     */
    fun setGroupMessageQuiet(observable: Observable<ActivityEvent>?, groupId: Long, isQuiet: Boolean, complete: (() -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    /**
     * 群组设置进群审核
     */
    fun setGroupJoinCheck(observable: Observable<ActivityEvent>?, groupId: Long, joinCheck: Boolean, complete: (() -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    /**
     * 全员禁言
     */
    fun shutUpGroup(observable: Observable<ActivityEvent>?, groupId: Long, isOpen: Boolean, complete: (() -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    fun saveGroupPic(observable: Observable<ActivityEvent>?, groupId: Long, groupPic: String, complete: (() -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    fun saveGroupName(observable: Observable<ActivityEvent>?, groupId: Long, groupName: String, complete: (() -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    fun saveGroupUserName(observable: Observable<ActivityEvent>?, groupId: Long, groupUserName: String, complete: (() -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    fun saveGroupJoinFriend(observable: Observable<ActivityEvent>?, groupId: Long, isOpen: Boolean, complete: (() -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    fun getGroupRealm(): Realm

    fun getContactsRealm(): Realm


    /**
     * 群管理员
     */
    /**
     * 群管理员列表
     */
    fun getGroupAdminList(observable: Observable<ActivityEvent>?, groupId: Long, complete: ((GroupProto.GroupAdminListResp) -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    /**
     * 移除管理员
     */
    fun removeGroupAdmin(observable: Observable<ActivityEvent>?, groupId: Long, adminUid: Long, complete: (() -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    /**
     * 编辑管理员权限
     */
    fun editGroupAdminRight(observable: Observable<ActivityEvent>?, groupId: Long, targetUid: Long, bfJoinCheck: Boolean, bfPushNotice: Boolean, bfUpdateData: Boolean, bfSetAdmin: Boolean, groupOperator: GroupProto.GroupOperator, complete: (() -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    /**
     * 获取群公告
     */
    fun getNotice(observable: Observable<ActivityEvent>?, groupId: Long, noticeId: Long, complete: ((CommonProto.GroupNoticeBase) -> Unit)? = null, error: (() -> Unit)? = null)

    /**
     * 发布群公告
     */
    fun pushNotice(observable: Observable<ActivityEvent>?, groupId: Long, notify: Boolean, content: String, complete: ((Long) -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    /**
     * 清除群公告
     */
    fun clearNotice(observable: Observable<ActivityEvent>?, groupId: Long, complete: (() -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    fun clearGroupReq(observable: Observable<ActivityEvent>?, complete: (() -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    /**
     * 阅后即焚
     */
    fun setBurnAfterRead(observable: Observable<ActivityEvent>?, groupId: Long, burnAfterRead: Boolean, complete: (() -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    fun setBurnAfterReadTime(observable: Observable<ActivityEvent>?, groupId: Long, time: Int, complete: (() -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)


    fun setGroupMemberBanTime(observable: Observable<ActivityEvent>?, groupId: Long, targetUid: Long, time: Int, complete: (() -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    fun getGroupMemberRole(observable: Observable<ActivityEvent>?, groupId: Long, targetUid: Long, complete: ((Int) -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    fun disableGroup(observable: Observable<ActivityEvent>?, groupId: Long, complete: (() -> Unit)? = null, error: ((e: Throwable) -> Unit)? = null)

    fun deleteGroup(observable: Observable<ActivityEvent>?, groupId: Long, complete: (() -> Unit)?, error: ((e: Throwable) -> Unit)?)
}