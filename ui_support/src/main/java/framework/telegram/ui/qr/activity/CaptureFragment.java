package framework.telegram.ui.qr.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;

import framework.telegram.ui.R;
import framework.telegram.ui.imagepicker.ImagePicker;
import framework.telegram.ui.imagepicker.MimeType;
import framework.telegram.ui.imagepicker.engine.impl.GlideEngine;
import framework.telegram.ui.imagepicker.internal.entity.MediaInfo;
import framework.telegram.ui.qr.camera.CameraManager;
import framework.telegram.ui.qr.decoding.CaptureActivityHandler;
import framework.telegram.ui.qr.decoding.InactivityTimer;
import framework.telegram.ui.qr.view.ViewfinderView;

import static android.app.Activity.RESULT_OK;

/**
 * 自定义实现的扫描Fragment
 */
public class CaptureFragment extends Fragment implements SurfaceHolder.Callback {

    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    private Vector<BarcodeFormat> decodeFormats;
    private String characterSet;
    private InactivityTimer inactivityTimer;
    private MediaPlayer mediaPlayer;
    private boolean playBeep;
    private static final float BEEP_VOLUME = 0.10f;
    private boolean vibrate;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private CodeUtils.AnalyzeCallback analyzeCallback;
    private Camera camera;
    private ImageView mClose;
    private TextView mPhoto , mQrCode,mFlash;
    private FrameLayout mFrameLayout;
    private int TOOL_IMAGEPICKER_REQUESTCODE = 300;
    private int GET_PERMISSIONS_REQUEST_CODE = 124;
    private boolean mIsFlash = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        CameraManager.init(getActivity().getApplication());

