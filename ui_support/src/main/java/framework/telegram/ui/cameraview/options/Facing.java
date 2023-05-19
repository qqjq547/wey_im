package framework.telegram.ui.cameraview.options;


import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import framework.telegram.ui.cameraview.utils.CameraUtils;
import framework.telegram.ui.cameraview.CameraView;

/**
 * Facing value indicates which camera sensor should be used for the current session.
 *
 * @see CameraView#setFacing(Facing)
 */
public enum Facing implements Control {

    /**
     * Back-facing camera sensor.
     */
    BACK(0),

    /**
     * Front-facing camera sensor.
     */
    FRONT(1);

    @NonNull
    public static Facing DEFAULT(@Nullable Context context) {
        if (context == null) {
            return BACK;
        } else if (CameraUtils.hasCameraFacing(context, BACK)) {
            return BACK;
        } else if (CameraUtils.hasCameraFacing(context, FRONT)) {
            return FRONT;
        } else {
            // The controller will throw a CameraException.
            // This device has no cameras.
            return BACK;
        }
    }

    private int value;

    Facing(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    @Nullable
    public static Facing fromValue(int value) {
        Facing[] list = Facing.values();
        for (Facing action : list) {
            if (action.value() == value) {
                return action;
            }
        }
        return null;
    }
}
