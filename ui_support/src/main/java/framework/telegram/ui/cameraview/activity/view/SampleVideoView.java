package framework.telegram.ui.cameraview.activity.view;

import android.content.Context;
import android.util.AttributeSet;

import framework.telegram.ui.R;
import framework.telegram.ui.videoplayer.video.StandardGSYVideoPlayer;

public class SampleVideoView extends StandardGSYVideoPlayer {

    public SampleVideoView(Context context, Boolean fullFlag) {
        super(context, fullFlag);
    }

    public SampleVideoView(Context context) {
        super(context);
    }

    public SampleVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public int getLayoutId() {
        return R.layout.sample_video;
    }

    @Override
    protected void touchSurfaceMoveFullLogic(float absDeltaX, float absDeltaY) {
        super.touchSurfaceMoveFullLogic(absDeltaX, absDeltaY);
        //不给触摸快进，如果需要，屏蔽下方代码即可
        mChangePosition = false;

        //不给触摸音量，如果需要，屏蔽下方代码即可
        mChangeVolume = false;

        //不给触摸亮度，如果需要，屏蔽下方代码即可
        mBrightness = false;
    }

    @Override
    protected void touchDoubleUp() {
        //super.touchDoubleUp();
        //不需要双击暂停
    }
}
