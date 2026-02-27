package com.example.heartrateapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class RealtimeGraphView extends View {

    // Klasa przechowująca pojedynczy punkt na wykresie
    private static class PointData {
        float value;
        boolean isPeak; // Czy to uderzenie serca?
        PointData(float v, boolean p) { value = v; isPeak = p; }
    }

    private final Paint linePaint = new Paint();
    private final Paint peakPaint = new Paint(); // Pędzel do kropek
    private final Path graphPath = new Path();
    private final List<PointData> dataPoints = new ArrayList<>();

    private static final int MAX_POINTS = 100;

    public RealtimeGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        linePaint.setColor(ContextCompat.getColor(context, R.color.calm_accent));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(6f);
        linePaint.setAntiAlias(true);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        // Wygląd kropki uderzenia serca
        peakPaint.setColor(Color.WHITE); // Zrobiłem białą, żeby ładnie kontrastowała na turkusowej linii
        peakPaint.setStyle(Paint.Style.FILL);
        peakPaint.setAntiAlias(true);
    }

    // Dodano drugi parametr: isPeak
    public void addDataPoint(float value, boolean isPeak) {
        dataPoints.add(new PointData(value, isPeak));
        if (dataPoints.size() > MAX_POINTS) {
            dataPoints.remove(0);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dataPoints.size() < 2) return;

        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (PointData pd : dataPoints) {
            if (pd.value < min) min = pd.value;
            if (pd.value > max) max = pd.value;
        }

        float range = max - min;
        if (range == 0) range = 1;

        graphPath.reset();
        float width = getWidth();
        float height = getHeight();
        float stepX = width / (MAX_POINTS - 1);

        // 1. Najpierw rysujemy linię wykresu
        for (int i = 0; i < dataPoints.size(); i++) {
            float x = i * stepX;
            float normalizedValue = (dataPoints.get(i).value - min) / range;
            float y = normalizedValue * height;

            if (i == 0) {
                graphPath.moveTo(x, y);
            } else {
                graphPath.lineTo(x, y);
            }
        }
        canvas.drawPath(graphPath, linePaint);

        // 2. Potem rysujemy kropki w miejscach "Peak" (żeby były na wierzchu linii)
        for (int i = 0; i < dataPoints.size(); i++) {
            if (dataPoints.get(i).isPeak) {
                float x = i * stepX;
                float normalizedValue = (dataPoints.get(i).value - min) / range;
                float y = normalizedValue * height;

                // Rysujemy kółko o promieniu 10 pikseli
                canvas.drawCircle(x, y, 10f, peakPaint);
            }
        }
    }
}