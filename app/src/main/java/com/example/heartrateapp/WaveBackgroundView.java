package com.example.heartrateapp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class WaveBackgroundView extends View {

    private final Paint wavePaint = new Paint();
    private final List<Wave> waves = new ArrayList<>();
    private int waveColor;

    public WaveBackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        waveColor = ContextCompat.getColor(context, R.color.calm_accent);
        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeWidth(4f); // Delikatniejsza, cieńsza linia (z 5 na 4)
        wavePaint.setAntiAlias(true);

        // ZMIANA 1: Tworzymy tylko 2 fale zamiast 3
        // Druga fala startuje z opóźnieniem 4000 ms (4 sekund) po pierwszej
        for (int i = 0; i < 2; i++) {
            waves.add(new Wave(i * 4000L));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() * 0.46f;

        // ZMIANA: Promień równy odległości do najdalszego rogu ekranu,
        // żeby fala na pewno wyleciała całkowicie za ekran
        float maxRadius = (float) Math.hypot(centerX, getHeight() - centerY);

        for (Wave wave : waves) {
            if (!wave.isStarted) {
                wave.start(maxRadius);
            }

            // Ustawiamy przezroczystość (zanika przy krawędziach, pojawia się w środku)
            wavePaint.setColor(waveColor);
            wavePaint.setAlpha((int) (255 * (1f - wave.progress)));

            // Rysujemy okrąg
            canvas.drawCircle(centerX, centerY, wave.currentRadius, wavePaint);
        }

        // Wymuszamy ciągłe przerysowywanie
        invalidate();
    }

    private class Wave {
        float currentRadius = 0f;
        float progress = 0f;
        boolean isStarted = false;
        long startDelay;

        Wave(long delay) {
            this.startDelay = delay;
        }

        void start(float maxRadius) {
            isStarted = true;
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);

            // ZMIANA 2: Bardzo wolna animacja - 8 sekund w jedną stronę!
            animator.setDuration(8000);

            // ZMIANA 3: Nieskończona pętla, ale w trybie REVERSE (Wte i wewte)
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.REVERSE); // Rozszerza się, a potem kurczy!

            // ZMIANA 4: "Miękkie" zwalnianie na końcach (jak przy oddychaniu)
            animator.setInterpolator(new AccelerateDecelerateInterpolator());

            animator.setStartDelay(startDelay);

            animator.addUpdateListener(animation -> {
                progress = (float) animation.getAnimatedValue();
                currentRadius = progress * maxRadius;
            });
            animator.start();
        }
    }
}