package framework.telegram.business.bridge.event;

public class QuitGroupEvent {
    private long groupId;

    public QuitGroupEvent(long groupId) {
        this.groupId = groupId;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }
}
