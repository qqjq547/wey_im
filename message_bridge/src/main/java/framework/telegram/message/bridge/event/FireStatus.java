package framework.telegram.message.bridge.event;

/**
 * Created by lzh on 20-4-10.
 * INFO:
 */
public class FireStatus{
    private long uid;
    private boolean statu;

    public FireStatus(long uid,boolean statu){
        this.uid = uid;
        this.statu = statu;
    }

    public long getUid() {
        return uid;
    }

    public boolean isStatu() {
        return statu;
    }
}