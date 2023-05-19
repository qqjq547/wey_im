package framework.telegram.message.ui.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieTask;
import com.caesar.musicspectrumbarlibrary.MusicSpectrumBar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import framework.ideas.common.audio.AudioSampleUtils;
import framework.telegram.support.tools.CastArrayUtil;
import framework.telegram.support.tools.file.DirManager;
import framework.telegram.message.R;
import framework.telegram.message.audio.AudioPlayer;
import framework.telegram.message.audio.AudioRecorder;
import framework.telegram.message.bridge.event.ChatHistoryChangeEvent;
import framework.telegram.message.manager.SendMessageManager;
import framework.telegram.message.sp.CommonPref;
import framework.telegram.message.sp.bean.AudioDraft;
import framework.telegram.support.BaseApp;
import framework.telegram.support.account.AccountManager;
import framework.telegram.support.system.event.EventBus;
import framework.telegram.support.system.gson.GsonInstanceCreater;
import framework.telegram.support.system.storage.sp.SharePreferencesStorage;
import framework.telegram.support.tools.TimeUtils;
import framework.telegram.ui.shimmerlayout.ShimmerLayout;
import framework.telegram.ui.utils.ScreenUtils;
import framework.telegram.ui.widget.MentionEditText;

import static android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_SEND;
import static android.view.inputmethod.EditorInfo.IME_NULL;

public class MessageInputView extends FrameLayout {

    private final static int RECORD_MAX_TIME = 120000;
    private final static int RECORD_MAX_COUNT_DOWN_TIME = 10000;
    private final static int RECORD_MIN_TIME = 1000;

    public enum UserBehaviour {
        CANCELING,
        LOCKING,
        NONE
    }

    public enum RecordingBehaviour {
        CANCELED,
        LOCKED,
        LOCK_DONE,
        RELEASED
    }

    public interface RecordingListener {

        boolean checkRecordable();

        void onRecordingStarted();

        void onRecordingLocked();

        void onRecordingCompleted(long recordTime, String recordFilePath, @NotNull Integer[] highDArr);

        void onRecordingCanceled();

        void onRecordSoShort();

        void onPlayAudioDraft();

        String getDraftStorageName();

        void onInputHeightChange(int height);

        void onInputing();
    }

    public interface ButtonsListener {

        void onClickSend(List<Long> atUids, String msg);

        void onClickFace();

        void onClickTools();

        void onClickFire();
    }

    private View imageViewAudio, imageViewLockArrow, imageViewLock, imageViewLockBlue, imageViewStop, imageViewSend,
            imageViewAttachment, imageViewFace, imageViewDeleteDraft, textViewTip;
    private View layoutMessageMask, layoutLock, layoutDraft, layoutInput;
    private MentionEditText editTextMessage;
    private TextView timeText, stopButton, fireText;
    private ProgressBar progressBarVolumn;
    private LottieAnimationView imageViewMic;
    private ShimmerLayout layoutSlideCancel;

    private ImageView audioButton, sendButton;

    private Animation animBlink, animJumpFast;

    private boolean isBlink;
    private boolean isDeleting;
    private boolean stopTrackingAction;

    private float lastX, lastY;
    private float firstX, firstY;

    private float directionOffset = 0, cancelOffset = 0, lockOffset = 0;
    private boolean isLocked = false;

    private UserBehaviour userBehaviour = UserBehaviour.NONE;
    private RecordingListener recordingListener;
    private ButtonsListener buttonsListener;

    private AudioRecorder audioRecorder;

    private boolean isSaveToDraft = false;

    private boolean mOnRecordingLocked = false;//解决录音锁定时，一直回调RecordingListener.onInputHeightChange的bug

    public MessageInputView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public MessageInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public MessageInputView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        View view = inflate(getContext(), R.layout.msg_recording_layout, null);
        addView(view);

        imageViewAttachment = view.findViewById(R.id.imageViewAttachment);
        imageViewFace = view.findViewById(R.id.imageViewFace);
        editTextMessage = view.findViewById(R.id.editTextMessage);
        imageViewDeleteDraft = view.findViewById(R.id.imageViewDeleteDraft);

        sendButton = view.findViewById(R.id.imageSend);
        stopButton = view.findViewById(R.id.imageStop);
        audioButton = view.findViewById(R.id.imageAudio);
        textViewTip = view.findViewById(R.id.textViewTip);

