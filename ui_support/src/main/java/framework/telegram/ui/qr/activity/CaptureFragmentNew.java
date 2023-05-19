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
import android.util.Log;
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

import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zbar.ZBarView;
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
public class CaptureFragmentNew extends Fragment implements QRCodeView.Delegate{

    private ImageView mClose;
    private TextView mPhoto , mQrCode,mFlash;
    private int TOOL_IMAGEPICKER_REQUESTCODE = 300;
    private int GET_PERMISSIONS_REQUEST_CODE = 124;

    private CodeUtils.AnalyzeCallback analyzeCallback;

    private boolean mIsFlash = false;

    private ZBarView mZBarView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            view = inflater.inflate(R.layout.fragment_capture_new, null);
        }
        mZBarView= view.findViewById(R.id.zbarview);
        mZBarView.setDelegate(this);
        mPhoto = view.findViewById(R.id.image_view_photo);
        mClose = view.findViewById(R.id.image_view_close);
        mQrCode = view.findViewById(R.id.text_view_qr);
        mFlash= view.findViewById(R.id.text_view_flash);

        mClose.setOnClickListener(v -> CaptureFragmentNew.this.getActivity().finish());

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
            if (mIsFlash){
                mZBarView.openFlashlight();
            }else {
                mZBarView.closeFlashlight();
            }
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
        mZBarView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
//        mZBarView.startCamera(Camera.CameraInfo.CAMERA_FACING_FRONT); // 打开前置摄像头开始预览，但是并未开始识别
        mZBarView.startSpotAndShowRect(); // 显示扫描框，并开始识别
    }

    @Override
    public void onPause() {
        super.onPause();
        mZBarView.stopCamera(); // 关闭摄像头预览，并且隐藏扫描框
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mZBarView.onDestroy(); // 销毁二维码扫描控件
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == TOOL_IMAGEPICKER_REQUESTCODE) {
                List<MediaInfo> paths = ImagePicker.from(CaptureFragmentNew.this).obtainInfoResult(data);
                if (paths != null && paths.size() >= 1) {
                    CodeUtils.analyzeBitmap(paths.get(0).getPath(), analyzeCallback);
//                    mZBarView.decodeQRCode(paths.get(0).getPath());
                } else {
                    Toast.makeText(CaptureFragmentNew.this.getActivity(), getString(R.string.image_retrieval_failed), Toast.LENGTH_SHORT).show();
                }
            }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requestCode == GET_PERMISSIONS_REQUEST_CODE){
                if (checkPermission(false)) {
                    openImagePicker();
                }else {
                    Toast.makeText(CaptureFragmentNew.this.getContext(), getString(R.string.no_permissions_were_obtained), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void openImagePicker(){
        ImagePicker.from(CaptureFragmentNew.this).choose(EnumSet.of(MimeType.JPEG, MimeType.PNG))
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
            if (CaptureFragmentNew.this.getContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || CaptureFragmentNew.this.getContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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

    public CodeUtils.AnalyzeCallback getAnalyzeCallback() {
        return analyzeCallback;
    }

    public void setAnalyzeCallback(CodeUtils.AnalyzeCallback analyzeCallback) {
        this.analyzeCallback = analyzeCallback;
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        vibrate();
        if (result == null || TextUtils.isEmpty(result)) {
            if (analyzeCallback != null) {
                analyzeCallback.onAnalyzeFailed();
            }
        } else {
            if (analyzeCallback != null) {
                analyzeCallback.onAnalyzeSuccess( result);
            }
        }
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getActivity().getSystemService(getActivity().VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }

    @Override
    public void onCameraAmbientBrightnessChanged(boolean isDark) {

    }

    @Override
    public void onScanQRCodeOpenCameraError() {
    }


}
