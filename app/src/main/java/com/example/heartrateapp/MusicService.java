package com.example.heartrateapp;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class MusicService extends Service {

    private MediaPlayer mediaPlayer;
    // ZMIANA: Domyślnie false, bo startujemy w ciszy
    public static boolean isMusicPlaying = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = MediaPlayer.create(this, R.raw.muzyka);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(0.5f, 0.5f);
            // ZMIANA: Usunąłem tutaj mediaPlayer.start()
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Reagujemy TYLKO na komendę przełączenia
        if (intent != null && "ACTION_TOGGLE".equals(intent.getAction())) {
            toggleMusic();
        }
        // ZMIANA: Usunąłem sekcję "else", która wymuszała start muzyki

        return START_STICKY;
    }

    private void startMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            isMusicPlaying = true;
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isMusicPlaying = false;
        }
    }

    private void toggleMusic() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                pauseMusic();
            } else {
                startMusic();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isMusicPlaying = false;
    }
}