package com.example.heartrateapp;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DynamicRelaxActivity extends AppCompatActivity {

    private static class Step {
        final String title;
        final String hint;
        final int seconds;

        Step(String title, String hint, int seconds) {
            this.title = title;
            this.hint = hint;
            this.seconds = seconds;
        }
    }

    private final Step[] steps = new Step[] {
            new Step("Rozgrzewka", "Stań prosto. 6 spokojnych wdechów i wydechów.", 30),
            new Step("Dynamiczny ruch", "Unieś ręce w górę przy wdechu, opuść przy wydechu.", 30),
            new Step("Rozluźnienie", "Zrób krążenia barków i rozluźnij szczękę.", 30),
            new Step("Schłodzenie", "Oddychaj wolno: wdech 4s, wydech 6s.", 40)
    };

    private int currentStep = 0;
    private boolean running = false;
    private long remainingMs = 0;
    private CountDownTimer timer;

    private TextView txtStepTitle;
    private TextView txtStepHint;
    private TextView txtStepTimer;
    private Button btnStartPause;
    private Button btnPrev;
    private Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dynamic_relax);

        txtStepTitle = findViewById(R.id.txtStepTitle);
        txtStepHint = findViewById(R.id.txtStepHint);
        txtStepTimer = findViewById(R.id.txtStepTimer);
        btnStartPause = findViewById(R.id.btnStartPause);
        btnPrev = findViewById(R.id.btnPrevStep);
        btnNext = findViewById(R.id.btnNextStep);

        findViewById(R.id.btnFinishRelax).setOnClickListener(v -> finish());

        btnStartPause.setOnClickListener(v -> {
            if (running) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (currentStep > 0) {
                setStep(currentStep - 1);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentStep < steps.length - 1) {
                setStep(currentStep + 1);
            }
        });

        setStep(0);
    }

    private void setStep(int idx) {
        cancelTimer();
        currentStep = idx;
        Step s = steps[currentStep];
        txtStepTitle.setText(s.title);
        txtStepHint.setText(s.hint);
        remainingMs = s.seconds * 1000L;
        running = false;
        btnStartPause.setText("Start");
        updateTimerText(remainingMs);

        btnPrev.setEnabled(currentStep > 0);
        btnNext.setEnabled(currentStep < steps.length - 1);
    }

    private void startTimer() {
        if (remainingMs <= 0) {
            remainingMs = steps[currentStep].seconds * 1000L;
        }

        running = true;
        btnStartPause.setText("Pauza");

        timer = new CountDownTimer(remainingMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMs = millisUntilFinished;
                updateTimerText(remainingMs);
            }

            @Override
            public void onFinish() {
                remainingMs = 0;
                updateTimerText(0);
                running = false;
                btnStartPause.setText("Start");

                if (currentStep < steps.length - 1) {
                    setStep(currentStep + 1);
                }
            }
        }.start();
    }

    private void pauseTimer() {
        cancelTimer();
        running = false;
        btnStartPause.setText("Start");
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void updateTimerText(long ms) {
        long totalSec = Math.max(0, ms) / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        String s = min + ":" + (sec < 10 ? "0" + sec : String.valueOf(sec));
        txtStepTimer.setText(s);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Bezpiecznie: pauza gdy aplikacja idzie w tło
        if (running) {
            pauseTimer();
        }
    }
}
