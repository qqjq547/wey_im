/*
This class is a part of Chat Voice Player View Android Library
Developed by: Jagar Yousef
Date: 2019
*/

package framework.telegram.ui.voice;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import framework.telegram.ui.R;

public class VoicePlayerView extends LinearLayout implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static LastPlayingVoiceInfo sLastPlayingVoiceInfo;

    private String audioPath;
    private long audioTime;

    private ImageView imgPlay, imgPause;
    public ImageView imgFail;
    private View progressBar;
    private SeekBar seekBar;
    private TextView txtCurrentTime, txtAllTime;
    private PlaySettingCallback playSettingCallback;

    public VoicePlayerView(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.voice_player_view, this);
    }

    public VoicePlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews(context, attrs);
    }

    public VoicePlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews(context, attrs);
    }

    private void initViews(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.VoicePlayerView, 0, 0);

        int playIconResId;
        int pauseIconResId;
        int failIconResId;
        int seekBarProgressColor;
        int seekBarThumbColor;
        int timeColor;
        try {
            playIconResId = typedArray.getResourceId(R.styleable.VoicePlayerView_playIcon, R.drawable.ic_play);
            pauseIconResId = typedArray.getResourceId(R.styleable.VoicePlayerView_pauseIcon, R.drawable.ic_pause);
            failIconResId = typedArray.getResourceId(R.styleable.VoicePlayerView_failIcon, R.drawable.msg_icon_fail);
            seekBarProgressColor = typedArray.getColor(R.styleable.VoicePlayerView_seekBarProgressColor, getResources().getColor(R.color.c178aff));
            seekBarThumbColor = typedArray.getColor(R.styleable.VoicePlayerView_seekBarThumbColor, getResources().getColor(R.color.c178aff));
            timeColor = typedArray.getColor(R.styleable.VoicePlayerView_timeColor, getResources().getColor(R.color.white));
        } finally {
            typedArray.recycle();
        }

        LayoutInflater.from(context).inflate(R.layout.voice_player_view, this);
        imgPlay = this.findViewById(R.id.imgPlay);
        imgPause = this.findViewById(R.id.imgPause);
        imgFail = this.findViewById(R.id.imgFail);
        progressBar = this.findViewById(R.id.progressBar);
        seekBar = this.findViewById(R.id.seekBar);
        txtCurrentTime = this.findViewById(R.id.txtCurrentTime);
        txtAllTime = this.findViewById(R.id.txtAllTime);

        imgPlay.setImageResource(playIconResId);
        imgPause.setImageResource(pauseIconResId);
        imgFail.setImageResource(failIconResId);
        seekBar.getProgressDrawable().setColorFilter(seekBarProgressColor, PorterDuff.Mode.SRC_IN);
        seekBar.getThumb().setColorFilter(seekBarThumbColor, PorterDuff.Mode.SRC_IN);
        seekBar.setOnTouchListener((v, event) -> true);
        txtCurrentTime.setTextColor(timeColor);
        txtAllTime.setTextColor(timeColor);
    }

    //Set the audio source and prepare mediaplayer
    public void setAudio(int audioTime, String audioPath) {
        if (this.audioPath != null && this.audioPath.equals(audioPath)) {
            return;
        }

        this.audioTime = audioTime;
        this.audioPath = audioPath;
//        seekBar.setOnSeekBarChangeListener(seekBarListener);
        imgPlay.setOnClickListener(imgPlayClickListener);
        imgPause.setOnClickListener(imgPauseClickListener);
        hideProgressBar();
    }

    //Components' listeners
    OnClickListener imgPlayClickListener = v -> playVoice();

    OnClickListener imgPauseClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (sLastPlayingVoiceInfo != null && sLastPlayingVoiceInfo.mediaPlayer != null) {
                sLastPlayingVoiceInfo.mediaPlayer.pause();
                AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
                am.setMode(AudioManager.MODE_NORMAL);
            }
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (sLastPlayingVoiceInfo != null && sLastPlayingVoiceInfo.mediaPlayer != null && fromUser) {
                sLastPlayingVoiceInfo.mediaPlayer.seekTo(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (sLastPlayingVoiceInfo != null && sLastPlayingVoiceInfo.mediaPlayer != null) {
                sLastPlayingVoiceInfo.mediaPlayer.start();
            }
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startUpdateUI();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopUpdateUI();
    }

    public void showProgressBar() {
        imgPlay.setVisibility(View.GONE);
        imgPause.setVisibility(View.GONE);
        imgFail.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar() {
        imgPlay.setVisibility(View.VISIBLE);
        imgPause.setVisibility(View.GONE);
        imgFail.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    public void showLoadFail() {
        imgPlay.setVisibility(View.GONE);
        imgPause.setVisibility(View.GONE);
        imgFail.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    public void hideLoadFail() {
        imgPlay.setVisibility(View.VISIBLE);
        imgPause.setVisibility(View.GONE);
        imgFail.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    //Updating seekBar in realtime
    private void updateUI() {
        if (progressBar.getVisibility() == View.VISIBLE) {
            return;
        }

        if (sLastPlayingVoiceInfo != null && sLastPlayingVoiceInfo.audioPath.equals(audioPath)) {
            try {
                //设置进来的是正在播放的音频
                if (sLastPlayingVoiceInfo.mediaPlayer.isPlaying()) {
                    imgPause.setVisibility(View.VISIBLE);
                    imgPlay.setVisibility(View.GONE);
                } else {
                    imgPause.setVisibility(View.GONE);
                    imgPlay.setVisibility(View.VISIBLE);
                }

//                    seekBar.setEnabled(true);

                if (sLastPlayingVoiceInfo.mediaPlayer.getDuration() - sLastPlayingVoiceInfo.mediaPlayer.getCurrentPosition() > 100) {
                    txtCurrentTime.setText(convertSecondsToHMmSs(sLastPlayingVoiceInfo.mediaPlayer.getCurrentPosition() / 1000));
                    txtAllTime.setText(convertSecondsToHMmSs(sLastPlayingVoiceInfo.mediaPlayer.getDuration() / 1000));
                    seekBar.setMax(sLastPlayingVoiceInfo.mediaPlayer.getDuration());
                    seekBar.setProgress(sLastPlayingVoiceInfo.mediaPlayer.getCurrentPosition());
                } else {
                    txtCurrentTime.setText("00:00:00");
                    txtAllTime.setText(convertSecondsToHMmSs(sLastPlayingVoiceInfo.mediaPlayer.getDuration() / 1000));
                    seekBar.setMax(sLastPlayingVoiceInfo.mediaPlayer.getDuration());
                    seekBar.setProgress(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
                imgPause.setVisibility(View.GONE);
                imgPlay.setVisibility(View.VISIBLE);
                txtCurrentTime.setText("00:00:00");
                txtAllTime.setText(convertSecondsToHMmSs(audioTime / 1000));
                seekBar.setProgress(0);
//                    seekBar.setEnabled(false);
            }
        } else {
            imgPause.setVisibility(View.GONE);
            imgPlay.setVisibility(View.VISIBLE);
            txtCurrentTime.setText("00:00:00");
            txtAllTime.setText(convertSecondsToHMmSs(audioTime / 1000));
            seekBar.setProgress(0);
//                seekBar.setEnabled(false);
        }
    }

    public void speakerPlay(@NonNull Activity activity) {
        playVoice(activity, true);
    }

    public void playVoice() {
        playVoice(null, playSettingCallback != null && playSettingCallback.isSpeakerPlay());
    }

    private void playVoice(Activity activity, boolean speakerPlay) {
        if (TextUtils.isEmpty(audioPath)) {
            return;
        }

        destoryLastPlay();
        sLastPlayingVoiceInfo = new LastPlayingVoiceInfo(audioPath, new MediaPlayer());
        try {
            sLastPlayingVoiceInfo.mediaPlayer.setDataSource(audioPath);

            if (speakerPlay) {
                //听筒播放
                activity = activity == null ? (Activity) getContext() : activity;
                AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
                am.setSpeakerphoneOn(false);
                activity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
                am.setMode(AudioManager.MODE_IN_CALL);
                sLastPlayingVoiceInfo.mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            } else {
                //扬声器播放
                sLastPlayingVoiceInfo.mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }

            sLastPlayingVoiceInfo.mediaPlayer.prepare();
            sLastPlayingVoiceInfo.mediaPlayer.setVolume(10, 10);
            //START and PAUSE are in other listeners
            sLastPlayingVoiceInfo.mediaPlayer.setOnPreparedListener(this);
            sLastPlayingVoiceInfo.mediaPlayer.setOnCompletionListener(this);
            sLastPlayingVoiceInfo.mediaPlayer.setOnErrorListener(this);
        } catch (IOException e) {
            e.printStackTrace();
            destoryLastPlay();
        }
    }

    public static void destoryLastPlay() {
        if (sLastPlayingVoiceInfo != null) {
            if (sLastPlayingVoiceInfo.mediaPlayer != null) {
                try {
                    sLastPlayingVoiceInfo.mediaPlayer.setOnPreparedListener(null);
                    sLastPlayingVoiceInfo.mediaPlayer.setOnCompletionListener(null);
                    sLastPlayingVoiceInfo.mediaPlayer.stop();
                    sLastPlayingVoiceInfo.mediaPlayer.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            sLastPlayingVoiceInfo = null;
        }
    }

    //Convert long milli seconds to a formatted String to display it

    private static String convertSecondsToHMmSs(long seconds) {
        long s = seconds % 60;
        long m = (seconds / 60) % 60;
        long h = (seconds / (60 * 60)) % 24;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private static Timer sTimer;
    private static Set<VoicePlayerView> sVoicePlayerViews;

    private void startUpdateUI() {
        if (sTimer == null) {
            sTimer = new Timer();
            sTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    ((Activity) getContext()).runOnUiThread(() -> {
                        if (sVoicePlayerViews != null && !sVoicePlayerViews.isEmpty()) {
                            List<VoicePlayerView> allView = new ArrayList<>(sVoicePlayerViews);
                            for (VoicePlayerView v : allView) {
                                v.updateUI();
                            }
                        }
                    });
                }
            }, 0, 1000);
        } else {
            updateUI();
        }

        if (sVoicePlayerViews == null) {
            sVoicePlayerViews = new HashSet<>();
        }
        sVoicePlayerViews.add(this);
    }

    private void stopUpdateUI() {
        if (sVoicePlayerViews != null) {
            sVoicePlayerViews.remove(this);
        }

        if (sVoicePlayerViews == null || sVoicePlayerViews.isEmpty()) {
            if (sTimer != null) {
                sTimer.cancel();
                sTimer = null;
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        destoryLastPlay();
        imgPause.setVisibility(View.GONE);
        imgPlay.setVisibility(View.VISIBLE);

        AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_NORMAL);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        seekBar.setMax(mp.getDuration());
        txtCurrentTime.setText("00:00:00");
        txtAllTime.setText(convertSecondsToHMmSs(mp.getDuration() / 1000));
        mp.start();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        destoryLastPlay();
        return false;
    }

    public void setPlaySettingCallback(PlaySettingCallback playSettingCallback) {
        this.playSettingCallback = playSettingCallback;
    }

    private static class LastPlayingVoiceInfo {
        MediaPlayer mediaPlayer;
        String currentTime;
        String allTime;
        int progress;
        int max;
        String audioPath;

        public LastPlayingVoiceInfo(String audioPath, MediaPlayer mediaPlayer) {
            this.audioPath = audioPath;
            this.mediaPlayer = mediaPlayer;
        }
    }

    // Programmatically functions

    public void setPlayIconStyle(int resId) {
        imgPlay.setImageResource(resId);
    }

    public void setSeekBarStyle(int progressColor, int thumbColor) {
        seekBar.getProgressDrawable().setColorFilter(getResources().getColor(progressColor), PorterDuff.Mode.SRC_IN);
        seekBar.getThumb().setColorFilter(getResources().getColor(thumbColor), PorterDuff.Mode.SRC_IN);
    }

    public static interface PlaySettingCallback {
        boolean isSpeakerPlay();
    }
}
