package framework.ideas.common.model.im;

import com.chad.library.adapter.base.entity.MultiItemEntity;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class StreamCallModel extends RealmObject implements MultiItemEntity {

    public static StreamCallModel createStream(String sessionId, int streamType, int isSend, long targetUid, String targetName, String targetNickName, String targetIcon, long reqTime) {
        StreamCallModel msgModel = new StreamCallModel();
        msgModel.sessionId = sessionId;
        msgModel.chaterId = targetUid;
        msgModel.chaterName = targetName;
        msgModel.chaterNickName = targetNickName;
        msgModel.chaterIcon = targetIcon;
        msgModel.status = 0;
        msgModel.streamType = streamType;
        msgModel.reqTime = reqTime;
        msgModel.startTime = 0;
        msgModel.endTime = 0;
        msgModel.isSend = isSend;

        return msgModel;
    }

    public StreamCallModel copyStream() {
        StreamCallModel msgModel = new StreamCallModel();
        msgModel.sessionId = sessionId;
        msgModel.chaterId = chaterId;
        msgModel.chaterName = chaterName;
        msgModel.chaterNickName = chaterNickName;
        msgModel.chaterIcon = chaterIcon;
        msgModel.status = status;
        msgModel.streamType = streamType;
        msgModel.reqTime = reqTime;
        msgModel.startTime = startTime;
        msgModel.endTime = endTime;
        msgModel.isSend = isSend;

        return msgModel;
    }

    @PrimaryKey
    private String sessionId;//会话id

    private long chaterId;

    private String chaterIcon;

    private String chaterName;//显示名(可能是备注，也可能是昵称)

    private String chaterNickName;//昵称

    private long reqTime;//请求时间

    private long startTime;//开始时间

    private long endTime;//结束时间

    private int streamType;//0 音频，1视频

    private int status;//0 等待对方未响应,1 已同意 2 已拒绝, 3 已取消, 4 对方正忙，5 完成

    private int isSend = 0;//是否是发起方0是，1不是

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setChaterId(long chaterId) {
        this.chaterId = chaterId;
    }

    public long getChaterId() {
        return chaterId;
    }

    public void setChaterNickName(String chaterNickName) {
        this.chaterNickName = chaterNickName;
    }

    public String getChaterNickName() {
        return chaterNickName;
    }

    public void setChaterName(String chaterName) {
        this.chaterName = chaterName;
    }

    public String getChaterName() {
        return chaterName;
    }

    public void setChaterIcon(String chaterIcon) {
        this.chaterIcon = chaterIcon;
    }

    public String getChaterIcon() {
        return chaterIcon;
    }

    public void setReqTime(long reqTime) {
        this.reqTime = reqTime;
    }

    public long getReqTime() {
        return reqTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setStreamType(int streamType) {
        this.streamType = streamType;
    }

    public int getStreamType() {
        return streamType;
    }

    public void setIsSend(int isSend) {
        this.isSend = isSend;
    }

    public int getIsSend() {
        return isSend;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public int getItemType() {
        return ITEM_STREAM_CALL;
    }

    public static final int ITEM_STREAM_CALL = 201;

}
