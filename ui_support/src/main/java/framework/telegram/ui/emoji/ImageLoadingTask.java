package framework.telegram.ui.emoji;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import androidx.annotation.Nullable;

import android.widget.ImageView;

import framework.telegram.ui.emoji.base.Emoji;

import java.lang.ref.WeakReference;

final class ImageLoadingTask extends AsyncTask<Emoji, Void, Drawable> {
    private final WeakReference<ImageView> imageViewReference;
    private final WeakReference<Context> contextReference;

    ImageLoadingTask(final ImageView imageView) {
        imageViewReference = new WeakReference<>(imageView);
        contextReference = new WeakReference<>(imageView.getContext());
    }

    @Override
    protected Drawable doInBackground(final Emoji... emoji) {
        final Context context = contextReference.get();

        if (context != null && !isCancelled()) {
            return emoji[0].getDrawable(context);
        }

        return null;
    }

    @Override
    protected void onPostExecute(@Nullable final Drawable drawable) {
        if (!isCancelled() && drawable != null) {
            final ImageView imageView = imageViewReference.get();

            if (imageView != null) {
                imageView.setImageDrawable(drawable);
            }
        }
    }
}
