package framework.telegram.ui.emoji;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.widget.GridView;

import framework.telegram.ui.emoji.base.EmojiCategory;
import framework.telegram.ui.emoji.listeners.OnEmojiClickListener;
import framework.telegram.ui.emoji.listeners.OnEmojiLongClickListener;

import framework.telegram.ui.R;

class EmojiGridView extends GridView {
    protected EmojiArrayAdapter emojiArrayAdapter;

    EmojiGridView(final Context context) {
        super(context);

        final Resources resources = getResources();
        final int width = resources.getDimensionPixelSize(R.dimen.emoji_grid_view_column_width);
        final int spacing = resources.getDimensionPixelSize(R.dimen.emoji_grid_view_spacing);

        setColumnWidth(width);
        setHorizontalSpacing(spacing);
        setVerticalSpacing(spacing);
        setPadding(spacing, spacing, spacing, spacing);
        setNumColumns(AUTO_FIT);
        setClipToPadding(false);
        setVerticalScrollBarEnabled(false);
    }

    public EmojiGridView init(@Nullable final OnEmojiClickListener onEmojiClickListener,
                              @Nullable final OnEmojiLongClickListener onEmojiLongClickListener,
                              @NonNull final EmojiCategory category, @NonNull final VariantEmoji variantManager) {
        emojiArrayAdapter = new EmojiArrayAdapter(getContext(), category.getEmojis(), variantManager,
                onEmojiClickListener, onEmojiLongClickListener);

        setAdapter(emojiArrayAdapter);

        return this;
    }
}
