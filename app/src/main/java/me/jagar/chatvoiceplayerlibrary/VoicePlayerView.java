package me.jagar.chatvoiceplayerlibrary;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.app.myfriend.R;

import java.io.IOException;
import java.util.Locale;

public class VoicePlayerView extends LinearLayout {
    private final Button playButton;
    private final SeekBar seekBar;
    private final TextView timeView;
    private MediaPlayer mediaPlayer;
    private String audioPath;

    public VoicePlayerView(Context context) {
        this(context, null);
    }

    public VoicePlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VoicePlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int padding = dp(6);
        setPadding(padding, padding, padding, padding);

        playButton = new Button(context);
        playButton.setText("Play");
        addView(playButton, new LayoutParams(dp(72), dp(40)));

        seekBar = new SeekBar(context);
        LayoutParams seekParams = new LayoutParams(dp(140), ViewGroup.LayoutParams.WRAP_CONTENT);
        addView(seekBar, seekParams);

        timeView = new TextView(context);
        timeView.setText("0:00");
        timeView.setTextColor(Color.BLACK);
        addView(timeView, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        boolean showTiming = true;
        int viewBackground = Color.TRANSPARENT;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VoicePlayerView);
            showTiming = a.getBoolean(R.styleable.VoicePlayerView_showTiming, true);
            viewBackground = a.getColor(R.styleable.VoicePlayerView_viewBackground, Color.TRANSPARENT);
            timeView.setTextColor(a.getColor(R.styleable.VoicePlayerView_progressTimeColor, Color.BLACK));
            a.recycle();
        }
        setBackgroundColor(viewBackground);
        timeView.setVisibility(showTiming ? VISIBLE : GONE);

        playButton.setOnClickListener(v -> togglePlayback());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    updateTime(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    public void setAudio(String audioPath) {
        this.audioPath = audioPath;
        releasePlayer();
        playButton.setText("Play");
        seekBar.setProgress(0);
        timeView.setText("0:00");
    }

    public void refreshPlayer(String audioPath) {
        setAudio(audioPath);
    }

    public void onPause() {
        pausePlayback();
    }

    public void onStop() {
        releasePlayer();
    }

    public void showPlayProgressbar() {
    }

    public void hidePlayProgressbar() {
    }

    public void setTimingVisibility(boolean visibility) {
        timeView.setVisibility(visibility ? VISIBLE : GONE);
    }

    public void setShareButtonVisibility(boolean visibility) {
    }

    public void setShareText(String shareText) {
    }

    public void setViewBackgroundShape(int color, float radius) {
        setBackgroundResource(color);
    }

    public void setShareBackgroundShape(int color, float radius) {
    }

    public void setPlayPaueseBackgroundShape(int color, float radius) {
        playButton.setBackgroundResource(color);
    }

    public void setSeekBarStyle(int progressColor, int thumbColor) {
    }

    private void togglePlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            pausePlayback();
            return;
        }
        startPlayback();
    }

    private void startPlayback() {
        if (audioPath == null || audioPath.trim().isEmpty()) {
            return;
        }
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(getContext(), Uri.parse(audioPath));
                mediaPlayer.setOnPreparedListener(mp -> {
                    seekBar.setMax(mp.getDuration());
                    mp.start();
                    playButton.setText("Pause");
                    post(updateRunnable);
                });
                mediaPlayer.setOnCompletionListener(mp -> {
                    playButton.setText("Play");
                    seekBar.setProgress(0);
                    updateTime(0);
                });
                mediaPlayer.prepareAsync();
            } else {
                mediaPlayer.start();
                playButton.setText("Pause");
                post(updateRunnable);
            }
        } catch (IOException | IllegalArgumentException | IllegalStateException ignored) {
            releasePlayer();
        }
    }

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                int position = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(position);
                updateTime(position);
                postDelayed(this, 500);
            }
        }
    };

    private void pausePlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        playButton.setText("Play");
    }

    private void releasePlayer() {
        removeCallbacks(updateRunnable);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void updateTime(int millis) {
        int seconds = millis / 1000;
        timeView.setText(String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
