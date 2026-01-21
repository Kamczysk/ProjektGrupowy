package com.example.heartrateapp;

// --- WSZYSTKIE POTRZEBNE IMPORTY ---
import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MeasurementActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private Camera camera;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private long lastBeatTime = 0;
    private int currentHeartRate = 0;
    private TextView txtTimer;
    private android.os.CountDownTimer measurementTimer;

    // Przyciski jako zmienne klasowe, żeby mieć do nich dostęp wszędzie
    private Button btnStart;
    private Button btnInstruction;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement);

        // 1. Znajdowanie widoków
        viewFinder = findViewById(R.id.viewFinder);
        txtTimer = findViewById(R.id.txtTimer);
        // Uwaga: zmieniłem ID przycisku start w XML na bardziej logiczne
        btnStart = findViewById(R.id.btnStartMeasurement);
        btnInstruction = findViewById(R.id.btnShowInstruction);

        // 2. Sprawdzenie uprawnień i start kamery
        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }

        // 3. OBSŁUGA PRZYCISKU INSTRUKCJI (NAPRAWIONA)
        btnInstruction.setOnClickListener(v -> {
            // Po kliknięciu ma się TYLKO pokazać okienko.
            // Przycisk nie powinien znikać, bo może ktoś chce przeczytać jeszcze raz.
            showInstructionDialog();
        });

        // 4. OBSŁUGA PRZYCISKU START (NAPRAWIONA)
        btnStart.setOnClickListener(v -> {
            // Ten przycisk powinien działać ZAWSZE.

            // Ukrywamy OBA przyciski, żeby nie przeszkadzały podczas pomiaru
            btnStart.setVisibility(View.GONE);
            btnInstruction.setVisibility(View.GONE);

            // Resetujemy wynik i startujemy timer
            currentHeartRate = 0;
            txtTimer.setText("Przygotowanie...");
            // Dajemy chwilę na ustabilizowanie palca przed startem odliczania
            viewFinder.postDelayed(() -> startTimer(), 1000);
        });
    }

    // --- Metoda wyświetlająca okienko z instrukcją ---
    private void showInstructionDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            // Upewnij się, że plik dialog_instructions.xml istnieje!
            View dialogView = inflater.inflate(R.layout.activity_instructions, null);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            // Przezroczyste tło dla ładnych zaokrąglonych rogów
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            dialog.show();

            // Obsługa przycisku "Zamknij" wewnątrz okienka
            Button btnClose = dialogView.findViewById(R.id.btnCloseDialog);
            btnClose.setOnClickListener(v -> dialog.dismiss());

        } catch (Exception e) {
            // Jeśli coś pójdzie nie tak, wyświetli się komunikat błędu
            Toast.makeText(this, "Błąd otwarcia instrukcji: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    // --- POZOSTAŁE METODY (startCamera, startTimer, permission) BEZ ZMIAN ---
    // (Dla pewności wklejam je tutaj, żebyś miał kompletny plik)

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                androidx.camera.core.ImageAnalysis imageAnalysis =
                        new androidx.camera.core.ImageAnalysis.Builder()
                                .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setTargetResolution(new android.util.Size(640, 480))
                                .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new HeartRateAnalyzer());
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                try {
                    cameraProvider.unbindAll();
                    camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview,imageAnalysis);

                    if (camera.getCameraInfo().hasFlashUnit()) {
                        camera.getCameraControl().enableTorch(true);
                    }
                    currentHeartRate = 0;

                } catch (Exception e) {
                    Toast.makeText(this, "Błąd kamery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startTimer() {
        measurementTimer = new android.os.CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                txtTimer.setText("Pozostało: " + secondsRemaining + " s");
            }

            @Override
            public void onFinish() {
                txtTimer.setText("Gotowe!");
                if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
                    camera.getCameraControl().enableTorch(false);
                }
                Intent intent = new Intent(MeasurementActivity.this, ResultActivity.class);
                if (currentHeartRate > 0) {
                    intent.putExtra("RESULT_TEXT", String.valueOf(currentHeartRate));
                } else {
                    intent.putExtra("RESULT_TEXT", "Błąd pomiaru");
                }
                startActivity(intent);
                finish();
            }
        };
        measurementTimer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (measurementTimer != null) { measurementTimer.cancel(); }
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(false);
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Wymagany dostęp do kamery!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    // --- ANALIZATOR (BEZ ZMIAN) ---
    private class HeartRateAnalyzer implements androidx.camera.core.ImageAnalysis.Analyzer {
        private double lastFilteredBrightness = 0;
        private boolean isBrightnessDropping = false;
        private static final long MIN_TIME_BETWEEN_BEATS_MS = 400;
        private final java.util.ArrayList<Integer> bpmHistory = new java.util.ArrayList<>();
        private static final int HISTORY_SIZE = 9;

        @Override
        public void analyze(@NonNull androidx.camera.core.ImageProxy image) {
            try {
                java.nio.ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                long totalBrightness = 0;
                for (int i = 0; i < data.length; i += 10) { totalBrightness += (data[i] & 0xFF); }
                double currentRawBrightness = (double) totalBrightness / (data.length / 10.0);
                double currentFiltered = (currentRawBrightness + lastFilteredBrightness) / 2;

                if (currentFiltered < lastFilteredBrightness) {
                    isBrightnessDropping = true;
                } else if (currentFiltered > lastFilteredBrightness && isBrightnessDropping) {
                    isBrightnessDropping = false;
                    long currentTime = System.currentTimeMillis();
                    long timeDifference = currentTime - lastBeatTime;
                    if (timeDifference > MIN_TIME_BETWEEN_BEATS_MS) {
                        double instantBpm = 60000.0 / timeDifference;
                        if (instantBpm > 45 && instantBpm < 170) {
                            bpmHistory.add((int) instantBpm);
                            if (bpmHistory.size() > HISTORY_SIZE) { bpmHistory.remove(0); }
                            java.util.ArrayList<Integer> sortedHistory = new java.util.ArrayList<>(bpmHistory);
                            java.util.Collections.sort(sortedHistory);
                            int medianBpm = sortedHistory.get(sortedHistory.size() / 2);
                            currentHeartRate = medianBpm;
                        }
                        lastBeatTime = currentTime;
                    }
                }
                lastFilteredBrightness = currentFiltered;
            } catch (Exception e) { e.printStackTrace(); } finally { image.close(); }
        }
    }
}