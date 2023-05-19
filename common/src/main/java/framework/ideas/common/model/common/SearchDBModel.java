package framework.ideas.common.model.common;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class SearchDBModel extends RealmObject {

    public static SearchDBModel createModel(long chatId, int chatType, long msgTime) {
        SearchDBModel model = new SearchDBModel();
        model.chatId = chatId;
        model.chatType = chatType;
        model.msgTime = msgTime;
        return model;
    }

    public SearchDBModel copyModel() {
        SearchDBModel model = new SearchDBModel();
        model.chatId = chatId;
        model.chatType = chatType;
        model.msgTime = msgTime;
        return model;
    }

    @PrimaryKey
    private long chatId = 0;//用户id

    private int chatType = 0;

    private long msgTime = 0;

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public int getChatType() {
        return chatType;
    }

    public void setChatType(int chatType) {
        this.chatType = chatType;
    }

    public long getMsgTime() {
        return msgTime;
    }

    public void setMsgTime(long msgTime) {
        this.msgTime = msgTime;
    }
}