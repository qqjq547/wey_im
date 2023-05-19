package framework.telegram.message.bridge.event;

public class OnlineStatusChangeEvent {

    private long uid;
    private boolean statu;

    public OnlineStatusChangeEvent(long uid,boolean state) {
        this.uid = uid;
        this.statu = state;
    }

    public long getUid() {
        return uid;
    }

    public boolean isStatu() {
        return statu;
    }
}
