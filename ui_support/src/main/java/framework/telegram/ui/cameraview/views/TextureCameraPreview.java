package framework.telegram.ui.cameraview.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import framework.telegram.ui.cameraview.AspectRatio;
import framework.telegram.ui.R;

public class TextureCameraPreview extends CameraPreview<TextureView, SurfaceTexture> {

    private View mRootView;

    public TextureCameraPreview(@NonNull Context context, @NonNull ViewGroup parent, @Nullable SurfaceCallback callback) {
        super(context, parent, callback);
    }

    @NonNull
    @Override
    protected TextureView onCreateView(@NonNull Context context, @NonNull ViewGroup parent) {
        View root = LayoutInflater.from(context).inflate(R.layout.cameraview_texture_view, parent, false);
        parent.addView(root, 0);
        TextureView texture = root.findViewById(R.id.texture_view);
        texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                dispatchOnSurfaceAvailable(width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                dispatchOnSurfaceSizeChanged(width, height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                dispatchOnSurfaceDestroyed();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
        mRootView = root;
        return texture;
    }

    @NonNull
    @Override
    public View getRootView() {
        return mRootView;
    }

    @NonNull
    @Override
    public Class<SurfaceTexture> getOutputClass() {
        return SurfaceTexture.class;
    }

    @NonNull
    @Override
    public SurfaceTexture getOutput() {
        return getView().getSurfaceTexture();
    }

    @TargetApi(15)
    @Override
    public void setStreamSize(int width, int height, boolean wasFlipped) {
        super.setStreamSize(width, height, wasFlipped);
        if (getView().getSurfaceTexture() != null) {
            getView().getSurfaceTexture().setDefaultBufferSize(width, height);
        }
    }

    @Override
    public boolean supportsCropping() {
        return true;
    }

    @Override
    protected void crop() {
        mCropTask.start();
        getView().post(() -> {
            if (mInputStreamHeight == 0 || mInputStreamWidth == 0 ||
                    mOutputSurfaceHeight == 0 || mOutputSurfaceWidth == 0) {
                mCropTask.end(null);
                return;
            }
            float scaleX = 1f, scaleY = 1f;
            AspectRatio current = AspectRatio.of(mOutputSurfaceWidth, mOutputSurfaceHeight);
            AspectRatio target = AspectRatio.of(mInputStreamWidth, mInputStreamHeight);
            if (current.toFloat() >= target.toFloat()) {
                // We are too short. Must increase height.
                scaleY = current.toFloat() / target.toFloat();
            } else {
                // We must increase width.
                scaleX = target.toFloat() / current.toFloat();
            }

            getView().setScaleX(scaleX);
            getView().setScaleY(scaleY);

            mCropping = scaleX > 1.02f || scaleY > 1.02f;
            LOG.i("crop:", "applied scaleX=", scaleX);
            LOG.i("crop:", "applied scaleY=", scaleY);
            mCropTask.end(null);
        });
    }
}
