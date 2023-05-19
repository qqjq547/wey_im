package framework.telegram.ui.cameraview.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.lang.ref.WeakReference;

import framework.telegram.ui.R;
import framework.telegram.ui.cameraview.VideoResult;
import framework.telegram.ui.cameraview.activity.view.SampleVideoView;
import framework.telegram.ui.fragment.FragmentBackHandler;
import framework.telegram.ui.videoplayer.GSYVideoManager;

/**
 * 单独的视频播放页面
 */
public class VideoPreviewFragment extends Fragment implements View.OnClickListener, FragmentBackHandler {

    public static VideoPreviewFragment newInstance(@Nullable VideoResult result) {
        return newInstance(result, 0);
    }

    public static VideoPreviewFragment newInstance(@Nullable VideoResult result, int doneButtonRes) {
        setVideoResult(result);

        Bundle bundle = new Bundle();
        if (doneButtonRes > 0) {
            bundle.putInt("doneButtonRes", doneButtonRes);
        }

        VideoPreviewFragment fragment = new VideoPreviewFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    private static WeakReference<VideoResult> sVideoResult;

    private static void setVideoResult(@Nullable VideoResult result) {
        sVideoResult = result != null ? new WeakReference<>(result) : null;
    }

    private SampleVideoView mVideoPlayer;

    private File mVideoFile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final VideoResult result = sVideoResult == null ? null : sVideoResult.get();
        if (result == null) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
            return;
        }

        if (getArguments() != null) {
            ((ImageButton) view.findViewById(R.id.image_button_send)).setImageResource(getArguments().getInt("doneButtonRes", R.drawable.ic_camera_send));
        }

        mVideoPlayer = view.findViewById(R.id.video_player);

        view.findViewById(R.id.image_button_retry).setOnClickListener(this);
        view.findViewById(R.id.image_button_send).setOnClickListener(this);

        mVideoFile = result.getFile();
        Log.d("VideoPreview", "Size->" + result.getSize());
        Log.d("VideoPreview", "Snapshot->" + result.isSnapshot());
        Log.d("VideoPreview", "Rotation->" + result.getRotation());
        Log.d("VideoPreview", "Audio->" + result.getAudio().name());
        Log.d("VideoPreview", "Audio bit rate->" + result.getAudioBitRate() + " bits per sec.");
        Log.d("VideoPreview", "VideoCodec->" + result.getVideoCodec().name());
        Log.d("VideoPreview", "Video bit rate->" + result.getVideoBitRate() + " bits per sec.");
        Log.d("VideoPreview", "Video frame rate->" + result.getVideoFrameRate() + " fps");

        init();
    }

    private void init() {
        mVideoPlayer.setLooping(true);
        mVideoPlayer.setUp(Uri.fromFile(mVideoFile).toString(), false, mVideoFile, getString(R.string.test_video));
        mVideoPlayer.startPlayLogic();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mVideoPlayer != null) {
            mVideoPlayer.onVideoPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mVideoPlayer != null) {
            mVideoPlayer.onVideoResume();
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.image_button_retry) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        } else if (view.getId() == R.id.image_button_send) {
            Intent result = new Intent();
            result.putExtra(CameraActivity.RESULT_FLAG_MIME_TYPE, CameraActivity.MP4);
            result.putExtra(CameraActivity.RESULT_FLAG_PATH, mVideoFile.getAbsolutePath());
            ((CameraActivity) getActivity()).setResultAndFinish(this, result);
        }
    }

    @Override
    public boolean onBackPressed() {
        //释放所有
        if (mVideoPlayer != null) {
            mVideoPlayer.setVideoAllCallBack(null);
        }
        
        GSYVideoManager.releaseAllVideos();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            return true;
//        } else {
//            new Handler().postDelayed(() -> {
//                getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
////                overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
//            }, 500);
//        }
        return false;
    }
}
