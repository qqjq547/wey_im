package yourpet.client.android.lame;

import java.io.File;

/**
 * 7/4/14  1:20 PM
 * Created by JustinZhang.
 */
public class LameUtil {
    public static final int NUM_CHANNELS = 1;
    public static final int SAMPLE_RATE = 16000;
    public static final int BITRATE = 8;
    public static final int MODE = 1;
    public static final int QUALITY = 5;

    public native void initEncoder(int numChannels, int sampleRate, int bitRate, int mode, int quality);

    public native void destroyEncoder();

    public native void encodeFile(String sourcePath, String targetPath);

    static {
        System.loadLibrary("mp3lame");
    }

    public LameUtil(int numChannels, int sampleRate, int bitRate) {
        initEncoder(numChannels, sampleRate, bitRate, MODE, QUALITY);
    }

    public void raw2mp3(File inFile, File outFile) {
        encodeFile(inFile.getAbsolutePath(), outFile.getAbsolutePath());
        destroyEncoder();
    }
}
