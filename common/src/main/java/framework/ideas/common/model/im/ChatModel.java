package framework.ideas.common.model.im;

import com.chad.library.adapter.base.entity.MultiItemEntity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ChatModel extends RealmObject implements MultiItemEntity {

    public static final int CHAT_TYPE_PVT = 0;
    public static final int CHAT_TYPE_GROUP = 1;

    public static final int CHAT_TYPE_GROUP_NOTIFY = 100;

    public static ChatModel createChat(int chaterType, long targetUid, String targetName, String targetNickName, String targetIcon, long lastMsgLocalId, String lastMsg, long lastMsgTime) {
        ChatModel model = new ChatModel();
        model.id = System.nanoTime();
        model.chaterType = chaterType;
        model.chaterId = targetUid;
        model.chaterName = targetName;
        model.chaterNickName = targetNickName;
        model.chaterIcon = targetIcon;
        model.lastMsgLocalId = lastMsgLocalId;
        model.lastMsg = lastMsg;
        model.lastMsgTime = lastMsgTime;
        model.isTop = 0;
        model.unReadCount = 0;
        model.atMeCount = 0;
        model.bfDisturb = 0;

        return model;
    }

    public static ChatModel createChat(int chaterType, long targetUid, String targetName, String targetNickName, String targetIcon, long lastMsgLocalId, String lastMsg, long lastMsgTime, int unReadCount) {
        ChatModel model = new ChatModel();
        model.id = System.nanoTime();
        model.chaterType = chaterType;
        model.chaterId = targetUid;
        model.chaterName = targetName;
        model.chaterNickName = targetNickName;
        model.chaterIcon = targetIcon;
        model.lastMsgLocalId = lastMsgLocalId;
        model.lastMsg = lastMsg;
        model.lastMsgTime = lastMsgTime;
        model.isTop = 0;
        model.unReadCount = unReadCount;
        model.atMeCount = 0;
        model.bfDisturb = 0;

        return model;
    }

    public static ChatModel createChat(int chaterType, long targetUid, String targetName, String targetNickName, String targetIcon, long lastMsgLocalId, String lastMsg, long lastMsgTime, int unReadCount, int atMeCount) {
        ChatModel model = new ChatModel();
        model.id = System.nanoTime();
        model.chaterType = chaterType;
        model.chaterId = targetUid;
        model.chaterName = targetName;
        model.chaterNickName = targetNickName;
        model.chaterIcon = targetIcon;
        model.lastMsgLocalId = lastMsgLocalId;
        model.lastMsg = lastMsg;
        model.lastMsgTime = lastMsgTime;
        model.isTop = 0;
        model.unReadCount = unReadCount;
        model.atMeCount = atMeCount;
        model.bfDisturb = 0;

        return model;
    }

    public ChatModel copyChat() {
        ChatModel model = new ChatModel();
        model.id = id;
        model.chaterType = chaterType;
        model.chaterId = chaterId;
        model.chaterName = chaterName;
        model.chaterNickName = chaterNickName;
        model.chaterIcon = chaterIcon;
        model.lastMsgTime = lastMsgTime;
        model.isTop = isTop;
        model.unReadCount = unReadCount;
        model.atMeCount = atMeCount;
        model.lastMsgLocalId = lastMsgLocalId;
        model.lastMsg = lastMsg;
        model.bfDisturb = bfDisturb;

        return model;
    }

    @PrimaryKey
    private long id = 0;

    private int chaterType;

    private long chaterId;

    private String chaterIcon;

    private String chaterName;//显示名(可能是备注，也可能是昵称)

    private String chaterNickName;//昵称

    private long lastMsgLocalId;

    private String lastMsg;

    private long lastMsgTime;

    private int isTop;

    private int unReadCount;

    private int atMeCount;//@我的次数

    private int bfDisturb;//是否免打扰

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getChaterType() {
        return chaterType;
    }

    public void setChaterType(int chaterType) {
        this.chaterType = chaterType;
    }

    public long getChaterId() {
        return chaterId;
    }

    public void setChaterId(long chaterId) {
        this.chaterId = chaterId;
    }

    public String getChaterIcon() {
        return chaterIcon;
    }

    public void setChaterIcon(String chaterIcon) {
        this.chaterIcon = chaterIcon;
    }

    public String getChaterNickName() {
        return chaterNickName;
    }

    public void setChaterNickName(String chaterNickName) {
        this.chaterNickName = chaterNickName;
    }

    public String getChaterName() {
        return chaterName;
    }

    public void setChaterName(String chaterName) {
        this.chaterName = chaterName;
    }

    public void setLastMsgTime(long lastMsgTime) {
        this.lastMsgTime = lastMsgTime;
    }

    public long getLastMsgTime() {
        return lastMsgTime;
    }

    public int getIsTop() {
        return isTop;
    }

    public void setIsTop(int isTop) {
        this.isTop = isTop;
    }

    public long getLastMsgLocalId() {
        return lastMsgLocalId;
    }

    public void setLastMsgLocalId(long lastMsgLocalId) {
        this.lastMsgLocalId = lastMsgLocalId;
    }

    public String getLastMsg() {
        return lastMsg;
    }

    public void setLastMsg(String lastMsg) {
        this.lastMsg = lastMsg;
    }

    public int getUnReadCount() {
        return unReadCount;
    }

    public void setUnReadCount(int unReadCount) {
        this.unReadCount = unReadCount;
    }

    public void setAtMeCount(int atMeCount) {
        this.atMeCount = atMeCount;
    }

    public int getAtMeCount() {
        return atMeCount;
    }

    public void setBfDisturb(int bfDisturb) {
        this.bfDisturb = bfDisturb;
    }

    public int getBfDisturb() {
        return bfDisturb;
    }

    @Override
    public int getItemType() {
        return chaterType;
    }
}
