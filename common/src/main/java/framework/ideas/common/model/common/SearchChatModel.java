package framework.ideas.common.model.common;

import com.chad.library.adapter.base.entity.MultiItemEntity;

public class SearchChatModel implements MultiItemEntity {

    public static final int SEARCH_CHAT = 1123;

    private long chatId = 0L;
    private int chatType = 0;
    private String chatContent = "";
    private long msgId = 0L;
    private long msgLocalId = 0L;
    private long msgTime = 0L;
    private int msgType = 0;//文本，文件，地图，通知
    private long indexId = 0;//indexId,群是负数，联系人是正式，用来区分 chatType 和 chatId
    private long senderId = 0;//群的话，会有记录发送人的id,用来获取头像昵称

    public SearchChatModel(long chatId, int chatType, String chatContent,long msgId,long msgLocalId,long msgTime,int type,long indexId,long senderId){
        this.chatId = chatId;
        this.chatType = chatType;
        this.chatContent = chatContent;
        this.msgId = msgId;
        this.msgLocalId = msgLocalId;
        this.msgTime = msgTime;
        this.msgType = type;
        this.indexId = indexId;
        this.senderId = senderId;
    }

    public SearchChatModel(long indexId,String chatContent){
        this.indexId = indexId;
        this.chatContent = chatContent;
    }

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

    public String getChatContent() {
        return chatContent;
    }

    public void setChatContent(String chatContent) {
        this.chatContent = chatContent;
    }

    public long getMsgId() {
        return msgId;
    }

    public void setMsgId(long msgId) {
        this.msgId = msgId;
    }

    public long getMsgLocalId() {
        return msgLocalId;
    }

    public void setMsgLocalId(long msgLocalId) {
        this.msgLocalId = msgLocalId;
    }

    public long getMsgTime() {
        return msgTime;
    }

    public void setMsgTime(long msgTime) {
        this.msgTime = msgTime;
    }

    public static int getSearchChat() {
        return SEARCH_CHAT;
    }

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public long getIndexId() {
        return indexId;
    }

    public long getSenderId() {
        return senderId;
    }

    @Override
    public int getItemType() {
        return SEARCH_CHAT;
    }
}
