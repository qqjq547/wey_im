package framework.telegram.ui.cameraview.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.Locale;

import framework.telegram.support.BaseActivity;
import framework.telegram.support.tools.language.LocalManageUtil;
import framework.telegram.ui.R;
import framework.telegram.ui.image.AppImageView;
import framework.telegram.ui.videoplayer.GSYVideoManager;
import framework.telegram.ui.videoplayer.video.StandardGSYVideoPlayer;

public class VideoPreviewActivity extends BaseActivity {

    public static Intent getLaunchIntentWithUrl(Context context, String videoUrl, String videoThumbUrl) {
        Intent intent = new Intent(context, VideoPreviewActivity.class);
        intent.putExtra("videoUrl", videoUrl);
        intent.putExtra("videoThumbUrl", videoThumbUrl);
        return intent;
    }

    public static Intent getLaunchIntentWithPath(Context context, String videoPath, String videoThumbPath) {
        Intent intent = new Intent(context, VideoPreviewActivity.class);
        intent.putExtra("videoPath", videoPath);
        intent.putExtra("videoThumbPath", videoThumbPath);
        return intent;
    }

    private StandardGSYVideoPlayer videoPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean fromNetwork = false;
        String source = getIntent().getStringExtra("videoPath");
        if (TextUtils.isEmpty(source)) {
            source = getIntent().getStringExtra("videoUrl");
            fromNetwork = true;
        }
        if (TextUtils.isEmpty(source)) {
            finish();
            return;
        }

        setContentView(R.layout.activity_video_preview);
        init(source, fromNetwork);
    }

    private void init(String source, boolean fromNetwork) {
        videoPlayer = findViewById(R.id.video_player);
        if (fromNetwork) {
            videoPlayer.setUp(source, false, getString(R.string.test_video));
        } else {
            File file = new File(source);
            videoPlayer.setUp(Uri.fromFile(file).toString(), false, file, getString(R.string.test_video));
        }

        //增加封面
        AppImageView imageView = new AppImageView(this);
        String thumb = getIntent().getStringExtra("videoThumbPath");
        if (!TextUtils.isEmpty(source)) {
            imageView.setImageURI(Uri.fromFile(new File(thumb)).toString());
            videoPlayer.setThumbImageView(imageView);
        } else {
            thumb = getIntent().getStringExtra("videoThumbUrl");
            if (!TextUtils.isEmpty(source)) {
                imageView.setImageURI(thumb);
                videoPlayer.setThumbImageView(imageView);
            }
        }

        //增加title
        videoPlayer.getTitleTextView().setVisibility(View.VISIBLE);
        //设置返回键
        videoPlayer.getBackButton().setVisibility(View.GONE);
        //设置全屏按键功能,这是使用的是选择屏幕，而不是全屏
        videoPlayer.getFullscreenButton().setVisibility(View.GONE);
        //是否可以滑动调整
        videoPlayer.setIsTouchWiget(true);
        //设置返回按键功能
        videoPlayer.getBackButton().setOnClickListener(v -> onBackPressed());
        videoPlayer.startPlayLogic();

        findViewById(R.id.image_button_close).setOnClickListener(v -> finish());
    }


    @Override
    protected void onPause() {
        super.onPause();
        videoPlayer.onVideoPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoPlayer.onVideoResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GSYVideoManager.releaseAllVideos();
    }

    @Override
    public void onBackPressed() {
        //释放所有
        videoPlayer.setVideoAllCallBack(null);
        super.onBackPressed();
    }
}
