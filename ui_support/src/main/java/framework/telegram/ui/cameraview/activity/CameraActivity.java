package framework.telegram.ui.cameraview.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.qmuiteam.qmui.widget.QMUIProgressBar;

import java.io.File;
import java.util.Locale;

import framework.telegram.support.BaseActivity;
import framework.telegram.support.tools.language.LocalManageUtil;
import framework.telegram.ui.R;
import framework.telegram.ui.cameraview.CameraException;
import framework.telegram.ui.cameraview.CameraListener;
import framework.telegram.ui.cameraview.CameraOptions;
import framework.telegram.ui.cameraview.CameraView;
import framework.telegram.ui.cameraview.PictureResult;
import framework.telegram.ui.cameraview.VideoResult;
import framework.telegram.ui.cameraview.options.Facing;
import framework.telegram.ui.cameraview.options.Flash;
import framework.telegram.ui.cameraview.utils.CameraLogger;
import framework.telegram.ui.fragment.BackHandlerHelper;
import android.os.LocaleList;

public class CameraActivity extends BaseActivity implements View.OnClickListener {

    private final static int GET_PERMISSIONS_REQUEST_CODE = 100;

    private CameraView camera;

    private QMUIProgressBar progressBar;

    private TextView timeTextView;

    private TextView tipsTextView;

    private ImageButton controllerButton;

    private long startRecordTime = 0;

    public static final String RESULT_FLAG_MIME_TYPE = "mime_type";

    public static final String RESULT_FLAG_PATH = "uri";

    public static final String JPEG = "image/jpeg";

    public static final String MP4 = "video/mp4";

    private File mSaveDir;

    private long downTime;

    public static Intent getLaunchIntent(Context context, File saveDir) {
        Intent intent = new Intent(context, CameraActivity.class);
        intent.putExtra("saveDir", saveDir.getAbsolutePath());
        return intent;
    }

    void setResultAndFinish(Fragment fragment, Intent result) {
        getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String saveDir = getIntent().getStringExtra("saveDir");
        if (!TextUtils.isEmpty(saveDir)) {
            try {
                File file = new File(saveDir);
                if (!file.exists()) {
                    file.mkdirs();
                }

                mSaveDir = file;
            } catch (Exception e) {
                e.printStackTrace();
                finish();
                return;
            }
        }

        setContentView(R.layout.activity_camera);
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);

        camera = findViewById(R.id.camera_view);
        camera.setLifecycleOwner(CameraActivity.this);
        camera.addCameraListener(new Listener());
        camera.setFlash(Flash.OFF);

        timeTextView = findViewById(R.id.text_view_time);
        tipsTextView = findViewById(R.id.text_view_tips);
        progressBar = findViewById(R.id.normal_background_progress);
        controllerButton = findViewById(R.id.image_button_controller);
        controllerButton.setOnTouchListener(mOnTouchListener);
        findViewById(R.id.image_button_toggle_camera).setOnClickListener(this);
        findViewById(R.id.image_button_toggle_flash).setOnClickListener(this);
        findViewById(R.id.image_button_close).setOnClickListener(this);

