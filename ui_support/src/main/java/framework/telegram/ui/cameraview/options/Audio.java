package framework.telegram.ui.cameraview.options;


import androidx.annotation.Nullable;

import framework.telegram.ui.cameraview.CameraView;

/**
 * Audio values indicate whether to record audio stream when record video.
 *
 * @see CameraView#setAudio(Audio)
 */
public enum Audio implements Control {

    /**
     * No Audio.
     */
    OFF(0),

    /**
     * With Audio.
     */
    ON(1);

    public final static Audio DEFAULT = ON;

    private int value;

    Audio(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    @Nullable
    public static Audio fromValue(int value) {
        Audio[] list = Audio.values();
        for (Audio action : list) {
            if (action.value() == value) {
                return action;
            }
        }
        return null;
    }
}
