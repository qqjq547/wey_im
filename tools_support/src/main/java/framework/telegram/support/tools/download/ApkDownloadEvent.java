package framework.telegram.support.tools.download;

/**
 * Created by hyf on 16/1/7.
 */
public class ApkDownloadEvent {
    public String downloadUrl;

    public ApkDownloadEvent(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
