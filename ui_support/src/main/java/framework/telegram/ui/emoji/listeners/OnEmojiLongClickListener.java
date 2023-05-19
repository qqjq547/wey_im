package framework.telegram.ui.emoji.listeners;

import androidx.annotation.NonNull;

import framework.telegram.ui.emoji.base.Emoji;
import framework.telegram.ui.emoji.EmojiImageView;

public interface OnEmojiLongClickListener {
  void onEmojiLongClick(@NonNull EmojiImageView view, @NonNull Emoji emoji);
}