        imageViewAudio = view.findViewById(R.id.imageViewAudio);
        imageViewStop = view.findViewById(R.id.imageViewStop);
        imageViewSend = view.findViewById(R.id.imageViewSend);
        imageViewLock = view.findViewById(R.id.imageViewLock);
        imageViewLockBlue = view.findViewById(R.id.imageViewLockBlue);
        imageViewLockArrow = view.findViewById(R.id.imageViewLockArrow);
        layoutMessageMask = view.findViewById(R.id.layoutMessageMask);
        timeText = view.findViewById(R.id.textViewTime);
        fireText = view.findViewById(R.id.textViewFire);
        progressBarVolumn = view.findViewById(R.id.progressBarVolumn);
        layoutSlideCancel = view.findViewById(R.id.layoutSlideCancel);
        layoutLock = view.findViewById(R.id.layoutLock);
        layoutDraft = view.findViewById(R.id.layoutDraft);
        imageViewMic = view.findViewById(R.id.imageViewMic);
        layoutInput = view.findViewById(R.id.recording);

        imageViewLock.setAlpha(1.0f);
        imageViewLockBlue.setAlpha(0.0f);
        imageViewLockBlue.setScaleX(1.0f);
        imageViewLockBlue.setScaleY(1.0f);

        animBlink = AnimationUtils.loadAnimation(getContext(),
                R.anim.anim_up_in);
        animJumpFast = AnimationUtils.loadAnimation(getContext(),
                R.anim.jump_fast);

        LottieTask<LottieComposition> compositionCancellable = LottieCompositionFactory.fromAsset(BaseApp.app, "delete_anim.json");
        compositionCancellable.addListener(result -> {
            imageViewMic.setComposition(result);
        });
        imageViewMic.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                deleteAnimComplete();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                deleteAnimComplete();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        layoutInput.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (recordingListener != null && !mOnRecordingLocked) {
                recordingListener.onInputHeightChange(bottom - top);
            }
        });

