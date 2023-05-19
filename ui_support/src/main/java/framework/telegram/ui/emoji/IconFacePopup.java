package framework.telegram.ui.emoji;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.autofill.AutofillManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupWindow;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import framework.telegram.ui.R;
import framework.telegram.ui.emoji.listeners.OnPopupDismissListener;
import framework.telegram.ui.emoji.listeners.OnPopupShownListener;
import framework.telegram.ui.emoji.listeners.OnSoftKeyboardCloseListener;
import framework.telegram.ui.emoji.listeners.OnSoftKeyboardOpenListener;
import framework.telegram.ui.emoji.listeners.OnToolClickListener;
import framework.telegram.ui.face.IconFaceItem;
import framework.telegram.ui.face.IconFacePageView;
import framework.telegram.ui.face.dynamic.DynamicFaceBean;
import framework.telegram.ui.utils.ScreenUtils;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;

public final class IconFacePopup implements EmojiResultReceiver.Receiver {
    private static final String TAG = "IconFacePopup";

    private static final int MIN_KEYBOARD_HEIGHT = 50;

    private final View rootView;
    final AppCompatActivity context;

    private final PopupWindow popupWindow;
    private final EditText editText;

    private boolean isPendingOpen;
    private boolean isKeyboardOpen;

    @Nullable
    private
    OnPopupShownListener onToolsPopupShownListener;
    @Nullable
    private
    OnSoftKeyboardCloseListener onSoftKeyboardCloseListener;
    private
    @Nullable
    OnSoftKeyboardOpenListener onSoftKeyboardOpenListener;
    private
    @Nullable
    IconFacePageView.OnIconFaceListener onIconFaceListener;
    private
    @Nullable
    OnPopupDismissListener onPopupDismissListener;

    private int originalImeOptions = -1;

    private final EmojiResultReceiver emojiResultReceiver = new EmojiResultReceiver(new Handler(Looper.getMainLooper()));

    IconFacePopup(@NonNull final View rootView, @NonNull final EditText editText, @StyleRes final int animationStyle) {
        this.context = Utils.asActivity(rootView.getContext());
        this.rootView = rootView.getRootView();
        this.editText = editText;

        this.popupWindow = new PopupWindow(context);

        IconFacePageView iconFacePageView = (IconFacePageView) LayoutInflater.from(context).inflate(R.layout.petface_page_view, null);
        iconFacePageView.setOnIconFaceListener(new IconFacePageView.OnIconFaceListener() {
            @Override
            public void onIconFaceBackspaceClicked() {
                Utils.backspace(editText);

                if (onIconFaceListener != null) {
                    onIconFaceListener.onIconFaceBackspaceClicked();
                }
            }

            @Override
            public void onIconFaceClicked(IconFaceItem iconFaceItem, int type) {
                if (type != 3) {
                    final int start = editText.getSelectionStart();
                    final int end = editText.getSelectionEnd();

                    if (start < 0) {
                        editText.append(iconFaceItem.name);
                    } else {
                        editText.getText().replace(Math.min(start, end), Math.max(start, end), iconFaceItem.name, 0, iconFaceItem.name.length());
                    }
                }

                if (onIconFaceListener != null) {
                    onIconFaceListener.onIconFaceClicked(iconFaceItem, type);
                }
            }
        });

        popupWindow.setContentView(iconFacePageView);

        popupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        popupWindow.setBackgroundDrawable(new BitmapDrawable(context.getResources(), (Bitmap) null)); // To avoid borders and overdraw.
        popupWindow.setOnDismissListener(() -> {
            if (onPopupDismissListener != null) {
                onPopupDismissListener.onPopupDismiss();
            }
        });

        if (animationStyle != 0) {
            popupWindow.setAnimationStyle(animationStyle);
        }

        ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = () -> updateKeyboardState();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
    }

    public void addDynamicFaceIcons(List<DynamicFaceBean> faces) {
        ((IconFacePageView) popupWindow.getContentView()).addDynamicFaceIcons(faces);
    }

    void updateKeyboardState() {
        final int keyboardHeight = Utils.getInputMethodHeight(context, rootView);

        if (keyboardHeight > Utils.dpToPx(context, MIN_KEYBOARD_HEIGHT)) {
            updateKeyboardStateOpened(keyboardHeight);
        } else {
            updateKeyboardStateClosed();
        }
    }

    private void updateKeyboardStateOpened(final int keyboardHeight) {
        if (popupWindow.getHeight() != keyboardHeight) {
            popupWindow.setHeight(keyboardHeight);
        }

        final Rect rect = Utils.windowVisibleDisplayFrame(context);

        final int properWidth = Utils.getOrientation(context) == Configuration.ORIENTATION_PORTRAIT ? rect.right : Utils.getScreenWidth(context);
        if (popupWindow.getWidth() != properWidth) {
            popupWindow.setWidth(properWidth);
        }

        if (!isKeyboardOpen) {
            isKeyboardOpen = true;
            if (onSoftKeyboardOpenListener != null) {
                onSoftKeyboardOpenListener.onKeyboardOpen(keyboardHeight);
            }
        }

        if (isPendingOpen) {
            showAtBottom();
        }
    }