        hasSurface = false;
        inactivityTimer = new InactivityTimer(this.getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Bundle bundle = getArguments();
        View view = null;
        if (bundle != null) {
            int layoutId = bundle.getInt(CodeUtils.LAYOUT_ID);
            if (layoutId != -1) {
                view = inflater.inflate(layoutId, null);
            }
        }

        if (view == null) {
            view = inflater.inflate(R.layout.fragment_capture, null);
        }

        viewfinderView = (ViewfinderView) view.findViewById(R.id.viewfinder_view);
        surfaceView = (SurfaceView) view.findViewById(R.id.preview_view);
        surfaceHolder = surfaceView.getHolder();
        mPhoto = view.findViewById(R.id.image_view_photo);
        mClose = view.findViewById(R.id.image_view_close);
        mQrCode = view.findViewById(R.id.text_view_qr);
        mFlash= view.findViewById(R.id.text_view_flash);
        mFrameLayout= view.findViewById(R.id.frame_layout);

        mClose.setOnClickListener(v -> CaptureFragment.this.getActivity().finish());

        mQrCode.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction("framework.telegram.business.ui.qr");
            intent.addCategory("android.intent.category.DEFAULT");
                startActivity(intent);
        });

        mPhoto.setOnClickListener(v ->{
            if (checkPermission(true)){
                openImagePicker();
                }
            });

        setFlash();
        mFlash.setOnClickListener(v->{
            mIsFlash = !mIsFlash;
            setFlash();
            CodeUtils.isLightEnable(mIsFlash);
        });

        return view;
    }

    private void setFlash(){
        mFlash.setCompoundDrawablesRelativeWithIntrinsicBounds(0,mIsFlash?R.drawable.ic_camera_flash_close:R.drawable.ic_camera_flash_open,0,0);
        mFlash.setText(mIsFlash? getString(R.string.flash_off):getString(R.string.flash_open));
    }


    @Override
    public void onResume() {
        super.onResume();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        decodeFormats = null;
        characterSet = null;

        playBeep = true;
        AudioManager audioService = (AudioManager) getActivity().getSystemService(getActivity().AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false;
        }
        initBeepSound();
        vibrate = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.get().closeDriver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        inactivityTimer.shutdown();
    }


    /**
     * Handler scan result
     *
     * @param result
     * @param barcode
     */
    public void handleDecode(Result result, Bitmap barcode) {
        inactivityTimer.onActivity();
        playBeepSoundAndVibrate();

        if (result == null || TextUtils.isEmpty(result.getText())) {
            if (analyzeCallback != null) {
                analyzeCallback.onAnalyzeFailed();
            }
        } else {
            if (analyzeCallback != null) {
                analyzeCallback.onAnalyzeSuccess( result.getText());
            }
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
            camera = CameraManager.get().getCamera();
        } catch (Exception e) {
            if (callBack != null) {
                callBack.callBack(e);
            }
            return;
        }
        if (callBack != null) {
            callBack.callBack(null);
        }
        if (handler == null) {
            handler = new CaptureActivityHandler(this, decodeFormats, characterSet, viewfinderView);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
        if (camera != null) {
            if (camera != null && CameraManager.get().isPreviewing()) {
                if (!CameraManager.get().isUseOneShotPreviewCallback()) {
                    camera.setPreviewCallback(null);
                }
                camera.stopPreview();
                CameraManager.get().getPreviewCallback().setHandler(null, 0);
                CameraManager.get().getAutoFocusCallback().setHandler(null, 0);
                CameraManager.get().setPreviewing(false);
            }
        }
    }

    public Handler getHandler() {
        return handler;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();

    }

    private void initBeepSound() {
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(beepListener);

            AssetFileDescriptor file = getResources().openRawResourceFd(
                    R.raw.common_qrcode);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
                file.close();
                mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mediaPlayer.prepare();
            } catch (IOException e) {
                mediaPlayer = null;
            }
        }
    }


    private static final long VIBRATE_DURATION = 200L;

    private void playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) getActivity().getSystemService(getActivity().VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final MediaPlayer.OnCompletionListener beepListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };

    public CodeUtils.AnalyzeCallback getAnalyzeCallback() {
        return analyzeCallback;
    }

    public void setAnalyzeCallback(CodeUtils.AnalyzeCallback analyzeCallback) {
        this.analyzeCallback = analyzeCallback;
    }

    @Nullable
    CameraInitCallBack callBack;

    /**
     * Set callback for Camera check whether Camera init success or not.
     */
    public void setCameraInitCallBack(CameraInitCallBack callBack) {
        this.callBack = callBack;
    }

    interface CameraInitCallBack {
        /**
         * Callback for Camera init result.
         *
         * @param e If is's null,means success.otherwise Camera init failed with the Exception.
         */
        void callBack(Exception e);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == TOOL_IMAGEPICKER_REQUESTCODE) {
                List<MediaInfo> paths = ImagePicker.from(CaptureFragment.this).obtainInfoResult(data);
                if (paths != null && paths.size() >= 1) {
//                    CodeUtils.analyzeBitmap(paths.get(0).getPath(), analyzeCallback);
                } else {
                    Toast.makeText(CaptureFragment.this.getActivity(), getString(R.string.image_retrieval_failed), Toast.LENGTH_SHORT).show();
                }
            }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requestCode == GET_PERMISSIONS_REQUEST_CODE){
                if (checkPermission(false)) {
                    openImagePicker();
                }else {
                    Toast.makeText(CaptureFragment.this.getContext(), getString(R.string.no_permissions_were_obtained), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void openImagePicker(){
        ImagePicker.from(CaptureFragment.this).choose(EnumSet.of(MimeType.JPEG, MimeType.PNG))
                .countable(false)
                .maxSelectable(1)
                .thumbnailScale(0.85f)
                .originalEnable(false)
                .showSingleMediaType(false)
                .imageEngine(new GlideEngine())
                .forResult(TOOL_IMAGEPICKER_REQUESTCODE);
    }

    private boolean checkPermission(Boolean ask) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (CaptureFragment.this.getContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || CaptureFragment.this.getContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ask){
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, GET_PERMISSIONS_REQUEST_CODE);
                }
                return false;
            }else {
                return true;
            }
        }
        return true;
    }


}