//        editTextMessage.setOnEditorActionListener((v, actionId, event) -> {
//            boolean isOK = false;
//            if (actionId == EditorInfo.IME_ACTION_SEND) {
//                sendMessage();
//                isOK = true;
//            }
//            return isOK;
//        });

        setupRecording();
    }

    private void deleteAnimComplete() {
        isDeleting = false;

        imageViewMic.setFrame(1);
        imageViewMic.setVisibility(View.INVISIBLE);
        imageViewMic.setRotation(0);

        imageViewAudio.setEnabled(true);
        imageViewAttachment.setVisibility(View.VISIBLE);
        imageViewFace.setVisibility(View.VISIBLE);

        editTextMessage.setVisibility(View.VISIBLE);
        editTextMessage.setFocusableInTouchMode(true);
        editTextMessage.setFocusable(true);
        editTextMessage.requestFocus();

        layoutMessageMask.clearAnimation();
        layoutMessageMask.setVisibility(View.GONE);
        layoutMessageMask.setAlpha(1.0f);
    }

    public void setAudioRecordButtonImage(int imageResource) {
        audioButton.setImageResource(imageResource);
    }

    public void setStopButtonText(String stopText) {
        stopButton.setText(stopText);
    }

    public void setSendButtonImage(int imageResource) {
        sendButton.setImageResource(imageResource);
    }

    public RecordingListener getRecordingListener() {
        return recordingListener;
    }

    public void setRecordingListener(RecordingListener recordingListener) {
        this.recordingListener = recordingListener;
    }

    public ButtonsListener getButtonsListener() {
        return buttonsListener;
    }

    public void setButtonsListener(ButtonsListener buttonsListener) {
        this.buttonsListener = buttonsListener;
    }

    public void mentionUser(long uid, String name) {
        editTextMessage.mentionUser(uid, name);
    }

    public View getSendView() {
        return imageViewSend;
    }

    public View getAttachmentView() {
        return imageViewAttachment;
    }

    public ImageView getFaceView() {
        return (ImageView) imageViewFace;
    }

    public MentionEditText getInputView() {
        return editTextMessage;
    }

    public void setFireText(String text) {
        if (TextUtils.isEmpty(text)) {
            fireText.setVisibility(View.GONE);
        } else {
            fireText.setText(text);
            fireText.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupRecording() {
        editTextMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    imageViewAudio.setVisibility(View.VISIBLE);
                    imageViewSend.setVisibility(View.GONE);
                } else {
                    imageViewAudio.setVisibility(View.GONE);
                    imageViewSend.setVisibility(View.VISIBLE);
                }

                if (recordingListener != null) {
                    recordingListener.onInputing();
                }
            }
        });

        imageViewAudio.setOnTouchListener((view, motionEvent) -> {
            RecordingListener recordingListener = getRecordingListener();
            if (!isDeleting) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if (recordingListener != null && recordingListener.checkRecordable()) {
                        cancelOffset = (float) (imageViewAudio.getX() / 2.8);
                        lockOffset = (float) (imageViewAudio.getX() / 2.5);

                        if (firstX == 0) {
                            firstX = motionEvent.getRawX();
                        }

                        if (firstY == 0) {
                            firstY = motionEvent.getRawY();
                        }

                        startRecord();
                    }
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP
                        || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {

                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        stopRecording(RecordingBehaviour.RELEASED);
                    }

                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                    if (stopTrackingAction) {
                        return true;
                    }

                    UserBehaviour direction = UserBehaviour.NONE;

                    float motionX = Math.abs(firstX - motionEvent.getRawX());
                    float motionY = Math.abs(firstY - motionEvent.getRawY());

                    if (motionX > directionOffset &&
                            motionX > directionOffset &&
                            lastX < firstX && lastY < firstY) {

                        if (motionX > motionY && lastX < firstX) {
                            direction = UserBehaviour.CANCELING;

                        } else if (motionY > motionX && lastY < firstY) {
                            direction = UserBehaviour.LOCKING;
                        }

                    } else if (motionX > motionY && motionX > directionOffset && lastX < firstX) {
                        direction = UserBehaviour.CANCELING;
                    } else if (motionY > motionX && motionY > directionOffset && lastY < firstY) {
                        direction = UserBehaviour.LOCKING;
                    }

                    if (direction == UserBehaviour.CANCELING) {
                        if (userBehaviour == UserBehaviour.NONE || motionEvent.getRawY() + imageViewAudio.getWidth() / 2 > firstY) {
                            userBehaviour = UserBehaviour.CANCELING;
                        }

                        if (userBehaviour == UserBehaviour.CANCELING) {
                            translateX(-(firstX - motionEvent.getRawX()));
                        }
                    } else if (direction == UserBehaviour.LOCKING) {
                        if (userBehaviour == UserBehaviour.NONE || motionEvent.getRawX() + imageViewAudio.getWidth() / 2 > firstX) {
                            userBehaviour = UserBehaviour.LOCKING;
                        }

                        if (userBehaviour == UserBehaviour.LOCKING) {
                            translateY(-(firstY - motionEvent.getRawY()));
                        }
                    }

                    lastX = motionEvent.getRawX();
                    lastY = motionEvent.getRawY();
                }
                view.onTouchEvent(motionEvent);
            }

            return true;
        });

        imageViewStop.setOnClickListener(v -> {
            isLocked = false;
            stopRecording(RecordingBehaviour.CANCELED);
        });

        imageViewFace.setOnClickListener(v -> {
            if (buttonsListener != null) {
                buttonsListener.onClickFace();
            }
        });

        imageViewAttachment.setOnClickListener(v -> {
            if (buttonsListener != null) {
                buttonsListener.onClickTools();
            }
        });

        fireText.setOnClickListener(v -> {
            if (buttonsListener != null) {
                buttonsListener.onClickFire();
            }
        });

        imageViewSend.setOnClickListener(v -> {
            sendMessage();
        });

        imageViewDeleteDraft.setOnClickListener(v -> {
            deleteDraft();
        });
    }

    private void translateY(float y) {
        if (audioRecorder == null || audioRecorder.isLessThanOneSecond()) {
            return;
        }

        float offset = Math.max(1.0f, -y);
        float alpha = Math.max(0.0f, Math.min(1.0f, offset / lockOffset));
        if (alpha > 0.4f) {
            imageViewLock.setAlpha(Math.max(0.0f, 1.0f - alpha));
            imageViewLockBlue.setAlpha(alpha);
            imageViewLockBlue.setScaleX(0.6f + alpha);
            imageViewLockBlue.setScaleY(0.6f + alpha);
        }

        if (y < -lockOffset) {
            locked();
            return;
        }

        layoutLock.setTranslationY(y / 3);
    }

    private void translateX(float x) {
        if (audioRecorder == null) {
            return;
        }

        if (x < -cancelOffset) {
            canceled();
            layoutSlideCancel.setTranslationX(0);
            layoutSlideCancel.setAlpha(1.0f);
            return;
        }

        layoutSlideCancel.setTranslationX(x / 2);
        layoutSlideCancel.setAlpha(Math.max(0.0f, Math.min(1.0f, (cancelOffset + x) / cancelOffset)));
        layoutLock.setTranslationY(0);
    }

    private void locked() {
        stopTrackingAction = true;
        stopRecording(RecordingBehaviour.LOCKED);
        isLocked = true;
    }

    private void canceled() {
        stopTrackingAction = true;
        stopRecording(RecordingBehaviour.CANCELED);
    }

    private void stopRecording(RecordingBehaviour recordingBehaviour) {
        stopTrackingAction = true;
        firstX = 0;
        firstY = 0;
        lastX = 0;
        lastY = 0;

        userBehaviour = UserBehaviour.NONE;

        layoutSlideCancel.stopShimmerAnimation();
        layoutSlideCancel.setTranslationX(0);
        layoutSlideCancel.setAlpha(1.0f);
        layoutSlideCancel.setVisibility(View.GONE);

        imageViewAudio.setAlpha(1.0f);

        layoutLock.setVisibility(View.GONE);
        layoutLock.setTranslationY(0);
        layoutLock.removeCallbacks(layoutLockAnimRunnable);
        layoutLock.clearAnimation();
        imageViewLockArrow.clearAnimation();

        if (isLocked) {
            return;
        }

        if (recordingBehaviour == RecordingBehaviour.LOCKED) {
            imageViewStop.setVisibility(View.VISIBLE);
            imageViewSend.setVisibility(View.VISIBLE);
            imageViewAudio.setVisibility(View.GONE);

            if (recordingListener != null) {
                recordingListener.onRecordingLocked();
                mOnRecordingLocked = true;
            }
        } else if (recordingBehaviour == RecordingBehaviour.CANCELED) {
            cancelRecordAudio();

            isBlink = false;
            timeText.clearAnimation();
            timeText.setTextColor(Color.BLACK);
            timeText.setVisibility(View.INVISIBLE);
            timeText.setText("00:00");
            progressBarVolumn.setVisibility(View.INVISIBLE);
            imageViewStop.setVisibility(View.GONE);
            imageViewSend.setVisibility(View.GONE);

            imageViewLockBlue.setAlpha(0.0f);
            imageViewLockBlue.setScaleX(1.0f);
            imageViewLockBlue.setScaleY(1.0f);
            imageViewLock.setAlpha(1.0f);

            imageViewAttachment.setVisibility(View.VISIBLE);
            imageViewFace.setVisibility(View.VISIBLE);

            if (TextUtils.isEmpty(editTextMessage.getText().toString())) {
                imageViewAudio.setVisibility(View.VISIBLE);
                imageViewSend.setVisibility(View.GONE);
            } else {
                imageViewAudio.setVisibility(View.GONE);
                imageViewSend.setVisibility(View.VISIBLE);
            }

            delete();
        } else if (recordingBehaviour == RecordingBehaviour.RELEASED || recordingBehaviour == RecordingBehaviour.LOCK_DONE) {
            stopRecordAudio();

            isBlink = false;
            timeText.clearAnimation();
            timeText.setTextColor(Color.BLACK);
            timeText.setVisibility(View.INVISIBLE);
            timeText.setText("00:00");
            progressBarVolumn.setVisibility(View.INVISIBLE);
            imageViewMic.setVisibility(View.INVISIBLE);
            imageViewStop.setVisibility(View.GONE);
            imageViewSend.setVisibility(View.GONE);

            editTextMessage.setVisibility(View.VISIBLE);
            editTextMessage.setFocusableInTouchMode(true);
            editTextMessage.setFocusable(true);
            editTextMessage.requestFocus();

            layoutMessageMask.clearAnimation();
            layoutMessageMask.setVisibility(View.GONE);
            layoutMessageMask.setAlpha(1.0f);

            imageViewLockBlue.setAlpha(0.0f);
            imageViewLockBlue.setScaleX(1.0f);
            imageViewLockBlue.setScaleY(1.0f);
            imageViewLock.setAlpha(1.0f);

            imageViewAttachment.setVisibility(View.VISIBLE);
            imageViewFace.setVisibility(View.VISIBLE);

            if (TextUtils.isEmpty(editTextMessage.getText().toString())) {
                imageViewAudio.setVisibility(View.VISIBLE);
                imageViewSend.setVisibility(View.GONE);
            } else {
                imageViewAudio.setVisibility(View.GONE);
                imageViewSend.setVisibility(View.VISIBLE);
            }
        }
    }

    private void startRecord() {
        setupAudioRecorder();

        stopTrackingAction = false;
        imageViewAttachment.setVisibility(View.INVISIBLE);
        imageViewFace.setVisibility(View.INVISIBLE);

        editTextMessage.setVisibility(View.GONE);
        editTextMessage.setFocusable(false);
        editTextMessage.setFocusableInTouchMode(false);

        layoutMessageMask.clearAnimation();
        layoutMessageMask.setVisibility(View.VISIBLE);
        layoutMessageMask.setAlpha(1.0f);

        timeText.clearAnimation();
        timeText.setVisibility(View.VISIBLE);
        timeText.setTextColor(Color.BLACK);
        timeText.setText("00:00");
        progressBarVolumn.setVisibility(View.VISIBLE);
        layoutSlideCancel.setVisibility(View.VISIBLE);
        layoutSlideCancel.startShimmerAnimation();
        imageViewMic.setVisibility(View.VISIBLE);
        imageViewLockArrow.clearAnimation();
        imageViewLockArrow.startAnimation(animJumpFast);

        imageViewAudio.setAlpha(0.5f);

        layoutLock.postDelayed(layoutLockAnimRunnable, 1000);
    }

    private Runnable layoutLockAnimRunnable = new Runnable() {
        @Override
        public void run() {
            layoutLock.setVisibility(View.VISIBLE);
            ObjectAnimator.ofFloat(layoutLock, "alpha", 0f, 1f).setDuration(300).start();
        }
    };

    private Runnable textViewTipAnimRunnable = new Runnable() {
        @Override
        public void run() {
            ObjectAnimator animator = ObjectAnimator.ofFloat(textViewTip, "alpha", 1f, 0f).setDuration(300);
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    textViewTip.setVisibility(View.GONE);
                    textViewTip.setAlpha(1.0f);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    textViewTip.setVisibility(View.GONE);
                    textViewTip.setAlpha(1.0f);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animator.start();
        }
    };

    private void delete() {
        isDeleting = true;
        imageViewAudio.setEnabled(false);
        imageViewMic.playAnimation();
    }

    private void setupAudioRecorder() {
        audioRecorder = new AudioRecorder();
        audioRecorder.setOnAudioRecorderListener(onAudioRecorderListener);
        File saveDir = DirManager.INSTANCE.getVoiceCacheDir(getContext(), AccountManager.INSTANCE.getLoginAccountUUid());
        audioRecorder.startRecording(saveDir, RECORD_MIN_TIME, RECORD_MAX_TIME);
    }

    private void stopRecordAudio() {
        if (audioRecorder != null && audioRecorder.isRecording()) {
            audioRecorder.stopRecording();
            audioRecorder = null;
        }
    }

    private void cancelRecordAudio() {
        if (audioRecorder != null && audioRecorder.isRecording()) {
            audioRecorder.cancelRecording();
            audioRecorder = null;
        }
    }

    public boolean isRecording() {
        return audioRecorder != null && audioRecorder.isRecording();
    }

    /*****************************************与草稿有关的代码*****************************************************/

    private CommonPref commonPref;

    private CommonPref getCommonPref() {
        if (commonPref == null) {
            String storageName = recordingListener == null ? "error_draft_no_storage_name" : recordingListener.getDraftStorageName();
            commonPref = SharePreferencesStorage.INSTANCE.createStorageInstance(CommonPref.class, storageName);
        }
        return commonPref;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //查找是否有草稿
        findDraft();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 停止播放音频
        stopPlay();
    }

    public void stopPlay() {
        if (audioPlayer.isPlaying()) {
            audioPlayer.stopPlaying(new WeakReference<>((Activity) getContext()));
        }
    }

    public void saveToDraft() {
        if (audioRecorder != null && audioRecorder.isRecording()) {
            if (audioRecorder.isLessThanOneSecond()) {
                isLocked = false;
                stopRecording(RecordingBehaviour.CANCELED);
            } else {
                isSaveToDraft = true;
                isLocked = false;
                stopRecording(RecordingBehaviour.RELEASED);
            }
        } else {
            String text = editTextMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(text) && !"@".equals(text)) {
                saveDraft(text);
            } else {
                EventBus.publishEvent(new ChatHistoryChangeEvent());
            }
        }
    }

    private void findDraft() {
        String audioDraftStr = getCommonPref().getAudioRecordDraft();
        if (!TextUtils.isEmpty(audioDraftStr)) {
            AudioDraft audioDraft = GsonInstanceCreater.INSTANCE.getDefaultGson().fromJson(audioDraftStr, AudioDraft.class);
            showDraft(audioDraft);
        } else {
            String textDraft = getCommonPref().getTextDraft();
            getCommonPref().putTextDraft("");
            showDraft(textDraft);
        }
    }

    private void saveDraft(String text) {
        getCommonPref().putTextDraft(text);
        getCommonPref().putAudioRecordDraft("");
        EventBus.publishEvent(new ChatHistoryChangeEvent());
    }

    private void saveDraft(File file, long recordTime, Integer[] highDArr) {
        SendMessageManager.INSTANCE.compressVoice(file, mp3File -> {
            AudioDraft audioDraft = new AudioDraft(mp3File.getAbsolutePath(), recordTime, CastArrayUtil.toPrimitive(highDArr));
            getCommonPref().putAudioRecordDraft(GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(audioDraft));
            getCommonPref().putTextDraft("");
            isSaveToDraft = false;
            EventBus.publishEvent(new ChatHistoryChangeEvent());

            // 如果当前view还显示着，则显示草稿
            if (isAttachedToWindow()) {
                showDraft(audioDraft);
            }
            return null;
        }, throwable -> {
            isSaveToDraft = false;
            return null;
        });
    }

    private void deleteDraft() {
        // 停止播放语音
        stopPlay();
        // 清空草稿
        getCommonPref().putAudioRecordDraft("");
        getCommonPref().putTextDraft("");
        // 还原view
        layoutDraft.setVisibility(View.GONE);
        imageViewSend.setVisibility(View.GONE);

        if (TextUtils.isEmpty(editTextMessage.getText().toString())) {
            imageViewAudio.setVisibility(View.VISIBLE);
            imageViewSend.setVisibility(View.GONE);
        } else {
            imageViewAudio.setVisibility(View.GONE);
            imageViewSend.setVisibility(View.VISIBLE);
        }

        editTextMessage.setVisibility(View.VISIBLE);
        editTextMessage.setFocusableInTouchMode(true);
        editTextMessage.setFocusable(true);
        editTextMessage.requestFocus();

        EventBus.publishEvent(new ChatHistoryChangeEvent());
    }

    private void showDraft(String textDraft) {
        if (!TextUtils.isEmpty(textDraft)) {
            editTextMessage.setText(textDraft);
            editTextMessage.setSelection(textDraft.length());
        }
    }

    private void showDraft(final AudioDraft audioDraft) {
        if (audioDraft != null) {
            layoutDraft.setVisibility(View.VISIBLE);
            imageViewSend.setVisibility(View.VISIBLE);
            imageViewAudio.setVisibility(View.GONE);

            editTextMessage.setVisibility(View.GONE);
            editTextMessage.setFocusable(false);
            editTextMessage.setFocusableInTouchMode(false);

            final MusicSpectrumBar seekBar = findViewById(R.id.seekBar);
            ImageView imgPlay = findViewById(R.id.imgPlay);
            ImageView imgPause = findViewById(R.id.imgPause);
            TextView txtAllTime = findViewById(R.id.txtAllTime);

            imgPlay.setOnClickListener(v -> {
                float seekTo = seekBar.getCurrent() / 100.0f;
                long time = audioDraft.getAudioTime();

                if (recordingListener != null) {
                    recordingListener.onPlayAudioDraft();
                }
                audioPlayer.startPlaying(new WeakReference<>((Activity) getContext()),
                        Uri.fromFile(new File(audioDraft.getAudioPath())), "", true, (int) (seekTo * time));
            });
            imgPause.setOnClickListener(v -> stopPlay());
            seekBar.setOnSeekBarChangeListener(() -> {
                if (audioPlayer.isPlaying()) {
                    float seekTo = seekBar.getCurrent() / 100.0f;
                    long time = audioDraft.getAudioTime();
                    audioPlayer.seekTo((int) (seekTo * time));
                }
            });

            txtAllTime.setText(TimeUtils.INSTANCE.timeFormatToMediaDuration(audioDraft.getAudioTime()));
            resetVoicePlayerUI(audioDraft);
        }
    }

    private void resetVoicePlayerUI(AudioDraft audioDraft) {
        MusicSpectrumBar seekBar = findViewById(R.id.seekBar);
        ImageView imgPlay = findViewById(R.id.imgPlay);
        ImageView imgPause = findViewById(R.id.imgPause);
        ImageView imgFail = findViewById(R.id.imgFail);
        View progressBar = findViewById(R.id.progressBar);
        TextView txtCurrentTime = findViewById(R.id.txtCurrentTime);

        imgPlay.setVisibility(View.VISIBLE);
        imgPause.setVisibility(View.GONE);
        imgFail.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        txtCurrentTime.setText("00:00");

        if (audioDraft != null) {
            int[] arr = audioDraft.getHighDArr();
            int recordTime = (int) (audioDraft.getAudioTime() / 1000.0f);
            if (arr == null || arr.length == 0) {
                arr = AudioSampleUtils.INSTANCE.getDefaultSamples(recordTime);
            } else {
                arr = AudioSampleUtils.INSTANCE.adjustedSamples(audioDraft.getHighDArr(), recordTime);
            }

            int samplesViewWidth = ScreenUtils.dp2px(BaseApp.app, AudioSampleUtils.INSTANCE.getSamplesViewWidth(arr.length));
            ViewGroup.LayoutParams params = seekBar.getLayoutParams();
            params.width = samplesViewWidth;
            seekBar.setLayoutParams(params);
            seekBar.setDatas(audioDraft.getAudioPath().hashCode(), arr);
            seekBar.setCurrent(-1);
        }
    }

    private void updateVoicePlayerSeekUI(long time) {
        MusicSpectrumBar seekBar = findViewById(R.id.seekBar);
        ImageView imgPlay = findViewById(R.id.imgPlay);
        ImageView imgPause = findViewById(R.id.imgPause);
        ImageView imgFail = findViewById(R.id.imgFail);
        View progressBar = findViewById(R.id.progressBar);
        TextView txtCurrentTime = findViewById(R.id.txtCurrentTime);

        imgPlay.setVisibility(View.GONE);
        imgPause.setVisibility(View.VISIBLE);
        imgFail.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        txtCurrentTime.setText(TimeUtils.INSTANCE.timeFormatToMediaDuration(time));

        int totalPosition = (int) audioPlayer.getMediaPlayer().getDuration();
        int currentPosition = (int) audioPlayer.getMediaPlayer().getCurrentPosition();
        seekBar.setCurrent(100 * currentPosition / totalPosition);
    }

    private AudioPlayer.OnAudioPlayerListener onAudioPlayerListener = new AudioPlayer.OnAudioPlayerListener() {

        @Override
        public void onPlayComplete() {
            resetVoicePlayerUI(null);
        }

        @Override
        public void onStopPlay() {
            resetVoicePlayerUI(null);
        }

        @Override
        public void onTimerChange(long time) {
            updateVoicePlayerSeekUI(time);
        }

        @Override
        public void onStartPlay() {

        }

        @Override
        public void onPrepare() {

        }

        @Override
        public void onDownloadComplete() {

        }

        @Override
        public void onDownloadProgress(long totalBytes, long downloadedBytes, int progress) {

        }

        @Override
        public void onDownloadStart() {

        }
    };
    private AudioPlayer audioPlayer = new AudioPlayer(onAudioPlayerListener);

    private AudioRecorder.OnAudioRecorderListener onAudioRecorderListener = new AudioRecorder.OnAudioRecorderListener() {

        @Override
        public void onStartRecord() {
            if (recordingListener != null) {
                recordingListener.onRecordingStarted();
            }
        }

        @Override
        public void onRecordComplete(@NotNull File file, long recordTime, @NotNull Integer[] highDArr) {
            if (recordTime < RECORD_MIN_TIME) {
                return;
            }

            if (isSaveToDraft) {
                //保存到草稿
                saveDraft(file, recordTime, highDArr);
            } else {
                if (recordingListener != null) {
                    recordingListener.onRecordingCompleted(recordTime, file.getAbsolutePath(), highDArr);
                    mOnRecordingLocked = false;
                }
            }
        }

        @Override
        public void onRecordFail(@NotNull Throwable e, @Nullable String info) {
            if (recordingListener != null) {
                recordingListener.onRecordingCanceled();
                mOnRecordingLocked = false;
            }
        }

        @Override
        public void onRecordCancel() {
            if (recordingListener != null) {
                recordingListener.onRecordingCanceled();
                mOnRecordingLocked = false;
            }
        }

        @Override
        public void onRecordSoLong(@NotNull File file, long recordTime, @NotNull Integer[] highDArr) {
            if (isLocked) {
                isLocked = false;
                stopRecording(RecordingBehaviour.LOCK_DONE);
            } else {
                stopRecording(RecordingBehaviour.RELEASED);
            }

            if (isSaveToDraft) {
                //保存到草稿
                saveDraft(file, recordTime, highDArr);
            } else {
                if (recordingListener != null) {
                    recordingListener.onRecordingCompleted(recordTime, file.getAbsolutePath(), highDArr);
                    mOnRecordingLocked = false;
                }
            }
        }

        @Override
        public void onRecordSoShort() {
            if (recordingListener != null) {
                recordingListener.onRecordSoShort();
            }

            textViewTip.removeCallbacks(textViewTipAnimRunnable);
            textViewTip.clearAnimation();

            textViewTip.setVisibility(View.VISIBLE);
            ObjectAnimator.ofFloat(textViewTip, "alpha", 0f, 1f).setDuration(300).start();
            textViewTip.postDelayed(textViewTipAnimRunnable, 3000);
        }

        @Override
        public void onVolumnChange(double volumnLevel) {
            progressBarVolumn.setMax(100);
            progressBarVolumn.setProgress((int) volumnLevel);
        }

        @Override
        public void onTimerChange(long time) {
            if (audioRecorder != null) {
                if (time >= RECORD_MAX_TIME - RECORD_MAX_COUNT_DOWN_TIME) {
                    if (!isBlink) {
                        isBlink = true;
                        timeText.startAnimation(animBlink);
                        timeText.setTextColor(Color.RED);
                    }
                    timeText.setText(getContext().getString(R.string.residue) + ((RECORD_MAX_TIME - time) / 1000) + "s");
                } else {
                    timeText.setText(TimeUtils.INSTANCE.timeFormatToMediaDuration(time));
                }
            }
        }
    };

    public void setKeyboardSend(Boolean isOpen) {
        editTextMessage.setInputType(TYPE_TEXT_FLAG_MULTI_LINE);
        editTextMessage.setSingleLine(false);
//        if (isOpen) {
//            editTextMessage.setImeOptions(IME_ACTION_SEND);
//        } else {
//            editTextMessage.setImeOptions(IME_NULL);
//        }
        editTextMessage.setMaxLines(5);
    }

    private void sendMessage() {
        if (audioRecorder != null && audioRecorder.isRecording()) {
            isLocked = false;
            stopRecording(RecordingBehaviour.LOCK_DONE);
        } else {
            String audioDraftStr = getCommonPref().getAudioRecordDraft();
            AudioDraft audioDraft = GsonInstanceCreater.INSTANCE.getDefaultGson().fromJson(audioDraftStr, AudioDraft.class);
            if (audioDraft != null) {
                // 发送草稿
                deleteDraft();
                if (recordingListener != null) {
                    recordingListener.onRecordingCompleted(audioDraft.getAudioTime(), audioDraft.getAudioPath(), CastArrayUtil.toWrap(audioDraft.getHighDArr()));
                    mOnRecordingLocked = false;
                }
            } else {
                String text = editTextMessage.getText().toString().trim();
                List<Long> uids = editTextMessage.getAtUids();
                editTextMessage.setText("");
                deleteDraft();
                if (buttonsListener != null) {
                    buttonsListener.onClickSend(uids, text);
                }
            }
        }
    }
}