    private void updateKeyboardStateClosed() {
        isKeyboardOpen = false;

        if (onSoftKeyboardCloseListener != null) {
            onSoftKeyboardCloseListener.onKeyboardClose();
        }

        if (isShowing()) {
            dismiss();
        }
    }

    public void toggle() {
        if (!popupWindow.isShowing()) {
            if (Utils.shouldOverrideRegularCondition(context, editText) && originalImeOptions == -1) {
                originalImeOptions = editText.getImeOptions();
            }
            editText.setFocusableInTouchMode(true);
            editText.requestFocus();
            showAtBottomPending();
        } else {
            dismiss();
        }
    }

    private void showAtBottomPending() {
        isPendingOpen = true;
        final InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (Utils.shouldOverrideRegularCondition(context, editText)) {
            editText.setImeOptions(editText.getImeOptions() | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            if (inputMethodManager != null) {
                inputMethodManager.restartInput(editText);
            }
        }

        if (inputMethodManager != null) {
            emojiResultReceiver.setReceiver(this);
            inputMethodManager.showSoftInput(editText, InputMethodManager.RESULT_UNCHANGED_SHOWN, emojiResultReceiver);
        }
    }

    public boolean isShowing() {
        return popupWindow.isShowing();
    }

    public void dismiss() {
        popupWindow.dismiss();

        emojiResultReceiver.setReceiver(null);

        if (originalImeOptions != -1) {
            editText.setImeOptions(originalImeOptions);
            final InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

            if (inputMethodManager != null) {
                inputMethodManager.restartInput(editText);
            }

            if (SDK_INT >= O) {
                final AutofillManager autofillManager = context.getSystemService(AutofillManager.class);
                if (autofillManager != null) {
                    autofillManager.cancel();
                }
            }
        }
    }

    void showAtBottom() {
        isPendingOpen = false;
        popupWindow.showAtLocation(rootView, Gravity.BOTTOM, 0, 0);

        if (onToolsPopupShownListener != null) {
            onToolsPopupShownListener.onPopupShown();
        }
    }

    @Override
    public void onReceiveResult(final int resultCode, final Bundle data) {
        if (resultCode == 0 || resultCode == 1) {
            showAtBottom();
        }
    }

    public static final class Builder {
        @NonNull
        private final View rootView;
        @StyleRes
        private int keyboardAnimationStyle;
        @Nullable
        private OnPopupShownListener onPopupShownListener;
        @Nullable
        private OnSoftKeyboardCloseListener onSoftKeyboardCloseListener;
        @Nullable
        private OnSoftKeyboardOpenListener onSoftKeyboardOpenListener;
        @Nullable
        private OnPopupDismissListener onPopupDismissListener;
        @Nullable
        IconFacePageView.OnIconFaceListener onIconFaceListener;

        private Builder(final View rootView) {
            this.rootView = Utils.checkNotNull(rootView, "The root View can't be null");
        }

        /**
         * @param rootView The root View of your layout.xml which will be used for calculating the height
         *                 of the keyboard.
         * @return builder For building the {@link IconFacePopup}.
         */
        @CheckResult
        public static Builder fromRootView(final View rootView) {
            return new Builder(rootView);
        }

        @CheckResult
        public Builder setOnSoftKeyboardCloseListener(@Nullable final OnSoftKeyboardCloseListener listener) {
            onSoftKeyboardCloseListener = listener;
            return this;
        }

        @CheckResult
        public Builder setOnSoftKeyboardOpenListener(@Nullable final OnSoftKeyboardOpenListener listener) {
            onSoftKeyboardOpenListener = listener;
            return this;
        }

        @CheckResult
        public Builder setOnPopupShownListener(@Nullable final OnPopupShownListener listener) {
            onPopupShownListener = listener;
            return this;
        }

        @CheckResult
        public Builder setOnPopupDismissListener(@Nullable final OnPopupDismissListener listener) {
            onPopupDismissListener = listener;
            return this;
        }

        @CheckResult
        public Builder setKeyboardAnimationStyle(@StyleRes final int animation) {
            keyboardAnimationStyle = animation;
            return this;
        }

        public Builder setOnIconFaceListener(@Nullable IconFacePageView.OnIconFaceListener onIconFaceListener) {
            this.onIconFaceListener = onIconFaceListener;
            return this;
        }

        @CheckResult
        public IconFacePopup build(@NonNull final EditText editText) {
            EmojiManager.getInstance().verifyInstalled();
            Utils.checkNotNull(editText, "EditText can't be null");

            final IconFacePopup toolsPopup = new IconFacePopup(rootView, editText, keyboardAnimationStyle);
            toolsPopup.onSoftKeyboardCloseListener = onSoftKeyboardCloseListener;
            toolsPopup.onIconFaceListener = onIconFaceListener;
            toolsPopup.onSoftKeyboardOpenListener = onSoftKeyboardOpenListener;
            toolsPopup.onToolsPopupShownListener = onPopupShownListener;
            toolsPopup.onPopupDismissListener = onPopupDismissListener;
            return toolsPopup;
        }
    }
}
