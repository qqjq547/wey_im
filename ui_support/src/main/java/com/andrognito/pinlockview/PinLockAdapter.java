package com.andrognito.pinlockview;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import framework.telegram.ui.R;

/**
 * Created by aritraroy on 31/05/16.
 */
public class PinLockAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_NUMBER = 0;
    private static final int VIEW_TYPE_DELETE = 1;

    private Context mContext;
    private CustomizationOptionsBundle mCustomizationOptionsBundle;
    private OnNumberClickListener mOnNumberClickListener;
    private OnDeleteClickListener mOnDeleteClickListener;
    private int mPinLength;

    private int[] mKeyValues;

    public PinLockAdapter(Context context) {
        this.mContext = context;
        this.mKeyValues = getAdjustKeyValues(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0});
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_NUMBER) {
            View view = inflater.inflate(R.layout.layout_number_item, parent, false);
            viewHolder = new NumberViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.layout_delete_item, parent, false);
            viewHolder = new DeleteViewHolder(view);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_NUMBER) {
            NumberViewHolder vh1 = (NumberViewHolder) holder;
            configureNumberButtonHolder(vh1, position);
        } else if (holder.getItemViewType() == VIEW_TYPE_DELETE) {
            DeleteViewHolder vh2 = (DeleteViewHolder) holder;
            configureDeleteButtonHolder(vh2);
        }
    }

    private void configureNumberButtonHolder(NumberViewHolder holder, int position) {
        if (holder != null) {
            if (position == 9) {
                holder.mNumberButton.setVisibility(View.GONE);
            } else {
                holder.mNumberButton.setText(String.valueOf(mKeyValues[position]));
                holder.mNumberButton.setVisibility(View.VISIBLE);
                holder.mNumberButton.setTag(mKeyValues[position]);
            }

            if (mCustomizationOptionsBundle != null) {
                holder.mNumberButton.setTextColor(mCustomizationOptionsBundle.getTextColor());
                if (mCustomizationOptionsBundle.getButtonBackgroundDrawable() != null) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        holder.mNumberButton.setBackgroundDrawable(
                                mCustomizationOptionsBundle.getButtonBackgroundDrawable());
                    } else {
                        holder.mNumberButton.setBackground(
                                mCustomizationOptionsBundle.getButtonBackgroundDrawable());
                    }
                }
                holder.mNumberButton.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        mCustomizationOptionsBundle.getTextSize());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        mCustomizationOptionsBundle.getButtonSize(),
                        mCustomizationOptionsBundle.getButtonSize());
                holder.mNumberButton.setLayoutParams(params);
            }
        }
    }

    private void configureDeleteButtonHolder(DeleteViewHolder holder) {
        if (holder != null) {
            if (mCustomizationOptionsBundle.isShowDeleteButton() && mPinLength > 0) {
                holder.mDeleteButton.setVisibility(View.VISIBLE);
                if (mCustomizationOptionsBundle.getDeleteButtonDrawable() != null) {
                    holder.mDeleteButton.setImageDrawable(mCustomizationOptionsBundle.getDeleteButtonDrawable());
                }
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        mCustomizationOptionsBundle.getDeleteButtonSize(),
                        mCustomizationOptionsBundle.getDeleteButtonSize());
                holder.mDeleteButton.setLayoutParams(params);
            } else {
                holder.mDeleteButton.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return 12;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1) {
            return VIEW_TYPE_DELETE;
        }
        return VIEW_TYPE_NUMBER;
    }

    public int getPinLength() {
        return mPinLength;
    }

    public void setPinLength(int pinLength) {
        this.mPinLength = pinLength;
    }

    public int[] getKeyValues() {
        return mKeyValues;
    }

    public void setKeyValues(int[] keyValues) {
        this.mKeyValues = getAdjustKeyValues(keyValues);
        notifyDataSetChanged();
    }

    private int[] getAdjustKeyValues(int[] keyValues) {
        int[] adjustedKeyValues = new int[keyValues.length + 1];
        for (int i = 0; i < keyValues.length; i++) {
            if (i < 9) {
                adjustedKeyValues[i] = keyValues[i];
            } else {
                adjustedKeyValues[i] = -1;
                adjustedKeyValues[i + 1] = keyValues[i];
            }
        }
        return adjustedKeyValues;
    }

    public OnNumberClickListener getOnItemClickListener() {
        return mOnNumberClickListener;
    }

    public void setOnItemClickListener(OnNumberClickListener onNumberClickListener) {
        this.mOnNumberClickListener = onNumberClickListener;
    }

    public OnDeleteClickListener getOnDeleteClickListener() {
        return mOnDeleteClickListener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener onDeleteClickListener) {
        this.mOnDeleteClickListener = onDeleteClickListener;
    }

    public CustomizationOptionsBundle getCustomizationOptions() {
        return mCustomizationOptionsBundle;
    }

    public void setCustomizationOptions(CustomizationOptionsBundle customizationOptionsBundle) {
        this.mCustomizationOptionsBundle = customizationOptionsBundle;
    }

    public class NumberViewHolder extends RecyclerView.ViewHolder {
        Button mNumberButton;

        public NumberViewHolder(final View itemView) {
            super(itemView);
            mNumberButton = itemView.findViewById(R.id.button);
            mNumberButton.setOnClickListener(v -> {
                if (mOnNumberClickListener != null) {
                    mOnNumberClickListener.onNumberClicked((Integer) v.getTag());
                }
            });

            mNumberButton.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mNumberButton.setTextColor(Color.parseColor("#007AFF"));
                    mNumberButton.setBackgroundResource(R.drawable.bus_oval_007aff_trans_1);
                }
                if (event.getAction() == MotionEvent.ACTION_UP
                        || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    mNumberButton.setTextColor(Color.BLACK);
                    mNumberButton.setBackgroundResource(R.drawable.bus_oval_f8fafd);
                }
                return false;
            });
        }
    }

    public class DeleteViewHolder extends RecyclerView.ViewHolder {
        ImageView mDeleteButton;

        public DeleteViewHolder(final View itemView) {
            super(itemView);
            mDeleteButton = itemView.findViewById(R.id.buttonImage);

            if (mCustomizationOptionsBundle.isShowDeleteButton()) {
                mDeleteButton.setOnClickListener(v -> {
                    if (mOnDeleteClickListener != null) {
                        mOnDeleteClickListener.onDeleteClicked();
                    }
                });

                mDeleteButton.setOnLongClickListener(v -> {
                    if (mOnDeleteClickListener != null) {
                        mOnDeleteClickListener.onDeleteLongClicked();
                    }
                    return true;
                });

            }
        }
    }

    public interface OnNumberClickListener {
        void onNumberClicked(int keyValue);
    }

    public interface OnDeleteClickListener {
        void onDeleteClicked();

        void onDeleteLongClicked();
    }
}
