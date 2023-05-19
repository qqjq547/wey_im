package framework.ideas.common.model.group;

import com.chad.library.adapter.base.entity.MultiItemEntity;

import framework.telegram.support.system.pinyin.FastPinyin;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class GroupInfoModel extends RealmObject implements MultiItemEntity {

    public static GroupInfoModel createGroup(long groupId, long hostId, String name, String pic, long createTime, String groupNickName,
                                             boolean bfStar, boolean bfDisturb, boolean bfAddress, int memberCount,
                                             boolean forbidJoinFriend, boolean forShutupGroup,
                                             boolean bfJoinCheck, boolean bfUpdateData, boolean bfPushNotice, boolean bfSetAdmin,
                                             int memberRole, String notice, long noticeId, boolean bfMember, boolean bfGroupReadCancel, int groupMsgCancelTime, boolean bfGroupBanned) {
        GroupInfoModel model = new GroupInfoModel();
        model.groupId = groupId;
        model.hostId = hostId;
        model.name = name;
        model.searchName =  FastPinyin.Companion.toAllPinyin(name);
        model.shortName =  FastPinyin.Companion.toAllPinyinFirst(name,false);
        model.pic = pic;
        model.bfJoinCheck = bfJoinCheck;
        model.createTime = createTime;
        model.groupNickName = groupNickName;
        model.bfStar = bfStar;
        model.bfDisturb = bfDisturb;
        model.bfAddress = bfAddress;
        model.memberCount = memberCount;
        model.forbidJoinFriend = forbidJoinFriend;
        model.forShutupGroup = forShutupGroup;
        model.bfUpdateData = bfUpdateData;
        model.bfPushNotice = bfPushNotice;
        model.bfSetAdmin = bfSetAdmin;
        model.memberRole = memberRole;
        model.notice = notice;
        model.noticeId = noticeId;
        model.bfMember = bfMember;
        model.bfGroupReadCancel = bfGroupReadCancel;
        model.groupMsgCancelTime = groupMsgCancelTime;
        model.bfGroupBanned = bfGroupBanned;
        return model;
    }

    public GroupInfoModel copyGroupInfoModel() {
        GroupInfoModel model = new GroupInfoModel();
        model.groupId = groupId;
        model.hostId = hostId;
        model.name = name;
        model.searchName =  searchName;
        model.shortName =  shortName;
        model.pic = pic;
        model.createTime = createTime;
        model.groupNickName = groupNickName;
        model.bfStar = bfStar;
        model.bfDisturb = bfDisturb;
        model.bfAddress = bfAddress;
        model.memberCount = memberCount;
        model.forbidJoinFriend = forbidJoinFriend;
        model.forShutupGroup = forShutupGroup;
        model.bfJoinCheck = bfJoinCheck;
        model.bfUpdateData = bfUpdateData;
        model.bfPushNotice = bfPushNotice;
        model.bfSetAdmin = bfSetAdmin;
        model.memberRole = memberRole;
        model.notice = notice;
        model.noticeId = noticeId;
        model.bfMember = bfMember;
        model.bfGroupReadCancel = bfGroupReadCancel;
        model.groupMsgCancelTime = groupMsgCancelTime;
        model.bfGroupBanned = bfGroupBanned;
        return model;
    }

    /**
     * 群信息相关
     */
    @PrimaryKey
    private long groupId; // 群ID

    private long hostId; // 群主UID

    private String name; // 群名称

    private String searchName; // 群名称(搜索)

    private String shortName; // 群名称)

    private String pic; // 群图标URL

    private long createTime; // 建群时间戳

    private int memberCount; // 群成员数量

    /**
     * 群设置相关
     */
    private String groupNickName; // 用户群昵称


    private boolean bfStar; // 星标

    private boolean bfDisturb; // 免打扰

    private boolean bfAddress; // 保存通讯录

    private boolean forbidJoinFriend; // 群内禁止添加好友

    private boolean forShutupGroup;//是否全群禁言

    private boolean bfUpdateData;// 修改群资料

    private boolean bfJoinCheck;// 改群是否需要审核进群

    private boolean bfPushNotice;// 发布群公告

    private boolean bfSetAdmin;// 设置其他管理员

    private int memberRole;//群成员的角色 0 群主;  1 管理员; 2 成员;

    private boolean bfMember;// 是否是群成员

    private String notice;

    private long noticeId;

    private boolean bfGroupReadCancel;//是否开启阅后既焚
    private int groupMsgCancelTime;//开启阅后即焚的时间

    private boolean bfGroupBanned;//群是否被禁（非禁言）

    public boolean getBfGroupReadCancel() {
        return bfGroupReadCancel;
    }

    public void setBfGroupReadCancel(boolean bfGroupReadCancel) {
        this.bfGroupReadCancel = bfGroupReadCancel;
    }

    public int getGroupMsgCancelTime() {
        return groupMsgCancelTime;
    }

    public void setGroupMsgCancelTime(int groupMsgCancelTime) {
        this.groupMsgCancelTime = groupMsgCancelTime;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public boolean getBfJoinCheck() {
        return bfJoinCheck;
    }

    public void setBfJoinCheck(boolean bfJoinCheck) {
        this.bfJoinCheck = bfJoinCheck;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getGroupNickName() {
        return groupNickName;
    }

    public void setGroupNickName(String groupNickName) {
        this.groupNickName = groupNickName;
    }

    public boolean getBfStar() {
        return bfStar;
    }

    public void setBfStar(boolean bfStar) {
        this.bfStar = bfStar;
    }

    public boolean getBfDisturb() {
        return bfDisturb;
    }

    public void setBfDisturb(boolean bfDisturb) {
        this.bfDisturb = bfDisturb;
    }

    public boolean getBfAddress() {
        return bfAddress;
    }

    public void setBfAddress(boolean bfAddress) {
        this.bfAddress = bfAddress;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public Boolean getForbidJoinFriend() {
        return forbidJoinFriend;
    }

    public void setForbidJoinFriend(Boolean joinFriend) {
        this.forbidJoinFriend = joinFriend;
    }

    public boolean getForShutupGroup() {
        return forShutupGroup;
    }

    public void setForShutupGroup(boolean forShutupGroup) {
        this.forShutupGroup = forShutupGroup;
    }

    public boolean getBfUpdateData() {
        return bfUpdateData;
    }

    public void setBfUpdateData(boolean bfUpdateData) {
        this.bfUpdateData = bfUpdateData;
    }

    public boolean getBfPushNotice() {
        return bfPushNotice;
    }

    public void setBfPushNotice(boolean bfPushNotice) {
        this.bfPushNotice = bfPushNotice;
    }

    public boolean getBfSetAdmin() {
        return bfSetAdmin;
    }

    public void setBfSetAdmin(boolean bfSetAdmin) {
        this.bfSetAdmin = bfSetAdmin;
    }

    public int getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(int memberRole) {
        this.memberRole = memberRole;
    }

    public long getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(long noticeId) {
        this.noticeId = noticeId;
    }

    public boolean isBfMember() {
        return bfMember;
    }

    public void setBfMember(boolean bfMember) {
        this.bfMember = bfMember;
    }


    public boolean getBfGroupBanned() {
        return bfGroupBanned;
    }

    public void setBfGroupBanned(boolean bfGroupBanned) {
        this.bfGroupBanned = bfGroupBanned;
    }

    @Override
    public int getItemType() {
        return GROUP_INFO_TYPE;
    }

    public static final int GROUP_INFO_TYPE = 311;

    public String getSearchName() {
        return searchName;
    }

    public String getShortName() {
        return shortName;
    }
}
