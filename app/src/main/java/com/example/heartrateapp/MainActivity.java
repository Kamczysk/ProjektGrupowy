package com.example.heartrateapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Uruchamiamy serwis (ale on teraz siedzi cicho i czeka)
        Intent musicIntent = new Intent(this, MusicService.class);
        startService(musicIntent);

        // ZMIANA: Usunąłem "MusicService.isMusicPlaying = true", bo domyślnie jest false.

        // 2. Obsługa przycisku muzyki
        FloatingActionButton btnMusic = findViewById(R.id.btnMusicToggle);

        // Ustawienie początkowej ikony (Będzie przekreślona, bo isMusicPlaying jest false)
        updateMusicIcon(btnMusic);

        btnMusic.setOnClickListener(v -> {
            // Wysyłamy sygnał "Włącz/Wyłącz"
            Intent toggleIntent = new Intent(this, MusicService.class);
            toggleIntent.setAction("ACTION_TOGGLE");
            startService(toggleIntent);

            // Zmieniamy stan w pamięci (z false na true i odwrotnie)
            MusicService.isMusicPlaying = !MusicService.isMusicPlaying;

            // Aktualizujemy ikonę
            updateMusicIcon(btnMusic);
        });

        // --- RESZTA PRZYCISKÓW ---
        findViewById(R.id.cardMeasure).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MeasurementActivity.class));
        });

        // Tutaj reszta Twoich przycisków (Settings, Relax itp.)
    }

    private void updateMusicIcon(FloatingActionButton btn) {
        if (MusicService.isMusicPlaying) {
            // Muzyka gra -> ikona normalna
            btn.setImageResource(R.drawable.ic_music_on);
        } else {
            // Muzyka STOP -> ikona przekreślona
            btn.setImageResource(R.drawable.ic_music_off);
        }
    }
}