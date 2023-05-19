package framework.telegram.message.sp.bean;

import java.io.Serializable;

public class AudioDraft implements Serializable {

    private static final long serialVersionUID = 401231;

    private String audioPath;

    private long audioTime;

    private int[] highDArr;

    public AudioDraft(String audioPath, long audioTime, int[] highDArr) {
        this.audioPath = audioPath;
        this.audioTime = audioTime;
        this.highDArr = highDArr;
    }

    public long getAudioTime() {
        return audioTime;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public int[] getHighDArr() {
        return highDArr;
    }
}
