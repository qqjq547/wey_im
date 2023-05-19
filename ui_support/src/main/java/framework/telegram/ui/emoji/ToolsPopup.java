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
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;

import framework.telegram.ui.R;
import framework.telegram.ui.emoji.listeners.OnPopupDismissListener;
import framework.telegram.ui.emoji.listeners.OnPopupShownListener;
import framework.telegram.ui.emoji.listeners.OnSoftKeyboardCloseListener;
import framework.telegram.ui.emoji.listeners.OnSoftKeyboardOpenListener;
import framework.telegram.ui.emoji.listeners.OnToolClickListener;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;

public final class ToolsPopup implements EmojiResultReceiver.Receiver {
    static final String TAG = "ToolsPopup";

    static final int MIN_KEYBOARD_HEIGHT = 50;

    final View rootView;
    final AppCompatActivity context;

    final PopupWindow popupWindow;
    final EditText editText;

    boolean isPendingOpen;
    boolean isKeyboardOpen;

    @Nullable
    OnPopupShownListener onToolsPopupShownListener;
    @Nullable
    OnSoftKeyboardCloseListener onSoftKeyboardCloseListener;
    @Nullable
    OnSoftKeyboardOpenListener onSoftKeyboardOpenListener;
    @Nullable
    OnToolClickListener onToolClickListener;
    @Nullable
    OnPopupDismissListener onPopupDismissListener;
    int originalImeOptions = -1;

    private LinearLayout mNameCard = null;
    private LinearLayout mLocation = null;

    private final EmojiResultReceiver emojiResultReceiver = new EmojiResultReceiver(new Handler(Looper.getMainLooper()));
    private final ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = this::updateKeyboardState;

    ToolsPopup(@NonNull final View rootView, @NonNull final EditText editText, @StyleRes final int animationStyle) {
        this.context = Utils.asActivity(rootView.getContext());
        this.rootView = rootView.getRootView();
        this.rootView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {

            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                if (isShowing())
                    dismiss();
            }
        });

        this.popupWindow = new PopupWindow(context);

        this.editText = editText;
        this.editText.setFocusableInTouchMode(true);

        final OnToolClickListener clickListener = index -> {
            if (onToolClickListener != null) {
                onToolClickListener.onToolClick(index);
            }
        };

        final View toolsView = LayoutInflater.from(context).inflate(R.layout.msg_chat_tools_view, null);
        toolsView.findViewById(R.id.linear_layout_picture).setOnClickListener(v -> clickListener.onToolClick(0));

        toolsView.findViewById(R.id.linear_layout_camera).setOnClickListener(v -> clickListener.onToolClick(1));

        mNameCard = toolsView.findViewById(R.id.linear_layout_namecard);
        mNameCard.setOnClickListener(v -> clickListener.onToolClick(2));

//        toolsView.findViewById(R.id.linear_layout_file).setOnClickListener(v -> clickListener.onToolClick(3));
        mLocation =toolsView.findViewById(R.id.linear_layout_location);
        mLocation.setOnClickListener(v -> clickListener.onToolClick(4));

        popupWindow.setContentView(toolsView);

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

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
    }


    public void toggle() {
        if (!popupWindow.isShowing()) {
            if (Utils.shouldOverrideRegularCondition(context, editText) && originalImeOptions == -1) {
                originalImeOptions = editText.getImeOptions();
            }
            editText.requestFocus();
            showAtBottomPending();
        } else {
            dismiss();
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

    @Override
    public void onReceiveResult(final int resultCode, final Bundle data) {
        if (resultCode == 0 || resultCode == 1) {
            showAtBottom();
        }
    }

    private void updateKeyboardState() {
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
        if (isKeyboardOpen) {

            if (onSoftKeyboardCloseListener != null) {
                onSoftKeyboardCloseListener.onKeyboardClose();
            }

            if (isShowing()) {
                dismiss();
            }

            isKeyboardOpen = false;
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


    private void showAtBottom() {
        isPendingOpen = false;
        popupWindow.showAtLocation(rootView, Gravity.BOTTOM, 0, 0);

        if (onToolsPopupShownListener != null) {
            onToolsPopupShownListener.onPopupShown();
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
        private OnToolClickListener onToolClickListener;
        @Nullable
        private OnPopupDismissListener onPopupDismissListener;

        private Builder(final View rootView) {
            this.rootView = Utils.checkNotNull(rootView, "The root View can't be null");
        }

        /**
         * @param rootView The root View of your layout.xml which will be used for calculating the height
         *                 of the keyboard.
         * @return builder For building the {@link ToolsPopup}.
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
        public Builder setOnToolClickListener(@Nullable final OnToolClickListener listener) {
            onToolClickListener = listener;
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

        @CheckResult
        public ToolsPopup build(@NonNull final EditText editText) {
            EmojiManager.getInstance().verifyInstalled();
            Utils.checkNotNull(editText, "EditText can't be null");

            final ToolsPopup toolsPopup = new ToolsPopup(rootView, editText, keyboardAnimationStyle);
            toolsPopup.onSoftKeyboardCloseListener = onSoftKeyboardCloseListener;
            toolsPopup.onToolClickListener = onToolClickListener;
            toolsPopup.onSoftKeyboardOpenListener = onSoftKeyboardOpenListener;
            toolsPopup.onToolsPopupShownListener = onPopupShownListener;
            toolsPopup.onPopupDismissListener = onPopupDismissListener;
            return toolsPopup;
        }
    }
}
