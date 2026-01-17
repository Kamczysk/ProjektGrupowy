package com.example.heartrateapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement);

        viewFinder = findViewById(R.id.viewFinder);
        txtTimer = findViewById(R.id.txtTimer);
        Button btnDone = findViewById(R.id.btnSimulateDone);
        if (btnDone != null) {
            btnDone.setVisibility(View.GONE);
        }

        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }

        btnDone.setOnClickListener(v -> {
            // Wyłączamy latarkę przed zmianą ekranu (dla pewności)
            if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
                camera.getCameraControl().enableTorch(false);
            }

            Intent intent = new Intent(MeasurementActivity.this, ResultActivity.class);
            if (currentHeartRate>0) {
                intent.putExtra("RESULT_TEXT", String.valueOf(currentHeartRate));
            }else{
                intent.putExtra("RESULT_TEXT", "Błąd pomiaru");
            }
            startActivity(intent);
            finish(); // To też automatycznie zwolni kamerę i wyłączy latarkę
        });
    }

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
                                // Tylko najnowsza klatka (nie chcemy opóźnień)
                                .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                // Mała rozdzielczość (VGA) jest lepsza - procesor szybciej liczy
                                .setTargetResolution(new android.util.Size(640, 480))
                                .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new HeartRateAnalyzer());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                try {
                    cameraProvider.unbindAll();

                    camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview,imageAnalysis);

                    if (camera.getCameraInfo().hasFlashUnit()) {
                        camera.getCameraControl().enableTorch(true);
                    } else {
                        Toast.makeText(this, "Brak latarki w urządzeniu", Toast.LENGTH_SHORT).show();
                    }
                    currentHeartRate = 0;
                    viewFinder.postDelayed(() -> startTimer(), 500);

                } catch (Exception e) {
                    Toast.makeText(this, "Błąd kamery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));

    }
    private void startTimer() {
        // Czas trwania: 15000 ms (15 sekund)
        // Interwał: 1000 ms (co 1 sekundę aktualizacja)
        measurementTimer = new android.os.CountDownTimer(15000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                // Aktualizujemy tekst co sekundę
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                txtTimer.setText("Pozostało: " + secondsRemaining + " s");
            }

            @Override
            public void onFinish() {
                txtTimer.setText("Gotowe!");

                // --- TUTAJ LOGIKA ZAKOŃCZENIA (skopiowana z dawnego przycisku) ---

                // 1. Wyłączamy latarkę
                if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
                    camera.getCameraControl().enableTorch(false);
                }

                // 2. Przechodzimy do wyniku
                Intent intent = new Intent(MeasurementActivity.this, ResultActivity.class);

                // Sprawdzamy, czy udało się zmierzyć tętno przez te 15 sekund
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
        // Jeśli wychodzimy z aplikacji, ubijamy timer
        if (measurementTimer != null) {
            measurementTimer.cancel();
        }
        // Wyłączamy latarkę dla pewności
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(false);
        }
    }

    // --- Sekcja Uprawnień (bez zmian) ---
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
                Toast.makeText(this, "Aplikacja wymaga dostępu do kamery!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    //wewnętrzna klasa analizy tętna
    // Zastąp starą klasę HeartRateAnalyzer tą nową wersją:
    private class HeartRateAnalyzer implements androidx.camera.core.ImageAnalysis.Analyzer {

        // Zmienne do analizy sygnału
        private double lastFilteredBrightness = 0;
        private boolean isBrightnessDropping = false;

        // Zwiększamy minimalny czas do 400ms (max 150 BPM).
        // Dla spoczynkowego tętna to bezpieczne, a wytnie "podwójne" zliczenia.
        private static final long MIN_TIME_BETWEEN_BEATS_MS = 400;

        // Bufor na historię wyników (do wyliczania mediany)
        private final java.util.ArrayList<Integer> bpmHistory = new java.util.ArrayList<>();
        private static final int HISTORY_SIZE = 9; // Większa historia = stabilniejszy wynik

        @Override
        public void analyze(@NonNull androidx.camera.core.ImageProxy image) {
            boolean isRecording = true;
            if (!isRecording) {
                image.close();
                return;
            }

            try {
                java.nio.ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);

                // --- KROK 1: Obliczanie jasności (Bez zmian) ---
                long totalBrightness = 0;
                // Próbkowanie co 10 pikseli dla szybkości
                for (int i = 0; i < data.length; i += 10) {
                    totalBrightness += (data[i] & 0xFF);
                }
                double currentRawBrightness = (double) totalBrightness / (data.length / 10.0);

                // --- KROK 2: FILTRACJA SYGNAŁU (NOWE!) ---
                // Low-Pass Filter: Uśredniamy obecną wartość z poprzednią.
                // To wygładza wykres i usuwa "szpilki" szumu kamery.
                double currentFiltered = (currentRawBrightness + lastFilteredBrightness) / 2;

                // --- KROK 3: DETEKCJA ---
                // Używamy wartości przefiltrowanej
                if (currentFiltered < lastFilteredBrightness) {
                    isBrightnessDropping = true;
                }
                else if (currentFiltered > lastFilteredBrightness && isBrightnessDropping) {
                    // Wykryto dołek (uderzenie)
                    isBrightnessDropping = false;

                    long currentTime = System.currentTimeMillis();
                    long timeDifference = currentTime - lastBeatTime;

                    if (timeDifference > MIN_TIME_BETWEEN_BEATS_MS) {

                        double instantBpm = 60000.0 / timeDifference;

                        // Odrzucamy wyniki absurdalne (np. poniżej 50 lub powyżej 160 w spoczynku)
                        if (instantBpm > 45 && instantBpm < 170) {

                            // Dodajemy do historii
                            bpmHistory.add((int) instantBpm);
                            if (bpmHistory.size() > HISTORY_SIZE) {
                                bpmHistory.remove(0);
                            }

                            // --- KROK 4: MEDIANA (KLUCZ DO SUKCESU) ---
                            // Sortujemy wyniki i bierzemy środkowy.
                            // Jeśli mamy: [70, 72, 130(błąd), 71, 73] -> Po sortowaniu: [70, 71, 72, 73, 130]
                            // Środek to 72. Błąd 130 zostaje całkowicie zignorowany!

                            java.util.ArrayList<Integer> sortedHistory = new java.util.ArrayList<>(bpmHistory);
                            java.util.Collections.sort(sortedHistory);

                            // Bierzemy środkowy element
                            int medianBpm = sortedHistory.get(sortedHistory.size() / 2);

                            currentHeartRate = medianBpm;

                            // Logowanie dla sprawdzenia
                            android.util.Log.d("HEART_RATE", "Raw: " + (int)instantBpm + " | Mediana: " + medianBpm);
                        }

                        lastBeatTime = currentTime;
                    }
                }

                lastFilteredBrightness = currentFiltered;

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                image.close();
            }
        }
    }
}