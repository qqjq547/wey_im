package framework.ideas.common.model.group;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.chad.library.adapter.base.entity.MultiItemEntity;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class GroupMemberModel extends RealmObject implements MultiItemEntity {

    public static GroupMemberModel createGroupMember(long uid, String nickName, int gender, String icon, boolean bfFriend, String remarkName,
                                                     long groupId, int type, String groupNickName, long sortScore,
                                                     boolean onlineStatus, long lastOnlineTime, boolean isShowLastOnlineTime) {
        GroupMemberModel model = new GroupMemberModel();
        model.uid = uid;
        model.nickName = nickName;
        model.gender = gender;
        model.icon = icon;
        model.bfFriend = bfFriend;
        model.remarkName = remarkName;
        model.groupId = groupId;
        model.type = type;
        model.groupNickName = groupNickName;
        model.sortScore = sortScore;
        model.onlineStatus = onlineStatus;
        model.lastOnlineTime = lastOnlineTime;
        model.isShowLastOnlineTime = isShowLastOnlineTime;
        return model;
    }

    public static GroupMemberModel createGroupMember(long uid, String nickName, String icon) {
        GroupMemberModel model = new GroupMemberModel();
        model.uid = uid;
        model.nickName = nickName;
        model.icon = icon;
        return model;
    }

    public GroupMemberModel copyGroupMemberModel() {
        GroupMemberModel model = new GroupMemberModel();
        model.uid = uid;
        model.nickName = nickName;
        model.gender = gender;
        model.icon = icon;
        model.bfFriend = bfFriend;
        model.remarkName = remarkName;
        model.groupId = groupId;
        model.type = type;
        model.groupNickName = groupNickName;
        model.sortScore = sortScore;
        model.onlineStatus = onlineStatus;
        model.lastOnlineTime = lastOnlineTime;
        model.isShowLastOnlineTime = isShowLastOnlineTime;
        return model;
    }

    /**
     * 资料相关
     */
    @PrimaryKey
    private long uid; // 用户ID

    private String nickName; // 昵称

    private String icon; // 头像

    private int gender; // 性别 0保密  1男  2女

    /**
     * 关系相关
     */
    private boolean bfFriend; // 是否好友

    private String remarkName; // 备注名

    /**
     * 群相关
     */
    @Index
    private long groupId; // 群ID

    private int type; // 成员类型 0群主 1管理员 2成员

    private String groupNickName; // 用户群昵称

    private long sortScore; // 排序权重值

    private boolean onlineStatus;//最后上下线状态

    private long lastOnlineTime;//最后上下线时间

    private boolean isShowLastOnlineTime;//是否显示最后在线时间


    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public boolean isBfFriend() {
        return bfFriend;
    }

    public void setBfFriend(boolean bfFriend) {
        this.bfFriend = bfFriend;
    }

    public String getRemarkName() {
        return remarkName;
    }

    public void setRemarkName(String remarkName) {
        this.remarkName = remarkName;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getGroupNickName() {
        return groupNickName;
    }

    public void setGroupNickName(String groupNickName) {
        this.groupNickName = groupNickName;
    }

    public long getSortScore() {
        return sortScore;
    }

    public void setSortScore(long sortScore) {
        this.sortScore = sortScore;
    }

    public long getLastOnlineTime() {
        return lastOnlineTime;
    }

    public void setLastOnlineTime(long lastOnlineTime) {
        this.lastOnlineTime = lastOnlineTime;
    }

    public void setOnlineStatus(boolean onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public boolean isOnlineStatus() {
        return onlineStatus;
    }

    public boolean isShowLastOnlineTime() {
        return isShowLastOnlineTime;
    }

    public void setShowLastOnlineTime(boolean showLastOnlineTime) {
        isShowLastOnlineTime = showLastOnlineTime;
    }

    @Override
    public int getItemType() {
        return GROUP_MEMBER_TYPE;
    }

    public static final int GROUP_MEMBER_TYPE = 313;

    public String getDisplayName() {
        String displayName = TextUtils.isEmpty(remarkName) ? groupNickName : remarkName;
        return TextUtils.isEmpty(displayName) ? nickName : displayName;
    }

    public String getDisplayAtMeName() {
        return TextUtils.isEmpty(groupNickName) ? nickName : groupNickName;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof GroupMemberModel
                && uid == ((GroupMemberModel) obj).uid
                && groupId == ((GroupMemberModel) obj).groupId;
    }
}
