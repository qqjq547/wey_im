package framework.telegram.message.bridge.event;


import framework.ideas.common.model.im.MessageModel;

public class MessageSendStatusEvent {

    private MessageModel msgModel;

    public MessageSendStatusEvent(MessageModel msgModel) {
        this.msgModel = msgModel;
    }

    public MessageModel getMsgModel() {
        return msgModel;
    }
}
