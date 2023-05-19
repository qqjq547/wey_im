package framework.telegram.message.bridge.event;

public class JoinContactReqEvent {

    public static int contactReqCount;

    public JoinContactReqEvent(int contactReqCount) {
        JoinContactReqEvent.contactReqCount = contactReqCount;
    }
}
