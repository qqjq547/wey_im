package framework.telegram.business.bridge.service

import android.widget.TextView
import com.alibaba.android.arouter.facade.template.IProvider
import com.im.domain.pb.CommonProto
import com.im.domain.pb.ContactsProto
import com.im.pb.IMPB
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.ideas.common.model.contacts.ContactDataModel
import io.reactivex.Observable

/**
 * Created by lzh on 19-5-28.
 * INFO:
 */
interface IContactService : IProvider {

    /**
     * 获取联系人资料，若本地有使用本地缓存，本地没有则获取网络最新数据
     *
     * 建立私聊消息会话和进入私聊界面时调用
     *
     * complete Boolean 返回值表示是否是本地缓存
     */
    fun getContactInfo(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        complete: ((ContactDataModel, Boolean) -> Unit)? = null,
        error: (() -> Unit)? = null
    )

    fun getContactInfoCache(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        complete: ((ContactDataModel) -> Unit)? = null,
        error: (() -> Unit)? = null
    )

    fun getContactsInfoCache(
        observable: Observable<ActivityEvent>?,
        userIds: List<Long>,
        complete: ((List<ContactDataModel>) -> Unit)? = null,
        error: (() -> Unit)? = null
    )

    fun getContactInfoFromQr(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        qrCode: String,
        complete: ((ContactDataModel, String) -> Unit)? = null,
        error: (() -> Unit)? = null
    )

    /**
     * 更新联系人资料，获取网络最新数据并覆盖本地缓存
     *
     * 获取联系人详情时调用
     * 同意添加对方为联系人时调用
     * 被同意添加为对方联系人时调用
     * 收到对方消息时，但对方并不在联系人列表时调用
     *
     * complete Boolean 返回值表示是否是本地缓存
     */
    fun updateContactInfo(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        cacheComplete: ((ContactDataModel) -> Unit)? = null,
        updateComplete: ((ContactDataModel) -> Unit)? = null,
        error: (() -> Unit)? = null
    )

    fun updateContactInfo(
        observable: Observable<ActivityEvent>?,
        detail: CommonProto.ContactsDetailBase,
        complete: ((ContactDataModel) -> Unit)? = null,
        error: (() -> Unit)? = null
    )

    fun updateContactInfo(
        observable: Observable<ActivityEvent>?,
        uid: Long,
        bfReadReceipt: Boolean,
        complete: ((ContactDataModel) -> Unit)? = null,
        error: (() -> Unit)? = null
    )

    fun updateContactInfoByNet(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        groupId: Long,
        complete: ((ContactDataModel, Int, String) -> Unit)? = null,
        error: (() -> Unit)? = null
    )

    /**
     * 判断userId是否在联系人列表中
     */
    fun isInContactList(
        userId: Long,
        complete: ((Boolean) -> Unit)? = null,
        error: (() -> Unit)? = null
    )

    /**
     * 同步所有联系人
     *
     * 用户登录或重新获取token时调用
     */
    fun syncAllContact(
        observable: Observable<ActivityEvent>?,
        complete: (() -> Unit)? = null,
        error: (() -> Unit)? = null
    )

    /**
     * 获取所有联系人
     */
    fun getAllContact(
        complete: ((List<ContactDataModel>) -> Unit)? = null,
        error: (() -> Unit)? = null
    )

    /**
     * 同意添加联系人请求
     */
    fun agreeContactReq(
        observable: Observable<ActivityEvent>?,
        applyUid: Long,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    /**
     * 忽略添加联系人好友请求
     */
    fun deleteContactReq(
        observable: Observable<ActivityEvent>?,
        applyUid: Long,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    /**
     * 同步所有添加联系人请求
     *
     * 进入添加联系人列表时调用（调用后，后台会将用户的红点数字清零）
     */
    fun updateAllContactReq(
        observable: Observable<ActivityEvent>?,
        complete: (() -> Unit)? = null,
        error: (() -> Unit)? = null
    )

    /**
     * 消息免打扰
     */
    fun setContactMessageQuiet(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        isQuiet: Boolean,
        complete: (() -> Unit)? = null,
        error: ((String) -> Unit)? = null
    )

    /**
     * 设置星标
     */
    fun setContactStar(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        isStar: Boolean,
        complete: (() -> Unit)? = null,
        error: ((String) -> Unit)? = null
    )

    /**
     * 设置note
     */
    fun setContactNote(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        nickName: String,
        note: String,
        complete: (() -> Unit)? = null,
        error: ((String) -> Unit)? = null
    )

    /**
     * 设置描述
     */
    fun setContactDescribe(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        describe: String,
        complete: (() -> Unit)? = null,
        error: ((String) -> Unit)? = null
    )

    /**
     * 设置是否开启阅后即焚
     */
    fun setBurnAfterRead(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        burnAfterRead: Boolean,
        complete: (() -> Unit)? = null,
        error: ((String) -> Unit)? = null
    )

    /**
     * 设置阅后即焚时间
     */
    fun setBurnAfterReadTime(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        time: Int,
        complete: (() -> Unit)? = null,
        error: ((String) -> Unit)? = null
    )

    /**
     * 设置是否开启截屏通知
     */
    fun setContactScreenshot(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        screenshot: Boolean,
        complete: (() -> Unit)? = null,
        error: ((String) -> Unit)? = null
    )


    /**
     * 设置黑名单
     */
    fun setContactBlack(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        isBlack: Boolean,
        complete: (() -> Unit)? = null,
        error: ((String) -> Unit)? = null
    )

    /**
     * 设置删除用户成员
     */
    fun setContactDelete(
        observable: Observable<ActivityEvent>?,
        userId: Long,
        complete: (() -> Unit)? = null,
        error: (() -> Unit)? = null
    )

    fun syncAllBlackContact(
        observable: Observable<ActivityEvent>?,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    fun syncAllDisShowOnlineContact(
        observable: Observable<ActivityEvent>?,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    fun getAllDisShowOnlineCacheContacts(
        complete: ((List<ContactDataModel>) -> Unit)? = null,
        error: (() -> Unit)? = null
    )

    fun setContactLastOnline(
        observable: Observable<ActivityEvent>?,
        onlineStatus: List<CommonProto.UserOnOrOffLine>,
        complete: (() -> Unit)? = null,
        error: ((Throwable) -> Unit)? = null
    )

    fun showOnlineStatus(
        uid: Long,
        isShowLastOnlineTime: Boolean,
        isOnlineStatus: Boolean,
        lastOnlineTime: Long,
        statusView: TextView
    )

    fun addDisShowOnlineContacts(
        observable: Observable<ActivityEvent>?,
        uids: List<Long>,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    )

    fun deleteDisShowOnlineContact(
        observable: Observable<ActivityEvent>?,
        uid: Long,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    )

    fun addFriend(
        identify: String,
        userId: Long,
        findSign: String,
        complete: () -> Unit,
        error: (Int, String) -> Unit
    )

    fun getAddToken(
        identify: String,
        userId: Long,
        findSign: String,
        complete: (Long, String) -> Unit,
        error: (Int, String) -> Unit
    )

    fun checkFriendShip(
        myUid: Long,
        targetUid: Long,
        complete: ((Boolean) -> Unit),
        error: ((Throwable) -> Unit)?
    )

    fun updataFriendShip(
        targetUid: Long,
        isDelete: Boolean,
        complete: (() -> Unit)?,
        error: ((Throwable) -> Unit)?
    )
}
