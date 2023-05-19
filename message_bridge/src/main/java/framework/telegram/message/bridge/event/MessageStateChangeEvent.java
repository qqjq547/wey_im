package framework.telegram.message.bridge.event;

import java.util.List;

import framework.ideas.common.model.im.MessageModel;

public class MessageStateChangeEvent {

    private List<MessageModel> msgModels;

    public MessageStateChangeEvent(List<MessageModel> msgModels) {
        this.msgModels = msgModels;
    }

    public List<MessageModel> getMsgModels() {
        return msgModels;
    }
}
