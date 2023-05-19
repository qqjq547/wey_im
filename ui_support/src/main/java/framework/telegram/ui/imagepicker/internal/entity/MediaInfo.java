package framework.telegram.ui.imagepicker.internal.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

public class MediaInfo implements Parcelable {
    private int type;//0 未知 1 静态图，2动态图，3视频
    private String path;

    public MediaInfo(int type, String path) {
        this.type = type;
        this.path = path;
    }

    private MediaInfo(Parcel source) {
        type = source.readInt();
        path = source.readString();
    }

    public int getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeString(path);
    }

    public static final Creator<MediaInfo> CREATOR = new Creator<MediaInfo>() {
        @Override
        @Nullable
        public MediaInfo createFromParcel(Parcel source) {
            return new MediaInfo(source);
        }

        @Override
        public MediaInfo[] newArray(int size) {
            return new MediaInfo[size];
        }
    };

}
