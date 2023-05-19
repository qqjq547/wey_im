package framework.ideas.common.bean;

public class StreamMessageContentBean {

    public String sessionId;//会话id

    public int status;//0 等待对方未响应,1 已同意 2 已拒绝, 3 已取消, 4 对方正忙，5 完成

    public int streamType;//0 音频，1视频

    public long startTime;//开始时间

    public long endTime;//结束时间

    public int isSend;//是否是发起方0是，1不是

}