        if (camera.getFlash() == Flash.OFF) {
            ((ImageView) findViewById(R.id.image_button_toggle_flash)).setImageResource(R.drawable.ic_camera_flash_close);
        } else if (camera.getFlash() == Flash.ON) {
            ((ImageView) findViewById(R.id.image_button_toggle_flash)).setImageResource(R.drawable.ic_camera_flash_open);
        } else {
            ((ImageView) findViewById(R.id.image_button_toggle_flash)).setImageResource(R.drawable.ic_camera_flash_auto);
        }
    }

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downTime = System.currentTimeMillis();
                    controllerButton.postDelayed(runnable2, 800);
                    controllerButton.setImageDrawable(getResources().getDrawable(R.drawable.layer_camera_button_pressing));
                    tipsTextView.setVisibility(View.GONE);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    controllerButton.setImageDrawable(getResources().getDrawable(R.drawable.layer_camera_button_unpress));
                    tipsTextView.setVisibility(View.VISIBLE);
                    long tmpDownTime = downTime;
                    downTime = 0;
                    controllerButton.removeCallbacks(runnable2);
                    controllerButton.setEnabled(false);
                    controllerButton.postDelayed(() -> controllerButton.setEnabled(true), 1500);

                    long upTime = System.currentTimeMillis();
                    if (upTime - tmpDownTime < 500) {
                        //短按
                        capturePicture();
                    } else {
                        stopRecordVideo();
                    }
                    break;
            }
            return true;
        }
    };

    @Override
    public void onBackPressed() {
        if (!BackHandlerHelper.handleBackPress(this)) {
            super.onBackPressed();
        }
    }

    private class Listener extends CameraListener {

        @Override
        public void onCameraOpened(@NonNull CameraOptions options) {

        }

        @Override
        public void onCameraError(@NonNull CameraException exception) {
            super.onCameraError(exception);
            // String errorMsg = exception.getReason();
            exception.printStackTrace();
        }

        @Override
        public void onPictureTaken(@NonNull final PictureResult result) {
            super.onPictureTaken(result);
            findViewById(R.id.view_white_broad).setVisibility(View.GONE);
            result.toJpgFile(new File(mSaveDir, System.currentTimeMillis() + ".jpg"), file -> getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_preview_container, PicturePreviewFragment.newInstance(result, file)).commitAllowingStateLoss());
        }

        @Override
        public void onVideoTaken(@NonNull VideoResult result) {
            super.onVideoTaken(result);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_preview_container, VideoPreviewFragment.newInstance(result)).commitAllowingStateLoss();
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.image_button_toggle_camera) {
            toggleCamera();
        } else if (view.getId() == R.id.image_button_toggle_flash) {
            if (camera.getFlash() == Flash.OFF) {
                toggleFlash(Flash.ON);
                ((ImageView) view).setImageResource(R.drawable.ic_camera_flash_open);
            } else if (camera.getFlash() == Flash.ON) {
                toggleFlash(Flash.AUTO);
                ((ImageView) view).setImageResource(R.drawable.ic_camera_flash_auto);
            } else {
                toggleFlash(Flash.OFF);
                ((ImageView) view).setImageResource(R.drawable.ic_camera_flash_close);
            }
        } else if (view.getId() == R.id.image_button_close) {
            finish();
        }
    }

    private void capturePicture() {
        if (camera.isTakingPicture())
            return;

        if (camera.getFacing() == Facing.FRONT) {
            findViewById(R.id.view_white_broad).setVisibility(View.VISIBLE);
        }

        camera.takePictureSnapshot();
    }

    private void captureVideoSnapshot() {
        if (camera.isTakingVideo())
            return;

        startRecordTime = System.currentTimeMillis();
        progressBar.setMaxValue(60 * 1000);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.postDelayed(runnable, 38);
        timeTextView.setText("0S");
        timeTextView.setVisibility(View.VISIBLE);
        findViewById(R.id.image_button_toggle_flash).setVisibility(View.GONE);
        findViewById(R.id.image_button_close).setVisibility(View.GONE);
        camera.takeVideo(new File(mSaveDir, System.currentTimeMillis() + ".mp4"), 60 * 1000);
    }

    private void stopRecordVideo() {
        startRecordTime = 0;
        progressBar.removeCallbacks(runnable);
        progressBar.setVisibility(View.GONE);
        progressBar.setProgress(0);
        timeTextView.setVisibility(View.GONE);
        findViewById(R.id.image_button_toggle_flash).setVisibility(View.VISIBLE);
        findViewById(R.id.image_button_close).setVisibility(View.VISIBLE);

        if (!camera.isTakingVideo())
            return;

        camera.stopVideo();
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            int time = (int) (System.currentTimeMillis() - startRecordTime);
            progressBar.setProgress(time);
            progressBar.postDelayed(this, 38);
            timeTextView.setText(Math.min(time / 1000, 60) + "S");
        }
    };

    private Runnable runnable2 = new Runnable() {
        @Override
        public void run() {
            if (downTime == 0) {
                return;
            }

            long touchTime = System.currentTimeMillis() - downTime;
            if (touchTime >= 800) {
                //长按
                captureVideoSnapshot();
            } else {
                controllerButton.postDelayed(this, 100);
            }
        }
    };

    private void toggleCamera() {
        if (camera.isTakingPicture() || camera.isTakingVideo())
            return;

        camera.toggleFacing();
    }

    private void toggleFlash(Flash flash) {
        if (camera.isTakingPicture() || camera.isTakingVideo())
            return;

        camera.setFlash(flash);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, GET_PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requestCode == GET_PERMISSIONS_REQUEST_CODE) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), getString(R.string.no_permissions_were_obtained), Toast.LENGTH_SHORT);
                finish();
            }
        }
    }
}
