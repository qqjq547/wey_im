package framework.telegram.message.bridge.event;

public class WebOnlineStatusChangeEvent {

    private long uid;

    private boolean isOnline;

    public WebOnlineStatusChangeEvent(long uid, boolean isOnline) {
        this.uid = uid;
        this.isOnline = isOnline;
    }

    public long getUid() {
        return uid;
    }

    public boolean isOnline() {
        return isOnline;
    }
}
