package framework.telegram.ui.imagepicker.engine.impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import framework.telegram.ui.imagepicker.engine.ImageEngine;
import framework.telegram.ui.utils.BitmapUtils;
import framework.telegram.ui.widget.scale.ImageSource;
import framework.telegram.ui.widget.scale.SubsamplingScaleImageView;

/**
 * {@link ImageEngine} implementation using glide.
 */

public class GlideEngine implements ImageEngine {

    public GlideEngine() {

    }

    @Override
    public void loadThumbnail(Context context, int resize, Drawable placeholder, ImageView imageView, Uri uri) {
        Glide.with(context).asBitmap().load(uri).centerCrop().override(resize, resize).placeholder(placeholder).into(imageView);
    }

    @Override
    public void loadGifThumbnail(Context context, int resize, Drawable placeholder, ImageView imageView,
                                 Uri uri) {
        loadThumbnail(context, resize, placeholder, imageView, uri);
    }

    @Override
    public void loadImage(Context context, int resizeX, int resizeY, ImageView imageView, Uri uri) {
        Glide.with(context).load(uri).centerInside().override(resizeX, resizeY).into(imageView);

    }

    @Override
    public void loadGifImage(Context context, int resizeX, int resizeY, ImageView imageView, Uri uri) {
        loadImage(context, resizeX, resizeY, imageView, uri);
    }

    @Override
    public void loadThumbnail(Context context, int resize, Drawable placeholder, final SubsamplingScaleImageView imageView, Uri uri) {
        if (resize <= 0) {
            Glide.with(context).load(uri).centerCrop().placeholder(placeholder).into(new SimpleTarget<Drawable>() {
                @Override
                public void onResourceReady(@NonNull Drawable o, @Nullable Transition transition) {
                    imageView.setImage(ImageSource.bitmap(BitmapUtils.Companion.drawable2Bitmap(o)));
                }
            });
        } else {
            Glide.with(context).load(uri).centerCrop().override(resize, resize).placeholder(placeholder).into(new SimpleTarget<Drawable>() {
                @Override
                public void onResourceReady(@NonNull Drawable o, @Nullable Transition transition) {
                    imageView.setImage(ImageSource.bitmap(BitmapUtils.Companion.drawable2Bitmap(o)));
                }
            });
        }
    }

    @Override
    public void loadGifThumbnail(Context context, int resize, Drawable placeholder, SubsamplingScaleImageView imageView,
                                 Uri uri) {
        loadThumbnail(context, resize, placeholder, imageView, uri);
    }

    @Override
    public void loadImage(Context context, int resizeX, int resizeY, SubsamplingScaleImageView imageView, Uri uri) {
        if (resizeX <= 0 || resizeY <= 0) {
            Glide.with(context).load(uri).into(new SimpleTarget<Drawable>() {
                @Override
                public void onResourceReady(@NonNull Drawable o, @Nullable Transition transition) {
                    imageView.setImage(ImageSource.bitmap(BitmapUtils.Companion.drawable2Bitmap(o)));
                }
            });
        } else {
            Glide.with(context).load(uri).override(resizeX, resizeY).into(new SimpleTarget<Drawable>() {
                @Override
                public void onResourceReady(@NonNull Drawable o, @Nullable Transition transition) {
                    imageView.setImage(ImageSource.bitmap(BitmapUtils.Companion.drawable2Bitmap(o)));
                }
            });
        }
    }

    @Override
    public void loadGifImage(Context context, int resizeX, int resizeY, SubsamplingScaleImageView imageView, Uri uri) {
        loadImage(context, resizeX, resizeY, imageView, uri);
    }

    @Override
    public boolean supportAnimatedGif() {
        return false;
    }


}
