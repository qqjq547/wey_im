package framework.telegram.ui.emoji.listeners;

import androidx.annotation.NonNull;

import framework.telegram.ui.emoji.base.Emoji;
import framework.telegram.ui.emoji.EmojiImageView;

public interface OnEmojiClickListener {
  void onEmojiClick(@NonNull EmojiImageView emoji, @NonNull Emoji imageView);
}
