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

public class PicturePreviewActivity extends BaseActivity {

    public static Intent getLaunchIntentWithUrl(Context context, String imagePath, String imageThumbPath) {
        Intent intent = new Intent(context, PicturePreviewActivity.class);
        intent.putExtra("imageUri", Uri.fromFile(new File(imagePath)).toString());
        intent.putExtra("imageThumbUri", Uri.fromFile(new File(imageThumbPath)).toString());
        return intent;
    }

    private AppImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = null;
        String source = getIntent().getStringExtra("imageUri");
        if (!TextUtils.isEmpty(source)) {
            uri = Uri.parse(source);
        }

        if (uri == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_picture_preview);
        init(uri);
    }

    private void init(Uri source) {
        mImageView = findViewById(R.id.app_image_view);
        mImageView.setImageURI(source);

        findViewById(R.id.image_button_close).setOnClickListener(v -> finish());
    }
}
