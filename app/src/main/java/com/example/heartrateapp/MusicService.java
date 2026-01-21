package com.example.heartrateapp;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.animation.ValueAnimator;

import androidx.annotation.Nullable;

public class MusicService extends Service {

    private MediaPlayer mediaPlayer;
    private static final float MAX_VOLUME = 0.5f; // Maksymalna głośność (50%)

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Tworzymy odtwarzacz
        mediaPlayer = MediaPlayer.create(this, R.raw.muzyka);

        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true); // Muzyka będzie grać w kółko
            mediaPlayer.setVolume(0, 0);  // Zaczynamy od ciszy
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            fadeInMusic(); // Uruchamiamy efekt zgłaśniania
        }
        return START_STICKY; // Serwis będzie działał, dopóki go nie zatrzymamy
    }

    // Efekt Fade-In (Płynne wejście)
    private void fadeInMusic() {
        final int FADE_DURATION = 3000; // 3 sekundy rozkręcania

        ValueAnimator fadeAnim = ValueAnimator.ofFloat(0, MAX_VOLUME);
        fadeAnim.setDuration(FADE_DURATION);
        fadeAnim.addUpdateListener(animation -> {
            if (mediaPlayer != null) {
                float volume = (float) animation.getAnimatedValue();
                mediaPlayer.setVolume(volume, volume);
            }
        });
        fadeAnim.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}