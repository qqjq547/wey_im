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
import framework.telegram.ui.cameraview.AspectRatio;
import framework.telegram.ui.cameraview.PictureResult;
import framework.telegram.ui.fragment.BackHandlerHelper;
import framework.telegram.ui.fragment.FragmentBackHandler;
import framework.telegram.ui.image.AppImageView;

public class PicturePreviewFragment extends Fragment implements View.OnClickListener, FragmentBackHandler {

    @Override
    public boolean onBackPressed() {
        return BackHandlerHelper.handleBackPress(this);
    }

    public static PicturePreviewFragment newInstance(@Nullable PictureResult im, @Nullable File file) {
        return newInstance(im, file, 0);
    }

    public static PicturePreviewFragment newInstance(@Nullable PictureResult im, @Nullable File file, int doneButtonRes) {
        setPictureResult(im, file);

        Bundle bundle = new Bundle();
        if (doneButtonRes > 0) {
            bundle.putInt("doneButtonRes", doneButtonRes);
        }

        PicturePreviewFragment fragment = new PicturePreviewFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    private static WeakReference<PictureResult> sImage;

    private static File sImageFile;

    private static void setPictureResult(@Nullable PictureResult im, @Nullable File file) {
        sImage = im != null ? new WeakReference<>(im) : null;
        sImageFile = file;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_picture_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PictureResult result = sImage == null ? null : sImage.get();
        if (sImageFile == null || !sImageFile.exists() || result == null) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
            return;
        }

        if (getArguments() != null) {
            ((ImageButton) view.findViewById(R.id.image_button_send)).setImageResource(getArguments().getInt("doneButtonRes", R.drawable.ic_camera_send));
        }

        view.findViewById(R.id.image_button_retry).setOnClickListener(this);
        view.findViewById(R.id.image_button_send).setOnClickListener(this);

        AspectRatio ratio = AspectRatio.of(result.getSize());
        Log.d("PicturePreview", "Resolution->" + result.getSize() + " (" + ratio + ")");
        Log.d("PicturePreview", "EXIF rotation->" + result.getRotation());

        ((AppImageView) view.findViewById(R.id.image)).setImageURI(Uri.fromFile(sImageFile));
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.image_button_retry) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        } else if (view.getId() == R.id.image_button_send) {
            Intent result = new Intent();
            result.putExtra(CameraActivity.RESULT_FLAG_MIME_TYPE, CameraActivity.JPEG);
            result.putExtra(CameraActivity.RESULT_FLAG_PATH, sImageFile.getAbsolutePath());
            ((CameraActivity) getActivity()).setResultAndFinish(this, result);
        }
    }
}
