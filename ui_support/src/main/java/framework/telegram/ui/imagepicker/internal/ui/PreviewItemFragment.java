package framework.telegram.ui.imagepicker.internal.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import framework.telegram.ui.R;
import framework.telegram.ui.imagepicker.internal.entity.Item;
import framework.telegram.ui.imagepicker.internal.entity.SelectionSpec;
import framework.telegram.ui.widget.scale.SubsamplingScaleImageView;

public class PreviewItemFragment extends Fragment {

    private static final String ARGS_ITEM = "args_item";

    private Drawable mPlaceholder = null;

    public static PreviewItemFragment newInstance(Item item) {
        PreviewItemFragment fragment = new PreviewItemFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARGS_ITEM, item);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_preview_item, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Item item = getArguments().getParcelable(ARGS_ITEM);
        if (item == null) {
            return;
        }

        View videoPlayButton = view.findViewById(R.id.video_play_button);
        if (item.isVideo()) {
            videoPlayButton.setVisibility(View.VISIBLE);
            videoPlayButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(item.uri, "video/*");
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getContext(), R.string.error_no_video_activity, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            videoPlayButton.setVisibility(View.GONE);
        }

        if (mPlaceholder == null){
            TypedArray ta = getContext().getTheme().obtainStyledAttributes(
                    new int[]{R.attr.album_thumbnail_placeholder});
            mPlaceholder = ta.getDrawable(0);
            ta.recycle();
        }

        SubsamplingScaleImageView image = view.findViewById(R.id.scale_image_view);
        ImageView imageView = view.findViewById(R.id.scale_image_view_2);
        if (item.isGif()) {
            image.setVisibility(View.GONE);
            SelectionSpec.getInstance().imageEngine.loadGifImage(getContext(), 0, 0, imageView,
                    item.getContentUri());
        } else if (item.isVideo()) {
            imageView.setVisibility(View.GONE);
            SelectionSpec.getInstance().imageEngine.loadThumbnail(getContext(),
                    image.getSWidth(),
                    mPlaceholder,
                    image, item.getContentUri());
        } else {
            imageView.setVisibility(View.GONE);
            SelectionSpec.getInstance().imageEngine.loadImage(getContext(), 0, 0, image,
                    item.getContentUri());
        }
    }

    public void resetView() {
        if (getView() != null) {
            ((SubsamplingScaleImageView) getView().findViewById(R.id.scale_image_view)).resetScaleAndCenter();
        }
    }
}
