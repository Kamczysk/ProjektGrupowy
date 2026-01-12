package com.example.heartrateapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class RelaxMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relax_menu);

        MaterialCardView cardDynamicRelax = findViewById(R.id.cardDynamicRelax);
        MaterialCardView cardGame2048 = findViewById(R.id.cardGame2048);

        cardDynamicRelax.setOnClickListener(v -> {
            Intent intent = new Intent(RelaxMenuActivity.this, DynamicRelaxActivity.class);
            startActivity(intent);
        });

        cardGame2048.setOnClickListener(v -> {
            Intent intent = new Intent(RelaxMenuActivity.this, Game2048Activity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnRelaxBack).setOnClickListener(v -> finish());
    }
}
