package framework.telegram.message.bridge.event;

import java.util.List;

public class FireStatusChangeEvent {

    private List<FireStatus> fireList;

    public FireStatusChangeEvent(List<FireStatus> fireList) {
        this.fireList = fireList;
    }

    public List<FireStatus> getFireList(){
        return fireList;
    }

}
