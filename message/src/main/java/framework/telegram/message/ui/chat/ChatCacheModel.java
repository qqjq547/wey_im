package framework.telegram.message.ui.chat;

import com.chad.library.adapter.base.entity.MultiItemEntity;

import framework.ideas.common.model.im.ChatModel;

public class ChatCacheModel implements MultiItemEntity {

    private Boolean isOnlineStatus;
    private Boolean isShowFire;
    private int msgStatus;
    private ChatModel chatModel;

    public static ChatCacheModel createCacheChat(Boolean isOnlineStatus, int msgStatus, Boolean isShowFire,ChatModel chatModel) {
        ChatCacheModel model = new ChatCacheModel();
        model.isOnlineStatus = isOnlineStatus;
        model.msgStatus = msgStatus;
        model.isShowFire = isShowFire;
        model.chatModel = chatModel;
        return model;
    }

    public Boolean isOnlineStatus() {
        return isOnlineStatus;
    }

    public void setOnlineStatus(Boolean onlineStatus) {
        isOnlineStatus = onlineStatus;
    }

    public Boolean isFireStatus() {
        return isShowFire;
    }

    public void setShowFire(Boolean showFire){
        isShowFire = showFire;
    }

    public int getMsgStatus() {
        return msgStatus;
    }

    public void setMsgStatus(int msgStatus) {
        this.msgStatus = msgStatus;
    }

    public ChatModel getChatModel() {
        return chatModel;
    }

    public void setChatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public int getItemType() {
        return chatModel.getChaterType();
    }
}
