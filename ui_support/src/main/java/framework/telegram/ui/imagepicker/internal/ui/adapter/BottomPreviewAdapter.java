package framework.telegram.ui.imagepicker.internal.ui.adapter;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import framework.telegram.ui.R;
import framework.telegram.ui.imagepicker.MimeType;
import framework.telegram.ui.imagepicker.internal.entity.Item;
import framework.telegram.ui.imagepicker.internal.entity.SelectionSpec;

public class BottomPreviewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Item> mItemList = new ArrayList<>();
    private onBottomItemClickListener<Item> mListener = null;

    private final int TYPE_IMAGE = 1;
    private final int TYPE_VIDEO = 2;

    private Drawable mPlaceholder = null;

    private int mSelectPos=-1;//选择的项

    public BottomPreviewAdapter(List<Item> itemList, onBottomItemClickListener<Item> itemClickListener){
        this.mItemList = itemList;
        this.mListener = itemClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        if (MimeType.MP4.toString().equals(mItemList.get(position).mimeType))
            return TYPE_VIDEO;
        else
            return TYPE_IMAGE;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mPlaceholder == null){
            TypedArray ta = parent.getContext().getTheme().obtainStyledAttributes(
                    new int[]{R.attr.album_thumbnail_placeholder});
            mPlaceholder = ta.getDrawable(0);
            ta.recycle();
        }
        View view = null;
        if (viewType == TYPE_IMAGE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bottom_pic, parent, false);
            return new ImageHolder(view);
        }
        else if (viewType == TYPE_VIDEO) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bottom_video, parent, false);
            return new VideoHolder(view);
        }
        return null;
    }


    @Override
    public int getItemCount() {
        return mItemList.size() > 0 ? mItemList.size() : 0;
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (mItemList.size() > position) {
            Item target = mItemList.get(position);
            ImageView imageView = null;
            if (holder instanceof ImageHolder){
                imageView = ((ImageHolder) holder).imageView;
                SelectionSpec.getInstance().imageEngine
                        .loadImage(holder.itemView.getContext(),
                                holder.itemView.getContext().getResources().getDimensionPixelSize(R.dimen.media_grid_size),
                                holder.itemView.getContext().getResources().getDimensionPixelSize(R.dimen.media_grid_size),
                                imageView, target.getContentUri());
                if (mSelectPos == position){
                    ((ImageHolder) holder).mFlFrame.setVisibility(View.VISIBLE);
                }else {
                    ((ImageHolder) holder).mFlFrame.setVisibility(View.GONE);
                }
            }else if (holder instanceof VideoHolder) {
                ((VideoHolder) holder).textView.setText(DateUtils.formatElapsedTime(target.duration / 1000));
                imageView = ((VideoHolder) holder).imageView;
                SelectionSpec.getInstance().imageEngine.loadThumbnail(holder.itemView.getContext(),
                        holder.itemView.getContext().getResources().getDimensionPixelSize(R.dimen.media_grid_size),
                        mPlaceholder,
                        imageView, target.getContentUri());
                if (mSelectPos == position){
                    ((VideoHolder) holder).mFlFrame.setVisibility(View.VISIBLE);
                }else {
                    ((VideoHolder) holder).mFlFrame.setVisibility(View.GONE);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                mSelectPos=position;
                notifyDataSetChanged();
                if (mListener != null)
                    mListener.onBottomItemClick(position,target);
            });
        }
    }

    public void setSelectPos(int mSelectPos) {
        this.mSelectPos = mSelectPos;
        notifyDataSetChanged();
    }

    public void addAll(List<Item> itemList){
        this.mItemList = itemList;
    }

    public void add(Item newItem){
        for (Item item: mItemList){
            if (item.equals(newItem))
                return;
        }
        mItemList.add(newItem);
    }


    public void remove(Item target){
        mItemList.remove(target);
    }

    private class ImageHolder extends RecyclerView.ViewHolder{
        private ImageView imageView;
        private FrameLayout mFlFrame;
        ImageHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.bottom_pic);
            mFlFrame = itemView.findViewById(R.id.fl_frame);
        }
    }

    private class VideoHolder extends RecyclerView.ViewHolder{
        private ImageView imageView;
        private TextView textView;
        private FrameLayout mFlFrame;
        VideoHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.bottom_video);
            textView = itemView.findViewById(R.id.text_view_video_length);
            mFlFrame = itemView.findViewById(R.id.fl_frame);
        }
    }

    public interface onBottomItemClickListener<T>{
        void onBottomItemClick(int position,T t);
    }
}
