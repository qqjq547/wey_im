package framework.ideas.common.model.im;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class MessageReceiptModel extends RealmObject {

    @PrimaryKey
    private long id = 0;

    @Index
    private long msgId = 0;//服务器消息id

    private long senderUid = 0;//发送者uid

    private long deliverTime = 0L;//送达时间

    private long readTime = 0L;//已读时间

    private long readedAttachmentTime = 0;

    private int messageType = 0;//消息类型

    public MessageReceiptModel copyMessage() {
        MessageReceiptModel msgModel = new MessageReceiptModel();
        msgModel.id = id;
        msgModel.msgId = msgId;
        msgModel.senderUid = senderUid;
        msgModel.deliverTime = deliverTime;
        msgModel.readTime = readTime;
        msgModel.readedAttachmentTime = readedAttachmentTime;
        msgModel.messageType = messageType;
        return msgModel;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(long senderUid) {
        this.senderUid = senderUid;
    }

    public long getDeliverTime() {
        return deliverTime;
    }

    public void setDeliverTime(long deliverTime) {
        this.deliverTime = deliverTime;
    }

    public long getReadedAttachmentTime() {
        return readedAttachmentTime;
    }

    public void setReadedAttachmentTime(long readedAttachmentTime) {
        this.readedAttachmentTime = readedAttachmentTime;
    }

    public long getReadTime() {
        return readTime;
    }

    public void setReadTime(long readTime) {
        this.readTime = readTime;
    }

    public long getMsgId() {
        return msgId;
    }

    public void setMsgId(long msgId) {
        this.msgId = msgId;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }
}