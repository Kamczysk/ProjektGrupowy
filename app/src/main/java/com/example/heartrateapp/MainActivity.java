package com.example.heartrateapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
//test wiktor
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obsługa kliknięcia w kartę "Pomiar"
        findViewById(R.id.cardMeasure).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MeasurementActivity.class);
                startActivity(intent);
            }
        });

        // Obsługa przycisku Profil (tylko komunikat)
        findViewById(R.id.btnProfile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Profil(chyba)", Toast.LENGTH_SHORT).show();
            }
        });

        // Obsługa przycisku Ustawienia (tylko komunikat)

        // Obsługa przycisku Relaks (menu ćwiczenia / gra)
        findViewById(R.id.btnRelax).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RelaxMenuActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Ustawienia(może)", Toast.LENGTH_SHORT).show();
            }
        });
    }
}