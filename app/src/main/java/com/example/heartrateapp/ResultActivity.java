package com.example.heartrateapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Odbieramy wynik przekazany z poprzedniego ekranu
        String resultMessage = getIntent().getStringExtra("RESULT_TEXT");

        if (resultMessage == null) {
            resultMessage = "Błąd odczytu";
        }

        TextView txtResult = findViewById(R.id.txtResult);
        txtResult.setText(resultMessage);

        // Przycisk: Zmierz ponownie (otwiera ekran pomiaru)
        findViewById(R.id.btnRetry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultActivity.this, MeasurementActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Przycisk: Relaks (menu z ćwiczeniami i grą)
        findViewById(R.id.btnRelax).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultActivity.this, RelaxMenuActivity.class);
                startActivity(intent);
            }
        });

        // Przycisk: Powrót (zamyka ten ekran, wracając do Menu)
        findViewById(R.id.btnHome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}